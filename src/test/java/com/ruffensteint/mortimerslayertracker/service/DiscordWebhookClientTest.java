package com.ruffensteint.mortimerslayertracker.service;

import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DiscordWebhookClientTest
{
	@Test
	public void acceptsDiscordWebhookUrls()
	{
		assertNotNull(DiscordWebhookClient.parseDiscordWebhookUrl(
			"https://discord.com/api/webhooks/123/token"));
		assertNotNull(DiscordWebhookClient.parseDiscordWebhookUrl(
			"https://discordapp.com/api/webhooks/123/token"));
	}

	@Test
	public void rejectsNonDiscordAndInsecureUrls()
	{
		assertNull(DiscordWebhookClient.parseDiscordWebhookUrl(""));
		assertNull(DiscordWebhookClient.parseDiscordWebhookUrl("http://discord.com/api/webhooks/123/token"));
		assertNull(DiscordWebhookClient.parseDiscordWebhookUrl("https://example.com/api/webhooks/123/token"));
		assertNull(DiscordWebhookClient.parseDiscordWebhookUrl("https://discord.com/channels/123"));
	}

	@Test
	public void buildsDirectAndSearchWikiQueries()
	{
		assertEquals("Dark Beasts",
			DiscordWebhookClient.wikiLookupUrl("Dark Beasts", false).queryParameter("titles"));
		assertEquals("1",
			DiscordWebhookClient.wikiLookupUrl("Dark Beasts", false).queryParameter("redirects"));
		assertEquals("Dark Beasts",
			DiscordWebhookClient.wikiLookupUrl("Dark Beasts", true).queryParameter("gsrsearch"));
		assertEquals("1",
			DiscordWebhookClient.wikiLookupUrl("Dark Beasts", true).queryParameter("gsrlimit"));
	}

	@Test
	public void extractsWikiThumbnailSource()
	{
		String response = "{\"query\":{\"pages\":[{\"thumbnail\":{\"source\":\"https://example/image.png\"}}]}}";
		assertEquals("https://example/image.png", DiscordWebhookClient.extractThumbnailUrl(
			new JsonParser().parse(response).getAsJsonObject()));
		assertNull(DiscordWebhookClient.extractThumbnailUrl(
			new JsonParser().parse("{\"query\":{\"pages\":[{\"missing\":true}]}}").getAsJsonObject()));
	}
}
