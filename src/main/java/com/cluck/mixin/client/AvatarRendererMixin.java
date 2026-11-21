package com.cluck.mixin.client;

import com.cluck.item.ModItems;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL")
	)
	private void battleChickens$useStillPronePose(
			Avatar avatar,
			AvatarRenderState state,
			float partialTick,
			CallbackInfo ci
	) {
		if (avatar instanceof Player player
				&& player.isShiftKeyDown()
				&& player.getMainHandItem().is(ModItems.SNIPER_BEAK)) {
			state.isCrouching = false;
			state.isVisuallySwimming = true;
			state.swimAmount = 0.0F;
		}
	}

	@Inject(
			method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
			at = @At("TAIL")
	)
	private void battleChickens$rotateSniperProneModel(
			AvatarRenderState state,
			PoseStack poseStack,
			float bodyRot,
			float partialTick,
			CallbackInfo ci
	) {
		if (state.isVisuallySwimming
				&& state.swimAmount == 0.0F
				&& state.getMainHandItemStack().is(ModItems.SNIPER_BEAK)) {
			poseStack.rotateDegrees(Axis.XP, -90.0F);
			poseStack.translate(0.0F, -1.0F, 0.3F);
		}
	}
}
