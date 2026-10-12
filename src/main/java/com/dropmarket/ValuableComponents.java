package com.dropmarket;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Untradeable drops that end up, through any number of crafting steps, in an expensive tradeable item (a vestige
 * becomes an icon becomes a ring), from the bundled snapshot made by tools/generate-wiki-drops.mjs. They have no GE
 * price of their own, so each one is worth the price of the item it ends up in. Parts of items that need several
 * parts (the Soulreaper axe) are listed with a value of 0: still worth celebrating, but not worth the whole item.
 */
@Slf4j
@Singleton
class ValuableComponents
{
	private static final String RESOURCE = "valuable-components.json";

	private static final class Entry
	{
		String name;
		long value;
		List<String> sources;
		List<String> parts;
	}

	private final Gson gson;
	private Map<String, Entry> byName;
	private Map<String, List<String>> namesBySource;

	@Inject
	ValuableComponents(Gson gson)
	{
		this.gson = gson;
	}

	synchronized boolean contains(String item)
	{
		load();
		return item != null && byName.containsKey(DropTables.normalize(item));
	}

	/** What the item is worth through what it becomes, or 0 if it has no assigned value (or isn't on the list). */
	synchronized long valueOf(String item)
	{
		load();
		Entry entry = item == null ? null : byName.get(DropTables.normalize(item));
		return entry == null ? 0 : entry.value;
	}

	/** The other untradeable drops needed for the same finished item (a Noxious blade's siblings), or empty. */
	synchronized List<String> partsOf(String item)
	{
		load();
		Entry entry = item == null ? null : byName.get(DropTables.normalize(item));
		return entry == null || entry.parts == null ? Collections.emptyList() : entry.parts;
	}

	/** The listed items that drop from this source, e.g. the Ultor vestige for Vardorvis. */
	synchronized List<String> dropsFrom(String source)
	{
		load();
		List<String> names = source == null ? null : namesBySource.get(DropTables.normalize(source));
		return names == null ? Collections.emptyList() : names;
	}

	private void load()
	{
		if (byName != null)
		{
			return;
		}
		byName = new HashMap<>();
		namesBySource = new HashMap<>();
		try (InputStream in = ValuableComponents.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Bundled valuable components missing");
				return;
			}
			Entry[] entries = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Entry[].class);
			for (Entry entry : entries == null ? new Entry[0] : entries)
			{
				byName.put(DropTables.normalize(entry.name), entry);
				for (String source : entry.sources == null ? Collections.<String>emptyList() : entry.sources)
				{
					namesBySource.computeIfAbsent(source, k -> new ArrayList<>()).add(entry.name);
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Bundled valuable components unreadable", e);
		}
	}
}
