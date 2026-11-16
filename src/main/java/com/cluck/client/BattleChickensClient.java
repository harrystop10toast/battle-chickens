package com.cluck.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import com.cluck.entity.ModEntities;

public final class BattleChickensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MedicChickenRenderer.MODEL_LAYER,
				MedicChickenRenderer::createLayer);
		ModelLayerRegistry.registerModelLayer(GrenadierChickenRenderer.MODEL_LAYER,
				GrenadierChickenRenderer::createLayer);
		ModelLayerRegistry.registerModelLayer(GrenadeEntityRenderer.MODEL_LAYER,
				GrenadeEntityRenderer::createLayer);
		for (var type : ModEntities.chickenTypes()) {
			if (type == ModEntities.WARRIOR_CHICKEN) {
				EntityRendererRegistry.register(type, WarriorChickenRenderer::new);
			} else if (type == ModEntities.SERGEANT_CHICKEN) {
				EntityRendererRegistry.register(type, SergeantChickenRenderer::new);
			} else if (type == ModEntities.MEDIC_CHICKEN) {
				EntityRendererRegistry.register(type, MedicChickenRenderer::new);
			} else if (type == ModEntities.MILITIA_CHICKEN) {
				EntityRendererRegistry.register(type, MilitiaChickenRenderer::new);
			} else if (type == ModEntities.RAVAGER_ASSAULT_CHICKEN) {
				EntityRendererRegistry.register(type, RavagerAssaultChickenRenderer::new);
			} else if (type == ModEntities.GRENADIER_CHICKEN) {
				EntityRendererRegistry.register(type, GrenadierChickenRenderer::new);
			} else if (type == ModEntities.RPG_CHICKEN) {
				EntityRendererRegistry.register(type, RpgChickenRenderer::new);
			} else if (type == ModEntities.BOMBER_GENERAL) {
				EntityRendererRegistry.register(type, BomberChickenRenderer::new);
			} else if (type == ModEntities.BUFFER_GENERAL) {
				EntityRendererRegistry.register(type, BufferGeneralChickenRenderer::new);
			} else if (type == ModEntities.SUMMONED_CHICKEN) {
				EntityRendererRegistry.register(type, SummonedChickenRenderer::new);
			} else if (type == ModEntities.SUMMONER_GENERAL) {
				EntityRendererRegistry.register(type, SummonerGeneralRenderer::new);
			} else if (type == ModEntities.TANK_GENERAL) {
				EntityRendererRegistry.register(type, TankGeneralRenderer::new);
			} else if (type == ModEntities.GIGA_CHICKEN) {
				EntityRendererRegistry.register(type, GigaChickenRenderer::new);
			} else if (type == ModEntities.GUNSHIP_GUNNER) {
				EntityRendererRegistry.register(type, GunnerChickenRenderer::new);
			} else if (type == ModEntities.RIOT_CHICKEN) {
				EntityRendererRegistry.register(type, RiotChickenRenderer::new);
			} else if (type == ModEntities.OVERLORD_CHICKEN) {
				EntityRendererRegistry.register(type, OverlordChickenRenderer::new);
			} else if (type == ModEntities.OVERLORD_BOMBARDIER) {
				EntityRendererRegistry.register(type, OverlordBombardierRenderer::new);
			} else {
				EntityRendererRegistry.register(type, ChickenRoleRenderer::new);
			}
		}
		EntityRendererRegistry.register(ModEntities.SHIELD_GENERATOR_GOLEM, ShieldGeneratorRenderer::new);
		EntityRendererRegistry.register(ModEntities.ENRAGED_GOLEM, EnragedGolemRenderer::new);
		EntityRendererRegistry.register(ModEntities.ARMORED_TANK, ArmoredTankRenderer::new);
		EntityRendererRegistry.register(ModEntities.GRENADE, GrenadeEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.BARREL, BarrelEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.POTION, PotionEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.OVERLORD_BEAST, OverlordBeastRenderer::new);
		EntityRendererRegistry.register(ModEntities.PURGE_DRONE, PurgeDroneRenderer::new);
		EntityRendererRegistry.register(ModEntities.PURGE_GUNSHIP, PurgeGunshipRenderer::new);
		EntityRendererRegistry.register(ModEntities.SKY_OVERLORD, SkyOverlordRenderer::new);
		EntityRendererRegistry.register(ModEntities.VETERAN_ROOSTER, VeteranRoosterRenderer::new);
		ItemTooltipHandler.register();
		NemesisTrophyTooltip.register();
	}
}
