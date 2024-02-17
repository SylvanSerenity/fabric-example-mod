package com.sylvan.event;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.sylvan.Presence;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class NearbySounds {
	private static final List<Map.Entry<SoundEvent, Float>> nearbySounds = new ArrayList<>();
	private static float totalWeight = 1.0f;

	public static void initEvent() {
		nearbySounds.addAll(List.of(
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_ITEM_PICKUP, Presence.config.nearbySoundsItemPickupWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_BIG_FALL, Presence.config.nearbySoundsBigFallWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_SMALL_FALL, Presence.config.nearbySoundsSmallFallWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_GENERIC_EAT, Presence.config.nearbySoundsEatWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_BREATH, Presence.config.nearbySoundsBreathWeight)
		));
		totalWeight = nearbySounds.stream().map(entry -> entry.getValue()).reduce(0.0f, Float::sum);
	}

	public static void scheduleEvent(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				playNearbySound(player);
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

		for (Map.Entry<SoundEvent, Float> entry : nearbySounds) {
			randomValue -= entry.getValue();
			if (randomValue <= 0.0f) {
				return entry.getKey();
			}
		}

		// This should not happen unless the list is empty or total weight is 0
		return SoundEvents.ENTITY_FOX_SCREECH;
	}

	public static void playNearbySound(final PlayerEntity player) {
		if (!player.isRemoved()) {
			player.getWorld().playSound(null, player.getBlockPos(), getRandomSound(), SoundCategory.PLAYERS);
		}
	}
}
