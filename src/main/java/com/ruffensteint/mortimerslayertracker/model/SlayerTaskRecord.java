package com.ruffensteint.mortimerslayertracker.model;

public class SlayerTaskRecord
{
	private int taskNumber;
	private String monster;
	private int assignedAmount;
	private String modifier;
	private int startSlayerXp;
	private int endSlayerXp;
	private int superiorCount;
	private int clueDropCount;
	private boolean completed;

	public SlayerTaskRecord()
	{
	}

	public SlayerTaskRecord(SlayerTaskRecord other)
	{
		taskNumber = other.taskNumber;
		monster = other.monster;
		assignedAmount = other.assignedAmount;
		modifier = other.modifier;
		startSlayerXp = other.startSlayerXp;
		endSlayerXp = other.endSlayerXp;
		superiorCount = other.superiorCount;
		clueDropCount = other.clueDropCount;
		completed = other.completed;
	}

	public int getTaskNumber()
	{
		return taskNumber;
	}

	public void setTaskNumber(int taskNumber)
	{
		this.taskNumber = taskNumber;
	}

	public String getMonster()
	{
		return monster;
	}

	public void setMonster(String monster)
	{
		this.monster = monster;
	}

	public int getAssignedAmount()
	{
		return assignedAmount;
	}

	public void setAssignedAmount(int assignedAmount)
	{
		this.assignedAmount = assignedAmount;
	}

	public String getModifier()
	{
		return modifier;
	}

	public void setModifier(String modifier)
	{
		this.modifier = modifier;
	}

	public int getStartSlayerXp()
	{
		return startSlayerXp;
	}

	public void setStartSlayerXp(int startSlayerXp)
	{
		this.startSlayerXp = startSlayerXp;
	}

	public int getEndSlayerXp()
	{
		return endSlayerXp;
	}

	public void setEndSlayerXp(int endSlayerXp)
	{
		this.endSlayerXp = endSlayerXp;
	}

	public int getSlayerXpGained()
	{
		return Math.max(0, endSlayerXp - startSlayerXp);
	}

	public int getSuperiorCount()
	{
		return superiorCount;
	}

	public void setSuperiorCount(int superiorCount)
	{
		this.superiorCount = superiorCount;
	}

	public int getClueDropCount()
	{
		return clueDropCount;
	}

	public void setClueDropCount(int clueDropCount)
	{
		this.clueDropCount = clueDropCount;
	}

	public boolean isCompleted()
	{
		return completed;
	}

	public void setCompleted(boolean completed)
	{
		this.completed = completed;
	}
}
