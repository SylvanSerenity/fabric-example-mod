package com.sylvan.event;

import java.util.*;
import java.util.concurrent.*;

import com.sylvan.Presence;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.dimension.DimensionType;

public class Events {
	public static ScheduledExecutorService scheduler;

	public static void registerEvents() {
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

		// Schedule footstep event on player join
		if (Presence.config.footstepsEnabled) {
			ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server) -> {
				Footsteps.scheduleEvent(serverPlayNetworkHandler.getPlayer());
				ExtinguishTorches.startTrackingTorches(serverPlayNetworkHandler.getPlayer());
			});
		}

		// Add torch tracker for extinguish torches event
		if (Presence.config.extinguishTorchesEnabled) {
			UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
				if (
					hand != Hand.MAIN_HAND ||
					world.getLightLevel(LightType.SKY, hitResult.getBlockPos()) > 0 ||
					player.getMainHandStack().getItem() != Items.TORCH
				) return ActionResult.PASS;

				if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
					Stack<Map.Entry<DimensionType, BlockPos>> torches = ExtinguishTorches.torchPlacementMap.get(player.getUuid());
					final Map.Entry<DimensionType, BlockPos> entry = new AbstractMap.SimpleEntry<>(
						world.getDimension(),
						hitResult.getBlockPos().offset(hitResult.getSide()) // Offset by 1 block in the direction of torch placement
					);

					if (torches.size() >= Presence.config.extinguishTorchesMax) {
						// Remove bottom of the stack to make room
						torches.remove(0);
					}
					torches.push(entry);
				}

				return ActionResult.PASS;
			});
		}
	}
}
