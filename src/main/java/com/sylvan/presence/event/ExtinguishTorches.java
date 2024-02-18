package com.sylvan.presence.event;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
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
	public static boolean extinguishTorchesEnabled = true;			// Whether the extinguish torches event is active
	private static int extinguishTorchesTrackDelayMin = 60 * 30;		// The minimum delay between beginning to track torches
	private static int extinguishTorchesTrackDelayMax = 60 * 60 * 5;	// The maximum delay between beginning to track torches
	private static int extinguishTorchesExtinguishRetryDelay = 60 * 10;	// The delay between tries to extinguish tracked torches
	public static int extinguishTorchesTrackedMax = 16;			// The maximum number of torches to track
	public static int extinguishTorchesTorchDistanceMax = 32;		// The maximum distance between the last tracked torch (so that the torches are in the same cave)
	public static int extinguishTorchesSkyLightLevelMax = 6;		// The maximum light level to determine whether to track the placed torch (so that surface torches are not tracked)

	public static void loadConfig() {
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
			Presence.initConfig();
		}
	}

	public static void scheduleTracking(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player.getUuid()).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (!player.isRemoved()) ExtinguishTorches.startTrackingTorches(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(extinguishTorchesTrackDelayMin, hauntLevel),
				Algorithms.divideByFloat(extinguishTorchesTrackDelayMax, hauntLevel)
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
