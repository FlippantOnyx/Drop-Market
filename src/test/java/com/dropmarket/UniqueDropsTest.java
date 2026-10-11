package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UniqueDropsTest
{
	@Test
	public void bossUniqueIsUniqueToThatBoss()
	{
		assertTrue(UniqueDrops.isUnique("Zulrah", "Tanzanite fang"));
		assertTrue(UniqueDrops.isUnique("Chambers of Xeric", "Twisted bow"));
	}

	@Test
	public void itemsSharedByAFewVariantsStillCount()
	{
		assertTrue(UniqueDrops.isUnique("The Nightmare", "Jar of dreams"));
		assertTrue(UniqueDrops.isUnique("Phosani's Nightmare", "Jar of dreams"));
	}

	@Test
	public void commonSharedDropsAreNotUnique()
	{
		assertFalse(UniqueDrops.isUnique("Zulrah", "Rune 2h sword"));
		assertFalse(UniqueDrops.isUnique("Zulrah", "Dragon med helm"));
	}

	@Test
	public void itemMustBeOnThatSourcesTable()
	{
		assertFalse(UniqueDrops.isUnique("Vorkath", "Tanzanite fang"));
		assertFalse(UniqueDrops.isUnique(null, "Tanzanite fang"));
	}
}
