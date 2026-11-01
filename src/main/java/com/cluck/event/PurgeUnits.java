package com.cluck.event;

import com.cluck.BattleChickens;
import com.cluck.entity.ModEntities;
import com.cluck.entity.WarriorChicken;
import com.cluck.item.ModItems;
import com.cluck.projectile.PurgeRocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PurgeUnits {
	private static final Map<UUID, Vec3> GRENADE_TARGETS = new HashMap<>();

	private PurgeUnits() {
	}

	public static void spawnDailySupport(ServerPlayer player, PurgeWorldData data) {
		ServerLevel level = player.level();
		if (data.currentDay() >= 4 && level.getRandom().nextFloat() < 0.2F) {
			spawnAerialUnit(level, player);
		}
		if (data.currentDay() >= 5 && level.getRandom().nextFloat() < 0.12F) {
			spawnMountedUnit(level, player);
		}
		if (data.currentDay() >= 6 && level.getRandom().nextFloat() < 0.15F) {
			spawnDrone(level, player);
		}
	}

	private static void spawnMountedUnit(ServerLevel level, ServerPlayer player) {
		if (level.getEntitiesOfClass(Ravager.class, player.getBoundingBox().inflate(64),
				entity -> hasName(entity, "Armored Ravager Tank")).size()
				+ level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(64),
				entity -> hasName(entity, "Golem Walker")).size() >= 2) {
			return;
		}
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		int distance = 40 + level.getRandom().nextInt(25);
		int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
		int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (level.getRandom().nextBoolean()) {
			Ravager tank = EntityTypes.RAVAGER.create(level, EntitySpawnReason.EVENT);
			if (tank == null) {
				return;
			}
			tank.setPos(x + 0.5, y, z + 0.5);
			tank.setPersistenceRequired();
			tank.setCustomName(net.minecraft.network.chat.Component.literal("Armored Ravager Tank"));
			tank.setCustomNameVisible(true);
			tank.setTarget(player);
			if (tank.getAttribute(Attributes.MAX_HEALTH) != null) {
				tank.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
						100 * BattleChickens.CONFIG.enemyHealthMultiplier());
				tank.setHealth((float) tank.getAttributeValue(Attributes.MAX_HEALTH));
			}
			level.addFreshEntity(tank);
		} else {
			IronGolem walker = ModEntities.SHIELD_GENERATOR_GOLEM.create(level, EntitySpawnReason.EVENT);
			if (walker == null) {
				return;
			}
			walker.setPos(x + 0.5, y, z + 0.5);
			walker.setPersistenceRequired();
			walker.setCustomName(net.minecraft.network.chat.Component.literal("Golem Walker"));
			walker.setCustomNameVisible(true);
			walker.setTarget(player);
			if (walker.getAttribute(Attributes.MAX_HEALTH) != null) {
				walker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
						80 * BattleChickens.CONFIG.enemyHealthMultiplier());
				walker.setHealth((float) walker.getAttributeValue(Attributes.MAX_HEALTH));
			}
			level.addFreshEntity(walker);
		}
	}

	private static void spawnDrone(ServerLevel level, ServerPlayer player) {
		if (level.getEntitiesOfClass(Vex.class, player.getBoundingBox().inflate(64),
				entity -> hasName(entity, "Purge Drone")).size() >= 2) {
			return;
		}
		Vex drone = EntityTypes.VEX.create(level, EntitySpawnReason.EVENT);
		if (drone == null) {
			return;
		}
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		int distance = 40 + level.getRandom().nextInt(25);
		drone.setPos(player.getX() + Math.cos(angle) * distance,
				player.getY() + 14, player.getZ() + Math.sin(angle) * distance);
		drone.setPersistenceRequired();
		drone.setCustomName(net.minecraft.network.chat.Component.literal("Purge Drone"));
		drone.setCustomNameVisible(true);
		drone.setTarget(player);
		if (drone.getAttribute(Attributes.MAX_HEALTH) != null) {
			drone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
					20 * BattleChickens.CONFIG.enemyHealthMultiplier());
			drone.setHealth((float) drone.getAttributeValue(Attributes.MAX_HEALTH));
		}
		level.addFreshEntity(drone);
	}

	private static void spawnAerialUnit(ServerLevel level, ServerPlayer player) {
		if (level.getEntitiesOfClass(Phantom.class, player.getBoundingBox().inflate(64),
				entity -> hasName(entity, "Purged Gunship")).size() >= 2) {
			return;
		}
		Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT);
		if (phantom == null) {
			return;
		}
		int x = player.blockPosition().getX() + level.getRandom().nextInt(25) - 12;
		int z = player.blockPosition().getZ() + level.getRandom().nextInt(25) - 12;
		int y = Math.min(level.getMaxY() - 8, player.blockPosition().getY() + 14);
		phantom.setPos(x + 0.5, y, z + 0.5);
		phantom.setCustomName(net.minecraft.network.chat.Component.literal("Purged Gunship"));
		phantom.setCustomNameVisible(true);
		phantom.setPersistenceRequired();
		phantom.setTarget(player);
		Chicken gunner = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "gunner");
		if (gunner == null) {
			return;
		}
		gunner.setPos(phantom.position());
		gunner.setPersistenceRequired();
		gunner.setCustomName(net.minecraft.network.chat.Component.literal("Gunship Gunner"));
		gunner.setCustomNameVisible(true);
		gunner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BEAK_47));
		gunner.setTarget(player);
		if (!level.addFreshEntity(phantom)) {
			return;
		}
		if (!level.addFreshEntity(gunner)) {
			phantom.discard();
			return;
		}
		gunner.startRiding(phantom, true, true);
	}

	private static String roleForDay(int roll, int day, int eventDays, int threat, boolean dominion) {
		if (day == eventDays) {
			if (roll < 15) {
				return "rpg";
			}
			if (roll < 35) {
				return "grenadier";
			}
			if (roll < 55) {
				return "sniper";
			}
			if (roll < 70) {
				return "medic";
			}
			if (roll < 85) {
				return "rifle";
			}
			return "sergeant";
		}
		if (day == 1 && roll < Math.min(25, BattleChickens.CONFIG.gearChancePerDay() + (dominion ? 5 : 2))) {
			return "rifle";
		}
		if ((day >= 4 || threat >= 4) && roll < (threat >= 4 ? 18 : 10)) {
			return "rpg";
		}
		if ((day >= 4 || threat >= 4) && roll < (threat >= 4 ? 36 : 22)) {
			return "sniper";
		}
		if (day >= 3 && roll < 45) {
			return "grenadier";
		}
		if (day >= 3 && roll < 55) {
			return "medic";
		}
		if (day >= 2 && roll < (dominion ? 50 : 42)) {
			return "sergeant";
		}
		if (day >= 2 && roll < 75) {
			return "rifle";
		}
		return "scavenger";
	}

	public static void tickUnits(ServerLevel level, List<ServerPlayer> players, int threat) {
		if (level.getGameTime() % 5 != 0) {
			return;
		}
		Set<UUID> ticked = new HashSet<>();
		for (ServerPlayer player : players) {
			if (player.level() != level || !player.isAlive()) {
				continue;
			}
			for (Chicken chicken : level.getEntitiesOfClass(Chicken.class, player.getBoundingBox().inflate(128),
					candidate -> isUnit(candidate) && ticked.add(candidate.getUUID()))) {
				ServerPlayer target = players.stream()
						.filter(candidate -> candidate.level() == level && candidate.isAlive()
								&& (!candidate.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)
								|| chicken.distanceToSqr(candidate) <= 3 * 3))
						.min((first, second) -> Double.compare(chicken.distanceToSqr(first), chicken.distanceToSqr(second)))
						.orElse(null);
				if (target != null) {
					tickUnit(chicken, target, level, threat);
				}
			}
		}
	}

	public static void promoteNaturalChicken(Chicken chicken) {
		Chicken warrior = replaceNaturalChicken(chicken, "warrior");
		if (warrior != null) {
			warrior.addTag("battle_chickens_military");
			if (warrior.getRandom().nextBoolean()) {
				warrior.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			}
		}
	}

	private static Chicken replaceNaturalChicken(Chicken chicken, String role) {
		if (!(chicken.level() instanceof ServerLevel level)) {
			return null;
		}
		Chicken replacement = ModEntities.createChicken(level, EntitySpawnReason.EVENT, role);
		if (replacement == null) {
			return null;
		}
		replacement.setPos(chicken.position());
		replacement.setYRot(chicken.getYRot());
		replacement.setXRot(chicken.getXRot());
		replacement.setYHeadRot(chicken.getYHeadRot());
		replacement.setVariant(chicken.getVariant());
		replacement.setAge(chicken.getAge());
		if (!level.addFreshEntity(replacement)) {
			return null;
		}
		chicken.discard();
		return replacement;
	}

	public static void promoteNaturalChicken(Chicken chicken, PurgeWorldData data) {
		if (!(chicken.level() instanceof ServerLevel level) || !data.isActive()) {
			return;
		}
		ServerPlayer nearest = level.getServer().getPlayerList().getPlayers().stream()
				.filter(player -> player.level() == level && player.isAlive())
				.min(java.util.Comparator.comparingDouble(chicken::distanceToSqr))
				.filter(player -> chicken.distanceToSqr(player) <= 128 * 128)
				.orElse(null);
		if (nearest == null) {
			return;
		}
		int nearbyUnits = level.getEntitiesOfClass(Chicken.class, nearest.getBoundingBox().inflate(96),
				PurgeUnits::isUnit).size();
		if (nearbyUnits >= BattleChickens.CONFIG.hostileChickenCap()) {
			return;
		}
		int chance = Math.min(95, BattleChickens.CONFIG.naturalMilitaryChancePercent()
				+ Math.max(0, data.currentDay() - 1) * 8 + Math.max(0, data.threat() - 1) * 4);
		if (level.getRandom().nextInt(100) >= chance) {
			return;
		}

		int roleRoll = level.getRandom().nextInt(100);
		String role = roleForDay(roleRoll, data.currentDay(), data.eventDays(), data.threat(), data.dominion());
		if (role.equals("rifle") && data.currentDay() >= 3 && roleRoll >= 45 && roleRoll < 51) {
			role = "riot";
		}
		if (data.currentDay() >= 5 && level.getRandom().nextFloat() < 0.1F) {
			role = "medic";
		}
		int rpgCap = BattleChickens.CONFIG.rpgCapPerPlayer()
				* (data.currentDay() == data.eventDays() ? 2 : 1);
		if (role.equals("rpg") && level.getEntitiesOfClass(Chicken.class, nearest.getBoundingBox().inflate(96),
				entity -> hasName(entity, "RPG Chicken") || hasName(entity, "Overlord RPG Chicken")).size()
				>= rpgCap) {
			role = data.currentDay() == data.eventDays() || data.currentDay() >= 4 ? "sniper" : "rifle";
		}

		Chicken soldier = replaceNaturalChicken(chicken, role);
		if (soldier == null) {
			return;
		}
		float health = (6 + data.currentDay() * 2 + data.threat() * 3)
				* BattleChickens.CONFIG.enemyHealthMultiplier();
		if (soldier.getAttribute(Attributes.MAX_HEALTH) != null) {
			soldier.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			soldier.setHealth(health);
		}
		equipRole(soldier, role, data, level);
		if (role.equals("rpg") && data.currentDay() == data.eventDays()) {
			soldier.addTag("battle_chickens_infinite_rpg_ammo");
		}
		soldier.setCustomNameVisible(true);
		soldier.setTarget(nearest);
	}

	public static void spawnFinalDayRpgWave(ServerLevel level, List<ServerPlayer> players,
											PurgeWorldData data) {
		if (!data.isActive() || data.currentDay() != data.eventDays()
				|| level.getGameTime() % 200 != 0) {
			return;
		}
		int localCap = Math.min(120, Math.max(BattleChickens.CONFIG.hostileChickenCap(),
				BattleChickens.CONFIG.hostileChickenCap() * 2));
		for (ServerPlayer player : players) {
			if (player.level() != level || !player.isAlive()) {
				continue;
			}
			AABB area = player.getBoundingBox().inflate(64);
			long units = level.getEntitiesOfClass(Chicken.class, area, PurgeUnits::isUnit).size();
			if (units >= localCap) {
				continue;
			}
			for (int index = 0; index < 2 && units < localCap; index++) {
				double angle = level.getRandom().nextDouble() * Math.PI * 2;
				int distance = 28 + level.getRandom().nextInt(13);
				int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
				int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				long nearbyRpgs = level.getEntitiesOfClass(Chicken.class, area,
						entity -> hasName(entity, "RPG Chicken") || hasName(entity, "Overlord RPG Chicken")).size();
				int rpgCap = BattleChickens.CONFIG.rpgCapPerPlayer() * 2;
				int roleRoll = level.getRandom().nextInt(100);
				String role = roleRoll < 20 && nearbyRpgs < rpgCap ? "rpg"
						: roleRoll < 40 ? "grenadier"
						: roleRoll < 58 ? "sniper"
						: roleRoll < 76 ? "medic"
						: roleRoll < 90 ? "rifle" : "sergeant";
				Chicken raider = ModEntities.createChicken(level, EntitySpawnReason.EVENT, role);
				if (raider == null) {
					continue;
				}
				raider.setPos(x + 0.5, y, z + 0.5);
				raider.setPersistenceRequired();
				equipRole(raider, role, data, level);
				raider.setCustomNameVisible(true);
				if (role.equals("rpg")) {
					raider.addTag("battle_chickens_infinite_rpg_ammo");
				}
				raider.setTarget(player);
				if (raider.getAttribute(Attributes.MAX_HEALTH) != null) {
					double health = 20 * BattleChickens.CONFIG.enemyHealthMultiplier();
					raider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
					raider.setHealth((float) health);
				}
				if (level.addFreshEntity(raider)) {
					units++;
				}
			}
		}
	}

	private static void equipRole(Chicken chicken, String role, PurgeWorldData data, ServerLevel level) {
		if (role.equals("rpg")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("RPG Chicken"));
		} else if (role.equals("medic")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPLASH_POTION));
			chicken.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Medic Hen"));
		} else if (role.equals("riot")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
			chicken.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
			chicken.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Riot Chicken"));
		} else if (role.equals("grenadier")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FIRE_CHARGE));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Grenadier Hen"));
			attachGrenadeModel(level, chicken);
		} else if (role.equals("sniper")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Sniper Hen"));
		} else if (role.equals("sergeant")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			chicken.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			chicken.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Sergeant Chicken"));
		} else if (role.equals("rifle")) {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Militia Chicken"));
		} else {
			chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
			float armorChance = Math.min(1.0F, 0.30F
					+ BattleChickens.CONFIG.gearChancePerDay() * (data.currentDay() - 1) / 100.0F
					+ (data.dominion() ? 0.1F : 0.0F));
			if (level.getRandom().nextFloat() < armorChance) {
				chicken.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			}
			chicken.setCustomName(net.minecraft.network.chat.Component.literal("Scavenger Chicken"));
		}
	}

	public static void onWarriorDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity instanceof Chicken chicken) || !isWarrior(chicken)) {
			return;
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile
				&& projectile.getOwner() != null) {
			attacker = projectile.getOwner();
		}
		if (attacker instanceof LivingEntity livingAttacker && livingAttacker != chicken) {
			chicken.addTag("battle_chickens_retaliating");
			chicken.setTarget(livingAttacker);
		}
	}

	public static boolean isWarrior(Entity entity) {
		return entity.entityTags().contains("battle_chickens_military")
				|| hasName(entity, "Warrior Chicken") || isNemesis(entity);
	}

	public static boolean isNemesis(Entity entity) {
		return entity.getCustomName() != null
				&& entity.getCustomName().getString().startsWith("Nemesis Chicken:");
	}

	public static int nemesisRank(Entity entity) {
		return (int) entity.entityTags().stream()
				.filter(tag -> tag.startsWith("battle_chickens_nemesis_rank_"))
				.count();
	}

	public static void promoteToNemesis(Chicken chicken, ServerPlayer victim) {
		int rank = nemesisRank(chicken) + 1;
		chicken.addTag("battle_chickens_nemesis_rank_" + rank);
		chicken.setCustomName(net.minecraft.network.chat.Component.literal(
				"Nemesis Chicken: " + victim.getGameProfile().name() + " (Rank " + rank + ")"));
		chicken.setCustomNameVisible(true);
		chicken.setPersistenceRequired();
		double maxHealth = 24 + rank * 8;
		if (chicken.getAttribute(Attributes.MAX_HEALTH) != null) {
			chicken.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
		}
		chicken.setHealth((float) Math.max(chicken.getHealth(), maxHealth));
		chicken.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		if (rank >= 2) {
			chicken.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		}
		chicken.setTarget(null);
	}

	public static void tickNaturalWarriors(ServerLevel level, List<ServerPlayer> players) {
		if (level.getGameTime() % 5 != 0) {
			return;
		}
		Set<UUID> ticked = new HashSet<>();
		for (ServerPlayer player : players) {
			if (player.level() != level || !player.isAlive()) {
				continue;
			}
			for (Chicken chicken : level.getEntitiesOfClass(Chicken.class, player.getBoundingBox().inflate(96),
					candidate -> isWarrior(candidate) && ticked.add(candidate.getUUID()))) {
				LivingEntity target = chicken.getTarget();
				if ((target == null || !target.isAlive() || target.level() != level)
						&& (isNemesis(chicken)
								|| chicken.entityTags().contains("battle_chickens_retaliating"))) {
					target = players.stream()
							.filter(candidate -> candidate.level() == level && candidate.isAlive()
									&& chicken.distanceToSqr(candidate) <= 96 * 96)
							.min(java.util.Comparator.comparingDouble(chicken::distanceToSqr))
							.orElse(null);
					chicken.setTarget(target);
				}
				if (target == null || !target.isAlive() || target.level() != level
						|| chicken.distanceToSqr(target) > 96 * 96) {
					if (target != null && !isNemesis(chicken)
							&& !chicken.entityTags().contains("battle_chickens_retaliating")) {
						chicken.setTarget(null);
					}
					continue;
				}
				if (chicken.distanceToSqr(target) > 2.5 * 2.5) {
					chicken.getNavigation().moveTo(target, 2.0);
				} else if (chicken.tickCount % 25 == 0) {
					target.hurtServer(level, level.damageSources().mobAttack(chicken),
							4 + nemesisRank(chicken) * 2);
				}
			}
		}
	}

	public static void markSurvivors(ServerLevel level, List<ServerPlayer> players) {
		Set<UUID> marked = new HashSet<>();
		for (ServerPlayer player : players) {
			if (player.level() != level) {
				continue;
			}
			for (Chicken chicken : level.getEntitiesOfClass(Chicken.class,
					player.getBoundingBox().inflate(48), candidate -> !isUnit(candidate))) {
				if (marked.add(chicken.getUUID())) {
					chicken.addTag("battle_chickens_purge_survivor");
				}
			}
		}
	}

	public static void tickGoldenEggLayers(ServerLevel level, List<ServerPlayer> players) {
		if (level.getGameTime() % 12_000 != 0) {
			return;
		}
		Set<UUID> checked = new HashSet<>();
		for (ServerPlayer player : players) {
			if (player.level() != level) {
				continue;
			}
			for (Chicken chicken : level.getEntitiesOfClass(Chicken.class,
					player.getBoundingBox().inflate(64),
					candidate -> candidate.entityTags().contains("battle_chickens_purge_survivor")
							&& checked.add(candidate.getUUID()))) {
				if (level.getRandom().nextFloat() < 0.05F) {
					drop(level, chicken.position(), ModItems.GOLDEN_EGG, 1);
				}
			}
		}
	}

	public static void tickGigaChickens(ServerLevel level, List<ServerPlayer> players) {
		if (level.getGameTime() % 5 != 0) {
			return;
		}
		for (ServerPlayer player : players) {
			if (player.level() != level || !player.isAlive()) {
				continue;
			}
			for (Chicken mount : level.getEntitiesOfClass(Chicken.class, player.getBoundingBox().inflate(32),
					chicken -> hasName(chicken, "Giga Chicken") && chicken.hasPassenger(player))) {
				Vec3 look = player.getLookAngle();
				mount.setYRot(player.getYRot());
				mount.getNavigation().moveTo(player.getX() + look.x * 8, player.getY(),
						player.getZ() + look.z * 8, 1.8);
			}
		}
	}

	private static void tickUnit(Chicken chicken, ServerPlayer player, ServerLevel level, int threat) {
		if (chicken.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) {
			chicken.getNavigation().stop();
			return;
		}
		double distanceSquared = chicken.distanceToSqr(player);
		if (chicken.entityTags().contains("battle_chickens_ravager_assault")) {
			if (distanceSquared > 2.8 * 2.8) {
				chicken.getNavigation().moveTo(player, 2.4);
			} else if (chicken.tickCount % 40 == 0) {
				player.hurtServer(level, level.damageSources().mobAttack(chicken),
						(5 + threat) * BattleChickens.CONFIG.enemyDamageMultiplier());
				Vec3 tackle = player.position().subtract(chicken.position()).normalize();
				BossController.applyDaySevenKnockback(player, tackle.scale(0.35).add(0, 0.18, 0));
				level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.6,
						player.getZ(), 6, 0.25, 0.25, 0.25, 0.06);
			}
			return;
		}
		if (isGunner(chicken)) {
			chicken.getNavigation().stop();
			if (distanceSquared > 3.5 * 3.5 && distanceSquared <= 64 * 64
					&& chicken.getSensing().hasLineOfSight(player) && chicken.tickCount % 12 == 0) {
				player.hurtServer(level, level.damageSources().mobAttack(chicken),
						(3 + threat) * BattleChickens.CONFIG.enemyDamageMultiplier());
				Vec3 from = chicken.getEyePosition();
				Vec3 to = player.getBoundingBox().getCenter();
				Vec3 tracer = from.add(to.subtract(from).scale(0.5));
				level.sendParticles(ParticleTypes.CRIT, tracer.x, tracer.y, tracer.z,
						4, 0.12, 0.12, 0.12, 0.02);
			}
			return;
		}
		boolean overlordSniper = hasName(chicken, "Overlord Sniper");
		if (distanceSquared > 48 * 48 && !overlordSniper) {
			chicken.getNavigation().moveTo(player, 2.0);
			return;
		}
		boolean rpg = hasName(chicken, "RPG Chicken") || hasName(chicken, "Overlord RPG Chicken")
				|| hasName(chicken, "Bomber General");
		boolean regularRpgHasAmmo = !hasName(chicken, "RPG Chicken")
				|| chicken.entityTags().contains("battle_chickens_infinite_rpg_ammo")
				|| !chicken.entityTags().contains("battle_chickens_rpg_round_3");
		if (hasName(chicken, "Buffer General") && chicken.tickCount % 100 == 0) {
			for (Chicken ally : level.getEntitiesOfClass(Chicken.class, chicken.getBoundingBox().inflate(12),
					candidate -> isUnit(candidate) && candidate != chicken)) {
				ally.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.STRENGTH, 120, 0));
			}
		}
		if (hasName(chicken, "Medic Hen") && chicken.tickCount % 60 == 0) {
			for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, chicken.getBoundingBox().inflate(12),
					candidate -> candidate != chicken && isPurgeAlly(candidate)
							&& candidate.getHealth() < candidate.getMaxHealth())) {
				ally.heal(3);
				ally.addEffect(new net.minecraft.world.effect.MobEffectInstance(
						net.minecraft.world.effect.MobEffects.REGENERATION, 80, 0));
			}
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, chicken.getX(), chicken.getY() + 0.6,
					chicken.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
		}
		if (hasName(chicken, "Summoner General") && chicken.tickCount % 300 == 0) {
			long count = level.getEntitiesOfClass(Chicken.class, chicken.getBoundingBox().inflate(48), PurgeUnits::isUnit).size();
			if (count < BattleChickens.CONFIG.hostileChickenCap()) {
				spawnSummonedChicken(level, chicken, player);
			}
		}
		boolean sniper = hasName(chicken, "Sniper Hen") || overlordSniper;
		if ((rpg && regularRpgHasAmmo) || sniper) {
			double range = rpg ? 48 : overlordSniper ? 96 : 40;
			if (distanceSquared > 3.5 * 3.5 && distanceSquared <= range * range
					&& chicken.getSensing().hasLineOfSight(player)) {
				int reload = overlordSniper ? 100 : hasName(chicken, "Bomber General")
						? 100 : threat >= 4 ? 120 : 160;
				int telegraphTicks = overlordSniper ? 35 : threat >= 4 ? 20 : 30;
				if (chicken.tickCount % reload == reload - telegraphTicks) {
					BossController.showCombatAlert(player, net.minecraft.network.chat.Component.literal(overlordSniper
							? "The Overlord's sniper has you in its sights! Break line of sight!"
							: "A chicken sights you! Take cover!"));
					for (int step = 0; step < 8; step++) {
						double angle = Math.PI * 2 * step / 8;
						level.sendParticles(ParticleTypes.FLAME,
								player.getX() + Math.cos(angle) * 0.65, player.getY() + 0.1,
								player.getZ() + Math.sin(angle) * 0.65, 1, 0, 0, 0, 0);
					}
					level.playSound(null, player, SoundEvents.NOTE_BLOCK_PLING,
							SoundSource.HOSTILE, 0.8F, 1.8F);
				}
				if (chicken.tickCount > 0 && chicken.tickCount % reload == 0) {
					if (rpg) {
						Vec3 aim = player.getBoundingBox().getCenter().subtract(chicken.getEyePosition());
						if (level.addFreshEntity(new PurgeRocketEntity(level, chicken, aim))
								&& hasName(chicken, "RPG Chicken")) {
							markRpgRoundUsed(chicken);
						}
					} else {
						player.hurtServer(level, level.damageSources().mobAttack(chicken),
								(overlordSniper ? 18 + threat * 3 : 7 + threat)
										* BattleChickens.CONFIG.enemyDamageMultiplier());
					}
				}
				chicken.getNavigation().stop();
				return;
			}
		}
		if (hasName(chicken, "Grenadier Hen") && distanceSquared <= 20 * 20) {
			chicken.getNavigation().stop();
			if (chicken.tickCount % 100 == 70) {
				Vec3 impact = player.position();
				GRENADE_TARGETS.put(chicken.getUUID(), impact);
				BossController.showCombatAlert(player, net.minecraft.network.chat.Component.literal(
						"Grenadier bomb incoming — leave the marked spot!"));
				emitBlastRing(level, player.position(), 2.5);
				throwVisibleGrenade(level, chicken, impact, 30);
			}
			if (chicken.tickCount % 100 == 0) {
				Vec3 impact = GRENADE_TARGETS.remove(chicken.getUUID());
				detonateGrenade(level, chicken, impact == null ? player.position() : impact, threat);
			}
		} else if (distanceSquared > 2.8 * 2.8) {
			chicken.getNavigation().moveTo(player, 2.0);
		} else if (chicken.tickCount % 30 == 0) {
			player.hurtServer(level, level.damageSources().mobAttack(chicken),
					(3 + threat) * BattleChickens.CONFIG.enemyDamageMultiplier());
		}
	}

	private static void detonateGrenade(ServerLevel level, Chicken grenade, Vec3 impact, int threat) {
			double radius = 3.5;
			boolean reflected = false;
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
					new AABB(impact, impact).inflate(radius), LivingEntity::isAlive)) {
				if (target == grenade || (isPurgeAlly(grenade) && isPurgeAlly(target))) {
					continue;
				}
				double distance = Math.sqrt(target.distanceToSqr(impact));
				if (distance > radius || !level.clip(new net.minecraft.world.level.ClipContext(
						impact.add(0, 0.5, 0), target.getBoundingBox().getCenter(),
						net.minecraft.world.level.ClipContext.Block.COLLIDER,
						net.minecraft.world.level.ClipContext.Fluid.NONE, grenade)).getType().equals(
						net.minecraft.world.phys.HitResult.Type.MISS)) {
					continue;
				}
				float damage = (8 + threat * 2) * (float) (1.0 - distance / (radius + 0.5));
				if (target instanceof ServerPlayer player) {
					if (!reflected && player.getOffhandItem().is(ModItems.CLUCK_SHIELD)) {
						grenade.hurtServer(level, level.damageSources().playerAttack(player), damage * 0.5F);
						reflected = true;
						continue;
					}
					damage *= explosiveDefenseMultiplier(player);
				}
				if (damage > 0) {
					target.hurtServer(level, level.damageSources().mobAttack(grenade), damage);
					Vec3 knockback = target.position().subtract(impact).normalize();
					BossController.applyDaySevenKnockback(target, knockback.scale(0.7).add(0, 0.3, 0));
				}
			}
			if (level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
				int blockRadius = Math.min(2, BattleChickens.CONFIG.rocketBlockRadius());
				BlockPos center = BlockPos.containing(impact);
				for (int x = -blockRadius; x <= blockRadius; x++) {
					for (int y = -blockRadius; y <= blockRadius; y++) {
						for (int z = -blockRadius; z <= blockRadius; z++) {
							if (x * x + y * y + z * z <= blockRadius * blockRadius) {
								BlockPos block = center.offset(x, y, z);
								if (!level.getBlockState(block).isAir()
										&& !com.cluck.block.BunkerBlock.handleRocketHit(null, level, block, grenade)) {
									level.destroyBlock(block, true, grenade);
								}
							}
						}
					}
				}
			}
			level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 14, 0.45, 0.35, 0.45, 0.04);
	}

	private static void emitBlastRing(ServerLevel level, Vec3 center, double radius) {
			for (int step = 0; step < 16; step++) {
				double angle = Math.PI * 2 * step / 16;
				level.sendParticles(ParticleTypes.FLAME,
						center.x + Math.cos(angle) * radius, center.y + 0.1,
						center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
			}
	}

	public static float explosiveDefenseMultiplier(ServerPlayer player) {
			if (player.getOffhandItem().is(ModItems.CLUCK_SHIELD)) {
				return 0.5F;
			}
			if (hasInventoryItem(player, ModItems.REACTIVE_PLATING)) {
				return 0.6F;
			}
			return 1.0F;
	}

	public static boolean hasInventoryItem(ServerPlayer player, Item item) {
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				if (player.getInventory().getItem(slot).is(item)) {
					return true;
				}
			}
			return false;
	}

	public static boolean spawnOverlord(List<ServerPlayer> players, PurgeWorldData data) {
		for (ServerPlayer player : players) {
			if (!player.isAlive()) {
				continue;
			}
			ServerLevel level = player.level();
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			int distance = 80 + level.getRandom().nextInt(21);
			int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			Ravager overlord = EntityTypes.RAVAGER.create(level, EntitySpawnReason.EVENT);
			Chicken rider = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "overlord");
			if (overlord == null || rider == null) {
				return false;
			}
			overlord.setPos(x + 0.5, y, z + 0.5);
			overlord.setPersistenceRequired();
			overlord.setCustomName(net.minecraft.network.chat.Component.literal("The Overlord"));
			overlord.setCustomNameVisible(true);
			if (overlord.getAttribute(Attributes.MAX_HEALTH) != null) {
				overlord.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
						(1400 + data.threat() * 350) * BattleChickens.CONFIG.enemyHealthMultiplier());
				overlord.setHealth((float) overlord.getAttributeValue(Attributes.MAX_HEALTH));
			}
			if (overlord.getAttribute(Attributes.SCALE) != null) {
				overlord.getAttribute(Attributes.SCALE).setBaseValue(4.0);
				overlord.refreshDimensions();
			}
			overlord.setPermanentlyInvulnerable(true);
			rider.setPos(x + 0.5, y + 1, z + 0.5);
			rider.setPersistenceRequired();
			rider.setCustomName(net.minecraft.network.chat.Component.literal("Overlord Rooster"));
			rider.setCustomNameVisible(true);
			rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
			if (rider.getAttribute(Attributes.MAX_HEALTH) != null) {
				rider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);
				rider.setHealth(1);
			}
			if (rider.getAttribute(Attributes.SCALE) != null) {
				rider.getAttribute(Attributes.SCALE).setBaseValue(2.5);
				rider.refreshDimensions();
			}
			if (!level.addFreshEntity(overlord)) {
				return false;
			}
			if (!level.addFreshEntity(rider)) {
				overlord.discard();
				return false;
			}
			rider.startRiding(overlord, true, true);
			spawnMountedOverlordSniper(level, overlord, player);
			BossController.spawnShieldGenerators(level, overlord, player, data.threat());
			spawnOverlordEscorts(level, overlord, player, data);
			spawnOverlordMedics(level, overlord, player, data.threat());
			spawnAerialOverlord(level, overlord, player, data);
			return true;
		}
		return false;
	}

	private static void spawnOverlordEscorts(ServerLevel level, Ravager boss, ServerPlayer target, PurgeWorldData data) {
		int escortCount = data.threat() >= 4 ? 3 : 2;
		for (int index = 0; index < escortCount; index++) {
			double angle = Math.PI * 2 * index / escortCount;
			int x = (int) Math.round(boss.getX() + Math.cos(angle) * 8);
			int z = (int) Math.round(boss.getZ() + Math.sin(angle) * 8);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			Chicken escort = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "overlord_rpg");
			if (escort == null) {
				continue;
			}
			escort.setPos(x + 0.5, y, z + 0.5);
			escort.setPersistenceRequired();
			escort.setCustomName(net.minecraft.network.chat.Component.literal("Overlord RPG Chicken"));
			escort.setCustomNameVisible(true);
			escort.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
			if (escort.getAttribute(Attributes.MAX_HEALTH) != null) {
				escort.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
						(24 + data.threat() * 4) * BattleChickens.CONFIG.enemyHealthMultiplier());
				escort.setHealth((float) escort.getAttributeValue(Attributes.MAX_HEALTH));
			}
			escort.setTarget(target);
			level.addFreshEntity(escort);
		}
		String[] generals = {"Summoner General", "Tank General", "Bomber General", "Buffer General"};
		int generalCount = data.threat() >= 4 ? generals.length * 2 : generals.length;
		for (int index = 0; index < generalCount; index++) {
			int generalRole = index % generals.length;
			double angle = Math.PI * 2 * index / generalCount + Math.PI / 4;
			int x = (int) Math.round(boss.getX() + Math.cos(angle) * 12);
			int z = (int) Math.round(boss.getZ() + Math.sin(angle) * 12);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			Chicken general = ModEntities.createChicken(level, EntitySpawnReason.EVENT,
					switch (generalRole) {
						case 0 -> "summoner";
						case 1 -> "tank";
						case 2 -> "bomber";
						default -> "buffer";
					});
			if (general == null) {
				continue;
			}
			general.setPos(x + 0.5, y, z + 0.5);
			general.setPersistenceRequired();
			general.setCustomName(net.minecraft.network.chat.Component.literal(generals[generalRole]));
			general.setCustomNameVisible(true);
			general.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(
					generalRole == 1 ? Items.IRON_AXE : generalRole == 2 ? Items.FIRE_CHARGE : Items.IRON_SWORD));
			general.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
			if (general.getAttribute(Attributes.MAX_HEALTH) != null) {
				general.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
						(40 + data.threat() * 5) * BattleChickens.CONFIG.enemyHealthMultiplier());
				general.setHealth((float) general.getAttributeValue(Attributes.MAX_HEALTH));
			}
			general.setTarget(target);
			level.addFreshEntity(general);
			if (generalRole == 2) {
				attachGrenadeModel(level, general);
			}
		}
		for (int index = 0; index < 2; index++) {
			spawnSummonedChicken(level, boss, target);
		}
		spawnWarlordGunner(level, boss, target, "Overlord Gunner", false);
	}

	private static void spawnAerialOverlord(ServerLevel level, Ravager groundBoss,
											ServerPlayer target, PurgeWorldData data) {
		Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT);
		Chicken rider = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "bombardier");
		if (phantom == null || rider == null) {
			return;
		}
		phantom.setPos(groundBoss.getX() + 6, groundBoss.getY() + 18, groundBoss.getZ() + 6);
		phantom.setPersistenceRequired();
		phantom.setCustomName(net.minecraft.network.chat.Component.literal("The Sky Overlord"));
		phantom.setCustomNameVisible(true);
		phantom.setPhantomSize(8);
		if (phantom.getAttribute(Attributes.MAX_HEALTH) != null) {
			double health = (500 + data.threat() * 150) * BattleChickens.CONFIG.enemyHealthMultiplier();
			phantom.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			phantom.setHealth((float) health);
		}
		if (phantom.getAttribute(Attributes.SCALE) != null) {
			phantom.getAttribute(Attributes.SCALE).setBaseValue(2.5);
			phantom.refreshDimensions();
		}
		phantom.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1200, 0, true, false));
		phantom.setTarget(target);
		rider.setPos(phantom.getX(), phantom.getY() + 2, phantom.getZ());
		rider.setPersistenceRequired();
		rider.setCustomName(net.minecraft.network.chat.Component.literal("Overlord Bombardier"));
		rider.setCustomNameVisible(true);
		rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
		rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
		if (rider.getAttribute(Attributes.MAX_HEALTH) != null) {
			rider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);
			rider.setHealth(1);
		}
		if (rider.getAttribute(Attributes.SCALE) != null) {
			rider.getAttribute(Attributes.SCALE).setBaseValue(1.8);
			rider.refreshDimensions();
		}
		if (!level.addFreshEntity(phantom)) {
			return;
		}
		if (!level.addFreshEntity(rider)) {
			phantom.discard();
			return;
		}
		rider.startRiding(phantom, true, true);
		attachGrenadeModel(level, rider);
		spawnWarlordGunner(level, phantom, target, "Sky Gunner", true);
	}

	private static void spawnMountedOverlordSniper(ServerLevel level, Ravager boss, ServerPlayer target) {
		Chicken sniper = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "sniper");
		if (sniper == null) {
			return;
		}

		sniper.setPos(boss.position());
		sniper.setPersistenceRequired();
		sniper.setCustomName(net.minecraft.network.chat.Component.literal("Overlord Sniper"));
		sniper.setCustomNameVisible(true);
		sniper.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
		sniper.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		if (sniper.getAttribute(Attributes.MAX_HEALTH) != null) {
			double health = 20 * BattleChickens.CONFIG.enemyHealthMultiplier();
			sniper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			sniper.setHealth((float) health);
		}
		sniper.setTarget(target);
		if (level.addFreshEntity(sniper)) {
			sniper.startRiding(boss, true, true);
		}
	}

	private static void spawnOverlordMedics(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		for (int index = 0; index < 2; index++) {
			Chicken medic = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "medic");
			if (medic == null) {
				continue;
			}
			double offset = index == 0 ? -5 : 5;
			medic.setPos(boss.getX() + offset, boss.getY(), boss.getZ() + 5);
			medic.setPersistenceRequired();
			medic.setCustomName(net.minecraft.network.chat.Component.literal("Medic Hen"));
			medic.setCustomNameVisible(true);
			medic.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPLASH_POTION));
			medic.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			medic.setTarget(target);
			if (medic.getAttribute(Attributes.MAX_HEALTH) != null) {
				double health = (36 + threat * 4) * BattleChickens.CONFIG.enemyHealthMultiplier();
				medic.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
				medic.setHealth((float) health);
			}
			level.addFreshEntity(medic);
		}
	}

	public static void spawnWarlordGunner(ServerLevel level, Entity warlord, ServerPlayer target,
										  String name, boolean airborne) {
		int cap = airborne ? 2 : 3;
		long existing = level.getEntitiesOfClass(Chicken.class, warlord.getBoundingBox().inflate(64),
				entity -> isGunner(entity) && entity.getCustomName() != null
						&& entity.getCustomName().getString().equals(name)).size();
		if (existing >= cap) {
			return;
		}
		Chicken gunner = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "gunner");
		if (gunner == null) {
			return;
		}
		gunner.setPos(warlord.position());
		gunner.setPersistenceRequired();
		gunner.setCustomName(net.minecraft.network.chat.Component.literal(name));
		gunner.setCustomNameVisible(true);
		gunner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BEAK_47));
		gunner.setTarget(target);
		if (!level.addFreshEntity(gunner)) {
			return;
		}
		if (airborne) {
			gunner.startRiding(warlord, true, true);
		}
	}

	private static void attachGrenadeModel(ServerLevel level, Chicken chicken) {
		Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.EVENT);
		if (display == null) {
			return;
		}
		display.getSlot(0).set(new ItemStack(ModItems.PURGE_GRENADE));
		display.setCustomName(net.minecraft.network.chat.Component.literal("Grenadier Bomb Model"));
		if (level.addFreshEntity(display)) {
			display.startRiding(chicken, true, true);
		}
	}

	public static void throwBossGrenade(ServerLevel level, Phantom bomber, Vec3 impact) {
		throwVisibleGrenade(level, bomber, impact, 40);
		level.playSound(null, bomber, net.minecraft.sounds.SoundEvents.CREEPER_PRIMED,
				net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.65F);
	}

	private static void throwVisibleGrenade(ServerLevel level, LivingEntity bomber, Vec3 impact, int flightTicks) {
		ItemEntity bomb = new ItemEntity(level, bomber.getX(), bomber.getY() - 0.5,
				bomber.getZ(), new ItemStack(ModItems.PURGE_GRENADE));
		bomb.setThrower(bomber);
		bomb.setNeverPickUp();
		bomb.setUnlimitedLifetime();
		bomb.setCustomName(net.minecraft.network.chat.Component.literal("Purge Grenade Projectile"));
		Vec3 difference = impact.subtract(bomb.position());
		double gravity = 0.04;
		bomb.setDeltaMovement(difference.x / flightTicks,
				(difference.y + 0.5 * gravity * flightTicks * flightTicks) / flightTicks,
				difference.z / flightTicks);
		level.addFreshEntity(bomb);
	}

	public static void detonateBossGrenade(ServerLevel level, LivingEntity bomber, Vec3 impact, int threat) {
		double radius = 6.0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(impact, impact).inflate(radius), LivingEntity::isAlive)) {
			if (target == bomber || (isPurgeAlly(bomber) && isPurgeAlly(target))) {
				continue;
			}
			double distance = Math.sqrt(target.distanceToSqr(impact));
			if (distance > radius) {
				continue;
			}
			float damage = (32 + threat * 4) * (float) Math.max(0.15, 1.0 - distance / (radius + 0.5));
			if (target instanceof ServerPlayer player) {
				damage *= explosiveDefenseMultiplier(player);
			}
			target.hurtServer(level, level.damageSources().mobAttack(bomber), damage);
			Vec3 knockback = target.position().subtract(impact).normalize();
			BossController.applyDaySevenKnockback(target, knockback.scale(1.4).add(0, 0.7, 0));
		}
		if (level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			int blockRadius = BattleChickens.CONFIG.bossTerrainRadius();
			BlockPos center = BlockPos.containing(impact);
			for (int x = -blockRadius; x <= blockRadius; x++) {
				for (int y = -blockRadius; y <= blockRadius; y++) {
					for (int z = -blockRadius; z <= blockRadius; z++) {
						if (x * x + y * y + z * z > blockRadius * blockRadius) {
							continue;
						}
						BlockPos pos = center.offset(x, y, z);
						if (!level.getBlockState(pos).isAir()
								&& !com.cluck.block.BunkerBlock.handleRocketHit(null, level, pos, bomber)) {
							level.destroyBlock(pos, false, bomber);
						}
					}
				}
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 3, 0.5, 0.5, 0.5, 0.1);
		level.sendParticles(ParticleTypes.LAVA, impact.x, impact.y, impact.z, 18, 1.4, 0.5, 1.4, 0.05);
		level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 36, 1.8, 1.2, 1.8, 0.08);
		level.playSound(null, BlockPos.containing(impact), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
				net.minecraft.sounds.SoundSource.HOSTILE, 4.0F, 0.7F);
	}

	private static void spawnSummonedChicken(ServerLevel level, Entity origin, ServerPlayer target) {
		Chicken summon = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "summoned");
		if (summon == null) {
			return;
		}
		summon.setPos(origin.getX() + level.getRandom().nextInt(5) - 2, origin.getY(),
				origin.getZ() + level.getRandom().nextInt(5) - 2);
		summon.setPersistenceRequired();
		summon.setCustomName(net.minecraft.network.chat.Component.literal("Summoned Chicken"));
		summon.setCustomNameVisible(true);
		summon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		summon.setTarget(target);
		level.addFreshEntity(summon);
	}

	public static void spawnBossReinforcements(ServerLevel level, Entity boss, ServerPlayer target, int threat) {
		int nearby = level.getEntitiesOfClass(Chicken.class, boss.getBoundingBox().inflate(96),
				PurgeUnits::isUnit).size();
		int available = Math.min(2, BattleChickens.CONFIG.hostileChickenCap() - nearby);
		for (int index = 0; index < available; index++) {
			long nearbyRpgs = level.getEntitiesOfClass(Chicken.class, boss.getBoundingBox().inflate(96),
					entity -> hasName(entity, "RPG Chicken") || hasName(entity, "Overlord RPG Chicken")).size();
			int rpgCap = BattleChickens.CONFIG.rpgCapPerPlayer() * 2;
			int roll = level.getRandom().nextInt(100);
			String role = roll < 15 && nearbyRpgs < rpgCap ? "rpg"
					: roll < 40 ? "grenadier"
					: roll < 60 ? "sniper"
					: roll < 80 ? "medic"
					: roll < 90 ? "rifle" : "sergeant";
			Chicken soldier = ModEntities.createChicken(level, EntitySpawnReason.EVENT, role);
			if (soldier == null) {
				continue;
			}

			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			soldier.setPos(boss.getX() + Math.cos(angle) * 5, boss.getY(),
					boss.getZ() + Math.sin(angle) * 5);
			soldier.setPersistenceRequired();
			equipRole(soldier, role, PurgeWorldData.get(level), level);
			soldier.setCustomNameVisible(true);
			if (soldier.getAttribute(Attributes.MAX_HEALTH) != null) {
				soldier.getAttribute(Attributes.MAX_HEALTH).setBaseValue(
						(24 + threat * 4) * BattleChickens.CONFIG.enemyHealthMultiplier());
				soldier.setHealth((float) soldier.getAttributeValue(Attributes.MAX_HEALTH));
			}
			soldier.setTarget(target);
			if (level.addFreshEntity(soldier) && role.equals("grenadier")) {
				attachGrenadeModel(level, soldier);
			}
		}
	}

	public static void spawnRavagerMilitia(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		int active = level.getEntitiesOfClass(Chicken.class, boss.getBoundingBox().inflate(64),
				entity -> entity.entityTags().contains("battle_chickens_ravager_assault")).size();
		int count = Math.min(2, 6 - active);
		for (int index = 0; index < count; index++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			double distance = 4 + level.getRandom().nextDouble() * 3;
			int x = (int) Math.floor(boss.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(boss.getZ() + Math.sin(angle) * distance);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			Chicken assault = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "militia");
			if (assault == null) {
				continue;
			}
			assault.setPos(x + 0.5, y, z + 0.5);
			assault.setPersistenceRequired();
			assault.setCustomName(net.minecraft.network.chat.Component.literal("Ravager Assault Chicken"));
			assault.setCustomNameVisible(true);
			assault.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			assault.addTag("battle_chickens_ravager_assault");
			assault.setTarget(target);
			if (assault.getAttribute(Attributes.MAX_HEALTH) != null) {
				double health = (20 + threat * 4) * BattleChickens.CONFIG.enemyHealthMultiplier();
				assault.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
				assault.setHealth((float) health);
			}
			level.addFreshEntity(assault);
		}
	}

	public static void destroyBossImpact(ServerLevel level, Vec3 impact, Entity boss) {
		if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		int radius = BattleChickens.CONFIG.bossTerrainRadius();
		BlockPos center = BlockPos.containing(impact);
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					if (x * x + y * y + z * z > radius * radius) {
						continue;
					}
					BlockPos pos = center.offset(x, y, z);
					if (level.getBlockState(pos).isAir()) {
						continue;
					}
					if (com.cluck.block.BunkerBlock.handleRocketHit(null, level, pos, boss)) {
						continue;
					}
					level.destroyBlock(pos, false, boss);
				}
			}
		}
	}

	public static void detonateBossTnt(ServerLevel level, LivingEntity boss, Vec3 impact, int threat,
									   double radius, float damage) {
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
				new AABB(impact, impact).inflate(radius), ServerPlayer::isAlive)) {
			double distance = Math.sqrt(player.distanceToSqr(impact));
			if (distance > radius) {
				continue;
			}
			float scaledDamage = damage * (float) Math.max(0.15, 1.0 - distance / (radius + 0.5))
					* BattleChickens.CONFIG.enemyDamageMultiplier()
					* explosiveDefenseMultiplier(player);
			player.hurtServer(level, level.damageSources().mobAttack(boss), scaledDamage);
			Vec3 knockback = player.position().subtract(impact).normalize();
			BossController.applyDaySevenKnockback(player, knockback.scale(1.2).add(0, 0.8, 0));
		}
		destroyBossImpact(level, impact, boss);
		level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 3, 0.5, 0.5, 0.5, 0.08);
		level.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z, 24, 1.2, 0.7, 1.2, 0.08);
		level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 34, 1.5, 0.9, 1.5, 0.07);
		level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.HOSTILE, 4.0F, 0.65F);
	}

	private static void markRpgRoundUsed(Chicken chicken) {
		for (int round = 1; round <= 3; round++) {
			String tag = "battle_chickens_rpg_round_" + round;
			if (!chicken.entityTags().contains(tag)) {
				chicken.addTag(tag);
				return;
			}
		}
	}

	public static boolean isBoss(Entity entity) {
		return hasName(entity, "The Overlord");
	}

	public static boolean isFinalBoss(Entity entity) {
		return isBoss(entity) || BossController.isAerialBoss(entity);
	}

	public static boolean isUnit(Entity entity) {
		if (entity == null) {
			return false;
		}
		String name = entity.getCustomName() == null ? "" : entity.getCustomName().getString();
		return name.equals("RPG Chicken") || name.equals("Sniper Hen")
				|| name.equals("Sergeant Chicken") || name.equals("Militia Chicken")
				|| name.equals("Scavenger Chicken") || name.equals("Outpost Guard")
				|| name.equals("Overlord RPG Chicken") || name.equals("Grenadier Hen")
				|| name.equals("Medic Hen") || name.equals("Riot Chicken")
				|| name.equals("Gunship Gunner") || name.equals("Overlord Gunner")
				|| name.equals("Sky Gunner") || name.equals("Overlord Sniper")
				|| name.endsWith("General") || name.equals("Summoned Chicken")
				|| name.equals("Ravager Assault Chicken")
				|| name.equals("Armored Ravager Tank") || name.equals("Golem Walker")
				|| name.equals("Purge Drone");
	}

	public static boolean isRewardedUnit(Entity entity) {
		return isUnit(entity) || hasName(entity, "Purged Gunship") || isNemesis(entity);
	}

	public static boolean isPurgeAlly(Entity entity) {
		return isFinalBoss(entity) || isUnit(entity)
				|| BossController.isGenerator(entity) || hasName(entity, "Purged Gunship")
				|| hasName(entity, "Overlord Rooster") || hasName(entity, "Overlord Bombardier")
				|| hasName(entity, "Sky Overlord's Enraged Golem");
	}

	public static void clearOutpostSquads(ServerLevel level, Vec3 position) {
		AABB radius = new AABB(position, position).inflate(40);
		for (Entity entity : level.getEntitiesOfClass(Entity.class, radius,
				candidate -> isUnit(candidate) && !hasName(candidate, "Outpost Guard"))) {
			entity.discard();
		}
	}

	public static void triggerMeltdown(ServerLevel level, List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (player.level() != level) {
				continue;
			}
			Ravager boss = level.getEntitiesOfClass(Ravager.class, player.getBoundingBox().inflate(128),
					PurgeUnits::isBoss).stream().findFirst().orElse(null);
			if (boss == null) {
				continue;
			}
			Vec3 impact = boss.position();
			level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 48, 2, 1, 2, 0.05);
			if (level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
				int radius = BattleChickens.CONFIG.bossTerrainRadius();
				BlockPos center = BlockPos.containing(impact);
				for (int x = -radius; x <= radius; x++) {
					for (int z = -radius; z <= radius; z++) {
						if (x * x + z * z > radius * radius) {
							continue;
						}
						for (int y = -2; y <= 0; y++) {
							BlockPos pos = center.offset(x, y, z);
							if (!level.getBlockState(pos).isAir()) {
								level.destroyBlock(pos, false, boss);
							}
						}
					}
				}
			}
			BlockPos cachePos = BlockPos.containing(impact).above();
			boolean cacheCreated = false;
			if (level.getBlockState(cachePos).isAir()
					&& level.getBlockState(cachePos.below()).isSolid()) {
				level.setBlockAndUpdate(cachePos, Blocks.CHEST.defaultBlockState());
				if (level.getBlockEntity(cachePos) instanceof ChestBlockEntity cache) {
					for (int slot = 0; slot < 12; slot++) {
						cache.setItem(slot, new ItemStack(Items.EGG, 2));
					}
					cache.setChanged();
					cacheCreated = true;
				}
			}
			if (!cacheCreated) {
				for (int index = 0; index < 12; index++) {
					double offsetX = level.getRandom().nextDouble() * 8 - 4;
					double offsetZ = level.getRandom().nextDouble() * 8 - 4;
					drop(level, impact.add(offsetX, 1, offsetZ), Items.EGG, 2);
				}
			}
			return;
		}
	}

	public static void clearEventEntities(ServerLevel level, List<ServerPlayer> players, boolean clearUnits) {
		Set<UUID> removed = new HashSet<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity == null) {
				continue;
			}
			BossController.forget(entity);
			if (removed.add(entity.getUUID()) && (isBoss(entity) || BossController.isGenerator(entity)
					|| BossController.isAerialBoss(entity)
					|| hasName(entity, "Overlord Rooster")
					|| hasName(entity, "Overlord Bombardier")
					|| hasName(entity, "Overlord Bomb")
					|| hasName(entity, "Overlord TNT")
					|| hasName(entity, "Sky Overlord's Enraged Golem")
					|| hasName(entity, "Purge Grenade Projectile")
					|| hasName(entity, "Grenadier Bomb Model")
					|| hasName(entity, "Purge Rocket")
					|| (clearUnits && (isUnit(entity) || hasName(entity, "Purged Gunship"))))) {
				entity.discard();
			}
		}
	}

	public static void removeGrenadeModel(Entity entity) {
		for (Entity passenger : List.copyOf(entity.getPassengers())) {
			if (hasName(passenger, "Grenadier Bomb Model")) {
				passenger.discard();
			}
		}
	}

	public static void discardPassengers(Entity entity) {
		for (Entity passenger : List.copyOf(entity.getPassengers())) {
			discardPassengers(passenger);
			passenger.discard();
		}
	}

	public static void dropSalvage(LivingEntity dead, ServerLevel level, int threat, boolean dominion) {
		int baseFeathers = 1 + level.getRandom().nextInt(2);
		drop(level, dead.position(), ModItems.WAR_FEATHER, scaleSalvage(baseFeathers, threat, dominion, level));
		if (level.getRandom().nextFloat() < 0.5F) {
			drop(level, dead.position(), ModItems.SCRAP_PLATE, scaleSalvage(1, threat, dominion, level));
		}
		if (hasName(dead, "RPG Chicken") || hasName(dead, "Overlord RPG Chicken")) {
			drop(level, dead.position(), ModItems.ROCKET_CASING, scaleSalvage(1, threat, dominion, level));
			if (level.getRandom().nextFloat() < 0.08F) {
				drop(level, dead.position(), ModItems.CAPTURED_PECKZOOKA, 1);
			}
		} else if (hasName(dead, "Sniper Hen")) {
			drop(level, dead.position(), ModItems.SCRAP_CIRCUIT, scaleSalvage(1, threat, dominion, level));
		} else if (hasName(dead, "Grenadier Hen")) {
			drop(level, dead.position(), ModItems.GUNPOWDER_POUCH, 1 + level.getRandom().nextInt(2));
		} else if (hasName(dead, "Medic Hen")) {
			drop(level, dead.position(), ModItems.MEDIC_SYRINGE, 1);
		} else if (hasName(dead, "Riot Chicken")) {
			drop(level, dead.position(), ModItems.SCRAP_PLATE, 2);
		} else if (dead.getCustomName() != null && dead.getCustomName().getString().endsWith("General")) {
			drop(level, dead.position(), ModItems.GENERALS_PLUME, 1);
			drop(level, dead.position(), ModItems.SCRAP_PLATE, 2);
		} else if (dead.getCustomName() != null && dead.getCustomName().getString().contains("Sergeant")) {
			drop(level, dead.position(), ModItems.GOLDEN_FEATHER, 1);
		} else if (hasName(dead, "Purged Gunship")) {
			drop(level, dead.position(), ModItems.SCRAP_CIRCUIT, 2);
		} else if (hasName(dead, "Purge Drone")) {
			drop(level, dead.position(), ModItems.SCRAP_CIRCUIT, 1 + level.getRandom().nextInt(2));
		} else if (hasName(dead, "Armored Ravager Tank") || hasName(dead, "Golem Walker")) {
			drop(level, dead.position(), ModItems.SCRAP_PLATE, 3);
			if (level.getRandom().nextFloat() < 0.15F) {
				drop(level, dead.position(), ModItems.REACTIVE_PLATING, 1);
			}
		}
	}

	private static int scaleSalvage(int amount, int threat, boolean dominion, ServerLevel level) {
		double multiplier = (1.0 + Math.max(0, threat - 1) * 0.5)
				* BattleChickens.CONFIG.rewardMultiplier();
		if (dominion) {
			multiplier *= 1.1;
		}
		double scaled = amount * multiplier;
		int result = (int) scaled;
		return result + (level.getRandom().nextDouble() < scaled - result ? 1 : 0);
	}

	public static void drop(ServerLevel level, Vec3 pos, Item item, int count) {
		if (count <= 0) {
			return;
		}
		level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, pos.x, pos.y, pos.z, new ItemStack(item, count)));
	}

	private static boolean hasName(Entity entity, String name) {
		return entity != null && entity.getCustomName() != null
				&& entity.getCustomName().getString().equals(name);
	}

	private static boolean isGunner(Entity entity) {
		return entity != null && entity.getCustomName() != null
				&& entity.getCustomName().getString().contains("Gunner");
	}
}
