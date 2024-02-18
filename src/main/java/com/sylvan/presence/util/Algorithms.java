package com.sylvan.presence.util;

import java.util.List;
import java.util.Map;

import com.sylvan.presence.Presence;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class Algorithms {
	public static final Random RANDOM = Random.create();

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

	public static boolean canBlockBeSeen(final List<? extends PlayerEntity> players, final BlockPos blockPos) {
		for (PlayerEntity player : players) {
			Vec3d vec3d = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
			Vec3d vec3d2 = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			if (vec3d2.distanceTo(vec3d) > 128.0) continue;
			if (player.getWorld().raycast(
					new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player)
				).getType() != HitResult.Type.BLOCK
			) return true;
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

	public static BlockPos getNearestStandableBlockPosTowardsPlayer(final PlayerEntity player, BlockPos blockPos, final int minY, final int maxY) {
		final World world = player.getWorld();
		final BlockPos playerPos = player.getBlockPos();
		if (blockPos.getY() > playerPos.getY()) {
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

	public static Vec3d getDirectionFromPlayer(final BlockPos blockPos, final PlayerEntity player) {
		final Vec3d playerPos = player.getPos();
		return new Vec3d(
			blockPos.getX() - playerPos.getX(),
			blockPos.getY() - playerPos.getY(),
			blockPos.getZ() - playerPos.getZ()
		).normalize();
	}

	public static BlockPos getRandomBlockNearPlayer(final PlayerEntity player, final int distanceMin, final int distanceMax) {
		final Vec3d randomDirection = getRandomDirection();
		final int distance = RANDOM.nextBetween(distanceMin, distanceMax);

		// Scale the direction vector by the random distance
		final Vec3d randomOffset = randomDirection.multiply(distance);
		final BlockPos playerPos = player.getBlockPos();
		return new BlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ()).add(
			(int) Math.floor(randomOffset.getX()),
			(int) Math.floor(randomOffset.getY()),
			(int) Math.floor(randomOffset.getZ())
		);
	}

	public static BlockPos getRandomStandableBlockNearPlayer(final PlayerEntity player, final int distanceMin, final int distanceMax, final int maxAttempts) {
		final BlockPos playerPos = player.getBlockPos();
		final int moveDistance = Math.max(distanceMin, distanceMax - distanceMin);
		final int maxDistanceDown = playerPos.getY() - moveDistance;
		final int maxDistanceUp = playerPos.getY() + moveDistance;

		// Start with random block and check maxAttempt times
		BlockPos blockPos = getRandomBlockNearPlayer(player, distanceMin, distanceMax);
		for (int i = 0; i < maxAttempts; ++i) {
			// Move to nearest standable block
			blockPos = getNearestStandableBlockPosTowardsPlayer(player, blockPos, maxDistanceDown, maxDistanceUp);
			// Return if blockPos is within constraints
			if (!blockPos.isWithinDistance(playerPos, distanceMin) && blockPos.isWithinDistance(playerPos, distanceMax)) return blockPos;
			// Try again
			blockPos = getRandomBlockNearPlayer(player, distanceMin, distanceMax);
		}

		// If nothing is found in 50 attempts, just select a block in the wall
		return blockPos;
	}
}
