package me.shaweel.shmacro;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class Commands {
	private static void registerCombineBooksCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("combine_books")
		.then(ClientCommands.argument("max_level", IntegerArgumentType.integer(1, 10)).executes(context -> {
			Log.info(
				Component.empty()
				.append("Open an ")
				.append(Component.literal("Anvil").withStyle(ChatFormatting.GREEN))
				.append(Component.literal(" to start combining.").withStyle(ChatFormatting.RESET)),
				Component.literal("Cancels in 10 seconds").withStyle(ChatFormatting.RED)
			);

			BookCombiner.startWaiting(IntegerArgumentType.getInteger(context, "max_level"));

			return 1;
		})));
	}

	public static void initialize() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
			registerCombineBooksCommand(dispatcher);
		});
	}
}
