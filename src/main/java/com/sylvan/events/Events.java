package com.sylvan.events;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sylvan.Presence;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;

public class Events {
	public static ScheduledExecutorService scheduler;

	public static void registerEvents() {
		// Start/stop scheduler with server
		ServerLifecycleEvents.SERVER_STARTING.register((serverStarting) -> {
			scheduler = Executors.newSingleThreadScheduledExecutor();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((serverStopping) -> {
			scheduler.shutdown();
			try {
				scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		// Schedule footstep event on player join
		if (Presence.config.footstepsEnabled) {
			ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server) -> {
				Footsteps.scheduleEvent(serverPlayNetworkHandler.getPlayer());
			});
		}

		// Test event
		UseItemCallback.EVENT.register((player, world, hand) -> {
			return TypedActionResult.pass(ItemStack.EMPTY);
		});
	}
}
