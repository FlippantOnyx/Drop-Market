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
		// The vestiges are untradeable drops that go into the icons, which go into the rings.
		assertTrue(components.contains("Magus vestige"));
		assertTrue(components.contains("Ultor vestige"));
		assertTrue(components.contains("Bellator vestige"));
		assertTrue(components.contains("Venator vestige"));
	}

	@Test
	public void eachComponentIsWorthWhatItEndsUpAs()
	{
		// Ultor vestige -> Ultor icon -> Ultor ring (about 110m).
		assertTrue(components.valueOf("Ultor vestige") > 100_000_000L);
		assertTrue(components.valueOf("Mokhaiotl cloth") > 50_000_000L);
		assertTrue(components.valueOf("Araxyte fang") > 20_000_000L);
		// Soulreaper axe parts: worth celebrating, but not valued at the whole axe.
		assertTrue(components.contains("Eye of the Duke"));
		org.junit.Assert.assertEquals(0, components.valueOf("Eye of the Duke"));
		org.junit.Assert.assertEquals(0, components.valueOf("Executioner's axe head"));
	}

	@Test
	public void componentsKnowWhichSourceDropsThem()
	{
		assertTrue(components.dropsFrom("Vardorvis").contains("Ultor vestige"));
		assertTrue(components.dropsFrom("Duke Sucellus").contains("Magus vestige"));
		assertTrue(components.dropsFrom("Zulrah").isEmpty());
	}

	@Test
	public void thingsYouMakeButNeverGetAsADropAreNot()
	{
		// Icons and the infernal chunk are crafted, and the crystal weapon line is left alone on purpose.
		assertFalse(components.contains("Magus icon"));
		assertFalse(components.contains("Ultor icon"));
		assertFalse(components.contains("Infernal chunk"));
		assertFalse(components.contains("Guthixian icon"));
		assertFalse(components.contains("Blade of Saeldor (c)"));
		assertFalse(components.contains("Crystal shard"));
	}

	@Test
	public void plainUntradeablesAndCoinsAreNot()
	{
		assertFalse(components.contains("Xeric's talisman"));
		assertFalse(components.contains("Coins"));
		assertFalse(components.contains(null));
	}

	@Test
	public void multiPartItemsListTheirSiblings()
	{
		assertTrue(components.partsOf("Noxious pommel").containsAll(java.util.Arrays.asList("Noxious blade", "Noxious point", "Noxious pommel")));
		assertTrue(components.partsOf("Bludgeon claw").containsAll(java.util.Arrays.asList("Bludgeon axon", "Bludgeon claw", "Bludgeon spine")));
		// A vestige is a single part: it just takes the whole ring's value.
		assertTrue(components.partsOf("Ultor vestige").isEmpty());
		assertTrue(components.dropsFrom("Unsired").contains("Bludgeon spine"));
	}
}
