package me.shaweel.shmacro;

import net.fabricmc.api.ClientModInitializer;

public class shmacro implements ClientModInitializer {
	public static final String MOD_ID = "shmacro";

	@Override
	public void onInitializeClient() {
		Scheduler.initialize();
		Commands.initialize();
	}
}
