package com.cluck.event;

import com.cluck.BattleChickens;
import com.cluck.entity.ModEntities;
import com.cluck.item.ModItems;
import com.cluck.projectile.PurgeRocketEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class BossController {
	private static final String BOSS_NAME = "The Overlord";
	private static final String AERIAL_BOSS_NAME = "The Sky Overlord";
	private static final String GENERATOR_NAME = "Overlord Shield Generator";
	private static final Map<UUID, Vec3> ORBITAL_MARKS = new HashMap<>();
	private static final Map<UUID, List<MarkedBomb>> BOMB_MARKS = new HashMap<>();
	private static final Map<UUID, LightningChase> LIGHTNING_MARKS = new HashMap<>();
	private static final Map<UUID, SpinAttack> SPIN_ATTACKS = new HashMap<>();
	private static final Map<UUID, List<FlyingBossBomb>> FLYING_BOMBS = new HashMap<>();
	private static final Map<UUID, PursuitState> PURSUIT_STATES = new HashMap<>();
	private static final Map<UUID, RavagerCharge> RAVAGER_CHARGES = new HashMap<>();
	private static final Map<UUID, RavagerJump> RAVAGER_JUMPS = new HashMap<>();
	private static final Map<UUID, SonicBoom> SONIC_BOOMS = new HashMap<>();
	private static final Map<UUID, Integer> GENERATOR_PULSES = new HashMap<>();
	private static final Map<UUID, List<SoulFlameZone>> SOUL_FLAME_ZONES = new HashMap<>();
	private static final Map<UUID, Integer> PHANTOM_BITE_COOLDOWNS = new HashMap<>();

	private record SoulFlameZone(Vec3 position, long expiresAt, long nextDamageAt) {
	}

	private static final class RavagerCharge {
		private final Vec3 aim;
		private final int launchAt;
		private final Set<UUID> hitEntities = new HashSet<>();
		private Vec3 direction;

		private RavagerCharge(Vec3 aim, int launchAt) {
			this.aim = aim;
			this.launchAt = launchAt;
		}
	}

	private record RavagerJump(Vec3 landing, int launchAt, int impactAt, boolean launched) {
	}

	private record SonicBoom(UUID target, int fireAt) {
	}

	private record MarkedBomb(Vec3 position, int detonationTick) {
	}

	private record LightningChase(UUID target, int strikeTick) {
	}

	private record SpinAttack(int endTick, int nextWaveTick, boolean megaBomb, boolean launchedMega) {
	}

	private record FlyingBossBomb(PrimedTnt entity, int detonationTick, double radius, float damage,
			boolean mega) {
	}

	private static final class PursuitState {
		private Vec3 lastPosition;
		private int stalledTicks;
		private int lastTeleportTick = Integer.MIN_VALUE / 2;

		private PursuitState(Vec3 position) {
			lastPosition = position;
		}
	}

	private BossController() {
	}

	public static void spawnShieldGenerators(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		for (int index = 0; index < 4; index++) {
			double angle = Math.PI / 2 * index;
			double x = boss.getX() + Math.cos(angle) * 7;
			double z = boss.getZ() + Math.sin(angle) * 7;
			int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(int) x, (int) z);
			IronGolem generator = ModEntities.SHIELD_GENERATOR_GOLEM.create(level, EntitySpawnReason.EVENT);
			if (generator == null) {
				continue;
			}
			generator.setPos(x, y, z);
			generator.setPersistenceRequired();
			generator.setCustomName(Component.literal(GENERATOR_NAME));
			generator.setCustomNameVisible(true);
			generator.setTarget(target);
			double health = (220 + threat * 30) * BattleChickens.CONFIG.enemyHealthMultiplier();
			generator.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			generator.setHealth((float) health);
			if (level.addFreshEntity(generator)) {
				generator.setLeashedTo(boss, true);
			}
		}
	}

	public static void tick(ServerLevel level, List<ServerPlayer> players, int threat) {
		List<Entity> loadedEntities = new ArrayList<>();
		level.getAllEntities().forEach(loadedEntities::add);
		for (Entity entity : loadedEntities) {
			if (entity instanceof Ravager boss && hasName(boss, BOSS_NAME)) {
				tickBoss(level, boss, players, threat);
			}
			if (entity instanceof Phantom boss && isAerialBoss(boss)) {
				tickAerialBoss(level, boss, players, threat);
			}
		}
	}

	private static void tickAerialBoss(ServerLevel level, Phantom boss, List<ServerPlayer> players, int threat) {
		if (boss.tickCount % 40 == 0 || !boss.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			boss.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
		}
		boss.clearFire();
		ServerPlayer target = players.stream()
				.filter(player -> player.level() == level && player.isAlive())
				.min((first, second) -> Double.compare(boss.distanceToSqr(first), boss.distanceToSqr(second)))
				.orElse(null);
		if (target == null) {
			return;
		}
		boss.setTarget(target);
		Vec3 toTarget = target.getBoundingBox().getCenter().subtract(boss.getBoundingBox().getCenter());
		double distanceSquared = toTarget.lengthSqr();
		Vec3 direction = toTarget.normalize();
		if (distanceSquared > 4 * 4 && distanceSquared < 128 * 128) {
			Vec3 momentum = boss.getDeltaMovement().scale(0.65).add(direction.scale(0.035));
			if (momentum.lengthSqr() > 0.28 * 0.28) {
				momentum = momentum.normalize().scale(0.28);
			}
			boss.setDeltaMovement(momentum);
		} else if (distanceSquared >= 128 * 128) {
			boss.setTarget(null);
			boss.setDeltaMovement(boss.getDeltaMovement().scale(0.7));
		}
		boss.setYRot((float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI));
		boss.setYBodyRot(boss.getYRot());
		int biteCooldown = PHANTOM_BITE_COOLDOWNS.getOrDefault(boss.getUUID(), 0);
		if (distanceSquared <= 6.5 * 6.5 && boss.tickCount >= biteCooldown) {
			target.hurtServer(level, level.damageSources().mobAttack(boss),
					(12 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier());
			Vec3 biteKnockback = target.position().subtract(boss.position()).normalize();
			applyDaySevenKnockback(target, biteKnockback.scale(0.2).add(0, 0.12, 0));
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(),
					8, 0.3, 0.4, 0.3, 0.08);
			PHANTOM_BITE_COOLDOWNS.put(boss.getUUID(), boss.tickCount + 20);
		}
		if (boss.tickCount % 100 == 0) {
			showCombatAlert(target, Component.literal("Sky Overlord: rocket barrage — break line of sight!"));
			fireRocket(level, boss, target, -0.5);
			fireRocket(level, boss, target, 0.0);
			fireRocket(level, boss, target, 0.5);
			level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY(), boss.getZ(),
					18, 1.0, 0.5, 1.0, 0.08);
		}
		List<MarkedBomb> bombs = BOMB_MARKS.get(boss.getUUID());
		if ((bombs == null || bombs.isEmpty()) && boss.tickCount % 200 == 0) {
			List<MarkedBomb> barrage = new java.util.ArrayList<>();
			for (int index = -1; index <= 1; index++) {
				Vec3 impact = target.position().add(index * 7.0,
						0, level.getRandom().nextInt(7) - 3);
				barrage.add(new MarkedBomb(impact, boss.tickCount + 40));
				PurgeUnits.throwBossGrenade(level, boss, impact);
			}
			BOMB_MARKS.put(boss.getUUID(), barrage);
			showCombatAlert(target, Component.literal("Phantom bombardment — leave the marked zones!"));
			bombs = barrage;
		}
		if (bombs != null) {
			for (MarkedBomb bomb : bombs) {
				emitMark(level, bomb.position(), 5.5);
			}
			if (!bombs.isEmpty() && boss.tickCount >= bombs.getFirst().detonationTick()) {
				for (MarkedBomb bomb : bombs) {
					PurgeUnits.detonateBossGrenade(level, boss, bomb.position(), threat);
				}
				BOMB_MARKS.remove(boss.getUUID());
			}
		}
		if (boss.tickCount % 280 == 0) {
			showCombatAlert(target, Component.literal("Sky Overlord diving!"));
			Vec3 dive = target.position().subtract(boss.position()).normalize();
			boss.setDeltaMovement(dive.scale(0.42).add(0, -0.08, 0));
			if (boss.distanceToSqr(target) < 8 * 8) {
				target.hurtServer(level, level.damageSources().mobAttack(boss),
						(6 + threat) * BattleChickens.CONFIG.enemyDamageMultiplier());
			}
		}
		if (boss.tickCount % 800 == 0
				&& level.getEntitiesOfClass(IronGolem.class, boss.getBoundingBox().inflate(64),
				entity -> hasName(entity, "Sky Overlord's Enraged Golem")).isEmpty()) {
			throwEnragedGolem(level, boss, target, threat);
		}
		if (boss.tickCount % 400 == 0) {
			PurgeUnits.spawnWarlordGunner(level, boss, target, "Sky Gunner", true);
		}
		tickSpinAttack(level, boss, target, threat);
		tickFlyingBombs(level, boss, threat);
	}

	private static void tickSpinAttack(ServerLevel level, Phantom boss, ServerPlayer target, int threat) {
		SpinAttack attack = SPIN_ATTACKS.get(boss.getUUID());
		if (attack == null && boss.tickCount > 0 && boss.tickCount % 400 == 0) {
			attack = new SpinAttack(boss.tickCount + 60, boss.tickCount,
					level.getRandom().nextFloat() < 0.25F, false);
			SPIN_ATTACKS.put(boss.getUUID(), attack);
			showCombatAlert(target, Component.literal(attack.megaBomb()
					? "The Sky Overlord begins a spinning mega-bomb run! Run for cover!"
					: "The Sky Overlord spins up a massive TNT volley! Keep moving!"));
			level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP,
					net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.6F);
		}
		if (attack == null) {
			return;
		}
		boss.setYRot(boss.getYRot() + 36.0F);
		boss.setYBodyRot(boss.getYRot());
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY(), boss.getZ(),
				10, 1.5, 0.7, 1.5, 0.1);
		if (boss.tickCount >= attack.nextWaveTick() && boss.tickCount < attack.endTick()) {
			if (attack.megaBomb()) {
				if (!attack.launchedMega()) {
					throwBossBomb(level, boss, target.position(), 12.0, 1000.0F);
					attack = new SpinAttack(attack.endTick(), attack.endTick(), true, true);
				}
			} else {
				for (int index = 0; index < 4; index++) {
					double angle = Math.PI * 2 * index / 4 + level.getRandom().nextDouble() * 0.2;
					Vec3 aim = target.position().add(Math.cos(angle) * 10, 0, Math.sin(angle) * 10);
					throwBossBomb(level, boss, aim, 3.5, 16.0F + threat * 2.0F, false);
				}
				attack = new SpinAttack(attack.endTick(), attack.endTick(), false, false);
			}
			SPIN_ATTACKS.put(boss.getUUID(), attack);
		}
		if (boss.tickCount >= attack.endTick()) {
			SPIN_ATTACKS.remove(boss.getUUID());
		}
	}

	private static void throwEnragedGolem(ServerLevel level, Phantom boss, ServerPlayer target, int threat) {
		IronGolem golem = ModEntities.SHIELD_GENERATOR_GOLEM.create(level, EntitySpawnReason.EVENT);
		if (golem == null) {
			return;
		}
		golem.setPos(boss.getX(), boss.getY() - 1, boss.getZ());
		golem.setPersistenceRequired();
		golem.setCustomName(Component.literal("Sky Overlord's Enraged Golem"));
		golem.setCustomNameVisible(true);
		golem.setTarget(target);
		double health = (100 + threat * 20) * BattleChickens.CONFIG.enemyHealthMultiplier();
		golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		golem.setHealth((float) health);
		if (!level.addFreshEntity(golem)) {
			return;
		}
		Vec3 throwDirection = target.getBoundingBox().getCenter().subtract(golem.position()).normalize();
		golem.setDeltaMovement(throwDirection.scale(1.0).add(0, 0.65, 0));
		level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY(), boss.getZ(),
				24, 0.8, 0.5, 0.8, 0.12);
		showCombatAlert(target, Component.literal("Sky Overlord threw an Iron Golem!"));
	}

	private static void throwBossBomb(ServerLevel level, Phantom boss, Vec3 aim, double radius, float damage) {
		throwBossBomb(level, boss, aim, radius, damage, true);
	}

	private static void throwBossBomb(ServerLevel level, Phantom boss, Vec3 aim, double radius, float damage,
			boolean mega) {
		PrimedTnt bomb = EntityTypes.TNT.create(level, EntitySpawnReason.EVENT);
		if (bomb == null) {
			return;
		}
		bomb.setPos(boss.getX(), boss.getY(), boss.getZ());
		bomb.setFuse(1200);
		bomb.setCustomName(Component.literal(mega ? "Overlord Mega TNT" : "Overlord TNT"));
		bomb.setCustomNameVisible(true);
		Vec3 direction = aim.subtract(bomb.position()).normalize();
		bomb.setDeltaMovement(direction.scale(mega ? 0.48 : 0.65).add(0, mega ? 0.42 : 0.55, 0));
		if (level.addFreshEntity(bomb)) {
			FLYING_BOMBS.computeIfAbsent(boss.getUUID(), ignored -> new java.util.ArrayList<>())
					.add(new FlyingBossBomb(bomb, boss.tickCount + (mega ? 70 : 45), radius, damage, mega));
		}
	}

	private static void tickFlyingBombs(ServerLevel level, Phantom boss, int threat) {
		List<FlyingBossBomb> bombs = FLYING_BOMBS.get(boss.getUUID());
		if (bombs == null) {
			return;
		}
		bombs.removeIf(bomb -> {
			if (bomb.entity().isRemoved()) {
				return true;
			}
			if (boss.tickCount < bomb.detonationTick()) {
				level.sendParticles(ParticleTypes.SMOKE, bomb.entity().getX(), bomb.entity().getY(),
						bomb.entity().getZ(), 2, 0.1, 0.1, 0.1, 0.01);
				if (bomb.mega()) {
					emitMark(level, bomb.entity().position(), bomb.radius());
				}
				return false;
			}
			PurgeUnits.detonateBossTnt(level, boss, bomb.entity().position(), threat,
					bomb.radius(), bomb.damage());
			bomb.entity().discard();
			return true;
		});
		if (bombs.isEmpty()) {
			FLYING_BOMBS.remove(boss.getUUID());
		}
	}

	private static void emitMark(ServerLevel level, Vec3 center, double radius) {
		for (int step = 0; step < 20; step++) {
			double angle = Math.PI * 2 * step / 20;
			level.sendParticles(ParticleTypes.FLAME,
					center.x + Math.cos(angle) * radius, center.y + 0.1,
					center.z + Math.sin(angle) * radius, 2, 0, 0, 0, 0);
		}
	}

	private static void tickBoss(ServerLevel level, Ravager boss, List<ServerPlayer> players, int threat) {
		List<IronGolem> generators = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof IronGolem generator && hasName(generator, GENERATOR_NAME)) {
				generators.add(generator);
			}
		}
		boss.setPermanentlyInvulnerable(!generators.isEmpty());
		ServerPlayer target = players.stream()
				.filter(player -> player.level() == level && player.isAlive())
				.min((first, second) -> Double.compare(boss.distanceToSqr(first), boss.distanceToSqr(second)))
				.orElse(null);
		if (target == null) {
			return;
		}
		boss.setTarget(target);
		if (boss.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
			boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.78);
		}
		if (!RAVAGER_CHARGES.containsKey(boss.getUUID())
				&& !RAVAGER_JUMPS.containsKey(boss.getUUID())
				&& !SONIC_BOOMS.containsKey(boss.getUUID())
				&& boss.tickCount % 10 == 0) {
			boss.getNavigation().moveTo(target, 2.3);
		}
		if (boss.tickCount % 2 == 0) {
			breakPathBlocks(level, boss, target);
		}
		recoverStuckBoss(level, boss, target);
		tickGenerators(level, boss, generators, target, threat);
		tickRavagerMelee(level, boss, target, threat);
		tickRavagerCharge(level, boss, target);
		tickRavagerJump(level, boss, target, threat);
		tickSonicBoom(level, boss, target, threat);
		tickSoulFlameZones(level, boss, threat);
		tickLightningStrike(level, boss, target, threat);
		if (boss.tickCount % 120 == 0) {
			PurgeUnits.spawnRavagerMilitia(level, boss, target, threat);
		}
		if (boss.tickCount % 240 == 0) {
			PurgeUnits.spawnWarlordGunner(level, boss, target, "Overlord Gunner", false);
		}
		if (!generators.isEmpty()) {
			if (boss.tickCount % 60 == 0) {
				showCombatAlert(target, Component.literal(
						"The Overlord's shield is powered by " + generators.size() + " generators."));
			}
			if (boss.tickCount % 50 == 0 && boss.distanceToSqr(target) < 20 * 20) {
				target.hurtServer(level, level.damageSources().mobAttack(boss),
						(10 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier());
			}
			return;
		}

		float healthRatio = boss.getHealth() / Math.max(1, boss.getMaxHealth());
		if (healthRatio > 0.6F) {
			if (boss.tickCount % 100 == 70 && boss.distanceToSqr(target) < 24 * 24) {
				showCombatAlert(target, Component.literal("Ravager charging — dodge sideways!"));
				level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY() + 0.5,
						boss.getZ(), 16, 0.5, 0.2, 0.5, 0.02);
			}
			if (boss.tickCount % 100 == 0 && boss.distanceToSqr(target) < 24 * 24) {
				Vec3 charge = target.position().subtract(boss.position()).normalize();
				boss.setDeltaMovement(charge.scale(1.8).add(0, 0.18, 0));
				if (boss.distanceToSqr(target) < 12 * 12) {
					target.hurtServer(level, level.damageSources().mobAttack(boss),
							(14 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier());
				}
				showCombatAlert(target, Component.literal("Ravager charge!"));
			}
			if (boss.tickCount % 100 == 0) {
				fireRocket(level, boss, target, 0.35);
			}
		} else if (healthRatio > 0.3F) {
			if (boss.tickCount % 100 == 0) {
				showCombatAlert(target, Component.literal("Ravager rocket barrage — break line of sight!"));
				fireRocket(level, boss, target, 0.0);
				fireRocket(level, boss, target, 0.55);
				fireRocket(level, boss, target, -0.55);
			}
			if (boss.tickCount % 240 == 0) {
				PurgeUnits.spawnBossReinforcements(level, boss, target, threat);
			}
		} else {
			int orbitalTick = boss.tickCount % 120;
			if (orbitalTick == 0) {
				ORBITAL_MARKS.put(boss.getUUID(), target.position());
			}
			if (orbitalTick < 40) {
				Vec3 impact = ORBITAL_MARKS.computeIfAbsent(boss.getUUID(), ignored -> target.position());
				for (int step = 0; step < 24; step++) {
					double angle = Math.PI * 2 * step / 24;
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
							impact.x + Math.cos(angle) * 7, impact.y + 0.1,
							impact.z + Math.sin(angle) * 7, 1, 0, 0, 0, 0);
				}
				if (boss.tickCount % 120 == 0) {
					showCombatAlert(target, Component.literal("Orbital strike locked — leave the marked zone!"));
				}
			} else if (orbitalTick == 40) {
				Vec3 impact = ORBITAL_MARKS.getOrDefault(boss.getUUID(), target.position());
				level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1, impact.z, 1, 0, 0, 0, 0);
				for (ServerPlayer player : players) {
					if (player.level() == level && player.distanceToSqr(impact) < 49) {
						player.hurtServer(level, level.damageSources().mobAttack(boss),
								(40 + threat * 6) * BattleChickens.CONFIG.enemyDamageMultiplier()
										* PurgeUnits.explosiveDefenseMultiplier(player));
					}
				}
				PurgeUnits.destroyBossImpact(level, impact, boss);
				PurgeUnits.spawnBossReinforcements(level, boss, target, threat);
				ORBITAL_MARKS.remove(boss.getUUID());
			}
		}
	}

	private static void tickRavagerMelee(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		double reach = boss.getBbWidth() * 0.5 + 1.5;
		if (boss.distanceToSqr(target) <= reach * reach && boss.tickCount % 20 == 0) {
			target.hurtServer(level, level.damageSources().mobAttack(boss),
					(10 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier());
			Vec3 shove = target.position().subtract(boss.position()).normalize();
			applyDaySevenKnockback(target, shove.scale(0.45).add(0, 0.25, 0));
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(),
					14, 0.5, 0.4, 0.5, 0.08);
		}
	}

	private static void tickRavagerCharge(ServerLevel level, Ravager boss, ServerPlayer target) {
		RavagerCharge charge = RAVAGER_CHARGES.get(boss.getUUID());
		if (charge == null && boss.tickCount > 100 && boss.tickCount % 300 == 0
				&& !RAVAGER_JUMPS.containsKey(boss.getUUID())
				&& !SONIC_BOOMS.containsKey(boss.getUUID())) {
			charge = new RavagerCharge(target.position(), boss.tickCount + 100);
			RAVAGER_CHARGES.put(boss.getUUID(), charge);
			boss.getNavigation().stop();
			showCombatAlert(target, Component.literal(
					"The Overlord marks your position! It will charge there in five seconds!"));
			level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
					net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.65F);
		}
		if (charge == null) {
			return;
		}
		if (boss.tickCount < charge.launchAt) {
			level.sendParticles(ParticleTypes.FLAME, charge.aim.x, charge.aim.y + 0.1, charge.aim.z,
					5, 1.2, 0.1, 1.2, 0.01);
			return;
		}
		if (charge.direction == null) {
			charge.direction = charge.aim.subtract(boss.position()).normalize();
			if (charge.direction.lengthSqr() < 0.01) {
				RAVAGER_CHARGES.remove(boss.getUUID());
				return;
			}
			boss.getNavigation().stop();
		}
		boss.setDeltaMovement(charge.direction.scale(1.65));
		breakPathBlocks(level, boss, charge.direction);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(1.2),
				entity -> entity != boss && entity.isAlive()
						&& !(PurgeUnits.isPurgeAlly(entity) && !isAerialBoss(entity)))) {
			if (charge.hitEntities.add(victim.getUUID())) {
				victim.hurtServer(level, level.damageSources().mobAttack(boss),
						56 * BattleChickens.CONFIG.enemyDamageMultiplier());
				applyDaySevenKnockback(victim, charge.direction.scale(1.2).add(0, 0.5, 0));
			}
		}
		level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.5, boss.getZ(),
				12, 1.2, 0.5, 1.2, 0.08);
		if (boss.position().distanceToSqr(charge.aim) < 16 || boss.tickCount >= charge.launchAt + 60) {
			boss.setDeltaMovement(boss.getDeltaMovement().scale(0.2));
			RAVAGER_CHARGES.remove(boss.getUUID());
		}
	}

	private static void tickRavagerJump(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		RavagerJump jump = RAVAGER_JUMPS.get(boss.getUUID());
		if (jump == null && boss.tickCount > 100 && boss.tickCount % 420 == 0
				&& !RAVAGER_CHARGES.containsKey(boss.getUUID())
				&& !SONIC_BOOMS.containsKey(boss.getUUID())) {
			jump = new RavagerJump(target.position(), boss.tickCount + 30, boss.tickCount + 65, false);
			RAVAGER_JUMPS.put(boss.getUUID(), jump);
			boss.getNavigation().stop();
			showCombatAlert(target, Component.literal("Ravager leaping — leave the marked zone!"));
		}
		if (jump == null) {
			return;
		}
		if (!jump.launched() && boss.tickCount % 5 == 0) {
			for (int step = 0; step < 20; step++) {
				double angle = Math.PI * 2 * step / 20;
				level.sendParticles(ParticleTypes.CRIT, jump.landing().x + Math.cos(angle) * 5,
						jump.landing().y + 0.1, jump.landing().z + Math.sin(angle) * 5,
						1, 0, 0, 0, 0);
			}
		}
		if (!jump.launched() && boss.tickCount >= jump.launchAt()) {
			Vec3 landing = jump.landing();
			boss.setPos(landing.x, landing.y + 12, landing.z);
			boss.setDeltaMovement(0, -0.15, 0);
			jump = new RavagerJump(landing, jump.launchAt(), jump.impactAt(), true);
			RAVAGER_JUMPS.put(boss.getUUID(), jump);
		}
		if (jump.launched() && (boss.tickCount >= jump.impactAt()
				|| (boss.tickCount > jump.launchAt() + 5 && boss.onGround()))) {
			Vec3 landing = jump.landing();
			level.sendParticles(ParticleTypes.EXPLOSION, landing.x, landing.y + 0.5, landing.z,
					4, 1.5, 0.5, 1.5, 0.1);
			for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
					new AABB(landing, landing).inflate(6), ServerPlayer::isAlive)) {
				double distance = Math.sqrt(player.distanceToSqr(landing));
				player.hurtServer(level, level.damageSources().mobAttack(boss),
						(distance < 2 ? 55 : 30) * BattleChickens.CONFIG.enemyDamageMultiplier());
				Vec3 knockback = player.position().subtract(landing).normalize();
				applyDaySevenKnockback(player, knockback.scale(0.8).add(0, 0.8, 0));
			}
			RAVAGER_JUMPS.remove(boss.getUUID());
		}
	}

	private static void tickSonicBoom(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		SonicBoom attack = SONIC_BOOMS.get(boss.getUUID());
		if (attack == null && boss.tickCount > 100 && boss.tickCount % 240 == 0
				&& !RAVAGER_CHARGES.containsKey(boss.getUUID())
				&& !RAVAGER_JUMPS.containsKey(boss.getUUID())) {
			attack = new SonicBoom(target.getUUID(), boss.tickCount + 100);
			SONIC_BOOMS.put(boss.getUUID(), attack);
			showCombatAlert(target, Component.literal(
					"The Overlord channels a sonic boom! The blast will erupt in five seconds!"));
			level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE,
					net.minecraft.sounds.SoundSource.HOSTILE, 3.5F, 0.6F);
		}
		if (attack == null) {
			return;
		}
		UUID targetId = attack.target();
		ServerPlayer markedTarget = playersInLevel(level).stream()
				.filter(player -> player.getUUID().equals(targetId) && player.isAlive())
				.findFirst().orElse(target);
		Vec3 aim = markedTarget.getBoundingBox().getCenter();
		Vec3 direction = aim.subtract(boss.getEyePosition()).normalize();
		Vec3 origin = boss.getEyePosition().add(direction.scale(3.0));
		if (boss.tickCount < attack.fireAt()) {
			if (boss.tickCount % 5 == 0) {
				Vec3 telegraph = boss.getEyePosition();
				double length = Math.min(128, aim.distanceTo(telegraph));
				for (double distance = 3; distance < length; distance += 4) {
					Vec3 point = telegraph.add(direction.scale(distance));
					level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z,
							1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, aim.x, aim.y,
						aim.z, 6, 0.8, 0.3, 0.8, 0.02);
			}
			return;
		}
		double beamLength = Math.min(128, aim.distanceTo(origin));
		for (double distance = 0; distance <= beamLength; distance += 1.5) {
			Vec3 point = origin.add(direction.scale(distance));
			level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
		for (ServerPlayer player : playersInLevel(level)) {
			Vec3 relative = player.getBoundingBox().getCenter().subtract(origin);
			double along = relative.dot(direction);
			if (along < 0 || along > beamLength
					|| relative.subtract(direction.scale(along)).lengthSqr() > 4.0 * 4.0) {
				continue;
			}
			player.hurtServer(level, level.damageSources().mobAttack(boss),
					(35 + threat * 5) * BattleChickens.CONFIG.enemyDamageMultiplier());
			applyDaySevenKnockback(player, direction.scale(1.8).add(0, 0.35, 0));
		}
		level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
				net.minecraft.sounds.SoundSource.HOSTILE, 4.0F, 0.75F);
		SONIC_BOOMS.remove(boss.getUUID());
	}

	public static void applyDaySevenKnockback(LivingEntity entity, Vec3 impulse) {
		if (entity instanceof ServerPlayer player && entity.level() instanceof ServerLevel level
				&& PurgeWorldData.get(level).bossSpawned()) {
			player.setDeltaMovement(player.getDeltaMovement().add(impulse.scale(0.25)));
		} else {
			entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
		}
	}

	private static void tickGenerators(ServerLevel level, Ravager boss, List<IronGolem> generators,
			ServerPlayer target, int threat) {
		for (int index = 0; index < generators.size(); index++) {
			IronGolem generator = generators.get(index);
			if (!generator.isLeashed()) {
				generator.setLeashedTo(boss, true);
			}
			double angle = boss.tickCount * 0.01 + Math.PI * 2 * index / Math.max(1, generators.size());
			Vec3 formation = boss.position().add(Math.cos(angle) * 7, 0, Math.sin(angle) * 7);
			boolean obstructed = !level.noCollision(generator, generator.getBoundingBox());
			boolean farAway = generator.distanceToSqr(boss) > 18 * 18;
			boolean failedFormationRefresh = generator.tickCount % 400 == 0
					&& generator.distanceToSqr(formation) > 12 * 12;
			if (obstructed || farAway || failedFormationRefresh) {
				Vec3 recoveryPosition = findClearGeneratorPosition(level, generator, boss, index, generators.size());
				generator.teleportTo(recoveryPosition.x, recoveryPosition.y, recoveryPosition.z);
				generator.setDeltaMovement(Vec3.ZERO);
			} else if (generator.tickCount % 10 == 0) {
				generator.getNavigation().moveTo(formation.x, formation.y, formation.z, 1.3);
			}
			generator.setTarget(target);
			int nextPulse = GENERATOR_PULSES.computeIfAbsent(generator.getUUID(),
					ignored -> generator.tickCount + 70 + level.getRandom().nextInt(100));
			if (generator.tickCount >= nextPulse) {
				fireGeneratorPulse(level, generator, target, boss, threat);
				GENERATOR_PULSES.put(generator.getUUID(), generator.tickCount + 90 + level.getRandom().nextInt(100));
			}
		}
	}

	private static Vec3 findClearGeneratorPosition(ServerLevel level, IronGolem generator, Ravager boss,
			int index, int generatorCount) {
		for (int radius : new int[]{6, 8, 10}) {
			for (int step = 0; step < 16; step++) {
				double angle = Math.PI * 2 * (step + index / (double) Math.max(1, generatorCount)) / 16;
				double x = boss.getX() + Math.cos(angle) * radius;
				double z = boss.getZ() + Math.sin(angle) * radius;
				int blockX = (int) Math.floor(x);
				int blockZ = (int) Math.floor(z);
				int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						blockX, blockZ);
				double candidateX = blockX + 0.5;
				double candidateZ = blockZ + 0.5;
				var candidateBox = generator.getBoundingBox().move(
						candidateX - generator.getX(), y - generator.getY(), candidateZ - generator.getZ());
				if (level.noCollision(generator, candidateBox)) {
					return new Vec3(candidateX, y, candidateZ);
				}
			}
		}
		return boss.position().add(6, 0, 0);
	}

	private static void fireGeneratorPulse(ServerLevel level, IronGolem generator, ServerPlayer target,
			Ravager boss, int threat) {
		Vec3 start = generator.getEyePosition();
		Vec3 targetPosition = target.getBoundingBox().getCenter();
		BlockHitResult obstruction = level.clip(new net.minecraft.world.level.ClipContext(
				start, targetPosition, net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE, generator));
		boolean clearShot = obstruction.getType() == HitResult.Type.MISS;
		Vec3 end = clearShot ? targetPosition : obstruction.getLocation();
		Vec3 ray = end.subtract(start);
		double length = Math.min(64, ray.length());
		Vec3 direction = ray.normalize();
		for (double distance = 0; distance < length; distance += 0.75) {
			Vec3 point = start.add(direction.scale(distance));
			level.sendParticles(new DustParticleOptions(0xE32424, 1.5F), point.x, point.y, point.z,
					2, 0.03, 0.03, 0.03, 0);
		}
		if (clearShot && ray.length() <= 64) {
			target.hurtServer(level, level.damageSources().magic(),
					(16 + threat * 3) * BattleChickens.CONFIG.enemyDamageMultiplier());
			List<SoulFlameZone> zones = SOUL_FLAME_ZONES.computeIfAbsent(boss.getUUID(), ignored -> new ArrayList<>());
			if (zones.size() >= 4) {
				zones.removeFirst();
			}
			zones.add(new SoulFlameZone(target.position(), level.getGameTime() + 400, level.getGameTime()));
			showCombatAlert(target, Component.literal("Shield laser: soulflame at impact!"));
		} else if (!clearShot && ray.length() <= 64) {
			List<SoulFlameZone> zones = SOUL_FLAME_ZONES.computeIfAbsent(boss.getUUID(), ignored -> new ArrayList<>());
			if (zones.size() >= 4) {
				zones.removeFirst();
			}
			zones.add(new SoulFlameZone(end, level.getGameTime() + 400, level.getGameTime()));
		}
		level.playSound(null, generator.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
				net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.5F);
	}

	private static void tickSoulFlameZones(ServerLevel level, Ravager boss, int threat) {
		UUID bossId = boss.getUUID();
		List<SoulFlameZone> zones = SOUL_FLAME_ZONES.get(bossId);
		if (zones == null) {
			return;
		}
		for (int index = 0; index < zones.size(); index++) {
			SoulFlameZone zone = zones.get(index);
			if (boss.tickCount % 5 == 0) {
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, zone.position().x, zone.position().y + 0.1,
						zone.position().z, 5, 0.8, 0.3, 0.8, 0.02);
			}
			if (level.getGameTime() >= zone.nextDamageAt()) {
				AABB contactArea = new AABB(zone.position(), zone.position()).inflate(0.7, 0.8, 0.7);
				for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, contactArea,
						ServerPlayer::isAlive)) {
					float trueDamage = Math.max(1.0F, player.getMaxHealth() * 0.1F);
					player.hurtServer(level, level.damageSources().magic(), trueDamage);
				}
				zones.set(index, new SoulFlameZone(zone.position(), zone.expiresAt(), level.getGameTime() + 20));
			}
		}
		zones.removeIf(zone -> level.getGameTime() >= zone.expiresAt());
		if (zones.isEmpty()) {
			SOUL_FLAME_ZONES.remove(bossId);
		}
	}

	private static void breakPathBlocks(ServerLevel level, Ravager boss, ServerPlayer target) {
		breakPathBlocks(level, boss, target.position().subtract(boss.position()));
	}

	private static void breakPathBlocks(ServerLevel level, Ravager boss, Vec3 direction) {
		if (boss.tickCount % 5 != 0
				|| !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		if (horizontalLength < 0.01) {
			return;
		}
		double centerX = boss.getX() + direction.x / horizontalLength * 2.5;
		double centerZ = boss.getZ() + direction.z / horizontalLength * 2.5;
		int baseX = net.minecraft.core.BlockPos.containing(centerX, boss.getY(), centerZ).getX();
		int baseY = boss.blockPosition().getY();
		int baseZ = net.minecraft.core.BlockPos.containing(centerX, boss.getY(), centerZ).getZ();
		int height = Math.min(5, Math.max(2, (int) Math.ceil(boss.getBbHeight())));
		int broken = 0;
		for (int y = 0; y < height && broken < 9; y++) {
			for (int dx = -1; dx <= 1 && broken < 9; dx++) {
				for (int dz = -1; dz <= 1 && broken < 9; dz++) {
					net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(baseX + dx, baseY + y, baseZ + dz);
					var state = level.getBlockState(pos);
					if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) {
						continue;
					}
					if (level.destroyBlock(pos, false, boss)) {
						broken++;
					}
				}
			}
		}
	}

	private static void recoverStuckBoss(ServerLevel level, Ravager boss, ServerPlayer target) {
		PursuitState state = PURSUIT_STATES.computeIfAbsent(boss.getUUID(),
				ignored -> new PursuitState(boss.position()));
		if (state.lastPosition.distanceToSqr(boss.position()) < 0.2 * 0.2) {
			state.stalledTicks++;
		} else {
			state.stalledTicks = 0;
		}
		state.lastPosition = boss.position();
		if (state.stalledTicks < 100 || boss.distanceToSqr(target) < 64 * 64
				|| boss.tickCount - state.lastTeleportTick < 200) {
			return;
		}
		Vec3 awayFromTarget = boss.position().subtract(target.position()).normalize();
		int x = net.minecraft.core.BlockPos.containing(target.getX() + awayFromTarget.x * 18, target.getY(),
				target.getZ() + awayFromTarget.z * 18).getX();
		int z = net.minecraft.core.BlockPos.containing(target.getX() + awayFromTarget.x * 18, target.getY(),
				target.getZ() + awayFromTarget.z * 18).getZ();
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		boss.setPos(x + 0.5, y, z + 0.5);
		boss.setDeltaMovement(Vec3.ZERO);
		boss.getNavigation().stop();
		state.stalledTicks = 0;
		state.lastPosition = boss.position();
		state.lastTeleportTick = boss.tickCount;
	}

	private static void tickLightningStrike(ServerLevel level, Ravager boss, ServerPlayer target, int threat) {
		LightningChase strike = LIGHTNING_MARKS.get(boss.getUUID());
		if (strike == null && boss.tickCount % 200 == 0) {
			strike = new LightningChase(target.getUUID(), boss.tickCount + 30);
			LIGHTNING_MARKS.put(boss.getUUID(), strike);
			showCombatAlert(target, Component.literal("Lightning marked — leave the ring!"));
		}
		if (strike == null) {
			return;
		}
		UUID targetId = strike.target();
		ServerPlayer chasedPlayer = playersInLevel(level).stream()
				.filter(player -> player.getUUID().equals(targetId) && player.isAlive())
				.findFirst().orElse(target);
		Vec3 impact = chasedPlayer.position().add(chasedPlayer.getDeltaMovement().scale(6));
		emitLightningWarning(level, impact);
		if (boss.tickCount < strike.strikeTick()) {
			return;
		}
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(impact);
			level.addFreshEntity(bolt);
		}
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
				new AABB(impact, impact).inflate(2.0), ServerPlayer::isAlive)) {
			player.hurtServer(level, level.damageSources().mobAttack(boss),
					(12 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier()
							* PurgeUnits.explosiveDefenseMultiplier(player));
			Vec3 knockback = player.position().subtract(impact).normalize();
			applyDaySevenKnockback(player, knockback.scale(0.25).add(0, 0.2, 0));
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x,
				impact.y + 1, impact.z, 36, 1.5, 2.0, 1.5, 0.15);
		level.playSound(null, net.minecraft.core.BlockPos.containing(impact),
				net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
				net.minecraft.sounds.SoundSource.WEATHER, 2.0F, 1.3F);
		LIGHTNING_MARKS.remove(boss.getUUID());
	}

	private static void emitLightningWarning(ServerLevel level, Vec3 center) {
		for (int step = 0; step < 24; step++) {
			double angle = Math.PI * 2 * step / 24;
			double radius = 3.0;
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					center.x + Math.cos(angle) * radius, center.y + 0.1,
					center.z + Math.sin(angle) * radius, 2, 0, 0, 0, 0);
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.2, center.z,
				4, 0.35, 0.1, 0.35, 0.02);
	}

	private static List<ServerPlayer> playersInLevel(ServerLevel level) {
		return level.getServer().getPlayerList().getPlayers().stream()
				.filter(player -> player.level() == level).toList();
	}

	public static void showCombatAlert(ServerPlayer player, Component message) {
		player.sendSystemMessage(message, true);
	}

	private static void fireRocket(ServerLevel level, LivingEntity boss, ServerPlayer target, double spread) {
		Vec3 direction = target.getBoundingBox().getCenter().subtract(boss.getEyePosition());
		if (spread != 0) {
			Vec3 side = new Vec3(direction.z, 0, -direction.x).normalize().scale(spread);
			direction = direction.add(side);
		}
		level.addFreshEntity(new PurgeRocketEntity(level, boss, direction));
	}

	public static boolean isGenerator(Entity entity) {
		return hasName(entity, GENERATOR_NAME);
	}

	public static boolean isAerialBoss(Entity entity) {
		return hasName(entity, AERIAL_BOSS_NAME);
	}

	public static void forget(Entity entity) {
		if (entity == null) {
			return;
		}
		ORBITAL_MARKS.remove(entity.getUUID());
		BOMB_MARKS.remove(entity.getUUID());
		LIGHTNING_MARKS.remove(entity.getUUID());
		SPIN_ATTACKS.remove(entity.getUUID());
		PURSUIT_STATES.remove(entity.getUUID());
		RAVAGER_CHARGES.remove(entity.getUUID());
		RAVAGER_JUMPS.remove(entity.getUUID());
		SONIC_BOOMS.remove(entity.getUUID());
		GENERATOR_PULSES.remove(entity.getUUID());
		SOUL_FLAME_ZONES.remove(entity.getUUID());
		PHANTOM_BITE_COOLDOWNS.remove(entity.getUUID());
		List<FlyingBossBomb> bombs = FLYING_BOMBS.remove(entity.getUUID());
		if (bombs != null) {
			bombs.forEach(bomb -> bomb.entity().discard());
		}
	}

	private static boolean hasName(Entity entity, String name) {
		return entity != null && entity.getCustomName() != null && entity.getCustomName().getString().equals(name);
	}
}
