package com.cluck.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ShieldGeneratorGolem extends IronGolem {
	public ShieldGeneratorGolem(EntityType<? extends IronGolem> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return target instanceof Player && super.canAttack(target);
	}
}
