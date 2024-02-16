package com.sylvan.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Footsteps {
	public static void generateFootsteps(final PlayerEntity player, final int footstepCount) {
		final BlockPos blockPos = player.getBlockPos().offset(Direction.DOWN);
		final Direction behindPlayer = player.getHorizontalFacing().getOpposite();
		// Play footstep on each block approaching the player
		for (int distance = footstepCount; distance > 0; --distance) {
			playFootstep(player, blockPos.offset(behindPlayer, distance));
			// TODO Wait one tick
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
