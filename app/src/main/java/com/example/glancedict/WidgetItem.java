package com.example.glancedict;

public class WidgetItem {
    public final boolean categoryHeader;
    public final long wordId;
    public final String primary;
    public final String secondary;

    private WidgetItem(boolean categoryHeader, long wordId, String primary, String secondary) {
        this.categoryHeader = categoryHeader;
        this.wordId = wordId;
        this.primary = primary;
        this.secondary = secondary;
    }

    public static WidgetItem category(String name) {
        return new WidgetItem(true, -1L, name, "");
    }

    public static WidgetItem word(Word word) {
        return new WidgetItem(false, word.id, word.nativeWord, word.translatedWord);
    }
}
