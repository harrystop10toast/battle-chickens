package com.cluck.projectile;

import com.cluck.block.BunkerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SkyOverlordWitherSkull extends WitherSkull {
	private static final int BUNKER_DAMAGE = 128;
	private static final int BLAST_RADIUS = 2;

	public SkyOverlordWitherSkull(ServerLevel level, LivingEntity owner, Vec3 direction) {
		super(level, owner, direction);
	}

	@Override
	protected void onHit(HitResult result) {
		if (!(level() instanceof ServerLevel level)) {
			discard();
			return;
		}

		if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != getOwner()) {
			super.onHitEntity(entityHit);
		}
		detonateTerrain(level, result);
		discard();
	}

	private void detonateTerrain(ServerLevel level, HitResult result) {
		Vec3 impact = result.getLocation();
		level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}

		BlockPos center = result instanceof BlockHitResult blockHit
				? blockHit.getBlockPos()
				: BlockPos.containing(impact);
		for (int distanceSquared = 0; distanceSquared <= BLAST_RADIUS * BLAST_RADIUS; distanceSquared++) {
			for (int x = -BLAST_RADIUS; x <= BLAST_RADIUS; x++) {
				for (int y = -BLAST_RADIUS; y <= BLAST_RADIUS; y++) {
					for (int z = -BLAST_RADIUS; z <= BLAST_RADIUS; z++) {
						if (x * x + y * y + z * z != distanceSquared) {
							continue;
						}
						BlockPos pos = center.offset(x, y, z);
						BunkerBlock.handleBlastDestructionAttempt(null, level, impact, pos, this,
								BUNKER_DAMAGE, true);
					}
				}
			}
		}
	}
}
