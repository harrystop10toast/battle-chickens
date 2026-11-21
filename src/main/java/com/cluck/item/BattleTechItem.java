package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.entity.ModEntities;
import com.cluck.block.PurgeCoopBlock;
import com.cluck.config.PurgeConfig;
import com.cluck.event.PurgeUnits;
import com.cluck.event.PurgeWorldData;
import com.cluck.entity.PurgeDrone;
import com.cluck.entity.PurgeGunship;
import com.cluck.entity.SkyOverlord;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BattleTechItem extends Item {
	private static final double FLAME_CONE_SPREAD_PER_BLOCK = 0.4;
	private static final int FLAME_FRYER_USE_DURATION = 72_000;
	private static final int FLAME_FRYER_AMMO_INTERVAL_TICKS = 8;

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
			if (ability == Ability.FLAME_FRYER) {
				player.startUsingItem(hand);
				return InteractionResult.CONSUME;
			}
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
			case FLAME_FRYER -> startFlameFryer(serverPlayer, hand);
			case SNIPER_BEAK -> sniperBeak(serverPlayer);
			case MEDIC_SYRINGE -> medicSyringe(serverPlayer);
			case DRUMSTICK_OF_VALOR -> drumstick(serverPlayer);
			case CLUCK_RADAR -> radar(serverPlayer);
			case SIGNAL_SCRAMBLER -> scrambler(serverPlayer);
			case GIGA_CHICKEN_EGG -> gigaChicken(serverPlayer, hand);
		};
		return used ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return ability == Ability.FLAME_FRYER
				? FLAME_FRYER_USE_DURATION
				: super.getUseDuration(stack, user);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ability == Ability.FLAME_FRYER
				? ItemUseAnimation.NONE
				: super.getUseAnimation(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (ability != Ability.FLAME_FRYER
				|| !(level instanceof ServerLevel)
				|| !(user instanceof ServerPlayer player)) {
			return;
		}
		int heldTicks = FLAME_FRYER_USE_DURATION - remainingUseTicks;
		if (heldTicks > FLAME_FRYER_AMMO_INTERVAL_TICKS
				&& (heldTicks - 1) % FLAME_FRYER_AMMO_INTERVAL_TICKS == 0
				&& !consume(player, ModItems.GUNPOWDER_POUCH)) {
			player.stopUsingItem();
			player.sendSystemMessage(Component.literal("The Flame Fryer needs a Gunpowder Pouch."));
			return;
		}
		flameFryer(player, player.getUsedItemHand());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		return player == null ? InteractionResult.PASS : use(context.getLevel(), player, context.getHand());
	}

	private boolean startFlameFryer(ServerPlayer player, InteractionHand hand) {
		if (!consume(player, ModItems.GUNPOWDER_POUCH)) {
			player.sendSystemMessage(Component.literal("The Flame Fryer needs a Gunpowder Pouch."));
			return false;
		}
		player.startUsingItem(hand);
		return true;
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
				candidate -> PurgeUnits.isUnit(candidate) || candidate instanceof SkyOverlord
						|| candidate instanceof PurgeGunship)) {
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
				if (target instanceof ServerPlayer serverTarget) {
					damage *= PurgeUnits.explosiveDefenseMultiplier(serverTarget);
				}
				if (!PurgeUnits.hasBlastExposure((ServerLevel) level, impact, target)) {
					continue;
				}
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
		if (!consume(player, ModItems.SCRAP_CIRCUIT)) {
			player.sendSystemMessage(Component.literal("The Flak Cannon needs a Scrap Circuit charge."));
			return false;
		}

		Vec3 direction = player.getLookAngle().normalize();
		Vec3 start = player.getEyePosition().add(direction.scale(0.45));
		Vec3 right = direction.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 0.001) {
			right = direction.cross(new Vec3(1, 0, 0));
		}
		right = right.normalize();
		Vec3 up = right.cross(direction).normalize();
		double[][] spread = {
				{0, 0}, {0.055, 0}, {-0.055, 0}, {0, 0.055}, {0, -0.055},
				{0.04, 0.04}, {-0.04, -0.04}
		};
		double range = 20;
		AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(3);
		var candidates = level.getEntities(player, search,
				entity -> entity.isAlive() && (entity instanceof LivingEntity || entity instanceof PurgeDrone));
		Map<Entity, Integer> hits = new HashMap<>();
		for (double[] pellet : spread) {
			Vec3 ray = direction.add(right.scale(pellet[0])).add(up.scale(pellet[1])).normalize();
			Vec3 end = start.add(ray.scale(range));
			var blockHit = level.clip(new ClipContext(start, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS
					? end : blockHit.getLocation();
			Entity hitTarget = null;
			double closest = Double.MAX_VALUE;
			for (Entity candidate : candidates) {
				var entityHit = candidate.getBoundingBox().inflate(0.1).clip(start, traceEnd);
				if (entityHit.isPresent()) {
					double distance = start.distanceToSqr(entityHit.get());
					if (distance < closest) {
						closest = distance;
						hitTarget = candidate;
					}
				}
			}
			BulletTracer.fire(level, start, traceEnd);
			if (hitTarget != null) {
				hits.merge(hitTarget, 1, Integer::sum);
			}
		}
		for (Map.Entry<Entity, Integer> hit : hits.entrySet()) {
			Entity target = hit.getKey();
			if (target instanceof PurgeDrone drone) {
				drone.intercept(level);
			} else if (target instanceof LivingEntity livingTarget) {
				float damagePerPellet = 3.5F
						+ (livingTarget instanceof SkyOverlord || livingTarget instanceof PurgeGunship ? 0.5F : 0);
				damage(player, livingTarget, damagePerPellet * hit.getValue());
			}
		}
		level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
				SoundSource.PLAYERS, 1.0F, 0.8F);
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
				LivingEntity target = (LivingEntity) entity;
				if (target instanceof ServerPlayer serverTarget) {
					damage *= PurgeUnits.explosiveDefenseMultiplier(serverTarget);
				}
				if (PurgeUnits.hasBlastExposure(level, impact, target)) {
					target.hurtServer(level, player.damageSources().playerAttack(player), damage);
				}
			}
		}
		if (!player.getAbilities().instabuild) {
			player.getItemInHand(hand).shrink(1);
		}
		player.sendSystemMessage(Component.literal("The Overlord Core has called down an orbital strike."));
		return true;
	}

	private boolean beak47(ServerPlayer player) {
		if (!consume(player, Items.WHEAT_SEEDS)) {
			player.sendSystemMessage(Component.literal("The Beak-47 needs wheat seeds."));
			return false;
		}
		ServerLevel level = player.level();
		LivingEntity target = fireBullet(player, 40, 0.055, 0.20);
		if (target != null) {
			damage(player, target, 8);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.6, target.getZ(),
					4, 0.2, 0.25, 0.2, 0.05);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
				SoundSource.PLAYERS, 0.65F, 1.5F);
		cooldown(player, 4);
		return true;
	}

	private boolean roosterSpear(ServerPlayer player) {
		LivingEntity target = aimedTarget(player, 6.5);
		if (target == null) {
			return false;
		}
		damage(player, target, target instanceof SkyOverlord || target instanceof PurgeGunship
				? 18 : 12);
		Vec3 direction = player.getLookAngle().normalize();
		target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(0.75)).add(0, 0.2, 0));
		cooldown(player, 16);
		return true;
	}

	private void flameFryer(ServerPlayer player, InteractionHand hand) {
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 right = new Vec3(look.z, 0, -look.x);
		if (right.lengthSqr() < 1.0E-6) {
			right = new Vec3(1, 0, 0);
		} else {
			right = right.normalize();
		}
		Vec3 up = look.cross(right).normalize();
		boolean rightHanded = (hand == InteractionHand.MAIN_HAND)
				== (player.getMainArm() == HumanoidArm.RIGHT);
		Vec3 muzzle = player.getEyePosition().add(look.scale(0.75))
				.add(right.scale(rightHanded ? 0.28 : -0.28))
				.add(0, -0.18, 0);
		var targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8),
				entity -> entity != player && entity.isAlive()).stream().filter(target -> {
					return isInsideFlameCone(muzzle, look, target.getBoundingBox().getCenter())
							&& player.hasLineOfSight(target);
				}).toList();
		BlockHitResult blockHit = player.pick(7, 0, false) instanceof BlockHitResult hit ? hit : null;
		boolean canIgnite = blockHit != null && player.mayBuild()
				&& level.getGameRules().get(GameRules.MOB_GRIEFING);
		if (canIgnite) {
			igniteStructure(player, hand, level, blockHit, muzzle, look, right, up);
		}
		for (LivingEntity target : targets) {
			target.setRemainingFireTicks(100);
			damage(player, target, 4);
		}
		RandomSource random = level.getRandom();
		for (int i = 0; i < 8; i++) {
			double distance = random.nextDouble() * 4.5;
			Vec3 coneOffset = randomConeOffset(random, right, up, distance, FLAME_CONE_SPREAD_PER_BLOCK);
			Vec3 position = muzzle.add(look.scale(distance))
					.add(coneOffset);
			Vec3 velocity = look.scale(0.3 + random.nextDouble() * 0.25)
					.add(coneOffset.scale(0.035));
			level.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z,
					0, velocity.x, velocity.y, velocity.z, 1.0);
		}
		for (int i = 0; i < 1; i++) {
			double distance = random.nextDouble() * 0.9;
			Vec3 position = muzzle.add(look.scale(distance))
					.add(randomConeOffset(random, right, up, distance, 0.1));
			Vec3 velocity = look.scale(0.22 + random.nextDouble() * 0.18)
					.add(up.scale((random.nextDouble() - 0.5) * 0.18));
			level.sendParticles(ParticleTypes.LAVA, position.x, position.y, position.z,
					0, velocity.x, velocity.y, velocity.z, 1.0);
		}
		for (int i = 0; i < 2; i++) {
			double distance = random.nextDouble() * 2.2;
			Vec3 position = muzzle.add(look.scale(distance))
					.add(randomConeOffset(random, right, up, distance, 0.18));
			Vec3 velocity = look.scale(0.12 + random.nextDouble() * 0.16)
					.add(up.scale(0.04 + random.nextDouble() * 0.08));
			level.sendParticles(ParticleTypes.SMOKE, position.x, position.y, position.z,
					0, velocity.x, velocity.y, velocity.z, 1.0);
		}
	}

	private static Vec3 randomConeOffset(RandomSource random, Vec3 right, Vec3 up,
			double distance, double spreadPerBlock) {
		double angle = random.nextDouble() * Math.PI * 2;
		double radius = Math.sqrt(random.nextDouble()) * (0.025 + distance * spreadPerBlock);
		return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
	}

	private static boolean isInsideFlameCone(Vec3 origin, Vec3 look, Vec3 point) {
		Vec3 offset = point.subtract(origin);
		double distance = offset.dot(look);
		if (distance <= 0.0 || distance > 7.0) {
			return false;
		}
		double radius = 0.025 + distance * FLAME_CONE_SPREAD_PER_BLOCK;
		double radialDistanceSqr = Math.max(0.0, offset.lengthSqr() - distance * distance);
		return radialDistanceSqr <= radius * radius;
	}

	private boolean igniteStructure(ServerPlayer player, InteractionHand hand, ServerLevel level,
			BlockHitResult hit, Vec3 muzzle, Vec3 look, Vec3 right, Vec3 up) {
		BlockPlaceContext context = new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit);
		var fire = Blocks.FIRE.getStateForPlacement(context);
		if (fire == null) {
			return false;
		}

		double range = Math.min(7.0, muzzle.distanceTo(hit.getLocation()));
		int placed = 0;
		Set<BlockPos> checked = new HashSet<>();
		Set<BlockPos> placedPositions = new HashSet<>();
		for (double distance = 1.0; distance <= range; distance += 0.4) {
			double radius = 0.025 + distance * FLAME_CONE_SPREAD_PER_BLOCK;
			int divisions = Math.max(1, (int) Math.ceil(radius / 0.35));
			Vec3 axisPoint = muzzle.add(look.scale(distance));
			for (int sideIndex = -divisions; sideIndex <= divisions; sideIndex++) {
				double sideOffset = radius * sideIndex / divisions;
				for (int upIndex = -divisions; upIndex <= divisions; upIndex++) {
					double upOffset = radius * upIndex / divisions;
					if (sideOffset * sideOffset + upOffset * upOffset > radius * radius) {
						continue;
					}
					BlockPos candidate = BlockPos.containing(axisPoint
							.add(right.scale(sideOffset)).add(up.scale(upOffset)));
					if (!checked.add(candidate)) {
						continue;
					}
					BlockPos firePos = findFireSpreadPosition(level, candidate, fire);
					if (firePos != null && placedPositions.add(firePos)
							&& level.setBlock(firePos, fire, 3)) {
						placed++;
					}
				}
			}
		}
		return placed > 0;
	}

	private static BlockPos findFireSpreadPosition(ServerLevel level, BlockPos target,
			net.minecraft.world.level.block.state.BlockState fire) {
		BlockPos[] candidates = {target, target.below(), target.above()};
		for (BlockPos candidate : candidates) {
			if (level.isEmptyBlock(candidate) && fire.canSurvive(level, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private boolean sniperBeak(ServerPlayer player) {
		if (!consume(player, Items.ARROW)) {
			player.sendSystemMessage(Component.literal("The Sniper Beak needs an arrow."));
			return false;
		}
		int viewDistance = Math.max(1, Math.min(player.requestedViewDistance(),
				player.level().getServer().getPlayerList().getViewDistance()));
		LivingEntity target = fireBullet(player, viewDistance * 16.0, 0.004, 0.04);
		if (target != null) {
			damage(player, target, player.isCrouching() ? 36 : 24);
			player.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1,
					target.getZ(), 12, 0.15, 0.2, 0.15, 0.1);
		}
		cooldown(player, 80);
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
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false));
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

	private static LivingEntity fireBullet(ServerPlayer player, double range,
			double baseInaccuracy, double movementInaccuracy) {
		ServerLevel level = player.level();
		Vec3 movement = player.getDeltaMovement();
		double horizontalSpeed = movement.horizontalDistance();
		boolean stationary = player.onGround() && !player.isSprinting() && horizontalSpeed < 0.005;
		double inaccuracy = stationary
				? baseInaccuracy * (player.isCrouching() ? 0.02 : 0.1)
				: baseInaccuracy + Math.min(horizontalSpeed, 0.3) * movementInaccuracy + 0.035;
		if (!stationary && player.isSprinting()) {
			inaccuracy += 0.035;
		}
		if (!stationary && !player.onGround()) {
			inaccuracy += 0.05;
		}
		Vec3 direction = BulletSpread.apply(player.getLookAngle(), inaccuracy, level.getRandom());
		Vec3 start = player.getEyePosition().add(direction.scale(0.45));
		Vec3 rangeEnd = start.add(direction.scale(range));
		var blockHit = level.clip(new ClipContext(start, rangeEnd,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 traceEnd = blockHit.getType() == HitResult.Type.MISS
				? rangeEnd : blockHit.getLocation();
		LivingEntity target = null;
		Vec3 targetHit = null;
		double closest = Double.MAX_VALUE;
		AABB search = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1);
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search,
				entity -> entity != player && entity.isAlive())) {
			var entityHit = candidate.getBoundingBox().inflate(0.1).clip(start, traceEnd);
			if (entityHit.isPresent()) {
				double distance = start.distanceToSqr(entityHit.get());
				if (distance < closest) {
					closest = distance;
					target = candidate;
					targetHit = entityHit.get();
				}
			}
		}
		if (targetHit != null) {
			traceEnd = targetHit;
		}
		BulletTracer.fire(level, start, traceEnd);
		return target;
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
