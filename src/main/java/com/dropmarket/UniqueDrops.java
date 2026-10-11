package com.dropmarket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Whether an item is a unique drop specific to a monster/activity: it's on that source's table and appears on at most
 * one other known table (e.g. both Nightmare variants share their uniques, but a rune 2h sword is on dozens).
 */
final class UniqueDrops
{
	private static final int MAX_OTHER_SOURCES = 1;

	private static Map<String, Set<String>> sourcesByItem;

	private UniqueDrops()
	{
	}

	static synchronized boolean isUnique(String source, String item)
	{
		if (source == null || item == null)
		{
			return false;
		}
		if (sourcesByItem == null)
		{
			sourcesByItem = index();
		}
		Set<String> sources = sourcesByItem.get(DropTables.normalize(item));
		String key = DropTables.normalize(source);
		return sources != null && sources.contains(key) && sources.size() - 1 <= MAX_OTHER_SOURCES;
	}

	private static Map<String, Set<String>> index()
	{
		Map<String, Set<String>> out = new HashMap<>();
		add(out, DropTables.allTables());
		add(out, BundledDropTables.all());
		return out;
	}

	private static void add(Map<String, Set<String>> out, Map<String, List<String>> tables)
	{
		tables.forEach((source, items) ->
		{
			for (String item : items)
			{
				out.computeIfAbsent(DropTables.normalize(item), k -> new HashSet<>()).add(source);
			}
		});
	}
}
