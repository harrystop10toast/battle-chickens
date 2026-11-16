package com.cluck.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;

public final class OverlordBeast extends Ravager {
	public OverlordBeast(EntityType<? extends OverlordBeast> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
		double backwardOffset = passenger.getCustomName() != null
				&& passenger.getCustomName().getString().equals("Overlord Sniper") ? 1.5 : 0;
		double radians = Math.toRadians(getYRot());
		double offsetX = Math.sin(radians) * backwardOffset;
		double offsetZ = -Math.cos(radians) * backwardOffset;
		super.positionRider(passenger, (rider, x, y, z) ->
				callback.accept(rider, x + offsetX, y, z + offsetZ));
	}
}
