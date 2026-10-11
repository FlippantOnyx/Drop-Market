package com.dropmarket;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Singleton
class WikiDropTableClient
{
	private static final HttpUrl API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	// Only the most recently used activities are kept, so a session never grows or re-asks for the same monster.
	private static final int MAX_CACHED = 50;
	// A failed lookup is retried after a while instead of being stuck empty for the whole session.
	private static final long FAILURE_RETRY_MS = 10 * 60 * 1000L;

	private final OkHttpClient http;
	private final Gson gson;
	private final ClientThread clientThread;
	private final Map<String, CachedDrops> cache = new LinkedHashMap<String, CachedDrops>(16, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CachedDrops> eldest)
		{
			return size() > MAX_CACHED;
		}
	};
	private final Map<String, Boolean> inFlight = new ConcurrentHashMap<>();

	@Inject
	WikiDropTableClient(OkHttpClient http, Gson gson, ClientThread clientThread)
	{
		this.http = http;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	private static final class CachedDrops
	{
		final List<String> drops;
		final long expiresAt;

		CachedDrops(List<String> drops, long expiresAt)
		{
			this.drops = drops;
			this.expiresAt = expiresAt;
		}
	}

	/** Cached rare drops for the monster, ordered by wiki drop value; null if not fetched (or expired). */
	List<String> getCached(String monster)
	{
		return lookup(DropTables.normalize(monster));
	}

	private List<String> lookup(String key)
	{
		synchronized (cache)
		{
			CachedDrops entry = cache.get(key);
			if (entry == null)
			{
				return null;
			}
			if (System.currentTimeMillis() >= entry.expiresAt)
			{
				cache.remove(key);
				return null;
			}
			return entry.drops;
		}
	}

	void fetch(String monster, Consumer<List<String>> onClientThread)
	{
		String key = DropTables.normalize(monster);
		if (lookup(key) != null || inFlight.putIfAbsent(key, Boolean.TRUE) != null)
		{
			return;
		}

		String escaped = monster.replace("\\", "\\\\").replace("'", "\\'");
		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "bucket")
			.addQueryParameter("format", "json")
			.addQueryParameter("query", "bucket('dropsline').select('item_name','drop_json')"
				+ ".where('page_name','" + escaped + "').limit(500).run()")
			.build();

		http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Wiki drop lookup failed for {}", monster, e);
				finish(key, Collections.emptyList(), false, onClientThread);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				List<String> drops = Collections.emptyList();
				boolean ok = false;
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						drops = parse(body.string());
						ok = true;
					}
				}
				catch (Exception e)
				{
					log.debug("Wiki drop parse failed for {}", monster, e);
				}
				finish(key, drops, ok, onClientThread);
			}
		});
	}

	private void finish(String key, List<String> drops, boolean ok, Consumer<List<String>> onClientThread)
	{
		long expiresAt = ok ? Long.MAX_VALUE : System.currentTimeMillis() + FAILURE_RETRY_MS;
		synchronized (cache)
		{
			cache.put(key, new CachedDrops(drops, expiresAt));
		}
		inFlight.remove(key);
		clientThread.invoke(() -> onClientThread.accept(drops));
	}

	List<String> parse(String json)
	{
		JsonArray rows = gson.fromJson(json, JsonObject.class).getAsJsonArray("bucket");
		if (rows == null)
		{
			return Collections.emptyList();
		}

		Map<String, Long> valueByItem = new LinkedHashMap<>();
		for (JsonElement el : rows)
		{
			JsonObject row = el.getAsJsonObject();
			JsonElement name = row.get("item_name");
			JsonElement dropJson = row.get("drop_json");
			if (name == null || dropJson == null)
			{
				continue;
			}

			JsonObject drop = gson.fromJson(dropJson.getAsString(), JsonObject.class);
			JsonElement rarity = drop.get("Rarity");
			if (rarity != null && "Always".equalsIgnoreCase(rarity.getAsString()))
			{
				continue;
			}

			long value = 0;
			JsonElement dropValue = drop.get("Drop Value");
			if (dropValue != null && dropValue.isJsonPrimitive() && dropValue.getAsJsonPrimitive().isNumber())
			{
				value = dropValue.getAsLong();
			}
			valueByItem.merge(name.getAsString(), value, Math::max);
		}

		List<String> items = new ArrayList<>(valueByItem.keySet());
		items.sort((a, b) -> Long.compare(valueByItem.get(b), valueByItem.get(a)));
		return items;
	}
}
