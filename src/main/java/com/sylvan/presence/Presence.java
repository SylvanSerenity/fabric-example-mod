package com.sylvan.presence;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.Entities;
import com.sylvan.presence.event.AmbientSounds;
import com.sylvan.presence.event.Attack;
import com.sylvan.presence.event.ChatMessage;
import com.sylvan.presence.event.Events;
import com.sylvan.presence.event.ExtinguishTorches;
import com.sylvan.presence.event.FlickerDoor;
import com.sylvan.presence.event.FlowerGift;
import com.sylvan.presence.event.Footsteps;
import com.sylvan.presence.event.Freeze;
import com.sylvan.presence.event.NearbySounds;
import com.sylvan.presence.event.OpenChest;
import com.sylvan.presence.event.OpenDoor;
import com.sylvan.presence.event.Stalk;
import com.sylvan.presence.event.SubtitleWarning;
import com.sylvan.presence.event.TrampleCrops;
import com.sylvan.presence.event.Creep;
import com.sylvan.presence.util.Algorithms;
import com.sylvan.presence.util.JsonFile;

public class Presence implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final String MOD_ID = "presence";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static JsonFile config;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.info("Presence loading...");

		initConfig();
		Events.registerEvents();
		Commands.registerCommands();
		Entities.registerEntities();

		LOGGER.info("Presence loaded.");
	}

	public static void initConfig() {
		// Load/create config file
		config = new JsonFile(FabricLoader.getInstance().getConfigDir().toString() + "/" + MOD_ID + ".json");

		// Load config variables
		PlayerData.loadConfig();
		Algorithms.loadConfig();
		AmbientSounds.loadConfig();
		Attack.loadConfig();
		ChatMessage.loadConfig();
		Creep.loadConfig();
		ExtinguishTorches.loadConfig();
		FlickerDoor.loadConfig();
		FlowerGift.loadConfig();
		Footsteps.loadConfig();
		Freeze.loadConfig();
		NearbySounds.loadConfig();
		OpenChest.loadConfig();
		OpenDoor.loadConfig();
		Stalk.loadConfig();
		SubtitleWarning.loadConfig();
		TrampleCrops.loadConfig();
	}
}
