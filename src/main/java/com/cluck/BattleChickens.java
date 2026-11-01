package com.cluck;

import com.cluck.config.PurgeConfig;
import com.cluck.event.PurgeManager;
import com.cluck.entity.ModEntities;
import com.cluck.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BattleChickens implements ModInitializer {
	public static final String MOD_ID = "battle-chickens";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static PurgeConfig CONFIG;

	@Override
	public void onInitialize() {
		CONFIG = PurgeConfig.load();
		ModEntities.register();
		ModItems.register();
		PurgeManager.register();
		LOGGER.info("Chicken Overlord: The Great Purge foundation loaded.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
