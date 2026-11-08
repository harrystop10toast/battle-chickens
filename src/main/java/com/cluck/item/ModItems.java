package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.block.BunkerBlock;
import com.cluck.block.BunkerGlassBlock;
import com.cluck.block.DefenseCoreBlock;
import com.cluck.block.PurgeCoopBlock;
import com.cluck.block.ScarecrowTurretBlock;
import com.cluck.block.ScrapBenchBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModItems {
	public static final ScrapBenchBlock SCRAP_BENCH = registerScrapBench();
	public static final Item BUNKER_BLOCK = registerBunkerBlock();
	public static final Item BUNKER_GLASS = registerBunkerGlass();
	public static final Item OMINOUS_EGG = register("ominous_egg", 1);
	public static final Item GILDED_OMINOUS_EGG = register("gilded_ominous_egg", 2);
	public static final Item CURSED_OMINOUS_EGG = register("cursed_ominous_egg", 3);
	public static final Item WARLORD_OMINOUS_EGG = register("warlord_ominous_egg", 4);
	public static final Item SINGULARITY_OMINOUS_EGG = register("singularity_ominous_egg", 5);
	public static final Item WAR_FEATHER = material("war_feather");
	public static final Item SCRAP_PLATE = material("scrap_plate");
	public static final Item SCRAP_CIRCUIT = material("scrap_circuit");
	public static final Item ROCKET_CASING = material("rocket_casing");
	public static final Item GUNPOWDER_POUCH = material("gunpowder_pouch");
	public static final Item GOLDEN_FEATHER = material("golden_feather");
	public static final Item GENERALS_PLUME = displayMaterial("generals_plume", true);
	public static final Item OVERLORD_SHARD = material("overlord_shard");
	public static final Item OVERLORD_CORE = registerDefenseCore();
	public static final Item SCARECROW_TURRET = registerScarecrowTurret();
	public static final PurgeCoopBlock RADAR_COOP = registerCoopBlock("radar_coop", PurgeCoopBlock.Mode.RADAR);
	public static final PurgeCoopBlock JAMMER_COOP = registerCoopBlock("jammer_coop", PurgeCoopBlock.Mode.JAMMER);
	public static final PurgeCoopBlock SHIELD_COOP = registerCoopBlock("shield_coop", PurgeCoopBlock.Mode.SHIELD);
	public static final Item NEMESIS_TROPHY = displayMaterial("nemesis_trophy");
	public static final Item OVERLORD_CROWN = displayMaterial("overlord_crown", true);
	public static final Item RATIONS_CRATE = registerRationsCrate();
	public static final Item DUD_ROCKET = material("dud_rocket");
	public static final Item PURGE_GRENADE = material("purge_grenade");
	public static final Item FLOCKWAR_HORN = registerFlockwarHorn();
	public static final Item CAPTURED_PECKZOOKA = registerPeckzooka();
	public static final Item CLUCK_SHIELD = registerCluckShield();
	public static final Item FEATHER_CLOAK = battleTech("feather_cloak", BattleTechItem.Ability.FEATHER_CLOAK);
	public static final Item EMP_DISRUPTOR = battleTech("emp_disruptor", BattleTechItem.Ability.EMP_DISRUPTOR);
	public static final Item EGG_CANNON = battleTech("egg_cannon", BattleTechItem.Ability.EGG_CANNON);
	public static final Item FLAK_CANNON = battleTech("flak_cannon", BattleTechItem.Ability.FLAK_CANNON);
	public static final Item BEAK_47 = battleTech("beak_47", BattleTechItem.Ability.BEAK_47);
	public static final Item ROOSTER_SPEAR = battleTech("rooster_spear", BattleTechItem.Ability.ROOSTER_SPEAR);
	public static final Item FLAME_FRYER = battleTech("flame_fryer", BattleTechItem.Ability.FLAME_FRYER);
	public static final Item SNIPER_BEAK = battleTech("sniper_beak", BattleTechItem.Ability.SNIPER_BEAK);
	public static final Item MEDIC_SYRINGE = battleTech("medic_syringe", BattleTechItem.Ability.MEDIC_SYRINGE, 16);
	public static final Item DRUMSTICK_OF_VALOR = battleTech("drumstick_of_valor", BattleTechItem.Ability.DRUMSTICK_OF_VALOR, 16);
	public static final Item CLUCK_RADAR = battleTech("cluck_radar", BattleTechItem.Ability.CLUCK_RADAR);
	public static final Item SIGNAL_SCRAMBLER = battleTech("signal_scrambler", BattleTechItem.Ability.SIGNAL_SCRAMBLER);
	public static final Item GOLDEN_EGG = battleTech("golden_egg", BattleTechItem.Ability.GIGA_CHICKEN_EGG);
	public static final Item GENERATOR_CORE = material("generator_core");
	public static final Item FLOCKGUARD_SIGIL = registerFlockguardSigil();
	public static final Item LASSO = registerLasso();

	private ModItems() {
	}

	private static ScrapBenchBlock registerScrapBench() {
		var id = BattleChickens.id("scrap_bench");
		var blockKey = ResourceKey.create(Registries.BLOCK, id);
		var block = Registry.register(BuiltInRegistries.BLOCK, id,
				new ScrapBenchBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).setId(blockKey)));
		var itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(BuiltInRegistries.ITEM, id,
				new BlockItem(block, new Item.Properties().setId(itemKey)));
		return block;
	}

	private static Item registerBunkerBlock() {
		var id = BattleChickens.id("bunker_block");
		var blockKey = ResourceKey.create(Registries.BLOCK, id);
		var block = Registry.register(BuiltInRegistries.BLOCK, id,
				new BunkerBlock(BlockBehaviour.Properties.of().strength(50.0F, 1200.0F)
						.requiresCorrectToolForDrops().setId(blockKey)));
		var itemKey = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties().setId(itemKey)));
	}

	private static Item registerBunkerGlass() {
		var id = BattleChickens.id("bunker_glass");
		var blockKey = ResourceKey.create(Registries.BLOCK, id);
		var block = Registry.register(BuiltInRegistries.BLOCK, id,
				new BunkerGlassBlock(BlockBehaviour.Properties.of().strength(25.0F, 600.0F)
						.sound(SoundType.GLASS).noOcclusion().requiresCorrectToolForDrops().setId(blockKey)));
		var itemKey = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties().setId(itemKey)));
	}

	private static Item registerDefenseCore() {
		var id = BattleChickens.id("defense_core");
		var blockKey = ResourceKey.create(Registries.BLOCK, id);
		var block = Registry.register(BuiltInRegistries.BLOCK, id,
				new DefenseCoreBlock(BlockBehaviour.Properties.of().strength(8.0F, 1200.0F)
						.sound(SoundType.METAL).setId(blockKey)));
		var itemKey = ResourceKey.create(Registries.ITEM, BattleChickens.id("overlord_core"));
		return Registry.register(BuiltInRegistries.ITEM, BattleChickens.id("overlord_core"),
				new OverlordCoreItem(block, new Item.Properties().setId(itemKey).stacksTo(1)));
	}

	private static Item registerScarecrowTurret() {
		var id = BattleChickens.id("scarecrow_turret");
		var blockKey = ResourceKey.create(Registries.BLOCK, id);
		Registry.register(BuiltInRegistries.BLOCK, id,
				new ScarecrowTurretBlock(BlockBehaviour.Properties.of().strength(2.0F)
						.sound(SoundType.WOOD).setId(blockKey)));
		var itemKey = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new ScarecrowTurretItem(new Item.Properties().setId(itemKey).stacksTo(1)));
	}

	private static PurgeCoopBlock registerCoopBlock(String name, PurgeCoopBlock.Mode mode) {
		var id = BattleChickens.id(name);
		var blockKey = ResourceKey.create(Registries.BLOCK, id);
		var block = Registry.register(BuiltInRegistries.BLOCK, id,
				new PurgeCoopBlock(BlockBehaviour.Properties.of().strength(3.0F)
						.sound(SoundType.METAL).setId(blockKey), mode));
		var itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(BuiltInRegistries.ITEM, id,
				new BlockItem(block, new Item.Properties().setId(itemKey)));
		return block;
	}

	private static Item material(String name) {
		var id = BattleChickens.id(name);
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(key)));
	}

	private static Item displayMaterial(String name) {
		return displayMaterial(name, false);
	}

	private static Item displayMaterial(String name, boolean wearable) {
		var id = BattleChickens.id(name);
		var key = ResourceKey.create(Registries.ITEM, id);
		var properties = new Item.Properties().setId(key).stacksTo(1);
		if (wearable) {
			properties.equippable(EquipmentSlot.HEAD);
		}
		return Registry.register(BuiltInRegistries.ITEM, id,
				new TrophyDisplayItem(properties, wearable));
	}

	private static Item register(String name, int threat) {
		var id = BattleChickens.id(name);
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new OminousEggItem(new Item.Properties().setId(key), threat));
	}

	private static Item registerPeckzooka() {
		var id = BattleChickens.id("captured_peckzooka");
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new CapturedPeckzookaItem(new Item.Properties().setId(key).stacksTo(1).durability(120)));
	}

	private static Item registerRationsCrate() {
		var id = BattleChickens.id("rations_crate");
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new RationsCrateItem(new Item.Properties().setId(key).stacksTo(1)));
	}

	private static Item registerFlockwarHorn() {
		var id = BattleChickens.id("flockwar_horn");
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new FlockwarHornItem(new Item.Properties().setId(key).stacksTo(1)));
	}

	private static Item registerFlockguardSigil() {
		var id = BattleChickens.id("flockguard_sigil");
		var key = ResourceKey.create(Registries.ITEM, id);
		var modifiers = net.minecraft.world.item.component.ItemAttributeModifiers.builder()
				.add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
						new net.minecraft.world.entity.ai.attributes.AttributeModifier(
								BattleChickens.id("flockguard_sigil_knockback_resistance"), 1.0,
								net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.HAND)
				.build();
		return Registry.register(BuiltInRegistries.ITEM, id,
				new Item(new Item.Properties().setId(key).stacksTo(1).attributes(modifiers)));
	}

	private static Item registerCluckShield() {
		var id = BattleChickens.id("cluck_shield");
		var key = ResourceKey.create(Registries.ITEM, id);
		var modifiers = net.minecraft.world.item.component.ItemAttributeModifiers.builder()
				.add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
						new net.minecraft.world.entity.ai.attributes.AttributeModifier(
								BattleChickens.id("cluck_shield_knockback_resistance"), 0.9,
								net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
						net.minecraft.world.entity.EquipmentSlotGroup.HAND)
				.build();
		return Registry.register(BuiltInRegistries.ITEM, id,
				new BattleTechItem(new Item.Properties().setId(key).stacksTo(1).attributes(modifiers),
						BattleTechItem.Ability.CLUCK_SHIELD));
	}

	private static Item registerLasso() {
		var id = BattleChickens.id("lasso");
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new LassoItem(new Item.Properties().setId(key).stacksTo(1)));
	}

	private static Item battleTech(String name, BattleTechItem.Ability ability) {
		return battleTech(name, ability, 1);
	}

	private static Item battleTech(String name, BattleTechItem.Ability ability, int maxCount) {
		var id = BattleChickens.id(name);
		var key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, id,
				new BattleTechItem(new Item.Properties().setId(key).stacksTo(maxCount), ability));
	}

	public static void register() {
		var ingredients = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"));
		CreativeModeTabEvents.modifyOutputEvent(ingredients).register(output -> {
			output.accept(SCRAP_BENCH.asItem());
			output.accept(BUNKER_BLOCK);
			output.accept(BUNKER_GLASS);
			output.accept(OMINOUS_EGG);
			output.accept(GILDED_OMINOUS_EGG);
			output.accept(CURSED_OMINOUS_EGG);
			output.accept(WARLORD_OMINOUS_EGG);
			output.accept(SINGULARITY_OMINOUS_EGG);
			output.accept(WAR_FEATHER);
			output.accept(SCRAP_PLATE);
			output.accept(SCRAP_CIRCUIT);
			output.accept(ROCKET_CASING);
			output.accept(GUNPOWDER_POUCH);
			output.accept(GOLDEN_FEATHER);
			output.accept(GENERALS_PLUME);
			output.accept(OVERLORD_SHARD);
			output.accept(OVERLORD_CORE);
			output.accept(SCARECROW_TURRET);
			output.accept(RADAR_COOP.asItem());
			output.accept(JAMMER_COOP.asItem());
			output.accept(SHIELD_COOP.asItem());
			output.accept(NEMESIS_TROPHY);
			output.accept(OVERLORD_CROWN);
			output.accept(RATIONS_CRATE);
			output.accept(DUD_ROCKET);
			output.accept(FLOCKWAR_HORN);
			output.accept(CAPTURED_PECKZOOKA);
			output.accept(CLUCK_SHIELD);
			output.accept(FEATHER_CLOAK);
			output.accept(EMP_DISRUPTOR);
			output.accept(EGG_CANNON);
			output.accept(FLAK_CANNON);
			output.accept(BEAK_47);
			output.accept(ROOSTER_SPEAR);
			output.accept(FLAME_FRYER);
			output.accept(SNIPER_BEAK);
			output.accept(MEDIC_SYRINGE);
			output.accept(DRUMSTICK_OF_VALOR);
			output.accept(CLUCK_RADAR);
			output.accept(SIGNAL_SCRAMBLER);
			output.accept(GOLDEN_EGG);
			output.accept(GENERATOR_CORE);
			output.accept(FLOCKGUARD_SIGIL);
			output.accept(LASSO);
		});
	}
}
