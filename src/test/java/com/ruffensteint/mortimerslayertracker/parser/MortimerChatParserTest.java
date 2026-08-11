package com.ruffensteint.mortimerslayertracker.parser;

import java.util.OptionalInt;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MortimerChatParserTest
{
	private final MortimerChatParser parser = new MortimerChatParser();

	@Test
	public void parsesCompletedTaskCount()
	{
		OptionalInt result = parser.parseCompletedTaskCount(
			"You've completed 39 Mortimer tasks and earned some points.");

		assertTrue(result.isPresent());
		assertEquals(39, result.getAsInt());
	}

	@Test
	public void parsesCurlyApostropheAndThousandsSeparator()
	{
		OptionalInt result = parser.parseCompletedTaskCount("You’ve completed 1,234 Mortimer tasks.");

		assertTrue(result.isPresent());
		assertEquals(1234, result.getAsInt());
	}

	@Test
	public void ignoresUnrelatedMessages()
	{
		assertFalse(parser.parseCompletedTaskCount("Your Slayer task is gryphons.").isPresent());
		assertFalse(parser.parseCompletedTaskCount(null).isPresent());
	}
}
