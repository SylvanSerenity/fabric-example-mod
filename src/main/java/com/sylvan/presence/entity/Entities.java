package com.sylvan.presence.entity;

import com.sylvan.presence.Presence;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Entities {
	public static EntityType<HerobrineEntity> herobrineEntity;

	public static void registerEntityTypes() {
		herobrineEntity = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(Presence.MOD_ID, "herobrine"),
			FabricEntityTypeBuilder
				.create(SpawnGroup.MISC, HerobrineEntity::new)
				.dimensions(EntityDimensions.fixed(1.0f, 2.0f))
				.disableSaving()
				.fireImmune()
			.build()
		);
	}

	public static void registerEntityAttributes() {
		FabricDefaultAttributeRegistry.register(herobrineEntity, HerobrineEntity.createMobAttributes());
	}

	public static void registerEntities() {
		registerEntityTypes();
		registerEntityAttributes();
	}
}
