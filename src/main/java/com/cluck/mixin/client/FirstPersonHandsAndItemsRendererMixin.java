package com.cluck.mixin.client;

import com.cluck.client.FirstPersonRecoil;
import com.cluck.client.SniperBeakScopeOverlay;
import com.cluck.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"))
	private void battleChickens$reduceFlameFryerWalkSway(
			float partialTick,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			PlayerRenderState playerState,
			FirstPersonHandsAndItemsRenderState handsState,
			CallbackInfo ci
	) {
		FirstPersonRecoil.setVanillaSwingSuppressed(
				FirstPersonRecoil.isTrackedWeapon(handsState.mainHandItem)
						|| FirstPersonRecoil.isTrackedWeapon(handsState.offHandItem));
		if (handsState.mainHandItem.is(ModItems.FLAME_FRYER)) {
			handsState.xBob *= 0.35F;
			handsState.yBob = handsState.viewYRot;
		}
	}

	@Inject(method = "submitHandsWithItems", at = @At("RETURN"))
	private void battleChickens$clearWeaponSwingSuppression(CallbackInfo ci) {
		FirstPersonRecoil.setVanillaSwingSuppressed(false);
	}

	@ModifyVariable(
			method = "submitArmWithItem",
			at = @At("HEAD"),
			argsOnly = true,
			ordinal = 2
	)
	private float battleChickens$replaceWeaponSwingWithRecoil(float swingProgress) {
		return FirstPersonRecoil.isVanillaSwingSuppressed() ? 0.0F : swingProgress;
	}

	@Inject(method = "renderPlayerArm", at = @At("HEAD"))
	private void battleChickens$positionFlameFryerSupportHand(
			PoseStack poseStack,
			SubmitNodeCollector collector,
			int light,
			float height,
			float swingProgress,
			HumanoidArm arm,
			PlayerRenderState playerState,
			CallbackInfo ci
	) {
		if (playerState.avatarRenderState != null
				&& playerState.firstPersonHandsAndItems.mainHandItem.is(ModItems.FLAME_FRYER)
				&& arm != playerState.avatarRenderState.mainArm) {
			poseStack.translate(0.45F, 0.05F, -0.05F);
		}
	}

	@Inject(
			method = "submitArmWithItem",
			at = @At("HEAD"),
			cancellable = true
	)
	private void battleChickens$hideBeak47WhileAiming(
			PlayerRenderState playerState,
			FirstPersonHandsAndItemsRenderState handsState,
			float partialTick,
			float pitch,
			InteractionHand hand,
			float swingProgress,
			ItemStack itemStack,
			float height,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			int light,
			CallbackInfo ci
	) {
		if (SniperBeakScopeOverlay.isAiming()) {
			ci.cancel();
			return;
		}

		if (hand == InteractionHand.MAIN_HAND
				&& itemStack.is(ModItems.BEAK_47)
				&& playerState.avatarRenderState != null
				&& playerState.avatarRenderState.isCrouching) {
			ci.cancel();
			return;
		}

		if (FirstPersonRecoil.isTrackedWeapon(itemStack)) {
			FirstPersonRecoil.setCurrentRenderItem(itemStack);
			poseStack.pushPose();
			FirstPersonRecoil.apply(itemStack, poseStack);
		}
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void battleChickens$restoreRecoilPose(
			PlayerRenderState playerState,
			FirstPersonHandsAndItemsRenderState handsState,
			float partialTick,
			float pitch,
			InteractionHand hand,
			float swingProgress,
			ItemStack itemStack,
			float height,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			int light,
			CallbackInfo ci
	) {
		boolean itemHidden = SniperBeakScopeOverlay.isAiming()
				|| (hand == InteractionHand.MAIN_HAND
						&& itemStack.is(ModItems.BEAK_47)
						&& playerState.avatarRenderState != null
						&& playerState.avatarRenderState.isCrouching);
		if (!itemHidden && FirstPersonRecoil.isTrackedWeapon(itemStack)) {
			poseStack.popPose();
		}
		FirstPersonRecoil.clearCurrentRenderItem();
	}
}
