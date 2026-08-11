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
			new SlayerAssignment("Gryphons", 114, "Superior unique (+105)", 1_000_000)));

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
		SlayerAssignment assignment = new SlayerAssignment("Gargoyles", 158, "Points (+30)", 2_000);

		assertTrue(tracker.startAssignment(history, assignment));
		assertFalse(tracker.startAssignment(history, assignment));
		assertEquals(1, history.getTasks().size());
	}

	@Test
	public void completesAssignmentAndCalculatesXp()
	{
		SlayerHistory history = new SlayerHistory();
		tracker.startAssignment(history, new SlayerAssignment("Pyrefiends", 41, "Clues (+110)", 5_000));

		assertTrue(tracker.completeAssignment(history, 6_800));
		assertFalse(tracker.completeAssignment(history, 7_000));
		assertTrue(history.getTasks().get(0).isCompleted());
		assertEquals(1_800, history.getTasks().get(0).getSlayerXpGained());
	}

	@Test
	public void doesNotReopenLingeringCompletedAssignment()
	{
		SlayerHistory history = new SlayerHistory();
		SlayerAssignment assignment = new SlayerAssignment("Wyrms", 106, "Superior unique (+205)", 10_000);
		tracker.startAssignment(history, assignment);
		tracker.completeAssignment(history, 20_800);

		assertFalse(tracker.startAssignment(history, assignment));
		assertEquals(1, history.getTasks().size());

		tracker.clearAssignment();
		assertTrue(tracker.startAssignment(history, assignment));
		assertEquals(2, history.getTasks().size());
	}
}
