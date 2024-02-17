package com.sylvan.event;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.sylvan.Presence;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

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

	public static BlockPos getRandomSourceBlock(final PlayerEntity player) {
		// Generate random direction and distance
		final Vec3d randomDirection = new Vec3d(
			Presence.RANDOM.nextDouble() * 2 - 1,
			Presence.RANDOM.nextDouble() * 2 - 1,
			Presence.RANDOM.nextDouble() * 2 - 1
		).normalize();
		final int distance = Presence.RANDOM.nextBetween(Presence.config.nearbySoundsDistanceMin, Presence.config.nearbySoundsDistanceMax);

		// Scale the direction vector by the random distance
		Vec3d randomOffset = randomDirection.multiply(distance);

		// Add the random offset to the player's position to get a random nearby position
		final BlockPos playerPos = player.getBlockPos();
		Vec3i randomNearbyPos = new Vec3i(playerPos.getX(), playerPos.getY(), playerPos.getZ()).add(
			(int) Math.floor(randomOffset.getX()),
			(int) Math.floor(randomOffset.getY()),
			(int) Math.floor(randomOffset.getZ())
		);

		// Ensure the position is within the world bounds
		final World world = player.getWorld();

		// Find the top solid block
		BlockPos soundPos = new BlockPos(randomNearbyPos);
		while (world.getBlockState(soundPos).isAir() && soundPos.getY() > (playerPos.getY() - Presence.config.nearbySoundsDistanceMax)) {
			soundPos = soundPos.down();
		}
		while (world.getBlockState(soundPos).isAir() && soundPos.getY() < (playerPos.getY() + Presence.config.nearbySoundsDistanceMax)) {
			soundPos = soundPos.up();
		}

		return soundPos;
	}

	public static void playNearbySound(final PlayerEntity player) {
		if (!player.isRemoved()) {
			player.getWorld().playSound(null, getRandomSourceBlock(player), getRandomSound(), SoundCategory.PLAYERS);
		}
	}
}
