package com.joinrestartabroad.glancedict;

import android.os.Build;

import java.util.Locale;

/**
 * Produces a Latin-script reading (pinyin, Revised Romanization, etc.) for text in a
 * non-Latin script, using the ICU transliterator bundled with Android (API 29+). No added
 * dependency and fully offline, matching the app's translation model.
 * <p>
 * Quality notes: Chinese ({@code Han-Latin}) gives tone-marked pinyin but picks the most common
 * reading for polyphonic characters; Korean ({@code Hangul-Latin}) is algorithmic and reliable;
 * other non-Latin scripts fall back to {@code Any-Latin}. Japanese is intentionally unsupported
 * for now because ICU cannot derive kanji readings (that needs a morphological dictionary).
 */
public final class Romanizer {
    private Romanizer() {
    }

    /**
     * @return the romanization of {@code text} for the given BCP-47 language, or "" when the
     * language is unsupported, ICU is unavailable (API < 24), or the result adds nothing
     * (text was already Latin script).
     */
    public static String romanize(String text, String languageTag) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "";
        }
        String id = transliteratorId(languageTag);
        if (id == null) {
            return "";
        }
        try {
            android.icu.text.Transliterator transliterator =
                    android.icu.text.Transliterator.getInstance(id);
            String result = transliterator.transliterate(text);
            if (result == null) {
                return "";
            }
            result = result.trim();
            // Latin-script input passes through unchanged; drop it so we don't echo the word.
            if (result.equalsIgnoreCase(text.trim())) {
                return "";
            }
            return result;
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Whether a non-empty romanization can be produced for the given language on this device. */
    public static boolean isSupported(String languageTag) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && transliteratorId(languageTag) != null;
    }

    private static String transliteratorId(String languageTag) {
        String lang = baseLanguage(languageTag);
        if (lang == null || lang.equals("ja")) {
            // Japanese excluded: ICU romanizes kana but renders kanji as Chinese pinyin, which
            // is wrong. Proper support needs a morphological analyzer (e.g. Kuromoji).
            return null;
        }
        switch (lang) {
            case "zh":
                return "Han-Latin";
            case "ko":
                return "Hangul-Latin";
            default:
                // Covers Cyrillic, Greek, Arabic, Hebrew, Thai, Devanagari, etc. Latin-script
                // languages pass through and are filtered out by romanize().
                return "Any-Latin";
        }
    }

    private static String baseLanguage(String languageTag) {
        if (languageTag == null) {
            return null;
        }
        String lang = languageTag.trim().toLowerCase(Locale.ROOT);
        if (lang.isEmpty()) {
            return null;
        }
        int separator = lang.indexOf('-');
        return separator > 0 ? lang.substring(0, separator) : lang;
    }
}
