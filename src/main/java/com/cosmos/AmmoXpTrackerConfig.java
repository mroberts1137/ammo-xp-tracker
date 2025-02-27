package com.cosmos;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ammoxptracker")
public interface AmmoXpTrackerConfig extends Config {
	@ConfigItem(
			keyName = "trackAllCombatXp",
			name = "Track All Combat XP",
			description = "Track XP from all combat skills, not just Ranged",
			position = 1
	)
	default boolean trackAllCombatXp() {
		return true;
	}
}
