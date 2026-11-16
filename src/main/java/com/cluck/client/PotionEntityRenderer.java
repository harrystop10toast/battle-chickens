package com.cluck.client;

import com.cluck.entity.PotionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class PotionEntityRenderer extends EntityRenderer<PotionEntity, PotionEntityRenderer.State> {
	private static final ChickenJemModel JEM =
			ChickenJemModel.load("/assets/battle-chickens/optifine/cem/potion.jem");
	private static final Identifier TEXTURE = JEM.texture();

	private final ModelPart model;

	public PotionEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = JEM.createStandaloneModel();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PotionEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot();
		state.xRot = entity.getXRot();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.rotate(Axis.XP.rotationDegrees(state.xRot));
		poseStack.scale(0.5F, 0.5F, 0.5F);
		collector.submitModelPart(model, poseStack, RenderTypes.entityCutout(TEXTURE),
				state.lightCoords, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, cameraState);
	}

	static final class State extends EntityRenderState {
		private float yRot;
		private float xRot;
	}
}
