package com.cluck.projectile;

import com.cluck.BattleChickens;
import com.cluck.block.BunkerBlock;
import com.cluck.event.PurgeManager;
import com.cluck.event.PurgeUnits;
import com.cluck.item.ModItems;
import com.cluck.event.BossController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class PurgeRocketEntity extends FireworkRocketEntity {
	private static final float DIRECT_DAMAGE = 28;
	private static final float SPLASH_DAMAGE = 10;
	private static final double SPLASH_RADIUS = 3.5;
	private BlockPos blockImpact;

	public PurgeRocketEntity(Level level, LivingEntity shooter) {
		this(level, shooter, shooter.getLookAngle());
	}

	public PurgeRocketEntity(Level level, LivingEntity shooter, Vec3 direction) {
		super(level, shooter.getX(), shooter.getEyeY() - 0.15, shooter.getZ(), new ItemStack(Items.FIREWORK_ROCKET));
		setOwner(shooter);
		setCustomName(Component.literal("Purge Rocket"));
		setCustomNameVisible(false);
		direction = direction.normalize();
		shoot(direction.x, direction.y, direction.z, 1.8F, 0.02F);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel serverLevel) {
			detonate(serverLevel, hit.getLocation(), hit.getEntity());
		} else {
			discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		if (level() instanceof ServerLevel serverLevel) {
			blockImpact = hit.getBlockPos();
			detonate(serverLevel, hit.getLocation(), null);
		} else {
			discard();
		}
	}

	private void detonate(ServerLevel level, Vec3 impact, Entity directHit) {
		detonate(level, impact, directHit, blockImpact, this, getOwner(), false, null);
	}

	public static void detonateDrone(ServerLevel level, Vec3 impact, Entity directHit, BlockPos blockImpact,
			Entity projectile, Entity owner, LivingEntity target) {
		detonate(level, impact, directHit, blockImpact, projectile, owner, true, target);
	}

	private static void detonate(ServerLevel level, Vec3 impact, Entity directHit, BlockPos blockImpact,
			Entity projectile, Entity owner, boolean guidedDrone, LivingEntity homingTarget) {
		boolean bossRocket = !guidedDrone && (PurgeUnits.isBoss(owner) || BossController.isAerialBoss(owner));
		boolean bunkerImpact = blockImpact != null && level.getBlockState(blockImpact).getBlock() instanceof BunkerBlock;
		if (bunkerImpact) {
			BunkerBlock.handleDestructionAttempt(null, level, blockImpact, owner);
		}
		double blastRadius = guidedDrone ? 4.0 : bossRocket ? 5.5 : SPLASH_RADIUS;
		DamageSourceFactory damageSourceFactory = new DamageSourceFactory(level, projectile, owner, guidedDrone);
		AABB blast = new AABB(impact, impact).inflate(blastRadius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blast, LivingEntity::isAlive)) {
			if (bunkerImpact) {
				continue;
			}
			if (target == owner) {
				continue;
			}
			if (target == directHit) {
				if (!(guidedDrone && target == homingTarget)
						&& (!(owner instanceof LivingEntity shooter)
								|| !PurgeUnits.hasClearAttackPath(level, shooter, target))) {
					continue;
				}
			} else if (!PurgeUnits.hasBlastExposure(level, impact, target)) {
				continue;
			}
			if (BossController.isGenerator(target) && PurgeUnits.isPurgeAlly(owner)) {
				continue;
			}
			if (PurgeUnits.isPurgeAlly(owner) && PurgeUnits.isPurgeAlly(target)) {
				boolean ownerIsBoss = PurgeUnits.isBoss(owner) || BossController.isAerialBoss(owner);
				boolean ownerIsChicken = PurgeUnits.isChickenAlly(owner);
				boolean targetIsBoss = PurgeUnits.isBoss(target) || BossController.isAerialBoss(target);
				boolean targetIsChicken = PurgeUnits.isChickenAlly(target);
				if (!(ownerIsBoss && targetIsChicken) && !(ownerIsChicken && targetIsBoss)) {
					continue;
				}
			}
			float damage = target == directHit
					? (guidedDrone ? 36 : bossRocket ? 58 : DIRECT_DAMAGE)
					: (guidedDrone ? 14 : bossRocket ? 20 : SPLASH_DAMAGE);
			if (target == directHit && target instanceof ServerPlayer player
					&& owner instanceof LivingEntity livingOwner && !(owner instanceof ServerPlayer)
					&& player.getOffhandItem().is(ModItems.CLUCK_SHIELD)) {
				Vec3 reflectedDirection = livingOwner.position().subtract(player.getEyePosition()).normalize();
				PurgeRocketEntity reflectedRocket = new PurgeRocketEntity(level, player, reflectedDirection);
				if (level.addFreshEntity(reflectedRocket)) {
					level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getEyeY(), player.getZ(),
							10, 0.25, 0.25, 0.25, 0.12);
					continue;
				}
			}
			if (target instanceof ServerPlayer && owner instanceof ServerPlayer) {
				damage *= BattleChickens.CONFIG.peckzookaPvpDamageMultiplier();
			} else if (owner instanceof LivingEntity) {
				damage *= BattleChickens.CONFIG.enemyDamageMultiplier();
			}
			if (target instanceof ServerPlayer player && owner instanceof LivingEntity
					&& !(owner instanceof ServerPlayer)) {
				damage *= PurgeUnits.explosiveDefenseMultiplier(player);
			}
			if (damage > 0) {
				target.hurtServer(level, damageSourceFactory.create(), damage);
			}
		}
		if (directHit == null && owner instanceof net.minecraft.world.entity.animal.chicken.Chicken) {
			for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
					new AABB(impact, impact).inflate(8))) {
				double distanceSquared = player.distanceToSqr(impact);
				if (distanceSquared > SPLASH_RADIUS * SPLASH_RADIUS && distanceSquared <= 64
						&& !player.hasLineOfSight(projectile)) {
					PurgeManager.award(player, "click_beep", "click_beep");
				}
			}
		}

		if (!bunkerImpact && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			int radius = guidedDrone
					? 2
					: owner instanceof ServerPlayer
					? BattleChickens.CONFIG.rocketBlockRadius()
					: bossRocket
							? BattleChickens.CONFIG.bossTerrainRadius()
							: Math.min(3, BattleChickens.CONFIG.rocketBlockRadius());
			if (radius > 0 && (!(owner instanceof ServerPlayer player) || player.mayBuild())) {
				breakBlocks(level, impact, radius, owner);
			}
		}
		if (!bunkerImpact) {
			level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z,
					12, 0.45, 0.45, 0.45, 0.04);
			level.playSound(null, BlockPos.containing(impact), SoundEvents.FIREWORK_ROCKET_BLAST,
					SoundSource.PLAYERS, 1.0F, 0.85F);
			level.broadcastEntityEvent(projectile, (byte) 17);
		}
		projectile.discard();
	}

	private static void breakBlocks(ServerLevel level, Vec3 impact, int radius, Entity owner) {
		BlockPos center = BlockPos.containing(impact);
		ServerPlayer player = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					if (x * x + y * y + z * z > radius * radius) {
						continue;
					}
					BlockPos pos = center.offset(x, y, z);
					boolean dropItems = player != null
							|| !PurgeUnits.isBoss(owner) && !BossController.isAerialBoss(owner)
									&& !PurgeUnits.isUnit(owner);
					BunkerBlock.handleBlastDestructionAttempt(
							player, level, impact, pos, owner, true, dropItems);
				}
			}
		}
	}

	private record DamageSourceFactory(ServerLevel level, Entity projectile, Entity owner, boolean guidedDrone) {
		net.minecraft.world.damagesource.DamageSource create() {
			if (guidedDrone) {
				return level.damageSources().explosion(projectile, owner);
			}
			if (owner instanceof ServerPlayer player) {
				return level.damageSources().playerAttack(player);
			}
			if (owner instanceof LivingEntity living) {
				return level.damageSources().mobAttack(living);
			}
			return level.damageSources().fireworks((FireworkRocketEntity) projectile, owner);
		}
	}
}
