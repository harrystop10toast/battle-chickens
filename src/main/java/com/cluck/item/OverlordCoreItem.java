package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.block.BunkerBlock;
import com.cluck.event.OverlordCoreCooldownData;
import com.cluck.event.PurgeUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class OverlordCoreItem extends BlockItem {
	private static final int TICKS_PER_SECOND = 20;
	private static final int NORMAL_COOLDOWN_TICKS = 10 * TICKS_PER_SECOND;
	private static final int ULTIMATE_COOLDOWN_TICKS = 5 * 60 * TICKS_PER_SECOND;
	private static final int MAX_CHARGE_TICKS = 10 * TICKS_PER_SECOND;
	private static final int COOLDOWN_CHARGE_CAP_TICKS = 9 * TICKS_PER_SECOND;
	private static final int USE_DURATION_TICKS = 72_000;
	private static final int BEAM_HEIGHT = 48;
	private static final int ULTIMATE_DEPTH = 48;
	private static final double[][] ULTIMATE_OFFSETS = {
			{0, 0}, {1.5, 0}, {-1.5, 0}, {0, 1.5}, {0, -1.5}
	};

	private record Beam(double x, double z, int groundY) {
	}

	public OverlordCoreItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		return startCharging(level, player, hand);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		if (player.isShiftKeyDown()) {
			return super.useOn(context);
		}
		return startCharging(context.getLevel(), player, context.getHand());
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return USE_DURATION_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(level instanceof ServerLevel serverLevel) || !(user instanceof ServerPlayer player)) {
			return;
		}
		int heldTicks = Math.max(0, USE_DURATION_TICKS - remainingUseTicks);
		if (heldTicks % TICKS_PER_SECOND != 0) {
			return;
		}
		long now = gameTime(serverLevel);
		OverlordCoreCooldownData cooldowns = OverlordCoreCooldownData.get(serverLevel);
		boolean ultimateReady = cooldowns.ultimateCooldownRemaining(player.getUUID(), now) == 0;
		int chargeCap = ultimateReady ? MAX_CHARGE_TICKS : COOLDOWN_CHARGE_CAP_TICKS;
		int shownSeconds = Math.min(heldTicks, chargeCap) / TICKS_PER_SECOND;
		String chargeStatus = heldTicks >= chargeCap
				? (ultimateReady ? "Ultimate charged; release to fire!"
						: "Charge capped at 9s while the ultimate recharges.")
				: "Charge: " + shownSeconds + "/" + (ultimateReady ? 10 : 9) + "s";
		player.sendOverlayMessage(Component.literal("Overlord Core - " + chargeStatus));

		Beam target = targetBeam(serverLevel, player);
		double beamTop = beamTop(serverLevel, target);
		serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.x(), beamTop, target.z(),
				6, 0.5, 0.5, 0.5, 0.02);
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
		if (!(level instanceof ServerLevel serverLevel) || !(user instanceof ServerPlayer player)) {
			return false;
		}
		long now = gameTime(serverLevel);
		OverlordCoreCooldownData cooldowns = OverlordCoreCooldownData.get(serverLevel);
		long normalRemaining = cooldowns.normalCooldownRemaining(player.getUUID(), now);
		if (normalRemaining > 0) {
			sendCooldownMessage(player, normalRemaining);
			return false;
		}

		boolean ultimateReady = cooldowns.ultimateCooldownRemaining(player.getUUID(), now) == 0;
		int chargeCap = ultimateReady ? MAX_CHARGE_TICKS : COOLDOWN_CHARGE_CAP_TICKS;
		int heldTicks = Math.max(0, USE_DURATION_TICKS - timeLeft);
		int chargeTicks = Math.min(heldTicks, chargeCap);
		int chargeSeconds = chargeTicks / TICKS_PER_SECOND;
		cooldowns.startNormalCooldown(player.getUUID(), now + NORMAL_COOLDOWN_TICKS);

		if (ultimateReady && chargeTicks >= MAX_CHARGE_TICKS) {
			cooldowns.startUltimateCooldown(player.getUUID(), now + ULTIMATE_COOLDOWN_TICKS);
			fireUltimate(serverLevel, player);
		} else {
			fireSonicBoom(serverLevel, player, chargeSeconds);
		}
		return true;
	}

	private InteractionResult startCharging(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			player.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		long remaining = OverlordCoreCooldownData.get(serverLevel)
				.normalCooldownRemaining(serverPlayer.getUUID(), gameTime(serverLevel));
		if (remaining > 0) {
			sendCooldownMessage(serverPlayer, remaining);
			return InteractionResult.FAIL;
		}
		serverPlayer.startUsingItem(hand);
		serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE,
				SoundSource.PLAYERS, 1.2F, 0.85F);
		return InteractionResult.CONSUME;
	}

	private static void fireSonicBoom(ServerLevel level, ServerPlayer player, int chargeSeconds) {
		Beam beam = targetBeam(level, player);
		int depth = 2 + chargeSeconds;
		double damageRadius = 1.35 * (1.0 + 0.1 * chargeSeconds);
		double blockRadius = 0.5 + 0.04 * chargeSeconds;
		double damage = 18.0 * (1.0 + 0.1 * chargeSeconds);

		sendBeam(level, beam, damageRadius, depth);
		damageAlongBeams(level, player, List.of(beam), damageRadius, depth, damage, beam);
		destroyColumn(level, player, beam, blockRadius, depth, chargeSeconds + 1);
		level.playSound(null, BlockPos.containing(beam.x(), beam.groundY(), beam.z()),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
		player.sendOverlayMessage(Component.literal(
				"Sonic boom fired. The Overlord Core recharges for 10 seconds."));
	}

	private static void fireUltimate(ServerLevel level, ServerPlayer player) {
		Beam target = targetBeam(level, player);
		List<Beam> beams = new ArrayList<>(ULTIMATE_OFFSETS.length);
		for (double[] offset : ULTIMATE_OFFSETS) {
			beams.add(beamAt(level, target.x() + offset[0], target.z() + offset[1]));
		}

		for (Beam beam : beams) {
			sendBeam(level, beam, 3.0, ULTIMATE_DEPTH);
			destroyColumn(level, player, beam, 1.75, ULTIMATE_DEPTH, 32);
		}
		damageAlongBeams(level, player, beams, 3.0, ULTIMATE_DEPTH, 70.0, target);
		level.playSound(null, BlockPos.containing(target.x(), target.groundY(), target.z()),
				SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 6.0F, 0.55F);
		player.sendOverlayMessage(Component.literal(
				"Five mega sonic booms unleashed! Ultimate cooldown: 5 minutes."));
	}

	private static void sendBeam(ServerLevel level, Beam beam, double radius, int depth) {
		int top = (int) beamTop(level, beam);
		int bottom = Math.max(level.getMinY(), beam.groundY() - depth);
		int particleCount = Math.max(3, (int) Math.ceil(radius * 1.5));
		for (int y = top; y >= bottom; y -= 2) {
			level.sendParticles(ParticleTypes.SONIC_BOOM, beam.x(), y, beam.z(),
					particleCount, radius * 0.55, 0, radius * 0.55, 0);
		}
		level.sendParticles(ParticleTypes.EXPLOSION, beam.x(), beam.groundY(), beam.z(),
				1, 0, 0, 0, 0);
	}

	private static void damageAlongBeams(ServerLevel level, ServerPlayer attacker, List<Beam> beams,
			double radius, int depth, double damagePerBoom, Beam knockbackOrigin) {
		Map<LivingEntity, Integer> hits = new HashMap<>();
		for (Beam beam : beams) {
			int top = (int) beamTop(level, beam);
			int bottom = Math.max(level.getMinY(), beam.groundY() - depth);
			AABB bounds = new AABB(beam.x() - radius, bottom, beam.z() - radius,
					beam.x() + radius, top + 1, beam.z() + radius);
			Vec3 beamOrigin = new Vec3(beam.x(), beamTop(level, beam), beam.z());
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
					entity -> entity != attacker && entity.isAlive())) {
				Vec3 center = target.getBoundingBox().getCenter();
				double dx = center.x - beam.x();
				double dz = center.z - beam.z();
				if (center.y >= bottom && center.y <= top && dx * dx + dz * dz <= radius * radius
						&& PurgeUnits.hasBlastExposure(level, beamOrigin, target)) {
					hits.merge(target, 1, Integer::sum);
				}
			}
		}

		for (Map.Entry<LivingEntity, Integer> hit : hits.entrySet()) {
			LivingEntity target = hit.getKey();
			double damage = damagePerBoom * hit.getValue();
			if (target instanceof Player) {
				damage *= BattleChickens.CONFIG.peckzookaPvpDamageMultiplier();
			}
			if (damage <= 0 || !target.hurtServer(level, level.damageSources().sonicBoom(attacker),
					(float) damage)) {
				continue;
			}
			Vec3 center = target.getBoundingBox().getCenter();
			Vec3 knockback = new Vec3(center.x - knockbackOrigin.x(), 0, center.z - knockbackOrigin.z());
			if (knockback.lengthSqr() < 0.001) {
				Vec3 look = attacker.getLookAngle();
				knockback = new Vec3(look.x, 0, look.z);
			}
			if (knockback.lengthSqr() > 0.001) {
				knockback = knockback.normalize();
			}
			target.setDeltaMovement(target.getDeltaMovement().add(knockback.scale(1.0).add(0, 0.45, 0)));
		}
	}

	private static void destroyColumn(ServerLevel level, ServerPlayer player, Beam beam,
			double radius, int depth, int bunkerDamage) {
		if (!player.mayBuild()) {
			return;
		}
		double boundsRadius = radius * 1.35;
		int minX = (int) Math.floor(beam.x() - boundsRadius);
		int maxX = (int) Math.floor(beam.x() + boundsRadius);
		int minZ = (int) Math.floor(beam.z() - boundsRadius);
		int maxZ = (int) Math.floor(beam.z() + boundsRadius);
		long seed = level.getGameTime() ^ player.getUUID().getMostSignificantBits()
				^ player.getUUID().getLeastSignificantBits()
				^ BlockPos.containing(beam.x(), beam.groundY(), beam.z()).asLong();
		Random random = new Random(seed);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				double dx = x + 0.5 - beam.x();
				double dz = z + 0.5 - beam.z();
				double distanceSqr = dx * dx + dz * dz;
				double columnRadius = radius * (0.7 + random.nextDouble() * 0.6);
				if (distanceSqr > columnRadius * columnRadius) {
					continue;
				}
				int columnDepth = Math.max(1, (int) Math.round(depth * (0.75 + random.nextDouble() * 0.5)));
				double edgeDistance = Math.sqrt(distanceSqr) / columnRadius;
				int bottom = Math.max(level.getMinY(), beam.groundY() - columnDepth);
				boolean bunkerShielded = false;
				for (int y = beam.groundY() - 1; y >= bottom; y--) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					if (state.getBlock() instanceof BunkerBlock) {
						BunkerBlock.handleDestructionAttempt(null, level, pos, player, bunkerDamage);
						bunkerShielded = true;
						continue;
					}
					if (bunkerShielded) {
						continue;
					}
					if (edgeDistance > 0.6 && random.nextDouble() < 0.25) {
						continue;
					}
					if (BunkerBlock.handleDestructionAttempt(null, level, pos, player)
							|| state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) {
						continue;
					}
					level.destroyBlock(pos, false, player);
				}
			}
		}
	}

	private static Beam targetBeam(ServerLevel level, ServerPlayer player) {
		BlockPos target = BlockPos.containing(player.pick(128, 0, false).getLocation());
		return beamAt(level, target.getX() + 0.5, target.getZ() + 0.5);
	}

	private static Beam beamAt(ServerLevel level, double x, double z) {
		int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
		groundY = Math.max(level.getMinY() + 1, Math.min(groundY, level.getMaxY() - 1));
		return new Beam(x, z, groundY);
	}

	private static double beamTop(ServerLevel level, Beam beam) {
		return Math.min(level.getMaxY() - 1, beam.groundY() + BEAM_HEIGHT);
	}

	private static long gameTime(ServerLevel level) {
		return level.getServer().overworld().getGameTime();
	}

	private static void sendCooldownMessage(ServerPlayer player, long remainingTicks) {
		long seconds = (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
		player.sendOverlayMessage(Component.literal(
				"The Overlord Core is recharging for " + seconds + " seconds."));
	}
}
