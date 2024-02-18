package com.sylvan.presence.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sylvan.presence.Presence;
import com.sylvan.presence.event.AmbientSounds;
import com.sylvan.presence.event.ExtinguishTorches;
import com.sylvan.presence.event.Footsteps;
import com.sylvan.presence.event.NearbySounds;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;

public class PlayerData {
	public static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private static PlayerManager playerManager;

	// Config
	public static float defaultHauntChance = 0.5f;	// Default chance of being haunted when joining the server. Range: [0.0, 1.0]
	private static float defaultHauntLevel = 1.0f;	// The default haunt level of each player

	public static void addPlayerData(final UUID uuid) {
		playerDataMap.put(uuid, new PlayerData(uuid));
	}

	public static PlayerData getPlayerData(final UUID uuid) {
		if (playerDataMap.containsKey(uuid)) return playerDataMap.get(uuid);
		else return new PlayerData(uuid);
	}

	public static void savePlayerData(final UUID uuid) {
		// TODO Save haunt levels
	}

	public static void loadConfig() {
		try {
			defaultHauntChance = Presence.config.getOrSetValue("defaultHauntChance", defaultHauntChance).getAsFloat();
			defaultHauntLevel = Presence.config.getOrSetValue("defaultHauntLevel", defaultHauntLevel).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for PlayerData.java. Wiping and using default.", e);
			Presence.config.clearConfig();
			Presence.initConfig();
		}
	}

	public static void setPlayerManager(final PlayerManager manager) {
		playerManager = manager;
	}

	// Instance variables
	private UUID uuid;				// The player's unique identifier
	private float hauntChance = defaultHauntChance;	// Chance of being haunted when joining the server
	private float hauntLevel = defaultHauntLevel;	// Divides delay minima and maxima by hauntLevel, such that events happen more often as time goes on. 1.0 has no effect, and larger numbers increase events
	private boolean isHaunted = false;		// Whether the player is haunted and should be subject to events

	private PlayerData(final UUID uuid) {
		this.uuid = uuid;

		// TODO Load haunt level

		if (rollHauntChance()) {
			scheduleEvents();
		}
	}

	public float getHauntChance() {
		return hauntChance;
	}

	public float getHauntLevel() {
		return hauntLevel;
	}

	public PlayerEntity getPlayer() {
		return playerManager.getPlayer(uuid);
	}

	public boolean isHaunted() {
		return isHaunted;
	}

	public void setHaunted(boolean haunted) {
		isHaunted = haunted;
	}

	public boolean rollHauntChance() {
		isHaunted = Algorithms.RANDOM.nextFloat() <= hauntChance;
		return isHaunted;
	}

	public void scheduleEvents() {
		final PlayerEntity player = getPlayer();
		if (AmbientSounds.ambientSoundsEnabled) AmbientSounds.scheduleEvent(player);
		if (ExtinguishTorches.extinguishTorchesEnabled) ExtinguishTorches.scheduleTracking(player);
		if (Footsteps.footstepsEnabled) Footsteps.scheduleEvent(player);
		if (NearbySounds.nearbySoundsEnabled) NearbySounds.scheduleEvent(player);
	}
}
