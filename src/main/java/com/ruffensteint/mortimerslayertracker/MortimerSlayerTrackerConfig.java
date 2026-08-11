package com.ruffensteint.mortimerslayertracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(MortimerSlayerTrackerConfig.GROUP)
public interface MortimerSlayerTrackerConfig extends Config
{
	String GROUP = "mortimerSlayerTracker";

	@ConfigItem(
		keyName = "showRoadTo99",
		name = "Show Road to 99",
		description = "Show Road to 99 progress in Mortimer Slayer reports"
	)
	default boolean showRoadTo99()
	{
		return true;
	}
}
