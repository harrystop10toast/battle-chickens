package com.cluck.block;

import com.cluck.event.PurgeUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;

public final class ScarecrowTurretBlock extends Block {
	public static final IntegerProperty AMMO = IntegerProperty.create("ammo", 0, 64);

	public ScarecrowTurretBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AMMO, 64));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AMMO);
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
		if (state.getValue(AMMO) <= 0) {
			level.scheduleTick(pos, this, 100);
			return;
		}
		Chicken target = level.getEntitiesOfClass(Chicken.class, new AABB(pos).inflate(12),
						PurgeUnits::isUnit).stream()
				.min(java.util.Comparator.comparingDouble(chicken -> chicken.distanceToSqr(
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
				.orElse(null);
		if (target != null) {
			target.hurtServer(level, level.damageSources().magic(), 3);
			level.setBlockAndUpdate(pos, state.setValue(AMMO, state.getValue(AMMO) - 1));
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.6,
					target.getZ(), 3, 0.1, 0.15, 0.1, 0.03);
		}
		level.scheduleTick(pos, this, 20);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.WHEAT_SEEDS) || state.getValue(AMMO) >= 64) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			int loaded = Math.min(16, 64 - state.getValue(AMMO));
			level.setBlockAndUpdate(pos, state.setValue(AMMO, state.getValue(AMMO) + loaded));
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			((ServerLevel) level).scheduleTick(pos, this, 1);
			player.sendSystemMessage(Component.literal("Scarecrow Turret loaded: "
					+ (state.getValue(AMMO) + loaded) + "/64 seed rounds."));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		player.sendSystemMessage(Component.literal("Scarecrow Turret ammunition: "
				+ state.getValue(AMMO) + "/64. Use wheat seeds to reload."));
		return InteractionResult.SUCCESS;
	}
}
