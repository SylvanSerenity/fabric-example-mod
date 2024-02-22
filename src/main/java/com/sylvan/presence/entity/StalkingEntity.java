package com.sylvan.presence.entity;

import com.sylvan.presence.event.Stalk;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class StalkingEntity extends HerobrineEntity {
	private enum StalkingState {
		WATCHING,
		WAITING
	};

	private PlayerEntity trackedPlayer;
	private StalkingState stalkingState = StalkingState.WATCHING;
	private boolean shouldRemove = false;
	private long ticksSeen = 0;

	public StalkingEntity(World world, String skin, final PlayerEntity trackedPlayer) {
		super(world, skin);
		this.trackedPlayer = trackedPlayer;
	}

	public void tick() {
		switch (stalkingState) {
			case WATCHING: {
				tickWatching();
			} break;
			case WAITING: {
				tickWaiting();
			} break;
		}
	}

	private void tickWatching() {
		if (this.isSeenByPlayers(Stalk.stalkLookAtThreshold)) {
			++ticksSeen;
			if (ticksSeen > Stalk.stalkSeenTicksMax) {
				stalkingState = StalkingState.WAITING;
			}
		} else {
			// Look at player
			this.lookAt(trackedPlayer);
		}
	}

	private void tickWaiting() {
		// Continue looking away
		this.lookAt(trackedPlayer);

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
