package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.MiningEntity;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Mine {

	// Config
	public static boolean mineEnabled = true;			// Whether the nearby sounds event is active
	public static float mineHauntLevelMin = 1.75f;		// The minimum haunt level to play event
	private static int mineDelayMin = 60 * 45;			// The minimum delay between nearby sounds events
	private static int mineDelayMax = 60 * 60 * 3;		// The maximum delay between nearby sounds events
	private static int mineRetryDelay = 60 * 5;			// The maximum delay between nearby sounds events
	private static int mineDistanceMin = 3;				// The minimum distance to start mining towards the player.
	private static int mineDistanceMax = 12;			// The maximum distance to start mining towards the player.
	public static int mineBlocksMin = 1;				// The minimum number of blocks to mine towards the player.
	public static int mineBlocksMax = 5;				// The maximum number of blocks to mine towards the player.
	public static int mineTicksPerBreakProgress = 3;	// The number of ticks per 10% breaking progress.
	public static boolean mineBreakBlock = true;		// Whether to actually break the block.
	public static boolean mineLootBlock = true;			// Whether to drop the block's loot when mined.

	private static final List<MiningEntity> miningEntities = new ArrayList<>();

	public static void loadConfig() {
		try {
			mineEnabled = Presence.config.getOrSetValue("mineEnabled", mineEnabled).getAsBoolean();
			mineHauntLevelMin = Presence.config.getOrSetValue("mineHauntLevelMin", mineHauntLevelMin).getAsFloat();
			mineDelayMin = Presence.config.getOrSetValue("mineDelayMin", mineDelayMin).getAsInt();
			mineDelayMax = Presence.config.getOrSetValue("mineDelayMax", mineDelayMax).getAsInt();
			mineRetryDelay = Presence.config.getOrSetValue("mineRetryDelay", mineRetryDelay).getAsInt();
			mineDistanceMin = Presence.config.getOrSetValue("mineDistanceMin", mineDistanceMin).getAsInt();
			mineDistanceMax = Presence.config.getOrSetValue("mineDistanceMax", mineDistanceMax).getAsInt();
			mineBlocksMin = Presence.config.getOrSetValue("mineBlocksMin", mineBlocksMin).getAsInt();
			mineBlocksMax = Presence.config.getOrSetValue("mineBlocksMax", mineBlocksMax).getAsInt();
			mineTicksPerBreakProgress = Presence.config.getOrSetValue("mineTicksPerBreakProgress", mineTicksPerBreakProgress).getAsInt();
			mineBreakBlock = Presence.config.getOrSetValue("mineBreakBlock", mineBreakBlock).getAsBoolean();
			mineLootBlock = Presence.config.getOrSetValue("mineLootBlock", mineLootBlock).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for mine.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
				player,
				Algorithms.RANDOM.nextBetween(
						Algorithms.divideByFloat(mineDelayMin, hauntLevel),
						Algorithms.divideByFloat(mineDelayMax, hauntLevel)
				)
		);
	}

	public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
				() -> {
					if (player.isRemoved()) return;
					if (startMiningTowardsPlayer(player, false)) {
						scheduleEventWithDelay(
								player,
								Algorithms.RANDOM.nextBetween(
										Algorithms.divideByFloat(mineDelayMin, hauntLevel),
										Algorithms.divideByFloat(mineDelayMax, hauntLevel)
								)
						);
					} else {
						// Retry if it is a bad time
						scheduleEventWithDelay(player, mineRetryDelay);
					}
				},
				delay, TimeUnit.SECONDS
		);
	}

	public static void onWorldTick(final ServerWorld world) {
		if (miningEntities.isEmpty()) return;
		Iterator<MiningEntity> it = miningEntities.iterator();
		MiningEntity miningEntity;
		while (it.hasNext()) {
			miningEntity = it.next();
			final PlayerEntity player = miningEntity.getTrackedPlayer();

			// Remove if player leaves or is in another dimension
			if (player.isRemoved() || player.getEntityWorld().getDimension() != miningEntity.getWorld().getDimension() || miningEntity.shouldRemove()) {
				it.remove();
				continue;
			}
			if (player.getEntityWorld().getDimension() != world.getDimension()) continue;

			miningEntity.tick();
		}
	}

	public static boolean startMiningTowardsPlayer(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return true;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < mineHauntLevelMin) return true; // Reset event as if it passed
		}

		if (!Algorithms.isEntityInCave(player)) return false;

		miningEntities.add(
			new MiningEntity(
				player,
				Algorithms.getRandomCaveBlockNearEntity(player, mineDistanceMin, mineDistanceMax, 20, true),
				Algorithms.RANDOM.nextBetween(mineBlocksMin, mineBlocksMax)
			)
		);
		return true;
	}
}
