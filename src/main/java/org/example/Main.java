package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

public final class Main {

    public static void main(final String[] args) throws Exception {

        System.out.println("Working directory: " + System.getProperty("user.dir"));

        final File input = new File("./images/bicho.jpg");

        if (!input.exists() || !input.canRead()) {
            System.err.println("Cannot read: " + input.getPath());
            System.exit(1);
        }

        final BufferedImage original = ImageIO.read(input);

        final int cellHeight = 8;
        final int cellWidth = 8;

        final int w = (original.getWidth() / cellWidth) * cellWidth;
        final int h = (original.getHeight() / cellHeight) * cellHeight;
        BufferedImage img = EdgeDetector.resizeTo(original, w, h);

        img = EdgeDetector.toGrayscale(img);
        img = EdgeDetector.gaussianBlur(img, 1);
        img = EdgeDetector.sobelEdge(img, 50.0);

        final BufferedImage skeleton = Skeletonizer.thin(img);

        final Font font = new Font("Courier New", Font.PLAIN, 16);
        final Map<Character, BufferedImage> chars =
                CharacterImageFactory.generateAsciiSet(font, cellWidth, cellHeight);

        final CellGridMatcherOutline matcher = new CellGridMatcherOutline(chars, cellWidth, cellHeight);
        String ascii = matcher.matchToGrid(skeleton);
        ascii = ASCIISmoother.smooth(ascii);

        System.out.println("\n===== ASCII OUTPUT =====\n");
        System.out.println(ascii);

        java.nio.file.Files.writeString(new File("ascii_output.txt").toPath(), ascii);
        System.out.println("\n💾 Saved to ascii_output.txt");
    }

    public static String stretchAscii(String ascii, int factor) {
        StringBuilder out = new StringBuilder();
        for (String line : ascii.split("\n")) {
            for (char c : line.toCharArray()) {
                out.append(String.valueOf(c).repeat(Math.max(1, factor)));
            }
            out.append("\n");
        }
        return out.toString();
    }
}