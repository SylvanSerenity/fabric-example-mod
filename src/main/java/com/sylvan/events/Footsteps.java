package com.sylvan.events;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Footsteps {
	public static final int REFLEX_TIME = 600;
	public static final int MIN_STEPS = 1;
	public static final int MAX_STEPS = 5;

	public static void generateFootsteps(final PlayerEntity player, final int footstepCount) {
		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		final int msPerStep = REFLEX_TIME / footstepCount;

		final BlockPos blockPos = player.getBlockPos().offset(Direction.DOWN);
		final Direction behindPlayer = player.getHorizontalFacing().getOpposite();
		int delay;
		// Play footstep on each block approaching the player
		for (int distance = footstepCount; distance > 0; --distance) {
			delay = (footstepCount - distance) * msPerStep;
			final int blockDistance = distance;
			scheduler.schedule(() -> {
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
