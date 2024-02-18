package com.sylvan.presence.renderer;

import com.sylvan.presence.Presence;
import com.sylvan.presence.entity.HerobrineEntity;

import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class HerobrineEntityRenderer extends MobEntityRenderer<HerobrineEntity, PlayerEntityModel<HerobrineEntity>> {

	public HerobrineEntityRenderer(Context context) {
		super(context, new PlayerEntityModel<HerobrineEntity>(context.getPart(EntityModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public Identifier getTexture(HerobrineEntity entity) {
		return new Identifier(Presence.MOD_ID, "textures/entity/herobrine/smile.png");
	}
	
}
