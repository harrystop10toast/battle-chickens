package com.cluck.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BunkerBlock extends Block {
	private static final int CRACK_THRESHOLD = 256;
	private static final int BREAK_THRESHOLD = 1024;
	public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
	public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, BREAK_THRESHOLD - 1);

	public BunkerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(CRACKED, false).setValue(DAMAGE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CRACKED, DAMAGE);
	}

	public static boolean handleDestructionAttempt(ServerPlayer player, ServerLevel level, BlockPos pos,
			net.minecraft.world.entity.Entity owner) {
		return handleDestructionAttempt(player, level, pos, owner, 1);
	}

	public static boolean handleDestructionAttempt(ServerPlayer player, ServerLevel level, BlockPos pos,
			net.minecraft.world.entity.Entity owner, int attempts) {
		BlockState state = level.getBlockState(pos);
		if (state.is(Blocks.BEDROCK)) {
			return true;
		}
		if (!(state.getBlock() instanceof BunkerBlock)) {
			return false;
		}
		int damage = Math.min(BREAK_THRESHOLD, state.getValue(DAMAGE) + Math.max(1, attempts));
		if (damage >= BREAK_THRESHOLD) {
			level.destroyBlock(pos, false, owner);
		} else {
			level.setBlockAndUpdate(pos, state.setValue(CRACKED, damage >= CRACK_THRESHOLD)
					.setValue(DAMAGE, damage));
		}
		return true;
	}

	public static boolean handleBlastDestructionAttempt(ServerPlayer player, ServerLevel level, Vec3 origin,
			BlockPos pos, net.minecraft.world.entity.Entity owner, boolean damageBunker, boolean dropItems) {
		return handleBlastDestructionAttempt(player, level, origin, pos, owner, damageBunker ? 1 : 0, dropItems);
	}

	public static boolean handleBlastDestructionAttempt(ServerPlayer player, ServerLevel level, Vec3 origin,
			BlockPos pos, net.minecraft.world.entity.Entity owner, int bunkerDamage, boolean dropItems) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.is(Blocks.BEDROCK)) {
			return true;
		}
		if (!hasBlastLineOfSight(level, origin, pos)) {
			return true;
		}
		if (owner instanceof net.minecraft.world.entity.LivingEntity attacker
				&& !hasAttackerLineOfSight(level, attacker, pos)) {
			return true;
		}
		if (state.getBlock() instanceof BunkerBlock) {
			if (bunkerDamage > 0) {
				handleDestructionAttempt(player, level, pos, owner, bunkerDamage);
			}
			return true;
		}
		if (state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) {
			return true;
		}
		level.destroyBlock(pos, dropItems, owner);
		return true;
	}

	private static boolean hasBlastLineOfSight(ServerLevel level, Vec3 origin, BlockPos targetPos) {
		BlockPos originPos = BlockPos.containing(origin);
		if (!originPos.equals(targetPos)
				&& level.getBlockState(originPos).getBlock() instanceof BunkerBlock) {
			return false;
		}
		Vec3 target = Vec3.atCenterOf(targetPos);
		BlockHitResult hit = level.clip(new ClipContext(origin, target,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
		if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(targetPos)) {
			return true;
		}
		return false;
	}

	private static boolean hasAttackerLineOfSight(ServerLevel level,
			net.minecraft.world.entity.LivingEntity attacker, BlockPos targetPos) {
		Vec3 target = Vec3.atCenterOf(targetPos);
		BlockHitResult hit = level.clip(new ClipContext(attacker.getEyePosition(), target,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(targetPos);
	}
}
