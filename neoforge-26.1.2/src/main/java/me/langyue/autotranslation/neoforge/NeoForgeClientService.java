package me.langyue.autotranslation.neoforge;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.client1201.DisplayTranslations;
import me.langyue.autotranslation.core.ScreenTranslationState;
import me.langyue.autotranslation.core.TranslationEngine;
import me.langyue.autotranslation.core.TranslationFilter;
import me.langyue.autotranslation.core.TranslationQueue;
import me.langyue.autotranslation.core.TranslationStore;
import me.langyue.autotranslation.translate.TranslatorManager;
import me.langyue.autotranslation.translate.google.GoogleTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Client-owned service; all network work uses the bounded core queue. */
final class NeoForgeClientService implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ClientConfig config;
    private final java.nio.file.Path gameDirectory;
    private final ConfirmationState commandState;
    private final ScreenTranslationState screens;
    private final Set<String> discoveredNamespaces = ConcurrentHashMap.newKeySet();
    private Runtime runtime;

    NeoForgeClientService(ClientConfig config, java.nio.file.Path gameDirectory) {
        this.config = config;
        this.gameDirectory = gameDirectory.toAbsolutePath();
        this.screens = new ScreenTranslationState(this.gameDirectory, config.ignoreOriginalScreen);
        this.commandState = new ConfirmationState(Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "AutoTranslation-command-confirm");
            thread.setDaemon(true);
            return thread;
        }));
        DisplayTranslationRuntime.install(this::translateRaw, screens, config);
    }

    void onResourceReload(ResourceManager resources) {
        Runtime active = ensureStarted();
        if (active == null) return;
        Map<String, Map<String, String>> missing = LanguageDiscovery.discover(resources, active.store.language(), config, active.filter, active.store);
        discoveredNamespaces.clear();
        discoveredNamespaces.addAll(missing.keySet());
        missing.forEach((namespace, entries) ->
                active.engine.translateBatch(namespace, entries, () -> DisplayTranslations.replace(active.store.snapshot())));
        DisplayTranslations.replace(active.store.snapshot());
    }

    void reloadTranslations() throws IOException {
        Runtime active = requireStarted();
        for (String namespace : persistedNamespaces(active.store)) active.store.load(namespace);
        DisplayTranslations.replace(active.store.snapshot());
    }

    Path packTranslations(boolean increment) throws IOException {
        return requireStarted().resourceStore.pack(increment, Set.copyOf(discoveredNamespaces));
    }

    boolean schedulePack(Runnable action) { return commandState.schedule(action); }

    boolean confirmPack() { return commandState.confirm(); }

    /** The keybinding changes only persisted screen opt-in state; rendering hooks remain separate. */
    void onClientTick(KeyMapping key) {
        if (closed.get()) return;
        while (key.consumeClick()) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) screens.toggle(screen);
        }
    }

    private String translateRaw(String source) {
        Runtime active = ensureStarted();
        if (active == null) return source;
        String translated = active.engine.translate(TranslationStore.NO_KEY_NAMESPACE, source, source,
                ignored -> DisplayTranslations.replace(active.store.snapshot()));
        return translated == null ? source : translated;
    }

    private synchronized Runtime requireStarted() {
        Runtime active = ensureStarted();
        if (active == null) throw new IllegalStateException("Minecraft language services are not ready");
        return active;
    }

    /**
     * This is deliberately called only by post-bootstrap client events.  The
     * mod constructor must not query LanguageManager: it is null during part
     * of NeoForge's cold-start path.
     */
    private synchronized Runtime ensureStarted() {
        if (closed.get()) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) return null;
        String language = minecraft.getLanguageManager().getSelected();
        if (language == null || language.isBlank()) return null;
        if (runtime != null && runtime.store.language().equals(language)) return runtime;
        if (runtime != null) runtime.close();
        DisplayTranslations.replace(Map.of());
        TranslationStore store = new TranslationStore(gameDirectory, language);
        TranslationFilter filter = new TranslationFilter(config.enFeature, config.yourLanguageFeature,
                config.wordBlacklist, config.excludedNamespace);
        GoogleTranslator google = new GoogleTranslator(config.google.domain, config.google.dns);
        TranslatorManager.setWarningSink(message -> System.err.println("[AutoTranslation] " + message));
        TranslatorManager.registerTranslator(TranslatorManager.DEFAULT_TRANSLATOR, () -> google);
        TranslatorManager.setTranslator(config.translator);
        TranslationEngine engine = new TranslationEngine(new TranslationQueue(minecraft::execute, 1024, 3,
                Duration.ofMillis(400)), store, filter, config.appendOriginal);
        int packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).major();
        // Resource-pack discovery reads the same persisted AutoTranslation tree.
        // It intentionally owns no mutable Minecraft state.
        runtime = new Runtime(store, filter, google, engine,
                new NeoForgeResourcePack(store, packFormat));
        loadPersistedBestEffort(runtime);
        return runtime;
    }

    private void loadPersistedBestEffort(Runtime active) {
        try {
            for (String namespace : persistedNamespaces(active.store)) {
                try {
                    active.store.load(namespace);
                } catch (IOException malformedNamespace) {
                    System.err.println("[AutoTranslation] Could not load persisted namespace " + namespace);
                }
            }
            DisplayTranslations.replace(active.store.snapshot());
        } catch (IOException scanFailure) {
            System.err.println("[AutoTranslation] Could not scan persisted translations");
        }
    }

    private static Set<String> persistedNamespaces(TranslationStore store) throws IOException {
        Set<String> result = new TreeSet<>();
        if (!Files.isDirectory(store.root())) return result;
        try (var paths = Files.list(store.root())) {
            paths.filter(Files::isDirectory).forEach(path -> {
                String namespace = path.getFileName().toString();
                if (namespace.matches("[a-z0-9_.-]+")
                        && Files.isRegularFile(path.resolve(store.language() + ".json"))) {
                    result.add(namespace);
                }
            });
        }
        return result;
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        commandState.close();
        if (runtime != null) runtime.close();
        screens.close();
        DisplayTranslationRuntime.clear();
        DisplayTranslations.replace(Map.of());
    }

    private static final class Runtime implements AutoCloseable {
        private final TranslationStore store;
        private final TranslationFilter filter;
        private final GoogleTranslator google;
        private final TranslationEngine engine;
        private final NeoForgeResourcePack resourceStore;

        private Runtime(TranslationStore store, TranslationFilter filter, GoogleTranslator google,
                        TranslationEngine engine, NeoForgeResourcePack resourceStore) {
            this.store = store;
            this.filter = filter;
            this.google = google;
            this.engine = engine;
            this.resourceStore = resourceStore;
        }

        @Override public void close() {
            engine.close();
            google.close();
        }
    }

    /** A previous confirmation timeout must never clear a newer pending action. */
    private static final class ConfirmationState implements AutoCloseable {
        private final ScheduledExecutorService timer;
        private Runnable pending;
        private ScheduledFuture<?> timeout;

        private ConfirmationState(ScheduledExecutorService timer) { this.timer = timer; }

        private synchronized boolean schedule(Runnable action) {
            if (pending != null) return false;
            pending = action;
            timeout = timer.schedule(() -> expire(action), 30, TimeUnit.SECONDS);
            return true;
        }

        private void expire(Runnable action) {
            synchronized (this) {
                if (pending == action) pending = null;
            }
        }

        private boolean confirm() {
            Runnable action;
            synchronized (this) {
                action = pending;
                pending = null;
                if (timeout != null) timeout.cancel(false);
                timeout = null;
            }
            if (action == null) return false;
            action.run();
            return true;
        }

        @Override public void close() {
            synchronized (this) {
                pending = null;
                if (timeout != null) timeout.cancel(false);
                timeout = null;
            }
            timer.shutdownNow();
        }
    }
}
