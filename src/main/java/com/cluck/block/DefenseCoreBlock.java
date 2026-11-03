package com.cluck.block;

import com.cluck.event.PurgeUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DefenseCoreBlock extends Block {
	public DefenseCoreBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state,
							net.minecraft.world.entity.LivingEntity placer,
							net.minecraft.world.item.ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.scheduleTick(pos, this, 20);
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
				new AABB(pos).inflate(20), ServerPlayer::isAlive)) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.RESISTANCE, 60, 0, true, false));
		}
		Chicken target = level.getEntitiesOfClass(Chicken.class, new AABB(pos).inflate(14),
						chicken -> PurgeUnits.isUnit(chicken) || PurgeUnits.isWarrior(chicken)).stream()
				.min(java.util.Comparator.comparingDouble(chicken -> chicken.distanceToSqr(
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
				.orElse(null);
		if (target != null) {
			Vec3 origin = Vec3.atCenterOf(pos).add(0, 0.5, 0);
			Vec3 destination = target.getBoundingBox().getCenter();
			Vec3 direction = destination.subtract(origin);
			Vec3 step = direction.normalize().scale(0.35);
			for (double distance = 0; distance < direction.length(); distance += 0.35) {
				Vec3 point = origin.add(step.scale(distance / 0.35));
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
						2, 0.03, 0.03, 0.03, 0.01);
			}
			target.hurtServer(level, level.damageSources().magic(), 12);
			level.sendParticles(ParticleTypes.END_ROD, destination.x, destination.y, destination.z,
					8, 0.1, 0.1, 0.1, 0.03);
			level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.5F, 1.8F);
		}
		level.scheduleTick(pos, this, 20);
	}
}
