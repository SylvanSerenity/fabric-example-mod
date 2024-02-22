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
		WAITING
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
			case WAITING: {
				tickWaiting();
			} break;
		}
	}

	private void tickWatching() {
		// Turn away if seen for too long
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
				final float yawGoal = (float) Algorithms.directionToAngles(awayFromPlayer).getYaw();
				lastYaw = yawGoal + 180; // Should be looking away, so reverse the yaw
				yawTurnPerTick = (yawGoal - lastYaw) / Math.max(1, Stalk.stalkTurningTicks);
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

		// Start waiting for player to lose sight before vanishing
		if (turningTicks >= Stalk.stalkTurningTicks) stalkingState = StalkingState.WAITING;
	}

	private void tickWaiting() {
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
		this.setHeadRotation(0, awayFromPlayerYaw, 0);

		// Vanish when unseen
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
