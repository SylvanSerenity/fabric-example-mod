package com.sylvan.presence.entity;

import com.sylvan.presence.event.Stalk;

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
	private float turningYawGoal;
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
		// Look at player
		this.lookAt(trackedPlayer);

		// Turn and walk away if seen for too long
		if (this.isSeenByPlayers(Stalk.stalkLookAtThreshold)) {
			++ticksSeen;
			if (ticksSeen > Stalk.stalkSeenTicksMax) {
				stalkingState = StalkingState.TURNING;
				turningYawGoal = trackedPlayer.getYaw();
			}
		}
	}

	private void tickTurning() {
		++turningTicks;

		// TODO Turn stalkTurningTicks times towards turningDirectionGoal
		this.setBodyRotation(turningYawGoal);
		this.setHeadRotation(0, turningYawGoal, 0);

		// Start walking away
		if (turningTicks >= Stalk.stalkTurningTicks) stalkingState = StalkingState.WALKING;
	}

	private void tickWalking() {
		// TODO Walk away from player until unseen
		this.move(this.getRotationVector().multiply(Stalk.stalkMovementSpeed));
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
