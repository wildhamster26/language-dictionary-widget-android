package com.joinrestartabroad.glancedict;

public class Word {
    public final long id;
    public final long categoryId;
    public final String nativeWord;
    public final String translatedWord;
    public final String romanization;

    public Word(long id, long categoryId, String nativeWord, String translatedWord, String romanization) {
        this.id = id;
        this.categoryId = categoryId;
        this.nativeWord = nativeWord;
        this.translatedWord = translatedWord;
        this.romanization = romanization == null ? "" : romanization;
    }
}
