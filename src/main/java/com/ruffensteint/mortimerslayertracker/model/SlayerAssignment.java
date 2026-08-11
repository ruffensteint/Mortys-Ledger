package com.ruffensteint.mortimerslayertracker.model;

import java.util.Objects;

public class SlayerAssignment
{
	private final String monster;
	private final int assignedAmount;
	private final String modifier;
	private final int slayerXp;

	public SlayerAssignment(String monster, int assignedAmount, String modifier, int slayerXp)
	{
		this.monster = Objects.requireNonNull(monster);
		this.assignedAmount = assignedAmount;
		this.modifier = modifier == null ? "" : modifier;
		this.slayerXp = slayerXp;
	}

	public String getMonster()
	{
		return monster;
	}

	public int getAssignedAmount()
	{
		return assignedAmount;
	}

	public String getModifier()
	{
		return modifier;
	}

	public int getSlayerXp()
	{
		return slayerXp;
	}
}
