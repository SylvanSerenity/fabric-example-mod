package com.sylvan.presence.data;

import java.time.Duration;
import java.time.LocalDateTime;
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
	private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private static MinecraftServer server;
	private static String playerDataDirectory;

	// Config
	public static float defaultHauntChance = 0.1f;			// Default chance of being haunted when joining the server. Range: [0.0, 1.0]
	private static float defaultHauntLevel = 1.0f;			// The default haunt level of each player
	private static float hauntChanceMaxBeforeDecrease = 0.75f;	// The maximum haunt chance before haunted players start getting their haunt chance reduced based on play time
	private static int minutesToFullyReduceHauntChance = 600;	// The number of minutes that would reduce haunt chance by 100%

	public static PlayerData addPlayerData(final PlayerEntity player) {
		final PlayerData playerData = new PlayerData(player);
		playerDataMap.put(player.getUuid(), playerData);
		return playerData;
	}

	public static PlayerData getPlayerData(final PlayerEntity player) {
		if (playerDataMap.containsKey(player.getUuid())) return playerDataMap.get(player.getUuid());
		else return new PlayerData(player);
	}

	public static void loadConfig() {
		try {
			defaultHauntChance = Presence.config.getOrSetValue("defaultHauntChance", defaultHauntChance).getAsFloat();
			defaultHauntLevel = Presence.config.getOrSetValue("defaultHauntLevel", defaultHauntLevel).getAsFloat();
			hauntChanceMaxBeforeDecrease = Presence.config.getOrSetValue("hauntChanceMaxBeforeDecrease", hauntChanceMaxBeforeDecrease).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for PlayerData.java. Wiping and using default.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void setInstance(final MinecraftServer minecraftServer) {
		server = minecraftServer;
		playerDataDirectory = server.getSavePath(WorldSavePath.ROOT).toString() + "/presence/playerdata/";
	}

	public static boolean hasInstance() {
		return server != null && server.isRunning();
	}

	// Instance
	private UUID uuid;
	private PlayerEntity player;
	private String playerDataPath;
	private LocalDateTime joinTime;
	private boolean isHaunted = false;

	// Persistant
	private float hauntChance = defaultHauntChance;	// Chance of being haunted when joining the server
	private float hauntLevel = defaultHauntLevel;	// Divides delay minima and maxima by hauntLevel, such that events happen more often as time goes on. 1.0 has no effect, and larger numbers increase events
	private long playTime = 0;			// Time in minutes that the player has played

	private PlayerData(final PlayerEntity playerEntity) {
		player = playerEntity;
		uuid = playerEntity.getUuid();
		playerDataPath = playerDataDirectory + uuid.toString();
		joinTime = LocalDateTime.now();
		load();
		rollHauntChance();
	}

	public void startEvents() {
		if (isHaunted) {
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
		if (player == null) player = server.getPlayerManager().getPlayer(uuid);
		return player;
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
		final float difference = ((float) (playTime + Duration.between(joinTime, LocalDateTime.now()).toMinutes())) / Math.max(1.0f, minutesToFullyReduceHauntChance);
		if (isHaunted && hauntChance > hauntChanceMaxBeforeDecrease) {
			// Decrease haunt chace
			hauntChance = Math.max(
				defaultHauntChance,
				Math.min(1.0f, hauntChance - difference)
			);
		} else {
			// Incraese haunt chace
			hauntChance = Math.max(
				defaultHauntChance,
				Math.min(1.0f, hauntChance + difference)
			);
		}
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
			playTime = dataFile.getOrSetValue("playTime", playTime).getAsLong();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Corrupted player data for " + uuid.toString() + ". Wiping and using default values.", e);
			dataFile.wipe();
		}
		dataFile.writeJson(dataFile.getJsonObject(), false);
	}

	public void remove() {
		calculateHauntChance();
		save();
		playerDataMap.remove(uuid);
	}

	public void save() {
		final JsonFile dataFile = new JsonFile(playerDataPath);
		final JsonObject data = dataFile.getJsonObject();
		dataFile.setValue(data, "hauntChance", hauntChance);
		dataFile.setValue(data, "hauntLevel", hauntLevel);
		dataFile.setValue(data, "playTime", playTime + Duration.between(joinTime, LocalDateTime.now()).toMinutes());
		dataFile.writeJson(data, false);
	}
}
