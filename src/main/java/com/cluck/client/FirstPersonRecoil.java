package com.cluck.client;

import com.cluck.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FirstPersonRecoil {
	private static final float SOUL_FLAME_X = 14F;
	private static final float SOUL_FLAME_Y = 17F;
	private static final float SOUL_FLAME_Z = 14.5F;
	private static final float SOUL_FLAME_DIRECTION_X = 0.0F;
	private static final float SOUL_FLAME_DIRECTION_Y = 1.0F;
	private static final float SOUL_FLAME_DIRECTION_Z = 0.0F;

	private record Profile(float backward, float upward, float pitch, int durationTicks) {
	}

	private static final ThreadLocal<Boolean> SUPPRESS_VANILLA_SWING = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Item> CURRENT_RENDER_ITEM = new ThreadLocal<>();
	private static Item activeItem;
	private static Profile activeProfile;
	private static long recoilStartedAt;
	private static Vec3 tubeOffsetInView;
	private static Vec3 tubeDirectionInView;

	private FirstPersonRecoil() {
	}

	public static boolean isTrackedWeapon(ItemStack stack) {
		return stack.is(ModItems.BEAK_47)
				|| stack.is(ModItems.SNIPER_BEAK)
				|| stack.is(ModItems.FLAME_FRYER)
				|| stack.is(ModItems.CAPTURED_PECKZOOKA);
	}

	public static void trigger(ItemStack stack) {
		Profile profile = profileFor(stack);
		if (profile == null) {
			return;
		}
		activeItem = stack.getItem();
		activeProfile = profile;
		recoilStartedAt = System.nanoTime();
	}

	public static void apply(ItemStack stack, PoseStack poseStack) {
		if (activeProfile == null || stack.getItem() != activeItem) {
			return;
		}
		float progress = (System.nanoTime() - recoilStartedAt)
				/ (activeProfile.durationTicks * 50_000_000.0F);
		if (progress >= 1.0F) {
			activeItem = null;
			activeProfile = null;
			return;
		}

		float amount;
		if (progress < 0.12F) {
			float kick = progress / 0.12F;
			amount = kick * kick * (3.0F - 2.0F * kick);
		} else {
			float recovery = (progress - 0.12F) / 0.88F;
			amount = 1.0F - recovery * recovery * (3.0F - 2.0F * recovery);
		}

		poseStack.translate(0.0F, activeProfile.upward * amount, activeProfile.backward * amount);
		poseStack.mulPose(new Matrix4f().rotationX((float) Math.toRadians(-activeProfile.pitch * amount)));
	}

	public static void setVanillaSwingSuppressed(boolean suppress) {
		if (suppress) {
			SUPPRESS_VANILLA_SWING.set(true);
		} else {
			SUPPRESS_VANILLA_SWING.remove();
		}
	}

	public static boolean isVanillaSwingSuppressed() {
		return SUPPRESS_VANILLA_SWING.get();
	}

	public static void setCurrentRenderItem(ItemStack stack) {
		if (stack.is(ModItems.FLAME_FRYER)) {
			CURRENT_RENDER_ITEM.set(stack.getItem());
		} else {
			CURRENT_RENDER_ITEM.remove();
		}
	}

	public static void clearCurrentRenderItem() {
		CURRENT_RENDER_ITEM.remove();
	}

	public static void cacheTubeTransform(PoseStack poseStack) {
		Minecraft minecraft = Minecraft.getInstance();
		if (CURRENT_RENDER_ITEM.get() != ModItems.FLAME_FRYER
				|| minecraft.level == null || minecraft.player == null) {
			return;
		}

		Vector3f tubePosition = modelPosition(
				SOUL_FLAME_X, SOUL_FLAME_Y, SOUL_FLAME_Z);
		Vector3f forwardPosition = modelPosition(
				SOUL_FLAME_X + SOUL_FLAME_DIRECTION_X,
				SOUL_FLAME_Y + SOUL_FLAME_DIRECTION_Y,
				SOUL_FLAME_Z + SOUL_FLAME_DIRECTION_Z);
		poseStack.last().pose().transformPosition(tubePosition);
		poseStack.last().pose().transformPosition(forwardPosition);
		Vec3 viewForward = minecraft.player.getViewVector(1.0F).normalize();
		double viewYaw = Math.toRadians(minecraft.player.getYRot());
		Vec3 viewRight = new Vec3(Math.cos(viewYaw), 0.0, Math.sin(viewYaw));
		tubeOffsetInView = toViewSpace(
				new Vec3(tubePosition.x, tubePosition.y, tubePosition.z), viewForward, viewRight);
		tubeDirectionInView = toViewSpace(new Vec3(
				forwardPosition.x - tubePosition.x,
				forwardPosition.y - tubePosition.y,
				forwardPosition.z - tubePosition.z
		), viewForward, viewRight).normalize();
	}

	private static Vector3f modelPosition(float x, float y, float z) {
		return new Vector3f(x / 16.0F - 0.5F, y / 16.0F - 0.5F, z / 16.0F - 0.5F);
	}

	public static void tickHeldTubeParticle(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null
				|| (!minecraft.player.getMainHandItem().is(ModItems.FLAME_FRYER)
						&& !minecraft.player.getOffhandItem().is(ModItems.FLAME_FRYER))) {
			return;
		}

		double bodyYaw = Math.toRadians(minecraft.player.yBodyRot);
		Vec3 bodyForward = new Vec3(-Math.sin(bodyYaw), 0.0, Math.cos(bodyYaw));
		Vec3 bodyRight = rightOf(bodyForward);
		Vec3 direction = tubeDirectionInView == null
				? bodyForward
				: fromViewSpace(tubeDirectionInView, bodyForward, bodyRight).normalize();
		Vec3 position = tubeOffsetInView == null
				? minecraft.player.getEyePosition().add(bodyForward.scale(0.55))
				: minecraft.player.getEyePosition()
						.add(fromViewSpace(tubeOffsetInView, bodyForward, bodyRight));
		var random = minecraft.level.getRandom();
		position = position.add(
				(random.nextDouble() - 0.5) * 0.02,
				(random.nextDouble() - 0.5) * 0.02,
				(random.nextDouble() - 0.5) * 0.02);
		minecraft.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
				position.x, position.y, position.z,
				direction.x * 0.025, direction.y * 0.025, direction.z * 0.025);
	}

	private static Vec3 rightOf(Vec3 forward) {
		double yaw = Math.atan2(-forward.x, forward.z);
		return new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
	}

	private static Vec3 toViewSpace(Vec3 worldVector, Vec3 forward, Vec3 right) {
		Vec3 up = forward.cross(right).normalize();
		return new Vec3(worldVector.dot(right), worldVector.dot(up), -worldVector.dot(forward));
	}

	private static Vec3 fromViewSpace(Vec3 viewVector, Vec3 forward, Vec3 right) {
		Vec3 up = forward.cross(right).normalize();
		return right.scale(viewVector.x)
				.add(up.scale(viewVector.y))
				.subtract(forward.scale(viewVector.z));
	}

	private static Profile profileFor(ItemStack stack) {
		if (stack.is(ModItems.BEAK_47)) {
			return new Profile(0.06F, 0.008F, 1.2F, 4);
		}
		if (stack.is(ModItems.FLAME_FRYER)) {
			return new Profile(0.11F, 0.018F, 2.0F, 7);
		}
		if (stack.is(ModItems.SNIPER_BEAK)) {
			return new Profile(0.20F, 0.035F, 4.0F, 13);
		}
		if (stack.is(ModItems.CAPTURED_PECKZOOKA)) {
			return new Profile(0.28F, 0.055F, 5.0F, 16);
		}
		return null;
	}
}
