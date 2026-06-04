package com.example.glancedict;

public class Word {
    public final long id;
    public final long categoryId;
    public final String nativeWord;
    public final String translatedWord;

    public Word(long id, long categoryId, String nativeWord, String translatedWord) {
        this.id = id;
        this.categoryId = categoryId;
        this.nativeWord = nativeWord;
        this.translatedWord = translatedWord;
    }
}
