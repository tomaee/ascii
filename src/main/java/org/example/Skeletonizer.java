package org.example;

import java.awt.image.BufferedImage;

public final class Skeletonizer {

    public static BufferedImage thin(final BufferedImage binary) {
        final int w = binary.getWidth();
        final int h = binary.getHeight();

        final int[] img = new int[w * h];
        binary.getRGB(0, 0, w, h, img, 0, w);

        final int[][] bin = new int[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int idx = y * w + x;
                bin[y][x] = ((img[idx] & 0xFF) == 0) ? 1 : 0;
            }
        }

        boolean changed;
        do {
            changed = false;
            final java.util.List<int[]> toRemove = new java.util.ArrayList<>();

            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {

                    if (bin[y][x] != 1) continue;

                    final int[] n = neighbors(bin, x, y);
                    final int count = sum(n);

                    if (count < 2 || count > 6) continue;    // 1. O pixel deve ter entre 2 e 6 vizinhos de tinta (preserva pontas e junções complexas).
                    if (transitions(n) != 1) continue;       // 2. A remoção não deve quebrar a conectividade da linha (deve haver apenas 1 transição 0->1).
                    if (n[0] * n[2] * n[4] != 0) continue;   // 3. Pelo menos um dos vizinhos N, E ou S deve ser branco (protege linhas de 1 pixel que se estendem para o Sul).
                    if (n[2] * n[4] * n[6] != 0) continue;   // 4. Pelo menos um dos vizinhos E, S ou O deve ser branco (protege linhas de 1 pixel que se estendem para o Leste).

                    toRemove.add(new int[]{y, x});
                }
            }

            for (final int[] coords : toRemove) bin[coords[0]][coords[1]] = 0;
            changed |= !toRemove.isEmpty();
            toRemove.clear();

            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {

                    if (bin[y][x] != 1) continue;

                    final int[] n = neighbors(bin, x, y);
                    final int count = sum(n);

                    if (count < 2 || count > 6) continue;    // 1. O pixel deve ter entre 2 e 6 vizinhos de tinta (preserva pontas e junções complexas).
                    if (transitions(n) != 1) continue;       // 2. A remoção não deve quebrar a conectividade da linha (deve haver apenas 1 transição 0->1).
                    if (n[0] * n[2] * n[6] != 0) continue;   // 3. Pelo menos um dos vizinhos N, E ou O deve ser branco (protege linhas de 1 pixel que se estendem para o Norte).
                    if (n[0] * n[4] * n[6] != 0) continue;   // 4. Pelo menos um dos vizinhos N, S ou O deve ser branco (protege linhas de 1 pixel que se estendem para o Oeste).

                    toRemove.add(new int[]{y, x});
                }
            }

            for (final int[] coords : toRemove) bin[coords[0]][coords[1]] = 0;
            changed |= !toRemove.isEmpty();

        } while (changed);

        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int v = bin[y][x] == 1 ? 0xFF000000 : 0xFFFFFFFF;
                out.setRGB(x, y, v);
            }
        }

        return out;
    }

    private static int[] neighbors(final int[][] bin, final int x, final int y) {
        return new int[] {
                bin[y - 1][x],          // P2 (Topo)
                bin[y - 1][x + 1],      // P3 (Topo-Direita)
                bin[y][x + 1],          // P4 (Direita)
                bin[y + 1][x + 1],      // P5 (Base-Direita)
                bin[y + 1][x],          // P6 (Base)
                bin[y + 1][x - 1],      // P7 (Base-Esquerda)
                bin[y][x - 1],          // P8 (Esquerda)
                bin[y - 1][x - 1]       // P9 (Topo-Esquerda)
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