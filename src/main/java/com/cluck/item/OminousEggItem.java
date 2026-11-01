package com.cluck.item;

import com.cluck.event.PurgeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class OminousEggItem extends Item {
	private final int threat;

	public OminousEggItem(Properties properties, int threat) {
		super(properties);
		this.threat = threat;
	}

	@Override
	public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer && PurgeManager.start(serverPlayer, threat)) {
			ItemStack stack = player.getItemInHand(hand);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
	}
}
