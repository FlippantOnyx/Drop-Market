package com.dropmarket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CelebrationTest
{
	@Test
	public void endsAfterItsTimeEvenIfYouKeepKillingTheSameMonster()
	{
		// 15 seconds is 25 game ticks.
		assertFalse(Celebration.isOver(24, 25, false));
		assertTrue(Celebration.isOver(25, 25, false));
		assertTrue(Celebration.isOver(200, 25, false));
	}

	@Test
	public void endsImmediatelyWhenYouMoveToAnotherSource()
	{
		assertTrue(Celebration.isOver(1, 25, true));
	}
}
