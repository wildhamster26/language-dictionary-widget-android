package com.example.glancedict;

public class Category {
    public final long id;
    public final String name;

    public Category(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
