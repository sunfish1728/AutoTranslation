package me.langyue.autotranslation.fabric;

import com.mojang.brigadier.CommandDispatcher;
import me.langyue.autotranslation.AutoTranslation;
import me.langyue.autotranslation.resource.ResourceManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** Client-only command bridge: no command is ever registered with a server. */
final class FabricAutoTranslationCommands {
    private static final ScheduledExecutorService CONFIRM_TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "AutoTranslation-command-confirm");
        thread.setDaemon(true);
        return thread;
    });
    private static Runnable pendingPack;
    private FabricAutoTranslationCommands() { }

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("auto_translation")
                .then(literal("reload").executes(context -> {
                    ResourceManager.loadResource();
                    context.getSource().sendFeedback(Component.translatable("commands.autotranslation.command.reloaded"));
                    return 1;
                }))
                .then(literal("confirm").executes(context -> confirm(context.getSource())))
                .then(literal("pack_resource")
                        .then(literal("full").executes(context -> schedulePack(context.getSource(), false)))
                        .then(literal("increment").executes(context -> schedulePack(context.getSource(), true)))));
    }

    private static int schedulePack(FabricClientCommandSource source, boolean increment) {
        if (increment && ResourceManager.UNKNOWN_KEYS.isEmpty()) {
            source.sendError(Component.translatable("commands.autotranslation.command.pack_resource.none"));
            return 0;
        }
        pendingPack = () -> pack(source, increment);
        source.sendFeedback(Component.translatable("commands.autotranslation.command.pack_resource.unconfirmed"));
        CONFIRM_TIMER.schedule(() -> pendingPack = null, 30, TimeUnit.SECONDS);
        return 1;
    }

    private static int confirm(FabricClientCommandSource source) {
        Runnable action = pendingPack;
        pendingPack = null;
        if (action == null) {
            source.sendError(Component.translatable("commands.autotranslation.command.unconfirmed.empty"));
            return 0;
        }
        action.run();
        return 1;
    }

    private static int pack(FabricClientCommandSource source, boolean increment) {
        try {
            ResourceManager.packResource(increment);
            source.sendFeedback(Component.translatable("commands.autotranslation.command.pack_resource.packed"));
            return 1;
        } catch (Throwable exception) {
            AutoTranslation.LOGGER.warn("Could not package resource pack: {}", exception.getClass().getSimpleName());
            source.sendError(Component.translatable("commands.autotranslation.command.error.pack_resource"));
            return 0;
        }
    }
}
