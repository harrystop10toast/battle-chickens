package com.cluck.item;

import com.cluck.event.PurgeUnits;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ScarecrowTurretItem extends Item {
	private static final String TAG = "battle_chickens_scarecrow_turret";
	private static final int CAPACITY = 64;

	public ScarecrowTurretItem(Properties properties) {
		super(properties);
	}

	public static void registerEntityInteraction() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!isTurret(entity)) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			if (held.is(Items.WHEAT_SEEDS)) {
				int ammo = getAmmo(entity);
				if (ammo >= CAPACITY) {
					return InteractionResult.PASS;
				}
				int loaded = Math.min(16, CAPACITY - ammo);
				setAmmo(entity, ammo + loaded);
				if (!player.getAbilities().instabuild) {
					held.shrink(1);
				}
				serverPlayer.sendSystemMessage(Component.literal(
						"Scarecrow Turret loaded: " + (ammo + loaded) + "/64 seed rounds."));
				return InteractionResult.SUCCESS;
			}
			if (held.isEmpty()) {
				serverPlayer.sendSystemMessage(Component.literal(
						"Scarecrow Turret ammunition: " + getAmmo(entity) + "/64."));
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		if (player == null || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isSolid()) {
			return InteractionResult.FAIL;
		}
		ArmorStand turret = EntityTypes.ARMOR_STAND.create(serverLevel, EntitySpawnReason.EVENT);
		if (turret == null) {
			return InteractionResult.FAIL;
		}
		turret.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		turret.setYRot(player.getYRot());
		turret.setNoGravity(true);
		turret.setShowArms(true);
		turret.setNoBasePlate(true);
		turret.setCustomName(Component.literal("Scarecrow Turret"));
		turret.setCustomNameVisible(true);
		turret.addTag(TAG);
		setAmmo(turret, CAPACITY);
		turret.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
		turret.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
		turret.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
		if (!serverLevel.addFreshEntity(turret)) {
			return InteractionResult.FAIL;
		}
		if (!player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		serverLevel.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 1, 1);
		return InteractionResult.SUCCESS;
	}

	public static void tick(ServerLevel level) {
		for (Entity entity : level.getAllEntities()) {
			if (!isTurret(entity) || getAmmo(entity) <= 0) {
				continue;
			}
			Vec3 origin = entity.position().add(0, 1.35, 0);
			Chicken target = level.getEntitiesOfClass(Chicken.class, entity.getBoundingBox().inflate(16),
							chicken -> (PurgeUnits.isUnit(chicken) || PurgeUnits.isWarrior(chicken))
									&& level.clip(new ClipContext(origin, chicken.getBoundingBox().getCenter(),
											ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
											.getType() == HitResult.Type.MISS)
					.stream()
					.min(java.util.Comparator.comparingDouble(entity::distanceToSqr))
					.orElse(null);
			if (target == null) {
				continue;
			}
			Vec3 destination = target.getBoundingBox().getCenter();
			Vec3 direction = destination.subtract(origin);
			Vec3 step = direction.normalize().scale(0.5);
			for (double distance = 0; distance < direction.length(); distance += 0.5) {
				Vec3 point = origin.add(step.scale(distance / 0.5));
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
						2, 0.015, 0.015, 0.015, 0);
			}
			target.hurtServer(level, level.damageSources().mobAttack((ArmorStand) entity), 6);
			setAmmo(entity, getAmmo(entity) - 1);
			level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
					SoundSource.BLOCKS, 0.8F, 1.15F);
		}
	}

	private static boolean isTurret(Entity entity) {
		return entity instanceof ArmorStand && entity.entityTags().contains(TAG);
	}

	private static int getAmmo(Entity entity) {
		if (entity.getCustomName() == null) {
			return 0;
		}
		String name = entity.getCustomName().getString();
		int start = name.lastIndexOf('[');
		int end = name.lastIndexOf('/');
		try {
			return start >= 0 && end > start ? Integer.parseInt(name.substring(start + 1, end)) : 0;
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	private static void setAmmo(Entity entity, int ammo) {
		int remaining = Math.clamp(ammo, 0, CAPACITY);
		entity.setCustomName(Component.literal("Scarecrow Turret [" + remaining + "/" + CAPACITY + "]"));
	}
}
