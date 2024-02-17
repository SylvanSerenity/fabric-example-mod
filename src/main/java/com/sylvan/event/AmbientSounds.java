package com.sylvan.event;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.sylvan.Presence;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class AmbientSounds {
	private static final List<Map.Entry<SoundEvent, Float>> ambientSounds = new ArrayList<>();
	private static float totalWeight = 1.0f;

	public static void initEvent() {
		ambientSounds.addAll(List.of(
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_CAVE.value(), Presence.config.ambientSoundsCaveWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, Presence.config.ambientSoundsUnderwaterRareWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, Presence.config.ambientSoundsUnderwaterUltraRareWeight)
		));
		totalWeight = ambientSounds.stream().map(entry -> entry.getValue()).reduce(0.0f, Float::sum);
	}

	public static void scheduleEvent(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				playAmbientSound(player);
				scheduleEvent(player);
			},
			Presence.RANDOM.nextBetween(
				Presence.config.nearbySoundsDelayMin,
				Presence.config.nearbySoundsDelayMax
			), TimeUnit.SECONDS
		);
	}

	public static SoundEvent getRandomSound() {
		float randomValue = Presence.RANDOM.nextFloat() * totalWeight;

		for (Map.Entry<SoundEvent, Float> entry : ambientSounds) {
			randomValue -= entry.getValue();
			if (randomValue <= 0.0f) {
				return entry.getKey();
			}
		}

		// This should not happen unless the list is empty or total weight is 0
		return SoundEvents.ENTITY_FOX_SCREECH;
	}

	public static void playAmbientSound(final PlayerEntity player) {
		if (!player.isRemoved()) {
			final float pitch = Presence.config.ambientSoundsPitchMin + Presence.RANDOM.nextFloat() * (Presence.config.ambientSoundsPitchMax - Presence.config.ambientSoundsPitchMin);
			player.playSound(getRandomSound(), SoundCategory.AMBIENT, 128.0f, pitch);
		}
	}
}
