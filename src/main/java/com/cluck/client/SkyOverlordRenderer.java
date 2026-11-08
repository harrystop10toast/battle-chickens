package com.cluck.client;

import com.cluck.BattleChickens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;

public final class SkyOverlordRenderer extends PhantomRenderer {
	private static final Identifier TEXTURE = BattleChickens.id("textures/entity/sky_overlord.png");

	public SkyOverlordRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(PhantomRenderState state) {
		return TEXTURE;
	}
}
