package com.cluck.client;

import com.cluck.entity.BarrelEntity;
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

public final class BarrelEntityRenderer extends EntityRenderer<BarrelEntity, BarrelEntityRenderer.State> {
	private static final ChickenJemModel BARREL_MODEL =
			ChickenJemModel.load("/assets/battle-chickens/optifine/cem/barrel.jem");

	private final ModelPart model;
	private final Identifier texture;

	public BarrelEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = BARREL_MODEL.createStandaloneModel();
		texture = BARREL_MODEL.texture();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BarrelEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot();
		state.xRot = entity.getXRot();
		float age = entity.tickCount + partialTick;
		state.spinX = age * entity.spinRate();
		state.spinY = age * entity.ySpinRate();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.rotate(Axis.XP.rotationDegrees(state.xRot));
		poseStack.rotate(Axis.XP.rotationDegrees(state.spinX));
		poseStack.rotate(Axis.YP.rotationDegrees(state.spinY));
		collector.submitModelPart(model, poseStack, RenderTypes.entityCutout(texture),
				state.lightCoords, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, cameraState);
	}

	static final class State extends EntityRenderState {
		private float yRot;
		private float xRot;
		private float spinX;
		private float spinY;
	}
}
