package com.cluck.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class BulletTracer {
	private static final DustParticleOptions RED_TRAIL = new DustParticleOptions(0xD91F26, 0.7F);
	private static final DustParticleOptions RED_LASER = new DustParticleOptions(0xFF2020, 0.9F);
	private static final DustParticleOptions BLACK_BULLET = new DustParticleOptions(0x171719, 1.35F);
	private static final double SPEED_PER_TICK = 8.0;
	private static final Map<ServerLevel, List<Tracer>> ACTIVE = new IdentityHashMap<>();

	private BulletTracer() {
	}

	public static void telegraph(ServerLevel level, Vec3 start, Vec3 end) {
		double distance = start.distanceTo(end);
		if (distance < 0.01) {
			return;
		}
		int particles = Math.min(128, Math.max(2, (int) Math.ceil(distance * 2)));
		for (int index = 0; index <= particles; index++) {
			Vec3 point = start.lerp(end, index / (double) particles);
			level.sendParticles(RED_LASER, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
	}

	public static void fire(ServerLevel level, Vec3 start, Vec3 end) {
		double distance = start.distanceTo(end);
		if (distance < 0.01) {
			return;
		}
		ACTIVE.computeIfAbsent(level, ignored -> new ArrayList<>())
				.add(new Tracer(level, start, end, Math.max(1, (int) Math.ceil(distance / SPEED_PER_TICK))));
	}

	public static void tick(ServerLevel level) {
		List<Tracer> tracers = ACTIVE.get(level);
		if (tracers == null) {
			return;
		}
		for (Iterator<Tracer> iterator = tracers.iterator(); iterator.hasNext();) {
			if (iterator.next().tick()) {
				iterator.remove();
			}
		}
		if (tracers.isEmpty()) {
			ACTIVE.remove(level);
		}
	}

	private static final class Tracer {
		private final ServerLevel level;
		private final Vec3 start;
		private final Vec3 end;
		private final int duration;
		private int tick;

		private Tracer(ServerLevel level, Vec3 start, Vec3 end, int duration) {
			this.level = level;
			this.start = start;
			this.end = end;
			this.duration = duration;
		}

		private boolean tick() {
			Vec3 previous = start.lerp(end, tick / (double) duration);
			Vec3 current = start.lerp(end, ++tick / (double) duration);
			for (int step = 1; step <= 3; step++) {
				Vec3 point = previous.lerp(current, step / 3.0);
				level.sendParticles(RED_TRAIL, point.x, point.y, point.z, 1, 0, 0, 0, 0);
			}
			level.sendParticles(BLACK_BULLET, current.x, current.y, current.z, 1, 0, 0, 0, 0);
			return tick >= duration;
		}
	}
}
