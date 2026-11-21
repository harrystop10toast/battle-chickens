package com.cluck.mixin.client;

import com.cluck.item.ModItems;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	@Inject(
			method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
			at = @At("HEAD")
	)
	private void battleChickens$holdWeaponsWithBothHands(AvatarRenderState state, CallbackInfo ci) {
		if (isTwoHandedWeaponHeld(state)) {
			state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
			state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
		}
	}

	@Inject(
			method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
			at = @At("TAIL")
	)
	private void battleChickens$applyWeaponHoldPoses(AvatarRenderState state, CallbackInfo ci) {
		if (state.isVisuallySwimming && state.swimAmount == 0.0F && isSniperBeak(state)) {
			PlayerModel model = (PlayerModel) (Object) this;
			model.head.xRot = -1.2217305F;
			AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head,
					state.mainArm == HumanoidArm.RIGHT);
			model.rightArm.xRot = (float) Math.PI;
			model.leftArm.xRot = (float) Math.PI;
			model.rightArm.zRot = 0.45F;
			model.leftArm.zRot = -0.45F;
			return;
		}

		boolean inMainHand = isTwoHandedWeapon(state.getMainHandItemStack());
		if (!inMainHand && !isTwoHandedWeapon(getOffHandItemStack(state))) {
			return;
		}

		PlayerModel model = (PlayerModel) (Object) this;
		HumanoidArm itemArmSide = inMainHand ? state.mainArm : state.mainArm.getOpposite();
		boolean itemArmOnRight = itemArmSide == HumanoidArm.RIGHT;
		var itemArm = itemArmOnRight ? model.rightArm : model.leftArm;
		var supportArm = itemArmOnRight ? model.leftArm : model.rightArm;
		ItemStack heldWeapon = inMainHand ? state.getMainHandItemStack() : getOffHandItemStack(state);
		if (heldWeapon.is(ModItems.BEAK_47) || heldWeapon.is(ModItems.SNIPER_BEAK)) {
			AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, itemArmOnRight);
			return;
		}
		boolean isPeckzooka = heldWeapon.is(ModItems.CAPTURED_PECKZOOKA);

		itemArm.xRot = isPeckzooka ? -1.5707964F : -0.45F;
		itemArm.yRot = itemArmOnRight ? -0.20F : 0.20F;
		itemArm.zRot = itemArmOnRight ? 0.18F : -0.18F;
		supportArm.xRot = isPeckzooka ? -1.0F : -0.45F;
		supportArm.yRot = itemArmOnRight ? 0.50F : -0.50F;
		supportArm.zRot = itemArmOnRight ? 0.75F : -0.75F;
	}

	private static boolean isSniperBeak(AvatarRenderState state) {
		return state.getMainHandItemStack().is(ModItems.SNIPER_BEAK);
	}

	private static boolean isTwoHandedWeaponHeld(AvatarRenderState state) {
		return isTwoHandedWeapon(state.getMainHandItemStack())
				|| isTwoHandedWeapon(getOffHandItemStack(state));
	}

	private static ItemStack getOffHandItemStack(AvatarRenderState state) {
		return state.mainArm == HumanoidArm.RIGHT ? state.leftHandItemStack : state.rightHandItemStack;
	}

	private static boolean isTwoHandedWeapon(ItemStack itemStack) {
		return itemStack.is(ModItems.FLAME_FRYER)
				|| itemStack.is(ModItems.BEAK_47)
				|| itemStack.is(ModItems.SNIPER_BEAK)
				|| itemStack.is(ModItems.CAPTURED_PECKZOOKA);
	}
}
