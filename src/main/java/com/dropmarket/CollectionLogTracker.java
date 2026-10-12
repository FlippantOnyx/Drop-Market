package com.dropmarket;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Which collection log slots exist and which this account has unlocked, stored as normalized item names. */
@Slf4j
@Singleton
class CollectionLogTracker
{
	private static final String KEY_KNOWN = "clogKnown";
	private static final String KEY_OBTAINED = "clogObtained";
	private static final String KEY_FULL = "clogFull";
	private static final HttpUrl RUNEPROFILE = HttpUrl.get("https://api.runeprofile.com/v1/accounts");

	private final ConfigManager configManager;
	private final OkHttpClient http;
	private final Gson gson;
	private final ClientThread clientThread;

	private final Set<String> known = new HashSet<>();
	private final Set<String> obtained = new HashSet<>();
	private boolean fullLog;

	@Inject
	CollectionLogTracker(ConfigManager configManager, OkHttpClient http, Gson gson, ClientThread clientThread)
	{
		this.configManager = configManager;
		this.http = http;
		this.gson = gson;
		this.clientThread = clientThread;
	}

	void load()
	{
		known.clear();
		obtained.clear();
		fullLog = false;
		read(KEY_KNOWN, known);
		read(KEY_OBTAINED, obtained);
		fullLog = "true".equals(configManager.getRSProfileConfiguration(DropMarketConfig.GROUP, KEY_FULL));
	}

	private void read(String key, Set<String> into)
	{
		String value = configManager.getRSProfileConfiguration(DropMarketConfig.GROUP, key);
		if (value != null && !value.isEmpty())
		{
			into.addAll(Arrays.asList(value.split(",")));
		}
	}

	private void save()
	{
		configManager.setRSProfileConfiguration(DropMarketConfig.GROUP, KEY_KNOWN, String.join(",", known));
		configManager.setRSProfileConfiguration(DropMarketConfig.GROUP, KEY_OBTAINED, String.join(",", obtained));
	}

	/** True once the whole log is known (from RuneProfile), so slot membership is authoritative. */
	boolean hasFullLog()
	{
		return fullLog;
	}

	boolean isSlot(String item)
	{
		return known.contains(DropTables.normalize(item));
	}

	boolean isObtained(String item)
	{
		return obtained.contains(DropTables.normalize(item));
	}

	/** Returns true if anything changed. Call {@link #flush()} after a batch. */
	boolean record(String item, boolean got)
	{
		String n = DropTables.normalize(item);
		if (n.isEmpty())
		{
			return false;
		}
		boolean changed = known.add(n);
		if (got)
		{
			changed |= obtained.add(n);
		}
		return changed;
	}

	void recordAndFlush(String item, boolean got)
	{
		if (record(item, got))
		{
			save();
		}
	}

	void flush()
	{
		save();
	}

	void fetchRuneProfile(String username, Runnable onClientThread)
	{
		HttpUrl url = RUNEPROFILE.newBuilder()
			.addPathSegment(username.replace(' ', ' '))
			.addPathSegment("collection-log")
			.build();

		http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("RuneProfile lookup failed", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.debug("RuneProfile returned {}", response.code());
						return;
					}
					JsonObject clog = gson.fromJson(body.string(), JsonObject.class);
					clientThread.invoke(() ->
					{
						try
						{
							applyRuneProfile(clog);
							onClientThread.run();
						}
						catch (Exception | LinkageError e)
						{
							log.debug("RuneProfile result failed", e);
						}
					});
				}
				catch (Exception e)
				{
					log.debug("RuneProfile parse failed", e);
				}
			}
		});
	}

	private void applyRuneProfile(JsonObject clog)
	{
		Map<String, Boolean> slots = parseRuneProfile(clog);
		if (slots.isEmpty())
		{
			return;
		}
		slots.forEach(this::record);
		fullLog = true;
		configManager.setRSProfileConfiguration(DropMarketConfig.GROUP, KEY_FULL, "true");
		save();
	}

	/** Item name -> obtained, from RuneProfile's tabs[].pages[].items[] (quantity 0 = locked). */
	static Map<String, Boolean> parseRuneProfile(JsonObject clog)
	{
		Map<String, Boolean> slots = new HashMap<>();
		JsonArray tabs = clog.getAsJsonArray("tabs");
		if (tabs == null)
		{
			return slots;
		}
		for (JsonElement tab : tabs)
		{
			for (JsonElement page : tab.getAsJsonObject().getAsJsonArray("pages"))
			{
				for (JsonElement el : page.getAsJsonObject().getAsJsonArray("items"))
				{
					JsonObject item = el.getAsJsonObject();
					JsonElement qty = item.get("quantity");
					slots.merge(item.get("name").getAsString(), qty != null && qty.getAsInt() > 0, Boolean::logicalOr);
				}
			}
		}
		return slots;
	}
}
