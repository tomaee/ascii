package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class CharacterImageFactory {

    public static Map<Character, BufferedImage> generateAsciiSet(Font font, int width, int height) {
        final ConcurrentHashMap<Character, BufferedImage> tmp = new ConcurrentHashMap<>();

        IntStream.range(32, 127).parallel().forEach(i -> {
            final char c = (char) i;
            final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            final Graphics2D g = img.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);

                g.setColor(Color.BLACK);
                g.setFont(font);

                final FontMetrics metrics = g.getFontMetrics();
                final int x = (width - metrics.charWidth(c)) / 2;
                final int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

                g.drawString(String.valueOf(c), x, y);
            } finally {
                g.dispose();
            }
            tmp.put(c, img);
        });

        final LinkedHashMap<Character, BufferedImage> result = new LinkedHashMap<>();
        for (int i = 32; i < 127; i++) {
            result.put((char) i, tmp.get((char) i));
        }
        return result;
    }
}
