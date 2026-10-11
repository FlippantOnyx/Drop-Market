package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UniqueDropsTest
{
	@Test
	public void rareCollectionLogItemsAreUniques()
	{
		assertTrue(UniqueDrops.isUnique("Zulrah", "Tanzanite fang"));
		assertTrue(UniqueDrops.isUnique("Zulrah", "Serpentine visage"));
		assertTrue(UniqueDrops.isUnique("Alchemical Hydra", "Hydra's claw"));
	}

	@Test
	public void handWrittenTableItemsAreUniques()
	{
		assertTrue(UniqueDrops.isUnique("Chambers of Xeric", "Twisted bow"));
		assertTrue(UniqueDrops.isUnique("Chambers of Xeric", "Olmlet"));
		assertTrue(UniqueDrops.isUnique("Yama", "Soulflame horn"));
	}

	@Test
	public void bonesAndOtherNonLogDropsAreNotUniques()
	{
		assertFalse(UniqueDrops.isUnique("Alchemical Hydra", "Hydra bones"));
		assertFalse(UniqueDrops.isUnique("Wyrm", "Wyrm bones"));
		assertFalse(UniqueDrops.isUnique("Zulrah", "Zulrah's scales"));
		assertFalse(UniqueDrops.isUnique("Zulrah", "Coins"));
	}

	@Test
	public void superCommonLogItemsAreNotUniques()
	{
		assertFalse(UniqueDrops.isUnique("Doom of Mokhaiotl", "Demon tear"));
		assertFalse(UniqueDrops.isUnique("Chest (Barrows)", "Bolt rack"));
		assertFalse(UniqueDrops.isUnique("Rewards Chest (Fortis Colosseum)", "Sunfire splinters"));
	}

	@Test
	public void nullsAreNeverUnique()
	{
		assertFalse(UniqueDrops.isUnique(null, "Tanzanite fang"));
		assertFalse(UniqueDrops.isUnique("Zulrah", null));
	}
}
