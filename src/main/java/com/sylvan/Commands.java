package com.sylvan;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sylvan.events.Footsteps;

public class Commands {
	public static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			literal("presence")
			.requires(source -> source.hasPermissionLevel(2))
			.then(
				literal("event")
				.then(
					literal("footsteps")
					.executes(context -> {
						context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event."), false);
						Footsteps.generateFootsteps(context.getSource().getPlayer(), 3);
						return 1;
					})
					.then(
						argument("footstepCount", IntegerArgumentType.integer())
						.executes(context -> {
							final int footstepCount = IntegerArgumentType.getInteger(context, "footstepCount");
							context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event."), false);
							Footsteps.generateFootsteps(context.getSource().getPlayer(), footstepCount);
							return 1;
						})
					)
				)
			)
		));
	}
}
