package com.sylvan;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.random.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sylvan.events.Footsteps;

public class Presence implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("presence");
	public static final Random RANDOM = Random.create();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// TODO Register on-player-join events

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (!world.isClient()) Footsteps.generateFootsteps(player, RANDOM.nextBetween(Footsteps.MIN_STEPS, Footsteps.MAX_STEPS));
			return ActionResult.PASS;
		});

		LOGGER.info("Presence loaded.");
	}
}