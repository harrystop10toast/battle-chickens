# Chicken Overlord: The Great Purge

**Fabric Mod for Minecraft 26.3 | Java 25**

*Chicken Overlord: The Great Purge* is an intense apocalyptic survival mod where Minecraft's most unassuming creatures—chickens—reveal their true nature as an organized, militarized empire. Craft an Ominous Egg to awaken the Great Purge, survive a 7-day escalating siege of weaponized poultry, raid fortified outposts, scavenge battle-tech blueprints, and face down two gargantuan bosses on the Day 7 Doomsday.

---

## Table of Contents
1. [Overview & Core Gameplay](#overview--core-gameplay)
2. [Threat Levels & Progression](#threat-levels--progression)
   - [Linear Progression & Item Guide](#linear-progression--item-guide)
3. [Day 7 Doomsday Boss Fight](#day-7-doomsday-boss-fight)
   - [The Overlord (Ground Boss)](#the-overlord-ground-boss)
   - [The Sky Overlord (Aerial Boss)](#the-sky-overlord-aerial-boss)
   - [Shield Generator Golems & Resistance Scaling](#shield-generator-golems--resistance-scaling)
   - [Teleportation Pursuit Mechanic](#teleportation-pursuit-mechanic)
   - [Friendly Fire Dynamics](#friendly-fire-dynamics)
4. [Complete Item Compendium](#complete-item-compendium)
   - [Ominous Eggs](#ominous-eggs)
   - [Weapons & Ordnance](#weapons--ordnance)
   - [Defensive Armor & Shields](#defensive-armor--shields)
   - [Tactical Utilities & Consumables](#tactical-utilities--consumables)
   - [Salvage & Crafting Materials](#salvage--crafting-materials)
   - [Defensive Structures & Blocks](#defensive-structures--blocks)
   - [Mounts & Companions](#mounts--companions)
5. [Military Chicken Unit Roster](#military-chicken-unit-roster)
6. [Commands](#commands)
7. [Configuration](#configuration)
8. [Installation & Building](#installation--building)

---

## Overview & Core Gameplay

The Purge is a world-wide 7-day event triggered by using an **Ominous Egg**. Each Minecraft day lasts 24,000 ticks (20 real-time minutes).

- **Escalating Threat**: Rather than fixed daily waves, naturally spawned chickens near players have an increasing chance to convert into armed hostile military units.
- **Daily Rations**: At each sunrise, surviving players receive an emergency **Rations Crate** containing food, ammunition, and medical kits (except on Hardcore or Threat IV/V).
- **Salvage Outposts**: From Day 4 until Day 7, fortified chicken outposts appear in the world. Defeating the Outpost Guard scatters nearby squads and yields rare materials from the outpost cache.
- **Blueprints & Tech Tree**: Personally defeating a new Purge unit teaches its blueprint and unlocks its associated recipes. Craft at any crafting table; the **Scrap Bench** is a reusable crafting-table block.
- **Meltdown Timer**: On Day 7, the Overlords arrive. Players have one full in-game day (1200 seconds / 20 minutes) to eliminate both bosses before meltdown initiates.

---

## Threat Levels & Progression

Craft the first Ominous Egg in a shaped recipe; the higher-threat eggs use shapeless recipes:

| Threat Level | Crafting Recipe | Key Mechanics |
| :--- | :--- | :--- |
| **Threat I** | 4 Feathers + Rotten Flesh + 2 Gold Ingots + Egg + Bell | Starts the standard Purge. |
| **Threat II** | Ominous Egg + Gold Block + Netherite Scrap | Stronger enemies and improved salvage. |
| **Threat III** | Ominous Egg + Ominous Bottle + Nether Star | Stronger enemies; Phantom Gunships begin on Day 4. |
| **Threat IV** | Cursed Ominous Egg + Blaze Rod + Golden Feather | Requires Flockbreaker Rank III. Rations are disabled. |
| **Threat V** | Warlord Ominous Egg + Nether Star + Overlord Shard | Requires Flockbreaker Rank IV; the highest threat. |

Threat II and III are separate upgrades from the base Ominous Egg in the current recipes: the Cursed Ominous Egg does not consume a Gilded Ominous Egg. Use an egg on the ground to start its matching Purge. Threat IV and V are rank-gated when used, even if crafted.

### Linear Progression & Item Guide

The usual route is to prepare basic supplies, start a Threat I Purge, fight progressively stronger units for salvage and recipe blueprints, raid the Day 4+ outpost, then defeat the Day 7 bosses. Most weapons and defenses are not available in the recipe book until **you personally kill** the matching unit. Recipes can be made in any crafting grid; the Scrap Bench is optional.

#### 1. Prepare and start the Purge

Craft a **Scrap Bench** (4 Iron Ingots, 4 Oak Planks, 1 Feather) for a nearby crafting interface. Make the first **Ominous Egg** from 4 Feathers, 1 Rotten Flesh, 2 Gold Ingots, 1 Egg, and 1 Bell. Use the egg on the ground to begin Threat I. The event escalates by day, so gather vanilla food, armor, and weapons before triggering it.

#### 2. Days 1–3: collect salvage and unlock basic tech

Purge units normally drop 1–2 **War Feathers** and have a 50% chance to drop a **Scrap Plate**. Special units provide their own materials: kill RPG Chickens for **Rocket Casings** (and a rare Captured Peckzooka), Snipers for **Scrap Circuits**, Grenadiers for **Gunpowder Pouches**, Medics for **Medic Syringes**, and Sergeants for **Golden Feathers**. Kill units yourself to learn recipes: Scavengers unlock the turret, Militia unlock the Beak-47, Sergeants unlock the spear and cloak, and the other role-specific unlocks are listed in the recipe table below.

#### 3. Day 4 onward: raid the outpost and build defenses

An outpost can spawn from Day 4 until the final day. Defeat its **Outpost Guard** to scatter nearby squads and get 2 Scrap Circuits; search the chest for circuits, a Golden Feather, Rocket Casings, a Cluck Radar, and (on Day 4+) a Dud Rocket. Day 6+ chests can also contain an EMP Disruptor and Signal Scrambler. The outpost's Radar, Jammer, and Shield Coops are breakable and drop themselves. From Day 2 onward, nearby Purge squads can also deploy up to three Coops near you while the event is active.

Coop effects depend on who placed them. Purge-placed Coops strengthen nearby Purge chickens and debuff or reveal players; their Jammer also disrupts Cluck Radar. Player-placed Coops support you and hinder Purge chickens instead. Player-placed Radar grants nearby players Speed and makes Purge chickens Glow; Jammer slows and weakens Purge chickens; Shield grants players Resistance and weakens Purge chickens.

#### 4. Days 5–6: pursue elite salvage

Tanks and Golem Walkers drop 3 Scrap Plates; either can rarely drop Reactive Plating, and a Golem Walker has a 30% chance to drop a **Generator Core**. Generals drop their **General's Plume** and 2 Scrap Plates. Gunships and Drones drop Scrap Circuits. These elite drops and their blueprint unlocks make the late-game recipes possible. Open surviving Rations Crates for a random supply kit; on non-Hardcore Threat I–III runs, surviving a day earns a crate at dawn.

#### 5. Day 7: defeat both Overlords

The bosses arrive on the final Purge day. Defeating the final boss ends the event and rewards participants with an **Overlord Core**, **Flockwar Horn**, and Overlord Shards; players who contributed also receive the **Overlord's Crown**. The Overlord drops 3 + the threat level in Shards, and has a 30% chance to drop a **Golden Egg**. Use Shards to craft a Singularity Ominous Egg for a later Threat V run. The Golden Egg hatches a rideable Giga Chicken.

#### Item, recipe, and use reference

Ingredients below are exact recipe contents; shaped layouts are not needed. “Blueprint” means kill a unit of that role to unlock the recipe in your recipe book. Items without a listed recipe are drops, event rewards, or outpost finds.

**Event eggs and field essentials**

| Item | How to obtain / recipe | What it is for |
| :--- | :--- | :--- |
| Ominous Egg | Craft: 4 Feathers, Rotten Flesh, 2 Gold Ingots, Egg, Bell. | Starts Threat I when used on the ground. |
| Gilded Ominous Egg | Craft: Ominous Egg, Gold Block, Netherite Scrap. | Starts Threat II. |
| Cursed Ominous Egg | Craft: Ominous Egg, Ominous Bottle, Nether Star. | Starts Threat III. |
| Warlord Ominous Egg | Craft: Cursed Ominous Egg, Blaze Rod, Golden Feather; use requires Rank III. | Starts Threat IV. |
| Singularity Ominous Egg | Craft: Warlord Ominous Egg, Nether Star, Overlord Shard; use requires Rank IV. | Starts Threat V. |
| Scrap Bench | Craft: 4 Iron Ingots, 4 Oak Planks, Feather. | Opens a crafting-table menu; it is not required for recipes. |
| Rations Crate | Earned at dawn after surviving a day on Threat I–III (except Hardcore); may be awarded when the Purge begins. | Right-click to unpack one random food, ammunition, medical, or salvage kit; a Lasso can appear as a rare bonus. |

**Salvage, rewards, and non-crafted items**

| Item | How to obtain | What it is for |
| :--- | :--- | :--- |
| War Feather | Common Purge-unit drop; additional chance after reaching Rank I. | Ingredient for the spear, cloak, Lasso, and other gear; Veteran Rooster currency. |
| Scrap Plate | Common Purge-unit drop; also dropped by Riot Chickens, Generals, Tanks, and Walkers; found in some Rations kits. | Main metal ingredient for weapons, armor, bunkers, and the Flockguard Sigil. |
| Scrap Circuit | Snipers, Gunships, and Drones drop them; also found in outpost chests and from the Guard. | Ingredient for electronics, weapons, armor, and the turret. |
| Rocket Casing | RPG Chicken drop; also found in outpost chests. | Ingredient for Dud Rockets, the Peckzooka, and other weapons. |
| Gunpowder Pouch | Grenadier Hen drop, Rations kit, or craft 2 from 4 Paper + Gunpowder (Grenadier blueprint). | Ammunition/fuel for the Flame Fryer and explosive supplies. |
| Golden Feather | Sergeant drop, outpost chest, or Rank IV+ start reward; no recipe. | Ingredient for the Warlord Egg, Medic Syringe, Drumstick, and Flockguard Sigil; trade currency. |
| General's Plume | Dropped by Purge Generals or traded for 16 War Feathers; no recipe. | Wearable/displayable trophy. Its nearby Strength effect is not currently applied by the implementation. |
| Generator Core | 30% chance from a Golem Walker; no recipe. | Two are needed for the Flockguard Sigil. |
| Overlord Shard | Boss drop and victory reward; no recipe. | Ingredient for a Singularity Ominous Egg. |
| Golden Egg | 30% boss-drop chance; no recipe. | Use to hatch and ride a Giga Chicken. |
| Overlord Core | Purge victory reward; no recipe. | Use normally for an orbital strike; sneak-use on a block to place a Defense Core. |
| Overlord's Crown | Victory reward for players who contributed; no recipe. | Wear for a nearby-ally Strength aura, or display it. |
| Flockwar Horn | Sky Overlord drop and Purge victory reward; no recipe. | Rally nearby players with Strength, Resistance, Speed, and brief Regeneration. |
| Nemesis Trophy | Dropped when you kill a Nemesis Chicken that has killed you; no recipe. | Named trophy for display. |
| Dud Rocket | Craft: Rocket Casing, 3 Paper, Gunpowder (RPG blueprint); also found in outpost chests, Rations kits, and some rank rewards. | Ammunition for the Captured Peckzooka. |
| Purge Grenade | Enemy ordnance/visual item; no player crafting recipe. | Used by Grenadier Hens; not a player-usable grenade in the current implementation. |
| Defense Core | Place an Overlord Core by sneak-using it on a block. | Permanent defense block that grants nearby players Resistance and attacks Purge units. |
| Scarecrow Turret | Craft: 4 Sticks, Scrap Circuit, Hay Block, Carved Pumpkin (Scavenger blueprint). | Auto-attacks Purge chickens nearby; reload with Wheat Seeds. |
| Radar Coop, Jammer Coop, Shield Coop | Break and collect from Purge outposts; squads can deploy up to three near players during an active Purge when hostile chickens are nearby. | Effects depend on placement: Purge Coops buff Purge units and debuff players (Jammer also disrupts Cluck Radar); player Coops grant Speed/Resistance or reveal Purge chickens and apply Slowness/Weakness. |

**Weapons, armor, and utilities**

| Item | Recipe / blueprint unlock | What it is for |
| :--- | :--- | :--- |
| Beak-47 | 4 Scrap Plates, Scrap Circuit, 2 Iron Ingots (Militia blueprint). | Fires at a targeted enemy using Wheat Seeds as ammunition. |
| Captured Peckzooka | 3 Scrap Plates, Scrap Circuit, 2 Rocket Casings, 2 Iron Ingots (RPG blueprint); rare RPG Chicken drop. | Fires Dud Rockets; each shot consumes one rocket. |
| Sniper Beak | Scrap Circuit, Spyglass, 4 Iron Ingots (Sniper blueprint). | Long-range aimed shot using an Arrow; effective to 64 blocks. |
| Egg Cannon | 2 Scrap Plates, 2 Eggs, 2 Rocket Casings, Scrap Circuit (Grenadier blueprint). | Consumes an Egg to blast and knock back nearby targets. |
| Flak Cannon | 3 Scrap Plates, Scrap Circuit, 2 Rocket Casings, Iron Ingot (Drone or Gunship blueprint). | Uses a Scrap Circuit charge to attack the nearest visible airborne Purge target. |
| Flame Fryer | 2 Scrap Plates, Rocket Casing, 2 Iron Ingots (Grenadier blueprint). | Consumes a Gunpowder Pouch to burn targets in front of you. |
| Rooster Spear | War Feather, Iron Ingot, Scrap Plate (Sergeant blueprint). | Melee weapon with extended reach and extra damage to airborne foes. |
| Cluck Shield | 5 Scrap Plates, Scrap Circuit, Gold Ingot (Riot blueprint). | Use for temporary Absorption and Resistance; holding it in the offhand can deflect incoming rockets. |
| Riot Helmet | 5 Scrap Plates (Riot blueprint). | Use to gain Resistance; while that effect is active, it can block arrows from the direction you face. |
| Reactive Plating | 7 Scrap Plates, Scrap Circuit, Shield (Tank or Walker blueprint). | Use to gain temporary Resistance. |
| Feather Cloak | 4 War Feathers, Phantom Membrane, Scrap Circuit, Scrap Plate (Sergeant blueprint). | Use to gain Slow Falling and a small upward boost. |
| Flockguard Sigil | 4 War Feathers, Golden Feather, 2 Generator Cores, Shield, Scrap Plate (Walker blueprint). | Hold in either hand for knockback immunity and 40% less damage from chickens. |
| Medic Syringe | Craft 2 from Golden Feather, 3 Iron Nuggets, Glass Bottle (Medic blueprint); Medic Hen drop. | Consume one to heal yourself or a nearby player and grant Regeneration. |
| Drumstick of Valor | Golden Feather, 2 Cooked Chicken, 2 Gold Nuggets. | Consume for food and a short Strength boost. |
| Cluck Radar | 2 Scrap Circuits, 2 Iron Ingots, Redstone (General blueprint); also found in outpost chests. | Reports up to 8 nearby Purge units within 64 blocks; a Jammer Coop can interfere. |
| EMP Disruptor | Scrap Circuit, 2 Iron Ingots, 2 Redstone (Drone or Gunship blueprint); Day 6+ outpost chest. | Disrupts nearby Purge units and can destroy nearby outpost Coops. |
| Signal Scrambler | 4 Scrap Circuits, 4 Redstone, Iron Ingot (General blueprint); Day 6+ outpost chest. | Use for temporary Invisibility to evade enemy attention. |
| Lasso | 10% chance in a Purge outpost chest, 10% chance when the ground Overlord Ravager dies, or a rare bonus in Rations Crates; also available from the Veteran Rooster. | Player-held use hurls a flat 3x3 boulder in a ballistic arc; aim higher to throw farther. It explodes on impact; 10-second cooldown. This is separate from the boss's marked anvil barrage. |

**Crafted construction**

| Item | Recipe / blueprint unlock | What it is for |
| :--- | :--- | :--- |
| Bunker Block | Craft 4 from 4 Obsidian, 4 Scrap Plates, Iron Block (Tank or Walker blueprint). | Heavy fortification that takes 20 custom siege hits to break. |

The Veteran Rooster appears after a Threat III-or-higher victory once a player reaches Rank III. Trades include 8 War Feathers for 2 Dud Rockets, 4 Golden Feathers for 3 Scrap Circuits, 16 War Feathers for a General's Plume, or 12 War Feathers plus 8 Scrap Plates for a Lasso.

---

## Day 7 Doomsday Boss Fight

The final day unleashes the full might of the Chicken Empire. A catastrophic thunderstorm envelops all dimensions as the Overlords deploy.

### The Overlord (Ground Boss)
A 4x scale armored mechanical Ravager ridden by the **Overlord Rooster** and a mounted **Overlord Sniper**.
- **Melee Maul**: Crushing bite dealing heavy damage with reduced knockback.
- **Bulldozer Charge**: Marks the player's position, roaring before launching into a high-speed charge that tears through blocks and entities.
- **Seismic Leap**: Jumps high into the air and crashes down onto marked locations, causing a destructive shockwave.
- **Sonic Boom**: Channels Warden-class sonic energy for 5 seconds before firing a devastating long-range beam.
- **Orbital Strike (Phase 3)**: Locks a target laser onto the player, triggering an explosive orbital blast after 2 seconds.
- **Rocket Barrage**: Mounts twin rocket tubes firing heavy explosive projectiles.

### The Sky Overlord (Aerial Boss)
A 2.5x scale colossal Phantom piloted by the **Overlord Bombardier** and escorted by a **Sky Gunner**.
- **Strafing Barrage**: Fires triple rocket spreads in mid-flight.
- **Spinning TNT Run**: Spins rapidly while dropping waves of primed TNT.
- **Mega TNT**: Launches a glowing, high-yield explosive payload with a 22-block danger zone.
- **Dive Bomb**: Swoops down at extreme velocity to bite players.
- **Damage Cap Armor**: Resists burst damage—no single hit can deal more than 33% of its maximum health. The Sky Overlord has 1,024 base health and takes only 33% damage from the ground Overlord's Ravager attacks.

### Shield Generator Golems & Resistance Scaling
The Overlord Ravager is protected by **4 Shield Generator Golems** tethered around it. Unlike previous versions where the boss was completely invulnerable, the Ravager now receives scaled **Resistance** based directly on the number of living shield golems:

- **4 Golems Alive**: **Resistance IV** (80% damage reduction)
- **3 Golems Alive**: **Resistance III** (60% damage reduction)
- **2 Golems Alive**: **Resistance II** (40% damage reduction)
- **1 Golem Alive**: **Resistance I** (20% damage reduction)
- **0 Golems Alive**: **No Damage Reduction** (Shields Offline!)

*The top-screen boss bar dynamically displays the current shield count and Resistance level in real-time.*

### Teleportation Pursuit Mechanic
Fleeing from the apocalypse is no longer an escape. If players attempt to run away on Day 7 (exceeding 150 blocks from the bosses), **both the Overlord and Sky Overlord teleport within a 200-block radius** (50–70 blocks away) to intercept and confront the players.
- The Ravager's passengers and tethered Shield Generator Golems teleport alongside it in formation.
- The Sky Overlord repositions at altitude directly ahead of the player's vector.

### Friendly Fire Dynamics
To level the playing field against overwhelming hordes, **friendly fire exists between bosses and chickens**:
- **Boss AOE Obliteration**: The Ravager's charges, leaps, rockets, sonic blasts, orbital strikes, and the Phantom's TNT volleys will damage and blow away any friendly chicken infantry caught in the crossfire!
- **Chicken Crossfire**: Stray rockets and grenades launched by RPG Chickens and Grenadier Hens can clip and damage the bosses.
- **Shield Generator Protection**: The **Shield Generator Iron Golems are considered part of the Ravager's core chassis**. Consequently, shield golems are **immune to friendly fire from both bosses and chickens**, ensuring players must deliberately dismantle the shield array themselves.

---

## Complete Item Compendium

Every item in the mod features custom colored multi-line tooltips (categorized by tag, description, usage, and stats) and enhanced 3D models with bespoke display perspectives.

### Ominous Eggs
- **Ominous Egg (`battle-chickens:ominous_egg`)**: Starts the standard 7-day Chicken Purge (Threat I). Right-click on the ground to begin.
- **Gilded Ominous Egg (`battle-chickens:gilded_ominous_egg`)**: Initiates Threat Level II. Increased enemy stats and richer salvage drops.
- **Cursed Ominous Egg (`battle-chickens:cursed_ominous_egg`)**: Initiates Threat Level III. Enables Day 4+ Phantom Gunships; the Veteran Rooster appears after a qualifying victory at Rank III.
- **Warlord Ominous Egg (`battle-chickens:warlord_ominous_egg`)**: Initiates Threat Level IV. Rations are disabled; use requires Rank III.
- **Singularity Ominous Egg (`battle-chickens:singularity_ominous_egg`)**: Initiates Threat Level V. Use requires Rank IV.

### Weapons & Ordnance
- **Captured Peckzooka (`battle-chickens:captured_peckzooka`)**: Crafted after learning the RPG blueprint, with a rare RPG Chicken drop. Fires **Dud Rockets** with direct and splash damage. 120 durability, 4-second reload.
- **Dud Rocket (`battle-chickens:dud_rocket`)**: Full 3D modeled rocket projectile consumed as ammunition by the Captured Peckzooka.
- **Purge Grenade (`battle-chickens:purge_grenade`)**: Enemy ordnance used by Grenadier Hens; there is no player crafting recipe or use action.
- **Beak-47 (`battle-chickens:beak_47`)**: Rapid-fire assault rifle chambered in wheat seeds. Hold right-click to unleash rapid-fire bursts.
- **Rooster Spear (`battle-chickens:rooster_spear`)**: Extended-reach melee weapon. Deals 50% extra damage to airborne foes.
- **Sniper Beak (`battle-chickens:sniper_beak`)**: Long-range aimed shot with a 64-block effective range. Uses vanilla arrows.
- **Flame Fryer (`battle-chickens:flame_fryer`)**: Consumes a **Gunpowder Pouch** to burn targets in a short cone.
- **Egg Cannon (`battle-chickens:egg_cannon`)**: Consumes an Egg to blast and knock back nearby targets.
- **Flak Cannon (`battle-chickens:flak_cannon`)**: Uses a Scrap Circuit charge to attack the nearest visible airborne Purge target.

### Defensive Armor & Shields
- **Flockguard Sigil (`battle-chickens:flockguard_sigil`)**: Crafted from 2 Generator Cores, War Feathers, a Golden Feather, a shield, and a Scrap Plate. Holding it grants knockback immunity and reduces damage from chickens by 40%.
- **Cluck Shield (`battle-chickens:cluck_shield`)**: Use for temporary Absorption and Resistance; holding it in the offhand can deflect incoming rockets.
- **Overlord's Crown (`battle-chickens:overlord_crown`)**: Awarded to players who contribute to a Purge victory. Wearing it grants nearby allies a Strength aura within 8 blocks; it can also be displayed.
- **General's Plume (`battle-chickens:generals_plume`)**: Dropped by Purge Generals; wearable/displayable trophy. Its nearby Strength effect is not currently applied by the implementation.
- **Riot Helmet (`battle-chickens:riot_helmet`)**: Use to gain Resistance; while that effect is active, it can block arrows from the direction you face.
- **Reactive Plating (`battle-chickens:reactive_plating`)**: Use to gain temporary Resistance.
- **Feather Cloak (`battle-chickens:feather_cloak`)**: Use to gain Slow Falling and a small upward boost.

### Tactical Utilities & Consumables
- **EMP Disruptor (`battle-chickens:emp_disruptor`)**: Right-click to damage and slow nearby Purge units and Phantoms, and destroy nearby Outpost Coops (15-second cooldown).
- **Cluck Radar (`battle-chickens:cluck_radar`)**: Right-click to report up to 8 detected Purge units and their distances within 64 blocks.
- **Signal Scrambler (`battle-chickens:signal_scrambler`)**: Use for temporary Invisibility to evade enemy attention.
- **Flockwar Horn (`battle-chickens:flockwar_horn`)**: Right-click to grant nearby players Strength II, Resistance, Speed, and brief Regeneration.
- **Medic Syringe (`battle-chickens:medic_syringe`)**: Consume one to heal yourself or a nearby player and grant Regeneration.
- **Drumstick of Valor (`battle-chickens:drumstick_of_valor`)**: Consume for food and a short Strength boost.
- **Rations Crate (`battle-chickens:rations_crate`)**: Earned at dawn after surviving a day on Threat I–III (except Hardcore); right-click to unpack one random food, ammunition, medical, or salvage kit.

### Salvage & Crafting Materials
- **War Feather (`battle-chickens:war_feather`)**: Toughened military plumage dropped by Purge infantry. Core crafting material for weapons and defenses.
- **Scrap Plate (`battle-chickens:scrap_plate`)**: Common Purge-unit drop, also dropped by Riot Chickens, Generals, Tanks, and Walkers. Used for heavy plating and shields.
- **Scrap Circuit (`battle-chickens:scrap_circuit`)**: Salvaged from Snipers, Gunships, Drones, and Outposts. Essential for high-tech gadgets and turrets.
- **Rocket Casing (`battle-chickens:rocket_casing`)**: 3D modeled propulsion tube used with gunpowder and paper to craft Dud Rockets.
- **Gunpowder Pouch (`battle-chickens:gunpowder_pouch`)**: High-yield explosive pouch dropped by Grenadier Hens. Used as Flame Fryer fuel and for explosives.
- **Golden Feather (`battle-chickens:golden_feather`)**: Dropped by Sergeants, found in outpost chests, or granted as a Rank IV+ start reward; used in Veteran Rooster trades.
- **Generator Core (`battle-chickens:generator_core`)**: Full 3D modeled power core dropped with a 30% chance by Day 5+ Golem Walkers. Used to forge the Flockguard Sigil.
- **Overlord Shard (`battle-chickens:overlord_shard`)**: Crystallized void energy dropped upon defeating the Overlord. Used for Singularity Eggs and top-tier upgrades.
- **Overlord Core (`battle-chickens:overlord_core`)**: Full 3D modeled ultimate artifact. Right-click to call down an orbital strike, or Sneak-Right-Click a block to deploy a permanent Defense Core.

### Defensive Structures & Blocks
- **Scrap Bench (`battle-chickens:scrap_bench`)**: Crafting-table block. Right-click to open its crafting interface whether hands are empty or holding items.
- **Bunker Block (`battle-chickens:bunker_block`)**: Exceptionally hard fortification masonry. Custom siege attacks—including rockets, TNT blasts, and Ravager/Phantom block-breaking attacks—crack it on the first hit and break it after 20 siege hits. Player and Ravager Lasso impacts cannot damage or extract bunker blocks.
- **Defense Core (`battle-chickens:defense_core`)**: Permanent beacon created from an Overlord Core. Grants nearby players **Resistance** and zaps approaching Purge chickens.
- **Scarecrow Turret (`battle-chickens:scarecrow_turret`)**: Places a pumpkin-headed, crossbow-wielding armor stand that visibly zaps hostile Purge chickens within 16 blocks. Holds 64 seed rounds; right-click the stand with wheat seeds to reload (16 rounds/seed), or empty-hand it to inspect ammo.
- **Radar Coop (`battle-chickens:radar_coop`)**: Purge-placed, grants nearby Purge chickens Strength; player-placed, grants nearby players Speed and makes Purge chickens Glow. Purge placement does not reveal players. Effect radius: 24 blocks.
- **Jammer Coop (`battle-chickens:jammer_coop`)**: Purge-placed, grants nearby Purge chickens Speed/Strength, weakens players, and jams Cluck Radar within 12 blocks; player-placed, applies Slowness/Weakness to Purge chickens.
- **Shield Coop (`battle-chickens:shield_coop`)**: Purge-placed, grants Resistance to Purge chickens and Weakness to players; player-placed, grants Resistance to players and Weakness to Purge chickens. Effect radius: 12 blocks.

### Mounts & Companions
- **Golden Egg (`battle-chickens:golden_egg`)**: 30% chance to drop when the Purge bosses are defeated. Use it to hatch a rideable **Giga Chicken** mount!
- **Nemesis Trophy (`battle-chickens:nemesis_trophy`)**: Forged by slaying a player-killing Nemesis Chicken. Inscribed with rank and kill count; placeable in item frames or walls.

---

## Military Chicken Unit Roster

| Unit Name | Weapons / Behavior | Spawn Timeline |
| :--- | :--- | :--- |
| **Scavenger Chicken** | Light melee; swarms players. | Day 1+ |
| **Militia Chicken** | Armed melee; increased health. | Day 1+ |
| **Sergeant Chicken** | Iron sword; inspires nearby units with Strength. | Day 2+ |
| **Grenadier Hen** | 3D bomb backpack; lobs timed Purge Grenades. | Day 2+ |
| **Sniper Hen** | Sniper Beak rifle; telegraphs shots from up to 96 blocks. | Day 3+ |
| **RPG Chicken** | Fires 3 Dud Rockets at 8s intervals with audible telegraph. | Day 3+ (Unlimited on Day 7) |
| **Medic Hen** | Heals nearby chickens, shield golems, and bosses. | Day 3+ |
| **Riot Chicken** | Ballistic shield and Riot Helmet; highly durable. | Day 4+ |
| **Gunship Gunner** | Mounted on Phantom Gunships; fires automatic Beak-47 bursts. | Day 4+ |
| **Armored Ravager Tank** | Miniaturized Ravager tank vehicle with heavy melee. | Day 5+ |
| **Golem Walker** | Iron Golem walker; 30% chance to drop Generator Core. | Day 5+ |
| **Purge Drone** | High-speed Vex drone harassing players from air. | Day 6+ |
| **Purge Generals** | Summoner, Tank, Bomber, and Buffer command variants. | Day 5+ & Boss Fight |
| **Shield Generator Golem** | Leashed to the Overlord; powers resistance shield and fires lasers. | Day 7 Boss Fight |

---

## Commands

- `/purge status` - Shows current day, active threat level, boss countdown, and hostile unit counts.
- `/purge rank` - Displays your current Purge rank, titles, and unlocked perks.
- `/purge contribution` - Displays contribution points earned during the active event.
- `/purge blueprints` - Lists all unlocked weapons, armor, and utility recipes.
- `/purge debug day <day>` - *Operator command*: Jumps the active Purge to the specified day for testing.
- `/purge stop` - *Operator command*: Immediately ends the event and removes loaded Purge mobs and projectiles.

---

## Configuration

Located at `config/battle-chickens.properties`:

```properties
event_days=7
hostile_chicken_cap=40
boss_timeout_seconds=1200
boss_terrain_radius=4
rocket_block_radius=3
peckzooka_pvp_damage_multiplier=0.25
dominion_enabled=true
rpg_cap_per_player=3
reward_multiplier=1.0
difficulty_preset=standard
gear_chance_per_day=8
event_cooldown_seconds=1800
natural_military_chance_percent=30
```

- `event_days`: Duration of the Purge (1–30 in-game days). Set to 1 for quick testing.
- `hostile_chicken_cap`: Maximum simultaneous Purge chickens near each player (1–200).
- `boss_timeout_seconds`: Day 7 meltdown timer (60–86400, default 1200s / 20 min).
- `boss_terrain_radius`: Blast destruction radius for boss attacks (0 to disable griefing).
- `difficulty_preset`: `casual`, `standard`, or `hardcore`.
- `natural_military_chance_percent`: Base probability for vanilla chickens to convert to military units (0–100%).

---

## Installation & Building

### Requirements
- **Minecraft**: 26.3
- **Fabric Loader**: 0.20.1+
- **Fabric API**: 0.165.0+26.3+
- **Java**: JDK 25

### Build Instructions
```bash
git clone <repo-url>
cd battle-chickens
./gradlew build
```
The compiled jar will be available in `build/libs/battle-chickens-1.0.2.jar`. Copy it along with the Fabric API jar into your Minecraft `.minecraft/mods` folder.

To launch a development client directly:
```bash
./gradlew runClient
```
