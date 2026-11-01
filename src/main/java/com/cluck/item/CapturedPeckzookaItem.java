package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.projectile.PurgeRocketEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class CapturedPeckzookaItem extends Item {
	public CapturedPeckzookaItem(Properties properties) {
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
		if (!hasRocket(serverPlayer)) {
			serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("You need a Dud Rocket to fire the Peckzooka."));
			return InteractionResult.FAIL;
		}
		PurgeRocketEntity rocket = new PurgeRocketEntity(level, serverPlayer);
		if (!level.addFreshEntity(rocket)) {
			serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("The rocket could not be launched."));
			return InteractionResult.FAIL;
		}
		consumeRocket(serverPlayer);
		serverPlayer.getCooldowns().addCooldown(BattleChickens.id("captured_peckzooka"), 80);
		ItemStack weapon = serverPlayer.getItemInHand(hand);
		weapon.hurtAndBreak(1, serverPlayer, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
	}

	private static boolean hasRocket(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(ModItems.DUD_ROCKET)) {
				return true;
			}
		}
		return false;
	}

	private static void consumeRocket(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack item = player.getInventory().getItem(slot);
			if (item.is(ModItems.DUD_ROCKET)) {
				item.shrink(1);
				return;
			}
		}
	}
}
