package com.ruffensteint.mortimerslayertracker.service;

import com.google.gson.Gson;
import com.ruffensteint.mortimerslayertracker.model.SlayerHistory;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import net.runelite.client.RuneLite;

public class HistoryStore
{
	private static final String DIRECTORY_NAME = "mortimer-slayer-tracker";
	private static final String FILE_NAME = "task-history.json";

	private final Gson gson;
	private final Path historyDirectory;
	private final Path historyFile;

	@Inject
	public HistoryStore(Gson gson)
	{
		this(gson, RuneLite.RUNELITE_DIR.toPath().resolve(DIRECTORY_NAME));
	}

	HistoryStore(Gson gson, Path historyDirectory)
	{
		this.gson = gson.newBuilder().setPrettyPrinting().create();
		this.historyDirectory = historyDirectory;
		historyFile = historyDirectory.resolve(FILE_NAME);
	}

	public SlayerHistory load() throws IOException
	{
		if (!Files.exists(historyFile))
		{
			return new SlayerHistory();
		}

		try (Reader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8))
		{
			SlayerHistory history = gson.fromJson(reader, SlayerHistory.class);
			if (history == null)
			{
				history = new SlayerHistory();
			}
			history.normalize();
			return history;
		}
	}

	public void save(SlayerHistory history) throws IOException
	{
		Files.createDirectories(historyDirectory);
		Path temporaryFile = historyDirectory.resolve(FILE_NAME + ".new");
		try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8))
		{
			gson.toJson(history, writer);
		}

		try
		{
			Files.move(temporaryFile, historyFile, StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			Files.move(temporaryFile, historyFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
