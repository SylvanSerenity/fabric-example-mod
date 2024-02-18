package com.sylvan.presence.event;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
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

	public static void initEvents() {
		AmbientSounds.initEvent();
		NearbySounds.initEvent();
	}

	public static void registerEvents() {
		initEvents();

		// Start/stop scheduler with server
		ServerLifecycleEvents.SERVER_STARTING.register((serverStarting) -> {
			scheduler = Executors.newScheduledThreadPool(8);
			PlayerData.setPlayerManager(serverStarting.getPlayerManager());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((serverStopping) -> {
			scheduler.shutdown();

			if (!Presence.config.exists()) {
				Presence.config.clearConfig();
				initEvents();
			}

			try {
				scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Presence.LOGGER.error("Failed to await termination of event scheduler.", e);
			}
		});

		// Schedule player join events
		ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server) -> {
			final PlayerEntity player = serverPlayNetworkHandler.getPlayer();
			// Give player a random haunt chance when they join
			if (Algorithms.RANDOM.nextFloat() <= PlayerData.defaultHauntChance) {
				// If player is haunted, schedule all events per the configuration file
				PlayerData.addPlayerData(player.getUuid());
				if (Footsteps.footstepsEnabled) Footsteps.scheduleEvent(player);
				if (ExtinguishTorches.extinguishTorchesEnabled) ExtinguishTorches.scheduleTracking(player);
				if (NearbySounds.nearbySoundsEnabled) NearbySounds.scheduleEvent(player);
				if (AmbientSounds.ambientSoundsEnabled) AmbientSounds.scheduleEvent(player);
			}
		});

		// Attempt to remove torches when player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, server) -> {
			final PlayerEntity player = serverPlayNetworkHandler.getPlayer();
			if (ExtinguishTorches.extinguishTorchesEnabled) ExtinguishTorches.extinguishTrackedTorches(player);
			PlayerData.savePlayerData(player.getUuid());
		});

		// Add torch tracker for extinguish torches event
		if (ExtinguishTorches.extinguishTorchesEnabled) {
			UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
				final BlockPos torchPos = hitResult.getBlockPos().offset(hitResult.getSide()); // Offset by 1 block in the direction of torch placement
				if (
					world.isClient() ||												// World must be server-side
					(player.getMainHandStack().getItem() != Items.TORCH && player.getOffHandStack().getItem() != Items.TORCH) ||	// Player must be holding a torch
					world.getLightLevel(LightType.SKY, torchPos) > ExtinguishTorches.extinguishTorchesSkyLightLevelMax		// Torch must be underground
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
