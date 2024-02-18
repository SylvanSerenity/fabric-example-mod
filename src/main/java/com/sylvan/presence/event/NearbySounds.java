package com.sylvan.presence.event;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NearbySounds {
	private static final Map<SoundEvent, Float> nearbySounds = new HashMap<>();

	// Config
	public static boolean nearbySoundsEnabled = true;			// Whether the naerby sounds event is active
	private static int nearbySoundsDelayMin = 60 * 5;			// The minimum delay between nearby sounds events
	private static int nearbySoundsDelayMax = 60 * 60 * 3;			// The maximum delay between nearby sounds events
	private static int nearbySoundsDistanceMin = 12;			// The minimum distance of the sound from the player. 12 gives a distant-feeling sond to where it is often barely noticeable
	private static int nearbySoundsDistanceMax = 16;			// The maximum distance of the sound from the player. 16 is maximum distance to hear sounds
	private static JsonObject nearbySoundsSoundWeights = new JsonObject();	// A set of sound ID keys with weight values to play during the event

	public static void loadConfig() {
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_ITEM_PICKUP.getId().toString(), 45.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_SMALL_FALL.getId().toString(), 30.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_BIG_FALL.getId().toString(), 15.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_HURT.getId().toString(), 1.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_GENERIC_EAT.getId().toString(), 8.5f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_BREATH.getId().toString(), 0.5f);

		try {
			nearbySoundsEnabled = Presence.config.getOrSetValue("nearbySoundsEnabled", nearbySoundsEnabled).getAsBoolean();
			nearbySoundsDelayMin = Presence.config.getOrSetValue("nearbySoundsDelayMin", nearbySoundsDelayMin).getAsInt();
			nearbySoundsDelayMax = Presence.config.getOrSetValue("nearbySoundsDelayMax", nearbySoundsDelayMax).getAsInt();
			nearbySoundsDistanceMin = Presence.config.getOrSetValue("nearbySoundsDistanceMin", nearbySoundsDistanceMin).getAsInt();
			nearbySoundsDistanceMax = Presence.config.getOrSetValue("nearbySoundsDistanceMax", nearbySoundsDistanceMax).getAsInt();
			nearbySoundsSoundWeights = Presence.config.getOrSetValue("nearbySoundsSoundWeights", nearbySoundsSoundWeights).getAsJsonObject();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for NearbySounds.java. Wiping and using default default.", e);
			Presence.config.clearConfig();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		try {
			String key;
			for (Map.Entry<String, JsonElement> entry : nearbySoundsSoundWeights.entrySet()) {
				key = entry.getKey();
				final Identifier soundId = Algorithms.getIdentifierFromString(key);
				final SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
				if (sound == null) {
					Presence.LOGGER.warn("Could not find sound \"" + key + "\" in NearbySounds.java.");
					continue;
				}
				nearbySounds.put(sound, entry.getValue().getAsFloat());
			}
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for AmbientSounds.java. Wiping and using default default.", e);
			Presence.config.clearConfig();
			Presence.initConfig();
			Events.initEvents();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player.getUuid()).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				playNearbySound(player);
				if (!player.isRemoved()) scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(nearbySoundsDelayMin, hauntLevel),
				Algorithms.divideByFloat(nearbySoundsDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void playNearbySound(final PlayerEntity player) {
		if (player.isRemoved()) return;

		final World world = player.getWorld();
		final BlockPos soundPos = Algorithms.getRandomStandableBlockNearPlayer(player, nearbySoundsDistanceMin, nearbySoundsDistanceMax, 20);
		final SoundEvent sound = Algorithms.randomKeyFromWeightMap(nearbySounds);
		world.playSound(null, soundPos, sound, SoundCategory.PLAYERS);
	}
}
