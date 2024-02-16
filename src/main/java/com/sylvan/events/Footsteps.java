package com.sylvan.events;

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
		Presence.scheduler.schedule(() -> {
			generateFootsteps(player, Presence.RANDOM.nextBetween(Presence.config.footstepsMinSteps, Presence.config.footstepsMaxSteps));
			scheduleEvent(player);
		}, Presence.RANDOM.nextBetween(Presence.config.footstepsMinDelay, Presence.config.footstepsMaxDelay), TimeUnit.SECONDS);
	}

	public static void generateFootsteps(final PlayerEntity player, final int footstepCount) {
		final int msPerStep = (Presence.config.footstepsReflexTime + Presence.RANDOM.nextBetween(0, Presence.config.footstepsMaxReflexVariance)) / footstepCount;

		final BlockPos blockPos = player.getBlockPos().offset(Direction.DOWN);
		final Direction behindPlayer = player.getHorizontalFacing().getOpposite();
		int delay;
		// Play footstep on each block approaching the player
		for (int distance = footstepCount; distance > 0; --distance) {
			delay = (footstepCount - distance) * msPerStep + Presence.RANDOM.nextBetween(0, Presence.config.footstepsMaxStepVariance);
			final int blockDistance = distance;
			Presence.scheduler.schedule(() -> {
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
			SoundCategory.BLOCKS,
			1.0f,
			1.0f
		);
	}
}
