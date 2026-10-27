package me.shaweel.shmacro;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class Log {
	private static final Component INFO_PREFIX = Component.literal("[shmacro] ").withStyle(ChatFormatting.LIGHT_PURPLE);

	public static void info(Component... components) {
		for (Component component : components) {
			Minecraft.getInstance().player.sendSystemMessage(Component.empty().append(INFO_PREFIX).append(component));
		}
	}

	public static void info(String... strings) {
		for (String string : strings) {
			Minecraft.getInstance().player.sendSystemMessage(Component.empty().append(INFO_PREFIX).append(Component.literal(string)));
		}
	}
}
