package com.cluck.client;

import com.cluck.entity.OverlordBeast;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class OverlordBeastRenderer
		extends MobRenderer<OverlordBeast, ChickenRenderState, ChickenModel> {
	private static final ChickenJemModel JEM =
			ChickenJemModel.load("/assets/battle-chickens/optifine/cem/overlord.jem");
	private static final Identifier TEXTURE = JEM.texture();

	public OverlordBeastRenderer(EntityRendererProvider.Context context) {
		super(context, new AdultChickenModel(JEM.createModel()), 1.1F);
	}

	@Override
	public ChickenRenderState createRenderState() {
		return new ChickenRenderState();
	}

	@Override
	public void extractRenderState(OverlordBeast beast, ChickenRenderState state, float partialTick) {
		super.extractRenderState(beast, state, partialTick);
		state.flap = Mth.sin((beast.tickCount + partialTick) * 0.3F) * 0.4F;
		state.flapSpeed = 1.0F;
	}

	@Override
	protected void scale(ChickenRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(3.0F, 3.0F, 3.0F);
	}

	@Override
	public Identifier getTextureLocation(ChickenRenderState state) {
		return TEXTURE;
	}
}
