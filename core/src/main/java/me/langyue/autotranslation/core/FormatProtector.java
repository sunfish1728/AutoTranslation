package me.langyue.autotranslation.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Protects formatting tokens from translation services and restores them losslessly. */
public final class FormatProtector {
    private static final Pattern TOKENS = Pattern.compile(
            "%((\\d+)\\$)?[-#+ 0,(<]*\\d*(\\.\\d+)?[tT]?[a-zA-Z%]|\\{(?:\\d+)?}|§[0-9A-FK-ORa-fk-or]|\\r\\n|\\r|\\n|\\\\n");
    private static final Pattern ANY_MARKER = Pattern.compile("\\[\\[AT\\d+]]");
    private FormatProtector() { }

    public static ProtectedText protect(String text) {
        Matcher matcher = TOKENS.matcher(text);
        List<String> tokens = new ArrayList<>();
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String marker = "[[AT" + tokens.size() + "]]";
            tokens.add(matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(marker));
        }
        matcher.appendTail(result);
        return new ProtectedText(result.toString(), List.copyOf(tokens));
    }

    public record ProtectedText(String text, List<String> tokens) {
        public String restore(String translated) {
            if (translated == null) {
                throw new IllegalArgumentException("Translated text must not be null");
            }
            String restored = translated;
            for (int index = 0; index < tokens.size(); index++) {
                String marker = "[[AT" + index + "]]";
                if (countOccurrences(restored, marker) != 1) {
                    throw new IllegalArgumentException("Translation changed protected formatting tokens");
                }
                restored = restored.replace(marker, tokens.get(index));
            }
            if (ANY_MARKER.matcher(restored).find()) {
                throw new IllegalArgumentException("Translation introduced an unknown formatting token");
            }
            return restored;
        }

        private static int countOccurrences(String value, String target) {
            int count = 0;
            int offset = 0;
            while ((offset = value.indexOf(target, offset)) >= 0) {
                count++;
                offset += target.length();
            }
            return count;
        }
    }
}
