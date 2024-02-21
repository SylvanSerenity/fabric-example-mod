package com.sylvan.presence.event;

import java.util.concurrent.TimeUnit;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;

import net.minecraft.entity.player.PlayerEntity;

public class Attack {
	public static boolean attackEnabled = true;			// Whether the attack event is active
	private static float attackHauntLevelMin = 1.75f;		// The minimum haunt level to play event
	private static int attackDelayMin = 60 * 60;			// The minimum delay between attack events
	private static int attackDelayMax = 60 * 60 * 3;		// The maximum delay between attack events
	private static int attackRetryDelay = 60;			// The delay between retrying an attack if the previous attempt failed
	public static float attackDamageMin = 1.0f;			// The minimum damage to apply to the player
	public static float attackDamageMax = 4.0f;			// The maximum damage to apply to the player
	private static boolean attackHealthMinConstraint = true;	// Whether the player must be above a minimum health before attacking
	private static float attackHealthMin = 10.0f;			// The minimum health a player can before attacking

	public static void loadConfig() {
		try {
			attackEnabled = Presence.config.getOrSetValue("attackEnabled", attackEnabled).getAsBoolean();
			attackHauntLevelMin = Presence.config.getOrSetValue("attackHauntLevelMin", attackHauntLevelMin).getAsFloat();
			attackDelayMin = Presence.config.getOrSetValue("attackDelayMin", attackDelayMin).getAsInt();
			attackDelayMax = Presence.config.getOrSetValue("attackDelayMax", attackDelayMax).getAsInt();
			attackRetryDelay = Presence.config.getOrSetValue("attackRetryDelay", attackRetryDelay).getAsInt();
			attackDamageMin = Presence.config.getOrSetValue("attackDamageMin", attackDamageMin).getAsFloat();
			attackDamageMax = Presence.config.getOrSetValue("attackDamageMax", attackDamageMax).getAsFloat();
			attackHealthMinConstraint = Presence.config.getOrSetValue("attackHealthMinConstraint", attackHealthMinConstraint).getAsBoolean();
			attackHealthMin = Presence.config.getOrSetValue("attackHealthMin", attackHealthMin).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Attack.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(Attack.attackDelayMin, hauntLevel),
				Algorithms.divideByFloat(Attack.attackDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				if (attack(player, Algorithms.randomBetween(attackDamageMin, attackDamageMax), false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(attackDelayMin, hauntLevel),
							Algorithms.divideByFloat(attackDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, attackRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean attack(final PlayerEntity player, final float damage, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return false;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < attackHauntLevelMin) return true; // Reset event as if it passed
		}

		if (attackHealthMinConstraint && player.getHealth() < attackHealthMin) return false;

		player.damage(player.getWorld().getDamageSources().playerAttack(null), damage);

		return true;
	}
}
