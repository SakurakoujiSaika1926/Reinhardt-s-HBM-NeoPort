package com.reinhardt.hbm.client.search;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Optional bridge for JustEnoughCharacters/JECh.
 * <p>
 * JECh normally patches known search call sites with coremod transformers. HBM's
 * custom screens are not part of JECh's compatibility table, so call its public
 * matching helper reflectively when it is installed. This keeps HBM free from a
 * hard dependency and avoids maintaining our own pinyin dictionary.
 */
public final class JechSearchCompat {
    private static final Method JECH_CONTAINS = findJechContains();
    private static volatile boolean jechEnabled = JECH_CONTAINS != null;

    private JechSearchCompat() {
    }

    public static String normalizeQuery(String query) {
        return normalize(query).strip();
    }

    public static boolean contains(String text, String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }
        String normalizedText = normalize(text);
        if (normalizedText.contains(normalizedQuery)) {
            return true;
        }
        return jechContains(normalizedText, normalizedQuery);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean jechContains(String text, String query) {
        if (!jechEnabled) {
            return false;
        }
        try {
            Object result = JECH_CONTAINS.invoke(null, text, query);
            return result instanceof Boolean matched && matched;
        } catch (Throwable ignored) {
            // If JECh changes internals or fails to initialise, silently fall
            // back to vanilla search for the rest of this session.
            jechEnabled = false;
            return false;
        }
    }

    private static Method findJechContains() {
        try {
            Class<?> match = Class.forName("me.towdium.jecharacters.utils.Match", false,
                    JechSearchCompat.class.getClassLoader());
            return match.getMethod("contains", String.class, CharSequence.class);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
