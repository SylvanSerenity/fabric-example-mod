package com.sylvan.presence.event;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class AmbientSounds {
	private static final List<Map.Entry<SoundEvent, Float>> ambientSounds = new ArrayList<>();

	// Config
	public static boolean ambientSoundsEnabled = true;			// Whether the ambient sounds event is active
	private static int ambientSoundsDelayMin = 60 * 30;			// The minimum delay between ambient sounds
	private static int ambientSoundsDelayMax = 60 * 60 * 2;			// The maximum delay between ambient sounds
	private static int ambientSoundsRetryDelay = 60;			// The delay between tries to play ambient sound
	private static int ambientSoundsLightLevelMax = 7;			// The maximum light level to play ambient sound (so that it is dark)
	private static float ambientSoundsCaveWeight = 95.0f;			// The weight of the cave sound
	private static float ambientSoundsUnderwaterRareWeight = 4.5f;		// The weight of the underwater beast sound
	private static float ambientSoundsUnderwaterUltraRareWeight = 0.5f;	// The weight of the underwater leviathan sound
	private static float ambientSoundsPitchMin = 0.5f;			// The minimum sound pitch (so that it is slow and darker sounding)
	private static float ambientSoundsPitchMax = 1.0f;			// The maximum sound pitch

	public static void loadConfig() {
		try {
			ambientSoundsEnabled = Presence.config.getOrSetValue("ambientSoundsEnabled", ambientSoundsEnabled).getAsBoolean();
			ambientSoundsDelayMin = Presence.config.getOrSetValue("ambientSoundsDelayMin", ambientSoundsDelayMin).getAsInt();
			ambientSoundsDelayMax = Presence.config.getOrSetValue("ambientSoundsDelayMax", ambientSoundsDelayMax).getAsInt();
			ambientSoundsRetryDelay = Presence.config.getOrSetValue("ambientSoundsRetryDelay", ambientSoundsRetryDelay).getAsInt();
			ambientSoundsLightLevelMax = Presence.config.getOrSetValue("ambientSoundsLightLevelMax", ambientSoundsLightLevelMax).getAsInt();
			ambientSoundsCaveWeight = Presence.config.getOrSetValue("ambientSoundsCaveWeight", ambientSoundsCaveWeight).getAsFloat();
			ambientSoundsUnderwaterRareWeight = Presence.config.getOrSetValue("ambientSoundsUnderwaterRareWeight", ambientSoundsUnderwaterRareWeight).getAsFloat();
			ambientSoundsUnderwaterUltraRareWeight = Presence.config.getOrSetValue("ambientSoundsUnderwaterUltraRareWeight", ambientSoundsUnderwaterUltraRareWeight).getAsFloat();
			ambientSoundsPitchMin = Presence.config.getOrSetValue("ambientSoundsPitchMin", ambientSoundsPitchMin).getAsFloat();
			ambientSoundsPitchMax = Presence.config.getOrSetValue("ambientSoundsPitchMax", ambientSoundsPitchMax).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for AmbientSounds.java. Wiping and using default default.", e);
			Presence.config.clearConfig();
			Events.initEvents();
		}
	}

	public static void initEvent() {
		ambientSounds.addAll(List.of(
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_CAVE.value(), ambientSoundsCaveWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, ambientSoundsUnderwaterRareWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, ambientSoundsUnderwaterUltraRareWeight)
		));
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player.getUuid()).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				playAmbientSound(player);
				if (!player.isRemoved()) scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(ambientSoundsDelayMin, hauntLevel),
				Algorithms.divideByFloat(ambientSoundsDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void playAmbientSound(final PlayerEntity player) {
		if (player.isRemoved()) return;

		// Player must be in darkness
		if (player.getWorld().getLightLevel(player.getBlockPos()) > ambientSoundsLightLevelMax) {
			// Retry if it is a bad time
			Events.scheduler.schedule(
				() -> {
					playAmbientSound(player);
				}, ambientSoundsRetryDelay, TimeUnit.SECONDS
			);
			return;
		}

		final float pitch = Algorithms.randomBetween(ambientSoundsPitchMin, ambientSoundsPitchMax);
		final SoundEvent sound = Algorithms.randomKeyFromWeightMap(ambientSounds);
		player.playSound(sound, SoundCategory.AMBIENT, 256.0f, pitch);
	}
}
