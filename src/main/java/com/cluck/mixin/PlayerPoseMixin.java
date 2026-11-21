package com.cluck.mixin;

import com.cluck.item.ModItems;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPoseMixin {
	@Unique
	private boolean battleChickens$wasUsingSniperPronePose;

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void battleChickens$refreshProneDimensions(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		boolean usePronePose = player.isShiftKeyDown()
				&& player.getMainHandItem().is(ModItems.SNIPER_BEAK);
		if (usePronePose != this.battleChickens$wasUsingSniperPronePose) {
			this.battleChickens$wasUsingSniperPronePose = usePronePose;
			player.refreshDimensions();
		}
	}
}
