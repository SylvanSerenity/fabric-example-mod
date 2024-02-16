package com.sylvan.event;

import java.util.concurrent.TimeUnit;

import com.sylvan.Presence;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Footsteps {
	public static void scheduleEvent(final PlayerEntity player) {
		Events.scheduler.schedule(() -> {
			generateFootsteps(player, Presence.RANDOM.nextBetween(Presence.config.footstepsStepsMin, Presence.config.footstepsStepsMax));
			scheduleEvent(player);
		}, Presence.RANDOM.nextBetween(Presence.config.footstepsDelayMin, Presence.config.footstepsDelayMax), TimeUnit.SECONDS);
	}

	public static void generateFootsteps(final PlayerEntity player, final int footstepCount) {
		final int msPerStep = (
			(footstepCount > 2) ?
			Math.min(
				(Presence.config.footstepsReflexMs + Presence.RANDOM.nextBetween(0, Presence.config.footstepsMaxReflexVariance)) / footstepCount,
				Presence.config.footstepsMsPerStepMax
			) : (Presence.config.footstepsReflexMs + Presence.RANDOM.nextBetween(0, Presence.config.footstepsMaxReflexVariance)) / footstepCount
		);

		final BlockPos blockPos = player.getBlockPos().offset(Direction.DOWN);
		final Direction behindPlayer = player.getHorizontalFacing().getOpposite();
		int delay;
		// Play footstep on each block approaching the player
		for (int distance = footstepCount; distance > 0; --distance) {
			delay = (footstepCount - distance) * msPerStep + Presence.RANDOM.nextBetween(0, Presence.config.footstepsStepVarianceMax);
			final int blockDistance = distance;
			Events.scheduler.schedule(() -> {
				playFootstep(player, blockPos.offset(behindPlayer, blockDistance));
			}, delay, TimeUnit.MILLISECONDS);
		}
	}

	public static void playFootstep(final PlayerEntity player, final BlockPos blockPos) {
		final World world = player.getWorld();
		// Test if a player could stand on source block
		if (
			world.getBlockState(blockPos).isAir() ||
			!world.getBlockState(blockPos.offset(Direction.UP, 1)).isAir() ||
			!world.getBlockState(blockPos.offset(Direction.UP, 2)).isAir()
		) return;

		// Play the sound of the block distance blocks behind the player
		final SoundEvent stepSound = world.getBlockState(blockPos).getSoundGroup().getStepSound();

		world.playSound(
			null,
			blockPos,
			stepSound,
			SoundCategory.BLOCKS
		);
	}
}
