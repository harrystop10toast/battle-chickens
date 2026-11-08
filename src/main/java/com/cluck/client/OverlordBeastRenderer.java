package com.cluck.client;

import com.cluck.BattleChickens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RavagerRenderer;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.resources.Identifier;

public final class OverlordBeastRenderer extends RavagerRenderer {
	private static final Identifier TEXTURE = BattleChickens.id("textures/entity/overlord.png");

	public OverlordBeastRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(RavagerRenderState state) {
		return TEXTURE;
	}
}
