package com.example.glancedict;

public class Category {
    public final long id;
    public final String name;
    public final int wordCount;

    public Category(long id, String name, int wordCount) {
        this.id = id;
        this.name = name;
        this.wordCount = wordCount;
    }

    @Override
    public String toString() {
        return name;
    }
}
