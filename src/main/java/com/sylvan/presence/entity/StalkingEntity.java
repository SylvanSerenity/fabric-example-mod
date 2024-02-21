package com.sylvan.presence.entity;

import com.sylvan.presence.event.Stalk;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
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
	private float lastYaw;
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
	
			if (ticksSeen > Stalk.stalkSeenTicksMax) {
				stalkingState = StalkingState.TURNING;
				final Vec3d awayFromPlayer = Algorithms.getDirectionPosToPos(playerXZ, herobrineXZ);
				lastYaw = (float) this.getYaw();
				final float yawGoal = (float) Algorithms.directionToAngles(awayFromPlayer).getYaw();
				yawTurnPerTick = (yawGoal - lastYaw) / Stalk.stalkTurningTicks;
			}
		} else {
			// Look at player
			this.lookAt(trackedPlayer);
		}
	}

	private void tickTurning() {
		++turningTicks;

		final float newYaw = lastYaw + yawTurnPerTick;
		this.setHeadRotation(0, newYaw, 0);
		lastYaw = newYaw;

		// Start walking away
		if (turningTicks >= Stalk.stalkTurningTicks) stalkingState = StalkingState.WALKING;
	}

	private void tickWalking() {
		// TODO Set walk animation
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

		// Continue looking away
		final Vec3d awayFromPlayer = Algorithms.getDirectionPosToPos(playerXZ, herobrineXZ);
		final float awayFromPlayerYaw = Algorithms.directionToAngles(awayFromPlayer).getYaw();
		this.setBodyRotation(awayFromPlayerYaw);
		this.setHeadRotation(0, awayFromPlayerYaw, 0);

		// Move away until unseen
		// TODO Jump up blocks
		this.move(awayFromPlayer.multiply(Stalk.stalkMovementSpeed));
		if (!this.isSeenByPlayers(Stalk.stalkLookAtThresholdVanish)) {
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
