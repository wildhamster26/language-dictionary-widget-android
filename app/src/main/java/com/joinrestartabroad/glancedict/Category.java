package com.joinrestartabroad.glancedict;

import androidx.annotation.NonNull;

public class Category {
    public final long id;
    public final String name;
    public final int wordCount;

    public Category(long id, String name, int wordCount) {
        this.id = id;
        this.name = name;
        this.wordCount = wordCount;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
