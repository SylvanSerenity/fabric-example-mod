package com.sylvan.presence.event;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class OpenDoor {
	// Config
	public static boolean openDoorEnabled = true;			// Whether the open door event is active
	public static float openDoorHauntLevelMin = 1.25f;		// The minimum haunt level to play event
	private static int openDoorDelayMin = 60 * 60;			// The minimum delay between open door events
	private static int openDoorDelayMax = 60 * 60 * 4;		// The maximum delay between open door events
	private static int openDoorRetryDelay = 60;			// The delay between retrying an attack if the previous attempt failed
	private static int openDoorDistanceMin = 32;			// The minimum distance of the door to open
	private static int openDoorDistanceMax = 100;			// The maximum distance of the door to open
	private static boolean openDoorNotSeenConstraint = true;	// Whether the constraint for making the door open only when not seen is active
	private static boolean openDoorPlaySound = true;		// Whether or not to play the door open sound

	private static final ArrayList<Block> doorBlocks = new ArrayList<>();

	public static void loadConfig() {
		try {
			openDoorEnabled = Presence.config.getOrSetValue("openDoorEnabled", openDoorEnabled).getAsBoolean();
			openDoorHauntLevelMin = Presence.config.getOrSetValue("openDoorHauntLevelMin", openDoorHauntLevelMin).getAsFloat();
			openDoorDelayMin = Presence.config.getOrSetValue("openDoorDelayMin", openDoorDelayMin).getAsInt();
			openDoorDelayMax = Presence.config.getOrSetValue("openDoorDelayMax", openDoorDelayMax).getAsInt();
			openDoorRetryDelay = Presence.config.getOrSetValue("openDoorRetryDelay", openDoorRetryDelay).getAsInt();
			openDoorDistanceMin = Presence.config.getOrSetValue("openDoorDistanceMin", openDoorDistanceMin).getAsInt();
			openDoorDistanceMax = Presence.config.getOrSetValue("openDoorDistanceMax", openDoorDistanceMax).getAsInt();
			openDoorNotSeenConstraint = Presence.config.getOrSetValue("openDoorNotSeenConstraint", openDoorNotSeenConstraint).getAsBoolean();
			openDoorPlaySound = Presence.config.getOrSetValue("openDoorPlaySound", openDoorPlaySound).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for OpenDoor.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		doorBlocks.add(Blocks.ACACIA_DOOR);
		doorBlocks.add(Blocks.BAMBOO_DOOR);
		doorBlocks.add(Blocks.BIRCH_DOOR);
		doorBlocks.add(Blocks.CHERRY_DOOR);
		doorBlocks.add(Blocks.CRIMSON_DOOR);
		doorBlocks.add(Blocks.DARK_OAK_DOOR);
		doorBlocks.add(Blocks.JUNGLE_DOOR);
		doorBlocks.add(Blocks.MANGROVE_DOOR);
		doorBlocks.add(Blocks.OAK_DOOR);
		doorBlocks.add(Blocks.SPRUCE_DOOR);
		doorBlocks.add(Blocks.WARPED_DOOR);
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(openDoorDelayMin, hauntLevel),
				Algorithms.divideByFloat(openDoorDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				if (openDoor(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(openDoorDelayMin, hauntLevel),
							Algorithms.divideByFloat(openDoorDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, openDoorRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean openDoor(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return false;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < openDoorHauntLevelMin) return true; // Reset event as if it passed
		}

		// Get nearest door position
		BlockPos nearestDoorPos = Algorithms.getNearestBlockToEntity(player, doorBlocks, openDoorDelayMax);
		if (nearestDoorPos == null) return false;

		// Player must not see door open
		if (
			openDoorNotSeenConstraint && (
				Algorithms.couldPosBeSeenByEntity(player, nearestDoorPos.toCenterPos()) ||
				Algorithms.couldPosBeSeenByEntity(player, nearestDoorPos.up().toCenterPos())
			)
		) return false;

		final World world = player.getWorld();
		final BlockState currentBlockState = world.getBlockState(nearestDoorPos);
		world.setBlockState(nearestDoorPos, currentBlockState.with(DoorBlock.OPEN, true));
		if (openDoorPlaySound) {
			if (currentBlockState.getSoundGroup() == BlockSoundGroup.BAMBOO_WOOD) player.playSound(SoundEvents.BLOCK_BAMBOO_WOOD_DOOR_OPEN, SoundCategory.BLOCKS, 1, 1);
			else if (currentBlockState.getSoundGroup() == BlockSoundGroup.CHERRY_WOOD) player.playSound(SoundEvents.BLOCK_CHERRY_WOOD_DOOR_OPEN, SoundCategory.BLOCKS, 1, 1);
			else if (currentBlockState.getSoundGroup() == BlockSoundGroup.NETHER_WOOD) player.playSound(SoundEvents.BLOCK_NETHER_WOOD_DOOR_OPEN, SoundCategory.BLOCKS, 1, 1);
			else player.playSound(SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 1, 1);
		}

		return true;
	}
}
