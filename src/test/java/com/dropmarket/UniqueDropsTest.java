package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UniqueDropsTest
{
	static
	{
		BundledDropTables.configure(new com.google.gson.Gson());
		UniqueDrops.configure(new com.google.gson.Gson());
	}

	@Test
	public void collectionLogItemsFromAMonsterAreLogDrops()
	{
		assertTrue(UniqueDrops.isLogDrop("Zulrah", "Tanzanite fang"));
		assertTrue(UniqueDrops.isLogDrop("Zulrah", "Serpentine visage"));
		assertTrue(UniqueDrops.isLogDrop("Alchemical Hydra", "Hydra's claw"));
	}

	@Test
	public void handWrittenTableItemsAreLogDrops()
	{
		assertTrue(UniqueDrops.isLogDrop("Chambers of Xeric", "Twisted bow"));
		assertTrue(UniqueDrops.isLogDrop("Chambers of Xeric", "Olmlet"));
		assertTrue(UniqueDrops.isLogDrop("Yama", "Soulflame horn"));
	}

	@Test
	public void bonesAndOtherNonLogDropsAreNotLogDrops()
	{
		assertFalse(UniqueDrops.isLogDrop("Alchemical Hydra", "Hydra bones"));
		assertFalse(UniqueDrops.isLogDrop("Wyrm", "Wyrm bones"));
		assertFalse(UniqueDrops.isLogDrop("Zulrah", "Coins"));
	}

	@Test
	public void commonLogItemsAreStillLogDrops()
	{
		// Whether they win is the price check's job, not this list's.
		assertTrue(UniqueDrops.isLogDrop("Doom of Mokhaiotl", "Demon tear"));
	}

	@Test
	public void nullsAreNeverUnique()
	{
		assertFalse(UniqueDrops.isLogDrop(null, "Tanzanite fang"));
		assertFalse(UniqueDrops.isLogDrop("Zulrah", null));
	}
}
