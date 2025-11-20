package org.example;

public final class StrokeGroups {
    public static int getGroup(char c) {
        return switch (c) {
            case '|', '!', 'I', 'l' -> 1; // vertical
            case '-', '_', '=', '~' -> 2; // horizontal
            case '/', 'Y', '7' -> 3;      // diag up-right
            case '\\', 'L', 'J' -> 4;     // diag up-left
            case '+', '*', '#', 'X' -> 5; // junction
            default -> 0;                 // empty or unknown
        };
    }
}
