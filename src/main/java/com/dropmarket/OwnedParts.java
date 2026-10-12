package com.dropmarket;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Which parts of a multi-part item (Noxious halberd, Abyssal bludgeon) the player currently holds. The game won't drop
 * a part you already have until you have the whole set, so owned parts can't drop and the missing ones share the
 * finished item's value. Completing a set starts it over. Pure logic; MultipartTracker saves it per account.
 */
final class OwnedParts
{
	private final Set<String> owned = new HashSet<>();

	boolean owns(String part)
	{
		return owned.contains(DropTables.normalize(part));
	}

	/** Records a part; returns true if that completed the set (which is then cleared so the next set starts fresh). */
	boolean add(String part, Collection<String> setParts)
	{
		owned.add(DropTables.normalize(part));
		for (String p : setParts)
		{
			if (!owns(p))
			{
				return false;
			}
		}
		for (String p : setParts)
		{
			owned.remove(DropTables.normalize(p));
		}
		return true;
	}

	/** How many parts of the set are still missing (at least 1, counting a part that is dropping right now). */
	int missing(Collection<String> setParts)
	{
		int missing = 0;
		for (String p : setParts)
		{
			if (!owns(p))
			{
				missing++;
			}
		}
		return Math.max(1, missing);
	}

	Set<String> snapshot()
	{
		return new HashSet<>(owned);
	}

	void replaceAll(Collection<String> normalizedParts)
	{
		owned.clear();
		owned.addAll(normalizedParts);
	}
}
