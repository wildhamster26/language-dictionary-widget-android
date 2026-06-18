package com.joinrestartabroad.glancedict;

public class WidgetItem {
    public final boolean categoryHeader;
    public final long wordId;
    public final long categoryId;
    public final String primary;
    public final String secondary;
    public final String tertiary;

    private WidgetItem(boolean categoryHeader, long wordId, long categoryId,
                       String primary, String secondary, String tertiary) {
        this.categoryHeader = categoryHeader;
        this.wordId = wordId;
        this.categoryId = categoryId;
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
    }

    public static WidgetItem category(long id, String name) {
        return new WidgetItem(true, -1L, id, name, "", "");
    }

    public static WidgetItem word(Word word) {
        return new WidgetItem(false, word.id, -1L, word.nativeWord, word.translatedWord, word.romanization);
    }
}
