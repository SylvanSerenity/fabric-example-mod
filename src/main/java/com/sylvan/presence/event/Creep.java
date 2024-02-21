package com.sylvan.presence.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.HerobrineEntity;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Creep {
	// Config
	public static boolean creepEnabled = true;		// Whether the creep event is active
	private static float creepHauntLevelMin = 2.0f;	// The minimum haunt level to play event
	private static int creepDelayMin = 60 * 45;	// The minimum delay between creep events
	private static int creepDelayMax = 60 * 60 * 3;	// The maximum delay between creep events
	private static int creepRetryDelay = 1;		// The delay between retrying creep events in case of failure
	private static int creepDistanceMin = 1;		// The minimum distance behind the player to summon Herobrine
	private static int creepDistanceMax = 1;		// The maximum distance behind the player to summon Herobrine
	private static int creepVerticleDistanceMax = 3;	// The maximum distance Herobrine can be above/below the player
	private static int creepReflexMs = 0;		// The time in milliseconds before Herobrine vanishes
	private static double creepLookAtThreshold = 0.2;	// The threshold at which the player will be considered looking at Herobrine. -1.0 is directly oppsotie, 1.0 is directly towards

	public static final Map<HerobrineEntity, PlayerEntity> herobrineTrackers = new HashMap<>();

	public static void loadConfig() {
		try {
			creepEnabled = Presence.config.getOrSetValue("creepEnabled", creepEnabled).getAsBoolean();
			creepHauntLevelMin = Presence.config.getOrSetValue("creepHauntLevelMin", creepHauntLevelMin).getAsFloat();
			creepDelayMin = Presence.config.getOrSetValue("creepDelayMin", creepDelayMin).getAsInt();
			creepDelayMax = Presence.config.getOrSetValue("creepDelayMax", creepDelayMax).getAsInt();
			creepRetryDelay = Presence.config.getOrSetValue("creepRetryDelay", creepRetryDelay).getAsInt();
			creepDistanceMin = Presence.config.getOrSetValue("creepDistanceMin", creepDistanceMin).getAsInt();
			creepDistanceMax = Presence.config.getOrSetValue("creepDistanceMax", creepDistanceMax).getAsInt();
			creepVerticleDistanceMax = Presence.config.getOrSetValue("creepVerticleDistanceMax", creepVerticleDistanceMax).getAsInt();
			creepReflexMs = Presence.config.getOrSetValue("creepReflexMs", creepReflexMs).getAsInt();
			creepLookAtThreshold = Presence.config.getOrSetValue("creepLookAtThreshold", creepLookAtThreshold).getAsDouble();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Footsteps.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(Creep.creepDelayMax, hauntLevel),
				Algorithms.divideByFloat(Creep.creepDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				if (creep(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(creepDelayMin, hauntLevel),
							Algorithms.divideByFloat(creepDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, creepRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static void onWorldTick(final ServerWorld world) {
		if (herobrineTrackers.isEmpty()) return;
		final List<ServerPlayerEntity> players = world.getPlayers();
		if (players.isEmpty()) return;

		Iterator<HerobrineEntity> it = herobrineTrackers.keySet().iterator();
		HerobrineEntity herobrine;
		while (it.hasNext()) {
			herobrine = it.next();
			final PlayerEntity player = herobrineTrackers.get(herobrine);
			// Remove if player leaves or is in another dimension
			if (player.isRemoved() || player.getWorld().getDimension() != world.getDimension()) {
				herobrine.remove();
				it.remove();
				continue;
			}

			// Remove if seen
			if (herobrine.isSeenByPlayers(creepLookAtThreshold)) {
				if (creepReflexMs > 0) herobrine.scheduleRemoval(creepReflexMs);
				else herobrine.remove();
				it.remove();
				continue;
			}

			// Inch forward toward player
			// Pretend player and Herobrine are on the same block to prevent direction from being dependent on Y-axis
			final Vec3d playerXZ = new Vec3d(
				player.getPos().getX(),
				0,
				player.getPos().getZ()
			);
			final Vec3d herobrineXZ = new Vec3d(
				herobrine.getPos().getX(),
				0,
				herobrine.getPos().getZ()
			);
			final double playerDistanceXZ = playerXZ.distanceTo(herobrineXZ);
			final Vec3d towardsPlayer = Algorithms.getDirectionPostoPos(
				herobrineXZ,
				playerXZ
			);

			// Calculate spawn position
			Vec3d spawnPos = Algorithms.getPosOffsetInDirection(
				herobrine.getPos(),
				towardsPlayer,
				(float) Math.max(
					0,
					playerDistanceXZ - Algorithms.RANDOM.nextBetween(creepDistanceMin, creepDistanceMax)
				)
			);
			final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
				world,
				Algorithms.getBlockPosFromVec3d(spawnPos),
				player.getBlockPos().getY() - creepVerticleDistanceMax,
				player.getBlockPos().getY() + creepVerticleDistanceMax
			);
			spawnPos = new Vec3d(
				spawnPos.getX(),
				spawnBlockPos.getY() + 1, // Spawn on block while keeping X/Z offset
				spawnPos.getZ()
			);
			if (!Algorithms.canPlayerStandOnBlock(world, Algorithms.getBlockPosFromVec3d(spawnPos).down())) continue;
			herobrine.setPosition(spawnPos);
			herobrine.lookAt(player);
		}
	}

	public static void onShutdown() {
		for (final HerobrineEntity herobrine : herobrineTrackers.keySet()) {
			herobrine.remove();
		}
		herobrineTrackers.clear();
	}

	public static boolean creep(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return false;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < creepHauntLevelMin) return true; // Reset event as if it passed
		}

		final World world = player.getWorld();
		// Get the block behind the player
		final BlockPos playerBlockPos = player.getBlockPos();
		Vec3d spawnPos = Algorithms.getPosOffsetInDirection(
			player.getPos(),
			player.getRotationVector().negate(),
			Algorithms.RANDOM.nextBetween(creepDistanceMin, creepDistanceMax)
		);
		final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
			player.getWorld(),
			Algorithms.getBlockPosFromVec3d(spawnPos),
			playerBlockPos.getY() - creepVerticleDistanceMax,
			playerBlockPos.getY() + creepVerticleDistanceMax
		);
		spawnPos = new Vec3d(
			spawnPos.getX(),
			spawnBlockPos.getY() + 1, // Keep X/Z offset
			spawnPos.getZ()
		);
		if (!Algorithms.canPlayerStandOnBlock(world, Algorithms.getBlockPosFromVec3d(spawnPos).down())) return false;

		final HerobrineEntity herobrine = new HerobrineEntity(world, "smile");
		herobrine.setPosition(spawnPos);
		herobrine.lookAt(player);
		herobrine.summon();
		herobrineTrackers.put(herobrine, player);

		return true;
	}
}
