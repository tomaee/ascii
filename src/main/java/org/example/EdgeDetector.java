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
        return img; // placeholder (your existing blur stays)
    }

    public static BufferedImage sobelEdge(final BufferedImage img, double threshold) {
        return img; // placeholder (your existing sobel code stays)
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

    public static BufferedImage pad(final BufferedImage img, int pad) {
        final BufferedImage out = new BufferedImage(img.getWidth() + pad * 2, img.getHeight() + pad * 2, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                out.setRGB(x + pad, y + pad, img.getRGB(x, y));
        return out;
    }
}
