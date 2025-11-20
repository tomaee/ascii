package org.example;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class Optimizer {

    private final Map<Character, BufferedImage> charImages;
    private final int cellWidth;
    private final int cellHeight;
    private final AlignmentInsensitiveShapeSimilarity aiss;

    // Internal state for fast local scoring
    private double[][] currentCellScores;
    private final int BORDER_SIZE = 1;

    private static final int TOP_K = 5; // candidate chars per cell

    public Optimizer(final Map<Character, BufferedImage> charImages, final int cellWidth, final int cellHeight) {
        this.charImages = charImages;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.aiss = new AlignmentInsensitiveShapeSimilarity();
    }

    public String optimize(final BufferedImage skeletonImage,
                           final String initialAscii,
                           final int maxIterations,
                           final double coolingRate,
                           final int patience,
                           final int logEveryN) {

        final char[][] bestGrid = asciiToGrid(initialAscii);
        final char[][] currentGrid = deepCopyGrid(bestGrid);

        final int rows = currentGrid.length;
        final int cols = currentGrid[0].length;

        // 1. Initialize internal score map and calculate initial total energy
        double bestScore = initializeCellScores(currentGrid, skeletonImage, rows, cols);
        double currentScore = bestScore;

        // 2. Estimate robust starting temperature based on sampled energy changes (Critical for stability)
        double initialTemp = estimateInitialTemperature(currentGrid, skeletonImage, rows, cols);
        double temp = initialTemp;

        // 3. Pre-calculate candidate list
        final List<List<List<Character>>> candidateMap = buildCandidateList(skeletonImage, rows, cols);

        int iteration = 0;
        int noImprovement = 0;

        System.out.println("Optimization started. Estimated initial Temp: " + String.format("%.4f", initialTemp));

        while (iteration < maxIterations && noImprovement < patience && temp > 0.0001) {

            // Choose a random cell *inside* the border
            final int ry = ThreadLocalRandom.current().nextInt(rows - 2 * BORDER_SIZE) + BORDER_SIZE;
            final int rx = ThreadLocalRandom.current().nextInt(cols - 2 * BORDER_SIZE) + BORDER_SIZE;

            // Skip optimization on empty cells
            if (currentGrid[ry][rx] == ' ') {
                noImprovement++;
                temp *= coolingRate;
                iteration++;
                continue;
            }

            final List<Character> candidates = candidateMap.get(ry).get(rx);
            final char oldChar = currentGrid[ry][rx];

            // Choose a new, different candidate from the top K
            char newChar = oldChar;
            int attempts = 0;
            while (newChar == oldChar && attempts < 10) {
                newChar = candidates.get(ThreadLocalRandom.current().nextInt(Math.min(TOP_K, candidates.size())));
                attempts++;
            }
            if (newChar == oldChar) {
                noImprovement++;
                temp *= coolingRate;
                iteration++;
                continue;
            }

            // --- Local Score Calculation ---
            final BufferedImage cell = skeletonImage.getSubimage(
                    rx * cellWidth, ry * cellHeight, cellWidth, cellHeight);

            final double oldLocalScore = currentCellScores[ry][rx];
            final double newLocalScore = computeLocalScore(cell, newChar);

            // Calculate the change in total energy (Delta E)
            final double scoreDifference = newLocalScore - oldLocalScore;

            // --- Simulated Annealing Acceptance ---
            if (scoreDifference < 0.0 || ThreadLocalRandom.current().nextDouble() < Math.exp(-scoreDifference / temp)) {

                // Acceptance: Update the grid and update running scores
                currentGrid[ry][rx] = newChar;
                currentScore += scoreDifference;
                currentCellScores[ry][rx] = newLocalScore; // Update the cell score map

                if (currentScore < bestScore) {
                    bestScore = currentScore;
                    copyGrid(currentGrid, bestGrid);
                    noImprovement = 0;
                } else {
                    noImprovement++;
                }
            } else {
                // Rejection
                noImprovement++;
            }

            if (logEveryN > 0 && iteration % logEveryN == 0) {
                System.out.println("iter=" + iteration + " temp=" + String.format("%.4f", temp) +
                        " best=" + String.format("%.4f", bestScore) + " curr=" + String.format("%.4f", currentScore));
            }

            temp *= coolingRate;
            iteration++;
        }

        System.out.println("Optimization finished: iterations=" + iteration + " final best score: " + String.format("%.4f", bestScore));
        return gridToAscii(bestGrid);
    }

    /**
     * Samples random moves to estimate the average energy change (|Delta E|),
     * which is used as the robust initial temperature (T0 = Avg|Delta E|).
     */
    private double estimateInitialTemperature(final char[][] grid, final BufferedImage skeleton, final int rows, final int cols) {
        final int SAMPLE_SIZE = 200;
        double totalDeltaE = 0.0;
        int acceptedSamples = 0;

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            final int ry = ThreadLocalRandom.current().nextInt(rows - 2 * BORDER_SIZE) + BORDER_SIZE;
            final int rx = ThreadLocalRandom.current().nextInt(cols - 2 * BORDER_SIZE) + BORDER_SIZE;

            if (grid[ry][rx] == ' ') continue;

            final List<Map.Entry<Character, Double>> scores = calculateAllLocalScores(
                    skeleton.getSubimage(rx * cellWidth, ry * cellHeight, cellWidth, cellHeight));

            if (scores.isEmpty()) continue;

            final char oldChar = grid[ry][rx];
            final double oldLocalScore = currentCellScores[ry][rx];

            // Pick a random new character from the entire set
            final char newChar = scores.get(ThreadLocalRandom.current().nextInt(scores.size())).getKey();

            if (newChar == oldChar) continue;

            final double newLocalScore = computeLocalScore(
                    skeleton.getSubimage(rx * cellWidth, ry * cellHeight, cellWidth, cellHeight), newChar);

            // Calculate the absolute difference
            totalDeltaE += Math.abs(newLocalScore - oldLocalScore);
            acceptedSamples++;
        }

        // Return average Delta E, or a safe floor if sampling failed
        return acceptedSamples > 0 ? totalDeltaE / acceptedSamples : 0.5;
    }


    /** Calculates the initial total AISS score and populates the currentCellScores map. */
    private double initializeCellScores(final char[][] grid, final BufferedImage skeleton, final int rows, final int cols) {
        this.currentCellScores = new double[rows][cols];
        double totalScore = 0.0;

        for (int y = BORDER_SIZE; y < rows - BORDER_SIZE; y++) {
            for (int x = BORDER_SIZE; x < cols - BORDER_SIZE; x++) {
                char ch = grid[y][x];

                if (ch == ' ') {
                    this.currentCellScores[y][x] = 0.0;
                    continue;
                }

                final BufferedImage cell = skeleton.getSubimage(
                        x * cellWidth, y * cellHeight, cellWidth, cellHeight);

                double score = computeLocalScore(cell, ch);
                this.currentCellScores[y][x] = score;
                totalScore += score;
            }
        }
        return totalScore;
    }

    /** Computes the AISS score for a single cell and character. */
    private double computeLocalScore(final BufferedImage cell, final char glyph) {
        final BufferedImage glyphImg = charImages.get(glyph);
        if (glyphImg == null) return Double.MAX_VALUE;
        return aiss.computeSimilarity(cell, glyphImg);
    }

    /** Calculates local scores for ALL characters for a given cell. Used for T0 estimation. */
    private List<Map.Entry<Character, Double>> calculateAllLocalScores(final BufferedImage cell) {
        final List<Map.Entry<Character, Double>> scores = new ArrayList<>();
        for (final Map.Entry<Character, BufferedImage> entry : charImages.entrySet()) {
            final double score = aiss.computeSimilarity(cell, entry.getValue());
            scores.add(Map.entry(entry.getKey(), score));
        }
        return scores;
    }


    /** Pre-calculates the top K candidates for every cell. */
    private List<List<List<Character>>> buildCandidateList(final BufferedImage skeleton,
                                                           final int rows, final int cols) {
        final List<List<List<Character>>> result = new ArrayList<>();
        for (int y = 0; y < rows; y++) {
            result.add(new ArrayList<>());
            for (int x = 0; x < cols; x++) {
                final BufferedImage cell = skeleton.getSubimage(
                        x * cellWidth, y * cellHeight, cellWidth, cellHeight);

                // Use the helper that calculates all scores
                final List<Map.Entry<Character, Double>> scores = calculateAllLocalScores(cell);

                scores.sort(Map.Entry.comparingByValue());
                final List<Character> best = new ArrayList<>();
                for (int i = 0; i < Math.min(TOP_K, scores.size()); i++) {
                    best.add(scores.get(i).getKey());
                }
                result.get(y).add(best);
            }
        }
        return result;
    }

    // --- Utility methods ---

    private static char[][] asciiToGrid(final String ascii) {
        final String[] lines = ascii.split("\n");
        if (lines.length == 0) return new char[0][0];

        // Determine column width from the first non-empty line
        int cols = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                cols = line.length();
                break;
            }
        }
        if (cols == 0) return new char[lines.length][0];


        final char[][] grid = new char[lines.length][cols];
        for (int i = 0; i < lines.length; i++) {
            // Fill row with spaces first
            char[] row = new char[cols];
            Arrays.fill(row, ' ');

            // Copy existing content up to the max width
            System.arraycopy(lines[i].toCharArray(), 0, row, 0, Math.min(lines[i].length(), cols));
            grid[i] = row;
        }
        return grid;
    }

    private static void copyGrid(final char[][] src, final char[][] dst) {
        for (int i = 0; i < src.length; i++) dst[i] = Arrays.copyOf(src[i], src[i].length);
    }

    private static char[][] deepCopyGrid(final char[][] src) {
        final char[][] dst = new char[src.length][];
        for (int i = 0; i < src.length; i++) dst[i] = Arrays.copyOf(src[i], src[i].length);
        return dst;
    }

    private static String gridToAscii(final char[][] grid) {
        final StringBuilder sb = new StringBuilder();
        for (final char[] row : grid) sb.append(row).append("\n");
        return sb.toString();
    }
}