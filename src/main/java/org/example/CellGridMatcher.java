package org.example;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class CellGridMatcher {

    private final Map<Character, BufferedImage> charImages;
    private final AlignmentInsensitiveShapeSimilarity similarity;
    private final int cellWidth;
    private final int cellHeight;

    public CellGridMatcher(
            final Map<Character, BufferedImage> charImages,
            final int cellWidth,
            final int cellHeight,
            final AlignmentInsensitiveShapeSimilarity similarity
    ) {
        this.charImages = charImages;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.similarity = similarity;
    }

    public String match(final BufferedImage skeleton) {

        final int rows = skeleton.getHeight() / cellHeight;
        final int cols = skeleton.getWidth() / cellWidth;
        final StringBuilder sb = new StringBuilder();

        for (int y = 0; y < rows; y++) {

            for (int x = 0; x < cols; x++) {

                // Extract cell
                final BufferedImage cell = skeleton.getSubimage(
                        x * cellWidth,
                        y * cellHeight,
                        cellWidth,
                        cellHeight
                );

                char bestChar = ' ';
                double bestScore = Double.MAX_VALUE;

                for (Map.Entry<Character, BufferedImage> entry : charImages.entrySet()) {

                    final char glyph = entry.getKey();
                    final BufferedImage glyphImg = entry.getValue();

                    // Compute similarity score
                    final double score = similarity.computeSimilarity(cell, glyphImg);

                    if (score < bestScore) {
                        bestScore = score;
                        bestChar = glyph;
                    }
                }

                sb.append(bestChar);
            }

            sb.append('\n');
        }

        return sb.toString();
    }
}
