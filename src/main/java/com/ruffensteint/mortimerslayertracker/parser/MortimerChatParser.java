package com.ruffensteint.mortimerslayertracker.parser;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MortimerChatParser
{
	private static final Pattern COMPLETION_PATTERN = Pattern.compile(
		"\\bYou['\\u2019]ve\\s+completed\\s+([\\d,]+)\\s+Mortimer\\s+tasks?\\b",
		Pattern.CASE_INSENSITIVE);

	public OptionalInt parseCompletedTaskCount(String message)
	{
		if (message == null)
		{
			return OptionalInt.empty();
		}

		Matcher matcher = COMPLETION_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return OptionalInt.empty();
		}

		try
		{
			return OptionalInt.of(Integer.parseInt(matcher.group(1).replace(",", "")));
		}
		catch (NumberFormatException ex)
		{
			return OptionalInt.empty();
		}
	}
}
