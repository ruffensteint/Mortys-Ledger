package com.ruffensteint.mortimerslayertracker.service;

import com.ruffensteint.mortimerslayertracker.model.SlayerAssignment;
import com.ruffensteint.mortimerslayertracker.model.SlayerHistory;
import com.ruffensteint.mortimerslayertracker.model.SlayerTaskRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskTrackerTest
{
	private final TaskTracker tracker = new TaskTracker();

	@Test
	public void startsNumberedAssignmentFromCompletionCount()
	{
		SlayerHistory history = new SlayerHistory();
		history.setMortimerTaskCount(42);

		assertTrue(tracker.startAssignment(history,
			new SlayerAssignment("Gryphons", 114, "Superior unique (+105)", 105, false, 1_000_000)));

		SlayerTaskRecord task = history.getActiveTask();
		assertEquals(43, task.getTaskNumber());
		assertEquals("Gryphons", task.getMonster());
		assertEquals(114, task.getAssignedAmount());
		assertEquals(1_000_000, task.getStartSlayerXp());
	}

	@Test
	public void ignoresDuplicateAssignmentUpdates()
	{
		SlayerHistory history = new SlayerHistory();
		SlayerAssignment assignment = new SlayerAssignment("Gargoyles", 158, "Points (+30)", 30, false, 2_000);

		assertTrue(tracker.startAssignment(history, assignment));
		assertFalse(tracker.startAssignment(history, assignment));
		assertEquals(1, history.getTasks().size());
	}

	@Test
	public void completesAssignmentAndCalculatesXp()
	{
		SlayerHistory history = new SlayerHistory();
		tracker.startAssignment(history, new SlayerAssignment("Pyrefiends", 41, "Clues (+110)", 110, false, 5_000));

		assertTrue(tracker.completeAssignment(history, 6_800));
		assertFalse(tracker.completeAssignment(history, 7_000));
		assertTrue(history.getTasks().get(0).isCompleted());
		assertEquals(1_800, history.getTasks().get(0).getSlayerXpGained());
	}

	@Test
	public void doesNotReopenLingeringCompletedAssignment()
	{
		SlayerHistory history = new SlayerHistory();
		SlayerAssignment assignment = new SlayerAssignment("Wyrms", 106, "Superior unique (+205)", 205, false, 10_000);
		tracker.startAssignment(history, assignment);
		tracker.completeAssignment(history, 20_800);

		assertFalse(tracker.startAssignment(history, assignment));
		assertEquals(1, history.getTasks().size());

		tracker.clearAssignment();
		assertTrue(tracker.startAssignment(history, assignment));
		assertEquals(2, history.getTasks().size());
	}

	@Test
	public void countsCluesOnlyForClueModifier()
	{
		SlayerHistory clueHistory = new SlayerHistory();
		tracker.startAssignment(clueHistory,
			new SlayerAssignment("Pyrefiends", 41, "Clue Drop Increase (+110)", 110, false, 5_000));
		assertTrue(tracker.recordClueDrop(clueHistory, 1));
		assertEquals(1, clueHistory.getActiveTask().getClueDropCount());

		TaskTracker otherTracker = new TaskTracker();
		SlayerHistory pointsHistory = new SlayerHistory();
		otherTracker.startAssignment(pointsHistory,
			new SlayerAssignment("Bloodveld", 178, "Slayer Points Increase (+10)", 10, false, 5_000));
		assertFalse(otherTracker.recordClueDrop(pointsHistory, 1));
		assertEquals(0, pointsHistory.getActiveTask().getClueDropCount());
	}

	@Test
	public void recordsSuperiorCountAndLootForEveryModifier()
	{
		SlayerHistory history = new SlayerHistory();
		tracker.startAssignment(history,
			new SlayerAssignment("Bloodveld", 178, "Slayer Points Increase (+10)", 10, false, 5_000));

		assertTrue(tracker.recordSuperiorSpawn(history));
		assertTrue(tracker.recordSuperiorLoot(history, 1391, "Battlestaff", 2));
		assertTrue(tracker.recordSuperiorLoot(history, 1391, "Battlestaff", 1));

		assertEquals(1, history.getActiveTask().getSuperiorCount());
		assertEquals(1, history.getActiveTask().getSuperiorLoot().size());
		assertEquals(3, history.getActiveTask().getSuperiorLoot().get(0).getQuantity());
	}

	@Test
	public void splitsObservedXpIntoBaseAndBonus()
	{
		SlayerHistory history = new SlayerHistory();
		tracker.startAssignment(history,
			new SlayerAssignment("Gargoyles", 100, "Slayer XP Increase (+100)", 100, false, 5_000));

		assertTrue(tracker.updateSlayerXp(history, 5_020));
		assertEquals(10, history.getActiveTask().getBaseSlayerXp());
		assertEquals(10, history.getActiveTask().getBonusSlayerXp());
	}

	@Test
	public void recalculatesInsteadOfAccumulatingAbsoluteXp()
	{
		SlayerHistory history = new SlayerHistory();
		tracker.startAssignment(history,
			new SlayerAssignment("Bloodveld", 178, "Slayer Points Increase (+10)", 10, false, 11_928_208));

		assertTrue(tracker.updateSlayerXp(history, 11_929_498));
		assertEquals(1_290, history.getActiveTask().getBaseSlayerXp());
		assertEquals(0, history.getActiveTask().getBonusSlayerXp());

		assertTrue(tracker.updateSlayerXp(history, 11_929_600));
		assertEquals(1_392, history.getActiveTask().getBaseSlayerXp());
	}
}
