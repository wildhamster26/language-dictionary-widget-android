package com.example.glancedict;

import java.util.ArrayList;
import java.util.List;

public final class BulkWordParser {
    public static class Pair {
        public final String nativeWord;
        public final String translatedWord;

        public Pair(String nativeWord, String translatedWord) {
            this.nativeWord = nativeWord;
            this.translatedWord = translatedWord;
        }
    }

    private BulkWordParser() {
    }

    public static List<Pair> parse(String input) {
        List<Pair> pairs = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return pairs;
        }

        String[] lines = input.split("\\r?\\n");
        for (String line : lines) {
            Pair pair = parseLine(line);
            if (pair != null) {
                pairs.add(pair);
            }
        }
        return pairs;
    }

    private static Pair parseLine(String line) {
        if (line == null) {
            return null;
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        int delimiterIndex = findFirstDelimiter(trimmed);
        if (delimiterIndex < 0) {
            return null;
        }

        String nativeWord = trimmed.substring(0, delimiterIndex).trim();
        String translatedWord = trimmed.substring(delimiterIndex + 1).trim();
        if (nativeWord.isEmpty() || translatedWord.isEmpty()) {
            return null;
        }
        return new Pair(nativeWord, translatedWord);
    }

    private static int findFirstDelimiter(String line) {
        int result = -1;
        char[] delimiters = new char[]{',', '-', ':'};
        for (char delimiter : delimiters) {
            int index = line.indexOf(delimiter);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }
}
