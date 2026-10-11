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
 * Whether an item is a "unique" drop of a monster/activity: a collection log item that isn't super common there
 * (so Hydra bones or Demon tears don't count, a Tanzanite fang does). Backed by the bundled wiki snapshot, plus
 * the hand-written tables, whose items are uniques by construction.
 */
@Slf4j
final class UniqueDrops
{
	private static final String RESOURCE = "collection-log.json";

	private static final class Data
	{
		List<String> items;
		Map<String, List<String>> uniques;
	}

	private static Set<String> logItems;
	private static Map<String, Set<String>> uniquesBySource;

	private UniqueDrops()
	{
	}

	static synchronized boolean isUnique(String source, String item)
	{
		if (source == null || item == null)
		{
			return false;
		}
		load();
		String n = DropTables.normalize(item);
		Set<String> rare = uniquesBySource.get(DropTables.normalize(source));
		if (rare != null && rare.contains(n))
		{
			return true;
		}
		// Hand-written tables list only the notable uniques; they count when they're log items (or pets).
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
		uniquesBySource = new HashMap<>();
		try (InputStream in = UniqueDrops.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Bundled collection log data missing");
				return;
			}
			Data data = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Data.class);
			if (data == null)
			{
				return;
			}
			for (String item : data.items == null ? Collections.<String>emptyList() : data.items)
			{
				logItems.add(DropTables.normalize(item));
			}
			if (data.uniques != null)
			{
				data.uniques.forEach((source, items) ->
				{
					Set<String> set = new HashSet<>();
					items.forEach(item -> set.add(DropTables.normalize(item)));
					uniquesBySource.put(source, set);
				});
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Bundled collection log data unreadable", e);
		}
	}
}
