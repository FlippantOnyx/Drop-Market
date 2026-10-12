package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UniqueDropsTest
{
	private final BundledDropTables bundled = new BundledDropTables(new com.google.gson.Gson());
	private final UniqueDrops uniqueDrops = new UniqueDrops(new com.google.gson.Gson());

	@Test
	public void collectionLogItemsFromAMonsterAreLogDrops()
	{
		assertTrue(uniqueDrops.isLogDrop("Zulrah", "Tanzanite fang"));
		assertTrue(uniqueDrops.isLogDrop("Zulrah", "Serpentine visage"));
		assertTrue(uniqueDrops.isLogDrop("Alchemical Hydra", "Hydra's claw"));
	}

	@Test
	public void handWrittenTableItemsAreLogDrops()
	{
		assertTrue(uniqueDrops.isLogDrop("Chambers of Xeric", "Twisted bow"));
		assertTrue(uniqueDrops.isLogDrop("Chambers of Xeric", "Olmlet"));
		assertTrue(uniqueDrops.isLogDrop("Yama", "Soulflame horn"));
	}

	@Test
	public void bonesAndOtherNonLogDropsAreNotLogDrops()
	{
		assertFalse(uniqueDrops.isLogDrop("Alchemical Hydra", "Hydra bones"));
		assertFalse(uniqueDrops.isLogDrop("Wyrm", "Wyrm bones"));
		assertFalse(uniqueDrops.isLogDrop("Zulrah", "Coins"));
	}

	@Test
	public void commonLogItemsAreStillLogDrops()
	{
		// Whether they win is the price check's job, not this list's.
		assertTrue(uniqueDrops.isLogDrop("Doom of Mokhaiotl", "Demon tear"));
	}

	@Test
	public void nullsAreNeverUnique()
	{
		assertFalse(uniqueDrops.isLogDrop(null, "Tanzanite fang"));
		assertFalse(uniqueDrops.isLogDrop("Zulrah", null));
	}
}
