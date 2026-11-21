package com.cluck.mixin.client;

import com.cluck.item.ModItems;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState.HandRenderSelection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
	@Inject(
			method = "evaluateWhichHandsToRender(Lnet/minecraft/client/player/LocalPlayer;)Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState$HandRenderSelection;",
			at = @At("RETURN"),
			cancellable = true
	)
	private static void battleChickens$showBeak47SupportHand(LocalPlayer player,
			CallbackInfoReturnable<HandRenderSelection> callback) {
		if ((player.getMainHandItem().is(ModItems.BEAK_47)
				|| player.getMainHandItem().is(ModItems.FLAME_FRYER))
				&& player.getOffhandItem().isEmpty()) {
			callback.setReturnValue(HandRenderSelection.RENDER_BOTH_HANDS);
		}
	}
}
