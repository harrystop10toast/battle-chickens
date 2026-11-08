package com.cluck.client;

import com.cluck.BattleChickens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public final class VeteranRoosterRenderer extends VillagerRenderer {
	private static final Identifier TEXTURE = BattleChickens.id("textures/entity/veteran_rooster.png");

	public VeteranRoosterRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return TEXTURE;
	}
}
