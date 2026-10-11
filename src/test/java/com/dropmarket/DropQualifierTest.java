package com.dropmarket;

import static com.dropmarket.DropQualifier.NO_PRICE;
import static com.dropmarket.DropQualifier.PRICES_NOT_LOADED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DropQualifierTest
{
	private static final long TWO_MIL = 2_000_000;

	private static boolean loot(long price, boolean onTable, boolean isTarget, boolean slot, boolean blacklisted)
	{
		return DropQualifier.lootQualifies(price, TWO_MIL, onTable, isTarget, slot, blacklisted, false, 0);
	}

	@Test
	public void cheapTableDropDoesNotCount()
	{
		assertFalse(loot(4_400, true, false, false, false));
	}

	@Test
	public void tableDropOverThresholdCounts()
	{
		assertTrue(loot(TWO_MIL, true, false, false, false));
	}

	@Test
	public void valuableItemOffTheTableDoesNotCount()
	{
		assertFalse(loot(50_000_000, false, false, false, false));
	}

	@Test
	public void marketTargetCountsOverThreshold()
	{
		assertTrue(loot(3_000_000, false, true, false, false));
	}

	@Test
	public void blacklistedItemNeverCounts()
	{
		assertFalse(loot(50_000_000, true, true, true, true));
		assertFalse(loot(NO_PRICE, true, true, true, true));
	}

	@Test
	public void unpricedChaseCountsWithoutAnyPrice()
	{
		// A brand new raid unique: no GE price yet, but it's on the table / the target / a known log slot.
		assertTrue(loot(NO_PRICE, true, false, false, false));
		assertTrue(loot(NO_PRICE, false, true, false, false));
		assertTrue(loot(NO_PRICE, false, false, true, false));
	}

	@Test
	public void unpricedJunkDoesNotCount()
	{
		assertFalse(loot(NO_PRICE, false, false, false, false));
	}

	@Test
	public void beforePricesLoadOnlyCollectionLogSlotsCount()
	{
		assertFalse(loot(PRICES_NOT_LOADED, true, true, false, false));
		assertTrue(loot(PRICES_NOT_LOADED, false, false, true, false));
		assertFalse(loot(PRICES_NOT_LOADED, false, false, true, true));
	}

	@Test
	public void valuableMessageCountsBeforePricesLoad()
	{
		// No value could be looked up (-1), but the game itself flagged the drop as valuable.
		assertTrue(DropQualifier.valuableMessageQualifies(-1, TWO_MIL, false, false, 0));
	}

	@Test
	public void popOffCountsEverythingByDefaultEvenBlacklisted()
	{
		assertTrue(DropQualifier.lootQualifies(10, TWO_MIL, false, false, false, true, true, 0));
		assertTrue(DropQualifier.lootQualifies(PRICES_NOT_LOADED, TWO_MIL, false, false, false, false, true, 0));
	}

	@Test
	public void popOffMinValueFiltersCheapDropsButKeepsUnpricedChases()
	{
		assertFalse(DropQualifier.lootQualifies(500, TWO_MIL, true, false, false, false, true, 1_000));
		assertTrue(DropQualifier.lootQualifies(1_000, TWO_MIL, false, false, false, false, true, 1_000));
		assertTrue(DropQualifier.lootQualifies(NO_PRICE, TWO_MIL, false, false, true, false, true, 1_000));
		assertFalse(DropQualifier.lootQualifies(NO_PRICE, TWO_MIL, false, false, false, false, true, 1_000));
	}

	@Test
	public void valuableMessageRespectsThresholdAndBlacklist()
	{
		assertFalse(DropQualifier.valuableMessageQualifies(4_400, TWO_MIL, false, false, 0));
		assertTrue(DropQualifier.valuableMessageQualifies(TWO_MIL, TWO_MIL, false, false, 0));
		assertFalse(DropQualifier.valuableMessageQualifies(TWO_MIL, TWO_MIL, true, false, 0));
	}

	@Test
	public void valuableMessageForUnpriceableItemIsTrusted()
	{
		assertTrue(DropQualifier.valuableMessageQualifies(-1, TWO_MIL, false, false, 0));
	}

	@Test
	public void untradeableMessageNeedsARealChase()
	{
		assertFalse(DropQualifier.untradeableMessageQualifies(false, false, false, false, false));
		assertTrue(DropQualifier.untradeableMessageQualifies(true, false, false, false, false));
		assertTrue(DropQualifier.untradeableMessageQualifies(false, true, false, false, false));
		// New content with no table yet: trust the game's announcement.
		assertTrue(DropQualifier.untradeableMessageQualifies(false, false, true, false, false));
		assertFalse(DropQualifier.untradeableMessageQualifies(true, true, true, true, false));
	}

	@Test
	public void untradeableMessageUnderPopOffAlwaysCounts()
	{
		assertTrue(DropQualifier.untradeableMessageQualifies(false, false, false, true, true));
	}
}
