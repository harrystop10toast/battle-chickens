package com.cluck.block;

import com.cluck.event.PurgeUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

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
						PurgeUnits::isUnit).stream()
				.min(java.util.Comparator.comparingDouble(chicken -> chicken.distanceToSqr(
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
				.orElse(null);
		if (target != null) {
			target.hurtServer(level, level.damageSources().magic(), 8);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.6,
					target.getZ(), 8, 0.2, 0.25, 0.2, 0.03);
		}
		level.scheduleTick(pos, this, 40);
	}
}
