package com.cluck.client;

import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.chicken.Chicken;

public final class RavagerAssaultChickenRenderer
		extends MobRenderer<Chicken, ChickenRenderState, ChickenModel> {
	private static final ChickenJemModel JEM =
			ChickenJemModel.load("/assets/battle-chickens/optifine/cem/assault.jem");
	private static final Identifier TEXTURE = JEM.texture();

	public RavagerAssaultChickenRenderer(EntityRendererProvider.Context context) {
		super(context, new AdultChickenModel(JEM.createModel()), 0.3F);
	}

	@Override
	public ChickenRenderState createRenderState() {
		return new ChickenRenderState();
	}

	@Override
	public void extractRenderState(Chicken chicken, ChickenRenderState state, float partialTick) {
		super.extractRenderState(chicken, state, partialTick);
		state.flap = Mth.lerp(partialTick, chicken.oFlap, chicken.flap);
		state.flapSpeed = Mth.lerp(partialTick, chicken.oFlapSpeed, chicken.flapSpeed);
		state.variant = chicken.getVariant().value();
	}

	@Override
	public Identifier getTextureLocation(ChickenRenderState state) {
		return TEXTURE;
	}
}
