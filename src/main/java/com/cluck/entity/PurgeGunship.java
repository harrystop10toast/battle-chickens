package com.cluck.entity;

import com.cluck.BattleChickens;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PurgeGunship extends Phantom {
	private static final double TARGET_RANGE = 128.0;
	private static final double GUNNER_HOVER_HEIGHT = 14.0;
	private static final double GUNNER_ORBIT_RADIUS = 5.0;
	private static final double GUNSHIP_SPEED = 0.72;
	private static final int CONTACT_DAMAGE_COOLDOWN = 20;
	private static final float CONTACT_DAMAGE = 12.0F;
	private int contactDamageCooldown;

	public PurgeGunship(EntityType<? extends PurgeGunship> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (contactDamageCooldown > 0) {
			contactDamageCooldown--;
		}

		ServerPlayer target = findTarget(serverLevel);
		if (target == null) {
			setTarget(null);
			setDeltaMovement(getDeltaMovement().scale(0.8));
			return;
		}

		if (hasLivingGunner()) {
			setTarget(null);
			steerToward(gunnerPosition(target), GUNSHIP_SPEED);
		} else {
			setTarget(target);
			steerToward(target.getBoundingBox().getCenter(), GUNSHIP_SPEED + 0.16);
			damageOnContact(serverLevel, target);
		}
	}

	private ServerPlayer findTarget(ServerLevel level) {
		LivingEntity currentTarget = getTarget();
		if (currentTarget instanceof ServerPlayer player && isValidTarget(player)) {
			return player;
		}
		return level.players().stream()
				.filter(this::isValidTarget)
				.min((first, second) -> Double.compare(
						distanceToSqr(first), distanceToSqr(second)))
				.orElse(null);
	}

	private boolean isValidTarget(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && player.level() == level()
				&& distanceToSqr(player) <= TARGET_RANGE * TARGET_RANGE
				&& (!player.hasEffect(MobEffects.INVISIBILITY) || distanceToSqr(player) <= 9.0);
	}

	private boolean hasLivingGunner() {
		return getPassengers().stream().anyMatch(passenger -> passenger.isAlive()
				&& passenger.getCustomName() != null
				&& passenger.getCustomName().getString().equals("Gunship Gunner"));
	}

	private Vec3 gunnerPosition(ServerPlayer target) {
		double phase = tickCount * 0.025
				+ (getUUID().getLeastSignificantBits() & 0xFFFF) * (Math.PI * 2.0 / 0x1_0000);
		double x = target.getX() + Math.cos(phase) * GUNNER_ORBIT_RADIUS;
		double z = target.getZ() + Math.sin(phase) * GUNNER_ORBIT_RADIUS;
		double y = Math.min(target.getY() + GUNNER_HOVER_HEIGHT, level().getMaxY() - 3.0);
		return new Vec3(x, y, z);
	}

	private void steerToward(Vec3 destination, double maxSpeed) {
		Vec3 offset = destination.subtract(position());
		double distance = offset.length();
		if (distance < 1.0e-4) {
			setDeltaMovement(getDeltaMovement().scale(0.6));
			return;
		}
		Vec3 desiredVelocity = offset.scale(Math.min(maxSpeed / distance, 0.08));
		Vec3 velocity = getDeltaMovement().scale(0.55).add(desiredVelocity.scale(0.45));
		if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
			velocity = velocity.normalize().scale(maxSpeed);
		}
		setDeltaMovement(velocity);
		setYRot((float) (Math.atan2(-offset.x, offset.z) * 180.0 / Math.PI));
		setYHeadRot(getYRot());
	}

	private void damageOnContact(ServerLevel level, ServerPlayer target) {
		if (contactDamageCooldown > 0
				|| !getBoundingBox().inflate(0.25).intersects(target.getBoundingBox())) {
			return;
		}
		contactDamageCooldown = CONTACT_DAMAGE_COOLDOWN;
		float damage = CONTACT_DAMAGE * (float) BattleChickens.CONFIG.enemyDamageMultiplier();
		if (target.hurtServer(level, level.damageSources().mobAttack(this), damage)) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(),
					8, 0.2, 0.2, 0.2, 0.08);
			level.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_BITE,
					SoundSource.HOSTILE, 1.0F, 0.8F);
		}
	}
}
