package com.cluck.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemTooltipHandler {
	private ItemTooltipHandler() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (!id.getNamespace().equals("battle-chickens")) {
				return;
			}
			String path = id.getPath();
			addTooltips(path, lines);
		});
	}

	private static void addTooltips(String path, List<Component> lines) {
		String categoryKey = "tooltip.battle-chickens." + path + ".tag";
		String descKey = "tooltip.battle-chickens." + path + ".desc";
		String usageKey = "tooltip.battle-chickens." + path + ".usage";
		String statKey = "tooltip.battle-chickens." + path + ".stat";

		Component category = Component.translatable(categoryKey);
		if (!category.getString().equals(categoryKey)) {
			lines.add(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
					.append(category.copy().withStyle(ChatFormatting.GOLD))
					.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY)));
		}

		Component desc = Component.translatable(descKey);
		if (!desc.getString().equals(descKey)) {
			lines.add(desc.copy().withStyle(ChatFormatting.AQUA));
		}

		Component usage = Component.translatable(usageKey);
		if (!usage.getString().equals(usageKey)) {
			lines.add(usage.copy().withStyle(ChatFormatting.GRAY));
		}

		Component stat = Component.translatable(statKey);
		if (!stat.getString().equals(statKey)) {
			lines.add(stat.copy().withStyle(ChatFormatting.YELLOW));
		}
	}
}
