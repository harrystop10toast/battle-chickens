package com.cluck.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record PurgeConfig(int eventDays, int hostileChickenCap, int bossTimeoutSeconds, int rocketBlockRadius,
						  int bossTerrainRadius, float peckzookaPvpDamageMultiplier, boolean dominionEnabled,
						  int rpgCapPerPlayer, float rewardMultiplier, String difficultyPreset,
						  int gearChancePerDay, int eventCooldownSeconds,
						  int naturalMilitaryChancePercent) {
	private static final int DEFAULT_EVENT_DAYS = 7;
	private static final int DEFAULT_HOSTILE_CHICKEN_CAP = 40;
	private static final int DEFAULT_BOSS_TIMEOUT_SECONDS = 1200;
	private static final int DEFAULT_ROCKET_BLOCK_RADIUS = 3;
	private static final int DEFAULT_BOSS_TERRAIN_RADIUS = 4;
	private static final float DEFAULT_PVP_DAMAGE_MULTIPLIER = 0.25F;
	private static final int DEFAULT_RPG_CAP_PER_PLAYER = 3;
	private static final float DEFAULT_REWARD_MULTIPLIER = 1.0F;
	private static final String DEFAULT_DIFFICULTY_PRESET = "standard";
	private static final int DEFAULT_GEAR_CHANCE_PER_DAY = 8;
	private static final int DEFAULT_EVENT_COOLDOWN_SECONDS = 1800;
	private static final int DEFAULT_NATURAL_MILITARY_CHANCE_PERCENT = 30;

	public static PurgeConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("battle-chickens.properties");
		Properties properties = new Properties();
		boolean writeConfig = false;
		if (Files.exists(path)) {
			try (var reader = Files.newBufferedReader(path)) {
				properties.load(reader);
			} catch (IOException exception) {
				throw new UncheckedIOException("Could not read " + path, exception);
			}
		} else {
			writeConfig = true;
			try {
				Files.createDirectories(path.getParent());
			} catch (IOException exception) {
				throw new UncheckedIOException("Could not create config directory for " + path, exception);
			}
		}

		int eventDays = intProperty(properties, path, "event_days", DEFAULT_EVENT_DAYS, 1, 30);
		int hostileChickenCap = intProperty(properties, path, "hostile_chicken_cap", DEFAULT_HOSTILE_CHICKEN_CAP, 1, 200);
		int bossTimeoutSeconds = intProperty(properties, path, "boss_timeout_seconds", DEFAULT_BOSS_TIMEOUT_SECONDS, 60, 86400);
		int rocketBlockRadius = intProperty(properties, path, "rocket_block_radius", DEFAULT_ROCKET_BLOCK_RADIUS, 0, 8);
		int bossTerrainRadius = intProperty(properties, path, "boss_terrain_radius", DEFAULT_BOSS_TERRAIN_RADIUS, 0, 8);
		float pvpDamage = floatProperty(properties, path, "peckzooka_pvp_damage_multiplier", DEFAULT_PVP_DAMAGE_MULTIPLIER, 0, 1);
		boolean dominionEnabled = booleanProperty(properties, path, "dominion_enabled", true);
		int rpgCapPerPlayer = intProperty(properties, path, "rpg_cap_per_player", DEFAULT_RPG_CAP_PER_PLAYER, 0, 20);
		float rewardMultiplier = floatProperty(properties, path, "reward_multiplier", DEFAULT_REWARD_MULTIPLIER, 0.1F, 5.0F);
		String difficultyPreset = stringProperty(properties, path, "difficulty_preset", DEFAULT_DIFFICULTY_PRESET,
				"casual", "standard", "hardcore");
		int gearChancePerDay = intProperty(properties, path, "gear_chance_per_day",
				DEFAULT_GEAR_CHANCE_PER_DAY, 0, 50);
		int eventCooldownSeconds = intProperty(properties, path, "event_cooldown_seconds",
				DEFAULT_EVENT_COOLDOWN_SECONDS, 0, 86400);
		int naturalMilitaryChancePercent = intProperty(properties, path, "natural_military_chance_percent",
				DEFAULT_NATURAL_MILITARY_CHANCE_PERCENT, 0, 100);
		writeConfig |= setDefault(properties, "event_days", Integer.toString(eventDays));
		writeConfig |= setDefault(properties, "hostile_chicken_cap", Integer.toString(hostileChickenCap));
		writeConfig |= setDefault(properties, "boss_timeout_seconds", Integer.toString(bossTimeoutSeconds));
		writeConfig |= setDefault(properties, "rocket_block_radius", Integer.toString(rocketBlockRadius));
		writeConfig |= setDefault(properties, "boss_terrain_radius", Integer.toString(bossTerrainRadius));
		writeConfig |= setDefault(properties, "peckzooka_pvp_damage_multiplier", Float.toString(pvpDamage));
		writeConfig |= setDefault(properties, "dominion_enabled", Boolean.toString(dominionEnabled));
		writeConfig |= setDefault(properties, "rpg_cap_per_player", Integer.toString(rpgCapPerPlayer));
		writeConfig |= setDefault(properties, "reward_multiplier", Float.toString(rewardMultiplier));
		writeConfig |= setDefault(properties, "difficulty_preset", difficultyPreset);
		writeConfig |= setDefault(properties, "gear_chance_per_day", Integer.toString(gearChancePerDay));
		writeConfig |= setDefault(properties, "event_cooldown_seconds", Integer.toString(eventCooldownSeconds));
		writeConfig |= setDefault(properties, "natural_military_chance_percent",
				Integer.toString(naturalMilitaryChancePercent));
		if (writeConfig) {
			try (var writer = Files.newBufferedWriter(path)) {
				properties.store(writer, "Chicken Overlord: The Great Purge");
			} catch (IOException exception) {
				throw new UncheckedIOException("Could not create " + path, exception);
			}
		}
		return new PurgeConfig(eventDays, hostileChickenCap, bossTimeoutSeconds, rocketBlockRadius, bossTerrainRadius, pvpDamage,
				dominionEnabled, rpgCapPerPlayer, rewardMultiplier, difficultyPreset,
				gearChancePerDay, eventCooldownSeconds, naturalMilitaryChancePercent);
	}

	public float enemyDamageMultiplier() {
		return switch (difficultyPreset) {
			case "casual" -> 0.75F;
			case "hardcore" -> 1.5F;
			default -> 1.0F;
		};
	}

	public float enemyHealthMultiplier() {
		return switch (difficultyPreset) {
			case "casual" -> 0.9F;
			case "hardcore" -> 1.3F;
			default -> 1.0F;
		};
	}

	private static boolean setDefault(Properties properties, String key, String value) {
		if (properties.containsKey(key)) {
			return false;
		}
		properties.setProperty(key, value);
		return true;
	}

	private static int intProperty(Properties properties, Path path, String key, int fallback, int min, int max) {
		int value;
		try {
			value = Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid " + key + " in " + path + "; expected a whole number from " + min + " to " + max + ".", exception);
		}
		if (value < min || value > max) {
			throw new IllegalArgumentException("Invalid " + key + " in " + path + "; expected a whole number from " + min + " to " + max + ".");
		}
		return value;
	}

	private static float floatProperty(Properties properties, Path path, String key, float fallback, float min, float max) {
		float value;
		try {
			value = Float.parseFloat(properties.getProperty(key, Float.toString(fallback)).trim());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid " + key + " in " + path + "; expected a number from " + min + " to " + max + ".", exception);
		}
		if (!Float.isFinite(value) || value < min || value > max) {
			throw new IllegalArgumentException("Invalid " + key + " in " + path + "; expected a number from " + min + " to " + max + ".");
		}
		return value;
	}

	private static boolean booleanProperty(Properties properties, Path path, String key, boolean fallback) {
		String value = properties.getProperty(key, Boolean.toString(fallback)).trim();
		if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
			throw new IllegalArgumentException("Invalid " + key + " in " + path + "; expected true or false.");
		}
		return Boolean.parseBoolean(value);
	}

	private static String stringProperty(Properties properties, Path path, String key, String fallback,
										 String... allowedValues) {
		String value = properties.getProperty(key, fallback).trim().toLowerCase(java.util.Locale.ROOT);
		for (String allowed : allowedValues) {
			if (value.equals(allowed)) {
				return value;
			}
		}
		throw new IllegalArgumentException("Invalid " + key + " in " + path + "; expected one of "
				+ String.join(", ", allowedValues) + ".");
	}
}
