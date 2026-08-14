package me.langyue.autotranslation.translate;

import me.langyue.autotranslation.AutoTranslation;
import me.langyue.autotranslation.translate.google.Google;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TranslatorManager {

    public static final String DEFAULT_TRANSLATOR = "Google";

    private static final Map<String, Supplier<ITranslator>> _TRANSLATOR_MAP = new LinkedHashMap<>() {{
        put(DEFAULT_TRANSLATOR, Google::getInstance);
    }};

    private static final Map<String, ITranslator> _TRANSLATOR_INSTANCES = new HashMap<>();


    public static void init() {
        setTranslator(AutoTranslation.CONFIG.translator);
        TranslateThreadPool.init();
    }

    public static void setTranslator(String name) {
        Supplier<ITranslator> factory = _TRANSLATOR_MAP.get(name);
        if (factory == null) {
            AutoTranslation.LOGGER.warn("Unknown translator '{}', falling back to {}", name, DEFAULT_TRANSLATOR);
            name = DEFAULT_TRANSLATOR;
            factory = _TRANSLATOR_MAP.get(name);
        }
        if (!_TRANSLATOR_INSTANCES.containsKey(name)) {
            ITranslator translator = factory.get();
            if (translator == null) {
                AutoTranslation.LOGGER.error("Translator factory returned no instance: {}", name);
            } else {
                translator.init();
                _TRANSLATOR_INSTANCES.put(name, translator);
            }
        }
    }

    public static void registerTranslator(String name, Supplier<ITranslator> getInstance) {
        _TRANSLATOR_MAP.put(name, getInstance);
    }

    public static ITranslator getTranslator() {
        ITranslator translator = getTranslator(AutoTranslation.CONFIG.translator);
        return translator == null ? getTranslator(DEFAULT_TRANSLATOR) : translator;
    }

    public static ITranslator getTranslator(String name) {
        if (StringUtils.isBlank(name)) return null;
        return _TRANSLATOR_INSTANCES.get(name);
    }
}
