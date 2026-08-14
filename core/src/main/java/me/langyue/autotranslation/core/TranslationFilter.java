package me.langyue.autotranslation.core;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Immutable filtering rules used by language-key discovery and screen translation. */
public final class TranslationFilter {
    private static final Pattern IDENTIFIER = Pattern.compile("(([^\\s:]+:)+([^\\s.]+\\.)*[^\\s.]+)|(([^\\s.]+\\.)+[^\\s.]+)");
    private final Pattern english;
    private final Pattern targetLanguage;
    private final Set<String> ignoredWords;
    private final Collection<Pattern> excludedNamespaces;

    public TranslationFilter(String englishPattern, String targetLanguagePattern,
                             Collection<String> ignoredWords, Collection<String> excludedNamespacePatterns) {
        this.english = compileOrDefault(englishPattern, "([A-Z]?[a-z]{2,}\\s*)+");
        this.targetLanguage = compileOrDefault(targetLanguagePattern, "[\\u0800-\\u9fa5\\uac00-\\ud7ff]+");
        this.ignoredWords = ignoredWords.stream()
                .filter(Objects::nonNull).map(value -> value.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.excludedNamespaces = excludedNamespacePatterns.stream()
                .filter(Objects::nonNull).map(value -> compileOrDefault(value, "a^" )).toList();
    }

    public boolean excludesNamespace(String namespace) {
        return namespace != null && excludedNamespaces.stream().anyMatch(pattern -> pattern.matcher(namespace).matches());
    }

    public boolean shouldTranslate(String languageCode, String key, String content, boolean cached) {
        if (languageCode == null || "en_us".equalsIgnoreCase(languageCode)) return false;
        if (content == null || content.trim().length() < 2 || content.trim().startsWith("* (")) return false;
        if (!english.matcher(content).find() || targetLanguage.matcher(content).find()) return false;
        if (IDENTIFIER.matcher(content).matches()) return false;
        if (cached) return true;
        String candidate = content.replaceAll("§[0-9a-rA-R]", " ")
                .replaceAll(IDENTIFIER.pattern(), " ").replaceAll("[@#*&]\\S+", " ").toLowerCase(Locale.ROOT);
        for (String ignored : ignoredWords) candidate = candidate.replace(ignored, " ");
        return english.matcher(candidate.trim()).find();
    }

    private static Pattern compileOrDefault(String expression, String fallback) {
        try {
            return Pattern.compile(expression == null || expression.isBlank() ? fallback : expression);
        } catch (PatternSyntaxException ignored) {
            return Pattern.compile(fallback);
        }
    }
}
