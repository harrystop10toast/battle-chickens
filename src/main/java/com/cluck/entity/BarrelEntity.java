package com.cluck.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.function.Consumer;

public final class BarrelEntity extends GrenadeEntity {
	private static final EntityDataAccessor<Float> Y_SPIN_RATE =
			SynchedEntityData.defineId(BarrelEntity.class, EntityDataSerializers.FLOAT);
	private Consumer<Vec3> impactHandler;
	private boolean impactHandled;

	public BarrelEntity(EntityType<? extends BarrelEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(Y_SPIN_RATE, 0.0F);
	}

	@Override
	public void launch(LivingEntity thrower, LivingEntity target, int flightTicks) {
		Vec3 velocity = target.getDeltaMovement();
		Vec3 head = target.getEyePosition().add(
				velocity.x * flightTicks,
				target.onGround() ? 0.0 : Math.max(-2.0, Math.min(2.0, velocity.y * flightTicks)),
				velocity.z * flightTicks);
		Vec3 aim = head.add(
				(level().getRandom().nextDouble() - 0.5) * 1.4,
				(level().getRandom().nextDouble() - 0.5) * 0.6,
				(level().getRandom().nextDouble() - 0.5) * 1.4);
		launch(thrower, aim, flightTicks);
	}

	@Override
	public void launch(LivingEntity thrower, Vec3 impact, int flightTicks) {
		super.launch(thrower, impact, flightTicks);
		float spinRate = 5.0F + level().getRandom().nextFloat() * 12.0F;
		if (level().getRandom().nextBoolean()) {
			spinRate = -spinRate;
		}
		getEntityData().set(Y_SPIN_RATE, spinRate);
	}

	@Override
	public boolean shouldDetonate() {
		return hasImpacted();
	}

	public void setImpactHandler(Consumer<Vec3> impactHandler) {
		this.impactHandler = impactHandler;
	}

	public float ySpinRate() {
		return getEntityData().get(Y_SPIN_RATE);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		handleImpact();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		handleImpact();
	}

	private void handleImpact() {
		if (level().isClientSide() || impactHandled) {
			return;
		}
		impactHandled = true;
		Vec3 impact = position();
		Consumer<Vec3> handler = impactHandler;
		impactHandler = null;
		discard();
		if (handler != null) {
			handler.accept(impact);
		}
	}
}
