package com.cluck.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GrenadeEntity extends Projectile {
	private static final EntityDataAccessor<Float> SPIN_RATE =
			SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.FLOAT);
	private static final int FUSE_TICKS = 100;
	private static final double GRAVITY = 0.04;
	private static final double AIR_RESISTANCE = 0.99;
	private boolean impacted;

	public GrenadeEntity(EntityType<? extends GrenadeEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(SPIN_RATE, 0.0F);
	}

	public void launch(LivingEntity thrower, Vec3 impact, int flightTicks) {
		if (flightTicks <= 0) {
			throw new IllegalArgumentException("Grenade flight time must be positive");
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
		setSpinRate();
		ProjectileUtil.rotateTowardsMovement(this, 0.2F);
	}

	public void launch(LivingEntity thrower, LivingEntity target, int flightTicks) {
		Vec3 targetVelocity = target.getDeltaMovement();
		Vec3 predictedTarget = target.getBoundingBox().getCenter().add(
				targetVelocity.x * flightTicks,
				target.onGround() ? 0.0 : Math.max(-2.0, Math.min(2.0, targetVelocity.y * flightTicks)),
				targetVelocity.z * flightTicks);
		launch(thrower, predictedTarget, flightTicks);
	}

	private void setSpinRate() {
		float spinRate = 6.0F + level().getRandom().nextFloat() * 14.0F;
		if (level().getRandom().nextBoolean()) {
			spinRate = -spinRate;
		}
		getEntityData().set(SPIN_RATE, spinRate);
	}

	@Override
	public void tick() {
		super.tick();
		if (shouldDetonate()) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}

		Vec3 movement = getDeltaMovement();
		boolean serverSide = !level().isClientSide();
		HitResult hit = serverSide ? ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity) : null;
		move(MoverType.SELF, movement);
		if (serverSide) {
			applyEffectsFromBlocks();
		}
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
		stopAt(hit.getLocation());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		stopAt(hit.getLocation());
	}

	private void stopAt(Vec3 position) {
		setPos(position);
		setDeltaMovement(Vec3.ZERO);
		impacted = true;
	}

	public boolean shouldDetonate() {
		return impacted || tickCount >= FUSE_TICKS;
	}

	protected boolean hasImpacted() {
		return impacted;
	}

	public float spinRate() {
		return getEntityData().get(SPIN_RATE);
	}
}
