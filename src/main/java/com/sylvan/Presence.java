package com.sylvan;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.math.random.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sylvan.events.Footsteps;

public class Presence implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("presence");
	public static final Random RANDOM = Random.create();
	public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server) -> {
			Footsteps.scheduleEvent(serverPlayNetworkHandler.getPlayer());
		});

		ServerLifecycleEvents.SERVER_STOPPING.register((serverStopping) -> {
			SCHEDULER.shutdown();
		});

		LOGGER.info("Presence loaded.");
	}
}