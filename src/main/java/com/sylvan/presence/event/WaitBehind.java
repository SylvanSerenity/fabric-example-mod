package com.sylvan.presence.event;

import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.HerobrineEntity;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WaitBehind {
	// Config
	public static boolean waitBehindEnabled = true;		// Whether the wait behind event is active
	public static int waitBehindDelayMin = 60 * 45;		// The minimum delay between wait behind events
	public static int waitBehindDelayMax = 60 * 60 * 5;	// The maximum delay between wait behind events
	public static int waitBehindRetryDelay = 60;		// The delay between retrying wait behind events in case of failure
	public static int waitBehindDistanceMin = 1;		// The minimum distance behind the player to summon Herobrine
	public static int waitBehindDistanceMax = 1;		// The maximum distance behind the player to summon Herobrine

	public static void loadConfig() {
		try {
			waitBehindEnabled = Presence.config.getOrSetValue("waitBehindEnabled", waitBehindEnabled).getAsBoolean();
			waitBehindDelayMin = Presence.config.getOrSetValue("waitBehindDelayMin", waitBehindDelayMin).getAsInt();
			waitBehindDelayMax = Presence.config.getOrSetValue("waitBehindDelayMax", waitBehindDelayMax).getAsInt();
			waitBehindRetryDelay = Presence.config.getOrSetValue("waitBehindRetryDelay", waitBehindRetryDelay).getAsInt();
			waitBehindDistanceMin = Presence.config.getOrSetValue("waitBehindDistanceMin", waitBehindDistanceMin).getAsInt();
			waitBehindDistanceMax = Presence.config.getOrSetValue("waitBehindDistanceMax", waitBehindDistanceMax).getAsInt();
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

	public static boolean waitBehind(final PlayerEntity player) {
		if (player.isRemoved()) return false;

		final World world = player.getWorld();
		final Direction facing = player.getHorizontalFacing();
		// Get the block behind the player
		// TODO Get position from vector so it isn't forced to spawn in cardinal directions
		final BlockPos standPos = player.getBlockPos().offset(
			facing.getOpposite(),
			Algorithms.RANDOM.nextBetween(waitBehindDistanceMin, waitBehindDistanceMax)
		);
		if (!Algorithms.canPlayerStandOnBlock(world, standPos)) return false;

		final HerobrineEntity herobrine = new HerobrineEntity(world, "smile");
		herobrine.setPosition(standPos.toCenterPos());
		herobrine.lookAt(player);
		herobrine.summon();

		// TODO Remove when player looks
		// herobrine.remove();

		return true;
	}
}
