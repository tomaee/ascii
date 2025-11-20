package org.example;

public final class ASCIISmoother {

    // How aggressively smoothing should override a cell
    // 0.30 = strong smoothing, 0.50 = balanced, 0.65 = light smoothing, 0.85 = almost no smoothing
    private static final double SMOOTHING_WEIGHT = 0.65;

    public static String smooth(String ascii) {

        final String[] lines = ascii.split("\n");
        final int rows = lines.length;
        final int cols = lines[0].length();

        final char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) grid[r] = lines[r].toCharArray();

        final char[][] out = new char[rows][cols];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                final char center = grid[y][x];
                final int centerGroup = StrokeGroups.getGroup(center);

                // Empty or undefined stays as is
                if (centerGroup == 0) {
                    out[y][x] = ' ';
                    continue;
                }

                int[] groupCounts = new int[6];
                int neighborTotal = 0;

                // Count neighbors
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {

                        if (dx == 0 && dy == 0) continue;

                        final int ny = y + dy;
                        final int nx = x + dx;

                        if (ny < 0 || nx < 0 || ny >= rows || nx >= cols)
                            continue;

                        final char neighbor = grid[ny][nx];
                        final int group = StrokeGroups.getGroup(neighbor);

                        if (group > 0) {
                            groupCounts[group]++;
                            neighborTotal++;
                        }
                    }
                }

                // Find most frequent neighbor stroke type
                int bestGroup = centerGroup;
                int bestScore = groupCounts[centerGroup];

                for (int g = 1; g <= 5; g++) {
                    if (groupCounts[g] > bestScore) {
                        bestScore = groupCounts[g];
                        bestGroup = g;
                    }
                }

                boolean replace = false;

                // Only override if almost all neighbors agree against center
                if (bestGroup != centerGroup && neighborTotal > 0) {

                    final double dominance = (double) bestScore / neighborTotal;

                    if (dominance >= SMOOTHING_WEIGHT) {
                        replace = true;
                    }
                }

                out[y][x] = replace ? representativeSymbol(bestGroup) : center;
            }
        }

        final StringBuilder result = new StringBuilder();
        for (char[] row : out) result.append(row).append("\n");
        return result.toString();
    }

    private static char representativeSymbol(int group) {
        return switch (group) {
            case 1 -> '|';
            case 2 -> '-';
            case 3 -> '/';
            case 4 -> '\\';
            case 5 -> '+';
            default -> ' ';
        };
    }
}
