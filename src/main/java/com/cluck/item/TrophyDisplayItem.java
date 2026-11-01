package com.cluck.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class TrophyDisplayItem extends Item {
	private final boolean wearable;

	public TrophyDisplayItem(Properties properties) {
		this(properties, false);
	}

	public TrophyDisplayItem(Properties properties, boolean wearable) {
		super(properties);
		this.wearable = wearable;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!wearable) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		ItemStack held = serverPlayer.getItemInHand(hand);
		ItemStack oldHelmet = serverPlayer.getItemBySlot(EquipmentSlot.HEAD);
		if (!oldHelmet.isEmpty()) {
			ItemStack displaced = oldHelmet.copy();
			if (!serverPlayer.getInventory().add(displaced)) {
				level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), displaced));
			}
		}
		serverPlayer.setItemSlot(EquipmentSlot.HEAD, held.copyWithCount(1));
		if (!serverPlayer.getAbilities().instabuild) {
			held.shrink(1);
		}
		serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				held.is(ModItems.OVERLORD_CROWN)
						? "The Overlord's Crown grants a strength aura to nearby allies."
						: "The General's Plume boosts your strength near Purge chickens."));
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}
		if (!player.mayBuild()) {
			return InteractionResult.FAIL;
		}
		Direction facing = context.getClickedFace();
		BlockPos displayPos = context.getClickedPos().relative(facing);
		ItemFrame frame = new ItemFrame(context.getLevel(), displayPos, facing);
		frame.setItem(context.getItemInHand().copyWithCount(1));
		if (!context.getLevel().addFreshEntity(frame)) {
			return InteractionResult.FAIL;
		}
		if (!player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}
}
