package com.ruffensteint.mortimerslayertracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MortimerSlayerTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MortimerSlayerTrackerPlugin.class);
		RuneLite.main(args);
	}
}
