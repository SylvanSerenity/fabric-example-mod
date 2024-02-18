package com.sylvan.presence;

import com.sylvan.presence.entity.Entities;
import com.sylvan.presence.renderer.HerobrineEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class PresenceClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		EntityRendererRegistry.register(Entities.herobrineEntity, (context) -> {
			return new HerobrineEntityRenderer(context);
		});
	}
}
