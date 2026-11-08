package com.cluck.event;

import com.cluck.BattleChickens;
import com.cluck.entity.OverlordBeast;
import com.cluck.entity.ModEntities;
import com.cluck.entity.ShieldGeneratorGolem;
import com.cluck.entity.EnragedGolem;
import com.cluck.entity.SkyOverlord;
import com.cluck.item.ModItems;
import com.cluck.projectile.PurgeRocketEntity;
import com.cluck.projectile.SkyOverlordWitherSkull;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Iterator;
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
	private static final Set<UUID> SONIC_BOOM_USED = new HashSet<>();
	private static final Map<UUID, SpinAttack> SPIN_ATTACKS = new HashMap<>();
	private static final Map<UUID, List<FlyingBossBomb>> FLYING_BOMBS = new HashMap<>();
	private static final Map<UUID, PursuitState> PURSUIT_STATES = new HashMap<>();
	private static final Map<UUID, RavagerCharge> RAVAGER_CHARGES = new HashMap<>();
	private static final Map<UUID, RavagerJump> RAVAGER_JUMPS = new HashMap<>();
	private static final Map<UUID, BoulderWarning> BOULDER_WARNINGS = new HashMap<>();
	private static final Map<UUID, Integer> BOULDER_COOLDOWNS = new HashMap<>();
	private static final Map<UUID, BoulderVolley> BOULDER_VOLLEYS = new HashMap<>();
	private static final Map<UUID, SonicBoom> SONIC_BOOMS = new HashMap<>();
	private static final Map<UUID, Integer> GENERATOR_PULSES = new HashMap<>();
	private static final Map<UUID, List<GeneratorPulse>> GENERATOR_PROJECTILES = new HashMap<>();
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

	private record BoulderWarning(Vec3 target, int launchAt) {
	}

	private static final class BoulderVolley {
		private final List<FallingBlockEntity> anvils;
		private final List<Vec3> impacts = new ArrayList<>();
		private int age;

		private BoulderVolley(List<FallingBlockEntity> anvils) {
			this.anvils = anvils;
		}
	}

	private record SonicBoom(Vec3 aim, int fireAt) {
	}

	private record GeneratorPulse(UUID target, Vec3 position, Vec3 direction, int age) {
	}

	private record MarkedBomb(int detonationTick, ItemEntity projectile) {
	}

	private record LightningChase(Vec3 impact, int strikeTick) {
	}

	private record SpinAttack(int endTick, int nextWaveTick, boolean megaBomb, boolean launchedMega) {
	}

	private record FlyingBossBomb(PrimedTnt entity, int detonationTick, double radius, float damage,
			boolean mega, boolean visualOnly) {
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

	public static void spawnShieldGenerators(ServerLevel level, OverlordBeast boss, ServerPlayer target, int threat) {
		double x = boss.getX() + 10;
		double z = boss.getZ();
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(int) x, (int) z);
		IronGolem generator = ModEntities.SHIELD_GENERATOR_GOLEM.create(level, EntitySpawnReason.EVENT);
		if (generator == null) {
			return;
		}
		generator.setPos(x, y, z);
		generator.setPersistenceRequired();
		generator.setCustomName(Component.literal(GENERATOR_NAME));
		generator.setCustomNameVisible(true);
		generator.setTarget(target);
		// One giant shield golem: 10x vanilla iron golem HP after the boss-health increase.
		double health = 1000.0 * BattleChickens.CONFIG.enemyHealthMultiplier();
		if (generator.getAttribute(Attributes.MAX_HEALTH) != null) {
			generator.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			generator.setHealth((float) health);
		}
		if (generator.getAttribute(Attributes.SCALE) != null) {
			generator.getAttribute(Attributes.SCALE).setBaseValue(3.0);
			generator.refreshDimensions();
		}
		level.addFreshEntity(generator);
	}

	public static void tick(ServerLevel level, List<ServerPlayer> players, int threat) {
		List<Entity> loadedEntities = new ArrayList<>();
		level.getAllEntities().forEach(loadedEntities::add);
		for (Entity entity : loadedEntities) {
			if (entity instanceof OverlordBeast boss && hasName(boss, BOSS_NAME)) {
				tickBoss(level, boss, players, threat);
			}
			if (entity instanceof SkyOverlord boss && isAerialBoss(boss)) {
				tickAerialBoss(level, boss, players, threat);
			}
		}
	}

	private static void tickAerialBoss(ServerLevel level, SkyOverlord boss, List<ServerPlayer> players, int threat) {
		if (boss.tickCount % 40 == 0 || !boss.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			boss.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false));
		}
		boss.clearFire();
		tickMarkedBombs(level, boss, threat);
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
			breakPhantomPathBlocks(level, boss, direction);
		} else if (distanceSquared >= 128 * 128) {
			boss.setTarget(null);
			boss.setDeltaMovement(boss.getDeltaMovement().scale(0.7));
		}
		boss.setYRot((float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI));
		boss.setYBodyRot(boss.getYRot());
		if (distanceSquared > 150 * 150) {
			teleportAerialBossNearPlayer(level, boss, target);
		}
		int biteCooldown = PHANTOM_BITE_COOLDOWNS.getOrDefault(boss.getUUID(), 0);
		Vec3 biteAim = PurgeUnits.bossAimPosition(level, target);
		if (distanceSquared <= 6.5 * 6.5 && boss.tickCount >= biteCooldown
				&& biteAim.distanceToSqr(target.getBoundingBox().getCenter()) <= 2.5 * 2.5) {
			if (target.hurtServer(level, level.damageSources().mobAttack(boss),
					(12 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier())) {
				Vec3 biteKnockback = target.position().subtract(boss.position()).normalize();
				applyDaySevenKnockback(target, biteKnockback.scale(0.2).add(0, 0.12, 0));
				level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(),
						8, 0.3, 0.4, 0.3, 0.08);
			}
			PHANTOM_BITE_COOLDOWNS.put(boss.getUUID(), boss.tickCount + 20);
		}
		if (boss.tickCount % 100 == 0) {
			if (PurgeUnits.hasClearAttackPath(level, boss, target)) {
				showCombatAlert(target, Component.literal("Sky Overlord: rocket barrage — break line of sight!"));
			}
			fireRocket(level, boss, target, -0.5);
			fireRocket(level, boss, target, 0.0);
			fireRocket(level, boss, target, 0.5);
			level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY(), boss.getZ(),
					18, 1.0, 0.5, 1.0, 0.08);
		}
		List<MarkedBomb> bombs = BOMB_MARKS.get(boss.getUUID());
		if ((bombs == null || bombs.isEmpty()) && boss.tickCount % 200 == 0
				&& PurgeUnits.hasClearAttackPath(level, boss, target)) {
			List<MarkedBomb> barrage = new java.util.ArrayList<>();
			for (int index = -1; index <= 1; index++) {
				Vec3 aim = target.position().add(aimOffset(level, target));
				Vec3 impact = aim.add(index * 7.0,
						0, level.getRandom().nextInt(7) - 3);
				barrage.add(new MarkedBomb(boss.tickCount + 40,
						PurgeUnits.throwBossGrenade(level, boss, impact)));
			}
			BOMB_MARKS.put(boss.getUUID(), barrage);
			showCombatAlert(target, Component.literal("Phantom bombardment — leave the marked zones!"));
			bombs = barrage;
		}
		if (boss.tickCount % 280 == 0) {
			showCombatAlert(target, Component.literal("Sky Overlord diving!"));
			Vec3 diveAim = target.position().add(aimOffset(level, target));
			Vec3 dive = diveAim.subtract(boss.position()).normalize();
			boss.setDeltaMovement(dive.scale(0.42).add(0, -0.08, 0));
			if (target.position().distanceToSqr(diveAim) < 6 * 6) {
				target.hurtServer(level, level.damageSources().mobAttack(boss),
						(6 + threat) * BattleChickens.CONFIG.enemyDamageMultiplier());
			}
			damageNearbyGenerators(level, boss, threat);
		}
		if (boss.tickCount % 160 == 0) {
			fireBlueWitherSkull(level, boss, target);
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

	private static void tickMarkedBombs(ServerLevel level, SkyOverlord boss, int threat) {
		List<MarkedBomb> bombs = BOMB_MARKS.get(boss.getUUID());
		if (bombs == null) {
			return;
		}
		if (!bombs.isEmpty() && boss.tickCount >= bombs.getFirst().detonationTick()) {
			for (MarkedBomb bomb : bombs) {
				if (bomb.projectile() != null) {
					PurgeUnits.detonateBossGrenade(level, boss, bomb.projectile().position(), threat);
					bomb.projectile().discard();
				}
			}
			BOMB_MARKS.remove(boss.getUUID());
		}
	}

	private static void fireBlueWitherSkull(ServerLevel level, SkyOverlord boss, ServerPlayer target) {
		Vec3 start = boss.getEyePosition();
		Vec3 aim = PurgeUnits.bossAimPosition(level, target).subtract(start);
		if (aim.lengthSqr() < 0.01) {
			return;
		}
		SkyOverlordWitherSkull skull = new SkyOverlordWitherSkull(level, boss, aim.normalize());
		skull.setOwner(boss);
		skull.setDangerous(true);
		skull.setPos(start.x, start.y, start.z);
		if (!level.addFreshEntity(skull)) {
			return;
		}
		level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SHOOT,
				net.minecraft.sounds.SoundSource.HOSTILE, 2.5F, 0.75F);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, start.x, start.y, start.z,
				18, 0.4, 0.4, 0.4, 0.05);
		showCombatAlert(target, Component.literal("Sky Overlord fires a blue wither skull!"));
	}

	private static void damageNearbyGenerators(ServerLevel level, SkyOverlord boss, int threat) {
		float contactDamage = (10 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier();
		for (IronGolem generator : level.getEntitiesOfClass(IronGolem.class, boss.getBoundingBox().inflate(6),
				BossController::isGenerator)) {
			if (generator.isAlive() && boss.distanceToSqr(generator) <= 5.5 * 5.5) {
				generator.hurtServer(level, level.damageSources().mobAttack(boss), contactDamage);
			}
		}
	}

	private static void tickSpinAttack(ServerLevel level, SkyOverlord boss, ServerPlayer target, int threat) {
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
					throwBossBomb(level, boss, target.position().add(aimOffset(level, target)),
							22.0, 500.0F);
					attack = new SpinAttack(attack.endTick(), attack.endTick(), true, true);
				}
			} else {
				for (int index = 0; index < 4; index++) {
					double angle = Math.PI * 2 * index / 4 + level.getRandom().nextDouble() * 0.2;
					Vec3 aim = target.position().add(aimOffset(level, target))
							.add(Math.cos(angle) * 10, 0, Math.sin(angle) * 10);
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

	private static void throwEnragedGolem(ServerLevel level, SkyOverlord boss, ServerPlayer target, int threat) {
		EnragedGolem golem = ModEntities.ENRAGED_GOLEM.create(level, EntitySpawnReason.EVENT);
		if (golem == null) {
			return;
		}
		golem.setPos(boss.getX(), boss.getY() - 1, boss.getZ());
		golem.setPersistenceRequired();
		golem.setCustomName(Component.literal("Sky Overlord's Enraged Golem"));
		golem.setCustomNameVisible(true);
		double health = (100 + threat * 20) * BattleChickens.CONFIG.enemyHealthMultiplier();
		golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		golem.setHealth((float) health);
		if (!level.addFreshEntity(golem)) {
			return;
		}
		golem.enrageOn(target);
		Vec3 throwDirection = PurgeUnits.bossAimPosition(level, target)
				.subtract(golem.position()).normalize();
		golem.setDeltaMovement(throwDirection.scale(1.0).add(0, 0.65, 0));
		level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY(), boss.getZ(),
				24, 0.8, 0.5, 0.8, 0.12);
		showCombatAlert(target, Component.literal("Sky Overlord threw an Iron Golem!"));
	}

	private static void throwBossBomb(ServerLevel level, SkyOverlord boss, Vec3 aim, double radius, float damage) {
		throwBossBomb(level, boss, aim, radius, damage, true);
	}

	private static void throwBossBomb(ServerLevel level, SkyOverlord boss, Vec3 aim, double radius, float damage,
			boolean mega) {
		Vec3 direction = aim.subtract(boss.position()).normalize();
		Vec3 side = new Vec3(direction.z, 0, -direction.x).normalize();
		if (side.lengthSqr() < 0.01) {
			side = new Vec3(1, 0, 0);
		}
		List<Vec3> offsets = mega ? List.of(
				side.scale(-0.9).add(0, -0.9, 0), new Vec3(0, -0.9, 0), side.scale(0.9).add(0, -0.9, 0),
				side.scale(-0.9), Vec3.ZERO, side.scale(0.9),
				side.scale(-0.9).add(0, 0.9, 0), new Vec3(0, 0.9, 0), side.scale(0.9).add(0, 0.9, 0))
				: List.of(Vec3.ZERO);
		List<FlyingBossBomb> bombs = FLYING_BOMBS.computeIfAbsent(boss.getUUID(),
				ignored -> new java.util.ArrayList<>());
		for (Vec3 offset : offsets) {
			PrimedTnt bomb = EntityTypes.TNT.create(level, EntitySpawnReason.EVENT);
			if (bomb == null) {
				continue;
			}
			bomb.setPos(boss.getX() + offset.x, boss.getY() + offset.y, boss.getZ() + offset.z);
			bomb.setFuse(1200);
			bomb.setCustomName(Component.literal(mega ? "OVERLORD MEGA TNT" : "Overlord TNT"));
			bomb.setCustomNameVisible(mega && offset.equals(Vec3.ZERO));
			bomb.setGlowingTag(mega);
			bomb.setDeltaMovement(direction.scale(mega ? 0.48 : 0.65).add(0, mega ? 0.42 : 0.55, 0));
			if (level.addFreshEntity(bomb)) {
				bombs.add(new FlyingBossBomb(bomb, boss.tickCount + (mega ? 70 : 45),
						radius, damage, mega, mega && !offset.equals(Vec3.ZERO)));
			}
		}
	}

	private static void tickFlyingBombs(ServerLevel level, SkyOverlord boss, int threat) {
		List<FlyingBossBomb> bombs = FLYING_BOMBS.get(boss.getUUID());
		if (bombs == null) {
			return;
		}
		bombs.removeIf(bomb -> {
			if (bomb.entity().isRemoved()) {
				return true;
			}
			if (boss.tickCount < bomb.detonationTick()) {
				level.sendParticles(bomb.mega() ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
						bomb.entity().getX(), bomb.entity().getY(),
						bomb.entity().getZ(), bomb.mega() ? 5 : 2, 0.1, 0.1, 0.1, 0.01);
				if (bomb.mega() && !bomb.visualOnly() && boss.tickCount % 5 == 0) {
					emitMark(level, bomb.entity().position(), bomb.radius());
				}
				return false;
			}
			if (!bomb.visualOnly()) {
				if (bomb.mega()) {
					PurgeUnits.detonateMegaBossTnt(level, boss, bomb.entity().position(),
							bomb.radius(), bomb.damage());
				} else {
					PurgeUnits.detonateBossTnt(level, boss, bomb.entity().position(), threat,
							bomb.radius(), bomb.damage());
				}
			}
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

	private static void breakPhantomPathBlocks(ServerLevel level, SkyOverlord boss, Vec3 direction) {
		if (boss.tickCount % 5 != 0
				|| !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
			return;
		}
		int centerX = (int) Math.floor(boss.getX() + direction.x * 2.5);
		int centerY = (int) Math.floor(boss.getY() + direction.y * 1.5);
		int centerZ = (int) Math.floor(boss.getZ() + direction.z * 2.5);
		int broken = 0;
		for (int x = -1; x <= 1 && broken < 9; x++) {
			for (int y = -1; y <= 1 && broken < 9; y++) {
				for (int z = -1; z <= 1 && broken < 9; z++) {
					var pos = new net.minecraft.core.BlockPos(centerX + x, centerY + y, centerZ + z);
					var state = level.getBlockState(pos);
					if (state.isAir() || level.getBlockEntity(pos) != null) {
						continue;
					}
					if (!com.cluck.block.BunkerBlock.hasClearBlockPath(
							level, boss.getEyePosition(), pos)) {
						continue;
					}
					if (com.cluck.block.BunkerBlock.handleDestructionAttempt(null, level, pos, boss, 10)) {
						broken++;
					} else if (state.getDestroySpeed(level, pos) >= 0
							&& level.destroyBlock(pos, false, boss)) {
						broken++;
					}
				}
			}
		}
	}

	private static void tickBoss(ServerLevel level, OverlordBeast boss, List<ServerPlayer> players, int threat) {
		List<IronGolem> generators = shieldGenerators(boss);
		boss.setPermanentlyInvulnerable(false);
		int shieldResistance = shieldResistanceAmplifier(generators);
		if (shieldResistance >= 0) {
			boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, shieldResistance, true, false, false));
		} else {
			boss.removeEffect(MobEffects.RESISTANCE);
		}
		ServerPlayer target = players.stream()
				.filter(player -> player.level() == level && player.isAlive())
				.min((first, second) -> Double.compare(boss.distanceToSqr(first), boss.distanceToSqr(second)))
				.orElse(null);
		if (target == null) {
			return;
		}
		boss.setTarget(target);
		if (boss.distanceToSqr(target) > 150 * 150) {
			teleportBossNearPlayer(level, boss, target, generators);
		}
		if (boss.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
			boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.78);
		}
		if (!RAVAGER_CHARGES.containsKey(boss.getUUID())
				&& !RAVAGER_JUMPS.containsKey(boss.getUUID())
				&& !BOULDER_WARNINGS.containsKey(boss.getUUID())
				&& !BOULDER_VOLLEYS.containsKey(boss.getUUID())
				&& !SONIC_BOOMS.containsKey(boss.getUUID())
				&& boss.tickCount % 10 == 0) {
			boss.getNavigation().moveTo(target, 2.3);
		}
		if (boss.tickCount % 2 == 0) {
			breakPathBlocks(level, boss, target);
		}
		recoverStuckBoss(level, boss, target);
		tickGenerators(level, boss, generators, target);
		tickRavagerMelee(level, boss, target, threat);
		tickRavagerCharge(level, boss, target);
		tickBoulderToss(level, boss, target);
		tickBoulderVolley(level, boss, threat);
		tickRavagerJump(level, boss, target, threat);
		tickSonicBoom(level, boss, target, threat);
		tickGeneratorProjectiles(level, boss, threat);
		tickSoulFlameZones(level, boss, threat);
		tickLightningStrike(level, boss, target, threat);
		if (boss.tickCount % 200 == 180) {
			showCombatAlert(target, Component.literal(
					"Overlord targeting drone inbound! Use flak or take cover!"));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getEyeY(),
					boss.getZ(), 16, 0.5, 0.25, 0.5, 0.12);
			level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
					net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.6F);
		}
		if (boss.tickCount % 200 == 0 && PurgeUnits.launchDrone(level, boss, target)) {
			level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getEyeY(), boss.getZ(),
					12, 0.3, 0.2, 0.3, 0.08);
		}
		if (boss.tickCount % 120 == 0) {
			PurgeUnits.spawnRavagerMilitia(level, boss, target, threat);
		}
		if (boss.tickCount % 240 == 0) {
			PurgeUnits.spawnWarlordGunner(level, boss, target, "Overlord Gunner", false);
		}
		if (!generators.isEmpty()) {
			if (boss.tickCount % 60 == 0) {
				IronGolem shield = generators.getFirst();
				int shieldPercent = (int) Math.ceil(100.0 * shield.getHealth() / Math.max(1.0F, shield.getMaxHealth()));
				showCombatAlert(target, Component.literal(
						"Shield golem at " + shieldPercent + "% — Overlord Resistance "
								+ toRoman(shieldResistance + 1) + ". Destroy it!"));
			}
			if (boss.tickCount % 50 == 0 && boss.distanceToSqr(target) < 20 * 20
					&& PurgeUnits.bossAimPosition(level, target)
							.distanceToSqr(target.getBoundingBox().getCenter()) <= 3 * 3
					&& PurgeUnits.hasClearAttackPath(level, boss, target)) {
				target.hurtServer(level, level.damageSources().mobAttack(boss),
						(10 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier());
			}
		}

		float healthRatio = boss.getHealth() / Math.max(1, boss.getMaxHealth());
		if (healthRatio > 0.6F) {
			if (boss.tickCount % 100 == 70 && boss.distanceToSqr(target) < 24 * 24) {
				showCombatAlert(target, Component.literal("Ravager charging — dodge sideways!"));
				level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY() + 0.5,
						boss.getZ(), 16, 0.5, 0.2, 0.5, 0.02);
			}
			if (boss.tickCount % 100 == 0 && boss.distanceToSqr(target) < 24 * 24) {
				Vec3 chargeAim = target.position().add(aimOffset(level, target));
				Vec3 charge = chargeAim.subtract(boss.position()).normalize();
				boss.setDeltaMovement(charge.scale(1.8).add(0, 0.18, 0));
				if (boss.distanceToSqr(target) < 12 * 12
						&& chargeAim.distanceToSqr(target.position()) <= 3 * 3
						&& PurgeUnits.hasClearAttackPath(level, boss, target)) {
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
				if (level.canSeeSky(target.blockPosition())) {
					ORBITAL_MARKS.put(boss.getUUID(), target.position().add(aimOffset(level, target)));
				} else {
					ORBITAL_MARKS.remove(boss.getUUID());
				}
			}
			Vec3 impact = ORBITAL_MARKS.get(boss.getUUID());
			boolean canHit = impact != null && level.canSeeSky(BlockPos.containing(impact))
					&& PurgeUnits.hasBlastExposure(level, impact, target);
			if (orbitalTick < 40 && canHit && boss.tickCount % 120 == 0) {
				showCombatAlert(target, Component.literal("Orbital strike incoming — get under cover!"));
			}
			if (orbitalTick == 40 && canHit) {
				level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1, impact.z, 1, 0, 0, 0, 0);
				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
						new AABB(impact, impact).inflate(7), entity -> entity != boss && entity.isAlive() && !isGenerator(entity))) {
					if (!PurgeUnits.hasBlastExposure(level, impact, victim)) {
						continue;
					}
					float dmg = (40 + threat * 6) * BattleChickens.CONFIG.enemyDamageMultiplier();
					if (victim instanceof ServerPlayer player) {
						dmg *= PurgeUnits.explosiveDefenseMultiplier(player);
					}
					victim.hurtServer(level, level.damageSources().mobAttack(boss), dmg);
				}
				PurgeUnits.destroyBossImpact(level, impact, boss);
				PurgeUnits.spawnBossReinforcements(level, boss, target, threat);
			}
			if (orbitalTick == 40) {
				ORBITAL_MARKS.remove(boss.getUUID());
			}
		}
	}

	private static void tickRavagerMelee(ServerLevel level, OverlordBeast boss, ServerPlayer target, int threat) {
		double reach = boss.getBbWidth() * 0.5 + 1.5;
		if (boss.distanceToSqr(target) <= reach * reach && boss.tickCount % 20 == 0
				&& PurgeUnits.bossAimPosition(level, target)
						.distanceToSqr(target.getBoundingBox().getCenter()) <= reach * reach) {
			if (target.hurtServer(level, level.damageSources().mobAttack(boss),
					(10 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier())) {
				Vec3 shove = target.position().subtract(boss.position()).normalize();
				applyDaySevenKnockback(target, shove.scale(0.45).add(0, 0.25, 0));
				level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(),
						14, 0.5, 0.4, 0.5, 0.08);
			}
		}
	}

	private static void tickRavagerCharge(ServerLevel level, OverlordBeast boss, ServerPlayer target) {
		RavagerCharge charge = RAVAGER_CHARGES.get(boss.getUUID());
		if (charge == null && boss.tickCount > 100 && boss.tickCount % 300 == 0
				&& !RAVAGER_JUMPS.containsKey(boss.getUUID())
				&& !BOULDER_WARNINGS.containsKey(boss.getUUID())
				&& !BOULDER_VOLLEYS.containsKey(boss.getUUID())
				&& !SONIC_BOOMS.containsKey(boss.getUUID())
				&& PurgeUnits.hasClearAttackPath(level, boss, target)) {
			charge = new RavagerCharge(target.position().add(aimOffset(level, target)),
					boss.tickCount + 100);
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
				entity -> entity != boss && entity.isAlive() && !isGenerator(entity))) {
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

	private static void tickBoulderToss(ServerLevel level, OverlordBeast boss, ServerPlayer target) {
		UUID bossId = boss.getUUID();
		BoulderWarning warning = BOULDER_WARNINGS.get(bossId);
		if (warning == null && boss.tickCount > 100
				&& boss.tickCount >= BOULDER_COOLDOWNS.getOrDefault(bossId, 100)
				&& !RAVAGER_CHARGES.containsKey(bossId)
				&& !RAVAGER_JUMPS.containsKey(bossId)
				&& !BOULDER_VOLLEYS.containsKey(bossId)
				&& !SONIC_BOOMS.containsKey(bossId)
				&& level.canSeeSky(target.blockPosition())) {
			Vec3 aim = target.position().add(target.getDeltaMovement().scale(8))
					.add(aimOffset(level, target));
			warning = new BoulderWarning(aim, boss.tickCount + 60);
			BOULDER_WARNINGS.put(bossId, warning);
			BOULDER_COOLDOWNS.put(bossId, boss.tickCount + 240);
			boss.getNavigation().stop();
			showCombatAlert(target, Component.literal("Anvil rain incoming — move out of the landing zone!"));
			level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.RAVAGER_ROAR,
					net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.8F);
		}
		if (warning == null) {
			return;
		}
		if (!level.canSeeSky(target.blockPosition())) {
			BOULDER_WARNINGS.remove(bossId);
			return;
		}
		if (boss.tickCount % 5 == 0) {
			for (int step = 0; step < 8; step++) {
				double offset = -1.5 + 3.0 * step / 7.0;
				for (double edge : new double[]{-1.5, 1.5}) {
					sendLandingMarker(level, warning.target().x + offset, warning.target().z + edge, target);
					sendLandingMarker(level, warning.target().x + edge, warning.target().z + offset, target);
				}
			}
		}
		if (boss.tickCount < warning.launchAt()) {
			return;
		}
		BOULDER_WARNINGS.remove(bossId);
		launchBoulder(level, boss, warning.target());
	}

	private static void launchBoulder(ServerLevel level, OverlordBeast boss, Vec3 targetPosition) {
		List<FallingBlockEntity> anvils = new ArrayList<>(9);
		for (int xOffset = -1; xOffset <= 1; xOffset++) {
			for (int zOffset = -1; zOffset <= 1; zOffset++) {
				int x = BlockPos.containing(targetPosition).getX() + xOffset;
				int z = BlockPos.containing(targetPosition).getZ() + zOffset;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 12;
				if (y >= level.getMaxY() - 1) {
					continue;
				}
				BlockPos spawnPos = new BlockPos(x, y, z);
				if (!level.getBlockState(spawnPos).isAir()
						|| !level.setBlockAndUpdate(spawnPos, Blocks.ANVIL.defaultBlockState())) {
					continue;
				}
				FallingBlockEntity anvil = FallingBlockEntity.fall(level, spawnPos, Blocks.ANVIL.defaultBlockState());
				anvil.disableDrop();
				anvil.setHurtsEntities(4.0F, 80);
				anvils.add(anvil);
			}
		}
		if (anvils.isEmpty()) {
			return;
		}
		BOULDER_VOLLEYS.put(boss.getUUID(), new BoulderVolley(anvils));
		level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND,
				net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.65F);
	}

	private static void sendLandingMarker(ServerLevel level, double x, double z, ServerPlayer target) {
		int blockX = BlockPos.containing(x, level.getMinY(), z).getX();
		int blockZ = BlockPos.containing(x, level.getMinY(), z).getZ();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
		Vec3 landing = new Vec3(x, y, z);
		if (PurgeUnits.hasBlastExposure(level, landing, target)) {
			level.sendParticles(ParticleTypes.CRIT, x, y + 0.1, z, 1, 0, 0, 0, 0);
		}
	}

	private static void tickBoulderVolley(ServerLevel level, OverlordBeast boss, int threat) {
		BoulderVolley volley = BOULDER_VOLLEYS.get(boss.getUUID());
		if (volley == null) {
			return;
		}
		volley.age++;
		Iterator<FallingBlockEntity> anvils = volley.anvils.iterator();
		while (anvils.hasNext()) {
			FallingBlockEntity anvil = anvils.next();
			if (anvil.isRemoved() || anvil.onGround()) {
				Vec3 impact = anvil.position();
				BlockPos landedPos = BlockPos.containing(impact);
				if (level.getBlockState(landedPos).is(Blocks.ANVIL)) {
					level.removeBlock(landedPos, false);
				}
				volley.impacts.add(impact);
				anvil.discard();
				anvils.remove();
			}
		}
		if (volley.anvils.isEmpty() || volley.age >= 200) {
			if (volley.impacts.isEmpty()) {
				volley.impacts.add(boss.position());
			}
			Vec3 impact = volley.impacts.stream().reduce(Vec3.ZERO, Vec3::add)
					.scale(1.0 / volley.impacts.size());
			detonateBoulder(level, boss, impact, threat);
		}
	}

	private static void detonateBoulder(ServerLevel level, OverlordBeast boss, Vec3 impact, int threat) {
		BoulderVolley volley = BOULDER_VOLLEYS.remove(boss.getUUID());
		if (volley != null) {
			volley.anvils.forEach(FallingBlockEntity::discard);
		}
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(impact, impact).inflate(5), entity -> entity != boss
						&& entity.isAlive() && !isGenerator(entity))) {
			if (!PurgeUnits.hasBlastExposure(level, impact, victim)) {
				continue;
			}
			double distance = Math.sqrt(victim.distanceToSqr(impact));
			float damage = (100 + threat * 20) * (float) Math.max(0.2, 1.0 - distance / 7.0)
					* BattleChickens.CONFIG.enemyDamageMultiplier();
			if (victim instanceof ServerPlayer player) {
				damage *= PurgeUnits.explosiveDefenseMultiplier(player);
			}
			victim.hurtServer(level, level.damageSources().mobAttack(boss), damage);
			Vec3 knockback = victim.position().subtract(impact).normalize();
			applyDaySevenKnockback(victim, knockback.scale(1.6).add(0, 0.8, 0));
		}
		PurgeUnits.destroyBossImpact(level, impact, boss);
		level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z,
				12, 2.5, 1.5, 2.5, 0.12);
		level.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y, impact.z,
				64, 4.0, 2.5, 4.0, 0.2);
		level.playSound(null, net.minecraft.core.BlockPos.containing(impact),
				net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
				net.minecraft.sounds.SoundSource.HOSTILE, 6.0F, 0.55F);
	}

	private static void tickRavagerJump(ServerLevel level, OverlordBeast boss, ServerPlayer target, int threat) {
		RavagerJump jump = RAVAGER_JUMPS.get(boss.getUUID());
		if (jump == null && boss.tickCount > 100 && boss.tickCount % 420 == 0
				&& !RAVAGER_CHARGES.containsKey(boss.getUUID())
				&& !BOULDER_WARNINGS.containsKey(boss.getUUID())
				&& !BOULDER_VOLLEYS.containsKey(boss.getUUID())
				&& !SONIC_BOOMS.containsKey(boss.getUUID())
				&& PurgeUnits.hasClearAttackPath(level, boss, target)
				&& level.canSeeSky(target.blockPosition())) {
			jump = new RavagerJump(target.position().add(aimOffset(level, target)),
					boss.tickCount + 30, boss.tickCount + 65, false);
			RAVAGER_JUMPS.put(boss.getUUID(), jump);
			boss.getNavigation().stop();
			showCombatAlert(target, Component.literal("Ravager leaping — leave the marked zone!"));
		}
		if (jump == null) {
			return;
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
			if (level.canSeeSky(BlockPos.containing(landing))
					&& level.canSeeSky(target.blockPosition())
					&& target.distanceToSqr(landing) <= 6.0 * 6.0
					&& PurgeUnits.hasBlastExposure(level, landing, target)) {
				level.sendParticles(ParticleTypes.EXPLOSION, landing.x, landing.y + 0.5, landing.z,
						4, 1.5, 0.5, 1.5, 0.1);
			}
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
					new AABB(landing, landing).inflate(6), entity -> entity != boss && entity.isAlive() && !isGenerator(entity))) {
				if (!PurgeUnits.hasBlastExposure(level, landing, victim)) {
					continue;
				}
				double distance = Math.sqrt(victim.distanceToSqr(landing));
				victim.hurtServer(level, level.damageSources().mobAttack(boss),
						(distance < 2 ? 55 : 30) * BattleChickens.CONFIG.enemyDamageMultiplier());
				Vec3 knockback = victim.position().subtract(landing).normalize();
				applyDaySevenKnockback(victim, knockback.scale(0.8).add(0, 0.8, 0));
			}
			RAVAGER_JUMPS.remove(boss.getUUID());
		}
	}

	private static void tickSonicBoom(ServerLevel level, OverlordBeast boss, ServerPlayer target, int threat) {
		SonicBoom attack = SONIC_BOOMS.get(boss.getUUID());
		if (attack == null && !SONIC_BOOM_USED.contains(boss.getUUID())
				&& boss.tickCount > 100 && boss.tickCount % 240 == 0
				&& !RAVAGER_CHARGES.containsKey(boss.getUUID())
				&& !RAVAGER_JUMPS.containsKey(boss.getUUID())
				&& !BOULDER_WARNINGS.containsKey(boss.getUUID())
				&& !BOULDER_VOLLEYS.containsKey(boss.getUUID())
				&& PurgeUnits.hasClearAttackPath(level, boss, target)) {
			attack = new SonicBoom(PurgeUnits.bossAimPosition(level, target), boss.tickCount + 100);
			SONIC_BOOMS.put(boss.getUUID(), attack);
			SONIC_BOOM_USED.add(boss.getUUID());
			showCombatAlert(target, Component.literal(
					"The Overlord channels a sonic boom! The blast will erupt in five seconds!"));
			level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE,
					net.minecraft.sounds.SoundSource.HOSTILE, 3.5F, 0.6F);
		}
		if (attack == null) {
			return;
		}
		Vec3 aim = attack.aim();
		Vec3 direction = aim.subtract(boss.getEyePosition()).normalize();
		Vec3 origin = boss.getEyePosition().add(direction.scale(3.0));
		if (boss.tickCount < attack.fireAt()) {
			return;
		}
		double beamLength = Math.min(128, aim.distanceTo(origin));
		for (double distance = 0; distance <= beamLength; distance += 1.5) {
			Vec3 point = origin.add(direction.scale(distance));
			if (level.clip(new net.minecraft.world.level.ClipContext(
					origin, point, net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE, boss)).getType()
					!= HitResult.Type.MISS) {
				break;
			}
			level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(128),
				entity -> entity != boss && entity.isAlive() && !isGenerator(entity))) {
			Vec3 relative = victim.getBoundingBox().getCenter().subtract(origin);
			double along = relative.dot(direction);
			if (along < 0 || along > beamLength
					|| relative.subtract(direction.scale(along)).lengthSqr() > 4.0 * 4.0) {
				continue;
			}
			if (!PurgeUnits.hasClearAttackPath(level, boss, victim)) {
				continue;
			}
			victim.hurtServer(level, level.damageSources().mobAttack(boss),
					(35 + threat * 5) * BattleChickens.CONFIG.enemyDamageMultiplier());
			applyDaySevenKnockback(victim, direction.scale(1.8).add(0, 0.35, 0));
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

	private static void tickGenerators(ServerLevel level, OverlordBeast boss, List<IronGolem> generators,
			ServerPlayer target) {
		for (int index = 0; index < generators.size(); index++) {
			IronGolem generator = generators.get(index);
			if (generator.isLeashed()) {
				generator.removeLeash();
			}
			double angle = boss.tickCount * 0.01 + Math.PI * 2 * index / Math.max(1, generators.size());
			double orbitRadius = 12.0;
			Vec3 formation = boss.position().add(Math.cos(angle) * orbitRadius, 0, Math.sin(angle) * orbitRadius);
			boolean obstructed = !level.noCollision(generator, generator.getBoundingBox());
			boolean farAway = generator.distanceToSqr(boss) > 28 * 28;
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
				fireGeneratorPulse(level, generator, target, boss);
				GENERATOR_PULSES.put(generator.getUUID(), generator.tickCount + 90 + level.getRandom().nextInt(100));
			}
		}
	}

	private static void tickGeneratorProjectiles(ServerLevel level, OverlordBeast boss, int threat) {
		List<GeneratorPulse> pulses = GENERATOR_PROJECTILES.get(boss.getUUID());
		if (pulses == null) {
			return;
		}
		for (int index = pulses.size() - 1; index >= 0; index--) {
			GeneratorPulse pulse = pulses.get(index);
			ServerPlayer target = playersInLevel(level).stream()
					.filter(player -> player.getUUID().equals(pulse.target()) && player.isAlive())
					.findFirst().orElse(null);
			if (target == null) {
				pulses.remove(index);
				continue;
			}
			Vec3 start = pulse.position();
			Vec3 next = start.add(pulse.direction().scale(0.65));
			BlockHitResult obstruction = level.clip(new net.minecraft.world.level.ClipContext(
					start, next, net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE, boss));
			Vec3 end = obstruction.getType() == HitResult.Type.MISS ? next : obstruction.getLocation();
			boolean hitTarget = target.getBoundingBox().inflate(0.35).clip(start, end).isPresent();
			level.sendParticles(new DustParticleOptions(0x2488FF, 1.5F),
					(start.x + end.x) * 0.5, (start.y + end.y) * 0.5, (start.z + end.z) * 0.5,
					3, 0.04, 0.04, 0.04, 0);
			if (hitTarget || obstruction.getType() != HitResult.Type.MISS) {
				Vec3 impact = hitTarget ? target.position() : end;
				if (hitTarget) {
					target.hurtServer(level, level.damageSources().magic(),
							(12 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier());
				}
				addSoulFlameZone(level, boss, impact);
				if (hitTarget) {
					showCombatAlert(target, Component.literal("Shield pulse impact — escape the soulflame!"));
				}
				pulses.remove(index);
				continue;
			}
			if (pulse.age() >= 99) {
				pulses.remove(index);
				continue;
			}
			pulses.set(index, new GeneratorPulse(pulse.target(), next, pulse.direction(), pulse.age() + 1));
		}
		if (pulses.isEmpty()) {
			GENERATOR_PROJECTILES.remove(boss.getUUID());
		}
	}

	private static Vec3 findClearGeneratorPosition(ServerLevel level, IronGolem generator, OverlordBeast boss,
			int index, int generatorCount) {
		for (int radius : new int[]{10, 12, 14, 16}) {
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
		return boss.position().add(12, 0, 0);
	}

	private static void fireGeneratorPulse(ServerLevel level, IronGolem generator, ServerPlayer target,
			OverlordBeast boss) {
		if (!PurgeUnits.hasClearAttackPath(level, generator, target)) {
			return;
		}
		Vec3 start = generator.getEyePosition();
		Vec3 targetPosition = PurgeUnits.bossAimPosition(level, target);
		Vec3 direction = targetPosition.subtract(start).normalize();
		GENERATOR_PROJECTILES.computeIfAbsent(boss.getUUID(), ignored -> new ArrayList<>())
				.add(new GeneratorPulse(target.getUUID(), start, direction, 0));
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z,
				12, 0.2, 0.2, 0.2, 0.08);
		showCombatAlert(target, Component.literal("Shield pulse incoming — keep moving!"));
		level.playSound(null, generator.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
				net.minecraft.sounds.SoundSource.HOSTILE, 1.4F, 0.8F);
	}

	private static void addSoulFlameZone(ServerLevel level, OverlordBeast boss, Vec3 impact) {
		int x = net.minecraft.core.BlockPos.containing(impact).getX();
		int z = net.minecraft.core.BlockPos.containing(impact).getZ();
		int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		Vec3 ground = new Vec3(x + 0.5, y, z + 0.5);
		List<SoulFlameZone> zones = SOUL_FLAME_ZONES.computeIfAbsent(boss.getUUID(),
				ignored -> new ArrayList<>());
		if (zones.size() >= 4) {
			zones.removeFirst();
		}
		zones.add(new SoulFlameZone(ground, level.getGameTime() + 200, level.getGameTime()));
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ground.x, ground.y + 0.1, ground.z,
				40, 1.2, 0.1, 1.2, 0.06);
	}

	private static void tickSoulFlameZones(ServerLevel level, OverlordBeast boss, int threat) {
		UUID bossId = boss.getUUID();
		List<SoulFlameZone> zones = SOUL_FLAME_ZONES.get(bossId);
		if (zones == null) {
			return;
		}
		for (int index = 0; index < zones.size(); index++) {
			SoulFlameZone zone = zones.get(index);
			if (boss.tickCount % 5 == 0) {
				for (int x = -1; x <= 1; x++) {
					for (int z = -1; z <= 1; z++) {
						level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
								zone.position().x + x, zone.position().y + 0.1,
								zone.position().z + z, 2, 0.12, 0.12, 0.12, 0.01);
					}
				}
			}
			if (level.getGameTime() >= zone.nextDamageAt()) {
				AABB contactArea = new AABB(zone.position(), zone.position()).inflate(1.5, 1.0, 1.5);
				for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, contactArea,
						entity -> entity != boss && entity.isAlive() && !isGenerator(entity))) {
					if (!PurgeUnits.hasBlastExposure(level, zone.position(), victim)) {
						continue;
					}
					victim.hurtServer(level, level.damageSources().magic(), 8.0F);
				}
				zones.set(index, new SoulFlameZone(zone.position(), zone.expiresAt(), level.getGameTime() + 20));
			}
		}
		zones.removeIf(zone -> level.getGameTime() >= zone.expiresAt());
		if (zones.isEmpty()) {
			SOUL_FLAME_ZONES.remove(bossId);
		}
	}

	private static void breakPathBlocks(ServerLevel level, OverlordBeast boss, ServerPlayer target) {
		breakPathBlocks(level, boss, target.position().subtract(boss.position()));
	}

	private static void breakPathBlocks(ServerLevel level, OverlordBeast boss, Vec3 direction) {
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
					if (state.isAir() || level.getBlockEntity(pos) != null) {
						continue;
					}
					if (!com.cluck.block.BunkerBlock.hasClearBlockPath(
							level, boss.getEyePosition(), pos)) {
						continue;
					}
					if (com.cluck.block.BunkerBlock.handleDestructionAttempt(null, level, pos, boss, 10)) {
						broken++;
					} else if (state.getDestroySpeed(level, pos) >= 0
							&& level.destroyBlock(pos, false, boss)) {
						broken++;
					}
				}
			}
		}
	}

	private static void recoverStuckBoss(ServerLevel level, OverlordBeast boss, ServerPlayer target) {
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

	private static void tickLightningStrike(ServerLevel level, OverlordBeast boss, ServerPlayer target, int threat) {
		LightningChase strike = LIGHTNING_MARKS.get(boss.getUUID());
		if (strike == null && boss.tickCount % 200 == 0 && level.canSeeSky(target.blockPosition())) {
			Vec3 markedPosition = target.position().add(target.getDeltaMovement().scale(6));
			markedPosition = markedPosition.add(aimOffset(level, target));
			if (!level.canSeeSky(BlockPos.containing(markedPosition))) {
				return;
			}
			strike = new LightningChase(markedPosition, boss.tickCount + 30);
			LIGHTNING_MARKS.put(boss.getUUID(), strike);
			showCombatAlert(target, Component.literal("Lightning marked — leave the ring!"));
		}
		if (strike == null) {
			return;
		}
		Vec3 impact = strike.impact();
		if (!level.canSeeSky(BlockPos.containing(impact))
				|| !level.canSeeSky(target.blockPosition())) {
			LIGHTNING_MARKS.remove(boss.getUUID());
			return;
		}
		if (boss.tickCount < strike.strikeTick()) {
			return;
		}
		if (!level.canSeeSky(BlockPos.containing(impact))
				|| !level.canSeeSky(target.blockPosition())) {
			LIGHTNING_MARKS.remove(boss.getUUID());
			return;
		}
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(impact);
			level.addFreshEntity(bolt);
		}
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				new AABB(impact, impact).inflate(2.0), entity -> entity != boss && entity.isAlive() && !isGenerator(entity))) {
			if (!level.canSeeSky(victim.blockPosition())
					|| !PurgeUnits.hasBlastExposure(level, impact.add(0, 32, 0), victim)) {
				continue;
			}
			float dmg = (12 + threat * 2) * BattleChickens.CONFIG.enemyDamageMultiplier();
			if (victim instanceof ServerPlayer player) {
				dmg *= PurgeUnits.explosiveDefenseMultiplier(player);
			}
			victim.hurtServer(level, level.damageSources().mobAttack(boss), dmg);
			Vec3 knockback = victim.position().subtract(impact).normalize();
			applyDaySevenKnockback(victim, knockback.scale(0.25).add(0, 0.2, 0));
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x,
				impact.y + 1, impact.z, 36, 1.5, 2.0, 1.5, 0.15);
		level.playSound(null, net.minecraft.core.BlockPos.containing(impact),
				net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
				net.minecraft.sounds.SoundSource.WEATHER, 2.0F, 1.3F);
		LIGHTNING_MARKS.remove(boss.getUUID());
	}

	private static List<ServerPlayer> playersInLevel(ServerLevel level) {
		return level.getServer().getPlayerList().getPlayers().stream()
				.filter(player -> player.level() == level).toList();
	}

	public static void showCombatAlert(ServerPlayer player, Component message) {
		String text = message.getString().toLowerCase(java.util.Locale.ROOT);
		if (!(text.contains("rocket barrage")
				|| text.contains("bombardment")
				|| text.contains("mega-bomb run")
				|| text.contains("orbital strike incoming")
				|| text.contains("lightning marked")
				|| text.contains("anvil rain incoming")
				|| text.contains("grenadier bomb incoming")
				|| text.contains("has you in its sights")
				|| text.contains("ravager charge!")
				|| text.contains("ravager leaping")
				|| text.contains("channels a sonic boom")
				|| text.contains("shield pulse incoming")
				|| text.contains("targeting drone inbound"))) {
			return;
		}
		player.sendSystemMessage(message, true);
	}

	private static Vec3 aimOffset(ServerLevel level, ServerPlayer target) {
		return PurgeUnits.bossAimPosition(level, target).subtract(target.getBoundingBox().getCenter());
	}

	private static void fireRocket(ServerLevel level, LivingEntity boss, ServerPlayer target, double spread) {
		Vec3 aim = PurgeUnits.bossAimPosition(level, target);
		Vec3 direction = aim.subtract(boss.getEyePosition());
		if (spread != 0) {
			Vec3 side = new Vec3(direction.z, 0, -direction.x).normalize().scale(spread);
			direction = direction.add(side);
		}
		level.addFreshEntity(new PurgeRocketEntity(level, boss, direction));
	}

	public static boolean isGenerator(Entity entity) {
		return entity instanceof ShieldGeneratorGolem || hasName(entity, GENERATOR_NAME);
	}

	/**
	 * Resistance amplifier from shield golem HP: full = IV (3), scales down to I (0) near death.
	 * Returns -1 when no shield is up.
	 */
	public static int shieldResistanceAmplifier(List<? extends LivingEntity> generators) {
		LivingEntity strongest = null;
		for (LivingEntity generator : generators) {
			if (generator != null && generator.isAlive()
					&& (strongest == null || generator.getHealth() > strongest.getHealth())) {
				strongest = generator;
			}
		}
		if (strongest == null) {
			return -1;
		}
		float ratio = strongest.getHealth() / Math.max(1.0F, strongest.getMaxHealth());
		return Math.min(3, Math.max(0, (int) Math.ceil(ratio * 4.0F) - 1));
	}

	public static List<IronGolem> shieldGenerators(Entity bossEntity) {
		if (!(bossEntity instanceof OverlordBeast boss) || !(boss.level() instanceof ServerLevel level)) {
			return List.of();
		}
		return level.getEntitiesOfClass(IronGolem.class, boss.getBoundingBox().inflate(128),
				generator -> generator.isAlive() && hasName(generator, GENERATOR_NAME));
	}

	public static String toRoman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(n);
		};
	}

	private static void teleportBossNearPlayer(ServerLevel level, OverlordBeast boss, ServerPlayer target,
			List<IronGolem> generators) {
		double angle = target.getYRot() * (Math.PI / 180.0) + (level.getRandom().nextDouble() - 0.5) * 0.5;
		double distance = 50.0 + level.getRandom().nextDouble() * 20.0;
		double tx = target.getX() - Math.sin(angle) * distance;
		double tz = target.getZ() + Math.cos(angle) * distance;
		int ty = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(int) Math.floor(tx), (int) Math.floor(tz));

		level.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), 40, 1.5, 1.5, 1.5, 0.2);
		boss.teleportTo(tx, ty, tz);
		boss.setDeltaMovement(Vec3.ZERO);
		boss.getNavigation().stop();
		level.sendParticles(ParticleTypes.PORTAL, tx, ty + 1, tz, 40, 1.5, 1.5, 1.5, 0.2);
		level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
				net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.6F);

		for (Entity passenger : boss.getPassengers()) {
			passenger.teleportTo(tx, ty + 1.2, tz);
			passenger.startRiding(boss, true, true);
		}

		for (int i = 0; i < generators.size(); i++) {
			IronGolem gen = generators.get(i);
			double genAngle = Math.PI * 2 * i / Math.max(1, generators.size());
			double gx = tx + Math.cos(genAngle) * 12.0;
			double gz = tz + Math.sin(genAngle) * 12.0;
			int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(int) Math.floor(gx), (int) Math.floor(gz));
			gen.teleportTo(gx, gy, gz);
			gen.setDeltaMovement(Vec3.ZERO);
		}

		showCombatAlert(target, Component.literal("The Overlord teleports to cut off your escape!"));
	}

	private static void teleportAerialBossNearPlayer(ServerLevel level, SkyOverlord boss, ServerPlayer target) {
		double angle = target.getYRot() * (Math.PI / 180.0) + (level.getRandom().nextDouble() - 0.5) * 0.5;
		double distance = 50.0 + level.getRandom().nextDouble() * 20.0;
		double tx = target.getX() - Math.sin(angle) * distance;
		double tz = target.getZ() + Math.cos(angle) * distance;
		int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(int) Math.floor(tx), (int) Math.floor(tz));
		double ty = Math.max(groundY + 16, target.getY() + 16);

		level.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 1.5, 1.5, 1.5, 0.2);
		boss.teleportTo(tx, ty, tz);
		boss.setDeltaMovement(Vec3.ZERO);
		level.sendParticles(ParticleTypes.PORTAL, tx, ty + 1, tz, 30, 1.5, 1.5, 1.5, 0.2);
		level.playSound(null, boss.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
				net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.7F);

		for (Entity passenger : boss.getPassengers()) {
			passenger.teleportTo(tx, ty + 1.0, tz);
			passenger.startRiding(boss, true, true);
		}

		showCombatAlert(target, Component.literal("The Sky Overlord teleports across the sky in pursuit!"));
	}

	public static boolean isAerialBoss(Entity entity) {
		return hasName(entity, AERIAL_BOSS_NAME);
	}

	public static void forget(Entity entity) {
		if (entity == null) {
			return;
		}
		ORBITAL_MARKS.remove(entity.getUUID());
		List<MarkedBomb> markedBombs = BOMB_MARKS.remove(entity.getUUID());
		if (markedBombs != null) {
			markedBombs.stream().map(MarkedBomb::projectile).filter(java.util.Objects::nonNull)
					.forEach(ItemEntity::discard);
		}
		LIGHTNING_MARKS.remove(entity.getUUID());
		SONIC_BOOM_USED.remove(entity.getUUID());
		SPIN_ATTACKS.remove(entity.getUUID());
		PURSUIT_STATES.remove(entity.getUUID());
		RAVAGER_CHARGES.remove(entity.getUUID());
		RAVAGER_JUMPS.remove(entity.getUUID());
		BOULDER_WARNINGS.remove(entity.getUUID());
		BOULDER_COOLDOWNS.remove(entity.getUUID());
		BoulderVolley volley = BOULDER_VOLLEYS.remove(entity.getUUID());
		if (volley != null) {
			volley.anvils.forEach(FallingBlockEntity::discard);
		}
		SONIC_BOOMS.remove(entity.getUUID());
		GENERATOR_PULSES.remove(entity.getUUID());
		GENERATOR_PROJECTILES.remove(entity.getUUID());
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
