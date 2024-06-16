package com.sylvan.presence.util;

import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

import com.sylvan.presence.Presence;

import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class Algorithms {
	public static final Random RANDOM = Random.create();
	public static final double EPSILON = 0.0000000001;

	// Config
	private static int algorithmsCaveDetectionRays = 30;				// The amount of rays to shoot in random directions to determine whether an entity is in a cave
	private static float algorithmsCaveDetectionMaxNonCaveBlockPercent = 0.0f;	// The percent of blocks a cave detection ray can collide with that are not usually found in a cave before assuming player is in a base

	public static void loadConfig() {
		try {
			algorithmsCaveDetectionRays = Presence.config.getOrSetValue("algorithmsCaveDetectionRays", algorithmsCaveDetectionRays).getAsInt();
			algorithmsCaveDetectionMaxNonCaveBlockPercent = Presence.config.getOrSetValue("algorithmsCaveDetectionMaxNonCaveBlockPercent", algorithmsCaveDetectionMaxNonCaveBlockPercent).getAsFloat();
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

	public static BlockPos getBlockPosFromVec3d(final Vec3d vec3d) {
		return new BlockPos(
			(int) Math.round(vec3d.x),
			(int) Math.round(vec3d.y),
			(int) Math.round(vec3d.z)
		);
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
		return Identifier.of(namespace, name);
	}

	public static HitResult castRayFromEye(final Entity entity, final Vec3d pos) {
		return entity.getEntityWorld().raycast(
			new RaycastContext(
				entity.getEyePos(),
				pos,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				entity
			)
		);
	}

	public static boolean couldPosBeSeenByEntity(final Entity entity, final Vec3d pos) {
		// Check max distance before calculating
		if (!entity.getBlockPos().isWithinDistance(pos, 127.0)) return false;

		// Check if behind transparent block
		final HitResult hitResult = castRayFromEye(entity, pos);
        return hitResult.getType() != HitResult.Type.BLOCK ||
                !entity.getEntityWorld().getBlockState(((BlockHitResult) hitResult).getBlockPos()).isOpaque();
    }

	public static boolean couldPosBeSeenByPlayers(final List<? extends PlayerEntity> players, final Vec3d pos) {
		for (final PlayerEntity player : players) {
			if (couldPosBeSeenByEntity(player, pos)) return true;
		}
		return false;
	}

	public static boolean couldBlockBeSeenByPlayers(final List<? extends PlayerEntity> players, final BlockPos pos) {
		Vec3d towardsPlayerDirection;
		BlockPos towardsPlayerPos;
		for (final PlayerEntity player : players) {
			// Move one block towards player to prevent the block itself from blocking raycast
			towardsPlayerDirection = getDirectionPosToPos(pos.toCenterPos(), player.getEyePos());

			towardsPlayerPos = pos.add(
				(int) Math.signum(towardsPlayerDirection.x),
				(int) Math.signum(towardsPlayerDirection.y),
				(int) Math.signum(towardsPlayerDirection.z)
			);

			if (couldPosBeSeenByEntity(player, towardsPlayerPos.toCenterPos())) return true;

			// Check if feet can see
			towardsPlayerDirection = getDirectionPosToPos(pos.toCenterPos(), player.getPos());
			towardsPlayerPos = pos.add(
					(int) Math.signum(towardsPlayerDirection.x),
					(int) Math.signum(towardsPlayerDirection.y),
					(int) Math.signum(towardsPlayerDirection.z)
			);
			if (couldPosBeSeenByEntity(player, towardsPlayerPos.toCenterPos())) return true;
		}
		return false;
	}

	public static boolean isPositionLookedAtByEntity(final Entity entity, final Vec3d pos, final double dotProductThreshold) {
		if (!couldPosBeSeenByEntity(entity, pos)) return false;
		final Vec3d lookingDirection = entity.getRotationVector(); // Where entity is actually looking
		final Vec3d lookAtDirection = getDirectionPosToPos(entity.getEyePos(), pos); // Where entity should be looking
		final double dotProduct = lookingDirection.dotProduct(lookAtDirection);
		return dotProduct > dotProductThreshold;
	}

	public static boolean couldPlayerStandOnBlock(final World world, final BlockPos blockPos) {
		// Check for opaque blocks
		if (
			!world.getBlockState(blockPos).isOpaque() ||
			world.getBlockState(blockPos.up()).isOpaque() ||
			world.getBlockState(blockPos.up(2)).isOpaque()
		) return false;

		// Cast ray from legs position
		Vec3d blockCenterPos = blockPos.up().toCenterPos();
		Vec3d outsideBlockPos = blockCenterPos.add(0.45, 0.45, 0.45);
		HitResult hitResult = world.raycast(
			new RaycastContext(
				blockCenterPos,
				outsideBlockPos,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				ShapeContext.absent()
			)
		);
		if (hitResult.getType() != HitResult.Type.MISS) return false;

		// Cast ray from eye position
		blockCenterPos = blockCenterPos.add(0, 1, 0);
		outsideBlockPos = outsideBlockPos.add(0, 1, 0);
		hitResult = world.raycast(
			new RaycastContext(
				blockCenterPos,
				outsideBlockPos,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				ShapeContext.absent()
			)
		);
        return hitResult.getType() == HitResult.Type.MISS;
    }

	public static BlockPos getNearestStandableBlockPos(final World world, BlockPos blockPos, final int minY, final int maxY) {
		while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() >= minY)) {
			blockPos = blockPos.down();
		}
		while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() <= maxY)) {
			blockPos = blockPos.up();
		}
		return blockPos;
	}

	public static BlockPos getNearestStandableBlockPosTowardsEntity(final Entity entity, BlockPos blockPos, final int minY, final int maxY) {
		final World world = entity.getEntityWorld();
		final BlockPos entityPos = entity.getBlockPos();
		if (blockPos.getY() > entityPos.getY()) {
			// Above player, try moving down first
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() >= minY)) {
				blockPos = blockPos.down();
			}
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() <= maxY)) {
				blockPos = blockPos.up();
			}
		} else {
			// Below player, try moving up first
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() <= maxY)) {
				blockPos = blockPos.up();
			}
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() >= minY)) {
				blockPos = blockPos.down();
			}
		}
		return blockPos;
	}

	public static Vec3d getRandomDirection(final boolean randomY) {
		return new Vec3d(
			RANDOM.nextDouble() * 2 - 1,
			randomY ? (RANDOM.nextDouble() * 2 - 1) : 0,
			RANDOM.nextDouble() * 2 - 1
		).normalize();
	}

	public static boolean isEqual(Vec3d a, Vec3d b) {
		return (a.getX() - b.getX() <= EPSILON) &&
				(a.getY() - b.getY() <= EPSILON) &&
				(a.getZ() - b.getZ() <= EPSILON);
	}

	public static Direction getBlockDirectionFromEntity(final Entity entity, final BlockPos blockPos) {
		final Vec3d entityPos = entity.getPos();
		final Vec3d direction = new Vec3d(
			blockPos.getX() - entityPos.x,
			blockPos.getY() - entityPos.y,
			blockPos.getZ() - entityPos.z
		).normalize();

		// Calculate the absolute values for comparison
		double absX = Math.abs(direction.x);
		double absY = Math.abs(direction.y);
		double absZ = Math.abs(direction.z);

		// Determine the cardinal direction based on the largest component
		if (absX >= absY && absX >= absZ) {
			return direction.x > 0 ? Direction.EAST : Direction.WEST;
		} else if (absY >= absX && absY >= absZ) {
			return direction.y > 0 ? Direction.UP : Direction.DOWN;
		} else {
			return direction.z > 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}

	public static BlockPos getTopBlock(final World world, final BlockPos start, final boolean opaque) {
		// Start from the world height and move down
		BlockPos position =  new BlockPos(start.getX(), world.getTopY(), start.getZ());
		if (opaque) {
			while (!world.getBlockState(position.down()).isOpaque() && (position.getY() > world.getBottomY())) {
				position = position.down();
			}
		} else {
			while (world.getBlockState(position.down()).isAir() && (position.getY() > world.getBottomY())) {
				position = position.down();
			}
		}
		return position;
	}

	public static Vec3d getRandomPosNearEntity(final Entity entity, final int distanceMin, final int distanceMax, final boolean randomY) {
		final Vec3d randomDirection = getRandomDirection(randomY);
		final int distance = RANDOM.nextBetween(distanceMin, distanceMax);

		// Scale the direction vector by the random distance magnitude
		final Vec3d randomOffset = randomDirection.multiply(distance);
		return entity.getPos().add(randomOffset);
	}

	public static BlockPos getRandomCaveBlockNearEntity(final Entity entity, final int distanceMin, final int distanceMax, final int maxAttempts, final boolean randomY) {
		final World world = entity.getEntityWorld();

		// Start with random block and check maxAttempt times
		BlockPos blockPos = getBlockPosFromVec3d(getRandomPosNearEntity(entity, distanceMin, distanceMax, randomY));
		for (int i = 0; i < maxAttempts; ++i) {
			// Return if blockPos is within constraints
			if (
					world.getBlockState(blockPos).isOpaque() &&
					isCaveBlockSound(world.getBlockState(blockPos).getSoundGroup()) &&
					!Algorithms.couldBlockBeSeenByPlayers(world.getPlayers(), blockPos)
			) return blockPos;
			// Try again
			else blockPos = getBlockPosFromVec3d(getRandomPosNearEntity(entity, distanceMin, distanceMax, randomY));
		}

		// If nothing is found in 50 attempts, just select a block in the wall
		return blockPos;
	}

	public static BlockPos getRandomStandableBlockNearEntity(final Entity entity, final int distanceMin, final int distanceMax, final int maxAttempts, final boolean randomY) {
		final BlockPos entityPos = entity.getBlockPos();
		final int moveDistance = Math.max(distanceMin, distanceMax - distanceMin);
		final int maxDistanceDown = entityPos.getY() - moveDistance;
		final int maxDistanceUp = entityPos.getY() + moveDistance;

		// Start with random block and check maxAttempt times
		BlockPos blockPos = getBlockPosFromVec3d(getRandomPosNearEntity(entity, distanceMin, distanceMax, randomY));
		for (int i = 0; i < maxAttempts; ++i) {
			// Move to nearest standable block
			blockPos = getNearestStandableBlockPosTowardsEntity(entity, blockPos, maxDistanceDown, maxDistanceUp);
			// Return if blockPos is within constraints
			if (!blockPos.isWithinDistance(entityPos, distanceMin) && blockPos.isWithinDistance(entityPos, distanceMax)) return blockPos;
			// Try again
			else blockPos = getBlockPosFromVec3d(getRandomPosNearEntity(entity, distanceMin, distanceMax, randomY));
		}

		// If nothing is found in 50 attempts, just select a block in the wall
		return blockPos;
	}

	public static boolean isCaveBlockSound(final BlockSoundGroup sound) {
		return (
			sound == BlockSoundGroup.STONE ||
			sound == BlockSoundGroup.DEEPSLATE ||
			sound == BlockSoundGroup.GRAVEL ||
			sound == BlockSoundGroup.DRIPSTONE_BLOCK ||
			sound == BlockSoundGroup.POINTED_DRIPSTONE ||
			sound == BlockSoundGroup.ROOTED_DIRT ||
			sound == BlockSoundGroup.TUFF ||
			sound == BlockSoundGroup.SCULK ||
			sound == BlockSoundGroup.SCULK_CATALYST ||
			sound == BlockSoundGroup.SCULK_SHRIEKER ||
			sound == BlockSoundGroup.SCULK_SENSOR
		);
	}

	public static boolean isEntityOutside(final Entity entity) {
		return entity.getEntityWorld().getLightLevel(LightType.SKY, entity.getBlockPos().up()) >= 15;
	}

	public static boolean isEntityInCave(final Entity entity) {
		final World world = entity.getEntityWorld();
		final BlockPos entityPos = entity.getBlockPos();
		if (world.getLightLevel(LightType.SKY, entityPos) > 0) return false;

		// Raycast in cardinal directions
		int nonCaveBlockCount = 0;
		HitResult ray = castRayFromEye(entity, entityPos.up(128).toCenterPos());
		BlockPos rayPos = ((BlockHitResult) ray).getBlockPos();
		BlockState state = world.getBlockState(rayPos);
		if ((ray.getType() != HitResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, entityPos.down(128).toCenterPos());
		rayPos = ((BlockHitResult) ray).getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.getType() != HitResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, entityPos.north(128).toCenterPos());
		rayPos = ((BlockHitResult) ray).getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.getType() != HitResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, entityPos.south(128).toCenterPos());
		rayPos = ((BlockHitResult) ray).getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.getType() != HitResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, entityPos.east(128).toCenterPos());
		rayPos = ((BlockHitResult) ray).getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.getType() != HitResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, entityPos.west(128).toCenterPos());
		rayPos = ((BlockHitResult) ray).getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.getType() != HitResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;

		// Cast rays in random directions. If they all hit, the sky cannot be seen.
		for (int i = 0; i < algorithmsCaveDetectionRays; ++i) {
			ray = castRayFromEye(entity, getRandomPosNearEntity(entity, 128, 128, true));
			rayPos = ((BlockHitResult) ray).getBlockPos();
			state = world.getBlockState(rayPos);
			if (ray.getType() != HitResult.Type.BLOCK) return false;
			if (!isCaveBlockSound(state.getSoundGroup())) ++nonCaveBlockCount;
		}

		// If over 5% of hit blocks are not normally found in a cave, assume player is in a base
        return !(((float) nonCaveBlockCount / (float) Math.max(1, algorithmsCaveDetectionRays + 6)) > algorithmsCaveDetectionMaxNonCaveBlockPercent);
    }

	public static Vec3d getDirectionPosToPos(final Vec3d pos1, final Vec3d pos2) {
		return pos2.subtract(pos1).normalize();
	}

	public static Vec3d getPosOffsetInDirection(final Vec3d pos, final Vec3d rotation, final float distance) {
		// Scale vector by distance magnitude
		final Vec3d offsetVector = rotation.multiply(distance);
		return pos.add(offsetVector);
	}

	public static EulerAngle directionToAngles(final Vec3d direction) {
		final float pitch = (float) -Math.toDegrees(Math.atan2(
			direction.y,
			Math.sqrt((direction.x * direction.x) + (direction.z * direction.z))
		));
		final float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
		return new EulerAngle(pitch, yaw, 0.0f);
	}

	public static boolean isEntityInDarkness(final LivingEntity entity, final int maxLightLevel) {
		if (entity.getEntityWorld().getLightLevel(entity.getBlockPos()) > maxLightLevel) return false;
		for (final StatusEffectInstance effect : entity.getStatusEffects()) {
			if (effect.getEffectType() == StatusEffects.NIGHT_VISION) return false;
		}
		return true;
	}

	@Nullable
	public static BlockPos getNearestBlockToEntity(final Entity entity, final Block blockType, final int range) {
		final World world = entity.getEntityWorld();
		final BlockPos entityBlockPos = entity.getBlockPos();
		final Vec3d entityPos = entity.getPos();
		double closestBlockDistance = 0, checkDistance;
		BlockPos closestBlockPos = null, checkPos;
		for (int x = -range; x < range; ++x) {
			for (int y = -range; y < range; ++y) {
				for (int z = -range; z < range; ++z) {
					checkPos = entityBlockPos.add(x, y, z);
					if (world.getBlockState(checkPos).getBlock() == blockType) {
						// Set first check to closestBlockPos
						if (closestBlockPos == null) {
							closestBlockPos = checkPos;
							closestBlockDistance = entityPos.distanceTo(closestBlockPos.toCenterPos());
							continue;
						}

						// Check if this block is closer than the previous one
						checkDistance = entityPos.distanceTo(closestBlockPos.toCenterPos());
						if (checkDistance < closestBlockDistance) {
							checkDistance = entityPos.distanceTo(closestBlockPos.toCenterPos());
							closestBlockDistance = checkDistance;
							closestBlockPos = checkPos;
						}
					}
				}
			}
		}
		return closestBlockPos;
	}

	public static boolean isBlockOfBlockTypes(final Block block, final List<Block> blockTypes) {
		for (final Block blockType : blockTypes) {
			if (block == blockType) return true;
		}
		return false;
	}

	@Nullable
	public static BlockPos getNearestBlockToEntity(final Entity entity, final List<Block> blockTypes, final int range) {
		final World world = entity.getEntityWorld();
		final BlockPos entityBlockPos = entity.getBlockPos();
		final Vec3d entityPos = entity.getPos();
		double closestBlockDistance = 0, checkDistance;
		BlockPos closestBlockPos = null, checkPos;
		for (int x = -range; x < range; ++x) {
			for (int y = -range; y < range; ++y) {
				for (int z = -range; z < range; ++z) {
					checkPos = entityBlockPos.add(x, y, z);
					if (isBlockOfBlockTypes(world.getBlockState(checkPos).getBlock(), blockTypes)) {
						// Set first check to closestBlockPos
						if (closestBlockPos == null) {
							closestBlockPos = checkPos;
							closestBlockDistance = entityPos.distanceTo(closestBlockPos.toCenterPos());
							continue;
						}

						// Check if this block is closer than the previous one
						checkDistance = entityPos.distanceTo(checkPos.toCenterPos());
						if (checkDistance < closestBlockDistance) {
							checkDistance = entityPos.distanceTo(closestBlockPos.toCenterPos());
							closestBlockDistance = checkDistance;
							closestBlockPos = checkPos;
						}
					}
				}
			}
		}
		return closestBlockPos;
	}
}
