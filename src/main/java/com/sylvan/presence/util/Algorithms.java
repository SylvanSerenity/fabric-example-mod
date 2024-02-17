package com.sylvan.presence.util;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
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

	public static <K> K randomKeyFromWeightMap(final List<Map.Entry<K, Float>> keyWeightMap) {
		final float totalWeight = keyWeightMap.stream().map(entry -> entry.getValue()).reduce(0.0f, Float::sum);
		float randomValue = RANDOM.nextFloat() * totalWeight;

		for (int i = 0; i < keyWeightMap.size(); ++i) {
			randomValue -= keyWeightMap.get(i).getValue();
			if (randomValue <= 0.0f) {
				return keyWeightMap.get(i).getKey();
			}
		}

		// This should not happen unless the list is empty or total weight is 0
		return keyWeightMap.get(0).getKey();
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

	public static BlockPos getRandomStandableBlockNearPlayer(final PlayerEntity player, final int distanceMin, final int distanceMax) {
		final BlockPos playerPos = player.getBlockPos();
		final int moveDistance = distanceMax - distanceMin;
		final int maxDistanceDown = playerPos.getY() - moveDistance;
		final int maxDistanceUp = playerPos.getY() + moveDistance;

		// Start with random block
		BlockPos blockPos = getRandomBlockNearPlayer(player, distanceMin, distanceMax);
		// Move to nearest standable block
		blockPos = getNearestStandableBlockPosTowardsPlayer(player, blockPos, maxDistanceDown, maxDistanceUp);

		// Check if out of range
		final Vec3d direction = getDirectionFromPlayer(blockPos, player);
		if (blockPos.isWithinDistance(playerPos, distanceMin)) {
			// Move farther
			blockPos = blockPos.add(
				(int) (direction.getX() * moveDistance),
				(int) (direction.getY() * moveDistance),
				(int) (direction.getZ() * moveDistance)
			);
			blockPos = getNearestStandableBlockPosTowardsPlayer(
				player,
				blockPos,
				maxDistanceDown,
				maxDistanceUp
			);
		} else if (!blockPos.isWithinDistance(playerPos, distanceMax)) {
			// Move closer
			blockPos = blockPos.add(
				(int) (-direction.getX() * moveDistance),
				(int) (-direction.getY() * moveDistance),
				(int) (-direction.getZ() * moveDistance)
			);
			blockPos = getNearestStandableBlockPosTowardsPlayer(
				player,
				blockPos,
				maxDistanceDown,
				maxDistanceUp
			);
		}
		return blockPos;
	}
}
