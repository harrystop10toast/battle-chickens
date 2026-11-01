package com.cluck.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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
		give(serverPlayer, new ItemStack(Items.COOKED_CHICKEN, 8));
		give(serverPlayer, new ItemStack(Items.ARROW, 16));
		give(serverPlayer, new ItemStack(ModItems.DUD_ROCKET, 2));
		serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Rations supplied: food, arrows, and Dud Rockets."));
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
