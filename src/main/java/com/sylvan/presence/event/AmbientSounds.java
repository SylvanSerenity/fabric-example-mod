package com.sylvan.presence.event;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class AmbientSounds {
	private static final List<Map.Entry<SoundEvent, Float>> ambientSounds = new ArrayList<>();

	// Config
	protected static boolean ambientSoundsEnabled = true;
	private static int ambientSoundsDelayMin = 60 * 45;
	private static int ambientSoundsDelayMax = 60 * 60 * 2;
	private static int ambientSoundsRetryDelay = 60;
	private static int ambientSoundsLightLevelMax = 7;
	private static float ambientSoundsCaveWeight = 95.0f;
	private static float ambientSoundsUnderwaterRareWeight = 4.5f;
	private static float ambientSoundsUnderwaterUltraRareWeight = 0.5f;
	private static float ambientSoundsPitchMin = 0.5f;
	private static float ambientSoundsPitchMax = 1.0f;

	private static void loadConfig() {
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
		loadConfig();
		ambientSounds.addAll(List.of(
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_CAVE.value(), ambientSoundsCaveWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, ambientSoundsUnderwaterRareWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, ambientSoundsUnderwaterUltraRareWeight)
		));
	}

	public static void scheduleEvent(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				playAmbientSound(player);
				if (!player.isRemoved()) scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				ambientSoundsDelayMin,
				ambientSoundsDelayMax
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
		}

		final float pitch = Algorithms.randomBetween(ambientSoundsPitchMin, ambientSoundsPitchMax);
		final SoundEvent sound = Algorithms.randomKeyFromWeightMap(ambientSounds);
		player.playSound(sound, SoundCategory.AMBIENT, 256.0f, pitch);
	}
}
