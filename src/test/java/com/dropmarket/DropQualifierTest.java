package com.dropmarket;

import static com.dropmarket.DropQualifier.ListStatus.BLACKLISTED;
import static com.dropmarket.DropQualifier.ListStatus.NONE;
import static com.dropmarket.DropQualifier.ListStatus.PRIORITY;
import static com.dropmarket.DropQualifier.NO_PRICE;
import static com.dropmarket.DropQualifier.PRICES_NOT_LOADED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.dropmarket.DropQualifier.ListStatus;
import org.junit.Test;

public class DropQualifierTest
{
	private static final long TWO_MIL = 2_000_000;

	private static boolean loot(long price, boolean onTable, boolean isTarget, boolean slot, ListStatus list)
	{
		return DropQualifier.lootQualifies(price, TWO_MIL, onTable, isTarget, slot, false, list, false);
	}

	private static boolean popOff(long price, boolean onTable, boolean slot, boolean unique, ListStatus list)
	{
		return DropQualifier.lootQualifies(price, TWO_MIL, onTable, false, slot, unique, list, true);
	}

	@Test
	public void cheapTableDropDoesNotCount()
	{
		assertFalse(loot(4_400, true, false, false, NONE));
	}

	@Test
	public void tableDropOverThresholdCounts()
	{
		assertTrue(loot(TWO_MIL, true, false, false, NONE));
	}

	@Test
	public void valuableItemOffTheTableDoesNotCount()
	{
		assertFalse(loot(50_000_000, false, false, false, NONE));
	}

	@Test
	public void marketTargetCountsOverThreshold()
	{
		assertTrue(loot(3_000_000, false, true, false, NONE));
	}

	@Test
	public void blacklistTrumpsEverything()
	{
		assertFalse(loot(50_000_000, true, true, true, BLACKLISTED));
		assertFalse(loot(NO_PRICE, true, true, true, BLACKLISTED));
		assertFalse(DropQualifier.lootQualifies(50_000_000, TWO_MIL, true, true, true, true, BLACKLISTED, false));
		assertFalse(popOff(50_000_000, true, true, true, BLACKLISTED));
		assertFalse(DropQualifier.valuableMessageQualifies(50_000_000, TWO_MIL, true, BLACKLISTED, true));
		assertFalse(DropQualifier.untradeableMessageQualifies(true, true, true, BLACKLISTED, true));
		assertFalse(DropQualifier.collectionLogMessageQualifies(BLACKLISTED));
		assertFalse(DropQualifier.marketMessageQualifies(BLACKLISTED));
	}

	@Test
	public void priorityTrumpsEverythingElse()
	{
		// Cheap, off the table, unpriced, or before prices load: a priority item is always a win.
		assertTrue(loot(10, false, false, false, PRIORITY));
		assertTrue(loot(NO_PRICE, false, false, false, PRIORITY));
		assertTrue(loot(PRICES_NOT_LOADED, false, false, false, PRIORITY));
		assertTrue(popOff(10, false, false, false, PRIORITY));
		assertTrue(DropQualifier.valuableMessageQualifies(4_400, TWO_MIL, false, PRIORITY, false));
		assertTrue(DropQualifier.untradeableMessageQualifies(false, false, false, PRIORITY, false));
		assertTrue(DropQualifier.collectionLogMessageQualifies(PRIORITY));
		assertTrue(DropQualifier.marketMessageQualifies(PRIORITY));
	}

	@Test
	public void unpricedChaseCountsWithoutAnyPrice()
	{
		// A brand new raid unique: no GE price yet, but it's on the table / the target / a known log slot.
		assertTrue(loot(NO_PRICE, true, false, false, NONE));
		assertTrue(loot(NO_PRICE, false, true, false, NONE));
		assertTrue(loot(NO_PRICE, false, false, true, NONE));
	}

	@Test
	public void unpricedJunkDoesNotCount()
	{
		assertFalse(loot(NO_PRICE, false, false, false, NONE));
	}

	@Test
	public void beforePricesLoadOnlyCollectionLogSlotsCount()
	{
		assertFalse(loot(PRICES_NOT_LOADED, true, true, false, NONE));
		assertTrue(loot(PRICES_NOT_LOADED, false, false, true, NONE));
	}

	@Test
	public void popOffCountsEverythingByDefault()
	{
		assertTrue(popOff(10, false, false, false, NONE));
		assertTrue(popOff(PRICES_NOT_LOADED, false, false, false, NONE));
	}

	@Test
	public void valuableMessageRespectsThreshold()
	{
		assertFalse(DropQualifier.valuableMessageQualifies(4_400, TWO_MIL, false, NONE, false));
		assertTrue(DropQualifier.valuableMessageQualifies(TWO_MIL, TWO_MIL, false, NONE, false));
	}

	@Test
	public void valuableMessageForUnpriceableItemIsTrusted()
	{
		assertTrue(DropQualifier.valuableMessageQualifies(-1, TWO_MIL, false, NONE, false));
	}

	@Test
	public void untradeableMessageNeedsARealChase()
	{
		assertFalse(DropQualifier.untradeableMessageQualifies(false, false, false, NONE, false));
		assertTrue(DropQualifier.untradeableMessageQualifies(true, false, false, NONE, false));
		assertTrue(DropQualifier.untradeableMessageQualifies(false, true, false, NONE, false));
		// New content with no table yet: trust the game's announcement.
		assertTrue(DropQualifier.untradeableMessageQualifies(false, false, true, NONE, false));
	}

	@Test
	public void untradeableMessageUnderPopOffAlwaysCounts()
	{
		assertTrue(DropQualifier.untradeableMessageQualifies(false, false, false, NONE, true));
	}

	@Test
	public void newLogItemCountsWhateverItIsWorth()
	{
		assertTrue(DropQualifier.lootQualifies(10, TWO_MIL, true, false, false, true, NONE, false));
		assertTrue(DropQualifier.lootQualifies(PRICES_NOT_LOADED, TWO_MIL, true, false, false, true, NONE, false));
		assertTrue(DropQualifier.lootQualifies(NO_PRICE, TWO_MIL, true, false, false, true, NONE, false));
	}

	@Test
	public void newLogItemCountsEvenWhenTheTableOmitsIt()
	{
		assertTrue(DropQualifier.lootQualifies(10, TWO_MIL, false, false, false, true, NONE, false));
	}

	@Test
	public void newLogItemValuableMessageCountsUnderTheThreshold()
	{
		assertTrue(DropQualifier.valuableMessageQualifies(4_400, TWO_MIL, true, NONE, false));
	}

	@Test
	public void repeatOfACommonLogItemIsPriceChecked()
	{
		// Not new to the log and under the threshold (demon tears, bolt racks...): not a win.
		assertFalse(DropQualifier.lootQualifies(900, TWO_MIL, true, false, true, false, NONE, false));
		assertFalse(DropQualifier.valuableMessageQualifies(900, TWO_MIL, false, NONE, false));
		// ...unless it clears the threshold.
		assertTrue(DropQualifier.lootQualifies(TWO_MIL, TWO_MIL, true, false, true, false, NONE, false));
	}
}
