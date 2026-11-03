package com.cluck.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;

public final class ShieldGeneratorRenderer extends IronGolemRenderer {
	public ShieldGeneratorRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected int getModelTint(IronGolemRenderState state) {
		return 0xFF687480;
	}
}
