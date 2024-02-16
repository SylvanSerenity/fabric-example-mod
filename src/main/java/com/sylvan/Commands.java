package com.sylvan;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sylvan.event.ExtinguishTorches;
import com.sylvan.event.Footsteps;

public class Commands {
	public static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			literal("presence")
			.requires(source -> source.hasPermissionLevel(2))
			.then(
				literal("event")
				.then(
					literal("footsteps")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event.").withColor(Formatting.BLUE.getColorValue()), false);
							Footsteps.generateFootsteps(context.getSource().getPlayer(), 3);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute footsteps event on server. Please specify a player.").withColor(Formatting.RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("footstepCount", IntegerArgumentType.integer())
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final int footstepCount = IntegerArgumentType.getInteger(context, "footstepCount");
								context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event.").withColor(Formatting.BLUE.getColorValue()), false);
								Footsteps.generateFootsteps(context.getSource().getPlayer(), footstepCount);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot execute footsteps event on server. Please specify a player.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.RED.getColorValue()), false);
								} else {
									final int footstepCount = IntegerArgumentType.getInteger(context, "footstepCount");
									context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event for player" + player.getDisplayName().getLiteralString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									Footsteps.generateFootsteps(player, footstepCount);
								}
								return 1;
							})
						)
					)
				)
				.then(
					literal("extinguishTorches")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal(
									"Extinguished tracked torches for " + player.getDisplayName().getLiteralString() + "."
								).withColor(Formatting.BLUE.getColorValue()), false);
								ExtinguishTorches.removeTrackedTorches(player);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						literal("track")
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final PlayerEntity player = context.getSource().getPlayer();
								if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Player is already being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal(
										"Started tracking torches for " + player.getDisplayName().getLiteralString() + "."
									).withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.startTrackingTorches(player);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.RED.getColorValue()), false);
								} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Player is already being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Started tracking torches for " + player.getDisplayName().getLiteralString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								}
								return 1;
							})
						)
					)
					.then(
						literal("extinguish")
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final PlayerEntity player = context.getSource().getPlayer();
								if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal(
										"Extinguished tracked torches for " + context.getSource().getDisplayName() + "."
									).withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.removeTrackedTorches(player);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.RED.getColorValue()), false);
								} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Extinguished torches for " + player.getDisplayName().getLiteralString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.removeTrackedTorches(player);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
								}
								return 1;
							})
						)
					)
				)
			)
		));
	}
}
