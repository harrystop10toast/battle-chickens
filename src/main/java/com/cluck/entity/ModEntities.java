package com.cluck.entity;

import com.cluck.BattleChickens;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;

public final class ModEntities {
	public static final EntityType<WarriorChicken> WARRIOR_CHICKEN = registerWarriorChicken();
	public static final EntityType<ShieldGeneratorGolem> SHIELD_GENERATOR_GOLEM = registerShieldGeneratorGolem();
	public static final EntityType<EnragedGolem> ENRAGED_GOLEM = registerEnragedGolem();
	public static final EntityType<ArmoredTank> ARMORED_TANK = registerArmoredTank();
	public static final EntityType<OverlordBeast> OVERLORD_BEAST = registerOverlordBeast();
	public static final EntityType<PurgeDrone> PURGE_DRONE = registerPurgeDrone();
	public static final EntityType<PurgeGunship> PURGE_GUNSHIP = registerPurgeGunship();
	public static final EntityType<SkyOverlord> SKY_OVERLORD = registerSkyOverlord();
	public static final EntityType<VeteranRooster> VETERAN_ROOSTER = registerVeteranRooster();
	public static final EntityType<PurgeChicken> RPG_CHICKEN = registerRole("rpg_chicken");
	public static final EntityType<PurgeChicken> GRENADIER_CHICKEN = registerRole("grenadier_chicken");
	public static final EntityType<PurgeChicken> SNIPER_CHICKEN = registerRole("sniper_chicken");
	public static final EntityType<PurgeChicken> MEDIC_CHICKEN = registerRole("medic_chicken");
	public static final EntityType<PurgeChicken> RIOT_CHICKEN = registerRole("riot_chicken");
	public static final EntityType<PurgeChicken> SERGEANT_CHICKEN = registerRole("sergeant_chicken");
	public static final EntityType<PurgeChicken> MILITIA_CHICKEN = registerRole("militia_chicken");
	public static final EntityType<PurgeChicken> SCAVENGER_CHICKEN = registerRole("scavenger_chicken");
	public static final EntityType<PurgeChicken> GUNSHIP_GUNNER = registerRole("gunship_gunner");
	public static final EntityType<PurgeChicken> OVERLORD_ROOSTER = registerRole("overlord_rooster");
	public static final EntityType<PurgeChicken> OVERLORD_RPG_CHICKEN = registerRole("overlord_rpg_chicken");
	public static final EntityType<PurgeChicken> SUMMONER_GENERAL = registerRole("summoner_general");
	public static final EntityType<PurgeChicken> TANK_GENERAL = registerRole("tank_general");
	public static final EntityType<PurgeChicken> BOMBER_GENERAL = registerRole("bomber_general");
	public static final EntityType<PurgeChicken> BUFFER_GENERAL = registerRole("buffer_general");
	public static final EntityType<PurgeChicken> OVERLORD_BOMBARDIER = registerRole("overlord_bombardier");
	public static final EntityType<PurgeChicken> SUMMONED_CHICKEN = registerRole("summoned_chicken");
	public static final EntityType<PurgeChicken> OUTPOST_GUARD = registerRole("outpost_guard");
	public static final EntityType<PurgeChicken> GIGA_CHICKEN = registerRole("giga_chicken");

	private ModEntities() {
	}

	private static final Map<String, EntityType<? extends Chicken>> ROLE_TYPES = Map.ofEntries(
			Map.entry("warrior", WARRIOR_CHICKEN),
			Map.entry("warrior_chicken", WARRIOR_CHICKEN),
			Map.entry("rpg", RPG_CHICKEN),
			Map.entry("rpg_chicken", RPG_CHICKEN),
			Map.entry("grenadier", GRENADIER_CHICKEN),
			Map.entry("grenadier_chicken", GRENADIER_CHICKEN),
			Map.entry("sniper", SNIPER_CHICKEN),
			Map.entry("sniper_chicken", SNIPER_CHICKEN),
			Map.entry("medic", MEDIC_CHICKEN),
			Map.entry("medic_chicken", MEDIC_CHICKEN),
			Map.entry("riot", RIOT_CHICKEN),
			Map.entry("riot_chicken", RIOT_CHICKEN),
			Map.entry("sergeant", SERGEANT_CHICKEN),
			Map.entry("sergeant_chicken", SERGEANT_CHICKEN),
			Map.entry("rifle", MILITIA_CHICKEN),
			Map.entry("militia", MILITIA_CHICKEN),
			Map.entry("militia_chicken", MILITIA_CHICKEN),
			Map.entry("scavenger", SCAVENGER_CHICKEN),
			Map.entry("scavenger_chicken", SCAVENGER_CHICKEN),
			Map.entry("gunner", GUNSHIP_GUNNER),
			Map.entry("gunship_gunner", GUNSHIP_GUNNER),
			Map.entry("overlord", OVERLORD_ROOSTER),
			Map.entry("overlord_rooster", OVERLORD_ROOSTER),
			Map.entry("overlord_rpg", OVERLORD_RPG_CHICKEN),
			Map.entry("overlord_rpg_chicken", OVERLORD_RPG_CHICKEN),
			Map.entry("summoner", SUMMONER_GENERAL),
			Map.entry("summoner_general", SUMMONER_GENERAL),
			Map.entry("tank", TANK_GENERAL),
			Map.entry("tank_general", TANK_GENERAL),
			Map.entry("bomber", BOMBER_GENERAL),
			Map.entry("bomber_general", BOMBER_GENERAL),
			Map.entry("buffer", BUFFER_GENERAL),
			Map.entry("buffer_general", BUFFER_GENERAL),
			Map.entry("bombardier", OVERLORD_BOMBARDIER),
			Map.entry("overlord_bombardier", OVERLORD_BOMBARDIER),
			Map.entry("summoned", SUMMONED_CHICKEN),
			Map.entry("summoned_chicken", SUMMONED_CHICKEN),
			Map.entry("guard", OUTPOST_GUARD),
			Map.entry("outpost_guard", OUTPOST_GUARD),
			Map.entry("giga", GIGA_CHICKEN),
			Map.entry("giga_chicken", GIGA_CHICKEN));
	private static final List<EntityType<? extends Chicken>> CHICKEN_TYPES = List.of(
			WARRIOR_CHICKEN, RPG_CHICKEN, GRENADIER_CHICKEN, SNIPER_CHICKEN, MEDIC_CHICKEN,
			RIOT_CHICKEN, SERGEANT_CHICKEN, MILITIA_CHICKEN, SCAVENGER_CHICKEN, GUNSHIP_GUNNER,
			OVERLORD_ROOSTER, OVERLORD_RPG_CHICKEN, SUMMONER_GENERAL, TANK_GENERAL, BOMBER_GENERAL,
			BUFFER_GENERAL, OVERLORD_BOMBARDIER, SUMMONED_CHICKEN, OUTPOST_GUARD, GIGA_CHICKEN);

	public static void register() {
		FabricDefaultAttributeRegistry.register(SHIELD_GENERATOR_GOLEM, ShieldGeneratorGolem.createAttributes());
		FabricDefaultAttributeRegistry.register(ENRAGED_GOLEM, IronGolem.createAttributes());
		FabricDefaultAttributeRegistry.register(ARMORED_TANK, Ravager.createAttributes());
		FabricDefaultAttributeRegistry.register(OVERLORD_BEAST, Ravager.createAttributes());
		FabricDefaultAttributeRegistry.register(PURGE_GUNSHIP, Monster.createMonsterAttributes());
		FabricDefaultAttributeRegistry.register(SKY_OVERLORD, Monster.createMonsterAttributes());
		FabricDefaultAttributeRegistry.register(VETERAN_ROOSTER, Villager.createAttributes());
		for (EntityType<? extends Chicken> type : CHICKEN_TYPES) {
			FabricDefaultAttributeRegistry.register(type, type == WARRIOR_CHICKEN
					? WarriorChicken.createAttributes() : PurgeChicken.createAttributes());
		}
	}

	public static List<EntityType<? extends Chicken>> chickenTypes() {
		return CHICKEN_TYPES;
	}

	public static Chicken createChicken(ServerLevel level, EntitySpawnReason reason, String role) {
		EntityType<? extends Chicken> type = ROLE_TYPES.get(role);
		if (type == null) {
			throw new IllegalArgumentException("Unknown Purge chicken role: " + role);
		}
		return type.create(level, reason);
	}

	private static EntityType<PurgeChicken> registerRole(String role) {
		var id = BattleChickens.id(role);
		var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.of(PurgeChicken::new, MobCategory.CREATURE)
						.sized(0.4F, 0.7F)
						.clientTrackingRange(8)
						.build(key));
	}

	private static EntityType<WarriorChicken> registerWarriorChicken() {
		var id = BattleChickens.id("warrior_chicken");
		var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.of(WarriorChicken::new, MobCategory.CREATURE)
						.sized(0.4F, 0.7F)
						.clientTrackingRange(8)
						.build(key));
	}

	private static EntityType<ShieldGeneratorGolem> registerShieldGeneratorGolem() {
		var id = BattleChickens.id("shield_generator_golem");
		var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.of(ShieldGeneratorGolem::new, MobCategory.MISC)
						.sized(1.4F, 2.7F)
						.clientTrackingRange(10)
						.build(key));
	}

	private static EntityType<EnragedGolem> registerEnragedGolem() {
		var id = BattleChickens.id("enraged_golem");
		var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.of(EnragedGolem::new, MobCategory.MISC)
						.sized(1.4F, 2.7F)
						.clientTrackingRange(10)
						.build(key));
	}

	private static EntityType<ArmoredTank> registerArmoredTank() {
		return registerMob("armored_tank", EntityType.Builder.of(ArmoredTank::new, MobCategory.MONSTER)
				.sized(1.95F, 2.2F)
				.passengerAttachments(new Vec3(0.0, 2.2625, -0.0625))
				.clientTrackingRange(10)
				.notInPeaceful());
	}

	private static EntityType<OverlordBeast> registerOverlordBeast() {
		return registerMob("overlord", EntityType.Builder.of(OverlordBeast::new, MobCategory.MONSTER)
				.sized(1.95F, 2.2F)
				.passengerAttachments(new Vec3(0.0, 2.2625, -0.0625))
				.clientTrackingRange(10)
				.notInPeaceful());
	}

	private static EntityType<PurgeDrone> registerPurgeDrone() {
		return registerMob("purge_drone", EntityType.Builder.of(PurgeDrone::new, MobCategory.MISC)
				.fireImmune()
				.sized(0.25F, 0.25F)
				.clientTrackingRange(8)
				);
	}

	private static EntityType<PurgeGunship> registerPurgeGunship() {
		return registerMob("purge_gunship", EntityType.Builder.of(PurgeGunship::new, MobCategory.MONSTER)
				.sized(0.9F, 0.5F)
				.eyeHeight(0.175F)
				.passengerAttachments(0.3375F)
				.ridingOffset(-0.125F)
				.clientTrackingRange(8)
				.notInPeaceful());
	}

	private static EntityType<SkyOverlord> registerSkyOverlord() {
		return registerMob("sky_overlord", EntityType.Builder.of(SkyOverlord::new, MobCategory.MONSTER)
				.sized(0.9F, 0.5F)
				.eyeHeight(0.175F)
				.passengerAttachments(0.3375F)
				.ridingOffset(-0.125F)
				.clientTrackingRange(8)
				.notInPeaceful());
	}

	private static EntityType<VeteranRooster> registerVeteranRooster() {
		return registerMob("veteran_rooster", EntityType.Builder.of(VeteranRooster::new, MobCategory.MISC)
				.sized(0.6F, 1.95F)
				.eyeHeight(1.62F)
				.clientTrackingRange(10));
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registerMob(String name,
			EntityType.Builder<T> builder) {
		var id = BattleChickens.id(name);
		var key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
	}
}
