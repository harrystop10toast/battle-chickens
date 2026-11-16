package com.cluck.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;

public final class SkyOverlord extends Phantom {
	public SkyOverlord(EntityType<? extends SkyOverlord> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
		double forwardOffset = 0;
		if (passenger.getCustomName() != null) {
			forwardOffset = switch (passenger.getCustomName().getString()) {
				case "Sky Gunner" -> 1.2;
				case "Overlord Bombardier" -> -1.0;
				default -> 0;
			};
		}
		double radians = Math.toRadians(getYRot());
		double offsetX = -Math.sin(radians) * forwardOffset;
		double offsetZ = Math.cos(radians) * forwardOffset;
		super.positionRider(passenger, (rider, x, y, z) ->
				callback.accept(rider, x + offsetX, y, z + offsetZ));
	}
}
