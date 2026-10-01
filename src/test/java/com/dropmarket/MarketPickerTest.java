package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class MarketPickerTest
{
	private static final long THRESHOLD = 250_000;
	private static final Map<String, Long> PRICES = ImmutableMap.of(
		"Tanzanite fang", 1_500_000L,
		"Magic fang", 1_200_000L,
		"Bronze spear", 30L,
		"Leaf-bladed battleaxe", 120_000L);

	private static long price(String item)
	{
		return PRICES.getOrDefault(item, -1L);
	}

	private static String pick(boolean petFirst, Set<String> locked, String... table)
	{
		return MarketPicker.pick(Arrays.asList(table), petFirst, THRESHOLD, MarketPickerTest::price, locked::contains);
	}

	@Test
	public void valuableTradeableWinsWhenPetAlreadyObtained()
	{
		assertEquals("Tanzanite fang",
			pick(false, ImmutableSet.of(), "Tanzanite fang", "Magic fang", "Pet snakeling"));
	}

	@Test
	public void unobtainedPetIsNeverExcludedByAValuableTradeable()
	{
		assertEquals("Pet snakeling",
			pick(false, ImmutableSet.of("Pet snakeling"), "Tanzanite fang", "Magic fang", "Pet snakeling"));
	}

	@Test
	public void cheapMonsterWithLockedUntradeableChasesIt()
	{
		assertEquals("Goblin champion scroll",
			pick(false, ImmutableSet.of("Goblin champion scroll"), "Bronze spear", "Goblin champion scroll"));
	}

	@Test
	public void cheapMonsterWithEverythingUnlockedIsLiveOdds()
	{
		assertNull(pick(false, ImmutableSet.of(), "Bronze spear", "Goblin champion scroll"));
		assertNull(pick(false, ImmutableSet.of(), "Leaf-bladed battleaxe"));
	}

	@Test
	public void petFirstPrefersLockedPetThenFallsBack()
	{
		assertEquals("Rock golem", pick(true, ImmutableSet.of("Rock golem"), "Rock golem"));
		assertNull(pick(true, ImmutableSet.of(), "Rock golem"));
		assertEquals("Tanzanite fang", pick(true, ImmutableSet.of(), "Phoenix", "Tanzanite fang"));
	}

	@Test
	public void firstLockedSlotInTableOrderIsChosenWhenNoPetIsInPlay()
	{
		assertEquals("Goblin champion scroll",
			pick(false, ImmutableSet.of("Goblin champion scroll", "Black mask"), "Goblin champion scroll", "Black mask"));
	}

	@Test
	public void unobtainedPetWinsOverOtherLockedSlotsRegardlessOfTableOrder()
	{
		assertEquals("Tzrek-jad",
			pick(false, ImmutableSet.of("Fire cape", "Tzrek-jad"), "Fire cape", "Tzrek-jad"));
		assertEquals("Tzrek-jad", pick(false, ImmutableSet.of("Tzrek-jad"), "Fire cape", "Tzrek-jad"));
	}
}
