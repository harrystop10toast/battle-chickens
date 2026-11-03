package com.cluck.event;

import com.cluck.BattleChickens;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PurgeWorldData extends SavedData {
	private record EventMeta(boolean veteranTraderSpawned, long nextEventTick,
							 Map<String, Integer> antiAirKills,
							 Map<String, Integer> civilianChickenKills,
							 Map<String, WeatherSnapshot> weatherSnapshots) {
		private static final Codec<EventMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("veteran_trader_spawned", false)
						.forGetter(EventMeta::veteranTraderSpawned),
				Codec.LONG.optionalFieldOf("next_event_tick", 0L).forGetter(EventMeta::nextEventTick),
				Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("anti_air_kills", Map.of())
						.forGetter(EventMeta::antiAirKills),
				Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("civilian_chicken_kills", Map.of())
						.forGetter(EventMeta::civilianChickenKills),
				Codec.unboundedMap(Codec.STRING, WeatherSnapshot.CODEC).optionalFieldOf("weather_snapshots", Map.of())
						.forGetter(EventMeta::weatherSnapshots)
		).apply(instance, EventMeta::new));
	}

	private record WeatherSnapshot(int clearTime, int rainTime, int thunderTime,
								   boolean raining, boolean thundering) {
		private static final Codec<WeatherSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("clear_time").forGetter(WeatherSnapshot::clearTime),
				Codec.INT.fieldOf("rain_time").forGetter(WeatherSnapshot::rainTime),
				Codec.INT.fieldOf("thunder_time").forGetter(WeatherSnapshot::thunderTime),
				Codec.BOOL.fieldOf("raining").forGetter(WeatherSnapshot::raining),
				Codec.BOOL.fieldOf("thundering").forGetter(WeatherSnapshot::thundering)
		).apply(instance, WeatherSnapshot::new));
	}

	private static final Codec<PurgeWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("active").forGetter(data -> data.active),
			Codec.LONG.fieldOf("elapsed_ticks").forGetter(data -> data.elapsedTicks),
			Codec.INT.fieldOf("threat").forGetter(data -> data.threat),
			Codec.INT.fieldOf("event_days").forGetter(data -> data.eventDays),
			Codec.INT.fieldOf("current_day").forGetter(data -> data.currentDay),
			Codec.INT.optionalFieldOf("spawned_day", 0).forGetter(data -> data.spawnedDay),
			Codec.BOOL.optionalFieldOf("boss_spawned", false).forGetter(data -> data.bossSpawned),
			Codec.INT.optionalFieldOf("boss_ticks", 0).forGetter(data -> data.bossTicks),
			Codec.BOOL.optionalFieldOf("dominion", false).forGetter(data -> data.dominion),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("flockbreaker_ranks", Map.of()).forGetter(data -> data.ranks),
			Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING)).optionalFieldOf("blueprints", Map.of()).forGetter(data -> data.blueprints),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("deaths_by_player", Map.of()).forGetter(data -> data.deathsByPlayer),
			Codec.BOOL.optionalFieldOf("outpost_spawned", false).forGetter(data -> data.outpostSpawned),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("contributions", Map.of()).forGetter(data -> data.contributions),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("nemesis_kills", Map.of()).forGetter(data -> data.nemesisKills),
			EventMeta.CODEC.optionalFieldOf("event_meta", new EventMeta(false, 0L, Map.of(), Map.of(), Map.of()))
					.forGetter(data -> new EventMeta(data.veteranTraderSpawned, data.nextEventTick,
							data.antiAirKills, data.civilianChickenKills, data.weatherSnapshots))
	).apply(instance, PurgeWorldData::new));

	private static final SavedDataType<PurgeWorldData> TYPE = new SavedDataType<>(
			BattleChickens.id("purge"), PurgeWorldData::new, CODEC, DataFixTypes.LEVEL);

	private boolean active;
	private long elapsedTicks;
	private int threat;
	private int eventDays;
	private int currentDay;
	private int spawnedDay;
	private boolean bossSpawned;
	private int bossTicks;
	private boolean dominion;
	private final Map<String, Integer> ranks;
	private final Map<String, List<String>> blueprints;
	private final Map<String, Integer> deathsByPlayer;
	private boolean outpostSpawned;
	private final Map<String, Integer> contributions;
	private final Map<String, Integer> nemesisKills;
	private boolean veteranTraderSpawned;
	private long nextEventTick;
	private final Map<String, Integer> antiAirKills;
	private final Map<String, Integer> civilianChickenKills;
	private final Map<String, WeatherSnapshot> weatherSnapshots;

	public PurgeWorldData() {
		this(false, 0, 0, BattleChickens.CONFIG.eventDays(), 0, 0, false, 0, false,
				Map.of(), Map.of(), Map.of(), false, Map.of(), Map.of(),
				new EventMeta(false, 0, Map.of(), Map.of(), Map.of()));
	}

	private PurgeWorldData(boolean active, long elapsedTicks, int threat, int eventDays, int currentDay,
						   int spawnedDay, boolean bossSpawned, int bossTicks, boolean dominion,
						   Map<String, Integer> ranks, Map<String, List<String>> blueprints,
						   Map<String, Integer> deathsByPlayer, boolean outpostSpawned,
						   Map<String, Integer> contributions, Map<String, Integer> nemesisKills,
						   EventMeta eventMeta) {
		this.active = active;
		this.elapsedTicks = elapsedTicks;
		this.threat = threat;
		this.eventDays = eventDays;
		this.currentDay = currentDay;
		this.spawnedDay = spawnedDay;
		this.bossSpawned = bossSpawned;
		this.bossTicks = bossTicks;
		this.dominion = dominion;
		this.ranks = new HashMap<>(ranks);
		this.blueprints = new HashMap<>();
		blueprints.forEach((player, known) -> this.blueprints.put(player, new ArrayList<>(known)));
		this.deathsByPlayer = new HashMap<>(deathsByPlayer);
		this.outpostSpawned = outpostSpawned;
		this.contributions = new HashMap<>(contributions);
		this.nemesisKills = new HashMap<>(nemesisKills);
		this.veteranTraderSpawned = eventMeta.veteranTraderSpawned();
		this.nextEventTick = eventMeta.nextEventTick();
		this.antiAirKills = new HashMap<>(eventMeta.antiAirKills());
		this.civilianChickenKills = new HashMap<>(eventMeta.civilianChickenKills());
		this.weatherSnapshots = new HashMap<>(eventMeta.weatherSnapshots());
	}

	public static PurgeWorldData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isActive() {
		return active;
	}

	public int threat() {
		return threat;
	}

	public int currentDay() {
		return currentDay;
	}

	public int eventDays() {
		return eventDays;
	}

	public boolean dominion() {
		return dominion;
	}

	public float progress() {
		return Math.min(1.0F, (float) elapsedTicks / (eventDays * 24_000.0F));
	}

	public boolean jumpToDay(int day) {
		if (!active || day < 1 || day > eventDays) {
			return false;
		}
		elapsedTicks = (day - 1L) * 24_000L;
		currentDay = day;
		setDirty();
		return true;
	}

	public boolean start(int threat, int eventDays, long currentWorldTick) {
		if (active || currentWorldTick < nextEventTick) {
			return false;
		}
		this.active = true;
		this.elapsedTicks = 0;
		this.threat = threat;
		this.eventDays = eventDays;
		this.currentDay = 1;
		this.spawnedDay = 0;
		this.bossSpawned = false;
		this.bossTicks = 0;
		this.deathsByPlayer.clear();
		this.civilianChickenKills.clear();
		this.contributions.clear();
		this.outpostSpawned = false;
		this.weatherSnapshots.clear();
		setDirty();
		return true;
	}

	public void stop(boolean failed, boolean dominionEnabled, long currentWorldTick, int cooldownSeconds) {
		active = false;
		dominion = failed && dominionEnabled;
		nextEventTick = currentWorldTick + cooldownSeconds * 20L;
		setDirty();
	}

	public void maintainBossStorm(Iterable<ServerLevel> levels) {
		boolean changed = false;
		for (ServerLevel level : levels) {
			if (!level.canHaveWeather()) {
				continue;
			}
			String dimension = level.dimension().identifier().toString();
			WeatherData weather = level.getWeatherData();
			if (weatherSnapshots.putIfAbsent(dimension, new WeatherSnapshot(
					weather.getClearWeatherTime(), weather.getRainTime(), weather.getThunderTime(),
					weather.isRaining(), weather.isThundering())) == null) {
				changed = true;
			}
			if (!weather.isRaining() || !weather.isThundering()
					|| weather.getRainTime() < 200 || weather.getThunderTime() < 200) {
				weather.setClearWeatherTime(0);
				weather.setRaining(true);
				weather.setThundering(true);
				weather.setRainTime(12_000);
				weather.setThunderTime(12_000);
			}
		}
		if (changed) {
			setDirty();
		}
	}

	public void restoreBossWeather(Iterable<ServerLevel> levels) {
		if (weatherSnapshots.isEmpty()) {
			return;
		}
		for (ServerLevel level : levels) {
			WeatherSnapshot snapshot = weatherSnapshots.get(level.dimension().identifier().toString());
			if (snapshot == null) {
				continue;
			}
			WeatherData weather = level.getWeatherData();
			weather.setClearWeatherTime(snapshot.clearTime());
			weather.setRainTime(snapshot.rainTime());
			weather.setThunderTime(snapshot.thunderTime());
			weather.setRaining(snapshot.raining());
			weather.setThundering(snapshot.thundering());
		}
		weatherSnapshots.clear();
		setDirty();
	}

	public int remainingCooldownSeconds(long currentWorldTick) {
		return (int) Math.max(0, Math.ceil((nextEventTick - currentWorldTick) / 20.0));
	}

	public int tick() {
		if (!active) {
			return currentDay;
		}
		elapsedTicks++;
		int nextDay = Math.min(eventDays, (int) (elapsedTicks / 24_000L) + 1);
		if (nextDay != currentDay) {
			currentDay = nextDay;
			setDirty();
		} else if (elapsedTicks % 200 == 0) {
			setDirty();
		}
		if (bossSpawned && currentDay == eventDays) {
			bossTicks++;
			if (bossTicks % 200 == 0) {
				setDirty();
			}
		}
		return currentDay;
	}

	public boolean markDaySpawned() {
		if (spawnedDay == currentDay) {
			return false;
		}
		spawnedDay = currentDay;
		setDirty();
		return true;
	}

	public boolean markOutpostSpawned() {
		if (outpostSpawned) {
			return false;
		}
		outpostSpawned = true;
		setDirty();
		return true;
	}

	public boolean outpostSpawned() {
		return outpostSpawned;
	}

	public boolean markBossSpawned() {
		if (bossSpawned) {
			return false;
		}
		bossSpawned = true;
		bossTicks = 0;
		setDirty();
		return true;
	}

	public int bossTicks() {
		return bossTicks;
	}

	public boolean bossSpawned() {
		return bossSpawned;
	}

	public int rank(String playerId) {
		return ranks.getOrDefault(playerId, 0);
	}

	public int grantVictory(String playerId, int threat) {
		int rank = Math.max(rank(playerId), Math.min(5, threat));
		ranks.put(playerId, rank);
		setDirty();
		return rank;
	}

	public boolean learnBlueprint(String playerId, String blueprint) {
		List<String> known = blueprints.computeIfAbsent(playerId, ignored -> new ArrayList<>());
		if (!known.add(blueprint)) {
			return false;
		}
		setDirty();
		return true;
	}

	public List<String> blueprints(String playerId) {
		return List.copyOf(blueprints.getOrDefault(playerId, List.of()));
	}

	public void markPlayerDeath(String playerId) {
		deathsByPlayer.put(playerId, currentDay);
		setDirty();
	}

	public int deaths(String playerId) {
		return deathsByPlayer.containsKey(playerId) ? 1 : 0;
	}

	public void recordAntiAirKill(String playerId) {
		antiAirKills.merge(playerId, 1, Integer::sum);
		setDirty();
	}

	public int antiAirKills(String playerId) {
		return antiAirKills.getOrDefault(playerId, 0);
	}

	public void recordCivilianChickenKill(String playerId) {
		civilianChickenKills.merge(playerId, 1, Integer::sum);
		setDirty();
	}

	public int civilianChickenKills(String playerId) {
		return civilianChickenKills.getOrDefault(playerId, 0);
	}

	public boolean survivedPreviousDay(String playerId, int newDay) {
		return deathsByPlayer.getOrDefault(playerId, 0) < newDay - 1;
	}

	public void addContribution(String playerId, int points) {
		if (points <= 0) {
			return;
		}
		contributions.merge(playerId, points, Integer::sum);
		setDirty();
	}

	public int contribution(String playerId) {
		return contributions.getOrDefault(playerId, 0);
	}

	public int highestContribution() {
		return contributions.values().stream().mapToInt(Integer::intValue).max().orElse(0);
	}

	public void recordNemesisKill(String playerId) {
		nemesisKills.merge(playerId, 1, Integer::sum);
		setDirty();
	}

	public int nemesisKills(String playerId) {
		return nemesisKills.getOrDefault(playerId, 0);
	}

	public boolean markVeteranTraderSpawned() {
		if (veteranTraderSpawned) {
			return false;
		}
		veteranTraderSpawned = true;
		setDirty();
		return true;
	}

	public boolean veteranTraderSpawned() {
		return veteranTraderSpawned;
	}
}
