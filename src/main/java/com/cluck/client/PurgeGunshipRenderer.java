package com.cluck.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;

public final class PurgeGunshipRenderer extends PhantomRenderer {
	private static final ChickenJemModel JEM =
			ChickenJemModel.load("/assets/battle-chickens/optifine/cem/purge_gunship.jem");
	private static final Identifier TEXTURE = JEM.texture();

	public PurgeGunshipRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = JEM.createPhantomModel();
	}

	@Override
	public Identifier getTextureLocation(PhantomRenderState state) {
		return TEXTURE;
	}
}
