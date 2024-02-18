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
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class ExtinguishTorches {
	public static Map<UUID, Map.Entry<DimensionType, Stack<BlockPos>>> torchPlacementMap = new HashMap<>();

	// Config
	protected static boolean extinguishTorchesEnabled = true;
	private static int extinguishTorchesTrackDelayMin = 60 * 30;
	private static int extinguishTorchesTrackDelayMax = 60 * 60 * 5;
	private static int extinguishTorchesExtinguishRetryDelay = 60;
	protected static int extinguishTorchesTrackedMax = 32;
	protected static int extinguishTorchesTorchDistanceMax = 32;
	protected static int extinguishTorchesSkyLightLevelMax = 6;

	private static void loadConfig() {
		try {
			extinguishTorchesEnabled = Presence.config.getOrSetValue("extinguishTorchesEnabled", extinguishTorchesEnabled).getAsBoolean();
			extinguishTorchesTrackDelayMin = Presence.config.getOrSetValue("extinguishTorchesTrackDelayMin", extinguishTorchesTrackDelayMin).getAsInt();
			extinguishTorchesTrackDelayMax = Presence.config.getOrSetValue("extinguishTorchesTrackDelayMax", extinguishTorchesTrackDelayMax).getAsInt();
			extinguishTorchesExtinguishRetryDelay = Presence.config.getOrSetValue("extinguishTorchesExtinguishRetryDelay", extinguishTorchesExtinguishRetryDelay).getAsInt();
			extinguishTorchesTrackedMax = Presence.config.getOrSetValue("extinguishTorchesTrackedMax", extinguishTorchesTrackedMax).getAsInt();
			extinguishTorchesTorchDistanceMax = Presence.config.getOrSetValue("extinguishTorchesTorchDistanceMax", extinguishTorchesTorchDistanceMax).getAsInt();
			extinguishTorchesSkyLightLevelMax = Presence.config.getOrSetValue("extinguishTorchesSkyLightLevelMax", extinguishTorchesSkyLightLevelMax).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for ExtinguishTorches.java. Wiping and using default.", e);
			Presence.config.clearConfig();
			Events.initEvents();
		}
	}

	public static void initEvent() {
		loadConfig();
	}

	public static void scheduleTracking(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				if (!player.isRemoved()) ExtinguishTorches.startTrackingTorches(player);
			},
			Algorithms.RANDOM.nextBetween(
				extinguishTorchesTrackDelayMin,
				extinguishTorchesTrackDelayMax
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
		}, extinguishTorchesExtinguishRetryDelay, TimeUnit.SECONDS);
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
			// Player must have tracked torches
			if (torchStack.empty()) return false; // Wait until next attempt

			Block block;
			for (BlockPos torchPos : torchStack) {
				block = world.getBlockState(torchPos).getBlock();
				if (
					((block == Blocks.TORCH) || (block == Blocks.WALL_TORCH)) &&	// Block must still be a torch
					!Algorithms.canBlockBeSeen(world.getPlayers(), torchPos)	// Player cannot see the torch being removed
				) {
					// Remove the torch
					world.removeBlock(torchPos, false);
				} // Otherwise skip torch
			}

			torchStack.clear();
		}
		if (torchPlacementMap.containsKey(player.getUuid())) torchPlacementMap.remove(player.getUuid());
		return true;
	}
}
