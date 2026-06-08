package com.example.glancedict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DictionaryDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "dictionary.db";
    public static final int DATABASE_VERSION = 2;
    public static final String DEFAULT_CATEGORY = "Uncategorized";

    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_WORDS = "words";

    private final Context mContext;

    public DictionaryDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "display_order INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE words (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_id INTEGER NOT NULL, " +
                "native_word TEXT NOT NULL, " +
                "translated_word TEXT NOT NULL, " +
                "date_added INTEGER NOT NULL, " +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_words_category_added ON words(category_id, date_added DESC)");

        ContentValues values = new ContentValues();
        values.put("name", DEFAULT_CATEGORY);
        db.insert(TABLE_CATEGORIES, null, values);

        loadInitialData(db);
    }

    private void loadInitialData(SQLiteDatabase db) {
        try (InputStream is = mContext.getAssets().open("initial_data.json")) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String json = result.toString(StandardCharsets.UTF_8.name());

            List<BulkWordParser.CategoryGroup> groups = BulkWordParser.parseJson(json);
            long now = System.currentTimeMillis();

            for (BulkWordParser.CategoryGroup group : groups) {
                ContentValues catValues = new ContentValues();
                catValues.put("name", group.name);
                long catId = db.insert(TABLE_CATEGORIES, null, catValues);

                if (catId != -1) {
                    for (BulkWordParser.Pair pair : group.pairs) {
                        ContentValues wordValues = new ContentValues();
                        wordValues.put("category_id", catId);
                        wordValues.put("native_word", pair.nativeWord);
                        wordValues.put("translated_word", pair.translatedWord);
                        wordValues.put("date_added", now);
                        db.insert(TABLE_WORDS, null, wordValues);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE categories ADD COLUMN display_order INTEGER DEFAULT 0");
        }
    }

    public long ensureDefaultCategory() {
        Long existing = findCategoryId(DEFAULT_CATEGORY);
        if (existing != null) {
            return existing;
        }
        return createCategory(DEFAULT_CATEGORY);
    }

    public long createCategory(String name) {
        String normalized = normalize(name);
        if (normalized.isEmpty()) {
            return -1L;
        }

        Long existing = findCategoryId(normalized);
        if (existing != null) {
            return existing;
        }

        ContentValues values = new ContentValues();
        values.put("name", normalized);
        return getWritableDatabase().insert(TABLE_CATEGORIES, null, values);
    }

    public List<Category> getCategories() {
        ensureDefaultCategory();
        List<Category> categories = new ArrayList<>();
        String query = "SELECT id, name, " +
                "(SELECT COUNT(*) FROM words WHERE words.category_id = categories.id) as word_count " +
                "FROM categories " +
                "ORDER BY CASE WHEN name = '" + DEFAULT_CATEGORY + "' THEN 0 ELSE 1 END, display_order ASC, name COLLATE NOCASE ASC";

        try (Cursor cursor = getReadableDatabase().rawQuery(query, null)) {
            int idIndex = cursor.getColumnIndexOrThrow("id");
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            int countIndex = cursor.getColumnIndexOrThrow("word_count");
            while (cursor.moveToNext()) {
                categories.add(new Category(
                        cursor.getLong(idIndex),
                        cursor.getString(nameIndex),
                        cursor.getInt(countIndex)));
            }
        }
        return categories;
    }

    public Word getWord(long wordId) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_WORDS,
                new String[]{"id", "category_id", "native_word", "translated_word"},
                "id = ?",
                new String[]{"" + wordId},
                null,
                null,
                null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return readWord(cursor);
        }
    }

    public long addWord(long categoryId, String nativeWord, String translatedWord) {
        String nativeValue = normalize(nativeWord);
        String translatedValue = normalize(translatedWord);
        if (nativeValue.isEmpty() || translatedValue.isEmpty()) {
            return -1L;
        }

        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("native_word", nativeValue);
        values.put("translated_word", translatedValue);
        values.put("date_added", System.currentTimeMillis());
        return getWritableDatabase().insert(TABLE_WORDS, null, values);
    }

    public int addWords(long categoryId, List<BulkWordParser.Pair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return 0;
        }

        SQLiteDatabase db = getWritableDatabase();
        int saved = 0;
        db.beginTransaction();
        try {
            for (BulkWordParser.Pair pair : pairs) {
                String nativeValue = normalize(pair.nativeWord);
                String translatedValue = normalize(pair.translatedWord);
                if (nativeValue.isEmpty() || translatedValue.isEmpty()) {
                    continue;
                }

                ContentValues values = new ContentValues();
                values.put("category_id", categoryId);
                values.put("native_word", nativeValue);
                values.put("translated_word", translatedValue);
                values.put("date_added", System.currentTimeMillis());
                if (db.insert(TABLE_WORDS, null, values) > 0) {
                    saved++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return saved;
    }

    public void updateWord(long wordId, long categoryId, String nativeWord, String translatedWord) {
        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("native_word", normalize(nativeWord));
        values.put("translated_word", normalize(translatedWord));
        getWritableDatabase().update(TABLE_WORDS, values, "id = ?", new String[]{"" + wordId});
    }

    public void deleteWord(long wordId) {
        getWritableDatabase().delete(TABLE_WORDS, "id = ?", new String[]{"" + wordId});
    }

    public int getWordCountForCategory(long categoryId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM words WHERE category_id = ?",
                new String[]{"" + categoryId})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void deleteCategoryAndWords(long categoryId) {
        long defaultId = ensureDefaultCategory();
        if (categoryId == defaultId) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_WORDS, "category_id = ?", new String[]{"" + categoryId});
            db.delete(TABLE_CATEGORIES, "id = ?", new String[]{"" + categoryId});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateCategoryOrder(List<Long> categoryIds) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int i = 0;
            while (i < categoryIds.size()) {
                ContentValues values = new ContentValues();
                values.put("display_order", i);
                db.update(TABLE_CATEGORIES, values, "id = ?", new String[]{"" + categoryIds.get(i)});
                i++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<WidgetItem> getWidgetItems(Set<Long> activeCategoryIds) {
        List<WidgetItem> items = new ArrayList<>();
        Set<Long> active = activeCategoryIds == null ? null : new HashSet<>(activeCategoryIds);
        for (Category category : getCategories()) {
            if (active != null && !active.contains(category.id)) {
                continue;
            }

            List<Word> words = getWordsForCategory(category.id);
            if (words.isEmpty()) {
                continue;
            }

            items.add(WidgetItem.category(category.id, category.name));
            for (Word word : words) {
                items.add(WidgetItem.word(word));
            }
        }
        return items;
    }

    public String getLongestActiveText(Set<Long> activeCategoryIds) {
        String longest = "";
        String longestNative = queryLongestWordColumn("native_word", activeCategoryIds);
        String longestTrans = queryLongestWordColumn("translated_word", activeCategoryIds);

        longest = longer(longest, longestNative);
        longest = longer(longest, longestTrans);
        
        // If no words yet, use a reasonable placeholder to prevent 0-width calculation
        if (longest.isEmpty()) {
            longest = DEFAULT_CATEGORY;
        }
        return longest;
    }

    public void refreshLongestTextCache(Context context) {
        DictionaryPrefs.setLongestTextCache(
                context,
                getLongestActiveText(DictionaryPrefs.getActiveCategoryIds(context)));
    }

    private List<Word> getWordsForCategory(long categoryId) {
        List<Word> words = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_WORDS,
                new String[]{"id", "category_id", "native_word", "translated_word"},
                "category_id = ?",
                new String[]{"" + categoryId},
                null,
                null,
                "date_added DESC, id DESC")) {
            while (cursor.moveToNext()) {
                words.add(readWord(cursor));
            }
        }
        return words;
    }

    private Long findCategoryId(String name) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_CATEGORIES,
                new String[]{"id"},
                "name = ? COLLATE NOCASE",
                new String[]{normalize(name)},
                null,
                null,
                null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : null;
        }
    }

    private Word readWord(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        long categoryId = cursor.getLong(cursor.getColumnIndexOrThrow("category_id"));
        String nativeWord = cursor.getString(cursor.getColumnIndexOrThrow("native_word"));
        String translatedWord = cursor.getString(cursor.getColumnIndexOrThrow("translated_word"));
        return new Word(id, categoryId, nativeWord, translatedWord);
    }

    private String queryLongestWordColumn(String column, Set<Long> activeCategoryIds) {
        List<String> args = new ArrayList<>();
        String where = activeFilter(activeCategoryIds, args);
        return queryLongestText(column, where, args);
    }

    private String queryLongestText(String column, String where, List<String> args) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(column)
                .append(" FROM ").append(TABLE_WORDS);
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(where);
        }
        sql.append(" ORDER BY LENGTH(").append(column).append(") DESC LIMIT 1");

        try (Cursor cursor = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]))) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    private String activeFilter(Set<Long> activeCategoryIds, List<String> args) {
        if (activeCategoryIds == null) {
            return "";
        }
        if (activeCategoryIds.isEmpty()) {
            return "0";
        }

        StringBuilder filter = new StringBuilder("category_id").append(" IN (");
        int index = 0;
        for (Long id : activeCategoryIds) {
            if (index > 0) {
                filter.append(",");
            }
            filter.append("?");
            args.add("" + id);
            index++;
        }
        filter.append(")");
        return filter.toString();
    }

    private String longer(String current, String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return current;
        }
        String trimmedCandidate = candidate.trim();
        return trimmedCandidate.length() > current.length() ? trimmedCandidate : current;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
