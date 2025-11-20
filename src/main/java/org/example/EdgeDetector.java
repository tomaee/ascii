package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class EdgeDetector {

    public static BufferedImage resizeTo(final BufferedImage src, int w, int h) {
        final Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics2D g = out.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    public static BufferedImage toGrayscale(final BufferedImage img) {
        final BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        final Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    public static BufferedImage gaussianBlur(final BufferedImage img, int radius) {
        if (radius <= 0) return img;

        final int w = img.getWidth();
        final int h = img.getHeight();
        final BufferedImage out = new BufferedImage(w, h, img.getType());

        final int[][] kernel = new int[][] {
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };
        final int weight = 16;
        final int kOffset = 1;

        for (int y = kOffset; y < h - kOffset; y++) {
            for (int x = kOffset; x < w - kOffset; x++) {
                long sumGray = 0;

                for (int ky = -kOffset; ky <= kOffset; ky++) {
                    for (int kx = -kOffset; kx <= kOffset; kx++) {
                        int rgb = img.getRGB(x + kx, y + ky);
                        int gray = (rgb & 0xFF);

                        sumGray += gray * kernel[ky + kOffset][kx + kOffset];
                    }
                }

                int newGray = (int) (sumGray / weight);
                int newRgb = (0xFF << 24) | (newGray << 16) | (newGray << 8) | newGray;
                out.setRGB(x, y, newRgb);
            }
        }
        return out;
    }


    public static BufferedImage sobelEdge(final BufferedImage img, double threshold) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);

        final int[][] Gx = new int[][]{
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        final int[][] Gy = new int[][]{
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };

        final int kOffset = 1;
        int maxGradient = 0;
        final int[] magnitude = new int[w * h];

        for (int y = kOffset; y < h - kOffset; y++) {
            for (int x = kOffset; x < w - kOffset; x++) {
                int sumGx = 0;
                int sumGy = 0;

                for (int ky = -kOffset; ky <= kOffset; ky++) {
                    for (int kx = -kOffset; kx <= kOffset; kx++) {
                        // Get grayscale value (0-255)
                        int rgb = img.getRGB(x + kx, y + ky);
                        int gray = (rgb & 0xFF);

                        sumGx += gray * Gx[ky + kOffset][kx + kOffset];
                        sumGy += gray * Gy[ky + kOffset][kx + kOffset];
                    }
                }

                int mag = (int) Math.sqrt(sumGx * sumGx + sumGy * sumGy);

                magnitude[y * w + x] = mag;
                if (mag > maxGradient) {
                    maxGradient = mag;
                }
            }
        }

        if (maxGradient == 0) maxGradient = 1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                // Set border pixels to white (background)
                if (y < kOffset || y >= h - kOffset || x < kOffset || x >= w - kOffset) {
                    out.setRGB(x, y, 0xFFFFFFFF);
                    continue;
                }

                int mag = magnitude[y * w + x];

                double normalizedMag = (double) mag * 255.0 / maxGradient;

                 if (normalizedMag > threshold) {
                    out.setRGB(x, y, 0xFF000000);
                } else {
                    out.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }

        return out;
    }

    public static BufferedImage erode(final BufferedImage img) {
        final BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 1; y < img.getHeight() - 1; y++) {
            for (int x = 1; x < img.getWidth() - 1; x++) {
                boolean solid = true;
                for (int j = -1; j <= 1; j++)
                    for (int i = -1; i <= 1; i++)
                        if ((img.getRGB(x + i, y + j) & 0xFF) != 0) solid = false;

                out.setRGB(x, y, solid ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return out;
    }

}