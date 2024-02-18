package com.sylvan.presence.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.sylvan.presence.Presence;
import com.sylvan.presence.event.AmbientSounds;
import com.sylvan.presence.event.ExtinguishTorches;
import com.sylvan.presence.event.Footsteps;
import com.sylvan.presence.event.NearbySounds;
import com.sylvan.presence.util.Algorithms;
import com.sylvan.presence.util.JsonFile;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public class PlayerData {
	public static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private static MinecraftServer server;

	// Config
	public static float defaultHauntChance = 0.1f;	// Default chance of being haunted when joining the server. Range: [0.0, 1.0]
	private static float defaultHauntLevel = 1.0f;	// The default haunt level of each player

	public static void addPlayerData(final UUID uuid) {
		playerDataMap.put(uuid, new PlayerData(uuid));
	}

	public static PlayerData getPlayerData(final UUID uuid) {
		if (playerDataMap.containsKey(uuid)) return playerDataMap.get(uuid);
		else return new PlayerData(uuid);
	}

	public static void loadConfig() {
		try {
			defaultHauntChance = Presence.config.getOrSetValue("defaultHauntChance", defaultHauntChance).getAsFloat();
			defaultHauntLevel = Presence.config.getOrSetValue("defaultHauntLevel", defaultHauntLevel).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for PlayerData.java. Wiping and using default.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void setInstance(final MinecraftServer minecraftServer) {
		server = minecraftServer;
	}

	// Instance variables
	private UUID uuid;
	private String playerDataPath;

	private float hauntChance = defaultHauntChance;	// Chance of being haunted when joining the server
	private float hauntLevel = defaultHauntLevel;	// Divides delay minima and maxima by hauntLevel, such that events happen more often as time goes on. 1.0 has no effect, and larger numbers increase events
	private boolean isHaunted = false;		// Whether the player is haunted and should be subject to events

	private PlayerData(final UUID uuid) {
		this.uuid = uuid;
		playerDataPath = server.getSavePath(WorldSavePath.ROOT).toString() + "/presence/" + uuid.toString();

		load();

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
		return server.getPlayerManager().getPlayer(uuid);
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

	public float calculateHauntChance() {
		// TODO If haunted, reduce, otherwise, increase
		return hauntChance;
	}

	public void scheduleEvents() {
		final PlayerEntity player = getPlayer();
		if (AmbientSounds.ambientSoundsEnabled) AmbientSounds.scheduleEvent(player);
		if (ExtinguishTorches.extinguishTorchesEnabled) ExtinguishTorches.scheduleTracking(player);
		if (Footsteps.footstepsEnabled) Footsteps.scheduleEvent(player);
		if (NearbySounds.nearbySoundsEnabled) NearbySounds.scheduleEvent(player);
	}

	private void load() {
		final JsonFile dataFile = new JsonFile(playerDataPath);
		try {
			hauntChance = dataFile.getOrSetValue("hauntChance", hauntChance).getAsFloat();
			hauntLevel = dataFile.getOrSetValue("hauntLevel", hauntLevel).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Corrupted player data for " + uuid.toString() + ". Wiping and using default values.", e);
			dataFile.wipe();
		}
		dataFile.writeJson(dataFile.getJsonObject(), false);
	}

	public void save() {
		final JsonFile dataFile = new JsonFile(playerDataPath);
		final JsonObject data = dataFile.getJsonObject();
		dataFile.setValue(data, "hauntChance", hauntChance);
		dataFile.setValue(data, "hauntLevel", hauntLevel);
		dataFile.writeJson(data, false);
	}
}
