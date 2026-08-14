package me.langyue.autotranslation.client1201;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.AutoConfig;
import me.langyue.autotranslation.core.LegacyConfigBackup;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Exact legacy keys are retained so Cloth writes config/autotranslation.json5. */
@Config(name = "autotranslation")
public final class ClientConfig implements ConfigData {
 public enum FilterMode { RESOURCE, CORRECTION }
 public enum ScreenArea { TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }
 public FilterMode mode = FilterMode.RESOURCE;
 public String enFeature = "([A-Z]?[a-z]{2,}\\s*)+";
 public String yourLanguageFeature = "[\\u0800-\\u9fa5\\uac00-\\ud7ff]+";
 public String translator = "Google";
 public boolean ignoreOriginalScreen = true;
 public boolean appendOriginal = true;
 public List<String> wordBlacklist = new ArrayList<>();
 public boolean debug = false;
 @ConfigEntry.Gui.Excluded public Set<String> excludedNamespace = new HashSet<>(Arrays.asList("minecraft", "^fabric-.*", "forge", "neoforge"));
 public Icon icon = new Icon(); public Google google = new Google();
 public static class Icon { public boolean alwaysDisplay; public ScreenArea displayArea = ScreenArea.TOP_RIGHT; public int offsetX; public int offsetY; }
 public static class Google { public String domain = "translate.google.com"; public Set<String> dns = new HashSet<>(Arrays.asList("64.233.189.191","108.177.97.100","216.239.32.40","74.125.196.113","142.251.171.90","142.250.1.90","172.217.218.90","108.177.126.90","142.251.1.90")); }
 @Override public void validatePostLoad() {
 if (wordBlacklist == null) wordBlacklist = new ArrayList<>();
  if (excludedNamespace == null) excludedNamespace = new HashSet<>();
  if (enFeature == null || enFeature.isBlank()) enFeature = "([A-Z]?[a-z]{2,}\\s*)+";
  if (yourLanguageFeature == null || yourLanguageFeature.isBlank()) yourLanguageFeature = "[\\u0800-\\u9fa5\\uac00-\\ud7ff]+";
  if (translator == null || translator.isBlank()) translator = "Google";
  if (icon == null) icon = new Icon();
  if (google == null) google = new Google();
  if (google.dns == null) google.dns = new HashSet<>();
  String raw = google.domain == null ? "" : google.domain.trim().toLowerCase(Locale.ROOT);
  raw = raw.replaceFirst("^https?://", "").replaceFirst("/.*$", "");
  google.domain = raw.matches("[a-z0-9.-]+") && !raw.startsWith(".") && !raw.endsWith(".") ? raw : "translate.google.com";
 }
 public static ClientConfig register(Path gameDirectory) {
  try { LegacyConfigBackup.createOnce(gameDirectory); }
  catch (IOException failure) { throw new IllegalStateException("Cannot back up config/autotranslation.json5 before migration", failure); }
  AutoConfig.register(ClientConfig.class, JanksonConfigSerializer::new);
  ClientConfig config=AutoConfig.getConfigHolder(ClientConfig.class).getConfig(); config.validatePostLoad(); return config;
 }
}
