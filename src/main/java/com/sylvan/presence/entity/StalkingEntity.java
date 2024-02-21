package com.sylvan.presence.entity;

import com.sylvan.presence.event.Stalk;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class StalkingEntity extends HerobrineEntity {
	private enum StalkingState {
		WATCHING,
		TURNING,
		WALKING
	};

	private PlayerEntity trackedPlayer;
	private StalkingState stalkingState = StalkingState.WATCHING;
	private boolean shouldRemove = false;
	private long ticksSeen = 0;
	private float yawTurnPerTick;
	private long turningTicks = 0;

	public StalkingEntity(World world, String skin, final PlayerEntity trackedPlayer) {
		super(world, skin);
		this.trackedPlayer = trackedPlayer;
	}

	public void tick() {
		switch (stalkingState) {
			case WATCHING: {
				tickWatching();
			} break;
			case TURNING: {
				tickTurning();
			} break;
			case WALKING: {
				tickWalking();
			} break;
		}
	}

	private void tickWatching() {
		// Turn and walk away if seen for too long
		if (this.isSeenByPlayers(Stalk.stalkLookAtThreshold)) {
			++ticksSeen;
		
			// Pretend player and Herobrine are on the same block to prevent direction from being dependent on Y-axis
			final Vec3d playerXZ = new Vec3d(
				trackedPlayer.getPos().getX(),
				0,
				trackedPlayer.getPos().getZ()
			);
			final Vec3d herobrineXZ = new Vec3d(
				this.getPos().getX(),
				0,
				this.getPos().getZ()
			);
			final double playerDistanceXZ = playerXZ.distanceTo(herobrineXZ);
	
			if (ticksSeen > Stalk.stalkSeenTicksMax) {
				stalkingState = StalkingState.TURNING;
				final Vec3d towardsPlayer = Algorithms.getDirectionPostoPos(playerXZ, herobrineXZ);
				final float startYaw = (float) this.getYaw();
				final float yawGoal = (float) Algorithms.directionToAngles(towardsPlayer).getYaw();
				yawTurnPerTick = (yawGoal - startYaw) / Stalk.stalkTurningTicks;
			}
		} else {
			// Look at player
			this.lookAt(trackedPlayer);
		}
	}

	private void tickTurning() {
		++turningTicks;

		final float newYaw = this.getYaw() + yawTurnPerTick;
		this.setBodyRotation(newYaw);
		this.setHeadRotation(0, newYaw, 0);

		// Start walking away
		if (turningTicks >= Stalk.stalkTurningTicks) stalkingState = StalkingState.WALKING;
	}

	private void tickWalking() {
		// TODO Walk away from player until unseen
		// TODO Set walk animation
		final float awayFromPlayerYaw = (float) Algorithms.getDirectionPostoPos(trackedPlayer.getEyePos(), this.getEyePos()).getY();
		this.move(this.getRotationVector().multiply(Stalk.stalkMovementSpeed));
		this.setBodyRotation(awayFromPlayerYaw); // TODO Get direction from player to entity
		this.setHeadRotation(0, awayFromPlayerYaw, 0);
		if (this.getPos().distanceTo(trackedPlayer.getPos()) > Stalk.stalkVanishDistance) {
			shouldRemove = true;
		}
	}

	public PlayerEntity getTrackedPlayer() {
		return trackedPlayer;
	}

	public boolean shouldRemove() {
		return shouldRemove;
	}
}
