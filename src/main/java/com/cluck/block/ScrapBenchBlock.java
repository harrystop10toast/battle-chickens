package com.cluck.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

public final class ScrapBenchBlock extends CraftingTableBlock {
	public ScrapBenchBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		openCraftingMenu(level, pos, player);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		openCraftingMenu(level, pos, player);
		return InteractionResult.SUCCESS;
	}

	private static void openCraftingMenu(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide()) {
			MenuProvider provider = new SimpleMenuProvider(
					(id, inventory, menuPlayer) -> new CraftingMenu(
							id, inventory, ContainerLevelAccess.create(level, pos)),
					Component.translatable("container.crafting"));
			player.openMenu(provider);
		}
	}
}
