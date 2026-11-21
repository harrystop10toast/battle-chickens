package com.cluck.mixin.client;

import com.cluck.client.FirstPersonRecoil;
import com.cluck.item.BattleTechItem;
import com.cluck.item.CapturedPeckzookaItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BattleTechItem.class, CapturedPeckzookaItem.class})
public abstract class ItemUseRecoilMixin {
	@Inject(method = "use", at = @At("RETURN"))
	private void battleChickens$startUseRecoil(
			Level level,
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> callback
	) {
		if (!level.isClientSide() || callback.getReturnValue() != InteractionResult.SUCCESS) {
			return;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (FirstPersonRecoil.isTrackedWeapon(stack)) {
			FirstPersonRecoil.trigger(stack);
		}
	}
}
