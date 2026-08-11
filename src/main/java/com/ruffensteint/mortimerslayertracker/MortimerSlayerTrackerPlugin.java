package com.ruffensteint.mortimerslayertracker;

import com.google.inject.Provides;
import com.ruffensteint.mortimerslayertracker.model.SlayerHistory;
import com.ruffensteint.mortimerslayertracker.parser.MortimerChatParser;
import com.ruffensteint.mortimerslayertracker.service.HistoryStore;
import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

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

	@Inject
	private ClientThread clientThread;

	@Inject
	private HistoryStore historyStore;

	private final MortimerChatParser chatParser = new MortimerChatParser();
	private ExecutorService persistenceExecutor;
	private SlayerHistory history = new SlayerHistory();
	private volatile boolean active;

	@Override
	protected void startUp()
	{
		active = true;
		persistenceExecutor = Executors.newSingleThreadExecutor(r ->
		{
			Thread thread = new Thread(r, "mortimer-slayer-history");
			thread.setDaemon(true);
			return thread;
		});
		persistenceExecutor.execute(this::loadHistory);
		log.debug("Mortimer Slayer Tracker started; Road to 99 display: {}", config.showRoadTo99());
	}

	@Override
	protected void shutDown()
	{
		active = false;
		if (persistenceExecutor != null)
		{
			persistenceExecutor.shutdownNow();
			persistenceExecutor = null;
		}
		history = new SlayerHistory();
		log.debug("Mortimer Slayer Tracker stopped");
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		OptionalInt completedTaskCount = chatParser.parseCompletedTaskCount(Text.removeTags(event.getMessage()));
		if (!completedTaskCount.isPresent())
		{
			return;
		}

		history.setMortimerTaskCount(completedTaskCount.getAsInt());
		queueSave(new SlayerHistory(history));
		log.debug("Recorded Mortimer completed task count: {}", completedTaskCount.getAsInt());
	}

	private void loadHistory()
	{
		try
		{
			SlayerHistory loadedHistory = historyStore.load();
			clientThread.invoke(() ->
			{
				if (active)
				{
					loadedHistory.setMortimerTaskCount(Math.max(
						loadedHistory.getMortimerTaskCount(), history.getMortimerTaskCount()));
					history = loadedHistory;
					log.debug("Loaded {} Mortimer Slayer task records", history.getTasks().size());
				}
			});
		}
		catch (IOException | RuntimeException ex)
		{
			log.debug("Unable to load Mortimer Slayer history", ex);
		}
	}

	private void queueSave(SlayerHistory snapshot)
	{
		ExecutorService executor = persistenceExecutor;
		if (executor == null || executor.isShutdown())
		{
			return;
		}

		executor.execute(() ->
		{
			try
			{
				historyStore.save(snapshot);
			}
			catch (IOException ex)
			{
				log.debug("Unable to save Mortimer Slayer history", ex);
			}
		});
	}

	@Provides
	MortimerSlayerTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MortimerSlayerTrackerConfig.class);
	}
}
