package com.ruffensteint.mortimerslayertracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Mortimer Slayer Tracker",
	description = "Tracks Mortimer Slayer assignments and Road to 99 progress",
	tags = {"slayer", "mortimer", "tracker", "discord"}
)
public class MortimerSlayerTrackerPlugin extends Plugin
{
	@Inject
	private MortimerSlayerTrackerConfig config;

	@Override
	protected void startUp()
	{
		log.debug("Mortimer Slayer Tracker started; Road to 99 display: {}", config.showRoadTo99());
	}

	@Override
	protected void shutDown()
	{
		log.debug("Mortimer Slayer Tracker stopped");
	}

	@Provides
	MortimerSlayerTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MortimerSlayerTrackerConfig.class);
	}
}
