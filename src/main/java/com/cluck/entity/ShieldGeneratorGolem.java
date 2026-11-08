package com.cluck.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ShieldGeneratorGolem extends IronGolem {
	public ShieldGeneratorGolem(EntityType<? extends IronGolem> entityType, Level level) {
		super(entityType, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return IronGolem.createAttributes()
				.add(Attributes.ATTACK_DAMAGE, 50.0)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
		goalSelector.addGoal(3, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (target == null || target instanceof Player) {
			super.setTarget(target);
		}
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return target instanceof Player && super.canAttack(target);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return target instanceof Player && super.doHurtTarget(level, target);
	}
}
