package org.example;

import java.awt.image.BufferedImage;
import java.util.*;

public final class Optimizer {

    private final Map<Character, BufferedImage> charImages;
    private final int cellWidth;
    private final int cellHeight;
    private final AlignmentInsensitiveShapeSimilarity aiss = new AlignmentInsensitiveShapeSimilarity();
    private final Random random = new Random();

    private static final int TOP_K = 5; // candidate chars per cell

    public Optimizer(final Map<Character, BufferedImage> charImages, final int cellWidth, final int cellHeight) {
        this.charImages = charImages;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public String optimize(final BufferedImage skeletonImage,
                           final String initialAscii,
                           final int maxIterations,
                           final double initialTemp,
                           final double coolingRate,
                           final int patience,
                           final int logEveryN) {

        final char[][] bestGrid = asciiToGrid(initialAscii);
        final char[][] currentGrid = deepCopyGrid(bestGrid);

        final int rows = bestGrid.length;
        final int cols = bestGrid[0].length;

        final Map<String, Double> scoreCache = new HashMap<>();

        final List<List<List<Character>>> candidateMap = buildCandidateList(skeletonImage, rows, cols);

        double bestScore = scoreGrid(bestGrid, skeletonImage, scoreCache);
        double currentScore = bestScore;

        double temp = initialTemp;
        int iteration = 0;
        int noImprovement = 0;

        while (iteration < maxIterations && noImprovement < patience && temp > 0.001) {

            final int ry = random.nextInt(rows);
            final int rx = random.nextInt(cols);

            final List<Character> candidates = candidateMap.get(ry).get(rx);
            final char oldChar = currentGrid[ry][rx];
            final char newChar = candidates.get(random.nextInt(Math.min(TOP_K, candidates.size())));

            if (newChar != oldChar) {
                currentGrid[ry][rx] = newChar;

                final double candidateScore = scoreGrid(currentGrid, skeletonImage, scoreCache);

                if (candidateScore < currentScore ||
                        random.nextDouble() < Math.exp((currentScore - candidateScore) / temp)) {

                    currentScore = candidateScore;
                    if (candidateScore < bestScore) {
                        bestScore = candidateScore;
                        copyGrid(currentGrid, bestGrid);
                        noImprovement = 0;
                    }
                } else {
                    currentGrid[ry][rx] = oldChar;
                    noImprovement++;
                }
            }

            if (logEveryN > 0 && iteration % logEveryN == 0) {
                System.out.println("iter=" + iteration + " temp=" + temp +
                        " best=" + bestScore + " curr=" + currentScore);
            }

            temp *= coolingRate;
            iteration++;
        }

        System.out.println("Optimization finished: iterations=" + iteration);
        return gridToAscii(bestGrid);
    }

    // --- Build top K candidates per cell ---
    private List<List<List<Character>>> buildCandidateList(final BufferedImage skeleton,
                                                           final int rows, final int cols) {
        final List<List<List<Character>>> result = new ArrayList<>();
        for (int y = 0; y < rows; y++) {
            result.add(new ArrayList<>());
            for (int x = 0; x < cols; x++) {
                final BufferedImage cell = skeleton.getSubimage(
                        x * cellWidth, y * cellHeight, cellWidth, cellHeight);

                final List<Map.Entry<Character, Double>> scores = new ArrayList<>();
                for (final Map.Entry<Character, BufferedImage> entry : charImages.entrySet()) {
                    final double score = aiss.computeSimilarity(cell, entry.getValue());
                    scores.add(Map.entry(entry.getKey(), score));
                }

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

    // --- Scoring with cache ---
    private double scoreGrid(final char[][] grid, final BufferedImage skeleton,
                             final Map<String, Double> scoreCache) {

        final String key = gridToAscii(grid);

        final Double cached = scoreCache.get(key);
        if (cached != null) return cached;

        final BufferedImage rendered = renderGridToImage(grid, skeleton.getWidth(), skeleton.getHeight());
        final double score = aiss.computeSimilarity(skeleton, rendered);

        scoreCache.put(key, score);
        return score;
    }

    private BufferedImage renderGridToImage(final char[][] grid, final int width, final int height) {
        final BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                final BufferedImage glyph = charImages.get(grid[y][x]);
                paste(out, glyph, x * cellWidth, y * cellHeight);
            }
        }
        return out;
    }

    private void paste(final BufferedImage base, final BufferedImage src, final int x, final int y) {
        for (int iy = 0; iy < src.getHeight(); iy++) {
            for (int ix = 0; ix < src.getWidth(); ix++) {
                base.setRGB(x + ix, y + iy, src.getRGB(ix, iy));
            }
        }
    }

    private static char[][] asciiToGrid(final String ascii) {
        final String[] lines = ascii.split("\n");
        final char[][] grid = new char[lines.length][lines[0].length()];
        for (int i = 0; i < lines.length; i++) grid[i] = lines[i].toCharArray();
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
