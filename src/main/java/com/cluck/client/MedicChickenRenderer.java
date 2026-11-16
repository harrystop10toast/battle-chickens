package com.cluck.client;

import com.cluck.BattleChickens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.chicken.Chicken;

public final class MedicChickenRenderer extends MobRenderer<Chicken, ChickenRenderState, ChickenModel> {
	static final ModelLayerLocation MODEL_LAYER =
			new ModelLayerLocation(BattleChickens.id("medic_chicken"), "main");

	private static final Identifier TEXTURE =
			BattleChickens.id("textures/entity/chicken/medic.png");

	public MedicChickenRenderer(EntityRendererProvider.Context context) {
		super(context, new AdultChickenModel(context.bakeLayer(MODEL_LAYER)), 0.3F);
		if (!FabricLoader.getInstance().isModLoaded("entity_model_features")) {
			addLayer(new MedicChickenAccessoriesLayer(this));
		}
	}

	static LayerDefinition createLayer() {
		return MedicChickenModel.createLayer();
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

	private static final class MedicChickenModel extends AdultChickenModel {
		private MedicChickenModel(ModelPart root) {
			super(root);
		}

		private static LayerDefinition createLayer() {
			return LayerDefinition.create(createBaseChickenModel(), 128, 128);
		}
	}
}
