package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import org.junit.Test;

public class ValuableComponentsTest
{
	private final ValuableComponents components = new ValuableComponents(new Gson());

	@Test
	public void untradeableBossDropsThatMakeExpensiveItemsAreOnTheList()
	{
		assertTrue(components.contains("Mokhaiotl cloth"));
		assertTrue(components.contains("Araxyte fang"));
		assertTrue(components.contains("  NOXIOUS BLADE "));
		assertTrue(components.contains("Bludgeon claw"));
		assertTrue(components.contains("Eye of the Duke"));
	}

	@Test
	public void thingsYouMakeButNeverGetAsADropAreNot()
	{
		// Icons are made from vestiges, the infernal chunk from oathplate shards, the blade from the seed.
		assertFalse(components.contains("Magus icon"));
		assertFalse(components.contains("Ultor icon"));
		assertFalse(components.contains("Infernal chunk"));
		assertFalse(components.contains("Guthixian icon"));
		assertFalse(components.contains("Blade of Saeldor (c)"));
	}

	@Test
	public void plainUntradeablesAndCoinsAreNot()
	{
		assertFalse(components.contains("Xeric's talisman"));
		assertFalse(components.contains("Coins"));
		assertFalse(components.contains(null));
	}
}
