package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class ImageUtils {
    public static BufferedImage resizeTo(final BufferedImage src, final int w, final int h) {
        final Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }
}
