package com.cluck.item;

import net.minecraft.core.particles.ParticleTypes;
import com.cluck.BattleChickens;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FlockwarHornItem extends Item {
	private static final int RADIUS = 32;
	private static final int COOLDOWN_TICKS = 24_000;

	public FlockwarHornItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer caller)) {
			return InteractionResult.PASS;
		}
		int rallied = 0;
		for (ServerPlayer ally : caller.level().getEntitiesOfClass(ServerPlayer.class,
				caller.getBoundingBox().inflate(RADIUS))) {
			if (ally.distanceToSqr(caller) > (double) RADIUS * RADIUS) {
				continue;
			}
			ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 1));
			ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0));
			ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 0));
			ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			caller.level().sendParticles(ParticleTypes.HAPPY_VILLAGER,
					ally.getX(), ally.getY() + 1, ally.getZ(), 16, 0.6, 0.8, 0.6, 0.1);
			rallied++;
		}
		caller.level().playSound(null, caller, SoundEvents.NOTE_BLOCK_HARP,
				SoundSource.PLAYERS, 3.0F, 0.8F);
		caller.getCooldowns().addCooldown(BattleChickens.id("flockwar_horn"), COOLDOWN_TICKS);
		caller.sendSystemMessage(Component.literal("The Flockwar Horn rallies " + rallied
				+ " nearby allies for 30 seconds. The horn needs one day to recharge."));
		return InteractionResult.SUCCESS_SERVER;
	}
}
