package com.cluck.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
		if (role.equals("riot_chicken")) {
			var maxHealth = getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null) {
				maxHealth.setBaseValue(maxHealth.getBaseValue() * 2);
				setHealth((float) maxHealth.getBaseValue());
			}
			var movementSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
			if (movementSpeed != null) {
				movementSpeed.setBaseValue(movementSpeed.getBaseValue() * 0.8);
			}
			var knockbackResistance = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (knockbackResistance != null) {
				knockbackResistance.setBaseValue(0.6);
			}
			if (!level.isClientSide()) {
				addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 1, true, false));
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (role.equals("riot_chicken") && isAlive() && !level().isClientSide()) {
			MobEffectInstance resistance = getEffect(MobEffects.RESISTANCE);
			if (resistance == null || resistance.getAmplifier() != 1
					|| resistance.getDuration() != -1) {
				addEffect(new MobEffectInstance(MobEffects.RESISTANCE, -1, 1, true, false));
			}
		}
	}

	public String role() {
		return role;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!role.equals("giga_chicken")
				|| !getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE)
				|| player.isShiftKeyDown()) {
			return super.mobInteract(player, hand);
		}
		if (isVehicle() || player.isPassenger()) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide()) {
			return InteractionResult.CONSUME;
		}
		return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
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
			case "ravager_assault_chicken" -> "Ravager Assault Chicken";
			case "scavenger_chicken" -> "Scavenger Chicken";
			case "gunship_gunner" -> "Gunship Gunner";
			case "overlord_rooster" -> "Overlord Rooster";
			case "overlord_chicken" -> "Overlord Chicken";
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
			case "rpg_chicken", "overlord_bombardier" ->
					new ItemStack(Items.CROSSBOW);
			case "grenadier_chicken", "bomber_general" -> new ItemStack(Items.FIRE_CHARGE);
			case "sniper_chicken" -> new ItemStack(Items.BOW);
			case "medic_chicken" -> new ItemStack(Items.SPLASH_POTION);
			case "riot_chicken" -> new ItemStack(Items.CROSSBOW);
			case "tank_general" -> new ItemStack(Items.IRON_AXE);
			case "militia_chicken", "ravager_assault_chicken", "scavenger_chicken",
					"gunship_gunner", "overlord_rooster", "summoner_general",
					"buffer_general", "outpost_guard" -> new ItemStack(Items.IRON_SWORD);
			default -> ItemStack.EMPTY;
		};
	}
}
