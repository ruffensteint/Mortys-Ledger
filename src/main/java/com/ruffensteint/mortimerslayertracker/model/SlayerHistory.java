package com.ruffensteint.mortimerslayertracker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlayerHistory
{
	private int schemaVersion = 1;
	private int mortimerTaskCount;
	private List<SlayerTaskRecord> tasks = new ArrayList<>();

	public SlayerHistory()
	{
	}

	public SlayerHistory(SlayerHistory other)
	{
		schemaVersion = other.schemaVersion;
		mortimerTaskCount = other.mortimerTaskCount;
		for (SlayerTaskRecord task : other.tasks)
		{
			tasks.add(new SlayerTaskRecord(task));
		}
	}

	public int getSchemaVersion()
	{
		return schemaVersion;
	}

	public int getMortimerTaskCount()
	{
		return mortimerTaskCount;
	}

	public void setMortimerTaskCount(int mortimerTaskCount)
	{
		this.mortimerTaskCount = mortimerTaskCount;
	}

	public List<SlayerTaskRecord> getTasks()
	{
		return Collections.unmodifiableList(tasks);
	}

	public void addTask(SlayerTaskRecord task)
	{
		tasks.add(task);
	}

	public SlayerTaskRecord getActiveTask()
	{
		for (int i = tasks.size() - 1; i >= 0; i--)
		{
			SlayerTaskRecord task = tasks.get(i);
			if (!task.isCompleted())
			{
				return task;
			}
		}
		return null;
	}

	public void normalize()
	{
		if (schemaVersion <= 0)
		{
			schemaVersion = 1;
		}
		if (tasks == null)
		{
			tasks = new ArrayList<>();
		}
	}
}
