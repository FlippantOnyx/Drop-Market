package com.dropmarket;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Untradeable items that DROP and are used to make expensive tradeable ones (Araxyte fang, Mokhaiotl cloth...), from the
 * bundled snapshot made by tools/generate-wiki-drops.mjs. Parts you only craft (icons, the infernal chunk) aren't here, but the untradeable drops that feed them (the vestiges) are. They have no GE price, but they are still worth celebrating.
 */
@Slf4j
@Singleton
class ValuableComponents
{
	private static final String RESOURCE = "valuable-components.json";

	private final Gson gson;
	private Set<String> items;

	@Inject
	ValuableComponents(Gson gson)
	{
		this.gson = gson;
	}

	synchronized boolean contains(String item)
	{
		if (item == null)
		{
			return false;
		}
		if (items == null)
		{
			items = load();
		}
		return items.contains(DropTables.normalize(item));
	}

	private Set<String> load()
	{
		Set<String> out = new HashSet<>();
		try (InputStream in = ValuableComponents.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("Bundled valuable components missing");
				return Collections.emptySet();
			}
			String[] names = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), String[].class);
			for (String name : names == null ? new String[0] : names)
			{
				out.add(DropTables.normalize(name));
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Bundled valuable components unreadable", e);
		}
		return out;
	}
}
