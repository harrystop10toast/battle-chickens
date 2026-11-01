package com.cluck.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.cluck.entity.ModEntities;
import net.minecraft.client.renderer.entity.IronGolemRenderer;

public final class BattleChickensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (var type : ModEntities.chickenTypes()) {
			EntityRendererRegistry.register(type, ChickenRoleRenderer::new);
		}
		EntityRendererRegistry.register(ModEntities.SHIELD_GENERATOR_GOLEM, IronGolemRenderer::new);
	}
}
