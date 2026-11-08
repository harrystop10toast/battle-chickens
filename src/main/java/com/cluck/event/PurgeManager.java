package com.cluck.event;

import com.cluck.BattleChickens;
import com.cluck.entity.ModEntities;
import com.cluck.entity.PurgeDrone;
import com.cluck.entity.VeteranRooster;
import com.cluck.item.ModItems;
import com.cluck.item.BulletTracer;
import com.cluck.item.LassoToss;
import com.cluck.item.ScarecrowTurretItem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class PurgeManager {
	private static ServerBossEvent bossBar;
	private static ServerBossEvent overlordHealthBar;
	private static ServerBossEvent aerialHealthBar;
	private static MinecraftServer bossBarServer;
	private static int displayedDay;
	private static int bossAbandonmentTicks;
	private static final Set<UUID> DAMAGE_CAP_REENTRY = new HashSet<>();
	private static final Set<UUID> FLOCKGUARD_REENTRY = new HashSet<>();
	private static final Set<UUID> EXPLOSION_SHIELD_REENTRY = new HashSet<>();

	private PurgeManager() {
	}

	public static void register() {
		ScarecrowTurretItem.registerEntityInteraction();
		ServerTickEvents.END_SERVER_TICK.register(PurgeManager::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(PurgeManager::onDeath);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (DAMAGE_CAP_REENTRY.contains(entity.getUUID())) {
				return true;
			}
			if (FLOCKGUARD_REENTRY.contains(entity.getUUID())) {
				return true;
			}
			if (EXPLOSION_SHIELD_REENTRY.contains(entity.getUUID())) {
				return true;
			}
			Entity attacker = source.getEntity();
			if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile
					&& projectile.getOwner() != null) {
				attacker = projectile.getOwner();
			}
			if (entity instanceof LivingEntity target && attacker instanceof LivingEntity livingAttacker
					&& entity != attacker && entity.level() instanceof ServerLevel level
					&& !(source.getDirectEntity() instanceof PurgeDrone)
					&& !PurgeUnits.hasClearAttackPath(level, livingAttacker, target)) {
				return false;
			}
			if (entity instanceof LivingEntity target && source.is(DamageTypeTags.IS_EXPLOSION)
					&& entity.level() instanceof ServerLevel level
					&& source.getSourcePosition() != null
					&& !PurgeUnits.hasBlastExposure(level, source.getSourcePosition(), target)) {
				return false;
			}
			if (entity instanceof ServerPlayer player && source.is(DamageTypeTags.IS_EXPLOSION)
					&& player.getOffhandItem().is(ModItems.CLUCK_SHIELD)
					&& entity.level() instanceof ServerLevel level) {
				EXPLOSION_SHIELD_REENTRY.add(entity.getUUID());
				try {
					player.hurtServer(level, source, amount * 0.5F);
				} finally {
					EXPLOSION_SHIELD_REENTRY.remove(entity.getUUID());
				}
				return false;
			}
			if (entity instanceof Chicken chicken
					&& chicken.getVehicle() instanceof LivingEntity mount
					&& entity.level() instanceof ServerLevel level
					&& chicken.getCustomName() != null) {
				String riderName = chicken.getCustomName().getString();
				boolean sharesRavagerHealth = riderName.equals("Overlord Rooster") && PurgeUnits.isBoss(mount);
				boolean sharesPhantomHealth = riderName.equals("Overlord Bombardier")
						&& BossController.isAerialBoss(mount);
				if (sharesRavagerHealth || sharesPhantomHealth) {
					if (attacker != mount) {
						mount.hurtServer(level, source, amount);
					}
					return false;
				}
			}
			if (entity instanceof ServerPlayer player && attacker instanceof Chicken
					&& (player.getMainHandItem().is(ModItems.FLOCKGUARD_SIGIL)
							|| player.getOffhandItem().is(ModItems.FLOCKGUARD_SIGIL))
					&& entity.level() instanceof ServerLevel level) {
				FLOCKGUARD_REENTRY.add(entity.getUUID());
				try {
					entity.hurtServer(level, source, amount * 0.6F);
				} finally {
					FLOCKGUARD_REENTRY.remove(entity.getUUID());
				}
				return false;
			}
			if (PurgeUnits.isPurgeAlly(entity) && PurgeUnits.isPurgeAlly(attacker)) {
				if (BossController.isGenerator(entity)) {
					// Shield golem can be hit by Sky Overlord nuke/contact to make the fight easier.
					if (!BossController.isAerialBoss(attacker)) {
						return false;
					}
				} else {
					boolean attackerBoss = PurgeUnits.isBoss(attacker) || BossController.isAerialBoss(attacker);
					boolean attackerChicken = PurgeUnits.isChickenAlly(attacker);
					boolean entityBoss = PurgeUnits.isBoss(entity) || BossController.isAerialBoss(entity);
					boolean entityChicken = PurgeUnits.isChickenAlly(entity);
					if (attackerBoss && entityChicken) {
						return true;
					}
					if (BossController.isGenerator(attacker) && entityChicken) {
						return true;
					}
					if (attackerChicken && entityBoss) {
						if (BossController.isAerialBoss(entity)
								&& entity.level() instanceof ServerLevel level
								&& capAerialBossDamage(entity, source, amount, level)) {
							return false;
						}
						return true;
					}
					if (PurgeUnits.isBoss(attacker) && BossController.isAerialBoss(entity)) {
						if (entity.level() instanceof ServerLevel level
								&& capAerialBossDamage(entity, source, amount, level)) {
							return false;
						}
						return true;
					}
					return false;
				}
			}
			if (BossController.isAerialBoss(entity) && entity.level() instanceof ServerLevel level) {
				float adjustedDamage = PurgeUnits.isBoss(attacker) ? amount * 0.33F : amount;
				float damageCap = entity.getMaxHealth() * 0.05F;
				if (adjustedDamage < amount || adjustedDamage > damageCap) {
					DAMAGE_CAP_REENTRY.add(entity.getUUID());
					try {
						entity.hurtServer(level, source, Math.min(adjustedDamage, damageCap));
					} finally {
						DAMAGE_CAP_REENTRY.remove(entity.getUUID());
					}
					return false;
				}
			}
			if (BossController.isGenerator(entity)
					&& amount > entity.getMaxHealth() / 5.0F
					&& entity.level() instanceof ServerLevel level) {
				DAMAGE_CAP_REENTRY.add(entity.getUUID());
				try {
					entity.hurtServer(level, source, entity.getMaxHealth() / 5.0F);
				} finally {
					DAMAGE_CAP_REENTRY.remove(entity.getUUID());
				}
				return false;
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			PurgeUnits.onWarriorDamaged(entity, source);
			Entity attacker = source.getEntity();
			if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile
					&& projectile.getOwner() != null) {
				attacker = projectile.getOwner();
			}
			if (PurgeUnits.isFinalBoss(entity) && attacker instanceof ServerPlayer player) {
				PurgeWorldData data = PurgeWorldData.get(player.level());
				if (data.isActive() && damageTaken > 0) {
					data.addContribution(player.getUUID().toString(), Math.max(1, (int) Math.ceil(damageTaken)));
				}
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Chicken chicken
					&& ((EntityLoadData) entity).spawnReason() == EntitySpawnReason.NATURAL
					&& !((EntityLoadData) entity).isLoadedFromDisk()) {
				PurgeWorldData data = PurgeWorldData.get(level.getServer().overworld());
				if (data.isActive()) {
					PurgeUnits.promoteNaturalChicken(chicken, data);
				} else if (level.getRandom().nextInt(100)
						< BattleChickens.CONFIG.naturalMilitaryChancePercent()) {
					PurgeUnits.promoteNaturalChicken(chicken);
				}
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(literal("purge")
						.then(literal("status").executes(context -> showStatus(context.getSource())))
						.then(literal("rank").executes(context -> showRank(context.getSource().getPlayerOrException())))
						.then(literal("contribution").executes(context -> showContribution(context.getSource().getPlayerOrException())))
						.then(literal("blueprints").executes(context -> showBlueprints(context.getSource().getPlayerOrException())))
						.then(literal("debug")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(literal("day").then(argument("day", IntegerArgumentType.integer(1))
										.executes(context -> debugDay(context.getSource(),
												IntegerArgumentType.getInteger(context, "day"))))))
						.then(literal("stop")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.executes(context -> stop(context.getSource().getLevel())))));
	}

	public static boolean start(ServerPlayer starter, int threat) {
		PurgeWorldData data = PurgeWorldData.get(starter.level());
		int rank = data.rank(starter.getUUID().toString());
		if (threat == 4 && rank < 3) {
			starter.sendSystemMessage(Component.literal("Threat IV unlocks after reaching Flockbreaker rank III."));
			return false;
		}
		if (threat == 5 && rank < 4) {
			starter.sendSystemMessage(Component.literal("Threat V unlocks after reaching Flockbreaker rank IV."));
			return false;
		}
		MinecraftServer server = starter.level().getServer();
		long worldTick = server.overworld().getGameTime();
		int cooldown = data.remainingCooldownSeconds(worldTick);
		if (cooldown > 0) {
			starter.sendSystemMessage(Component.literal("The next Purge can begin in about " + cooldown + " seconds."));
			return false;
		}
		if (!data.start(threat, BattleChickens.CONFIG.eventDays(), worldTick)) {
			starter.sendSystemMessage(Component.literal("A Purge is already active in this world."));
			return false;
		}
		award(starter, "first_egg", "awakened");
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int playerRank = data.rank(player.getUUID().toString());
			if (playerRank >= 2) {
				reward(player, ModItems.DUD_ROCKET);
			}
			if (playerRank >= 4) {
				reward(player, ModItems.GOLDEN_FEATHER);
			}
			if (playerRank >= 5) {
				reward(player, ModItems.OVERLORD_SHARD);
				player.sendSystemMessage(Component.literal("Overlord of Overlords: rank V bonus salvage."));
			}
		}
		if (data.dominion()) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				reward(player, ModItems.RATIONS_CRATE);
				player.sendSystemMessage(Component.literal("Veteran of the Purge: bonus salvage and a Day 1 Rations Crate."));
			}
		}
		server.getPlayerList().broadcastSystemMessage(
				Component.literal("The Ominous Egg awakens the flock. Purge Threat " + threat + " begins!"), false);
		displayedDay = 1;
		bossAbandonmentTicks = 0;
		updateBossBar(server, data);
		return true;
	}

	private static boolean capAerialBossDamage(LivingEntity entity, DamageSource source, float amount,
			ServerLevel level) {
		float damageCap = entity.getMaxHealth() * 0.05F;
		if (amount <= damageCap) {
			return false;
		}
		DAMAGE_CAP_REENTRY.add(entity.getUUID());
		try {
			entity.hurtServer(level, source, damageCap);
		} finally {
			DAMAGE_CAP_REENTRY.remove(entity.getUUID());
		}
		return true;
	}

	private static void tick(MinecraftServer server) {
		LassoToss.tick(server);
		PurgeWorldData data = PurgeWorldData.get(server.overworld());
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		if (server.overworld().getGameTime() % 20 == 0) {
			for (ServerPlayer leader : players) {
				if (leader.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.OVERLORD_CROWN)) {
					for (ServerPlayer ally : players) {
						if (ally.level() == leader.level() && ally.distanceToSqr(leader) <= 8 * 8) {
							ally.addEffect(new net.minecraft.world.effect.MobEffectInstance(
									net.minecraft.world.effect.MobEffects.STRENGTH, 60, 0, true, false));
						}
					}
				}
				if (leader.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.GENERALS_PLUME)
						&& !leader.level().getEntitiesOfClass(Chicken.class, leader.getBoundingBox().inflate(16),
						PurgeUnits::isUnit).isEmpty()) {
					leader.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.STRENGTH, 60, 0, true, false));
				}
			}
		}
		for (ServerLevel level : server.getAllLevels()) {
			BulletTracer.tick(level);
			PurgeUnits.tickGrenadeFlights(level);
			if (level.getGameTime() % 20 == 0) {
				ScarecrowTurretItem.tick(level);
			}
			PurgeUnits.tickNaturalWarriors(level, players);
			PurgeUnits.tickGigaChickens(level, players);
			PurgeUnits.tickGoldenEggLayers(level, players);
			PurgeUnits.tickCoopConstruction(level, players, data);
			PurgeUnits.tickBunkerSiege(level, players);
		}
		if (server.overworld().getGameTime() % 20 == 0) {
			for (ServerPlayer player : players) {
				int rockets = 0;
				for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
					ItemStack stack = player.getInventory().getItem(slot);
					if (stack.is(ModItems.DUD_ROCKET)) {
						rockets += stack.getCount();
					}
				}
				if (rockets >= 20) {
					award(player, "dud_collector", "dud_collector");
				}
			}
		}
		if (!data.isActive()) {
			data.restoreBossWeather(server.getAllLevels());
			clearBossBar();
			bossAbandonmentTicks = 0;
			return;
		}
		if (server.overworld().getGameTime() % 1200 == 0) {
			for (ServerPlayer player : players) {
				if (player.isAlive()) {
					PurgeUnits.spawnDailySupport(player, data);
				}
			}
		}

		int previousDay = data.currentDay();
		int day = data.tick();
		boolean dayAdvanced = day != previousDay;
		if (day != displayedDay) {
			displayedDay = day;
		}
		if (dayAdvanced) {
			server.getPlayerList().broadcastSystemMessage(Component.literal(
					"Purge Day " + day + " of " + data.eventDays() + (day == data.eventDays()
							? " begins: the Overlord is here."
							: " begins.")), false);
			if (day > 1) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (day == 2 && data.civilianChickenKills(player.getUUID().toString()) == 0
							&& data.survivedPreviousDay(player.getUUID().toString(), day)) {
						award(player, "not_my_chicken", "not_my_chicken");
					}
					if (data.threat() < 4 && !BattleChickens.CONFIG.difficultyPreset().equals("hardcore")
							&& data.survivedPreviousDay(player.getUUID().toString(), day)) {
						reward(player, ModItems.RATIONS_CRATE);
						player.sendSystemMessage(Component.literal("You survived the day and earned a Rations Crate."));
					}
				}
			}
		}

		if (!players.isEmpty()) {
			if (data.currentDay() == data.eventDays()) {
				if (!data.bossSpawned()) {
					if (PurgeUnits.spawnOverlord(players, data)) {
						data.markBossSpawned();
						server.getPlayerList().broadcastSystemMessage(Component.literal(
								"The Overlord has entered the battlefield! Defeat it before the meltdown."), false);
					}
				}
			} else if (data.markDaySpawned()) {
				server.getPlayerList().broadcastSystemMessage(Component.literal(
						"Naturally spawned chickens are being militarized. Stay alert near the flock."), false);
			}
			if (server.overworld().getGameTime() % 200 == 0
					&& data.currentDay() >= 4 && data.currentDay() < data.eventDays() && !data.outpostSpawned()) {
				for (ServerPlayer player : players) {
					if (PurgeOutposts.spawn(player, data)) {
						data.markOutpostSpawned();
						server.getPlayerList().broadcastSystemMessage(Component.literal(
								"A chicken outpost has been located. Raid its chest for rare salvage."), false);
						break;
					}
				}
			}
		}

		for (ServerLevel level : server.getAllLevels()) {
			PurgeUnits.tickUnits(level, players, data.threat());
			PurgeUnits.spawnFinalDayRpgWave(level, players, data);
			if (data.bossSpawned()) {
				BossController.tick(level, players, data.threat());
			}
		}
		if (data.bossSpawned()) {
			data.maintainBossStorm(server.getAllLevels());
			if (server.overworld().getGameTime() % 20 == 0 && data.bossTicks() > 100) {
				if (hasPlayerInBossArena(players)) {
					bossAbandonmentTicks = 0;
				} else if (++bossAbandonmentTicks >= 200) {
					server.getPlayerList().broadcastSystemMessage(Component.literal(
							"All challengers abandoned the battlefield. The Purge has failed."), false);
					fail(server, data);
					return;
				}
			}
		} else {
			bossAbandonmentTicks = 0;
		}
		if (data.bossSpawned()
				&& data.bossTicks() >= BattleChickens.CONFIG.bossTimeoutSeconds() * 20L) {
			fail(server, data);
		}
		updateBossBar(server, data);
	}

	private static void onDeath(LivingEntity dead, net.minecraft.world.damagesource.DamageSource damageSource) {
		if (!(dead.level() instanceof ServerLevel level)) {
			return;
		}
		PurgeUnits.removeGrenadeModel(dead);
		if (dead.getCustomName() != null
				&& dead.getCustomName().getString().equals("Purged Gunship")) {
			PurgeUnits.discardPassengers(dead);
		}
		BossController.forget(dead);
		PurgeWorldData data = PurgeWorldData.get(level);
		Entity killer = damageSource.getEntity();
		if (killer instanceof net.minecraft.world.entity.projectile.Projectile projectile
				&& projectile.getOwner() != null) {
			killer = projectile.getOwner();
		}
		if (data.isActive() && dead instanceof Chicken chicken && killer instanceof ServerPlayer player
				&& !PurgeUnits.isUnit(chicken) && !PurgeUnits.isWarrior(chicken)) {
			data.recordCivilianChickenKill(player.getUUID().toString());
		}
		if (killer instanceof ServerPlayer player
				&& (dead.getCustomName() != null
					&& (dead.getCustomName().getString().equals("Purged Gunship")
							|| dead.getCustomName().getString().equals("Purge Drone")))) {
			data.recordAntiAirKill(player.getUUID().toString());
			if (data.antiAirKills(player.getUUID().toString()) == 10) {
				award(player, "anti_air_ace", "anti_air_ace");
			}
		}
		if (killer instanceof ServerPlayer player
				&& dead.getCustomName() != null
				&& dead.getCustomName().getString().equals("Sniper Hen")) {
			Vec3 directionToPlayer = player.position().subtract(dead.position()).normalize();
			if (dead.getLookAngle().normalize().dot(directionToPlayer) < -0.5) {
				award(player, "silent_hunter", "silent_hunter");
			}
		}
		if (dead instanceof ServerPlayer player && data.isActive()) {
			data.markPlayerDeath(player.getUUID().toString());
			if (killer instanceof Chicken chicken && PurgeUnits.isWarrior(chicken)) {
				PurgeUnits.promoteToNemesis(chicken, player);
				player.sendSystemMessage(Component.literal("A Warrior Chicken has become your Nemesis (Rank "
						+ PurgeUnits.nemesisRank(chicken) + "). It grows stronger after each kill."));
			}
			if (data.bossSpawned()) {
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, true, false));
				level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
						player.getGameProfile().name() + " fell during the final battle. The Purge has failed."), false);
				fail(level.getServer(), data);
				return;
			}
		}
		if (data.isActive() && BossController.isAerialBoss(dead)) {
			PurgeUnits.discardPassengers(dead);
			if (killer instanceof ServerPlayer player) {
				data.addContribution(player.getUUID().toString(), 20);
				player.sendSystemMessage(Component.literal("Sky Overlord down! +20 contribution; the bomber's warhorn is yours."));
			}
			PurgeUnits.drop(level, dead.position(), ModItems.FLOCKWAR_HORN, 1);
		}
		if (data.isActive() && killer instanceof ServerPlayer player && BossController.isGenerator(dead)) {
			data.addContribution(player.getUUID().toString(), 5);
			player.sendSystemMessage(Component.literal("Shield generator destroyed: +5 Purge contribution."));
		}
		if (data.isActive() && killer instanceof ServerPlayer player
				&& dead.getCustomName() != null && dead.getCustomName().getString().equals("Outpost Guard")) {
			PurgeUnits.clearOutpostSquads(level, dead.position());
			data.addContribution(player.getUUID().toString(), 5);
			PurgeUnits.drop(level, dead.position(), ModItems.SCRAP_CIRCUIT, 2);
			player.sendSystemMessage(Component.literal("Outpost cleared: nearby Purge squads scattered; +5 contribution."));
		}
		if (PurgeUnits.isNemesis(dead) && killer instanceof ServerPlayer player) {
			String playerId = player.getUUID().toString();
			data.recordNemesisKill(playerId);
			ItemStack trophy = new ItemStack(ModItems.NEMESIS_TROPHY);
			trophy.set(DataComponents.CUSTOM_NAME, Component.literal(
					dead.getCustomName().getString() + " Trophy"));
			trophy.set(DataComponents.LORE, new ItemLore(List.of(
					Component.literal("Nemesis Rank: " + PurgeUnits.nemesisRank(dead)),
					Component.literal("Nemeses Defeated: " + data.nemesisKills(playerId)))));
			level.addFreshEntity(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(), trophy));
			award(player, "nemesis_down", "nemesis");
			if (data.isActive()) {
				data.addContribution(playerId, 5);
			}
		}
		if (!data.isActive()) {
			return;
		}
		if (PurgeUnits.isFinalBoss(dead)) {
			if (PurgeUnits.isBoss(dead) && level.getRandom().nextFloat() < 0.1F) {
				PurgeUnits.drop(level, dead.position(), ModItems.LASSO, 1);
			}
			boolean anotherBossAlive = false;
			for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
				for (Entity entity : serverLevel.getAllEntities()) {
					if (entity != dead && entity.isAlive() && PurgeUnits.isFinalBoss(entity)) {
						anotherBossAlive = true;
						break;
					}
				}
				if (anotherBossAlive) {
					break;
				}
			}
			if (!anotherBossAlive) {
				win(level.getServer(), data, dead);
			}
			return;
		}
		if (!PurgeUnits.isRewardedUnit(dead)) {
			return;
		}

		PurgeUnits.dropSalvage(dead, level, data.threat(), data.dominion());
		if (killer instanceof ServerPlayer player) {
			data.addContribution(player.getUUID().toString(),
					dead.getCustomName() != null && dead.getCustomName().getString().equals("Outpost Guard") ? 5 : 1);
			if (data.rank(player.getUUID().toString()) >= 1 && level.getRandom().nextFloat() < 0.05F) {
				PurgeUnits.drop(level, dead.position(), ModItems.WAR_FEATHER, 1);
			}
			String role = dead.getCustomName() == null ? "scavenger" : dead.getCustomName().getString().toLowerCase();
			if (role.contains("gunship")) {
				role = "gunship";
			} else if (role.contains("drone")) {
				role = "drone";
			} else if (role.contains("medic")) {
				role = "medic";
			} else if (role.contains("riot")) {
				role = "riot";
			} else if (role.contains("ravager")) {
				role = "tank";
			} else if (role.contains("walker")) {
				role = "walker";
			} else if (role.contains("general")) {
				role = "general";
			} else if (role.contains("grenadier")) {
				role = "grenadier";
			} else if (role.contains("rpg")) {
				role = "rpg";
			} else if (role.contains("sniper")) {
				role = "sniper";
			} else if (role.contains("sergeant")) {
				role = "sergeant";
			} else if (role.contains("militia")) {
				role = "militia";
			} else {
				role = "scavenger";
			}
			if (data.learnBlueprint(player.getUUID().toString(), role)) {
				player.sendSystemMessage(Component.literal("Blueprint learned: " + role + " chicken."));
				unlockBlueprintRecipes(player, role);
				if (role.equals("walker")) {
					player.sendSystemMessage(Component.literal(
							"Walker blueprint learned: the Flockguard Sigil recipe is now available."));
				}
			}
			player.giveExperiencePoints(2 + data.threat());
		}
	}

	private static void unlockBlueprintRecipes(ServerPlayer player, String blueprint) {
		String[] recipes = switch (blueprint) {
			case "rpg" -> new String[]{"captured_peckzooka", "dud_rocket"};
			case "grenadier" -> new String[]{"egg_cannon", "flame_fryer", "gunpowder_pouch"};
			case "sniper" -> new String[]{"sniper_beak"};
			case "drone", "gunship" -> new String[]{"flak_cannon", "emp_disruptor"};
			case "medic" -> new String[]{"medic_syringe"};
			case "riot" -> new String[]{"cluck_shield"};
			case "tank" -> new String[]{"bunker_block"};
			case "walker" -> new String[]{"bunker_block", "flockguard_sigil"};
			case "general" -> new String[]{"cluck_radar", "signal_scrambler"};
			case "sergeant" -> new String[]{"rooster_spear", "feather_cloak"};
			case "militia" -> new String[]{"beak_47"};
			case "scavenger" -> new String[]{"scarecrow_turret"};
			default -> new String[0];
		};
		for (String recipe : recipes) {
			player.awardRecipesByKey(List.of(ResourceKey.create(
					Registries.RECIPE, BattleChickens.id(recipe))));
		}
	}

	private static void win(MinecraftServer server, PurgeWorldData data, LivingEntity boss) {
		if (!data.isActive()) {
			return;
		}
		data.stop(false, BattleChickens.CONFIG.dominionEnabled(), server.overworld().getGameTime(),
				BattleChickens.CONFIG.eventCooldownSeconds());
		data.restoreBossWeather(server.getAllLevels());
		ServerLevel level = (ServerLevel) boss.level();
		PurgeUnits.drop(level, boss.position(), ModItems.OVERLORD_SHARD, 3 + data.threat());
		if (level.getRandom().nextFloat() < 0.3F) {
			PurgeUnits.drop(level, boss.position(), ModItems.GOLDEN_EGG, 1);
		}
		for (ServerLevel serverLevel : server.getAllLevels()) {
			PurgeUnits.markSurvivors(serverLevel, server.getPlayerList().getPlayers());
		}
		for (ServerLevel serverLevel : server.getAllLevels()) {
			PurgeUnits.clearEventEntities(serverLevel, server.getPlayerList().getPlayers(), true);
		}
		int highestContribution = data.highestContribution();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int previousRank = data.rank(player.getUUID().toString());
			int rank = data.grantVictory(player.getUUID().toString(), data.threat());
			for (int unlockedRank = previousRank + 1; unlockedRank <= rank; unlockedRank++) {
				reward(player, rankBanner(unlockedRank));
			}
			int score = data.contribution(player.getUUID().toString());
			double share = highestContribution == 0 ? 1.0 : Math.max(0.25, (double) score / highestContribution);
			int shardReward = Math.max(1, (int) Math.ceil(
					(3 + data.threat()) * share * BattleChickens.CONFIG.rewardMultiplier()));
			reward(player, ModItems.OVERLORD_CORE);
			reward(player, ModItems.FLOCKWAR_HORN);
			if (score > 0 || highestContribution == 0) {
				reward(player, ModItems.OVERLORD_CROWN);
			}
			reward(player, ModItems.OVERLORD_SHARD, shardReward);
			player.giveExperiencePoints(100 + data.threat() * 50);
			award(player, "flockbreaker", "victory");
			if (data.deaths(player.getUUID().toString()) == 0) {
				award(player, "untouchable", "untouchable");
			}
			player.sendSystemMessage(Component.literal("Victory! Your title is " + rankTitle(rank)
					+ " (Flockbreaker rank " + rank + "); contribution score " + score + "."));
			if (rank >= 3 && !data.veteranTraderSpawned() && spawnVeteranTrader(player)) {
				data.markVeteranTraderSpawned();
			}
		}
		server.getPlayerList().broadcastSystemMessage(Component.literal(
				"The Overlord falls. The flock is broken, and the Purge is over."), false);
		clearBossBar();
	}

	private static void fail(MinecraftServer server, PurgeWorldData data) {
		data.stop(true, BattleChickens.CONFIG.dominionEnabled(), server.overworld().getGameTime(),
				BattleChickens.CONFIG.eventCooldownSeconds());
		data.restoreBossWeather(server.getAllLevels());
		clearBossBar();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, true, false));
		}
		for (ServerLevel level : server.getAllLevels()) {
			PurgeUnits.triggerMeltdown(level, server.getPlayerList().getPlayers());
			PurgeUnits.clearEventEntities(level, server.getPlayerList().getPlayers(), true);
		}

		server.getPlayerList().broadcastSystemMessage(Component.literal(data.dominion()
				? "The meltdown erupts. Chicken Dominion empowers the flock for your next attempt."
				: "The meltdown erupts. The Purge has ended in defeat."), false);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.sendSystemMessage(Component.literal("Your collected salvage and blueprints are kept. Retry for a 10% salvage bonus."));
		}
	}

	private static boolean hasPlayerInBossArena(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (!player.isAlive()) {
				continue;
			}
			if (!player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(256),
					entity -> PurgeUnits.isBoss(entity) || BossController.isAerialBoss(entity)).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static void reward(ServerPlayer player, net.minecraft.world.item.Item item) {
		reward(player, item, 1);
	}

	private static void reward(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
		ItemStack stack = new ItemStack(item, count);
		reward(player, stack);
	}

	private static void reward(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
		}
	}

	private static ItemStack rankBanner(int rank) {
		net.minecraft.world.item.Item banner = switch (rank) {
			case 1 -> Items.BANNER.yellow();
			case 2 -> Items.BANNER.red();
			case 3 -> Items.BANNER.blue();
			case 4 -> Items.BANNER.black();
			default -> Items.BANNER.purple();
		};
		ItemStack stack = new ItemStack(banner);
		stack.set(DataComponents.CUSTOM_NAME,
				Component.literal(rankTitle(rank) + " Banner"));
		return stack;
	}

	private static boolean spawnVeteranTrader(ServerPlayer player) {
		ServerLevel level = player.level();
		VeteranRooster trader = ModEntities.VETERAN_ROOSTER.create(level, EntitySpawnReason.EVENT);
		if (trader == null) {
			return false;
		}
		trader.setPos(player.getX() + 3, player.getY(), player.getZ() + 3);
		trader.setPersistenceRequired();
		trader.setCustomName(Component.literal("Veteran Rooster"));
		trader.setCustomNameVisible(true);
		trader.getOffers().add(new MerchantOffer(new ItemCost(ModItems.WAR_FEATHER, 8),
				new ItemStack(ModItems.DUD_ROCKET, 2), 16, 5, 0.05F));
		trader.getOffers().add(new MerchantOffer(new ItemCost(ModItems.GOLDEN_FEATHER, 4),
				new ItemStack(ModItems.SCRAP_CIRCUIT, 3), 8, 8, 0.05F));
		trader.getOffers().add(new MerchantOffer(new ItemCost(ModItems.WAR_FEATHER, 16),
				new ItemStack(ModItems.GENERALS_PLUME), 4, 12, 0.05F));
		trader.getOffers().add(new MerchantOffer(new ItemCost(ModItems.WAR_FEATHER, 12),
				java.util.Optional.of(new ItemCost(ModItems.SCRAP_PLATE, 8)),
				new ItemStack(ModItems.LASSO), 1, 16, 0.05F));
		return level.addFreshEntity(trader);
	}

	public static void award(ServerPlayer player, String advancementId, String criterion) {
		var advancement = player.level().getServer().getAdvancements().get(BattleChickens.id(advancementId));
		if (advancement == null) {
			BattleChickens.LOGGER.error("Missing advancement battle-chickens:{}", advancementId);
			return;
		}
		if (!player.getAdvancements().award(advancement, criterion)) {
			return;
		}
		player.giveExperiencePoints(5);
		if (advancementId.equals("flockbreaker")) {
			reward(player, ModItems.RATIONS_CRATE);
			player.sendSystemMessage(Component.literal("Flockbreaker achievement reward: a Rations Crate."));
		}
	}

	private static int showStatus(net.minecraft.commands.CommandSourceStack source) {
		PurgeWorldData data = PurgeWorldData.get(source.getLevel());
		String status = !data.isActive()
				? "No Purge is active."
				: "Purge Threat " + data.threat() + ", Day " + data.currentDay() + " of " + data.eventDays() + ".";
		if (data.dominion()) {
			status += " Chicken Dominion is active.";
		}
		String message = status;
		source.sendSuccess(() -> Component.literal(message), false);
		return 1;
	}

	private static int showRank(ServerPlayer player) {
		PurgeWorldData data = PurgeWorldData.get(player.level());
		int rank = data.rank(player.getUUID().toString());
		player.sendSystemMessage(Component.literal("Title: " + rankTitle(rank) + " (Flockbreaker rank "
				+ rank + " / 5)."));
		return 1;
	}

	private static String rankTitle(int rank) {
		return switch (rank) {
			case 1 -> "Egg Scrambler";
			case 2 -> "Coop Crusher";
			case 3 -> "Flockbreaker";
			case 4 -> "Warlord Slayer";
			case 5 -> "Overlord of Overlords";
			default -> "Purge Recruit";
		};
	}

	private static int showContribution(ServerPlayer player) {
		PurgeWorldData data = PurgeWorldData.get(player.level());
		player.sendSystemMessage(Component.literal("Current Purge contribution score: "
				+ data.contribution(player.getUUID().toString()) + ". Boss damage counts per point of damage."));
		return 1;
	}

	private static int showBlueprints(ServerPlayer player) {
		List<String> blueprints = PurgeWorldData.get(player.level()).blueprints(player.getUUID().toString());
		player.sendSystemMessage(Component.literal(blueprints.isEmpty()
				? "You have not learned any chicken blueprints yet."
				: "Learned chicken blueprints: " + String.join(", ", blueprints) + "."));
		return 1;
	}

	private static int debugDay(net.minecraft.commands.CommandSourceStack source, int day) {
		PurgeWorldData data = PurgeWorldData.get(source.getLevel());
		if (!data.isActive()) {
			source.sendFailure(Component.literal("Cannot jump days because no Purge is active."));
			return 0;
		}
		if (!data.jumpToDay(day)) {
			source.sendFailure(Component.literal("Day must be between 1 and " + data.eventDays() + "."));
			return 0;
		}
		displayedDay = day;
		updateBossBar(source.getServer(), data);
		source.sendSuccess(() -> Component.literal("Jumped to Purge Day " + day + " of "
				+ data.eventDays() + "."), false);
		return 1;
	}

	private static int stop(ServerLevel level) {
		PurgeWorldData data = PurgeWorldData.get(level);
		if (!data.isActive()) {
			return 0;
		}
		data.stop(false, BattleChickens.CONFIG.dominionEnabled(), level.getServer().overworld().getGameTime(),
				BattleChickens.CONFIG.eventCooldownSeconds());
		data.restoreBossWeather(level.getServer().getAllLevels());
		clearBossBar();
		for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
			PurgeUnits.clearEventEntities(serverLevel, level.getServer().getPlayerList().getPlayers(), true);
		}
		level.getServer().getPlayerList().broadcastSystemMessage(
				Component.literal("The Purge has been stopped by an administrator."), false);
		return 1;
	}

	private static void updateBossBar(MinecraftServer server, PurgeWorldData data) {
		if (bossBarServer != null && bossBarServer != server) {
			clearBossBar();
		}
		if (data.bossSpawned()) {
			if (bossBar != null) {
				bossBar.removeAllPlayers();
			}
			updateBossHealthBars(server);
			return;
		}
		clearBossHealthBars();
		if (bossBar == null) {
			bossBar = new ServerBossEvent(UUID.randomUUID(), Component.empty(),
					BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
			bossBarServer = server;
		}
		bossBar.setName(Component.literal("The Purge - Day " + data.currentDay() + " / " + data.eventDays()));
		bossBar.setProgress(data.progress());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!bossBar.getPlayers().contains(player)) {
				bossBar.addPlayer(player);
			}
		}
	}

	private static void updateBossHealthBars(MinecraftServer server) {
		RavagerHealth ravagerHealth = null;
		RavagerHealth aerialHealth = null;
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity living && PurgeUnits.isBoss(entity)) {
					List<? extends LivingEntity> generators = BossController.shieldGenerators(entity);
					int resistance = BossController.shieldResistanceAmplifier(generators);
					float shieldHealth = 0;
					float shieldMax = 0;
					if (!generators.isEmpty()) {
						LivingEntity shield = generators.getFirst();
						shieldHealth = shield.getHealth();
						shieldMax = shield.getMaxHealth();
					}
					ravagerHealth = new RavagerHealth(living.getHealth(), living.getMaxHealth(),
							resistance, shieldHealth, shieldMax);
				} else if (entity instanceof LivingEntity living && BossController.isAerialBoss(entity)) {
					aerialHealth = new RavagerHealth(living.getHealth(), living.getMaxHealth(), -1, 0, 0);
				}
			}
		}
		if (ravagerHealth != null) {
			if (overlordHealthBar == null) {
				overlordHealthBar = new ServerBossEvent(UUID.randomUUID(), Component.empty(),
						BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
			}
			String shield = ravagerHealth.resistance() < 0 ? ""
					: " [SHIELD: " + (int) Math.ceil(100.0 * ravagerHealth.shieldHealth()
					/ Math.max(1.0F, ravagerHealth.shieldMaxHealth()))
					+ "% - Res " + BossController.toRoman(ravagerHealth.resistance() + 1) + "]";
			overlordHealthBar.setName(Component.literal("The Overlord" + shield + " - "
					+ Math.ceil(ravagerHealth.health()) + " / " + Math.ceil(ravagerHealth.maxHealth())));
			overlordHealthBar.setProgress(progress(ravagerHealth.health(), ravagerHealth.maxHealth()));
			addPlayers(overlordHealthBar, server);
		} else if (overlordHealthBar != null) {
			overlordHealthBar.removeAllPlayers();
			overlordHealthBar = null;
		}
		if (aerialHealth != null) {
			if (aerialHealthBar == null) {
				aerialHealthBar = new ServerBossEvent(UUID.randomUUID(), Component.empty(),
						BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
			}
			aerialHealthBar.setName(Component.literal("Sky Overlord - "
					+ Math.ceil(aerialHealth.health()) + " / " + Math.ceil(aerialHealth.maxHealth())));
			aerialHealthBar.setProgress(progress(aerialHealth.health(), aerialHealth.maxHealth()));
			addPlayers(aerialHealthBar, server);
		} else if (aerialHealthBar != null) {
			aerialHealthBar.removeAllPlayers();
			aerialHealthBar = null;
		}
	}

	private record RavagerHealth(float health, float maxHealth, int resistance,
			float shieldHealth, float shieldMaxHealth) {
	}

	private static float progress(float health, float maxHealth) {
		return maxHealth <= 0 ? 0 : Math.max(0, Math.min(1, health / maxHealth));
	}

	private static void addPlayers(ServerBossEvent event, MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!event.getPlayers().contains(player)) {
				event.addPlayer(player);
			}
		}
	}

	private static void clearBossHealthBars() {
		if (overlordHealthBar != null) {
			overlordHealthBar.removeAllPlayers();
			overlordHealthBar = null;
		}
		if (aerialHealthBar != null) {
			aerialHealthBar.removeAllPlayers();
			aerialHealthBar = null;
		}
	}

	private static void clearBossBar() {
		if (bossBar != null) {
			bossBar.removeAllPlayers();
			bossBar = null;
		}
		clearBossHealthBars();
		bossBarServer = null;
		displayedDay = 0;
	}
}
