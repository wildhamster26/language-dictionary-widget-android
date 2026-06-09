package com.joinrestartabroad.glancedict;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
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

    public static class CategoryGroup {
        public final String name;
        public final List<Pair> pairs;

        public CategoryGroup(String name, List<Pair> pairs) {
            this.name = name;
            this.pairs = pairs;
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

    public static boolean isJson(String input) {
        if (input == null) return false;
        String trimmed = input.trim();
        return trimmed.startsWith("{");
    }

    public static List<CategoryGroup> parseJson(String input) {
        List<CategoryGroup> groups = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return groups;
        }
        try {
            JSONObject root = new JSONObject(input.trim());
            Iterator<String> categoryKeys = root.keys();
            while (categoryKeys.hasNext()) {
                String categoryName = categoryKeys.next();
                JSONObject wordsObj;
                try {
                    wordsObj = root.getJSONObject(categoryName);
                } catch (JSONException e) {
                    continue;
                }
                List<Pair> pairs = new ArrayList<>();
                Iterator<String> wordKeys = wordsObj.keys();
                while (wordKeys.hasNext()) {
                    String word = wordKeys.next();
                    String translation;
                    try {
                        translation = wordsObj.getString(word);
                    } catch (JSONException e) {
                        continue;
                    }
                    if (!word.trim().isEmpty() && !translation.trim().isEmpty()) {
                        pairs.add(new Pair(word.trim(), translation.trim()));
                    }
                }
                if (!pairs.isEmpty()) {
                    groups.add(new CategoryGroup(categoryName.trim(), pairs));
                }
            }
        } catch (JSONException ignored) {
        }
        return groups;
    }

    private static int findFirstDelimiter(String line) {
        int result = -1;
        char[] delimiters = new char[]{',', ':'};
        for (char delimiter : delimiters) {
            int index = line.indexOf(delimiter);
            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }
        return result;
    }
}
