package com.cluck.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class BunkerBlock extends Block {
	public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");

	public BunkerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(CRACKED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CRACKED);
	}

	public static boolean handleRocketHit(ServerPlayer player, ServerLevel level, BlockPos pos, net.minecraft.world.entity.Entity owner) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BunkerBlock)) {
			return false;
		}
		if (state.getValue(CRACKED)) {
			if (player != null) {
				player.gameMode.destroyBlock(pos);
			} else {
				level.destroyBlock(pos, false, owner);
			}
		} else {
			level.setBlockAndUpdate(pos, state.setValue(CRACKED, true));
		}
		return true;
	}
}
