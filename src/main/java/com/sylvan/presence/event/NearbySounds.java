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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NearbySounds {
	private static final List<Map.Entry<SoundEvent, Float>> nearbySounds = new ArrayList<>();

	// Config
	public static boolean nearbySoundsEnabled = true;		// Whether the naerby sounds event is active
	private static int nearbySoundsDelayMin = 60 * 5;		// The minimum delay between nearby sounds events
	private static int nearbySoundsDelayMax = 60 * 60 * 3;		// The maximum delay between nearby sounds events
	private static int nearbySoundsDistanceMin = 12;		// The minimum distance of the sound from the player. 12 gives a distant-feeling sond to where it is often barely noticeable
	private static int nearbySoundsDistanceMax = 16;		// The maximum distance of the sound from the player. 16 is maximum distance to hear sounds
	private static float nearbySoundsItemPickupWeight = 45.0f;	// The weight of the item pickup sound
	private static float nearbySoundsBigFallWeight = 15.0f;		// The weight of the big fall sound
	private static float nearbySoundsSmallFallWeight = 30.0f;	// The weight of the small fall/trip sound
	private static float nearbySoundsEatWeight = 9.5f;		// The weight of the eat sound
	private static float nearbySoundsBreathWeight = 0.5f;		// The weight of the breath sound

	public static void loadConfig() {
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
		nearbySounds.addAll(List.of(
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_ITEM_PICKUP, nearbySoundsItemPickupWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_BIG_FALL, nearbySoundsBigFallWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_SMALL_FALL, nearbySoundsSmallFallWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_GENERIC_EAT, nearbySoundsEatWeight),
			new AbstractMap.SimpleEntry<>(SoundEvents.ENTITY_PLAYER_BREATH, nearbySoundsBreathWeight)
		));
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
