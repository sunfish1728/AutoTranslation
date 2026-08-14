package me.langyue.autotranslation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import me.langyue.autotranslation.AutoTranslation;
import me.langyue.autotranslation.translate.TranslatorManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loader-neutral configuration retaining the legacy field names. */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public enum FilterMode { RESOURCE, CORRECTION }
    public enum ScreenArea { TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }
    public FilterMode mode = FilterMode.RESOURCE;
    public String enFeature = "([A-Z]?[a-z]{2,}\\s*)+";
    public String yourLanguageFeature = "[\\u0800-\\u9fa5\\uac00-\\ud7ff]+";
    public String translator = TranslatorManager.DEFAULT_TRANSLATOR;
    public boolean ignoreOriginalScreen = true;
    public boolean appendOriginal = true;
    public List<String> wordBlacklist = new ArrayList<>();
    public boolean debug = false;
    public Set<String> excludedNamespace = new HashSet<>() {{ add("minecraft"); add("^fabric-.*"); add("forge"); add("neoforge"); }};
    public Icon icon = new Icon();
    public Google google = new Google();

    public static class Icon { public boolean alwaysDisplay = false; public ScreenArea displayArea = ScreenArea.TOP_RIGHT; public int offsetX; public int offsetY; }
    public static class Google { public String domain = "translate.google.com"; public Set<String> dns = new HashSet<>(); }

    public static void init() {
        Path directory = AutoTranslation.ROOT.getParent().resolve("config");
        Path json = directory.resolve("autotranslation.json");
        Path legacy = directory.resolve("autotranslation.json5");
        Config loaded = read(Files.exists(json) ? json : legacy);
        AutoTranslation.CONFIG = loaded == null ? new Config() : loaded;
        if (AutoTranslation.CONFIG.google == null) AutoTranslation.CONFIG.google = new Google();
        if (AutoTranslation.CONFIG.google.domain == null || AutoTranslation.CONFIG.google.domain.isBlank()) AutoTranslation.CONFIG.google.domain = "translate.google.com";
        AutoTranslation.CONFIG.google.domain = AutoTranslation.CONFIG.google.domain.toLowerCase().replaceFirst("^https?://", "").replaceFirst("/.*$", "");
        try { Files.createDirectories(directory); if (!Files.exists(json)) Files.writeString(json, GSON.toJson(AutoTranslation.CONFIG)); }
        catch (IOException exception) { AutoTranslation.LOGGER.warn("Could not save AutoTranslation configuration: {}", exception.getClass().getSimpleName()); }
    }

    private static Config read(Path path) {
        if (path == null || !Files.exists(path)) return null;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) { reader.setLenient(true); return GSON.fromJson(reader, Config.class); }
        catch (Exception exception) { AutoTranslation.LOGGER.warn("Could not read AutoTranslation configuration: {}", exception.getClass().getSimpleName()); return null; }
    }
}
