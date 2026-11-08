package com.cluck.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;

public final class OverlordBeast extends Ravager {
	public OverlordBeast(EntityType<? extends OverlordBeast> entityType, Level level) {
		super(entityType, level);
	}
}
