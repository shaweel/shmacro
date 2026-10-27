package me.shaweel.shmacro;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class BookCombiner {
	private static long waitingTime = -1;
	private static int maxEnchantmentLevel = 0;
	private static boolean finishedTwoBooks = false;
	private static AbstractContainerMenu currentContainerMenu;
	private static final int WAITING_TIME = 200;
	private static final int ANVIL_OR_SIGN_SLOT = 22;
	private static final int OUTPUT_SLOT = 13;
	private static Level waitingLevel;
	private static Scheduler.ConditionScheduledTask combineTask;
	private static Scheduler.ConditionOrTimeoutScheduledTask cancelWaitingTask;
	private static Scheduler.TimeoutScheduledTask shiftClickBook0Task;
	private static Scheduler.TimeoutScheduledTask shiftClickBook1Task;
	private static Scheduler.TimeoutScheduledTask clickAnvilTask;
	private static Scheduler.ConditionAndTimeoutScheduledTask clickSignTask;
	private static Scheduler.ConditionAndTimeoutScheduledTask combineTwoBooksTask;
	private record Books(int book0, int book1) {}

	private static boolean waiting() {
		return waitingTime >= 0 && Minecraft.getInstance().level.getGameTime() - waitingTime < WAITING_TIME;
	}

	private static boolean shouldCombine() {
		boolean inAnvil = Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getTitle().getString().equals("Anvil");
		return waiting() && inAnvil;
	}

	private static void resetState() {
		waitingTime = -1;
		waitingLevel = null;
		finishedTwoBooks = false;
		if (combineTask != null) combineTask.cancel();
		combineTask = null;

		if (cancelWaitingTask != null) cancelWaitingTask.cancel();
		cancelWaitingTask = null;
	}

	private static Books getValidBooks() {
		if (!verifyCorrectContainer()) return null;

		AbstractContainerMenu containerMenu = Minecraft.getInstance().player.containerMenu;

		for (Slot slot0 : containerMenu.slots) {
			ItemStack itemStack0 = slot0.getItem();
			if (itemStack0.getItem() != Items.ENCHANTED_BOOK || slot0.index == OUTPUT_SLOT) continue;

			CompoundTag enchantments0 = itemStack0.get(DataComponents.CUSTOM_DATA).copyTag().getCompoundOrEmpty("enchantments");
			if (enchantments0.size() > 1) continue;

			String enchantment0 = enchantments0.keySet().iterator().next();
			int enchantmentLevel0 = enchantments0.getIntOr(enchantment0, -1);
			if (enchantmentLevel0 == -1) continue;
			if (enchantmentLevel0 >= maxEnchantmentLevel) continue;

			for (Slot slot1 : containerMenu.slots) {
				ItemStack itemStack1 = slot1.getItem();
				if (itemStack1.getItem() != Items.ENCHANTED_BOOK || slot1.index == OUTPUT_SLOT) continue;

				CompoundTag enchantments1 = itemStack1.get(DataComponents.CUSTOM_DATA).copyTag().getCompoundOrEmpty("enchantments");
				if (enchantments1.size() > 1) continue;

				String enchantment1 = enchantments1.keySet().iterator().next();
				int enchantmentLevel1 = enchantments1.getIntOr(enchantment1, -1);
				if (enchantmentLevel1 == -1) continue;
				if (enchantmentLevel1 >= maxEnchantmentLevel) continue;

				if (slot0 == slot1) continue;

				if (
					slot0 == slot1 ||
					!enchantment0.equals(enchantment1) ||
					enchantmentLevel0 != enchantmentLevel1
				) continue;

				return new Books(slot0.index, slot1.index);
			}
		}

		return null;
	}

	private static boolean verifyCorrectContainer() {
		AbstractContainerMenu actualContainerMenu = Minecraft.getInstance().player.containerMenu;

		if (actualContainerMenu != currentContainerMenu) {
			shiftClickBook0Task.cancel();
			shiftClickBook1Task.cancel();
			clickAnvilTask.cancel();
			clickSignTask.cancel();
			combineTwoBooksTask.cancel();
			shiftClickBook0Task = null;
			shiftClickBook1Task = null;
			clickAnvilTask = null;
			clickSignTask = null;
			combineTwoBooksTask = null;
		}

		return actualContainerMenu == currentContainerMenu;
	}

	private static void shiftClickSlot(int slot) {
		if (!verifyCorrectContainer()) return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		AbstractContainerMenu containerMenu = player.containerMenu;

		minecraft.gameMode.handleContainerInput(
			containerMenu.containerId,
			slot,
			0,
			ContainerInput.QUICK_MOVE,
			player
		);
	}

	private static void clickAnvilOrSign() {
		if (!verifyCorrectContainer()) return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		AbstractContainerMenu containerMenu = player.containerMenu;

		minecraft.gameMode.handleContainerInput(
			containerMenu.containerId,
			ANVIL_OR_SIGN_SLOT,
			0,
			ContainerInput.PICKUP,
			player
		);
	}

	private static boolean getSignExists() {
		if (!verifyCorrectContainer()) return false;

		return Minecraft.getInstance().player.containerMenu
			.getSlot(ANVIL_OR_SIGN_SLOT)
			.getItem().getItem() == Items.OAK_SIGN;
	}

	private static void combineTwoBooks() {
		resetState();

		Books books = getValidBooks();
		if (books == null) return;

		int bookInterval0 = getSmallRandomInterval();
		int bookInterval1 = getSmallRandomInterval() + bookInterval0;
		int anvilInterval = getLargeRandomInterval() + bookInterval1;

		shiftClickBook0Task = Scheduler.doIn(() -> shiftClickSlot(books.book0), bookInterval0);
		shiftClickBook1Task = Scheduler.doIn(() -> shiftClickSlot(books.book1), bookInterval1);
		clickAnvilTask = Scheduler.doIn(BookCombiner::clickAnvilOrSign, anvilInterval);
		clickSignTask = Scheduler.doInWhen(() -> {
			clickAnvilOrSign();
			finishedTwoBooks = true;
		}, BookCombiner::getSignExists, getSmallRandomInterval());

		combineTwoBooksTask = Scheduler.doInWhen(BookCombiner::combineTwoBooks, () -> finishedTwoBooks, getSmallRandomInterval());
	}

	private static void startCombiningBooks() {
		currentContainerMenu = Minecraft.getInstance().player.containerMenu;
		combineTwoBooks();
	}

	private static int getSmallRandomInterval() {
		return ThreadLocalRandom.current().nextInt(4, 6);
	}

	private static int getLargeRandomInterval() {
		return ThreadLocalRandom.current().nextInt(11, 16);
	}

	private static void cancelWaiting() {
		if (!waiting()) return;
		resetState();

		Log.info(Component.literal("Cancelled book combining.").withStyle(ChatFormatting.RED));
	}

	public static void startWaiting(int maxLevel) {
		waitingLevel = Minecraft.getInstance().level;
		maxEnchantmentLevel = maxLevel;
		waitingTime = waitingLevel.getGameTime();
		combineTask = Scheduler.doWhen(BookCombiner::startCombiningBooks, BookCombiner::shouldCombine);

		cancelWaitingTask = Scheduler.doWhenOrIn(
			BookCombiner::cancelWaiting,
			() -> Minecraft.getInstance().level != waitingLevel,
			WAITING_TIME
		);
	}
}
