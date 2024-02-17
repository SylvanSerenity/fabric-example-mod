package com.sylvan.presence.event;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ExtinguishTorches {
	public static Map<UUID, Map.Entry<DimensionType, Stack<BlockPos>>> torchPlacementMap = new HashMap<>();

	// Config
	protected static int extinguishTorchesTrackedMax = 32;
	protected static int extinguishTorchesTorchDistanceMax = 32;
	private static int extinguishTorchesExtinguishTryInterval = 60;
	private static int extinguishTorchesTrackIntervalMin = 60 * 60 * 2;
	private static int extinguishTorchesTrackIntervalMax = 60 * 60 * 5;

	private static void loadConfig() {
		try {
			extinguishTorchesTrackedMax = Presence.config.getOrSetValue("extinguishTorchesTrackedMax", extinguishTorchesTrackedMax).getAsInt();
			extinguishTorchesTorchDistanceMax = Presence.config.getOrSetValue("extinguishTorchesTorchDistanceMax", extinguishTorchesTorchDistanceMax).getAsInt();
			extinguishTorchesExtinguishTryInterval = Presence.config.getOrSetValue("extinguishTorchesExtinguishTryInterval", extinguishTorchesExtinguishTryInterval).getAsInt();
			extinguishTorchesTrackIntervalMin = Presence.config.getOrSetValue("extinguishTorchesTrackIntervalMin", extinguishTorchesTrackIntervalMin).getAsInt();
			extinguishTorchesTrackIntervalMax = Presence.config.getOrSetValue("extinguishTorchesTrackIntervalMax", extinguishTorchesTrackIntervalMax).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for ExtinguishTorches.java. Wiping and using default.", e);
			Presence.config.clearConfig();
		}
	}

	public static void initEvent() {
		loadConfig();
	}

	public static void scheduleTracking(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				ExtinguishTorches.startTrackingTorches(player);
			},
			Algorithms.RANDOM.nextBetween(
				extinguishTorchesTrackIntervalMin,
				extinguishTorchesTrackIntervalMax
			),
			TimeUnit.SECONDS
		);
	}

	public static void startTrackingTorches(final PlayerEntity player) {
		if (!player.isRemoved() && !torchPlacementMap.containsKey(player.getUuid())) {
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
		}, extinguishTorchesExtinguishTryInterval, TimeUnit.SECONDS);
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
				world.getLightLevel(LightType.SKY, player.getBlockPos()) <= 0	// Player must be above ground
			) return false; // Wait until next attempt

			Block block;
			for (BlockPos torchPos : torchStack) {
				block = world.getBlockState(torchPos).getBlock();
				if (
					((block == Blocks.TORCH) || (block == Blocks.WALL_TORCH)) &&	// Block must still be a torch
					!Algorithms.blockCanBeSeen(world.getPlayers(), torchPos)	// Player cannot see the torch being removed
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
}
