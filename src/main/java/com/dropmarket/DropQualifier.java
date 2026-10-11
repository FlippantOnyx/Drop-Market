package com.dropmarket;

/**
 * Decides whether a drop counts as a success on the odds. Pure logic, kept separate from the plugin so every
 * rule (threshold, blacklist, pop-off, unpriced collection log items) can be tested without a client.
 */
final class DropQualifier
{
	/** No market value: untradeable, or new enough that the GE has no price for it yet. */
	static final long NO_PRICE = -1;
	/** The price data hasn't loaded yet, so the item's value can't be judged. */
	static final long PRICES_NOT_LOADED = -2;

	private DropQualifier()
	{
	}

	/**
	 * A received item. Pop Off counts everything (down to its minimum value); otherwise blacklisted items never
	 * count, a unique drop of this monster always counts, before prices load only a collection log slot counts,
	 * an unpriced item counts when it's a known chase (on the table, the market's own target, or a collection log
	 * slot) since it can't be judged on value, and a priced item must be on the table or be the target and clear
	 * the valuable-drop threshold.
	 */
	static boolean lootQualifies(long price, long threshold, boolean onTable, boolean isTarget, boolean clogSlot,
		boolean unique, boolean blacklisted, boolean popOff, long popOffMin)
	{
		boolean chase = onTable || isTarget || clogSlot;
		if (popOff)
		{
			return popOffMin <= 0 || price >= popOffMin || (unique && onTable) || (price == NO_PRICE && chase);
		}
		if (blacklisted)
		{
			return false;
		}
		if (unique && (onTable || isTarget))
		{
			// A unique drop of this very monster is a win whatever it's worth (or whether it's priced yet).
			return true;
		}
		if (price == PRICES_NOT_LOADED)
		{
			// Value can't be judged yet, but a collection log slot is a chase regardless of price.
			return clogSlot;
		}
		if (price == NO_PRICE)
		{
			return chase;
		}
		return (onTable || isTarget) && price >= threshold;
	}

	/** The game's "Valuable drop" chat message; {@code value} is negative when it couldn't be determined. */
	static boolean valuableMessageQualifies(long value, long threshold, boolean unique, boolean blacklisted,
		boolean popOff, long popOffMin)
	{
		if (popOff)
		{
			return popOffMin <= 0 || value < 0 || value >= popOffMin;
		}
		// The game flagged it as valuable but we can't price it (a brand new item): trust the game.
		return !blacklisted && (unique || value < 0 || value >= threshold);
	}

	/**
	 * The game's "Untradeable drop" chat message. Real chases count (collection log slot, on the table, or from a
	 * source with no known table at all - e.g. brand new content); other untradeables the game announces don't.
	 */
	static boolean untradeableMessageQualifies(boolean clogSlot, boolean onTable, boolean sourceHasNoTable,
		boolean blacklisted, boolean popOff)
	{
		return popOff || (!blacklisted && (clogSlot || onTable || sourceHasNoTable));
	}
}
