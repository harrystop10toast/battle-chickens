package com.cluck.client;

import com.cluck.BattleChickens;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class MedicChickenAccessoriesLayer extends RenderLayer<ChickenRenderState, ChickenModel> {
	private static final Identifier MEDIC_TEXTURE =
			BattleChickens.id("textures/entity/chicken/medic.png");

	private final ModelPart nurseHat;
	private final ModelPart babyNurseHat;
	private final ModelPart medicCross;
	private final ModelPart syringe;

	MedicChickenAccessoriesLayer(RenderLayerParent<ChickenRenderState, ChickenModel> parent) {
		super(parent);
		ModelPart hatRoot = createNurseHatLayer().bakeRoot();
		nurseHat = hatRoot.getChild("nurse_hat");
		babyNurseHat = hatRoot.getChild("baby_nurse_hat");
		medicCross = createMedicCrossLayer().bakeRoot().getChild("medic_cross");
		syringe = createSyringeLayer().bakeRoot().getChild("held_syringe");
		syringe.xScale = 0.09375F;
		syringe.yScale = 0.09375F;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
			ChickenRenderState state, float limbSwing, float limbSwingAmount) {
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

		ModelPart root = getParentModel().root();
		if (root.hasChild("head")) {
			poseStack.pushPose();
			root.getChild("head").translateAndRotate(poseStack);
			collector.submitModelPart(nurseHat, poseStack, RenderTypes.entityCutout(MEDIC_TEXTURE),
					packedLight, overlay, null);
			collector.submitModelPart(medicCross, poseStack, RenderTypes.entityCutout(MEDIC_TEXTURE),
					packedLight, overlay, null);
			poseStack.popPose();
		} else if (root.hasChild("body")) {
			poseStack.pushPose();
			root.getChild("body").translateAndRotate(poseStack);
			collector.submitModelPart(babyNurseHat, poseStack, RenderTypes.entityCutout(MEDIC_TEXTURE),
					packedLight, overlay, null);
			poseStack.popPose();
		}

		if (root.hasChild("right_wing")) {
			poseStack.pushPose();
			root.getChild("right_wing").translateAndRotate(poseStack);
			collector.submitModelPart(syringe, poseStack, RenderTypes.entityCutout(MEDIC_TEXTURE),
					packedLight, overlay, null);
			poseStack.popPose();
		}
	}

	private static LayerDefinition createNurseHatLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("nurse_hat", CubeListBuilder.create()
				.texOffs(66, 66).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 1.0F, 5.0F)
				.texOffs(66, 74).addBox(-2.0F, -10.0F, -2.0F, 4.0F, 3.0F, 3.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("baby_nurse_hat", CubeListBuilder.create()
				.texOffs(66, 66).addBox(-3.0F, -3.0F, -2.5F, 6.0F, 1.0F, 5.0F)
				.texOffs(66, 74).addBox(-2.0F, -6.0F, -1.5F, 4.0F, 3.0F, 3.0F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static LayerDefinition createMedicCrossLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("medic_cross", CubeListBuilder.create()
				.texOffs(96, 66).addBox(-1.0F, -10.0F, -2.05F, 2.0F, 3.0F, 0.05F)
				.texOffs(96, 66).addBox(-2.0F, -9.0F, -2.05F, 1.0F, 1.0F, 0.05F)
				.texOffs(96, 66).addBox(1.0F, -9.0F, -2.05F, 1.0F, 1.0F, 0.05F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static LayerDefinition createSyringeLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("held_syringe", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-32.0F, -32.0F, 0.0F, 64.0F, 64.0F, 0.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, -3.0F,
						-1.57055F, -0.07274F, 0.04140F));
		return LayerDefinition.create(mesh, 128, 128);
	}
}
