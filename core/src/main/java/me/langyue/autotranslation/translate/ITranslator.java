package me.langyue.autotranslation.translate;

/**
 * Public translator extension API retained from AutoTranslation 1.2.x.
 * Implementations must be registered before the configured translator is selected.
 */
public interface ITranslator {
    void init();

    default boolean ready() { return true; }

    int maxLength();

    default String translate(String text, String tl) {
        return translate(text, tl, "auto");
    }

    String translate(String text, String tl, String sl);
}
