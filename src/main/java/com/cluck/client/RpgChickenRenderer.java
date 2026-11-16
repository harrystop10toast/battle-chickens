package com.cluck.client;

import com.cluck.BattleChickens;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class RpgChickenRenderer extends BomberChickenRenderer {
	private static final Identifier TEXTURE =
			BattleChickens.id("textures/entity/chicken/rpg.png");

	public RpgChickenRenderer(EntityRendererProvider.Context context) {
		super(context, TEXTURE);
	}
}
