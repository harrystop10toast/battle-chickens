package com.cluck.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.cluck.entity.ModEntities;

public final class BattleChickensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (var type : ModEntities.chickenTypes()) {
			EntityRendererRegistry.register(type, ChickenRoleRenderer::new);
		}
		EntityRendererRegistry.register(ModEntities.SHIELD_GENERATOR_GOLEM, ShieldGeneratorRenderer::new);
		EntityRendererRegistry.register(ModEntities.ENRAGED_GOLEM, EnragedGolemRenderer::new);
		EntityRendererRegistry.register(ModEntities.ARMORED_TANK, ArmoredTankRenderer::new);
		EntityRendererRegistry.register(ModEntities.OVERLORD_BEAST, OverlordBeastRenderer::new);
		EntityRendererRegistry.register(ModEntities.PURGE_DRONE, PurgeDroneRenderer::new);
		EntityRendererRegistry.register(ModEntities.PURGE_GUNSHIP, PurgeGunshipRenderer::new);
		EntityRendererRegistry.register(ModEntities.SKY_OVERLORD, SkyOverlordRenderer::new);
		EntityRendererRegistry.register(ModEntities.VETERAN_ROOSTER, VeteranRoosterRenderer::new);
		ItemTooltipHandler.register();
		NemesisTrophyTooltip.register();
	}
}
