package org.example;

import java.awt.image.BufferedImage;

public final class Skeletonizer {

    public static BufferedImage thin(final BufferedImage binary) {
        final int w = binary.getWidth();
        final int h = binary.getHeight();
        final int[] img = new int[w * h];
        binary.getRGB(0, 0, w, h, img, 0, w);

        final int[] bin = new int[w * h];
        for (int i = 0; i < img.length; i++) bin[i] = ((img[i] & 0xFF) == 0) ? 1 : 0;

        boolean changed;
        do {
            changed = false;
            final java.util.List<Integer> toRemove = new java.util.ArrayList<>();

            // Step 1
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    final int idx = y * w + x;
                    if (bin[idx] != 1) continue;

                    final int[] n = neighbors(bin, w, x, y);
                    final int count = sum(n);

                    if (count < 2 || count > 6) continue;
                    if (transitions(n) != 1) continue;
                    if (n[0] * n[2] * n[4] != 0) continue;
                    if (n[2] * n[4] * n[6] != 0) continue;

                    toRemove.add(idx);
                }
            }

            for (final int idx : toRemove) bin[idx] = 0;
            changed |= !toRemove.isEmpty();
            toRemove.clear();

            // Step 2
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    final int idx = y * w + x;
                    if (bin[idx] != 1) continue;

                    final int[] n = neighbors(bin, w, x, y);
                    final int count = sum(n);

                    if (count < 2 || count > 6) continue;
                    if (transitions(n) != 1) continue;
                    if (n[0] * n[2] * n[6] != 0) continue;
                    if (n[0] * n[4] * n[6] != 0) continue;

                    toRemove.add(idx);
                }
            }

            for (final int idx : toRemove) bin[idx] = 0;
            changed |= !toRemove.isEmpty();

        } while (changed);

        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int v = bin[y * w + x] == 1 ? 0xFF000000 : 0xFFFFFFFF;
                out.setRGB(x, y, v);
            }
        }

        return out;
    }

    private static int[] neighbors(final int[] bin, final int w, final int x, final int y) {
        return new int[] {
                bin[(y - 1) * w + x],
                bin[(y - 1) * w + (x + 1)],
                bin[y * w + (x + 1)],
                bin[(y + 1) * w + (x + 1)],
                bin[(y + 1) * w + x],
                bin[(y + 1) * w + (x - 1)],
                bin[y * w + (x - 1)],
                bin[(y - 1) * w + (x - 1)]
        };
    }

    private static int sum(final int[] arr) {
        int s = 0;
        for (final int v : arr) s += v;
        return s;
    }

    private static int transitions(final int[] n) {
        int t = 0;
        for (int i = 0; i < n.length; i++) {
            if (n[i] == 0 && n[(i + 1) % n.length] == 1) t++;
        }
        return t;
    }
}
