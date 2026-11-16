package com.cluck.event;

import com.cluck.BattleChickens;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

public final class StructureLootFix {
	private static final ResourceKey<LootTable> STEVES_FRIED_CHICKEN_LOOT =
			ResourceKey.create(Registries.LOOT_TABLE, BattleChickens.id("chests/steves_fried_chicken"));

	private StructureLootFix() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide()
					|| !(level.getBlockEntity(hit.getBlockPos()) instanceof RandomizableContainerBlockEntity chest)) {
				return InteractionResult.PASS;
			}

			SeededContainerLoot loot = chest.components().get(DataComponents.CONTAINER_LOOT);
			if (loot != null && STEVES_FRIED_CHICKEN_LOOT.equals(loot.lootTable())) {
				chest.setComponents(chest.components().filter(type -> type != DataComponents.CONTAINER_LOOT));
				chest.setLootTable(loot.lootTable());
				chest.setLootTableSeed(loot.seed());
			}
			return InteractionResult.PASS;
		});
	}
}
