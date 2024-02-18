package com.sylvan.presence.util;

import java.util.List;
import java.util.Map;

import com.sylvan.presence.Presence;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class Algorithms {
	public static final Random RANDOM = Random.create();

	private static int algorithmsCaveDetectionRays = 50;				// The amount of rays to shoot in random directions to determine whether an entity is in a cave
	private static float algorithmsCaveDetectionMaxNonCaveaveBlockPercent = 0.1f;	// The percent of blocks a cave detection ray can collide with that are not usually found in a cave before assuming player is in a base

	public static void loadConfig() {
		try {
			algorithmsCaveDetectionRays = Presence.config.getOrSetValue("algorithmsCaveDetectionRays", algorithmsCaveDetectionRays).getAsInt();
			algorithmsCaveDetectionMaxNonCaveaveBlockPercent = Presence.config.getOrSetValue("algorithmsCaveDetectionMaxNonCaveaveBlockPercent", algorithmsCaveDetectionMaxNonCaveaveBlockPercent).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Algorithms.java. Wiping and using default.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static float randomBetween(final float min, final float max) {
		return min + RANDOM.nextFloat() * (max - min);
	}

	public static int divideByFloat(final int dividend, final float divisor) {
		return (int) (((float) dividend) / Math.max(0.001f, divisor));
	}

	public static <K> K randomKeyFromWeightMap(final Map<K, Float> keyWeightMap) {
		final float totalWeight = keyWeightMap.values().stream().reduce(0.0f, Float::sum);
		float randomValue = RANDOM.nextFloat() * totalWeight;

		for (final K key : keyWeightMap.keySet()) {
			randomValue -= keyWeightMap.get(key);
			if (randomValue <= 0.0f) {
				return key;
			}
		}

		// This should not happen unless the list is empty or total weight is 0
		return keyWeightMap.keySet().iterator().next();
	}

	public static Identifier getIdentifierFromString(final String identifier) {
		String namespace, name;
		String[] parts = identifier.split(":", 2);
		if (parts.length == 2) {
			namespace = parts[0];
			name = parts[1];
		} else if (parts.length == 1) {
			namespace = "minecraft";
			name = parts[0];
		} else {
			Presence.LOGGER.warn("Invalid sound key \"" + identifier + "\".");
			return null;
		}
		return new Identifier(namespace, name);
	}

	public static HitResult castRayFromEyeToBlock(final Entity entity, final BlockPos blockPos) {
		return entity.getWorld().raycast(
			new RaycastContext(
				entity.getEyePos(),
				blockPos.toCenterPos(),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				entity
			)
		);
	}

	public static HitResult.Type castRayFromEyeToEye(final Entity entity1, final Entity entity2) {
		if (entity1.getEntityWorld().getDimension() != entity2.getEntityWorld().getDimension()) {
			return HitResult.Type.MISS;
		}
		return entity1.getWorld().raycast(
			new RaycastContext(
				entity1.getEyePos(),
				entity2.getEyePos(),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				entity1
			)
		).getType();
	}

	public static boolean canBlockBeSeenByPlayers(final List<? extends PlayerEntity> players, final BlockPos blockPos) {
		for (PlayerEntity player : players) {
			if (!player.getBlockPos().isWithinDistance(blockPos, 128.0)) continue;
			if (castRayFromEyeToBlock(player, blockPos).getType() == HitResult.Type.MISS) return true;
		}
		return false;
	}

	public static boolean canPlayerStandOnBlock(final World world, final BlockPos blockPos) {
		return (
			!world.getBlockState(blockPos).isAir() &&
			world.getBlockState(blockPos.up()).isAir() &&
			world.getBlockState(blockPos.up(2)).isAir()
		);
	}

	public static BlockPos getNearestStandableBlockPos(final World world, BlockPos blockPos, final int minY, final int maxY) {
		while (!canPlayerStandOnBlock(world, blockPos) && (blockPos.getY() > minY)) {
			blockPos = blockPos.down();
		}
		while (!canPlayerStandOnBlock(world, blockPos) && (blockPos.getY() < maxY)) {
			blockPos = blockPos.up();
		}
		return blockPos;
	}

	public static BlockPos getNearestStandableBlockPosTowardsEntity(final Entity entity, BlockPos blockPos, final int minY, final int maxY) {
		final World world = entity.getWorld();
		final BlockPos entityPos = entity.getBlockPos();
		if (blockPos.getY() > entityPos.getY()) {
			// Above player, try moving down
			while (!canPlayerStandOnBlock(world, blockPos) && (blockPos.getY() > minY)) {
				blockPos = blockPos.down();
			}
		} else {
			// Below player, try moving up
			while (!canPlayerStandOnBlock(world, blockPos) && (blockPos.getY() < maxY)) {
				blockPos = blockPos.up();
			}
		}
		return blockPos;
	}

	public static Vec3d getRandomDirection() {
		return new Vec3d(
			RANDOM.nextDouble() * 2 - 1,
			RANDOM.nextDouble() * 2 - 1,
			RANDOM.nextDouble() * 2 - 1
		).normalize();
	}

	public static Vec3d getBlockDirectionFromEntity(final BlockPos blockPos, final Entity entity) {
		final Vec3d entityPos = entity.getPos();
		return new Vec3d(
			blockPos.getX() - entityPos.getX(),
			blockPos.getY() - entityPos.getY(),
			blockPos.getZ() - entityPos.getZ()
		).normalize();
	}

	public static BlockPos getRandomBlockNearEntity(final Entity entity, final int distanceMin, final int distanceMax) {
		final Vec3d randomDirection = getRandomDirection();
		final int distance = RANDOM.nextBetween(distanceMin, distanceMax);

		// Scale the direction vector by the random distance
		final Vec3d randomOffset = randomDirection.multiply(distance);
		final BlockPos entityPos = entity.getBlockPos();
		return entityPos.add(
			(int) Math.floor(randomOffset.getX()),
			(int) Math.floor(randomOffset.getY()),
			(int) Math.floor(randomOffset.getZ())
		);
	}

	public static BlockPos getRandomStandableBlockNearEntity(final Entity entity, final int distanceMin, final int distanceMax, final int maxAttempts) {
		final BlockPos entityPos = entity.getBlockPos();
		final int moveDistance = Math.max(distanceMin, distanceMax - distanceMin);
		final int maxDistanceDown = entityPos.getY() - moveDistance;
		final int maxDistanceUp = entityPos.getY() + moveDistance;

		// Start with random block and check maxAttempt times
		BlockPos blockPos = getRandomBlockNearEntity(entity, distanceMin, distanceMax);
		for (int i = 0; i < maxAttempts; ++i) {
			// Move to nearest standable block
			blockPos = getNearestStandableBlockPosTowardsEntity(entity, blockPos, maxDistanceDown, maxDistanceUp);
			// Return if blockPos is within constraints
			if (!blockPos.isWithinDistance(entityPos, distanceMin) && blockPos.isWithinDistance(entityPos, distanceMax)) return blockPos;
			// Try again
			blockPos = getRandomBlockNearEntity(entity, distanceMin, distanceMax);
		}

		// If nothing is found in 50 attempts, just select a block in the wall
		return blockPos;
	}

	public static boolean isEntityInCave(final Entity entity) {
		final World world = entity.getEntityWorld();
		if (world.getLightLevel(LightType.SKY, entity.getBlockPos()) > 0) return false;

		// Cast rays in random directions. If they all hit, the sky cannot be seen.
		int nonCaveBlockCount = 0;
		HitResult hit;
		BlockSoundGroup hitBlockSound;
		for (int i = 0; i < algorithmsCaveDetectionRays; ++i) {
			hit = castRayFromEyeToBlock(entity, getRandomBlockNearEntity(entity, 128, 128));
			if (hit.getType() == HitResult.Type.MISS) return false;
			hitBlockSound = world.getBlockState(
				new BlockPos(
					(int) hit.getPos().getX(),
					(int) hit.getPos().getY(),
					(int) hit.getPos().getZ()
				)
			).getSoundGroup();
			if (
				hitBlockSound != BlockSoundGroup.STONE &&
				hitBlockSound != BlockSoundGroup.DEEPSLATE &&
				hitBlockSound != BlockSoundGroup.GRAVEL &&
				hitBlockSound != BlockSoundGroup.DRIPSTONE_BLOCK &&
				hitBlockSound != BlockSoundGroup.POINTED_DRIPSTONE &&
				hitBlockSound != BlockSoundGroup.ROOTED_DIRT &&
				hitBlockSound != BlockSoundGroup.TUFF
			) ++nonCaveBlockCount;
		}

		// If over 5% of hit blocks are not normally found in a cave, assume player is in a base
		if (((float) nonCaveBlockCount / (float) Math.max(1, algorithmsCaveDetectionRays)) > algorithmsCaveDetectionMaxNonCaveaveBlockPercent) return false;
		return true;
	}
}
