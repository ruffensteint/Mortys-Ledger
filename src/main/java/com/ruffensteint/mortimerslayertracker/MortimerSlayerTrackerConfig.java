package com.ruffensteint.mortimerslayertracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(MortimerSlayerTrackerConfig.GROUP)
public interface MortimerSlayerTrackerConfig extends Config
{
	String GROUP = "mortimerSlayerTracker";
	String DISCORD_WEBHOOK_ENABLED_KEY = "discordWebhookEnabled";
	String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	@ConfigItem(
		keyName = "showRoadTo99",
		name = "Show Road to 99",
		description = "Show Road to 99 progress in Morty's Ledger reports"
	)
	default boolean showRoadTo99()
	{
		return true;
	}

	@ConfigItem(
		keyName = DISCORD_WEBHOOK_ENABLED_KEY,
		name = "Enable Discord webhook",
		description = "Post Mortimer task announcements to a Discord channel",
		warning = THIRD_PARTY_WARNING
	)
	default boolean discordWebhookEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "discordWebhookUrl",
		name = "Discord webhook URL",
		description = "Webhook URL copied from the target Discord channel",
		secret = true
	)
	default String discordWebhookUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discordNewTaskTitle",
		name = "New task title",
		description = "Title used for new-task Discord announcements"
	)
	default String discordNewTaskTitle()
	{
		return "New Mortimer Task";
	}

	@ConfigItem(
		keyName = "discordCompletedTaskTitle",
		name = "Completed task title",
		description = "Title used for completed-task Discord announcements"
	)
	default String discordCompletedTaskTitle()
	{
		return "Mortimer Task Complete";
	}
}
