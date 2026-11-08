package com.cluck.event;

import com.cluck.BattleChickens;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OverlordCoreCooldownData extends SavedData {
	private static final Codec<OverlordCoreCooldownData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
			.xmap(OverlordCoreCooldownData::new, data -> data.readyAtTicks);
	private static final SavedDataType<OverlordCoreCooldownData> TYPE = new SavedDataType<>(
			BattleChickens.id("overlord_core_cooldowns"), OverlordCoreCooldownData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Long> readyAtTicks;

	public OverlordCoreCooldownData() {
		this(Map.of());
	}

	private OverlordCoreCooldownData(Map<String, Long> readyAtTicks) {
		this.readyAtTicks = new HashMap<>(readyAtTicks);
	}

	public static OverlordCoreCooldownData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public long normalCooldownRemaining(UUID player, long now) {
		return remaining(player, "normal", now);
	}

	public long ultimateCooldownRemaining(UUID player, long now) {
		return remaining(player, "ultimate", now);
	}

	public void startNormalCooldown(UUID player, long readyAt) {
		readyAtTicks.put(key(player, "normal"), readyAt);
		setDirty();
	}

	public void startUltimateCooldown(UUID player, long readyAt) {
		readyAtTicks.put(key(player, "ultimate"), readyAt);
		setDirty();
	}

	private long remaining(UUID player, String type, long now) {
		String key = key(player, type);
		Long readyAt = readyAtTicks.get(key);
		if (readyAt == null) {
			return 0;
		}
		if (readyAt <= now) {
			readyAtTicks.remove(key);
			setDirty();
			return 0;
		}
		return readyAt - now;
	}

	private static String key(UUID player, String type) {
		return type + ":" + player;
	}
}
