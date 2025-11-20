package org.example;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class CellGridMatcherOutline {

    private final Map<Character, BufferedImage> charImages;
    private final int cellWidth;
    private final int cellHeight;

    private final AlignmentInsensitiveShapeSimilarity aiss = new AlignmentInsensitiveShapeSimilarity();

    private enum StrokeType { EMPTY, VERTICAL, HORIZONTAL, DIAG1, DIAG2, JUNCTION }

    private final Map<StrokeType, char[]> allowedChars = Map.of(
            StrokeType.EMPTY, new char[]{' '},
            StrokeType.VERTICAL, new char[]{'|', '!', 'I', 'l'},
            StrokeType.HORIZONTAL, new char[]{'-', '_', '=', '~'},
            StrokeType.DIAG1, new char[]{'/', 'Y', '7'},
            StrokeType.DIAG2, new char[]{'\\', 'L', 'J'},
            StrokeType.JUNCTION, new char[]{'+', '*', '#', 'X'}
    );

    // Multi-scale configuration (scales and weights must sum to 1)
    private final double[] scales = new double[]{0.6, 1.0, 1.6};
    private final double[] weights = new double[]{0.3, 0.5, 0.2};

    public CellGridMatcherOutline(final Map<Character, BufferedImage> charImages,
                                  final int cellWidth, final int cellHeight) {
        this.charImages = new HashMap<>(charImages);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        if (scales.length != weights.length) {
            throw new IllegalArgumentException("scales and weights length must match");
        }
    }

    public String matchToGrid(final BufferedImage skeleton) {

        final int rows = skeleton.getHeight() / cellHeight;
        final int cols = skeleton.getWidth() / cellWidth;

        final StringBuilder result = new StringBuilder();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                // hard border rule (1-cell wide border restored)
                if (x == 0 || y == 0 || x == cols - 1 || y == rows - 1) {
                    result.append(' ');
                    continue;
                }

                final BufferedImage cell = skeleton.getSubimage(
                        x * cellWidth, y * cellHeight, cellWidth, cellHeight
                );

                final int ink = countInk(cell);
                if (ink < 2) { // relaxed for thin lines
                    result.append(' ');
                    continue;
                }

                final StrokeType type = classifyStroke(cell);
                final char[] pool = allowedChars.get(type);

                char best = ' ';
                double bestScore = Double.MAX_VALUE;

                for (char ch : pool) {
                    final BufferedImage glyph = charImages.get(ch);
                    if (glyph == null) continue;

                    final double score = computeMultiScaleScore(cell, glyph);
                    if (score < bestScore) {
                        bestScore = score;
                        best = ch;
                    }
                }

                result.append(best);
            }
            result.append("\n");
        }

        return result.toString();
    }

    // ------------------------
    // multi-scale scoring
    // ------------------------
    private double computeMultiScaleScore(final BufferedImage cell, final BufferedImage glyph) {
        double sum = 0.0;
        for (int i = 0; i < scales.length; i++) {
            final double s = scales[i];
            final double w = weights[i];

            final int scaledW = Math.max(4, (int) Math.round(cellWidth * s));
            final int scaledH = Math.max(4, (int) Math.round(cellHeight * s));

            final BufferedImage cellScaled = ImageUtils.resizeTo(cell, scaledW, scaledH);
            final BufferedImage glyphScaled = ImageUtils.resizeTo(glyph, scaledW, scaledH);

            final double sc = aiss.computeSimilarity(cellScaled, glyphScaled);
            sum += w * sc;
        }
        return sum;
    }

    // ------------------------
    // helpers
    // ------------------------
    private int countInk(final BufferedImage img) {
        int c = 0;
        final int h = img.getHeight();
        final int w = img.getWidth();
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                if ((img.getRGB(xx, yy) & 0xFF) == 0) c++;
            }
        }
        return c;
    }

    private StrokeType classifyStroke(final BufferedImage img) {

        int vertical = 0, horizontal = 0, diag1 = 0, diag2 = 0;
        final int h = img.getHeight();
        final int w = img.getWidth();

        for (int y = 1; y < h; y++) {
            for (int x = 1; x < w; x++) {
                final boolean pix = (img.getRGB(x, y) & 0xFF) == 0;
                if (!pix) continue;

                if ((img.getRGB(x, y - 1) & 0xFF) == 0) vertical++;
                if ((img.getRGB(x - 1, y) & 0xFF) == 0) horizontal++;
                if ((img.getRGB(x - 1, y - 1) & 0xFF) == 0) diag1++;
                if (x < w - 1 && (img.getRGB(x + 1, y - 1) & 0xFF) == 0) diag2++;
            }
        }

        if (vertical >= horizontal && vertical >= diag1 && vertical >= diag2) return StrokeType.VERTICAL;
        if (horizontal >= vertical && horizontal >= diag1 && horizontal >= diag2) return StrokeType.HORIZONTAL;
        if (diag1 >= diag2) return StrokeType.DIAG1;
        return StrokeType.DIAG2;
    }
}