package com.cluck.entity;

import com.cluck.projectile.PurgeRocketEntity;
import com.cluck.event.PurgeUnits;
import com.cluck.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PurgeDrone extends Projectile implements ItemSupplier {
	private static final int MAX_FLIGHT_TICKS = 100;
	private LivingEntity target;
	private Vec3 aimPoint;
	private int flightTicks;

	public PurgeDrone(EntityType<? extends PurgeDrone> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	public void launch(LivingEntity shooter, LivingEntity target) {
		this.target = target;
		this.aimPoint = target instanceof ServerPlayer player && shooter.level() instanceof ServerLevel serverLevel
				? PurgeUnits.bossAimPosition(serverLevel, player)
				: target.getBoundingBox().getCenter();
		setOwner(shooter);
		setCustomName(net.minecraft.network.chat.Component.literal("Purge Drone"));
		setPos(shooter.getX(), shooter.getEyeY() - 0.15, shooter.getZ());
		Vec3 direction = aimPoint.subtract(position()).normalize();
		setDeltaMovement(direction.scale(0.65));
		setYRot((float) (Math.atan2(-direction.x, direction.z) * 180.0 / Math.PI));
		setXRot((float) (Math.atan2(-direction.y,
				Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * 180.0 / Math.PI));
		level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
				SoundSource.HOSTILE, 1.0F, 0.8F);
	}

	public void intercept(ServerLevel level) {
		level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10,
				0.2, 0.2, 0.2, 0.12);
		PurgeUnits.drop(level, position(), ModItems.SCRAP_CIRCUIT, 1 + level.getRandom().nextInt(2));
		discard();
	}

	@Override
	public ItemStack getItem() {
		return Items.FIREWORK_ROCKET.getDefaultInstance();
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			ProjectileUtil.rotateTowardsMovement(this, 0.2F);
			return;
		}
		ServerLevel serverLevel = (ServerLevel) level();
		if (target == null || !target.isAlive() || target.level() != level()) {
			discard();
			return;
		}

		if (!target.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
			aimPoint = target.getBoundingBox().getCenter();
		}
		Vec3 toTarget = aimPoint.subtract(position());
		if (toTarget.lengthSqr() > 0) {
			Vec3 direction = toTarget.normalize();
			Vec3 current = getDeltaMovement();
			double speed = Math.max(0.65, Math.min(1.05, current.length() + 0.025));
			Vec3 momentum;
			if (current.lengthSqr() == 0) {
				momentum = direction.scale(speed);
			} else {
				Vec3 currentDirection = current.normalize();
				double turn = currentDirection.dot(direction) < -0.4 ? 1.0 : 0.22;
				momentum = currentDirection.scale(1 - turn).add(direction.scale(turn))
						.normalize().scale(speed);
			}
			setDeltaMovement(momentum);
		}

		Vec3 movement = getDeltaMovement();
		HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		move(MoverType.SELF, movement);
		applyEffectsFromBlocks();
		ProjectileUtil.rotateTowardsMovement(this, 0.2F);
		if (hit.getType() != HitResult.Type.MISS) {
			hitTargetOrDeflectSelf(hit);
		}
		if (!isAlive()) {
			return;
		}

		flightTicks++;
		if (flightTicks >= MAX_FLIGHT_TICKS) {
			detonate(position(), null, null);
			return;
		}
		if (flightTicks % 2 == 0) {
			serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1,
					0.02, 0.02, 0.02, 0.01);
			serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1,
					0.04, 0.04, 0.04, 0.01);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel) {
			detonate(hit.getLocation(), hit.getEntity(), null);
		} else {
			discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		if (level() instanceof ServerLevel) {
			detonate(hit.getLocation(), null, hit.getBlockPos());
		} else {
			discard();
		}
	}

	private void detonate(Vec3 impact, Entity directHit, net.minecraft.core.BlockPos blockImpact) {
		if (level() instanceof ServerLevel serverLevel) {
			PurgeRocketEntity.detonateDrone(serverLevel, impact, directHit, blockImpact,
					this, getOwner(), target);
		} else {
			discard();
		}
	}
}
