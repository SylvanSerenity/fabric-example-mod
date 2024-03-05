package com.sylvan.presence.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FlickerDoor {
	// Config
	public static boolean flickerDoorEnabled = true;		// Whether the flicker door event is active
	public static float flickerDoorHauntLevelMin = 1.25f;		// The minimum haunt level to play event
	private static int flickerDoorDelayMin = 60 * 60;		// The minimum delay between flicker door events
	private static int flickerDoorDelayMax = 60 * 60 * 4;		// The maximum delay between flicker door events
	private static int flickerDoorFlickerMin = 1;			// The minimum number of times to flicker the door
	private static int flickerDoorFlickerMax = 4;			// The maximum number of times to flicker the door
	private static int flickerDoorFlickerDelayMin = 100;		// The minimum delay between using the door and it flickering in milliseconds
	private static int flickerDoorFlickerDelayMax = 1000;		// The maximum delay between using the door and it flickering in milliseconds
	private static int flickerDoorFlickerIntervalMin = 20;		// The minimum interval between flickering the door in milliseconds
	private static int flickerDoorFlickerIntervalMax = 150;		// The maximum interval between flickering the door in milliseconds
	private static boolean flickerDoorClosedConstraint = true;	// Whether the door should start flickering only when closing the door

	private static final List<UUID> trackedPlayersMap = new ArrayList<>();

	public static void loadConfig() {
		try {
			flickerDoorEnabled = Presence.config.getOrSetValue("flickerDoorEnabled", flickerDoorEnabled).getAsBoolean();
			flickerDoorHauntLevelMin = Presence.config.getOrSetValue("flickerDoorHauntLevelMin", flickerDoorHauntLevelMin).getAsFloat();
			flickerDoorDelayMin = Presence.config.getOrSetValue("flickerDoorDelayMin", flickerDoorDelayMin).getAsInt();
			flickerDoorDelayMax = Presence.config.getOrSetValue("flickerDoorDelayMax", flickerDoorDelayMax).getAsInt();
			flickerDoorFlickerMin = Presence.config.getOrSetValue("flickerDoorFlickerMin", flickerDoorFlickerMin).getAsInt();
			flickerDoorFlickerMax = Presence.config.getOrSetValue("flickerDoorFlickerMax", flickerDoorFlickerMax).getAsInt();
			flickerDoorFlickerDelayMin = Presence.config.getOrSetValue("flickerDoorFlickerDelayMin", flickerDoorFlickerDelayMin).getAsInt();
			flickerDoorFlickerDelayMax = Presence.config.getOrSetValue("flickerDoorFlickerDelayMax", flickerDoorFlickerDelayMax).getAsInt();
			flickerDoorFlickerIntervalMin = Presence.config.getOrSetValue("flickerDoorFlickerIntervalMin", flickerDoorFlickerIntervalMin).getAsInt();
			flickerDoorFlickerIntervalMax = Presence.config.getOrSetValue("flickerDoorFlickerIntervalMax", flickerDoorFlickerIntervalMax).getAsInt();
			flickerDoorClosedConstraint = Presence.config.getOrSetValue("flickerDoorClosedConstraint", flickerDoorClosedConstraint).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for FlickerDoor.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleTracking(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				trackPlayer(player, false);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(flickerDoorDelayMin, hauntLevel),
				Algorithms.divideByFloat(flickerDoorDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void trackPlayer(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < flickerDoorHauntLevelMin) return; // Reset event as if it passed
		}

		if (!trackedPlayersMap.contains(player.getUuid())) trackedPlayersMap.add(player.getUuid());
	}

	public static void onUseBlock(final PlayerEntity player, final World world, final BlockHitResult hitResult) {
		if (!flickerDoorEnabled) return;

		final BlockPos doorPos = hitResult.getBlockPos();
		if (
			!Algorithms.isBlockOfBlockTypes(world.getBlockState(doorPos).getBlock(), OpenDoor.doorBlocks) ||	// Must be a door
			(flickerDoorClosedConstraint && !world.getBlockState(doorPos).get(DoorBlock.OPEN))			// Must be closing door
		) return;

		if (trackedPlayersMap.contains(player.getUuid())) {
			int delay = Algorithms.RANDOM.nextBetween(flickerDoorFlickerDelayMin, flickerDoorFlickerDelayMax);
			for (int flickerCount = Algorithms.RANDOM.nextBetween(flickerDoorFlickerMin, flickerDoorFlickerMax); flickerCount > 0; --flickerCount) {
				Events.scheduler.schedule(() -> {
					flickerDoor(player, doorPos);
				}, delay, TimeUnit.MILLISECONDS);
				delay += Algorithms.RANDOM.nextBetween(flickerDoorFlickerIntervalMin, flickerDoorFlickerIntervalMax);
			}
			trackedPlayersMap.remove(player.getUuid());
		}
	}

	public static void flickerDoor(final PlayerEntity player, final BlockPos doorPos) {
		if (player.isRemoved()) return;

		final World world = player.getWorld();
		final BlockState currentBlockState = world.getBlockState(doorPos);
		if (currentBlockState.get(DoorBlock.OPEN)) {
			// Close door
			world.setBlockState(doorPos, currentBlockState.with(DoorBlock.OPEN, false));

			// Play flicker door sound
			if (currentBlockState.getSoundGroup() == BlockSoundGroup.BAMBOO_WOOD) world.playSound(null, doorPos, SoundEvents.BLOCK_BAMBOO_WOOD_DOOR_CLOSE, SoundCategory.BLOCKS);
			else if (currentBlockState.getSoundGroup() == BlockSoundGroup.CHERRY_WOOD) world.playSound(null, doorPos, SoundEvents.BLOCK_CHERRY_WOOD_DOOR_CLOSE, SoundCategory.BLOCKS);
			else if (currentBlockState.getSoundGroup() == BlockSoundGroup.NETHER_WOOD) world.playSound(null, doorPos, SoundEvents.BLOCK_NETHER_WOOD_DOOR_CLOSE, SoundCategory.BLOCKS);
			else world.playSound(null, doorPos, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS);
		} else {
			// Open door
			world.setBlockState(doorPos, currentBlockState.with(DoorBlock.OPEN, true));

			// Play flicker door sound
			if (currentBlockState.getSoundGroup() == BlockSoundGroup.BAMBOO_WOOD) world.playSound(null, doorPos, SoundEvents.BLOCK_BAMBOO_WOOD_DOOR_OPEN, SoundCategory.BLOCKS);
			else if (currentBlockState.getSoundGroup() == BlockSoundGroup.CHERRY_WOOD) world.playSound(null, doorPos, SoundEvents.BLOCK_CHERRY_WOOD_DOOR_OPEN, SoundCategory.BLOCKS);
			else if (currentBlockState.getSoundGroup() == BlockSoundGroup.NETHER_WOOD) world.playSound(null, doorPos, SoundEvents.BLOCK_NETHER_WOOD_DOOR_OPEN, SoundCategory.BLOCKS);
			else world.playSound(null, doorPos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS);
		}
	}
}
