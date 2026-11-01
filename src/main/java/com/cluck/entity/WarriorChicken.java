package com.cluck.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class WarriorChicken extends Chicken {
	public WarriorChicken(EntityType<? extends Chicken> entityType, Level level) {
		super(entityType, level);
		setCustomName(Component.literal("Warrior Chicken"));
		setCustomNameVisible(true);
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		if (!level.isClientSide() && getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
			getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(12);
			setHealth(12);
		}
	}

	@Override
	public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
		WarriorChicken offspring = ModEntities.WARRIOR_CHICKEN.create(level, EntitySpawnReason.BREEDING);
		if (offspring != null) {
			offspring.setVariant(getVariant());
		}
		return offspring;
	}
}
