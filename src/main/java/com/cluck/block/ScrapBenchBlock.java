package com.cluck.block;

import com.cluck.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class ScrapBenchBlock extends CraftingTableBlock {
	public ScrapBenchBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider(
				(containerId, inventory, player) -> new ScrapBenchMenu(containerId, inventory,
						ContainerLevelAccess.create(level, pos)),
				Component.translatable("container.crafting"));
	}

	private static final class ScrapBenchMenu extends CraftingMenu {
		private final ContainerLevelAccess access;

		private ScrapBenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
			super(containerId, inventory, access);
			this.access = access;
		}

		@Override
		public boolean stillValid(Player player) {
			return stillValid(access, player, ModItems.SCRAP_BENCH);
		}
	}
}
