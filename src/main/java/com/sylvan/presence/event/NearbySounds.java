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
	protected static boolean nearbySoundsEnabled = true;
	private static int nearbySoundsDelayMin = 60 * 15;
	private static int nearbySoundsDelayMax = 60 * 60 * 3;
	private static int nearbySoundsDistanceMin = 12; // 12 gives a distant-feeling sond to where it is often barely noticeable
	private static int nearbySoundsDistanceMax = 16; // 16 is maximum distance to hear sounds
	private static float nearbySoundsItemPickupWeight = 45.0f;
	private static float nearbySoundsBigFallWeight = 15.0f;
	private static float nearbySoundsSmallFallWeight = 30.0f;
	private static float nearbySoundsEatWeight = 9.5f;
	private static float nearbySoundsBreathWeight = 0.5f;

	private static void loadConfig() {
		try {
			nearbySoundsEnabled = Presence.config.getOrSetValue("nearbySoundsEnabled", nearbySoundsEnabled).getAsBoolean();
			nearbySoundsDelayMin = Presence.config.getOrSetValue("nearbySoundsDelayMin", nearbySoundsDelayMin).getAsInt();
			nearbySoundsDelayMax = Presence.config.getOrSetValue("nearbySoundsDelayMax", nearbySoundsDelayMax).getAsInt();
			nearbySoundsDistanceMin = Presence.config.getOrSetValue("nearbySoundsDistanceMin", nearbySoundsDistanceMin).getAsInt();
			nearbySoundsDistanceMax = Presence.config.getOrSetValue("nearbySoundsDistanceMax", nearbySoundsDistanceMax).getAsInt();
			nearbySoundsItemPickupWeight = Presence.config.getOrSetValue("nearbySoundsItemPickupWeight", nearbySoundsItemPickupWeight).getAsFloat();
			nearbySoundsBigFallWeight = Presence.config.getOrSetValue("nearbySoundsBigFallWeight", nearbySoundsBigFallWeight).getAsFloat();
			nearbySoundsSmallFallWeight = Presence.config.getOrSetValue("nearbySoundsSmallFallWeight", nearbySoundsSmallFallWeight).getAsFloat();
			nearbySoundsEatWeight = Presence.config.getOrSetValue("nearbySoundsEatWeight", nearbySoundsEatWeight).getAsFloat();
			nearbySoundsBreathWeight = Presence.config.getOrSetValue("nearbySoundsBreathWeight", nearbySoundsBreathWeight).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for NearbySounds.java. Wiping and using default default.", e);
			Presence.config.clearConfig();
			Events.initEvents();
		}
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
				if (!player.isRemoved()) scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				nearbySoundsDelayMin,
				nearbySoundsDelayMax
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
