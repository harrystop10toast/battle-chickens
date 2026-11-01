package com.cluck.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class PurgeChicken extends Chicken {
	private final String role;

	public PurgeChicken(EntityType<? extends Chicken> entityType, Level level) {
		super(entityType, level);
		role = EntityType.getKey(entityType).getPath();
		setCustomName(Component.literal(displayName(role)));
		setCustomNameVisible(true);
		ItemStack weapon = weapon(role);
		if (!weapon.isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, weapon);
		}
	}

	public String role() {
		return role;
	}

	@Override
	public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
		PurgeChicken offspring = (PurgeChicken) ModEntities.createChicken(
				level, EntitySpawnReason.BREEDING, role);
		offspring.setVariant(getVariant());
		return offspring;
	}

	private static String displayName(String role) {
		return switch (role) {
			case "rpg_chicken" -> "RPG Chicken";
			case "grenadier_chicken" -> "Grenadier Hen";
			case "sniper_chicken" -> "Sniper Hen";
			case "medic_chicken" -> "Medic Hen";
			case "riot_chicken" -> "Riot Chicken";
			case "sergeant_chicken" -> "Sergeant Chicken";
			case "militia_chicken" -> "Militia Chicken";
			case "scavenger_chicken" -> "Scavenger Chicken";
			case "gunship_gunner" -> "Gunship Gunner";
			case "overlord_rooster" -> "Overlord Rooster";
			case "overlord_rpg_chicken" -> "Overlord RPG Chicken";
			case "summoner_general" -> "Summoner General";
			case "tank_general" -> "Tank General";
			case "bomber_general" -> "Bomber General";
			case "buffer_general" -> "Buffer General";
			case "overlord_bombardier" -> "Overlord Bombardier";
			case "summoned_chicken" -> "Summoned Chicken";
			case "outpost_guard" -> "Outpost Guard";
			case "giga_chicken" -> "Giga Chicken";
			default -> "Purge Chicken";
		};
	}

	private static ItemStack weapon(String role) {
		return switch (role) {
			case "rpg_chicken", "overlord_rpg_chicken", "overlord_bombardier" ->
					new ItemStack(Items.CROSSBOW);
			case "grenadier_chicken", "bomber_general" -> new ItemStack(Items.FIRE_CHARGE);
			case "sniper_chicken" -> new ItemStack(Items.BOW);
			case "medic_chicken" -> new ItemStack(Items.SPLASH_POTION);
			case "riot_chicken", "tank_general" -> new ItemStack(Items.IRON_AXE);
			case "sergeant_chicken", "militia_chicken", "scavenger_chicken",
					"gunship_gunner", "overlord_rooster", "summoner_general",
					"buffer_general", "outpost_guard" -> new ItemStack(Items.IRON_SWORD);
			default -> ItemStack.EMPTY;
		};
	}
}
