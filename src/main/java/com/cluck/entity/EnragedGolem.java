package com.cluck.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class EnragedGolem extends IronGolem {
	private UUID enragedTarget;

	public EnragedGolem(EntityType<? extends EnragedGolem> entityType, Level level) {
		super(entityType, level);
	}

	public void enrageOn(Player player) {
		enragedTarget = player.getUUID();
		setPersistentAngerTarget(EntityReference.of(player));
		startPersistentAngerTimer();
		super.setTarget(player);
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target instanceof Player player) {
			enrageOn(player);
		} else if (target == null && enragedTarget == null) {
			super.setTarget(target);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}

		EntityReference<LivingEntity> angerTarget = getPersistentAngerTarget();
		UUID targetId = enragedTarget != null ? enragedTarget
				: angerTarget == null ? null : angerTarget.getUUID();
		if (targetId == null) {
			return;
		}
		enragedTarget = targetId;
		setPersistentAngerTarget(EntityReference.of(targetId));
		startPersistentAngerTimer();

		Player player = level().getPlayerByUUID(targetId);
		if (player != null && player.isAlive()) {
			if (getTarget() != player) {
				super.setTarget(player);
			}
		} else if (getTarget() != null) {
			super.setTarget(null);
		}
	}
}
