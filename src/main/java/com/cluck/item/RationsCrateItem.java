package com.cluck.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

public final class RationsCrateItem extends Item {
	public RationsCrateItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		if (!serverPlayer.getAbilities().instabuild) {
			serverPlayer.getItemInHand(hand).shrink(1);
		}
		int kit = serverPlayer.getRandom().nextInt(6);
		switch (kit) {
			case 0 -> {
				give(serverPlayer, new ItemStack(Items.COOKED_CHICKEN, 12));
				give(serverPlayer, new ItemStack(Items.GOLDEN_CARROT, 4));
			}
			case 1 -> {
				give(serverPlayer, new ItemStack(Items.ARROW, 32));
				give(serverPlayer, new ItemStack(Items.COOKED_CHICKEN, 6));
			}
			case 2 -> {
				give(serverPlayer, new ItemStack(ModItems.DUD_ROCKET, 3));
				give(serverPlayer, new ItemStack(ModItems.GUNPOWDER_POUCH, 4));
			}
			case 3 -> {
				give(serverPlayer, new ItemStack(Items.GOLDEN_APPLE, 2));
				give(serverPlayer, new ItemStack(Items.COOKED_CHICKEN, 6));
			}
			case 4 -> {
				give(serverPlayer, new ItemStack(ModItems.WAR_FEATHER, 5));
				give(serverPlayer, new ItemStack(ModItems.SCRAP_PLATE, 3));
			}
			default -> {
				give(serverPlayer, new ItemStack(Items.IRON_INGOT, 8));
				give(serverPlayer, new ItemStack(Items.COOKED_CHICKEN, 6));
			}
		}
		boolean foundLasso = serverPlayer.getRandom().nextInt(30) == 0;
		if (foundLasso) {
			give(serverPlayer, new ItemStack(ModItems.LASSO));
		}
		serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"Rations supplied: " + switch (kit) {
					case 0 -> "field provisions.";
					case 1 -> "archer's ammunition.";
					case 2 -> "demolition supplies.";
					case 3 -> "emergency medical supplies.";
					case 4 -> "salvaged materials.";
					default -> "reinforcement materials.";
				} + (foundLasso ? " A rare Lasso was tucked inside!" : "")));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
		}
	}
}
