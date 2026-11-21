package com.cluck.client;

import com.cluck.BattleChickens;
import com.cluck.item.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class Beak47AimOverlay {
	private static final Identifier TEXTURE = BattleChickens.id("textures/gui/aim.png");
	private static final int TEXTURE_WIDTH = 527;
	private static final int TEXTURE_HEIGHT = 473;
	private static final float RED_DOT_X = 275.7F;
	private static final float RED_DOT_Y = 133.0F;

	private Beak47AimOverlay() {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
			if (isAiming()) {
				return;
			}
			original.extractRenderState(graphics, deltaTracker);
		});
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
				BattleChickens.id("beak_47_aim_overlay"), (graphics, deltaTracker) -> {
					if (!isAiming()) {
						return;
					}

					int screenWidth = graphics.guiWidth();
					int screenHeight = graphics.guiHeight();
					float scale = screenHeight / (2.0F * (TEXTURE_HEIGHT - RED_DOT_Y));
					int drawHeight = Math.round(TEXTURE_HEIGHT * scale);
					int drawWidth = Math.round(TEXTURE_WIDTH * scale);
					int x = Math.round(screenWidth / 2.0F - RED_DOT_X * scale);
					int y = screenHeight - drawHeight;

					graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
							drawWidth, drawHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT,
							TEXTURE_WIDTH, TEXTURE_HEIGHT);
				});
	}

	private static boolean isAiming() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null
				&& minecraft.options.getCameraType().isFirstPerson()
				&& minecraft.gui.screen() == null
				&& minecraft.player.isCrouching()
				&& minecraft.player.getMainHandItem().is(ModItems.BEAK_47);
	}
}
