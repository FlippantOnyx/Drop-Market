package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import org.junit.Test;

public class ValuableComponentsTest
{
	private final ValuableComponents components = new ValuableComponents(new Gson());

	@Test
	public void expensiveCraftingComponentsAreOnTheList()
	{
		assertTrue(components.contains("Mokhaiotl cloth"));
		assertTrue(components.contains("Magus icon"));
		assertTrue(components.contains("  ULTOR ICON "));
		assertTrue(components.contains("Noxious blade"));
	}

	@Test
	public void plainUntradeablesAreNot()
	{
		assertFalse(components.contains("Xeric's talisman"));
		assertFalse(components.contains(null));
	}
}
