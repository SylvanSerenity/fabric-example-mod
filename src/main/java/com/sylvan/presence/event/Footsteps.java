package com.sylvan.presence.event;

import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Footsteps {
	// Config
	protected static boolean footstepsEnabled = true;
	private static int footstepsDelayMin = 60 * 60;
	private static int footstepsDelayMax = 60 * 60 * 4;
	private static int footstepsReflexMs = 500;
	private static int footstepsMaxReflexVariance = 150;
	private static int footstepsStepsMin = 1;
	private static int footstepsStepsMax = 5;
	private static int footstepsMsPerStepMax = 300;
	private static int footstepsStepVarianceMax = 25;

	private static void loadConfig() {
		try {
			footstepsEnabled = Presence.config.getOrSetValue("footstepsEnabled", footstepsEnabled).getAsBoolean();
			footstepsDelayMin = Presence.config.getOrSetValue("footstepsDelayMin", footstepsDelayMin).getAsInt();
			footstepsDelayMax = Presence.config.getOrSetValue("footstepsDelayMax", footstepsDelayMax).getAsInt();
			footstepsReflexMs = Presence.config.getOrSetValue("footstepsReflexMs", footstepsReflexMs).getAsInt();
			footstepsMaxReflexVariance = Presence.config.getOrSetValue("footstepsMaxReflexVariance", footstepsMaxReflexVariance).getAsInt();
			footstepsStepsMin = Presence.config.getOrSetValue("footstepsStepsMin", footstepsStepsMin).getAsInt();
			footstepsStepsMax = Presence.config.getOrSetValue("footstepsStepsMax", footstepsStepsMax).getAsInt();
			footstepsMsPerStepMax = Presence.config.getOrSetValue("footstepsMsPerStepMax", footstepsMsPerStepMax).getAsInt();
			footstepsStepVarianceMax = Presence.config.getOrSetValue("footstepsStepVarianceMax", footstepsStepVarianceMax).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Footsteps.java. Wiping and using default default.", e);
			Presence.config.clearConfig();
			Events.initEvents();
		}
	}

	public static void initEvent() {
		loadConfig();
	}

	public static void scheduleEvent(final PlayerEntity player) {
		Events.scheduler.schedule(
			() -> {
				generateFootsteps(player, Algorithms.RANDOM.nextBetween(footstepsStepsMin, footstepsStepsMax));
				if (!player.isRemoved()) scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				footstepsDelayMin,
				footstepsDelayMax
			), TimeUnit.SECONDS
		);
	}

	public static void generateFootsteps(final PlayerEntity player, final int footstepCount) {
		if (player.isRemoved()) return;

		final int msPerStep = (
			(footstepCount > 2) ?
			Math.min(
				(footstepsReflexMs + Algorithms.RANDOM.nextBetween(0, footstepsMaxReflexVariance)) / footstepCount,
				footstepsMsPerStepMax
			) : (footstepsReflexMs + Algorithms.RANDOM.nextBetween(0, footstepsMaxReflexVariance)) / footstepCount
		);

		final BlockPos blockPos = player.getBlockPos().down();
		final Direction behindPlayer = player.getHorizontalFacing().getOpposite();
		int delay;
		// Play footstep on each block approaching the player
		for (int distance = footstepCount; distance > 0; --distance) {
			delay = (footstepCount - distance) * msPerStep + Algorithms.RANDOM.nextBetween(0, footstepsStepVarianceMax);
			final int blockDistance = distance;
			Events.scheduler.schedule(() -> {
				playFootstep(player, blockPos.offset(behindPlayer, blockDistance));
			}, delay, TimeUnit.MILLISECONDS);
		}
	}

	public static void playFootstep(final PlayerEntity player, BlockPos soundPos) {
		final World world = player.getWorld();
		final BlockPos playerPos = player.getBlockPos();

		// Player must be able to stand on source block
		soundPos = Algorithms.getNearestStandableBlockPos(
			world,
			soundPos,
			playerPos.getY() - footstepsStepsMax,
			playerPos.getY() + footstepsStepsMax
		);
		if (!Algorithms.canPlayerStandOnBlock(world, soundPos)) return;

		// Play the sound of the block the foot steps on
		final SoundEvent stepSound = world.getBlockState(soundPos).getSoundGroup().getStepSound();
		world.playSound(null, soundPos, stepSound, SoundCategory.BLOCKS);
	}
}
