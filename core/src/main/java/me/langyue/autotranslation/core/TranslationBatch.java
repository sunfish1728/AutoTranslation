package me.langyue.autotranslation.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A line-oriented translation batch. Only values are translatable; stable IDs
 * let callers reject reordered, dropped, duplicated, or malformed responses.
 */
public final class TranslationBatch {
    private static final String PREFIX = "__AT_ITEM_";
    private final List<Entry> entries;

    private TranslationBatch(List<Entry> entries) { this.entries = List.copyOf(entries); }

    public static List<TranslationBatch> partition(Map<String, String> values, int maximumRequestLength) {
        Objects.requireNonNull(values, "values");
        if (maximumRequestLength < 64) throw new IllegalArgumentException("maximumRequestLength is too small");
        List<TranslationBatch> batches = new ArrayList<>();
        List<Entry> current = new ArrayList<>();
        int currentLength = 0;
        int id = 0;
        for (Map.Entry<String, String> value : values.entrySet()) {
            String marker = marker(id++);
            FormatProtector.ProtectedText protectedText = FormatProtector.protect(Objects.requireNonNull(value.getValue(), "translation value"));
            Entry entry = new Entry(value.getKey(), marker, protectedText);
            int lineLength = marker.length() + 1 + protectedText.text().length() + (current.isEmpty() ? 0 : 1);
            if (lineLength > maximumRequestLength) {
                throw new IllegalArgumentException("A single protected value exceeds the translator limit");
            }
            if (!current.isEmpty() && currentLength + lineLength > maximumRequestLength) {
                batches.add(new TranslationBatch(current));
                current = new ArrayList<>();
                currentLength = 0;
                lineLength--;
            }
            current.add(entry);
            currentLength += lineLength;
        }
        if (!current.isEmpty()) batches.add(new TranslationBatch(current));
        return List.copyOf(batches);
    }

    public String requestText() {
        StringBuilder request = new StringBuilder();
        for (Entry entry : entries) {
            if (!request.isEmpty()) request.append('\n');
            request.append(entry.marker).append('\t').append(entry.protectedText.text());
        }
        return request.toString();
    }

    public Map<String, String> decode(String response) {
        Objects.requireNonNull(response, "response");
        Map<String, String> byMarker = new LinkedHashMap<>();
        for (String line : response.split("\\R", -1)) {
            int separator = line.indexOf('\t');
            if (separator <= 0) throw new IllegalArgumentException("Malformed translation batch response");
            String id = line.substring(0, separator);
            if (byMarker.putIfAbsent(id, line.substring(separator + 1)) != null) {
                throw new IllegalArgumentException("Duplicate translation batch ID");
            }
        }
        if (byMarker.size() != entries.size()) throw new IllegalArgumentException("Translation batch item count changed");
        Map<String, String> decoded = new LinkedHashMap<>();
        for (Entry entry : entries) {
            String translated = byMarker.get(entry.marker);
            if (translated == null) throw new IllegalArgumentException("Translation batch ID was changed or removed");
            decoded.put(entry.key, entry.protectedText.restore(translated));
        }
        return Collections.unmodifiableMap(decoded);
    }

    private static String marker(int index) { return PREFIX + String.format("%06d", index) + "__"; }

    private record Entry(String key, String marker, FormatProtector.ProtectedText protectedText) { }
}
