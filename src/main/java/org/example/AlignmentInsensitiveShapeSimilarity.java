package org.example;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class AlignmentInsensitiveShapeSimilarity {

    // Number of angular sectors in the histogram
    private static final int ANGLE_BINS = 12;

    // Radii used for multi-scale structural sampling (paper-inspired preset)
    private final int[] radii = new int[]{4, 8, 14};

    // Weights per radius (must sum to 1)
    private final double[] radiusWeights = new double[]{0.5, 0.3, 0.2};


    public double computeSimilarity(final BufferedImage a, final BufferedImage b) {

        double totalScore = 0.0;

        for (int i = 0; i < radii.length; i++) {

            final double[] hA = computeLogPolarHistogram(a, radii[i]);
            final double[] hB = computeLogPolarHistogram(b, radii[i]);

            normalize(hA);
            normalize(hB);

            totalScore += radiusWeights[i] * chiSquareDistance(hA, hB);
        }

        return totalScore;
    }


    private double[] computeLogPolarHistogram(final BufferedImage img, final int radius) {

        final double[] hist = new double[ANGLE_BINS];

        final int w = img.getWidth();
        final int h = img.getHeight();

        final int cx = w / 2;
        final int cy = h / 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                final boolean ink = (img.getRGB(x, y) & 0xFF) == 0;
                if (!ink) continue;

                final double dx = x - cx;
                final double dy = cy - y;

                final double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > radius) continue;

                final double angle = Math.atan2(dy, dx);
                int bin = (int) ((angle + Math.PI) / (2 * Math.PI) * ANGLE_BINS);

                if (bin < 0) bin = 0;
                if (bin >= ANGLE_BINS) bin = ANGLE_BINS - 1;

                hist[bin] += 1.0;
            }
        }

        return hist;
    }


    private void normalize(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        if (sum == 0) return;
        for (int i = 0; i < arr.length; i++)
            arr[i] /= sum;
    }


    private double chiSquareDistance(double[] h1, double[] h2) {
        double sum = 0;
        for (int i = 0; i < h1.length; i++) {
            final double a = h1[i];
            final double b = h2[i];
            if (a + b == 0) continue;
            sum += (a - b) * (a - b) / (a + b);
        }
        return sum;
    }
}
