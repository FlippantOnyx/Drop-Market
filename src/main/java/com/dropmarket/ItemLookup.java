package com.dropmarket;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

/**
 * The only way the plugin talks to RuneLite's item and price data. It never throws and never hands back null: an
 * unknown item has an empty name, a failed search is an empty list and a failed price is 0.
 */
@Slf4j
@Singleton
class ItemLookup
{
	private final ItemManager itemManager;

	@Inject
	ItemLookup(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	/** The item's name, or an empty string if the game has no such item (or isn't ready). */
	String nameOf(int id)
	{
		try
		{
			ItemComposition composition = itemManager.getItemComposition(id);
			String name = composition == null ? null : composition.getName();
			return name == null ? "" : name;
		}
		catch (Exception e)
		{
			log.debug("Item lookup failed for {}", id, e);
			return "";
		}
	}

	/** The name of an item's canonical form (a noted or placeholder item resolves to the real one). */
	String canonicalNameOf(int id)
	{
		try
		{
			return nameOf(itemManager.canonicalize(id));
		}
		catch (Exception e)
		{
			log.debug("Item canonicalize failed for {}", id, e);
			return nameOf(id);
		}
	}

	/** Grand Exchange search results; empty until prices have loaded. */
	List<ItemPrice> search(String name)
	{
		try
		{
			List<ItemPrice> results = itemManager.search(name);
			return results == null ? Collections.emptyList() : results;
		}
		catch (Exception e)
		{
			log.debug("Item search failed for {}", name, e);
			return Collections.emptyList();
		}
	}

	long priceOf(int id)
	{
		try
		{
			return itemManager.getItemPrice(id);
		}
		catch (Exception e)
		{
			log.debug("Item price failed for {}", id, e);
			return 0;
		}
	}
}
