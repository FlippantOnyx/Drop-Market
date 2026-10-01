package com.dropmarket;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

final class MarketPicker
{
	private MarketPicker()
	{
	}

	/**
	 * An unobtained pet always wins, regardless of threshold - no valuable tradeable drop should ever shadow it
	 * out of rotation. Otherwise: priciest drop over the threshold, else first locked untradeable slot
	 * (pet-first: reversed), else null.
	 */
	static String pick(List<String> table, boolean petFirst, long threshold, ToLongFunction<String> price,
		Predicate<String> lockedSlot)
	{
		String bestTradeable = null;
		long bestPrice = 0;
		String chase = null;
		String petChase = null;
		for (String item : table)
		{
			long p = price.applyAsLong(item);
			if (p > bestPrice)
			{
				bestTradeable = item;
				bestPrice = p;
			}
			else if (p < 0 && lockedSlot.test(item))
			{
				if (DropTables.isPet(item))
				{
					if (petChase == null)
					{
						petChase = item;
					}
				}
				else if (chase == null)
				{
					chase = item;
				}
			}
		}

		if (petChase != null)
		{
			return petChase;
		}

		boolean valuable = bestTradeable != null && bestPrice >= threshold;
		if (petFirst)
		{
			return chase != null ? chase : valuable ? bestTradeable : null;
		}
		return valuable ? bestTradeable : chase;
	}
}
