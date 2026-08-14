package me.langyue.autotranslation.client1201;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Immutable snapshot read by client render hooks; never stores or mutates game Components. */
public final class DisplayTranslations {
    private static final AtomicReference<Map<String, String>> SNAPSHOT = new AtomicReference<>(Map.of());
    private DisplayTranslations() { }
    public static void replace(Map<String, String> values) { SNAPSHOT.set(Map.copyOf(values)); }
    public static String displayCopy(String key, String original) { return SNAPSHOT.get().getOrDefault(key, original); }
}
