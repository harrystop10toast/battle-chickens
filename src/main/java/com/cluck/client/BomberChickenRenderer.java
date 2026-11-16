package com.cluck.client;

import com.cluck.BattleChickens;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;

public class BomberChickenRenderer extends ChickenRenderer {
	private static final Identifier TEXTURE =
			BattleChickens.id("textures/entity/chicken/bomber_general.png");

	private final Identifier texture;

	public BomberChickenRenderer(EntityRendererProvider.Context context) {
		this(context, TEXTURE);
	}

	protected BomberChickenRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context);
		this.texture = texture;
	}

	@Override
	public Identifier getTextureLocation(ChickenRenderState state) {
		return texture;
	}
}
