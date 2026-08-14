package com.ruffensteint.mortimerslayertracker;

import com.google.inject.Provides;
import com.ruffensteint.mortimerslayertracker.model.SlayerAssignment;
import com.ruffensteint.mortimerslayertracker.model.SlayerHistory;
import com.ruffensteint.mortimerslayertracker.parser.MortimerChatParser;
import com.ruffensteint.mortimerslayertracker.service.HistoryStore;
import com.ruffensteint.mortimerslayertracker.service.DiscordWebhookClient;
import com.ruffensteint.mortimerslayertracker.service.SlayerAssignmentReader;
import com.ruffensteint.mortimerslayertracker.service.SuperiorNpc;
import com.ruffensteint.mortimerslayertracker.service.TaskTracker;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
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
	private static final String SUPERIOR_SPAWN_MESSAGE = "A superior foe has appeared...";

	@Inject
	private MortimerSlayerTrackerConfig config;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private HistoryStore historyStore;

	@Inject
	private SlayerAssignmentReader assignmentReader;

	@Inject
	private ItemManager itemManager;

	@Inject
	private DiscordWebhookClient discordWebhookClient;

	private final MortimerChatParser chatParser = new MortimerChatParser();
	private final TaskTracker taskTracker = new TaskTracker();
	private ExecutorService persistenceExecutor;
	private SlayerHistory history = new SlayerHistory();
	private volatile boolean active;
	private boolean historyReady;

	@Override
	protected void startUp()
	{
		active = true;
		historyReady = false;
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
		historyReady = false;
		log.debug("Mortimer Slayer Tracker stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::updateAssignment);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (MortimerSlayerTrackerConfig.GROUP.equals(event.getGroup())
			&& MortimerSlayerTrackerConfig.DISCORD_WEBHOOK_ENABLED_KEY.equals(event.getKey())
			&& config.discordWebhookEnabled()
			&& historyReady
			&& history.getActiveTask() != null)
		{
			discordWebhookClient.sendTaskStarted(
				config.discordWebhookUrl(), config.discordNewTaskTitle(), getPlayerName(), history.getActiveTask());
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varpId = event.getVarpId();
		int varbitId = event.getVarbitId();
		if (varpId == VarPlayerID.SLAYER_COUNT
			|| varpId == VarPlayerID.SLAYER_TARGET
			|| varpId == VarPlayerID.SLAYER_COUNT_ORIGINAL
			|| varbitId == VarbitID.SLAYER_TARGET_BOSSID
			|| varbitId == VarbitID.SLAYER_MODIFIER_ID
			|| varbitId == VarbitID.SLAYER_MODIFIER_VALUE
			|| varbitId == VarbitID.SLAYER_MODIFIER_NEGATIVE)
		{
			clientThread.invokeLater(this::updateAssignment);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage());
		if (SUPERIOR_SPAWN_MESSAGE.equals(message) && taskTracker.recordSuperiorSpawn(history))
		{
			queueSave(new SlayerHistory(history));
			log.debug("Recorded superior spawn for active Mortimer assignment");
		}

		OptionalInt completedTaskCount = chatParser.parseCompletedTaskCount(message);
		if (!completedTaskCount.isPresent())
		{
			return;
		}

		history.setMortimerTaskCount(completedTaskCount.getAsInt());
		if (taskTracker.completeAssignment(history, client.getSkillExperience(Skill.SLAYER)))
		{
			log.debug("Completed tracked Mortimer assignment");
			if (config.discordWebhookEnabled())
			{
				discordWebhookClient.sendTaskCompleted(
					config.discordWebhookUrl(),
					config.discordCompletedTaskTitle(),
					getPlayerName(),
					history.getTasks().get(history.getTasks().size() - 1));
			}
		}
		queueSave(new SlayerHistory(history));
		log.debug("Recorded Mortimer completed task count: {}", completedTaskCount.getAsInt());
	}

	@Subscribe
	public void onServerNpcLoot(ServerNpcLoot event)
	{
		if (!historyReady || history.getActiveTask() == null)
		{
			return;
		}

		boolean superior = SuperiorNpc.isSuperior(event.getComposition().getId());
		boolean changed = false;
		for (ItemStack item : event.getItems())
		{
			String itemName = itemManager.getItemComposition(item.getId()).getName();
			if (superior)
			{
				changed |= taskTracker.recordSuperiorLoot(
					history, item.getId(), itemName, item.getQuantity());
			}
			if (itemName.toLowerCase().startsWith("clue scroll"))
			{
				changed |= taskTracker.recordClueDrop(history, item.getQuantity());
			}
		}

		if (changed)
		{
			queueSave(new SlayerHistory(history));
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.SLAYER || history.getActiveTask() == null)
		{
			return;
		}

		updateAssignment();
		if (taskTracker.updateSlayerXp(history, event.getXp()))
		{
			queueSave(new SlayerHistory(history));
		}
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
					historyReady = true;
					log.debug("Loaded {} Mortimer Slayer task records", history.getTasks().size());
					updateAssignment();
				}
			});
		}
		catch (IOException | RuntimeException ex)
		{
			log.debug("Unable to load Mortimer Slayer history", ex);
		}
	}

	private void updateAssignment()
	{
		if (!active || !historyReady || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Optional<SlayerAssignment> assignment = assignmentReader.readMortimerAssignment();
		if (!assignment.isPresent())
		{
			taskTracker.clearAssignment();
			return;
		}

		boolean started = taskTracker.startAssignment(history, assignment.get());
		if (started)
		{
			queueSave(new SlayerHistory(history));
			if (config.discordWebhookEnabled())
			{
				discordWebhookClient.sendTaskStarted(
					config.discordWebhookUrl(), config.discordNewTaskTitle(), getPlayerName(), history.getActiveTask());
			}
			log.debug("Started Mortimer assignment #{}: {} x{}",
				history.getMortimerTaskCount() + 1,
				assignment.get().getMonster(),
				assignment.get().getAssignedAmount());
		}
	}

	private String getPlayerName()
	{
		return client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();
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
