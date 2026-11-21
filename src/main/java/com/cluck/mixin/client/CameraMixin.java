package com.cluck.mixin.client;

import com.cluck.client.SniperBeakScopeOverlay;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
	private void battleChickens$zoomSniperScope(float partialTick, CallbackInfoReturnable<Float> callback) {
		if (!SniperBeakScopeOverlay.isAiming()) {
			return;
		}

		double halfFovRadians = Math.toRadians(callback.getReturnValue() / 2.0);
		double zoomedFov = 2.0 * Math.atan(Math.tan(halfFovRadians) / 3.0);
		callback.setReturnValue((float) Math.toDegrees(zoomedFov));
	}
}
