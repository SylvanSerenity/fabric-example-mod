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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NearbySounds {
	private static final List<Map.Entry<SoundEvent, Float>> nearbySounds = new ArrayList<>();

	// Config
	private static int nearbySoundsDelayMin = 60 * 45;
	private static int nearbySoundsDelayMax = 60 * 60 * 2;
	private static int nearbySoundsDistanceMin = 5;
	private static int nearbySoundsDistanceMax = 15;
	private static float nearbySoundsItemPickupWeight = 45.0f;
	private static float nearbySoundsBigFallWeight = 15.0f;
	private static float nearbySoundsSmallFallWeight = 30.0f;
	private static float nearbySoundsEatWeight = 9.5f;
	private static float nearbySoundsBreathWeight = 0.5f;

	private static void loadConfig() {
		nearbySoundsDelayMin = Presence.config.getOrSetValue("nearbySoundsDelayMin", nearbySoundsDelayMin);
		nearbySoundsDelayMax = Presence.config.getOrSetValue("nearbySoundsDelayMax", nearbySoundsDelayMax);
		nearbySoundsDistanceMin = Presence.config.getOrSetValue("nearbySoundsDistanceMin", nearbySoundsDistanceMin);
		nearbySoundsDistanceMax = Presence.config.getOrSetValue("nearbySoundsDistanceMax", nearbySoundsDistanceMax);
		nearbySoundsItemPickupWeight = Presence.config.getOrSetValue("nearbySoundsItemPickupWeight", nearbySoundsItemPickupWeight);
		nearbySoundsBigFallWeight = Presence.config.getOrSetValue("nearbySoundsBigFallWeight", nearbySoundsBigFallWeight);
		nearbySoundsSmallFallWeight = Presence.config.getOrSetValue("nearbySoundsSmallFallWeight", nearbySoundsSmallFallWeight);
		nearbySoundsEatWeight = Presence.config.getOrSetValue("nearbySoundsEatWeight", nearbySoundsEatWeight);
		nearbySoundsBreathWeight = Presence.config.getOrSetValue("nearbySoundsBreathWeight", nearbySoundsBreathWeight);
	}

	public static void initEvent() {
		loadConfig();
		nearbySounds.addAll(List.of(
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_ITEM_PICKUP, nearbySoundsItemPickupWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_BIG_FALL, nearbySoundsBigFallWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_SMALL_FALL, nearbySoundsSmallFallWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_GENERIC_EAT, nearbySoundsEatWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_BREATH, nearbySoundsBreathWeight)
		));
	}

	public static void scheduleEvent(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				playNearbySound(player);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				nearbySoundsDelayMin,
				nearbySoundsDelayMax
			), TimeUnit.SECONDS
		);
	}

	public static void playNearbySound(final PlayerEntity player) {
		if (!player.isRemoved()) {
			final World world = player.getWorld();
			final BlockPos playerPos = player.getBlockPos();
			BlockPos soundPos = Algorithms.getRandomBlockNearPlayer(player, nearbySoundsDistanceMin, nearbySoundsDistanceMax);
			soundPos = Algorithms.getNearestStandableBlockPos(
				world,
				soundPos,
				playerPos.getY() - nearbySoundsDistanceMax,
				playerPos.getY() + nearbySoundsDistanceMax
			);
			final SoundEvent sound = Algorithms.randomKeyFromWeightMap(nearbySounds);
			world.playSound(null, soundPos, sound, SoundCategory.PLAYERS);
		}
	}
}
