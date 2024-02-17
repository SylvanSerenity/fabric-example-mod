package com.sylvan.event;

import java.util.*;
import java.util.concurrent.*;

import com.sylvan.Presence;

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

	public static void registerEvents() {
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
			if (Presence.RANDOM.nextFloat() <= Presence.config.hauntChance) {
				hauntedPlayers.add(player.getUuid());
				if (Presence.config.footstepsEnabled) Footsteps.scheduleEvent(player);
				if (Presence.config.extinguishTorchesEnabled) ExtinguishTorches.scheduleTracking(player);
				if (Presence.config.nearbySoundsEnabled) NearbySounds.scheduleEvent(player);
			}
		});

		// Attempt to remove torches when player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, server) -> {
			final PlayerEntity player = serverPlayNetworkHandler.getPlayer();
			if (Presence.config.extinguishTorchesEnabled) ExtinguishTorches.extinguishTrackedTorches(player);
			if (hauntedPlayers.contains(player.getUuid())) hauntedPlayers.remove(player.getUuid());
		});

		// Add torch tracker for extinguish torches event
		if (Presence.config.extinguishTorchesEnabled) {
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
						// Quit if not in the same dimension
						ExtinguishTorches.extinguishTrackedTorches(player);
						return ActionResult.PASS;
					}

					final Stack<BlockPos> torches = entry.getValue();
					if (
						!torches.empty() &&
						!torches.peek().isWithinDistance(torchPos, Presence.config.extinguishTorchesTorchDistanceMax)
					) {
						// Torch must be within extinguishTorchesTorchDistanceMax blocks from last torch
						return ActionResult.PASS;
					}

					if (torches.size() >= Presence.config.extinguishTorchesTrackedMax) {
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
