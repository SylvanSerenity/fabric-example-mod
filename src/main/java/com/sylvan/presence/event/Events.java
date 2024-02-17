package com.sylvan.presence.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.*;

import com.sylvan.presence.Presence;
import com.sylvan.presence.util.Algorithms;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.dimension.DimensionType;

public class Events {
	public static ScheduledExecutorService scheduler;
	public static List<UUID> hauntedPlayers = new ArrayList<>();

	// Config
	private static float hauntChance = 1.0f;
	private static boolean ambientSoundsEnabled = true;
	private static boolean extinguishTorchesEnabled = true;
	private static boolean footstepsEnabled = true;
	private static boolean nearbySoundsEnabled = true;

	private static void loadConfig() {
		hauntChance = Presence.config.getOrSetValue("hauntChance", hauntChance);
		ambientSoundsEnabled = Presence.config.getOrSetValue("ambientSoundsEnabled", ambientSoundsEnabled);
		extinguishTorchesEnabled = Presence.config.getOrSetValue("extinguishTorchesEnabled", extinguishTorchesEnabled);
		footstepsEnabled = Presence.config.getOrSetValue("footstepsEnabled", footstepsEnabled);
		nearbySoundsEnabled = Presence.config.getOrSetValue("nearbySoundsEnabled", nearbySoundsEnabled);
	}

	public static void registerEvents() {
		loadConfig();
		AmbientSounds.initEvent();
		ExtinguishTorches.initEvent();
		NearbySounds.initEvent();

		// Start/stop scheduler with server
		ServerLifecycleEvents.SERVER_STARTING.register((serverStarting) -> {
			scheduler = Executors.newScheduledThreadPool(8);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((serverStopping) -> {
			scheduler.shutdown();

			try {
				scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		// Schedule player join events
		ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server) -> {
			final PlayerEntity player = serverPlayNetworkHandler.getPlayer();
			if (Algorithms.RANDOM.nextFloat() <= hauntChance) {
				hauntedPlayers.add(player.getUuid());
				if (footstepsEnabled) Footsteps.scheduleEvent(player);
				if (extinguishTorchesEnabled) ExtinguishTorches.scheduleTracking(player);
				if (nearbySoundsEnabled) NearbySounds.scheduleEvent(player);
				if (ambientSoundsEnabled) AmbientSounds.scheduleEvent(player);
			}
		});

		// Attempt to remove torches when player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, server) -> {
			final PlayerEntity player = serverPlayNetworkHandler.getPlayer();
			if (extinguishTorchesEnabled) ExtinguishTorches.extinguishTrackedTorches(player);
			if (hauntedPlayers.contains(player.getUuid())) hauntedPlayers.remove(player.getUuid());
		});

		// Add torch tracker for extinguish torches event
		if (extinguishTorchesEnabled) {
			UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
				final BlockPos torchPos = hitResult.getBlockPos().offset(hitResult.getSide()); // Offset by 1 block in the direction of torch placement
				if (
					world.isClient() ||												// World must be server-side
					(player.getMainHandStack().getItem() != Items.TORCH && player.getOffHandStack().getItem() != Items.TORCH) ||	// Player must be holding a torch
					world.getLightLevel(LightType.SKY, torchPos) > 5								// Torch must be underground
				) return ActionResult.PASS;

				if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
					final Map.Entry<DimensionType, Stack<BlockPos>> entry = ExtinguishTorches.torchPlacementMap.get(player.getUuid());
					if (entry.getKey() != world.getDimension()) {
						// Restart if not in the same dimension
						ExtinguishTorches.extinguishTrackedTorches(player);
						ExtinguishTorches.startTrackingTorches(player);
						return ActionResult.PASS;
					}

					final Stack<BlockPos> torches = entry.getValue();
					if (
						!torches.empty() &&
						!torches.peek().isWithinDistance(torchPos, ExtinguishTorches.extinguishTorchesTorchDistanceMax)
					) {
						// Torch must be within extinguishTorchesTorchDistanceMax blocks from last torch
						return ActionResult.PASS;
					}

					if (torches.size() >= ExtinguishTorches.extinguishTorchesTrackedMax) {
						// Remove bottom of the stack to make room
						torches.remove(0);
					}
					torches.push(torchPos);
				}

				return ActionResult.PASS;
			});
		}
	}
}
