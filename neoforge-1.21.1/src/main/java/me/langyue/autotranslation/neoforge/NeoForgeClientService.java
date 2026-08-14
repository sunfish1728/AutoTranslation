package me.langyue.autotranslation.neoforge;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.client1201.DisplayTranslations;
import me.langyue.autotranslation.core.TranslationQueue;
import me.langyue.autotranslation.core.TranslationFilter;
import me.langyue.autotranslation.core.TranslationStore;
import me.langyue.autotranslation.core.TranslationEngine;
import me.langyue.autotranslation.core.ScreenTranslationState;
import me.langyue.autotranslation.translate.TranslatorManager;
import me.langyue.autotranslation.translate.google.GoogleTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import me.langyue.autotranslation.client1201.ClientResourceStore;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

final class NeoForgeClientService implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final TranslationQueue queue = new TranslationQueue(Minecraft.getInstance()::execute, 1024, 3, Duration.ofMillis(400));
    private final GoogleTranslator google;
    private final ClientConfig config;
    private final TranslationStore store;
    private final TranslationFilter filter;
    private final ClientResourceStore resourceStore;
    private final TranslationEngine engine;
    private final ScreenTranslationState screens;
    private NeoForgeClientCommands commands;

    NeoForgeClientService(ClientConfig config) {
        this.config = config;
        Minecraft minecraft = Minecraft.getInstance();
        this.store = new TranslationStore(minecraft.gameDirectory.toPath(), minecraft.options.languageCode);
        this.filter = new TranslationFilter(config.enFeature, config.yourLanguageFeature, config.wordBlacklist, config.excludedNamespace);
        this.resourceStore = new ClientResourceStore(minecraft.gameDirectory.toPath(), store.language(),
                SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        google = new GoogleTranslator(config.google.domain, config.google.dns);
        TranslatorManager.setWarningSink(message -> System.err.println("[AutoTranslation] " + message));
        TranslatorManager.registerTranslator(TranslatorManager.DEFAULT_TRANSLATOR, () -> google);
        TranslatorManager.setTranslator(config.translator);
        this.engine = new TranslationEngine(queue, store, filter, config.appendOriginal);
        this.screens = new ScreenTranslationState(minecraft.gameDirectory.toPath(), config.ignoreOriginalScreen);
        DisplayTranslationRuntime.install(this::translateRaw, screens, config);
    }

    TranslationQueue queue() { return queue; }

    void onResourceReload(ResourceManager resources) {
        LanguageDiscovery.discover(resources, store.language(), config, filter, store, engine,
                () -> DisplayTranslations.replace(store.snapshot()));
        DisplayTranslations.replace(store.snapshot());
    }

    private String translateRaw(String source) {
        String translated = engine.translate(TranslationStore.NO_KEY_NAMESPACE, source, source,
                ignored -> DisplayTranslations.replace(store.snapshot()));
        return translated == null ? source : translated;
    }

    void registerCommands(RegisterClientCommandsEvent event) {
        if (commands != null) commands.close();
        commands = new NeoForgeClientCommands(resourceStore);
        commands.register(event.getDispatcher());
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        queue.close();
        if (commands != null) commands.close();
        DisplayTranslationRuntime.clear();
        engine.close();
        screens.close();
        google.close();
    }
}
