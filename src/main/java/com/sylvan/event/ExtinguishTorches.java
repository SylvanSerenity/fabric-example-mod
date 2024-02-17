package com.sylvan.event;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.sylvan.Presence;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ExtinguishTorches {
	public static Map<UUID, Map.Entry<DimensionType, Stack<BlockPos>>> torchPlacementMap = new HashMap<>();

	public static void scheduleTracking(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				ExtinguishTorches.startTrackingTorches(player);
			},
			Presence.RANDOM.nextBetween(
				Presence.config.extinguishTorchesTrackIntervalMin,
				Presence.config.extinguishTorchesTrackIntervalMax
			),
			TimeUnit.SECONDS
		);
	}

	public static void startTrackingTorches(final PlayerEntity player) {
		if (!player.isRemoved()) {
			torchPlacementMap.put(player.getUuid(), new AbstractMap.SimpleEntry<>(player.getWorld().getDimension(), new Stack<>()));
			scheduleExtinguish(player);
		}
	}

	public static void scheduleExtinguish(final PlayerEntity player) {
		Events.scheduler.schedule(() -> {
			if (torchPlacementMap.containsKey(player.getUuid()) && !ExtinguishTorches.extinguishTrackedTorches(player)) {
				// Wait for torches to be placed if first try yielded no results
				scheduleExtinguish(player);
			} else if (!player.isRemoved()) {
				scheduleTracking(player);
			}
		}, Presence.config.extinguishTorchesExtinguishTryInterval, TimeUnit.SECONDS);
	}

	public static boolean extinguishTrackedTorches(final PlayerEntity player) {
		if (!player.isRemoved()) {
			// Player must be tracked
			if (!torchPlacementMap.containsKey(player.getUuid())) return false;

			final Map.Entry<DimensionType, Stack<BlockPos>> entry = torchPlacementMap.get(player.getUuid());
			final Stack<BlockPos> torchStack = entry.getValue();
			final World world = player.getWorld();
			// Player must be in same dimension
			if (entry.getKey() != world.getDimension()) {
				// Quit tracking if dimensions do not match
				torchStack.clear();
				torchPlacementMap.remove(player.getUuid());
				return false;
			}
			if (
				torchStack.empty() ||						// Player must have tracked torches
				(world.getLightLevel(LightType.SKY, player.getBlockPos()) <= 0)	// Player must be above ground
			) return false; // Wait until next attempt

			Block block;
			for (BlockPos torchPos : torchStack) {
				block = world.getBlockState(torchPos).getBlock();
				if (
					((block == Blocks.TORCH) || (block == Blocks.WALL_TORCH)) &&	// Block must still be a torch
					!blockCanBeSeen(world.getPlayers(), torchPos)			// Player cannot see the torch being removed
				) {
					// Remove the torch
					world.removeBlock(torchPos, false);
				} // Otherwise skip torch
			}

			torchStack.clear();
		}
		torchPlacementMap.remove(player.getUuid());
		return true;
	}

	public static boolean blockCanBeSeen(final List<? extends PlayerEntity> players, final BlockPos blockPos) {
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
}
