package com.ruffensteint.mortimerslayertracker.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ruffensteint.mortimerslayertracker.model.LootItemRecord;
import com.ruffensteint.mortimerslayertracker.model.SlayerTaskRecord;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
public class DiscordWebhookClient
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final int NEW_TASK_COLOR = 0x5865F2;
	private static final int COMPLETED_TASK_COLOR = 0x57F287;
	private static final int MAX_THUMBNAIL_BYTES = 2 * 1024 * 1024;
	private static final String WIKI_USER_AGENT = "MortimerSlayerTracker/0.1 (RuneLite plugin)";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final Map<String, String> thumbnailCache = new ConcurrentHashMap<>();
	private final Map<String, byte[]> thumbnailBytesCache = new ConcurrentHashMap<>();

	@Inject
	public DiscordWebhookClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	public void sendTaskStarted(String webhookUrl, String title, String playerName, SlayerTaskRecord task)
	{
		List<Field> fields = new ArrayList<>();
		fields.add(new Field("Slayer", safe(playerName), false));
		fields.add(new Field("Task", "#" + task.getTaskNumber(), true));
		fields.add(new Field("Monster", safe(task.getMonster()), true));
		fields.add(new Field("Amount", Integer.toString(task.getAssignedAmount()), true));
		fields.add(new Field("Modifier", safe(task.getModifier()), false));
		fields.add(new Field("Starting Slayer XP", Integer.toString(task.getStartSlayerXp()), false));
		send(webhookUrl,
			new Embed(safeTitle(title, "New Mortimer Task"), NEW_TASK_COLOR, fields),
			task.getMonster());
	}

	public void sendTaskCompleted(String webhookUrl, String title, String playerName, SlayerTaskRecord task)
	{
		List<Field> fields = new ArrayList<>();
		fields.add(new Field("Slayer", safe(playerName), false));
		fields.add(new Field("Task", "#" + task.getTaskNumber() + " - " + safe(task.getMonster()), false));
		fields.add(new Field("Slayer XP", task.getBaseSlayerXp() + " base (+" + task.getBonusSlayerXp() + " bonus)", false));
		fields.add(new Field("Superiors", Integer.toString(task.getSuperiorCount()), true));
		fields.add(new Field("Clues", Integer.toString(task.getClueDropCount()), true));
		if (!task.getSuperiorLoot().isEmpty())
		{
			fields.add(new Field("Superior Loot", formatLoot(task.getSuperiorLoot()), false));
		}
		send(webhookUrl,
			new Embed(safeTitle(title, "Mortimer Task Complete"), COMPLETED_TASK_COLOR, fields),
			task.getMonster());
	}

	private void send(String webhookUrl, Embed embed, String monster)
	{
		List<HttpUrl> urls = parseDiscordWebhookUrls(webhookUrl);
		if (urls.isEmpty())
		{
			log.debug("Discord webhook URLs are missing or invalid");
			return;
		}

		resolveThumbnail(monster, thumbnailUrl ->
		{
			if (thumbnailUrl == null)
			{
				for (HttpUrl url : urls)
				{
					sendRequest(url, createJsonBody(embed));
				}
				return;
			}
			embed.thumbnail = new Thumbnail(thumbnailUrl);
			fetchThumbnailAndSend(urls, embed);
		});
	}

	private void resolveThumbnail(String monster, Consumer<String> callback)
	{
		if (monster == null || monster.trim().isEmpty())
		{
			callback.accept(null);
			return;
		}
		String cacheKey = monster.trim().toLowerCase(java.util.Locale.ENGLISH);
		String cached = thumbnailCache.get(cacheKey);
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		queryWikiThumbnail(monster.trim(), false, thumbnailUrl ->
		{
			if (thumbnailUrl != null)
			{
				thumbnailCache.put(cacheKey, thumbnailUrl);
				callback.accept(thumbnailUrl);
				return;
			}
			queryWikiThumbnail(monster.trim(), true, searchedUrl ->
			{
				if (searchedUrl != null)
				{
					thumbnailCache.put(cacheKey, searchedUrl);
				}
				callback.accept(searchedUrl);
			});
		});
	}

	private void queryWikiThumbnail(String monster, boolean search, Consumer<String> callback)
	{
		Request request = new Request.Builder()
			.url(wikiLookupUrl(monster, search))
			.header("User-Agent", WIKI_USER_AGENT)
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				log.debug("Unable to resolve monster thumbnail", ex);
				callback.accept(null);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response ignored = response)
				{
					if (!response.isSuccessful() || response.body() == null)
					{
						log.debug("Monster thumbnail lookup returned HTTP {}", response.code());
						callback.accept(null);
						return;
					}
					JsonObject root = gson.fromJson(response.body().charStream(), JsonObject.class);
					callback.accept(extractThumbnailUrl(root));
				}
			}
		});
	}

	private void fetchThumbnailAndSend(List<HttpUrl> webhookUrls, Embed embed)
	{
		loadThumbnailBytes(embed.thumbnail.url, image ->
		{
			if (image == null)
			{
				embed.thumbnail = null;
				for (HttpUrl webhookUrl : webhookUrls)
				{
					sendRequest(webhookUrl, createJsonBody(embed));
				}
				return;
			}

			embed.thumbnail = new Thumbnail("attachment://monster.png");
			for (HttpUrl webhookUrl : webhookUrls)
			{
				MultipartBody body = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("payload_json", gson.toJson(createPayload(embed)))
					.addFormDataPart("files[0]", "monster.png",
						RequestBody.create(MediaType.get("image/png"), image))
					.build();
				sendRequest(webhookUrl, body);
			}
		});
	}

	public void loadMonsterThumbnail(String monster, Consumer<BufferedImage> callback)
	{
		resolveThumbnail(monster, thumbnailUrl ->
		{
			if (thumbnailUrl == null)
			{
				callback.accept(null);
				return;
			}
			loadThumbnailBytes(thumbnailUrl, image ->
			{
				if (image == null)
				{
					callback.accept(null);
					return;
				}
				try
				{
					callback.accept(ImageIO.read(new ByteArrayInputStream(image)));
				}
				catch (IOException ex)
				{
					log.debug("Unable to decode monster thumbnail", ex);
					callback.accept(null);
				}
			});
		});
	}

	private void loadThumbnailBytes(String thumbnailUrl, Consumer<byte[]> callback)
	{
		byte[] cached = thumbnailBytesCache.get(thumbnailUrl);
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		Request imageRequest = new Request.Builder()
			.url(thumbnailUrl)
			.header("User-Agent", WIKI_USER_AGENT)
			.build();
		httpClient.newCall(imageRequest).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				log.debug("Unable to fetch monster thumbnail", ex);
				callback.accept(null);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (Response ignored = response)
				{
					long contentLength = response.body() == null ? 0 : response.body().contentLength();
					if (!response.isSuccessful() || response.body() == null
						|| contentLength > MAX_THUMBNAIL_BYTES)
					{
						log.debug("Monster thumbnail request failed with HTTP {}", response.code());
						callback.accept(null);
						return;
					}
					byte[] image = response.body().bytes();
					if (image.length > MAX_THUMBNAIL_BYTES)
					{
						callback.accept(null);
						return;
					}
					thumbnailBytesCache.put(thumbnailUrl, image);
					callback.accept(image);
				}
			}
		});
	}

	private RequestBody createJsonBody(Embed embed)
	{
		return RequestBody.create(JSON, gson.toJson(createPayload(embed)));
	}

	private Map<String, Object> createPayload(Embed embed)
	{
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("embeds", Collections.singletonList(embed));
		payload.put("allowed_mentions", Collections.singletonMap("parse", Collections.emptyList()));
		return payload;
	}

	private void sendRequest(HttpUrl url, RequestBody body)
	{
		Request request = new Request.Builder().url(url).post(body).build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException ex)
			{
				log.debug("Unable to send Discord webhook", ex);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response ignored = response)
				{
					if (!response.isSuccessful())
					{
						log.debug("Discord webhook returned HTTP {}", response.code());
					}
				}
			}
		});
	}

	static HttpUrl parseDiscordWebhookUrl(String webhookUrl)
	{
		if (webhookUrl == null || webhookUrl.trim().isEmpty())
		{
			return null;
		}
		HttpUrl url = HttpUrl.parse(webhookUrl.trim());
		if (url == null || !"https".equals(url.scheme()))
		{
			return null;
		}
		String host = url.host();
		if (!("discord.com".equals(host) || "discordapp.com".equals(host))
			|| !url.encodedPath().startsWith("/api/webhooks/"))
		{
			return null;
		}
		return url;
	}

	static List<HttpUrl> parseDiscordWebhookUrls(String webhookUrls)
	{
		if (webhookUrls == null || webhookUrls.trim().isEmpty())
		{
			return Collections.emptyList();
		}
		Set<HttpUrl> urls = new LinkedHashSet<>();
		for (String candidate : webhookUrls.split("[,\\r\\n]+"))
		{
			HttpUrl url = parseDiscordWebhookUrl(candidate);
			if (url != null)
			{
				urls.add(url);
			}
		}
		return new ArrayList<>(urls);
	}

	static HttpUrl wikiLookupUrl(String monster, boolean search)
	{
		HttpUrl.Builder builder = new HttpUrl.Builder()
			.scheme("https")
			.host("oldschool.runescape.wiki")
			.addPathSegment("api.php")
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", "256")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2");
		if (search)
		{
			builder.addQueryParameter("generator", "search")
				.addQueryParameter("gsrsearch", monster)
				.addQueryParameter("gsrnamespace", "0")
				.addQueryParameter("gsrlimit", "1");
		}
		else
		{
			builder.addQueryParameter("titles", monster)
				.addQueryParameter("redirects", "1");
		}
		return builder.build();
	}

	static String extractThumbnailUrl(JsonObject root)
	{
		if (root == null || !root.has("query"))
		{
			return null;
		}
		JsonElement pages = root.getAsJsonObject("query").get("pages");
		if (pages == null || !pages.isJsonArray())
		{
			return null;
		}
		for (JsonElement page : pages.getAsJsonArray())
		{
			JsonObject pageObject = page.getAsJsonObject();
			if (pageObject.has("thumbnail"))
			{
				JsonObject thumbnail = pageObject.getAsJsonObject("thumbnail");
				if (thumbnail.has("source"))
				{
					return thumbnail.get("source").getAsString();
				}
			}
		}
		return null;
	}

	private static String formatLoot(List<LootItemRecord> loot)
	{
		StringBuilder result = new StringBuilder();
		for (LootItemRecord item : loot)
		{
			if (result.length() > 0)
			{
				result.append(", ");
			}
			result.append(item.getName()).append(" x").append(item.getQuantity());
		}
		return result.toString();
	}

	private static String safe(String value)
	{
		return value == null || value.trim().isEmpty() ? "None" : value;
	}

	private static String safeTitle(String title, String defaultTitle)
	{
		return title == null || title.trim().isEmpty() ? defaultTitle : title.trim();
	}

	private static class Embed
	{
		private final String title;
		private final int color;
		private final List<Field> fields;
		private Thumbnail thumbnail;

		private Embed(String title, int color, List<Field> fields)
		{
			this.title = title;
			this.color = color;
			this.fields = fields;
		}
	}

	private static class Thumbnail
	{
		private final String url;

		private Thumbnail(String url)
		{
			this.url = url;
		}
	}

	private static class Field
	{
		private final String name;
		private final String value;
		private final boolean inline;

		private Field(String name, String value, boolean inline)
		{
			this.name = name;
			this.value = value;
			this.inline = inline;
		}
	}
}
