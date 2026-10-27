package me.shaweel.shmacro;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class Scheduler {
	public static class TimeoutScheduledTask {
		final Runnable runnable;
		int timeout;

		TimeoutScheduledTask(Runnable runnable, int timeout) {
			this.runnable = runnable;
			this.timeout = timeout;
		}

		public void cancel() {
			timeoutScheduledTasks.remove(this);
		}
	}

	public record ConditionScheduledTask(Runnable runnable, BooleanSupplier condition) {
		public void cancel() {
			conditionScheduledTasks.remove(this);
		}
	}

	public static class ConditionOrTimeoutScheduledTask {
		final Runnable runnable;
		final BooleanSupplier condition;
		int timeout;

		ConditionOrTimeoutScheduledTask(
			Runnable runnable, BooleanSupplier condition, int timeout
		) {
			this.runnable = runnable;
			this.condition = condition;
			this.timeout = timeout;
		}

		public void cancel() {
			conditionOrTimeoutScheduledTasks.remove(this);
		}
	}

	public static class ConditionAndTimeoutScheduledTask {
		final Runnable runnable;
		final BooleanSupplier condition;
		int timeout;
		final int originalTimeout;

		ConditionAndTimeoutScheduledTask(
			Runnable runnable, BooleanSupplier condition, int timeout
		) {
			this.runnable = runnable;
			this.condition = condition;
			this.timeout = timeout;
			this.originalTimeout = timeout;
		}

		public void cancel() {
			conditionAndTimeoutScheduledTasks.remove(this);
		}
	}
	
	private static List<TimeoutScheduledTask> timeoutScheduledTasks = new ArrayList<>();
	private static List<ConditionScheduledTask> conditionScheduledTasks = new ArrayList<>();
	private static List<ConditionOrTimeoutScheduledTask> conditionOrTimeoutScheduledTasks = new ArrayList<>();
	private static List<ConditionAndTimeoutScheduledTask> conditionAndTimeoutScheduledTasks = new ArrayList<>();

	public static TimeoutScheduledTask doIn(Runnable runnable, int timeout) {
		TimeoutScheduledTask task = new TimeoutScheduledTask(runnable, timeout);
		timeoutScheduledTasks.add(task);
		return task;
	}

	public static ConditionScheduledTask doWhen(Runnable runnable, BooleanSupplier condition) {
		ConditionScheduledTask task = new ConditionScheduledTask(runnable, condition);
		conditionScheduledTasks.add(task);
		return task;
	}

	public static ConditionAndTimeoutScheduledTask doInWhen(Runnable runnable, BooleanSupplier condition, int timeout) {
		ConditionAndTimeoutScheduledTask task = new ConditionAndTimeoutScheduledTask(runnable, condition, timeout);
		conditionAndTimeoutScheduledTasks.add(task);
		return task;
	}


	public static ConditionOrTimeoutScheduledTask doWhenOrIn(Runnable runnable, BooleanSupplier condition, int timeout) {
		ConditionOrTimeoutScheduledTask task = new ConditionOrTimeoutScheduledTask(runnable, condition, timeout);
		conditionOrTimeoutScheduledTasks.add(task);
		return task;
	}

	private static void doTimeoutScheduledTasks() {
		for (TimeoutScheduledTask task : new ArrayList<>(timeoutScheduledTasks)) {
			if (task.timeout <= 0) {
				task.runnable.run();
				timeoutScheduledTasks.remove(task);
				continue;
			}

			task.timeout -= 1;
		}
	}

	private static void doConditionScheduledTasks() {
		for (ConditionScheduledTask task : new ArrayList<>(conditionScheduledTasks)) {
			if (!task.condition.getAsBoolean()) continue;
				
			conditionScheduledTasks.remove(task);
			task.runnable.run();
		}
	}

	private static void doConditionOrTimeoutScheduledTasks() {
		for (ConditionOrTimeoutScheduledTask task : new ArrayList<>(conditionOrTimeoutScheduledTasks)) {
			if (task.timeout <= 0) {
				conditionOrTimeoutScheduledTasks.remove(task);
				task.runnable.run();
				continue;
			}

			task.timeout -= 1;
			
			if (!task.condition.getAsBoolean()) continue;
			
			conditionOrTimeoutScheduledTasks.remove(task);
			task.runnable.run();
		}
	}

	private static void doConditionAndTimeoutScheduledTasks() {
		for (ConditionAndTimeoutScheduledTask task : new ArrayList<>(conditionAndTimeoutScheduledTasks)) {
			if (!task.condition.getAsBoolean()) {
				task.timeout = task.originalTimeout;
				continue;
			};

			if (task.timeout <= 0) {
				conditionAndTimeoutScheduledTasks.remove(task);
				task.runnable.run();
				continue;
			}

			task.timeout -= 1;
		}
	}


	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			doTimeoutScheduledTasks();
			doConditionScheduledTasks();
			doConditionOrTimeoutScheduledTasks();
			doConditionAndTimeoutScheduledTasks();
		});
	}
}
