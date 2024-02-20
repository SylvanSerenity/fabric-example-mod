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

public class WaitBehind {
	// Config
	public static boolean waitBehindEnabled = true;		// Whether the wait behind event is active
	public static int waitBehindDelayMin = 60 * 45;		// The minimum delay between wait behind events
	public static int waitBehindDelayMax = 60 * 60 * 5;	// The maximum delay between wait behind events
	public static int waitBehindRetryDelay = 1;		// The delay between retrying wait behind events in case of failure
	private static int waitBehindDistanceMin = 1;		// The minimum distance behind the player to summon Herobrine
	private static int waitBehindDistanceMax = 1;		// The maximum distance behind the player to summon Herobrine
	private static int waitBehindVerticleDistanceMax = 3;	// The maximum distance Herobrine can be above/below the player
	private static int waitBehindReflexMs = 0;		// The time in milliseconds before Herobrine vanishes

	public static final Map<HerobrineEntity, PlayerEntity> herobrineTrackers = new HashMap<>();

	public static void loadConfig() {
		try {
			waitBehindEnabled = Presence.config.getOrSetValue("waitBehindEnabled", waitBehindEnabled).getAsBoolean();
			waitBehindDelayMin = Presence.config.getOrSetValue("waitBehindDelayMin", waitBehindDelayMin).getAsInt();
			waitBehindDelayMax = Presence.config.getOrSetValue("waitBehindDelayMax", waitBehindDelayMax).getAsInt();
			waitBehindRetryDelay = Presence.config.getOrSetValue("waitBehindRetryDelay", waitBehindRetryDelay).getAsInt();
			waitBehindDistanceMin = Presence.config.getOrSetValue("waitBehindDistanceMin", waitBehindDistanceMin).getAsInt();
			waitBehindDistanceMax = Presence.config.getOrSetValue("waitBehindDistanceMax", waitBehindDistanceMax).getAsInt();
			waitBehindVerticleDistanceMax = Presence.config.getOrSetValue("waitBehindVerticleDistanceMax", waitBehindVerticleDistanceMax).getAsInt();
			waitBehindReflexMs = Presence.config.getOrSetValue("waitBehindReflexMs", waitBehindReflexMs).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Footsteps.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				if (waitBehind(player)) {
					scheduleEvent(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(waitBehindDelayMin, hauntLevel),
							Algorithms.divideByFloat(waitBehindDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEvent(player, waitBehindRetryDelay);
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
			if (herobrine.isSeenByPlayers(0.2)) {
				if (waitBehindReflexMs > 0) herobrine.scheduleRemoval(waitBehindReflexMs);
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
					playerDistanceXZ - Algorithms.RANDOM.nextBetween(waitBehindDistanceMin, waitBehindDistanceMax)
				)
			);
			final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
				world,
				Algorithms.getBlockPosFromVec3d(spawnPos),
				player.getBlockPos().getY() - waitBehindVerticleDistanceMax,
				player.getBlockPos().getY() + waitBehindVerticleDistanceMax
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

	public static boolean waitBehind(final PlayerEntity player) {
		if (player.isRemoved()) return false;

		final World world = player.getWorld();
		// Get the block behind the player
		final BlockPos playerBlockPos = player.getBlockPos();
		Vec3d spawnPos = Algorithms.getPosOffsetInDirection(
			player.getPos(),
			player.getRotationVector().negate(),
			Algorithms.RANDOM.nextBetween(waitBehindDistanceMin, waitBehindDistanceMax)
		);
		final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
			player.getWorld(),
			Algorithms.getBlockPosFromVec3d(spawnPos),
			playerBlockPos.getY() - waitBehindVerticleDistanceMax,
			playerBlockPos.getY() + waitBehindVerticleDistanceMax
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
