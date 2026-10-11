package com.dropmarket;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Whether an item is a collection log drop of a monster/activity (so Hydra bones don't count, a Tanzanite fang does).
 * Backed by the bundled wiki snapshot, plus the hand-written tables, whose items are log chases by construction. How
 * common the item is isn't judged here: the plugin price-checks log drops unless they're new to the account.
 */
@Slf4j
final class UniqueDrops
{
	private static final String RESOURCE = "collection-log.json";

	private static final class Data
	{
		List<String> items;
		Map<String, List<String>> sources;
	}

	private static Set<String> logItems;
	private static Map<String, Set<String>> logDropsBySource;
	private static Gson gson;

	private UniqueDrops()
	{
	}

	/** Uses the client's injected Gson (the plugin hub forbids creating fresh instances). */
	static synchronized void configure(Gson injected)
	{
		gson = injected;
		logItems = null;
	}

	static synchronized boolean isLogDrop(String source, String item)
	{
		if (source == null || item == null)
		{
			return false;
		}
		load();
		String n = DropTables.normalize(item);
		Set<String> fromWiki = logDropsBySource.get(DropTables.normalize(source));
		if (fromWiki != null && fromWiki.contains(n))
		{
			return true;
		}
		// Hand-written tables list only the notable chases; they count when they're log items (or pets).
		List<String> curated = DropTables.lookup(source);
		return curated != null && (logItems.contains(n) || DropTables.isPet(item)) && contains(curated, n);
	}

	private static boolean contains(List<String> table, String normalizedItem)
	{
		for (String entry : table)
		{
			if (DropTables.normalize(entry).equals(normalizedItem))
			{
				return true;
			}
		}
		return false;
	}

	private static void load()
	{
		if (logItems != null)
		{
			return;
		}
		logItems = new HashSet<>();
		logDropsBySource = new HashMap<>();
		if (gson == null)
		{
			log.warn("Bundled collection log data not configured");
			return;
		}
		try (InputStream in = UniqueDrops.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Bundled collection log data missing");
				return;
			}
			Data data = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Data.class);
			if (data == null)
			{
				return;
			}
			for (String item : data.items == null ? Collections.<String>emptyList() : data.items)
			{
				logItems.add(DropTables.normalize(item));
			}
			if (data.sources != null)
			{
				data.sources.forEach((source, items) ->
				{
					Set<String> set = new HashSet<>();
					items.forEach(item -> set.add(DropTables.normalize(item)));
					logDropsBySource.put(source, set);
				});
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Bundled collection log data unreadable", e);
		}
	}
}
