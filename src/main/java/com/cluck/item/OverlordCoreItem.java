package com.cluck.item;

import com.cluck.BattleChickens;
import com.cluck.event.PurgeUnits;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class OverlordCoreItem extends BlockItem {
	public OverlordCoreItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		return orbitalStrike(level, player, hand);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			return super.useOn(context);
		}
		return orbitalStrike(context.getLevel(), player, context.getHand());
	}

	private InteractionResult orbitalStrike(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		ServerLevel serverLevel = (ServerLevel) level;
		Vec3 impact = player.pick(64, 0, false).getLocation();
		serverLevel.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y + 1, impact.z,
				48, 1, 4, 1, 0.06);
		serverLevel.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1, impact.z,
				1, 0, 0, 0, 0);
		for (Entity entity : serverLevel.getEntities(player, new AABB(impact, impact).inflate(8),
				candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
			LivingEntity target = (LivingEntity) entity;
			if (!PurgeUnits.hasBlastExposure(serverLevel, impact, target)) {
				continue;
			}
			float damage = entity instanceof Player
					? 40 * BattleChickens.CONFIG.peckzookaPvpDamageMultiplier() : 40;
			if (damage > 0) {
				target.hurtServer(serverLevel, player.damageSources().playerAttack(player), damage);
			}
		}
		if (!serverPlayer.getAbilities().instabuild) {
			serverPlayer.getItemInHand(hand).shrink(1);
		}
		serverPlayer.sendSystemMessage(Component.literal(
				"The Overlord Core has called down an orbital strike. Sneak-use it on a block to place a permanent Defense Core instead."));
		return InteractionResult.SUCCESS_SERVER;
	}
}
