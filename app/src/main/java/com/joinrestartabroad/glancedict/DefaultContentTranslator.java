package com.joinrestartabroad.glancedict;

import android.os.Handler;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Re-translates the shipped default words and category names when the user changes their
 * languages. The original English text stored on each default row ({@code default_native_en} /
 * {@code default_name_en}) is the single source of truth, so translations are always
 * English &rarr; chosen language and never chain through intermediate languages.
 *
 * <p>Work runs on the caller's executor; the result is posted to the caller's main-thread
 * handler. The translation is all-or-nothing: only when every string succeeds is the new
 * text committed, otherwise the existing default text is left untouched (the caller then
 * surfaces a message, per the "keep current + notify" behaviour).
 */
public final class DefaultContentTranslator {

    public enum Result { SUCCESS, UNSUPPORTED, FAILED }

    public interface Callback {
        void onComplete(Result result);
    }

    private static final String ENGLISH = "en";
    private static final long TIMEOUT_MS = 60_000L;

    private DefaultContentTranslator() {
    }

    /**
     * Re-translates the native side of default words and the default category names into
     * {@code nativeLang}. The translated side and romanization are left unchanged.
     */
    public static void applyNativeLanguage(DictionaryDbHelper db, String nativeLang,
                                           ExecutorService executor, Handler mainHandler, Callback callback) {
        executor.execute(() -> {
            List<DictionaryDbHelper.DefaultWord> words = db.getRetranslatableDefaultWords();
            List<DictionaryDbHelper.DefaultCategory> categories = db.getDefaultCategories();

            Map<Long, String> wordNative = new HashMap<>();
            Map<Long, String> categoryNames = new HashMap<>();

            if (ENGLISH.equalsIgnoreCase(nativeLang)) {
                // Restore the canonical English directly; no model needed.
                for (DictionaryDbHelper.DefaultWord word : words) {
                    wordNative.put(word.id, word.englishNative);
                }
                for (DictionaryDbHelper.DefaultCategory category : categories) {
                    categoryNames.put(category.id, category.englishName);
                }
                db.commitNativeTranslations(wordNative, categoryNames);
                post(mainHandler, callback, Result.SUCCESS);
                return;
            }

            try (Translator translator = createTranslator(nativeLang)) {
                if (translator == null) {
                    post(mainHandler, callback, Result.UNSUPPORTED);
                    return;
                }
                Tasks.await(translator.downloadModelIfNeeded(), TIMEOUT_MS, TimeUnit.MILLISECONDS);
                for (DictionaryDbHelper.DefaultWord word : words) {
                    wordNative.put(word.id, translate(translator, word.englishNative));
                }
                for (DictionaryDbHelper.DefaultCategory category : categories) {
                    categoryNames.put(category.id, translate(translator, category.englishName));
                }
            } catch (Exception e) {
                post(mainHandler, callback, Result.FAILED);
                return;
            }

            db.commitNativeTranslations(wordNative, categoryNames);
            post(mainHandler, callback, Result.SUCCESS);
        });
    }

    /**
     * Re-translates the translated side (and recomputes romanization) of default words into
     * {@code targetLang}. The native side and category names are left unchanged.
     */
    public static void applyTargetLanguage(DictionaryDbHelper db, String targetLang,
                                           ExecutorService executor, Handler mainHandler, Callback callback) {
        executor.execute(() -> {
            List<DictionaryDbHelper.DefaultWord> words = db.getRetranslatableDefaultWords();

            Map<Long, String> translated = new HashMap<>();
            Map<Long, String> romanization = new HashMap<>();

            try (Translator translator = createTranslator(targetLang)) {
                if (translator == null) {
                    post(mainHandler, callback, Result.UNSUPPORTED);
                    return;
                }
                Tasks.await(translator.downloadModelIfNeeded(), TIMEOUT_MS, TimeUnit.MILLISECONDS);
                for (DictionaryDbHelper.DefaultWord word : words) {
                    String value = translate(translator, word.englishNative);
                    translated.put(word.id, value);
                    romanization.put(word.id, Romanizer.romanize(value, targetLang));
                }
            } catch (Exception e) {
                post(mainHandler, callback, Result.FAILED);
                return;
            }

            db.commitTargetTranslations(translated, romanization);
            post(mainHandler, callback, Result.SUCCESS);
        });
    }

    private static Translator createTranslator(String targetLang) {
        try {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(ENGLISH)
                    .setTargetLanguage(targetLang)
                    .build();
            return Translation.getClient(options);
        } catch (RuntimeException e) {
            // Unsupported language pair (IllegalArgumentException) or client init failure.
            return null;
        }
    }

    private static String translate(Translator translator, String text) throws Exception {
        return Tasks.await(translator.translate(text), TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private static void post(Handler mainHandler, Callback callback, Result result) {
        mainHandler.post(() -> callback.onComplete(result));
    }
}
