package com.cluck.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PotionEntity extends Projectile {
	private static final int MAX_FLIGHT_TICKS = 100;
	private static final int EFFECT_DURATION_TICKS = 5 * 20;
	private static final double GRAVITY = 0.04;
	private static final double AIR_RESISTANCE = 0.99;
	private static final double SPLASH_RADIUS = 1.5;
	private boolean impacted;

	public PotionEntity(EntityType<? extends PotionEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	public void launch(LivingEntity thrower, LivingEntity target, int flightTicks) {
		Vec3 targetVelocity = target.getDeltaMovement();
		Vec3 predictedTarget = target.getBoundingBox().getCenter().add(
				targetVelocity.x * flightTicks,
				target.onGround() ? 0.0 : Math.max(-2.0, Math.min(2.0, targetVelocity.y * flightTicks)),
				targetVelocity.z * flightTicks);
		launch(thrower, predictedTarget, flightTicks);
	}

	private void launch(LivingEntity thrower, Vec3 impact, int flightTicks) {
		if (flightTicks <= 0) {
			throw new IllegalArgumentException("Potion flight time must be positive");
		}
		setOwner(thrower);
		setPos(thrower.getEyePosition().add(0.0, -0.15, 0.0));
		Vec3 difference = impact.subtract(position());
		double dragDistance = 0.0;
		double gravityDistance = 0.0;
		double unitVelocity = 1.0;
		double verticalVelocity = 0.0;
		for (int tick = 0; tick < flightTicks; tick++) {
			dragDistance += unitVelocity;
			gravityDistance += verticalVelocity;
			unitVelocity *= AIR_RESISTANCE;
			verticalVelocity = verticalVelocity * AIR_RESISTANCE - GRAVITY;
		}
		setDeltaMovement(difference.x / dragDistance,
				(difference.y - gravityDistance) / dragDistance,
				difference.z / dragDistance);
		ProjectileUtil.rotateTowardsMovement(this, 0.2F);
	}

	@Override
	public void tick() {
		super.tick();
		if (impacted) {
			return;
		}
		if (tickCount >= MAX_FLIGHT_TICKS) {
			if (!level().isClientSide()) {
				splash(position());
			}
			return;
		}

		Vec3 movement = getDeltaMovement();
		boolean serverSide = !level().isClientSide();
		HitResult hit = serverSide
				? ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity)
				: null;
		move(MoverType.SELF, movement);
		if (hit != null && hit.getType() != HitResult.Type.MISS) {
			hitTargetOrDeflectSelf(hit);
		} else {
			setDeltaMovement(movement.multiply(AIR_RESISTANCE, AIR_RESISTANCE, AIR_RESISTANCE)
					.add(0, -GRAVITY, 0));
			ProjectileUtil.rotateTowardsMovement(this, 0.2F);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		splash(hit.getLocation());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		splash(hit.getLocation());
	}

	private void splash(Vec3 impact) {
		if (impacted) {
			return;
		}
		impacted = true;
		setPos(impact);
		setDeltaMovement(Vec3.ZERO);
		if (level() instanceof ServerLevel serverLevel) {
			AABB area = new AABB(impact, impact).inflate(SPLASH_RADIUS);
			serverLevel.sendParticles(ParticleTypes.SPLASH,
					impact.x, impact.y, impact.z, 12, 0.25, 0.25, 0.25, 0.05);
			for (LivingEntity target : serverLevel.getEntitiesOfClass(
					LivingEntity.class, area, LivingEntity::isAlive)) {
				if (target instanceof Chicken) {
					if (target.addEffect(new MobEffectInstance(
							MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 2, false, true))) {
						serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
								target.getX(), target.getY() + 0.6, target.getZ(),
								5, 0.2, 0.2, 0.2, 0.01);
					}
				} else if (target instanceof Player) {
					if (target.addEffect(new MobEffectInstance(
							MobEffects.POISON, EFFECT_DURATION_TICKS, 0, false, true))) {
						serverLevel.sendParticles(ParticleTypes.WITCH,
								target.getX(), target.getY() + 0.6, target.getZ(),
								5, 0.2, 0.2, 0.2, 0.01);
					}
				}
			}
		}
		discard();
	}
}
