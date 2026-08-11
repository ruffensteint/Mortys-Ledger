package com.ruffensteint.mortimerslayertracker.service;

import com.ruffensteint.mortimerslayertracker.model.SlayerAssignment;
import com.ruffensteint.mortimerslayertracker.model.SlayerHistory;
import com.ruffensteint.mortimerslayertracker.model.SlayerTaskRecord;
import java.util.Objects;

public class TaskTracker
{
	private boolean assignmentVisible;

	public boolean startAssignment(SlayerHistory history, SlayerAssignment assignment)
	{
		SlayerTaskRecord activeTask = history.getActiveTask();
		if (matches(activeTask, assignment))
		{
			assignmentVisible = true;
			return false;
		}
		if (assignmentVisible && matches(latestTask(history), assignment))
		{
			return false;
		}

		if (activeTask != null)
		{
			complete(activeTask, assignment.getSlayerXp());
		}

		SlayerTaskRecord task = new SlayerTaskRecord();
		task.setTaskNumber(history.getMortimerTaskCount() + 1);
		task.setMonster(assignment.getMonster());
		task.setAssignedAmount(assignment.getAssignedAmount());
		task.setModifier(assignment.getModifier());
		task.setStartSlayerXp(assignment.getSlayerXp());
		history.addTask(task);
		assignmentVisible = true;
		return true;
	}

	public void clearAssignment()
	{
		assignmentVisible = false;
	}

	public boolean completeAssignment(SlayerHistory history, int endSlayerXp)
	{
		SlayerTaskRecord activeTask = history.getActiveTask();
		if (activeTask == null)
		{
			return false;
		}

		complete(activeTask, endSlayerXp);
		return true;
	}

	private static boolean matches(SlayerTaskRecord activeTask, SlayerAssignment assignment)
	{
		return activeTask != null
			&& Objects.equals(activeTask.getMonster(), assignment.getMonster())
			&& activeTask.getAssignedAmount() == assignment.getAssignedAmount()
			&& Objects.equals(activeTask.getModifier(), assignment.getModifier());
	}

	private static SlayerTaskRecord latestTask(SlayerHistory history)
	{
		if (history.getTasks().isEmpty())
		{
			return null;
		}
		return history.getTasks().get(history.getTasks().size() - 1);
	}

	private static void complete(SlayerTaskRecord task, int endSlayerXp)
	{
		task.setEndSlayerXp(endSlayerXp);
		task.setCompleted(true);
	}
}
