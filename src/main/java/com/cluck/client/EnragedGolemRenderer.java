package com.cluck.client;

import com.cluck.BattleChickens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;

public final class EnragedGolemRenderer extends IronGolemRenderer {
	private static final Identifier TEXTURE = BattleChickens.id("textures/entity/enraged_golem.png");

	public EnragedGolemRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(IronGolemRenderState state) {
		return TEXTURE;
	}
}
