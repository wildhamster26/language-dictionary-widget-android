package com.joinrestartabroad.glancedict;

import androidx.annotation.NonNull;

public class Category {
    public final long id;
    public final String name;
    public final int wordCount;
    /** True for the protected default category, identified by its English name rather than its (possibly translated) display name. */
    public final boolean isDefault;

    public Category(long id, String name, int wordCount, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.wordCount = wordCount;
        this.isDefault = isDefault;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
