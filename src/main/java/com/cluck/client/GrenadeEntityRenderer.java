package com.cluck.client;

import com.cluck.BattleChickens;
import com.cluck.entity.GrenadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class GrenadeEntityRenderer extends EntityRenderer<GrenadeEntity, GrenadeEntityRenderer.State> {
	static final ModelLayerLocation MODEL_LAYER =
			new ModelLayerLocation(BattleChickens.id("grenade"), "main");

	private static final Identifier TEXTURE = BattleChickens.id("textures/entity/grenade.png");
	private static final float PIVOT_X = 4.5F;
	private static final float PIVOT_Y = 7.4F;
	private static final float PIVOT_Z = 0.1F;

	private final ModelPart model;

	public GrenadeEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = context.bakeLayer(MODEL_LAYER);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GrenadeEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot();
		state.xRot = entity.getXRot();
		state.spin = (entity.tickCount + partialTick) * entity.spinRate();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.yRot));
		poseStack.rotate(Axis.XP.rotationDegrees(state.xRot));
		poseStack.rotate(Axis.YP.rotationDegrees(state.spin));
		collector.submitModelPart(model, poseStack, RenderTypes.entityCutout(TEXTURE),
				state.lightCoords, OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, cameraState);
	}

	static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		CubeListBuilder body = CubeListBuilder.create();
		addAbsoluteBox(body, 64, 32, 4.0F, 9.5F, -1.12F, 1.0F, 3.0F, 0.6F);
		addAbsoluteBox(body, 80, 32, 4.0F, 3.0F, -0.5F, 1.0F, 6.5F, 1.0F);
		addAbsoluteBox(body, 96, 32, 4.0F, 9.5F, -1.12F, 1.0F, 0.5F, 0.6F);
		addAbsoluteBox(body, 96, 32, 4.0F, 12.0F, -1.12F, 1.0F, 0.5F, 0.6F);
		addAbsoluteBox(body, 96, 32, 3.9F, 9.2F, -0.6F, 1.2F, 0.5F, 1.2F);
		addAbsoluteBox(body, 96, 32, 4.1F, 11.85F, -0.4F, 0.8F, 1.0F, 0.8F);
		addAbsoluteBox(body, 96, 32, 4.3F, 12.2F, -0.2F, 0.4F, 1.0F, 0.4F);
		addAbsoluteBox(body, 96, 32, 3.9F, 2.7F, -0.6F, 1.2F, 0.5F, 1.2F);
		addAbsoluteBox(body, 96, 32, 4.25F, 2.65F, -0.25F, 0.5F, 2.0F, 0.5F);
		root.addOrReplaceChild("grenade_body", body, PartPose.ZERO);

		addRotatedBox(root, "warhead_facet_2", 5.3F, 11.0F, 0.0F, -90.0F,
				64, 32, 1.0F, 3.0F, 0.6F);
		addRotatedBox(root, "warhead_facet_3", 5.08F, 11.0F, 0.58F, -135.0F,
				64, 32, 1.0F, 3.0F, 0.6F);
		addRotatedBox(root, "warhead_facet_4", 4.5F, 11.0F, 0.82F, 180.0F,
				64, 32, 1.0F, 3.0F, 0.6F);
		addRotatedBox(root, "warhead_facet_5", 3.92F, 11.0F, 0.58F, 135.0F,
				64, 32, 1.0F, 3.0F, 0.6F);
		addRotatedBox(root, "warhead_facet_6", 3.7F, 11.0F, 0.0F, 90.0F,
				64, 32, 1.0F, 3.0F, 0.6F);
		addRotatedBox(root, "warhead_facet_7", 3.92F, 11.0F, -0.58F, 45.0F,
				64, 32, 1.0F, 3.0F, 0.6F);
		addRotatedBox(root, "warhead_facet_9", 5.08F, 11.0F, -0.58F, -45.0F,
				64, 32, 1.0F, 3.0F, 0.6F);

		addBand(root, "lower_band", 9.75F);
		addBand(root, "upper_band", 12.25F);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void addBand(PartDefinition root, String name, float y) {
		addRotatedBox(root, name + "_2", 5.3F, y, 0.0F, -90.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
		addRotatedBox(root, name + "_3", 5.08F, y, 0.58F, -135.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
		addRotatedBox(root, name + "_4", 4.5F, y, 0.82F, 180.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
		addRotatedBox(root, name + "_5", 3.92F, y, 0.58F, 135.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
		addRotatedBox(root, name + "_6", 3.7F, y, 0.0F, 90.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
		addRotatedBox(root, name + "_7", 3.92F, y, -0.58F, 45.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
		addRotatedBox(root, name + "_9", 5.08F, y, -0.58F, -45.0F,
				96, 32, 1.0F, 0.5F, 0.6F);
	}

	private static void addAbsoluteBox(CubeListBuilder builder, int u, int v,
			float x, float y, float z, float width, float height, float depth) {
		builder.texOffs(u, v).addBox(
				PIVOT_X - x - width,
				PIVOT_Y - y - height,
				z - PIVOT_Z,
				width, height, depth);
	}

	private static void addRotatedBox(PartDefinition root, String name,
			float x, float y, float z, float rotationY, int u, int v,
			float width, float height, float depth) {
		root.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(u, v)
						.addBox(-width / 2.0F, -height / 2.0F, -depth / 2.0F, width, height, depth),
				PartPose.offsetAndRotation(PIVOT_X - x, PIVOT_Y - y, z - PIVOT_Z,
						0.0F, (float) Math.toRadians(-rotationY), 0.0F));
	}

	static final class State extends EntityRenderState {
		private float yRot;
		private float xRot;
		private float spin;
	}
}
