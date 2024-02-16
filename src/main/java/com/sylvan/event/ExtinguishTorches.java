package com.sylvan.event;

import java.util.*;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;

public class ExtinguishTorches {
	public static Map<UUID, List<Map.Entry<DimensionType, BlockPos>>> torchPlacementMap = new HashMap<>();

	public static void loadSaveData(LevelStorage.Session session) {
		// TODO
	}

	public static void scheduleEvent() {
		// TODO Schedule torch placement tracking
		// TODO Schedule removal of torches
	}

	public static void startTrackingTorches(final PlayerEntity player) {
		torchPlacementMap.put(player.getUuid(), new ArrayList<>());
	}

	public static void removeTrackedTorches(final PlayerEntity player) {
		if (!torchPlacementMap.containsKey(player.getUuid()));

		final World world = player.getWorld();
		final DimensionType playerDimension = world.getDimension();
		DimensionType torchDimension;
		BlockPos torchPos;
		for (Map.Entry<DimensionType, BlockPos> torch : torchPlacementMap.get(player.getUuid())) {
			torchDimension = torch.getKey();
			torchPos = torch.getValue();
			if (
				playerDimension == torchDimension &&
				!playerCanSeeBlock(player, torchPos)
			) {
				world.removeBlock(torchPos, false);
			}
		}

		world.playSound(player, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS);
		torchPlacementMap.remove(player.getUuid());
	}

	public static boolean playerCanSeeBlock(final PlayerEntity player, final BlockPos blockPos) {
		Vec3d vec3d = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
		Vec3d vec3d2 = new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		if (vec3d2.distanceTo(vec3d) > 128.0) return false;
		return player.getWorld().raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player)).getType() == HitResult.Type.MISS;
	}
}
