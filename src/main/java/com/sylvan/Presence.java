package com.sylvan;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.math.random.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sylvan.event.Events;

public class Presence implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("presence");
	public static final Random RANDOM = Random.create();
	public static PresenceConfig config;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.info("Presence loading...");

		initConfig();
		Events.registerEvents();
		Commands.registerCommands();

		LOGGER.info("Presence loaded.");
	}

	private void initConfig() {
		// Load/save config
		config = PresenceConfig.loadConfig();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			config.saveConfig();
		}));
	}
}
