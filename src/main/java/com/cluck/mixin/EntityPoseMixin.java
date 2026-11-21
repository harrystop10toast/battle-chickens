package com.cluck.mixin;

import com.cluck.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPoseMixin {
	@Inject(method = "getPose()Lnet/minecraft/world/entity/Pose;", at = @At("HEAD"), cancellable = true)
	private void battleChickens$usePronePoseWithSniperBeak(CallbackInfoReturnable<Pose> cir) {
		if ((Object) this instanceof Player player
				&& player.isShiftKeyDown()
				&& player.getMainHandItem().is(ModItems.SNIPER_BEAK)) {
			cir.setReturnValue(Pose.SWIMMING);
		}
	}

	@Inject(method = "isCrouching()Z", at = @At("HEAD"), cancellable = true)
	private void battleChickens$remainCrouchingWithSniperBeak(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player
				&& player.isShiftKeyDown()
				&& player.getMainHandItem().is(ModItems.SNIPER_BEAK)) {
			cir.setReturnValue(true);
		}
	}
}
