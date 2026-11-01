package com.cluck.client;

import com.cluck.BattleChickens;
import com.cluck.entity.WarriorChicken;
import com.cluck.entity.PurgeChicken;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.chicken.Chicken;

public final class ChickenRoleRenderer extends ChickenRenderer {
	private static final Identifier MILITIA = BattleChickens.id("textures/entity/chicken/militia.png");
	private static final Identifier SCAVENGER = BattleChickens.id("textures/entity/chicken/scavenger.png");
	private static final Identifier WARRIOR = BattleChickens.id("textures/entity/chicken/warrior.png");
	private static final Identifier RPG = BattleChickens.id("textures/entity/chicken/rpg.png");
	private static final Identifier GRENADIER = BattleChickens.id("textures/entity/chicken/grenadier.png");
	private static final Identifier SNIPER = BattleChickens.id("textures/entity/chicken/sniper.png");
	private static final Identifier MEDIC = BattleChickens.id("textures/entity/chicken/medic.png");
	private static final Identifier RIOT = BattleChickens.id("textures/entity/chicken/riot.png");
	private static final Identifier SERGEANT = BattleChickens.id("textures/entity/chicken/sergeant.png");
	private static final Identifier OVERLORD = BattleChickens.id("textures/entity/chicken/overlord.png");
	private static final Identifier GUNNER = BattleChickens.id("textures/entity/chicken/gunner.png");
	private static final Identifier SUMMONER = BattleChickens.id("textures/entity/chicken/summoner.png");
	private static final Identifier TANK = BattleChickens.id("textures/entity/chicken/tank.png");
	private static final Identifier BOMBER = BattleChickens.id("textures/entity/chicken/bomber.png");
	private static final Identifier BUFFER = BattleChickens.id("textures/entity/chicken/buffer.png");
	private static final Identifier OVERLORD_RPG = BattleChickens.id("textures/entity/chicken/overlord_rpg.png");
	private static final Identifier BOMBARDIER = BattleChickens.id("textures/entity/chicken/bombardier.png");
	private static final Identifier GIGA = BattleChickens.id("textures/entity/chicken/giga.png");
	private static final Identifier GUARD = BattleChickens.id("textures/entity/chicken/guard.png");
	private static final Identifier SUMMONED = BattleChickens.id("textures/entity/chicken/summoned.png");

	public ChickenRoleRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ChickenRenderState createRenderState() {
		return new RoleRenderState();
	}

	@Override
	public void extractRenderState(Chicken chicken, ChickenRenderState state, float partialTick) {
		super.extractRenderState(chicken, state, partialTick);
		((RoleRenderState) state).roleTexture = textureFor(chicken);
	}

	@Override
	public Identifier getTextureLocation(ChickenRenderState state) {
		Identifier roleTexture = ((RoleRenderState) state).roleTexture;
		return roleTexture == null ? super.getTextureLocation(state) : roleTexture;
	}

	private static Identifier textureFor(Chicken chicken) {
		if (chicken instanceof WarriorChicken) {
			return WARRIOR;
		}
		if (chicken instanceof PurgeChicken purgeChicken) {
			return textureForRole(purgeChicken.role());
		}
		if (chicken.getCustomName() == null) {
			return MILITIA;
		}
		String name = chicken.getCustomName().getString();
		if (name.equals("Overlord Rooster") || name.equals("Overlord Bombardier")
				|| name.startsWith("Nemesis Chicken:")) {
			return OVERLORD;
		}
		if (name.equals("Gunship Gunner")) {
			return GUNNER;
		}
		if (name.equals("RPG Chicken") || name.equals("Overlord RPG Chicken")
				|| name.equals("Bomber General")) {
			return RPG;
		}
		if (name.equals("Grenadier Hen") || name.equals("Grenadier General")) {
			return GRENADIER;
		}
		if (name.equals("Sniper Hen")) {
			return SNIPER;
		}
		if (name.equals("Medic Hen")) {
			return MEDIC;
		}
		if (name.equals("Riot Chicken")) {
			return RIOT;
		}
		if (name.equals("Sergeant Chicken") || name.endsWith("General")) {
			return SERGEANT;
		}
		if (name.equals("Warrior Chicken") || name.equals("Militia Chicken")
				|| name.equals("Scavenger Chicken") || name.equals("Summoned Chicken")) {
			return MILITIA;
		}
		return null;
	}

	private static Identifier textureForRole(String role) {
		return switch (role) {
			case "rpg_chicken" -> RPG;
			case "overlord_rpg_chicken" -> OVERLORD_RPG;
			case "grenadier_chicken" -> GRENADIER;
			case "sniper_chicken" -> SNIPER;
			case "medic_chicken" -> MEDIC;
			case "riot_chicken" -> RIOT;
			case "sergeant_chicken" -> SERGEANT;
			case "militia_chicken" -> MILITIA;
			case "scavenger_chicken" -> SCAVENGER;
			case "gunship_gunner" -> GUNNER;
			case "overlord_rooster" -> OVERLORD;
			case "overlord_bombardier" -> BOMBARDIER;
			case "summoner_general" -> SUMMONER;
			case "tank_general" -> TANK;
			case "bomber_general" -> BOMBER;
			case "buffer_general" -> BUFFER;
			case "summoned_chicken" -> SUMMONED;
			case "outpost_guard" -> GUARD;
			case "giga_chicken" -> GIGA;
			default -> MILITIA;
		};
	}

	private static final class RoleRenderState extends ChickenRenderState {
		private Identifier roleTexture;
	}
}
