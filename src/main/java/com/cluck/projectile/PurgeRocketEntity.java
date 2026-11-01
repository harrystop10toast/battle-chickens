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
			detonate(serverLevel, hit.getLocation(), null);
		} else {
			discard();
		}
	}

	private void detonate(ServerLevel level, Vec3 impact, Entity directHit) {
		Entity owner = getOwner();
		boolean bossRocket = PurgeUnits.isBoss(owner) || BossController.isAerialBoss(owner);
		double blastRadius = bossRocket ? 5.5 : SPLASH_RADIUS;
		DamageSourceFactory damageSourceFactory = new DamageSourceFactory(level, this, owner);
		AABB blast = new AABB(impact, impact).inflate(blastRadius);
		ServerPlayer reflector = null;
		float reflectedDamage = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, blast, LivingEntity::isAlive)) {
			if (target == owner || (PurgeUnits.isPurgeAlly(owner) && PurgeUnits.isPurgeAlly(target))) {
				continue;
			}
			float damage = target == directHit
					? (bossRocket ? 58 : DIRECT_DAMAGE)
					: (bossRocket ? 20 : SPLASH_DAMAGE);
			if (target instanceof ServerPlayer player && owner instanceof LivingEntity
					&& !(owner instanceof ServerPlayer) && player.getOffhandItem().is(ModItems.CLUCK_SHIELD)) {
				reflector = player;
				reflectedDamage = Math.max(reflectedDamage, damage * 0.5F);
				continue;
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
		if (reflector != null && owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
			livingOwner.hurtServer(level, level.damageSources().playerAttack(reflector), reflectedDamage);
		}
		if (directHit == null && owner instanceof net.minecraft.world.entity.animal.chicken.Chicken) {
			for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
					new AABB(impact, impact).inflate(8))) {
				double distanceSquared = player.distanceToSqr(impact);
				if (distanceSquared > SPLASH_RADIUS * SPLASH_RADIUS && distanceSquared <= 64
						&& !player.hasLineOfSight(this)) {
					PurgeManager.award(player, "click_beep", "click_beep");
				}
			}
		}

		if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			int radius = owner instanceof ServerPlayer
					? BattleChickens.CONFIG.rocketBlockRadius()
					: bossRocket
							? BattleChickens.CONFIG.bossTerrainRadius()
							: Math.min(3, BattleChickens.CONFIG.rocketBlockRadius());
			if (radius > 0 && (!(owner instanceof ServerPlayer player) || player.mayBuild())) {
				breakBlocks(level, impact, radius, owner);
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 12, 0.45, 0.45, 0.45, 0.04);
		level.playSound(null, BlockPos.containing(impact), SoundEvents.FIREWORK_ROCKET_BLAST,
				SoundSource.PLAYERS, 1.0F, 0.85F);
		level.broadcastEntityEvent(this, (byte) 17);
		discard();
	}

	private void breakBlocks(ServerLevel level, Vec3 impact, int radius, Entity owner) {
		BlockPos center = BlockPos.containing(impact);
		ServerPlayer player = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
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
					if (BunkerBlock.handleRocketHit(player, level, pos, owner)) {
						continue;
					}
					if (player != null) {
						player.gameMode.destroyBlock(pos);
					} else {
						boolean dropItems = !PurgeUnits.isBoss(owner) && !BossController.isAerialBoss(owner)
								&& !PurgeUnits.isUnit(owner);
						level.destroyBlock(pos, dropItems, owner);
					}
				}
			}
		}
	}

	private record DamageSourceFactory(ServerLevel level, FireworkRocketEntity projectile, Entity owner) {
		net.minecraft.world.damagesource.DamageSource create() {
			if (owner instanceof ServerPlayer player) {
				return level.damageSources().playerAttack(player);
			}
			if (owner instanceof LivingEntity living) {
				return level.damageSources().mobAttack(living);
			}
			return level.damageSources().fireworks(projectile, owner);
		}
	}
}
