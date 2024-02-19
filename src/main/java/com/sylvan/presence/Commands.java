package com.sylvan.presence;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.HerobrineEntity;
import com.sylvan.presence.event.AmbientSounds;
import com.sylvan.presence.event.Attack;
import com.sylvan.presence.event.ExtinguishTorches;
import com.sylvan.presence.event.Footsteps;
import com.sylvan.presence.event.NearbySounds;
import com.sylvan.presence.util.Algorithms;

public class Commands {
	private static HerobrineEntity herobrineEntity;

	public static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			literal("presence")
			.requires(source -> source.hasPermissionLevel(2))
			.then(
				literal("event")
				.then(
					literal("ambientSounds")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing ambient sounds event.").withColor(Formatting.BLUE.getColorValue()), false);
							AmbientSounds.playAmbientSound(context.getSource().getPlayer());
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute ambient sounds event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Executing ambient sounds event for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								AmbientSounds.playAmbientSound(player);
							}
							return 1;
						})
					)
				)
				.then(
					literal("attack")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing attack event.").withColor(Formatting.BLUE.getColorValue()), false);
							Attack.attack(context.getSource().getPlayer(), Algorithms.randomBetween(Attack.attackDamageMin, Attack.attackDamageMax));
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute attack event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("damage", FloatArgumentType.floatArg())
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final float damage = FloatArgumentType.getFloat(context, "damage");
								context.getSource().sendFeedback(() -> Text.literal("Executing attack event.").withColor(Formatting.BLUE.getColorValue()), false);
								Attack.attack(context.getSource().getPlayer(), damage);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot execute attack event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									final float damage = FloatArgumentType.getFloat(context, "damage");
									context.getSource().sendFeedback(() -> Text.literal("Executing attack event for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									Attack.attack(player, damage);
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
									"Extinguished tracked torches for " + player.getName().getString() + "."
								).withColor(Formatting.BLUE.getColorValue()), false);
								ExtinguishTorches.extinguishTrackedTorches(player);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						literal("extinguish")
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final PlayerEntity player = context.getSource().getPlayer();
								if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal(
										"Extinguished tracked torches for " + player.getName().getString() + "."
									).withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.extinguishTrackedTorches(player);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Extinguished torches for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.extinguishTrackedTorches(player);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
								return 1;
							})
						)
					)
					.then(
						literal("track")
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final PlayerEntity player = context.getSource().getPlayer();
								if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Player is already being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal(
										"Started tracking torches for " + player.getName().getString() + "."
									).withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.startTrackingTorches(player);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Player is already being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Started tracking torches for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.startTrackingTorches(player);
								}
								return 1;
							})
						)
					)
				)
				.then(
					literal("footsteps")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event.").withColor(Formatting.BLUE.getColorValue()), false);
							Footsteps.generateFootsteps(context.getSource().getPlayer(), Algorithms.RANDOM.nextBetween(Footsteps.footstepsStepsMin, Footsteps.footstepsStepsMax));
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute footsteps event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
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
								context.getSource().sendFeedback(() -> Text.literal("Cannot execute footsteps event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									final int footstepCount = IntegerArgumentType.getInteger(context, "footstepCount");
									context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									Footsteps.generateFootsteps(player, footstepCount);
								}
								return 1;
							})
						)
					)
				)
				.then(
					literal("nearbySounds")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing nearby sounds event.").withColor(Formatting.BLUE.getColorValue()), false);
							NearbySounds.playNearbySound(context.getSource().getPlayer());
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute nearby sounds event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Executing nearby sounds event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								NearbySounds.playNearbySound(player);
							}
							return 1;
						})
					)
				)
			)
			.then(
				literal("fakeHerobrine")
				.then(
					literal("summon")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							context.getSource().sendFeedback(() -> Text.literal("Summoned fake Herobrine.").withColor(Formatting.BLUE.getColorValue()), false);
							summonFakeHerobrine(player);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot summon fake Herobrine by server.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
				)
				.then(
					literal("destroy")
					.executes(context -> {
						context.getSource().sendFeedback(() -> Text.literal("Destroyed fake Herobrine.").withColor(Formatting.BLUE.getColorValue()), false);
						destroyFakeHerobrine();
						return 1;
					})
				)
			)
			.then(
				literal("haunt")
				.executes(context -> {
					if (context.getSource().isExecutedByPlayer()) {
						final PlayerEntity player = context.getSource().getPlayer();
						final PlayerData playerData = PlayerData.getPlayerData(player);
						if (playerData.isHaunted()) {
							context.getSource().sendFeedback(() -> Text.literal("Freed yourself.").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Haunted yourself.").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(true);
						}
					} else {
						context.getSource().sendFeedback(() -> Text.literal("Cannot haunt server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
					}
					return 1;
				})
				.then(
					argument("player", StringArgumentType.word())
					.suggests((context, builder) -> {
						final Iterable<String> playerNames = context.getSource().getPlayerNames();
						for (final String playerName : playerNames) {
							builder.suggest(playerName);
						}
						return builder.buildFuture();
					})
					.executes(context -> {
						final String playerName = StringArgumentType.getString(context, "player");
						final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
						final PlayerData playerData = PlayerData.getPlayerData(player);
						if (player == null) {
							context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
						} else if (playerData.isHaunted()) {
							context.getSource().sendFeedback(() -> Text.literal("Freed player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Haunted player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(true);
						}
						return 1;
					})
				)
			)
			.then(
				literal("query")
				.then(
					literal("trackingTorches")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal("You are being tracked for torch placements.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server torch placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is being tracked for torch placements.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("haunted")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							final PlayerData playerData = PlayerData.getPlayerData(player);
							if (playerData.isHaunted()) {
								context.getSource().sendFeedback(() -> Text.literal("You are haunted.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not haunted.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server haunt status. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							final PlayerData playerData = PlayerData.getPlayerData(player);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (playerData.isHaunted()) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is haunted.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not haunted.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("isInCave")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							if (Algorithms.isEntityInCave(player)) {
								context.getSource().sendFeedback(() -> Text.literal("You are in a cave.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not in a cave.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server cave status. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (Algorithms.isEntityInCave(player)) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is in a cave.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not in a cave.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
			)
		));
	}

	private static void summonFakeHerobrine(final PlayerEntity player) {
		destroyFakeHerobrine();
		final World world = player.getWorld();
		herobrineEntity = new HerobrineEntity(world);
		herobrineEntity.setBodyRotation(player.getYaw());
		herobrineEntity.setHeadRotation(player.getPitch(), player.getYaw(), Algorithms.randomBetween(-15.0f, 15.0f));
		herobrineEntity.setPosition(player.getPos());
		herobrineEntity.summon();
	}

	private static void destroyFakeHerobrine() {
		if (herobrineEntity != null) herobrineEntity.remove();
	}
}
