package com.cluck.client;

import com.cluck.BattleChickens;
import com.cluck.item.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class SniperBeakScopeOverlay {
	private static final Identifier TEXTURE = BattleChickens.id("textures/gui/scope.png");
	private static final int TEXTURE_WIDTH = 367;
	private static final int TEXTURE_HEIGHT = 370;
	private static final Identifier[] HUD_ELEMENTS = {
			VanillaHudElements.MISC_OVERLAYS,
			VanillaHudElements.CROSSHAIR,
			VanillaHudElements.SPECTATOR_MENU,
			VanillaHudElements.HOTBAR,
			VanillaHudElements.ARMOR_BAR,
			VanillaHudElements.HEALTH_BAR,
			VanillaHudElements.FOOD_BAR,
			VanillaHudElements.AIR_BAR,
			VanillaHudElements.MOUNT_HEALTH,
			VanillaHudElements.INFO_BAR,
			VanillaHudElements.EXPERIENCE_LEVEL,
			VanillaHudElements.HELD_ITEM_TOOLTIP,
			VanillaHudElements.SPECTATOR_TOOLTIP,
			VanillaHudElements.MOB_EFFECTS,
			VanillaHudElements.BOSS_BAR,
			VanillaHudElements.DEMO_TIMER,
			VanillaHudElements.SCOREBOARD,
			VanillaHudElements.OVERLAY_MESSAGE,
			VanillaHudElements.TITLE_AND_SUBTITLE,
			VanillaHudElements.CHAT,
			VanillaHudElements.PLAYER_LIST,
			VanillaHudElements.SUBTITLES
	};

	private SniperBeakScopeOverlay() {
	}

	public static void register() {
		for (Identifier element : HUD_ELEMENTS) {
			HudElementRegistry.replaceElement(element, original -> (graphics, deltaTracker) -> {
				if (!isAiming()) {
					original.extractRenderState(graphics, deltaTracker);
				}
			});
		}
		HudElementRegistry.addLast(BattleChickens.id("sniper_beak_scope_overlay"),
				(graphics, deltaTracker) -> {
					if (!isAiming()) {
						return;
					}

					int screenWidth = graphics.guiWidth();
					int screenHeight = graphics.guiHeight();
					float scale = Math.min((float) screenWidth / TEXTURE_WIDTH,
							(float) screenHeight / TEXTURE_HEIGHT);
					int drawWidth = Math.round(TEXTURE_WIDTH * scale);
					int drawHeight = Math.round(TEXTURE_HEIGHT * scale);
					int x = (screenWidth - drawWidth) / 2;
					int y = (screenHeight - drawHeight) / 2;

					graphics.fill(0, 0, screenWidth, y, 0xFF000000);
					graphics.fill(0, y + drawHeight, screenWidth, screenHeight, 0xFF000000);
					graphics.fill(0, y, x, y + drawHeight, 0xFF000000);
					graphics.fill(x + drawWidth, y, screenWidth, y + drawHeight, 0xFF000000);
					graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
							drawWidth, drawHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT,
							TEXTURE_WIDTH, TEXTURE_HEIGHT);
				});
	}

	public static boolean isAiming() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null
				&& minecraft.options.getCameraType().isFirstPerson()
				&& minecraft.gui.screen() == null
				&& minecraft.player.isCrouching()
				&& minecraft.player.getMainHandItem().is(ModItems.SNIPER_BEAK);
	}
}
