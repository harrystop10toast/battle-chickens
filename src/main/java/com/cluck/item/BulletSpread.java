package com.cluck.item;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class BulletSpread {
	private BulletSpread() {
	}

	public static Vec3 apply(Vec3 direction, double inaccuracy, RandomSource random) {
		Vec3 forward = direction.normalize();
		Vec3 right = forward.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1.0e-6) {
			right = forward.cross(new Vec3(1, 0, 0));
		}
		right = right.normalize();
		Vec3 up = right.cross(forward).normalize();
		double horizontal = (random.nextDouble() * 2 - 1) * inaccuracy;
		double vertical = (random.nextDouble() * 2 - 1) * inaccuracy;
		return forward.add(right.scale(horizontal)).add(up.scale(vertical)).normalize();
	}
}
