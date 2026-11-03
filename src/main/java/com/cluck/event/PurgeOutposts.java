package com.cluck.event;

import com.cluck.item.ModItems;
import com.cluck.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

public final class PurgeOutposts {
	private PurgeOutposts() {
	}

	public static boolean spawn(ServerPlayer player, PurgeWorldData data) {
		ServerLevel level = player.level();
		int x = player.blockPosition().getX() + (level.getRandom().nextBoolean() ? 1 : -1) * (48 + level.getRandom().nextInt(24));
		int z = player.blockPosition().getZ() + (level.getRandom().nextBoolean() ? 1 : -1) * (48 + level.getRandom().nextInt(24));
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos center = new BlockPos(x, y, z);
		if (!level.getFluidState(center.below()).isEmpty()
				|| !level.getFluidState(center).isEmpty()
				|| center.getY() <= level.getMinY() + 2
				|| center.getY() >= level.getMaxY() - 8) {
			return false;
		}

		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
					BlockPos wall = center.offset(dx, 0, dz);
					level.setBlockAndUpdate(wall, Blocks.COBBLESTONE_WALL.defaultBlockState());
					level.setBlockAndUpdate(wall.above(), Blocks.COBBLESTONE_WALL.defaultBlockState());
				}
			}
		}
		for (int index = -2; index <= 2; index++) {
			level.setBlockAndUpdate(center.offset(index, 0, -3), Blocks.OAK_PLANKS.defaultBlockState());
			level.setBlockAndUpdate(center.offset(index, 0, 3), Blocks.OAK_PLANKS.defaultBlockState());
		}

		BlockPos chestPos = center.offset(0, 0, 2);
		level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
		if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			chest.setItem(0, new ItemStack(ModItems.SCRAP_CIRCUIT, 2 + data.currentDay() / 2));
			chest.setItem(1, new ItemStack(ModItems.GOLDEN_FEATHER, 1));
			chest.setItem(2, new ItemStack(ModItems.ROCKET_CASING, 1 + data.currentDay() / 4));
			chest.setItem(3, new ItemStack(Items.ARROW, 8 + data.threat() * 4));
			if (data.currentDay() >= 4) {
				chest.setItem(4, new ItemStack(ModItems.DUD_ROCKET, 1));
			}
			chest.setItem(5, new ItemStack(ModItems.CLUCK_RADAR));
			if (data.currentDay() >= 6) {
				chest.setItem(6, new ItemStack(ModItems.EMP_DISRUPTOR));
				chest.setItem(7, new ItemStack(ModItems.SIGNAL_SCRAMBLER));
			}
			if (level.getRandom().nextFloat() < 0.1F) {
				chest.setItem(8, new ItemStack(ModItems.LASSO));
			}
			chest.setChanged();
		}

		BlockPos radarPos = center.offset(-1, 0, 0);
		BlockPos jammerPos = center.offset(0, 0, -1);
		BlockPos shieldPos = center.offset(1, 0, 0);
		level.setBlockAndUpdate(radarPos, ModItems.RADAR_COOP.defaultBlockState());
		level.setBlockAndUpdate(jammerPos, ModItems.JAMMER_COOP.defaultBlockState());
		level.setBlockAndUpdate(shieldPos, ModItems.SHIELD_COOP.defaultBlockState());
		ModItems.RADAR_COOP.startTicker(level, radarPos);
		ModItems.JAMMER_COOP.startTicker(level, jammerPos);
		ModItems.SHIELD_COOP.startTicker(level, shieldPos);

		Chicken guard = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "guard");
		if (guard != null) {
			guard.setPos(center.getX() + 0.5, center.getY(), center.getZ() - 2.5);
			guard.setCustomName(net.minecraft.network.chat.Component.literal("Outpost Guard"));
			guard.setCustomNameVisible(true);
			guard.setPersistenceRequired();
			guard.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			level.addFreshEntity(guard);
		}
		return true;
	}
}
