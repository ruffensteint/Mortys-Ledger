package com.ruffensteint.mortimerslayertracker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlayerTaskRecord
{
	private int taskNumber;
	private String monster;
	private int assignedAmount;
	private String modifier;
	private int modifierValue;
	private boolean modifierNegative;
	private int startSlayerXp;
	private int endSlayerXp;
	private int baseSlayerXp;
	private int bonusSlayerXp;
	private int superiorCount;
	private int clueDropCount;
	private List<LootItemRecord> superiorLoot = new ArrayList<>();
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
		modifierValue = other.modifierValue;
		modifierNegative = other.modifierNegative;
		startSlayerXp = other.startSlayerXp;
		endSlayerXp = other.endSlayerXp;
		baseSlayerXp = other.baseSlayerXp;
		bonusSlayerXp = other.bonusSlayerXp;
		superiorCount = other.superiorCount;
		clueDropCount = other.clueDropCount;
		for (LootItemRecord item : other.superiorLoot)
		{
			superiorLoot.add(new LootItemRecord(item));
		}
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

	public int getModifierValue()
	{
		return modifierValue;
	}

	public void setModifierValue(int modifierValue)
	{
		this.modifierValue = modifierValue;
	}

	public boolean isModifierNegative()
	{
		return modifierNegative;
	}

	public void setModifierNegative(boolean modifierNegative)
	{
		this.modifierNegative = modifierNegative;
	}

	public boolean isClueModifier()
	{
		return modifier != null && modifier.toLowerCase().contains("clue");
	}

	public boolean isSlayerXpModifier()
	{
		return modifier != null && modifier.toLowerCase().contains("slayer xp");
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

	public int getBaseSlayerXp()
	{
		return baseSlayerXp;
	}

	public int getBonusSlayerXp()
	{
		return bonusSlayerXp;
	}

	public void addSlayerXp(int baseXp, int bonusXp)
	{
		baseSlayerXp += baseXp;
		bonusSlayerXp += bonusXp;
	}

	public void setSlayerXpBreakdown(int baseXp, int bonusXp)
	{
		baseSlayerXp = baseXp;
		bonusSlayerXp = bonusXp;
	}

	public int getSuperiorCount()
	{
		return superiorCount;
	}

	public void setSuperiorCount(int superiorCount)
	{
		this.superiorCount = superiorCount;
	}

	public void incrementSuperiorCount()
	{
		superiorCount++;
	}

	public int getClueDropCount()
	{
		return clueDropCount;
	}

	public void setClueDropCount(int clueDropCount)
	{
		this.clueDropCount = clueDropCount;
	}

	public void addClueDrops(int count)
	{
		clueDropCount += count;
	}

	public List<LootItemRecord> getSuperiorLoot()
	{
		return Collections.unmodifiableList(superiorLoot);
	}

	public void addSuperiorLoot(int itemId, String name, int quantity)
	{
		for (LootItemRecord item : superiorLoot)
		{
			if (item.getItemId() == itemId)
			{
				item.addQuantity(quantity);
				return;
			}
		}
		superiorLoot.add(new LootItemRecord(itemId, name, quantity));
	}

	public void normalize()
	{
		if (superiorLoot == null)
		{
			superiorLoot = new ArrayList<>();
		}
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
