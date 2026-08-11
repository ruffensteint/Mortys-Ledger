package com.ruffensteint.mortimerslayertracker.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SlayerTaskRecordTest
{
	@Test
	public void calculatesNonNegativeXpGain()
	{
		SlayerTaskRecord task = new SlayerTaskRecord();
		task.setStartSlayerXp(1_000);
		task.setEndSlayerXp(1_578);
		assertEquals(578, task.getSlayerXpGained());

		task.setEndSlayerXp(900);
		assertEquals(0, task.getSlayerXpGained());
	}
}
