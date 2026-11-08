package com.cluck.entity;

import com.cluck.event.PurgeUnits;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;

public final class ArmoredTank extends Ravager {
	public ArmoredTank(EntityType<? extends ArmoredTank> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target == null || !PurgeUnits.isPurgeAlly(target)) {
			super.setTarget(target);
		}
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !PurgeUnits.isPurgeAlly(target) && super.canAttack(target);
	}
}
