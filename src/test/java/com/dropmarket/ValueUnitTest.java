package com.dropmarket;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ValueUnitTest
{
	@Test
	public void convertsEachDenominationToGp()
	{
		assertEquals(250_000L, ValueUnit.toGp(ValueUnit.K, 250));
		assertEquals(2_000_000L, ValueUnit.toGp(ValueUnit.K, 2_000));
		assertEquals(2_500_000L, ValueUnit.toGp(ValueUnit.M, 2) + ValueUnit.toGp(ValueUnit.K, 500));
		assertEquals(1_000_000_000L, ValueUnit.toGp(ValueUnit.B, 1));
	}

	@Test
	public void aMissingUnitIsThousandsAndNegativesAreZero()
	{
		assertEquals(5_000L, ValueUnit.toGp(null, 5));
		assertEquals(0L, ValueUnit.toGp(ValueUnit.M, -3));
	}

	@Test
	public void biggestSettingStillFitsInALong()
	{
		assertEquals(100_000_000_000_000L, ValueUnit.toGp(ValueUnit.B, 100_000));
	}

	@Test
	public void defaultsMatchTheOldGpDefaults()
	{
		DropMarketConfig config = new DropMarketConfig()
		{
		};
		assertEquals(250_000L, ValueUnit.toGp(config.valueUnit(), config.marketMinimumAmount()));
		assertEquals(2_000_000L, ValueUnit.toGp(config.valueUnit(), config.winMinimumAmount()));
	}
}
