package com.cluck.mixin.client;

import com.cluck.client.FirstPersonRecoil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class FlameFryerItemParticleMixin {
	@Inject(
			method = "submit",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;applyTransform(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",
					shift = At.Shift.AFTER
			)
	)
	private void battleChickens$emitSoulFlameAtTube(
			PoseStack poseStack,
			SubmitNodeCollector collector,
			int light,
			int overlay,
			int tint,
			CallbackInfo ci
	) {
		FirstPersonRecoil.cacheTubeTransform(poseStack);
	}
}
