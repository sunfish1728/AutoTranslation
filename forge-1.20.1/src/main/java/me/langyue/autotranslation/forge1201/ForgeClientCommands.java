package me.langyue.autotranslation.forge1201;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.langyue.autotranslation.client1201.ClientCommandState;
import me.langyue.autotranslation.client1201.ClientResourceStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Forge adapter with the same reload/confirm/full/increment semantic state machine as Fabric. */
final class ForgeClientCommands {
 private final ClientResourceStore store; private final ClientCommandState state;
 ForgeClientCommands(ClientResourceStore store, ClientCommandState state) { this.store = store; this.state = state; }
 void register(CommandDispatcher<CommandSourceStack> d) {
  d.register(literal("auto_translation")
   .then(literal("reload").executes(c -> reload(c.getSource(), null)).then(argument("namespace", StringArgumentType.word()).executes(c -> reload(c.getSource(), StringArgumentType.getString(c,"namespace")))))
   .then(literal("confirm").executes(c -> state.confirm() ? 1 : 0))
   .then(literal("pack_resource").then(literal("full").executes(c -> schedule(c.getSource(), false))).then(literal("increment").executes(c -> schedule(c.getSource(), true)))));
 }
 private int reload(CommandSourceStack s,String n){try{store.reload(n);s.sendSuccess(()->Component.translatable("commands.autotranslation.command.reloaded"),false);return 1;}catch(Exception e){s.sendFailure(Component.translatable("commands.autotranslation.command.error.reload"));return 0;}}
 private int schedule(CommandSourceStack s,boolean i){try{if(i&&!store.hasIncrement())return 0;if(!state.schedule(()->pack(s,i)))return 0;s.sendSuccess(()->Component.translatable("commands.autotranslation.command.pack_resource.unconfirmed"),false);return 1;}catch(Exception e){return 0;}}
 private void pack(CommandSourceStack s,boolean i){try{store.pack(i);s.sendSuccess(()->Component.translatable("commands.autotranslation.command.pack_resource.packed"),false);}catch(Exception e){s.sendFailure(Component.translatable("commands.autotranslation.command.error.pack_resource"));}}
}
