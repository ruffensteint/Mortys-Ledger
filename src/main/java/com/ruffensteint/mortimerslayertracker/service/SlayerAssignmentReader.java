package com.ruffensteint.mortimerslayertracker.service;

import com.ruffensteint.mortimerslayertracker.model.SlayerAssignment;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

public class SlayerAssignmentReader
{
	private final Client client;

	@Inject
	public SlayerAssignmentReader(Client client)
	{
		this.client = client;
	}

	public Optional<SlayerAssignment> readMortimerAssignment()
	{
		int amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		int modifierId = client.getVarbitValue(VarbitID.SLAYER_MODIFIER_ID);
		int slayerXp = client.getSkillExperience(Skill.SLAYER);
		if (amount <= 0 || modifierId <= 0 || slayerXp <= 0)
		{
			return Optional.empty();
		}

		Optional<String> taskName = readTaskName();
		if (!taskName.isPresent())
		{
			return Optional.empty();
		}

		int assignedAmount = client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL);
		String modifier = readModifierName(modifierId).orElse("Modifier " + modifierId);
		int modifierValue = client.getVarbitValue(VarbitID.SLAYER_MODIFIER_VALUE);
		boolean negative = client.getVarbitValue(VarbitID.SLAYER_MODIFIER_NEGATIVE) == 1;
		if (modifierValue > 0)
		{
			modifier += " (" + (negative ? "-" : "+") + modifierValue + ")";
		}

		return Optional.of(new SlayerAssignment(
			taskName.get(),
			assignedAmount > 0 ? assignedAmount : amount,
			modifier,
			modifierValue,
			negative,
			slayerXp));
	}

	private Optional<String> readTaskName()
	{
		int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		List<Integer> taskRows = client.getDBRowsByValue(
			DBTableID.SlayerTask.ID,
			DBTableID.SlayerTask.COL_ID,
			0,
			taskId);
		if (taskRows.isEmpty())
		{
			return Optional.empty();
		}

		int taskRow = taskRows.get(0);
		if (taskRow == DBTableID.SlayerTask.Row.SLAYER_TARGET_BOSS)
		{
			List<Integer> bossRows = client.getDBRowsByValue(
				DBTableID.SlayerTaskSublist.ID,
				DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
				0,
				client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
			if (bossRows.isEmpty())
			{
				return Optional.empty();
			}
			taskRow = (Integer) client.getDBTableField(
				bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
		}

		return Optional.of((String) client.getDBTableField(
			taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0]);
	}

	private Optional<String> readModifierName(int modifierId)
	{
		List<Integer> modifierRows = client.getDBRowsByValue(
			DBTableID.SlayerModifiers.ID,
			DBTableID.SlayerModifiers.COL_ID,
			0,
			modifierId);
		if (modifierRows.isEmpty())
		{
			return Optional.empty();
		}

		return Optional.of((String) client.getDBTableField(
			modifierRows.get(0), DBTableID.SlayerModifiers.COL_NAME, 0)[0]);
	}
}
