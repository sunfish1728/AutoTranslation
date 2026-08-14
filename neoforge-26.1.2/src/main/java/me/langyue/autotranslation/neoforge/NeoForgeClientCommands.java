package me.langyue.autotranslation.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/** Native client commands: no command is sent to, or registered with, a server. */
final class NeoForgeClientCommands {
    private NeoForgeClientCommands() { }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, NeoForgeClientService service) {
        dispatcher.register(Commands.literal("auto_translation")
                .then(Commands.literal("reload").executes(context -> reload(context.getSource(), service)))
                .then(Commands.literal("confirm").executes(context -> confirm(context.getSource(), service)))
                .then(Commands.literal("pack_resource")
                        .executes(context -> requestPack(context.getSource(), service, false))
                        .then(Commands.literal("full").executes(context -> requestPack(context.getSource(), service, false)))
                        .then(Commands.literal("increment").executes(context -> requestPack(context.getSource(), service, true)))));
    }

    private static int reload(CommandSourceStack source, NeoForgeClientService service) {
        try {
            service.reloadTranslations();
            success(source, "AutoTranslation translations reloaded.");
            return 1;
        } catch (Exception failure) {
            failure(source, "AutoTranslation could not reload translations.");
            return 0;
        }
    }

    private static int requestPack(CommandSourceStack source, NeoForgeClientService service, boolean increment) {
        if (!service.schedulePack(() -> pack(source, service, increment))) {
            failure(source, "A resource-pack action is already waiting for confirmation.");
            return 0;
        }
        success(source, "Run /auto_translation confirm within 30 seconds to create the resource pack.");
        return 1;
    }

    private static int confirm(CommandSourceStack source, NeoForgeClientService service) {
        if (!service.confirmPack()) {
            failure(source, "There is no pending AutoTranslation action to confirm.");
            return 0;
        }
        return 1;
    }

    private static void pack(CommandSourceStack source, NeoForgeClientService service, boolean increment) {
        try {
            Path output = service.packTranslations(increment);
            success(source, "AutoTranslation resource pack created: " + output.getFileName());
        } catch (Exception failure) {
            failure(source, "AutoTranslation could not create a resource pack.");
        }
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static void failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
    }
}
