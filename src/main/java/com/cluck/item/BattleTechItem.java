package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.entity.ModEntities;
import com.cluck.block.PurgeCoopBlock;
import com.cluck.config.PurgeConfig;
import com.cluck.event.PurgeUnits;
import com.cluck.event.PurgeWorldData;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class BattleTechItem extends Item {
	public enum Ability {
		CLUCK_SHIELD,
		FEATHER_CLOAK,
		EMP_DISRUPTOR,
		EGG_CANNON,
		FLAK_CANNON,
		ORBITAL_STRIKE,
		BEAK_47,
		ROOSTER_SPEAR,
		FLAME_FRYER,
		SNIPER_BEAK,
		RIOT_HELMET,
		REACTIVE_PLATING,
		MEDIC_SYRINGE,
		DRUMSTICK_OF_VALOR,
		CLUCK_RADAR,
		SIGNAL_SCRAMBLER,
		GIGA_CHICKEN_EGG
	}

	private final Ability ability;

	public BattleTechItem(Properties properties, Ability ability) {
		super(properties);
		this.ability = ability;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		boolean used = switch (ability) {
			case CLUCK_SHIELD -> shield(serverPlayer);
			case FEATHER_CLOAK -> cloak(serverPlayer);
			case EMP_DISRUPTOR -> emp(serverPlayer);
			case EGG_CANNON -> eggCannon(serverPlayer);
			case FLAK_CANNON -> flak(serverPlayer);
			case ORBITAL_STRIKE -> orbitalStrike(serverPlayer, hand);
			case BEAK_47 -> beak47(serverPlayer);
			case ROOSTER_SPEAR -> roosterSpear(serverPlayer);
			case FLAME_FRYER -> flameFryer(serverPlayer, hand);
			case SNIPER_BEAK -> sniperBeak(serverPlayer);
			case RIOT_HELMET -> riotHelmet(serverPlayer);
			case REACTIVE_PLATING -> reactivePlating(serverPlayer);
			case MEDIC_SYRINGE -> medicSyringe(serverPlayer);
			case DRUMSTICK_OF_VALOR -> drumstick(serverPlayer);
			case CLUCK_RADAR -> radar(serverPlayer);
			case SIGNAL_SCRAMBLER -> scrambler(serverPlayer);
			case GIGA_CHICKEN_EGG -> gigaChicken(serverPlayer, hand);
		};
		return used ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
	}

	private boolean shield(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
		cooldown(player, 200);
		return true;
	}

	private boolean cloak(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160));
		Vec3 movement = player.getDeltaMovement();
		player.setDeltaMovement(movement.x, Math.max(movement.y, player.onGround() ? 0.9 : 0.55), movement.z);
		cooldown(player, 400);
		return true;
	}

	private boolean emp(ServerPlayer player) {
		ServerLevel level = player.level();
		AABB radius = player.getBoundingBox().inflate(12);
		int disabled = 0;
		for (Entity entity : level.getEntities(player, radius,
				candidate -> PurgeUnits.isUnit(candidate) || candidate instanceof Phantom)) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.setTarget(null);
			}
			if (entity instanceof LivingEntity living) {
				living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 4));
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
				if (living.hurtServer(level, player.damageSources().playerAttack(player), 12)) {
					disabled++;
				}
			}
		}
		if (player.mayBuild()) {
			for (int x = -12; x <= 12; x++) {
				for (int y = -8; y <= 8; y++) {
					for (int z = -12; z <= 12; z++) {
						if (x * x + y * y + z * z > 12 * 12) {
							continue;
						}
						BlockPos pos = player.blockPosition().offset(x, y, z);
						if (level.getBlockState(pos).getBlock() instanceof PurgeCoopBlock
								&& level.destroyBlock(pos, true, player)) {
							disabled++;
						}
					}
				}
			}
		}
		if (disabled == 0) {
			player.sendSystemMessage(Component.literal("No chicken technology was in range."));
			return false;
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1,
				player.getZ(), 40, 4, 2, 4, 0.04);
		player.sendSystemMessage(Component.literal("EMP disrupted " + disabled + " nearby targets."));
		cooldown(player, 300);
		return true;
	}

	private boolean eggCannon(ServerPlayer player) {
		if (!consume(player, Items.EGG)) {
			player.sendSystemMessage(Component.literal("You need an egg to fire the Egg Cannon."));
			return false;
		}
		Level level = player.level();
		Vec3 impact = player.pick(24, 0, false).getLocation();
		AABB radius = new AABB(impact, impact).inflate(3.5);
		for (Entity entity : level.getEntities(player, radius, candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
			float damage = entity instanceof Player ? 8 * BattleChickens.CONFIG.peckzookaPvpDamageMultiplier() : 8;
			if (damage > 0) {
				LivingEntity target = (LivingEntity) entity;
				target.hurtServer((ServerLevel) level,
						player.damageSources().playerAttack(player), damage);
				Vec3 knockback = target.position().subtract(impact).normalize();
				target.setDeltaMovement(target.getDeltaMovement().add(knockback.scale(0.9)).add(0, 0.35, 0));
			}
		}
		((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.EGG),
				impact.x, impact.y, impact.z, 18, 0.6, 0.6, 0.6, 0.05);
		player.sendSystemMessage(Component.literal("Egg grenade detonated."));
		cooldown(player, 40);
		return true;
	}

	private boolean flak(ServerPlayer player) {
		ServerLevel level = player.level();
		LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40),
						entity -> (entity instanceof Phantom
								|| entity instanceof Vex && PurgeUnits.isRewardedUnit(entity))
								&& player.hasLineOfSight(entity))
				.stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
		if (target == null) {
			player.sendSystemMessage(Component.literal("No airborne target in range."));
			return false;
		}
		if (!consume(player, ModItems.SCRAP_CIRCUIT)) {
			player.sendSystemMessage(Component.literal("The Flak Cannon needs a Scrap Circuit charge."));
			return false;
		}
		target.hurtServer(level, player.damageSources().playerAttack(player), 50);
		level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(), target.getZ(),
				14, 0.5, 0.5, 0.5, 0.15);
		cooldown(player, 30);
		return true;
	}

	private boolean orbitalStrike(ServerPlayer player, InteractionHand hand) {
		ServerLevel level = player.level();
		Vec3 impact = player.pick(64, 0, false).getLocation();
		level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y + 1, impact.z,
				48, 1, 4, 1, 0.06);
		level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1, impact.z,
				1, 0, 0, 0, 0);
		for (Entity entity : level.getEntities(player, new AABB(impact, impact).inflate(8),
				candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
			float damage = entity instanceof Player ? 40 * BattleChickens.CONFIG.peckzookaPvpDamageMultiplier() : 40;
			if (damage > 0) {
				((LivingEntity) entity).hurtServer(level, player.damageSources().playerAttack(player), damage);
			}
		}
		if (!player.getAbilities().instabuild) {
			player.getItemInHand(hand).shrink(1);
		}
		player.sendSystemMessage(Component.literal("The Overlord Core has called down an orbital strike."));
		return true;
	}

	private boolean beak47(ServerPlayer player) {
		LivingEntity target = aimedTarget(player, 28);
		if (target == null) {
			return false;
		}
		if (!consume(player, Items.WHEAT_SEEDS)) {
			player.sendSystemMessage(Component.literal("The Beak-47 needs wheat seeds."));
			return false;
		}
		damage(player, target, 5);
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.6, target.getZ(),
				4, 0.2, 0.25, 0.2, 0.05);
		cooldown(player, 4);
		return true;
	}

	private boolean roosterSpear(ServerPlayer player) {
		LivingEntity target = aimedTarget(player, 4.5);
		if (target == null) {
			return false;
		}
		damage(player, target, target instanceof Phantom || target instanceof Vex ? 18 : 12);
		Vec3 direction = player.getLookAngle().normalize();
		target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(0.75)).add(0, 0.2, 0));
		cooldown(player, 16);
		return true;
	}

	private boolean flameFryer(ServerPlayer player, InteractionHand hand) {
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle().normalize();
		var targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(7),
				entity -> entity != player && entity.isAlive()).stream().filter(target -> {
					Vec3 offset = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
					return offset.lengthSqr() <= 49 && look.dot(offset.normalize()) > 0.65
							&& player.hasLineOfSight(target);
				}).toList();
		BlockHitResult blockHit = player.pick(7, 0, false) instanceof BlockHitResult hit ? hit : null;
		boolean canIgnite = blockHit != null && player.mayBuild()
				&& level.getGameRules().get(GameRules.MOB_GRIEFING);
		if (targets.isEmpty() && !canIgnite) {
			return false;
		}
		if (!consume(player, ModItems.GUNPOWDER_POUCH)) {
			player.sendSystemMessage(Component.literal("The Flame Fryer needs a Gunpowder Pouch."));
			return false;
		}
		boolean ignited = canIgnite && igniteStructure(player, hand, level, blockHit);
		for (LivingEntity target : targets) {
			target.setRemainingFireTicks(100);
			damage(player, target, 4);
		}
		if (ignited) {
			player.sendSystemMessage(Component.literal("The Flame Fryer ignites the structure."));
		}
		level.sendParticles(ParticleTypes.FLAME, player.getX() + look.x * 2, player.getEyeY(),
				player.getZ() + look.z * 2, 18, 0.4, 0.3, 0.4, 0.04);
		cooldown(player, 20);
		return true;
	}

	private boolean igniteStructure(ServerPlayer player, InteractionHand hand, ServerLevel level, BlockHitResult hit) {
		BlockPlaceContext context = new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit);
		var fire = Blocks.FIRE.getStateForPlacement(context);
		if (fire == null || !fire.canSurvive(level, context.getClickedPos())) {
			return false;
		}
		return level.setBlock(context.getClickedPos(), fire, 3);
	}

	private boolean sniperBeak(ServerPlayer player) {
		LivingEntity target = aimedTarget(player, 64);
		if (target == null) {
			return false;
		}
		if (!consume(player, Items.ARROW)) {
			player.sendSystemMessage(Component.literal("The Sniper Beak needs an arrow."));
			return false;
		}
		damage(player, target, 24);
		player.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1,
				target.getZ(), 12, 0.15, 0.2, 0.15, 0.1);
		cooldown(player, 80);
		return true;
	}

	private boolean riotHelmet(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
		cooldown(player, 240);
		return true;
	}

	private boolean reactivePlating(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
		cooldown(player, 400);
		return true;
	}

	private boolean medicSyringe(ServerPlayer player) {
		LivingEntity target = aimedTarget(player, 6);
		ServerPlayer patient = target instanceof ServerPlayer ally ? ally : player;
		if (patient.getHealth() >= patient.getMaxHealth() && patient.hasEffect(MobEffects.REGENERATION)) {
			return false;
		}
		if (!consume(player, ModItems.MEDIC_SYRINGE)) {
			return false;
		}
		patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
		patient.heal(4);
		if (patient != player && PurgeWorldData.get(player.level()).isActive()) {
			PurgeWorldData.get(player.level()).addContribution(player.getUUID().toString(), 3);
		}
		player.sendSystemMessage(Component.literal(patient == player
				? "The Medic Syringe restores your health."
				: "You healed " + patient.getGameProfile().name() + " for +3 Purge contribution."));
		return true;
	}

	private boolean drumstick(ServerPlayer player) {
		if (!consume(player, ModItems.DRUMSTICK_OF_VALOR)) {
			return false;
		}
		player.getFoodData().eat(6, 0.8F);
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 0));
		return true;
	}

	private boolean radar(ServerPlayer player) {
		ServerLevel level = player.level();
		if (PurgeCoopBlock.jamsRadar(level, player.blockPosition())) {
			player.sendSystemMessage(Component.literal("A Jammer Coop is scrambling the Cluck Radar."));
			return false;
		}
		var units = level.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(64),
				PurgeUnits::isRewardedUnit);
		if (units.isEmpty()) {
			player.sendSystemMessage(Component.literal("No Purge units detected within 64 blocks."));
			return false;
		}
		String readings = units.stream().limit(8)
				.map(entity -> (entity.getCustomName() == null ? "Chicken" : entity.getCustomName().getString())
						+ " " + Math.round(player.distanceTo(entity)) + "m")
				.reduce((first, second) -> first + ", " + second).orElse("");
		player.sendSystemMessage(Component.literal("Cluck Radar: " + readings));
		cooldown(player, 40);
		return true;
	}

	private boolean scrambler(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));
		cooldown(player, 2400);
		return true;
	}

	private boolean gigaChicken(ServerPlayer player, InteractionHand hand) {
		ServerLevel level = player.level();
		Chicken mount = ModEntities.createChicken(level, EntitySpawnReason.EVENT, "giga");
		if (mount == null) {
			return false;
		}
		mount.setPos(player.getX(), player.getY(), player.getZ());
		mount.setPersistenceRequired();
		mount.setCustomName(Component.literal("Giga Chicken"));
		mount.setCustomNameVisible(true);
		mount.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
		mount.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
		mount.setHealth(40);
		mount.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35);
		if (!level.addFreshEntity(mount) || !player.startRiding(mount)) {
			mount.discard();
			return false;
		}
		if (!player.getAbilities().instabuild) {
			player.getItemInHand(hand).shrink(1);
		}
		player.sendSystemMessage(Component.literal("The Golden Egg hatches into a Giga Chicken mount."));
		return true;
	}

	private static LivingEntity aimedTarget(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 direction = player.getLookAngle().normalize();
		AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.5);
		LivingEntity closest = null;
		double closestDistance = range;
		for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, search,
				entity -> entity != player && entity.isAlive())) {
			Vec3 offset = target.getBoundingBox().getCenter().subtract(start);
			double along = offset.dot(direction);
			if (along > 0 && along <= closestDistance
					&& offset.subtract(direction.scale(along)).lengthSqr() <= 1.5 * 1.5
					&& player.hasLineOfSight(target)) {
				closest = target;
				closestDistance = along;
			}
		}
		return closest;
	}

	private static void damage(ServerPlayer player, LivingEntity target, float amount) {
		if (target instanceof Player) {
			amount *= BattleChickens.CONFIG.peckzookaPvpDamageMultiplier();
		}
		if (amount > 0) {
			target.hurtServer(player.level(), player.damageSources().playerAttack(player), amount);
		}
	}

	private static boolean consume(ServerPlayer player, Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private void cooldown(ServerPlayer player, int ticks) {
		player.getCooldowns().addCooldown(BattleChickens.id(
				getDescriptionId().substring("item.battle-chickens.".length())), ticks);
	}
}
