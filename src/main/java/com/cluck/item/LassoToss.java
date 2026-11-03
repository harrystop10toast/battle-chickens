package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.block.BunkerBlock;
import com.cluck.event.BossController;
import com.cluck.event.PurgeUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LassoToss {
	private static final int COOLDOWN_TICKS = 200;
	private static final int BLOCK_RADIUS = 1;
	private static final double LAUNCH_SPEED = 1.35;
	private static final int MAX_FLIGHT_TICKS = 120;
	private static final Map<UUID, Volley> VOLLEYS = new HashMap<>();

	private static final class BoulderBlock {
		private final FallingBlockEntity entity;
		private Vec3 previousPosition;

		private BoulderBlock(FallingBlockEntity entity) {
			this.entity = entity;
			this.previousPosition = entity.position();
		}
	}

	private static final class Volley {
		private final ServerLevel level;
		private final ServerPlayer owner;
		private final List<BoulderBlock> blocks;
		private int age;

		private Volley(ServerLevel level, ServerPlayer owner, List<BoulderBlock> blocks) {
			this.level = level;
			this.owner = owner;
			this.blocks = blocks;
		}
	}

	private LassoToss() {
	}

	public static boolean throwBoulder(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle();
		Vec3 start = player.getEyePosition();
		Vec3 velocity = look.normalize().scale(LAUNCH_SPEED);
		Vec3 forward = new Vec3(velocity.x, 0, velocity.z).normalize();
		if (forward.lengthSqr() < 0.01) {
			forward = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
		}
		if (forward.lengthSqr() < 0.01) {
			forward = new Vec3(0, 0, 1);
		}
		Vec3 side = new Vec3(-forward.z, 0, forward.x);
		List<BoulderBlock> blocks = new ArrayList<>(9);
		for (int row = -BLOCK_RADIUS; row <= BLOCK_RADIUS; row++) {
			for (int column = -BLOCK_RADIUS; column <= BLOCK_RADIUS; column++) {
				BlockPos source = player.blockPosition().offset(row, -1, column);
				BlockState state = level.getBlockState(source);
				if (state.getBlock() instanceof BunkerBlock || state.isAir()
						|| state.getDestroySpeed(level, source) < 0
						|| level.getBlockEntity(source) != null
						|| state.getCollisionShape(level, source).isEmpty()) {
					continue;
				}
				FallingBlockEntity block = FallingBlockEntity.fall(level, source, state);
				if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
					level.setBlockAndUpdate(source, state);
				}
				block.disableDrop();
				block.setPos(start.add(side.scale(row)).add(forward.scale(column)));
				block.setDeltaMovement(velocity);
				blocks.add(new BoulderBlock(block));
			}
		}
		if (blocks.isEmpty()) {
			player.sendSystemMessage(Component.literal("The Lasso needs solid ground beneath you."));
			return false;
		}
		VOLLEYS.put(UUID.randomUUID(), new Volley(level, player, blocks));
		player.getCooldowns().addCooldown(BattleChickens.id("lasso"), COOLDOWN_TICKS);
		level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
				SoundSource.PLAYERS, 1.5F, 1.2F);
		level.sendParticles(ParticleTypes.CLOUD, start.x, start.y, start.z, 48, 2, 1, 2, 0.14);
		player.sendSystemMessage(Component.literal("You hurl a 3x3 boulder!"));
		return true;
	}

	public static void tick(MinecraftServer server) {
		Iterator<Volley> volleys = VOLLEYS.values().iterator();
		while (volleys.hasNext()) {
			Volley volley = volleys.next();
			if (volley.level.getServer() != server) {
				discard(volley);
				volleys.remove();
				continue;
			}
			if (tickVolley(volley)) {
				volleys.remove();
			}
		}
	}

	private static boolean tickVolley(Volley volley) {
		volley.age++;
		Vec3 impact = null;
		Vec3 center = Vec3.ZERO;
		int liveBlocks = 0;
		for (BoulderBlock boulderBlock : volley.blocks) {
			FallingBlockEntity block = boulderBlock.entity;
			if (block.isRemoved()) {
				impact = boulderBlock.previousPosition;
				break;
			}
			Vec3 position = block.position();
			Vec3 movement = position.subtract(boulderBlock.previousPosition);
			AABB sweptBox = block.getBoundingBox().expandTowards(movement).inflate(0.1);
			List<LivingEntity> entities = volley.level.getEntitiesOfClass(LivingEntity.class, sweptBox,
					entity -> entity != volley.owner && entity.isAlive());
			if (!entities.isEmpty()) {
				impact = entities.getFirst().position();
				break;
			}
			if (block.onGround()) {
				impact = position;
				break;
			}
			boulderBlock.previousPosition = position;
			center = center.add(position);
			liveBlocks++;
		}
		if (liveBlocks > 0) {
			center = center.scale(1.0 / liveBlocks);
			volley.level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
					8, 0.7, 0.7, 0.7, 0.03);
		}
		if (impact != null || volley.age >= MAX_FLIGHT_TICKS) {
			detonate(volley, impact == null ? center : impact);
			return true;
		}
		return false;
	}

	private static void detonate(Volley volley, Vec3 impact) {
		discard(volley);
		for (LivingEntity victim : volley.level.getEntitiesOfClass(LivingEntity.class,
				new AABB(impact, impact).inflate(5), entity -> entity != volley.owner && entity.isAlive())) {
			if (!PurgeUnits.hasBlastExposure(volley.level, impact, victim)) {
				continue;
			}
			double distance = Math.sqrt(victim.distanceToSqr(impact));
			float damage = (60 + (float) Math.max(0, 40 * (1 - distance / 7)))
					* BattleChickens.CONFIG.enemyDamageMultiplier();
			if (victim instanceof ServerPlayer player) {
				damage *= PurgeUnits.explosiveDefenseMultiplier(player);
			}
			victim.hurtServer(volley.level, volley.level.damageSources().mobAttack(volley.owner), damage);
			Vec3 knockback = victim.position().subtract(impact).normalize();
			BossController.applyDaySevenKnockback(victim, knockback.scale(1.4).add(0, 0.8, 0));
		}
		PurgeUnits.destroyBossImpact(volley.level, impact, volley.owner, false);
		volley.level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z,
				12, 2.5, 1.5, 2.5, 0.12);
		volley.level.sendParticles(ParticleTypes.CLOUD, impact.x, impact.y, impact.z,
				64, 4, 2.5, 4, 0.2);
		volley.level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
				SoundSource.PLAYERS, 6, 0.6F);
	}

	private static void discard(Volley volley) {
		volley.blocks.forEach(block -> block.entity.discard());
	}
}
