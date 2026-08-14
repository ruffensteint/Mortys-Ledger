package com.ruffensteint.mortimerslayertracker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SlayerHistory
{
	private int schemaVersion = 1;
	private int mortimerTaskCount;
	private List<SlayerTaskRecord> tasks = new ArrayList<>();
	private Map<String, MonsterGearPreference> gearPreferences = new LinkedHashMap<>();

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
		for (Map.Entry<String, MonsterGearPreference> entry : other.gearPreferences.entrySet())
		{
			gearPreferences.put(entry.getKey(), new MonsterGearPreference(entry.getValue()));
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

	public MonsterGearPreference getGearPreference(String monster)
	{
		if (monster == null)
		{
			return null;
		}
		return gearPreferences.get(monster.trim().toLowerCase(Locale.ENGLISH));
	}

	public void setWeaponPreference(String monster, int itemId)
	{
		gearPreference(monster).setWeaponItemId(itemId);
	}

	public void setShieldPreference(String monster, int itemId)
	{
		gearPreference(monster).setShieldItemId(itemId);
	}

	public void setSlayerItemPreference(String monster, int itemId)
	{
		gearPreference(monster).setSlayerItemId(itemId);
	}

	public void setCannonPreference(String monster, boolean enabled)
	{
		gearPreference(monster).setCannonEnabled(enabled);
	}

	private MonsterGearPreference gearPreference(String monster)
	{
		String key = monster.trim().toLowerCase(Locale.ENGLISH);
		return gearPreferences.computeIfAbsent(key, ignored -> new MonsterGearPreference());
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
		if (gearPreferences == null)
		{
			gearPreferences = new LinkedHashMap<>();
		}
		for (SlayerTaskRecord task : tasks)
		{
			task.normalize();
		}
	}
}
