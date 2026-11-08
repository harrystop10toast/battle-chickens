package com.cluck.client;

import com.cluck.BattleChickens;
import com.cluck.item.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.EntityHitResult;

public final class NemesisTrophyTooltip {
	private NemesisTrophyTooltip() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
				BattleChickens.id("nemesis_trophy_tooltip"), (graphics, deltaTracker) -> {
					Minecraft minecraft = Minecraft.getInstance();
					if (minecraft.gui.screen() != null || !(minecraft.hitResult instanceof EntityHitResult hit)
							|| !(hit.getEntity() instanceof ItemFrame frame)) {
						return;
					}

					var trophy = frame.getItem();
					if (!trophy.is(ModItems.NEMESIS_TROPHY)) {
						return;
					}
					ItemLore lore = trophy.get(DataComponents.LORE);
					if (lore == null || lore.lines().isEmpty()) {
						return;
					}

					graphics.setComponentTooltipForNextFrame(minecraft.font, lore.lines(),
							graphics.guiWidth() / 2 + 10, graphics.guiHeight() / 2 + 10);
				});
	}
}
