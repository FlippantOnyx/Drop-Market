package com.dropmarket;

/**
 * Decides whether a drop counts as a success on the odds. Pure logic, kept separate from the plugin so every
 * rule (blacklist, priority, threshold, pop-off, new and unpriced collection log items) can be tested without
 * a client.
 */
final class DropQualifier
{
	/** Untradeable: it can never have a GE price. */
	static final long NO_PRICE = -1;
	/** The price data hasn't loaded yet, so the item's value can't be judged. */
	static final long PRICES_NOT_LOADED = -2;
	/** A tradeable item the GE has no price for yet (brand new content), as opposed to NO_PRICE (untradeable). */
	static final long NOT_PRICED_YET = -3;

	/** Where an item sits on the user's lists. A blacklisted item stays blacklisted even if it's also a priority. */
	enum ListStatus
	{
		NONE,
		PRIORITY,
		BLACKLISTED
	}

	private DropQualifier()
	{
	}

	/**
	 * A received item. The lists trump everything: a blacklisted item never counts (not even under Resolve Win on Every Drop), and a
	 * priority item always counts, whatever it's worth. Otherwise Resolve Win on Every Drop counts everything; a collection log
	 * item new to the account always counts (a log drop that isn't new is price-checked like any other drop); before
	 * prices load only a collection log slot counts; an unpriced item counts when it's a known chase (on the table, the market's own target, or a collection log
	 * slot) since it can't be judged on value; and a priced item must be on the table or be the target and clear the
	 * win minimum value.
	 */
	static boolean lootQualifies(long price, long threshold, boolean onTable, boolean isTarget, boolean clogSlot,
		boolean newLogItem, boolean keepsValue, ListStatus list, boolean popOff)
	{
		if (list == ListStatus.BLACKLISTED)
		{
			return false;
		}
		if (list == ListStatus.PRIORITY)
		{
			return true;
		}
		if (popOff)
		{
			return true;
		}
		if (newLogItem)
		{
			// It opens a new collection log slot, so it's a win whatever it's worth (or whether it's priced yet).
			return true;
		}
		if (price == PRICES_NOT_LOADED)
		{
			// Value can't be judged yet, but a collection log slot is a chase regardless of price.
			return clogSlot;
		}
		if (price == NOT_PRICED_YET)
		{
			// Brand new tradeable content the GE hasn't priced: it can't be judged on value, so a real chase counts.
			return onTable || isTarget || clogSlot;
		}
		if (price == NO_PRICE)
		{
			// Untradeable: repeats only count when it is worth something anyway (a pet, or a part of an expensive item).
			return keepsValue;
		}
		return (onTable || isTarget) && price >= threshold;
	}

	/**
	 * Whether a log drop is new to the account. Only trust our own record when we know the whole log (RuneProfile):
	 * otherwise "not seen yet" just means the plugin hasn't watched you get it, and an item you already own would
	 * flash as new. The game's own "New item added to your collection log" message covers genuinely new items.
	 */
	static boolean isNewLogItem(boolean logDrop, boolean fullLogKnown, boolean obtained)
	{
		return logDrop && fullLogKnown && !obtained;
	}

	/** The game's "Valuable drop" chat message; {@code value} is negative when it couldn't be determined. */
	static boolean valuableMessageQualifies(long value, long threshold, boolean newLogItem, ListStatus list,
		boolean popOff)
	{
		if (list != ListStatus.NONE)
		{
			return list == ListStatus.PRIORITY;
		}
		if (popOff)
		{
			return true;
		}
		// The game flagged it as valuable but we can't price it (a brand new item): trust the game.
		return newLogItem || value < 0 || value >= threshold;
	}

	/**
	 * The game's "Untradeable drop" chat message. It only counts if the item is new to your collection log, is worth
	 * something (a pet, or a part of an expensive item), or comes from a source with no known table at all (brand new
	 * content). Repeats of ordinary untradeable uniques, like a Xeric's talisman, don't.
	 */
	static boolean untradeableMessageQualifies(boolean newLogItem, boolean keepsValue, boolean sourceHasNoTable,
		ListStatus list, boolean popOff)
	{
		if (list != ListStatus.NONE)
		{
			return list == ListStatus.PRIORITY;
		}
		return popOff || newLogItem || keepsValue || sourceHasNoTable;
	}

	/** The game's collection log message: always a win unless the item is blacklisted. */
	static boolean collectionLogMessageQualifies(ListStatus list)
	{
		return list != ListStatus.BLACKLISTED;
	}

	/** Whether the market's own chat-message resolution (pet, named-item messages) may fire for this item. */
	static boolean marketMessageQualifies(ListStatus list)
	{
		return list != ListStatus.BLACKLISTED;
	}
}
