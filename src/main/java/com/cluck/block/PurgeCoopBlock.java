package com.cluck.block;

import com.cluck.event.PurgeManager;
import com.cluck.event.PurgeWorldData;
import com.cluck.event.PurgeUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class PurgeCoopBlock extends Block {
	public static final BooleanProperty PLAYER_PLACED = BooleanProperty.create("player_placed");

	public enum Mode {
		RADAR,
		JAMMER,
		SHIELD
	}

	private final Mode mode;

	public PurgeCoopBlock(BlockBehaviour.Properties properties, Mode mode) {
		super(properties);
		this.mode = mode;
		registerDefaultState(defaultBlockState().setValue(PLAYER_PLACED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PLAYER_PLACED);
	}

	public Mode mode() {
		return mode;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state,
							net.minecraft.world.entity.LivingEntity placer,
							net.minecraft.world.item.ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel serverLevel) {
			if (placer instanceof Player) {
				serverLevel.setBlockAndUpdate(pos, serverLevel.getBlockState(pos)
						.setValue(PLAYER_PLACED, true));
			}
			startTicker(serverLevel, pos);
		}
	}

	public void startTicker(ServerLevel level, BlockPos pos) {
		level.scheduleTick(pos, this, 20);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.is(this)) {
			return;
		}
		boolean playerPlaced = state.getValue(PLAYER_PLACED);
		int radius = mode == Mode.RADAR ? 24 : 12;
		for (Chicken chicken : level.getEntitiesOfClass(Chicken.class, new AABB(pos).inflate(radius),
				entity -> PurgeUnits.isUnit(entity) || PurgeUnits.isWarrior(entity))) {
			switch (mode) {
				case RADAR -> {
					if (playerPlaced) {
						chicken.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false));
					} else {
						chicken.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, true, false));
					}
				}
				case JAMMER -> {
					if (playerPlaced) {
						chicken.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false));
						chicken.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
					} else {
						chicken.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
						chicken.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false));
					}
				}
				case SHIELD -> chicken.addEffect(new MobEffectInstance(
						playerPlaced ? MobEffects.WEAKNESS : MobEffects.RESISTANCE,
						60, 0, true, false));
			}
		}
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
				new AABB(pos).inflate(radius), ServerPlayer::isAlive)) {
			switch (mode) {
				case RADAR -> {
					if (playerPlaced) {
						player.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, true, false));
					}
				}
				case JAMMER -> {
					if (!playerPlaced) {
						player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, true, false));
						player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
					}
				}
				case SHIELD -> {
					if (playerPlaced) {
						player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, true, false));
					} else {
						player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
					}
				}
			}
		}
		level.scheduleTick(pos, this, mode == Mode.RADAR ? 60 : 40);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (mode == Mode.RADAR && player instanceof ServerPlayer serverPlayer
				&& level instanceof ServerLevel serverLevel) {
			PurgeWorldData data = PurgeWorldData.get(serverLevel);
			if (data.isActive() && data.currentDay() < 6) {
				PurgeManager.award(serverPlayer, "pulled_the_plug", "pulled_the_plug");
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	public static boolean jamsRadar(ServerLevel level, BlockPos center) {
		for (int x = -12; x <= 12; x++) {
			for (int y = -8; y <= 8; y++) {
				for (int z = -12; z <= 12; z++) {
					if (x * x + y * y + z * z > 12 * 12) {
						continue;
					}
					BlockState state = level.getBlockState(center.offset(x, y, z));
					if (state.getBlock() instanceof PurgeCoopBlock coop
							&& coop.mode == Mode.JAMMER && !state.getValue(PLAYER_PLACED)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
