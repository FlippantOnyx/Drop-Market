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

	private final OkHttpClient http;
	private final Gson gson;
	private final ClientThread clientThread;
	private final Map<String, List<String>> cache = new ConcurrentHashMap<>();
	private final Map<String, Boolean> inFlight = new ConcurrentHashMap<>();

	@Inject
	WikiDropTableClient(OkHttpClient http, Gson gson, ClientThread clientThread)
	{
		this.http = http;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	/** Cached rare drops for the monster, ordered by wiki drop value; null if not fetched yet. */
	List<String> getCached(String monster)
	{
		return cache.get(DropTables.normalize(monster));
	}

	void fetch(String monster, Consumer<List<String>> onClientThread)
	{
		String key = DropTables.normalize(monster);
		if (cache.containsKey(key) || inFlight.putIfAbsent(key, Boolean.TRUE) != null)
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
				finish(key, Collections.emptyList(), onClientThread);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				List<String> drops = Collections.emptyList();
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						drops = parse(body.string());
					}
				}
				catch (Exception e)
				{
					log.debug("Wiki drop parse failed for {}", monster, e);
				}
				finish(key, drops, onClientThread);
			}
		});
	}

	private void finish(String key, List<String> drops, Consumer<List<String>> onClientThread)
	{
		cache.put(key, drops);
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
