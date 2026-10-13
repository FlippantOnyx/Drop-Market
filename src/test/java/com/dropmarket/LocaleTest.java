package com.dropmarket;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;

/** Matching and number formatting must work the same for a player whose computer is set to Turkish or German. */
public class LocaleTest
{
	@Test
	public void helpersTolerateNull()
	{
		assertEquals("", DropTables.normalize(null));
		assertEquals("item name", DropTables.normalize("ITEM name"));
		assertEquals("", MarketPainter.shortTitle(null, true).trim());
		assertEquals("", MarketPainter.abbreviatedTitle(null).trim());
	}

	@Test
	public void turkishDefaultLocaleDoesNotBreakMatching()
	{
		Locale before = Locale.getDefault();
		try
		{
			// With the default locale a capital I lower-cases to a dotless i and the table lookup would miss.
			Locale.setDefault(new Locale("tr", "TR"));
			assertEquals("tanzanite fang", DropTables.normalize("TANZANITE FANG"));
			assertEquals("TANZANITE FANG", MarketPainter.shortTitle("Tanzanite fang", false));
			assertEquals("ZULRAH DROPS TANZANITE FANG", MarketPainter.abbreviatedTitle("Zulrah drops Tanzanite fang"));
		}
		finally
		{
			Locale.setDefault(before);
		}
	}

	@Test
	public void germanDefaultLocaleDoesNotChangeTheNumbers()
	{
		Locale before = Locale.getDefault();
		try
		{
			Locale.setDefault(Locale.GERMANY);
			assertEquals("$1.5M Vol.", MarketPainter.formatVolume(1_500_000L));
			assertEquals("$2.2B Vol.", MarketPainter.formatVolume(2_200_000_000L));
			assertEquals("$1.5M", MarketPainter.formatPop(1_500_000L));
		}
		finally
		{
			Locale.setDefault(before);
		}
	}
}
