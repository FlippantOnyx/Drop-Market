package com.dropmarket;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

final class MarketPicker
{
	private MarketPicker()
	{
	}

	/** Priciest drop over the threshold, else first locked untradeable slot (pet-first: reversed), else null. */
	static String pick(List<String> table, boolean petFirst, long threshold, ToLongFunction<String> price,
		Predicate<String> lockedSlot)
	{
		String bestTradeable = null;
		long bestPrice = 0;
		String chase = null;
		for (String item : table)
		{
			long p = price.applyAsLong(item);
			if (p > bestPrice)
			{
				bestTradeable = item;
				bestPrice = p;
			}
			else if (p < 0 && chase == null && lockedSlot.test(item))
			{
				chase = item;
			}
		}

		boolean valuable = bestTradeable != null && bestPrice >= threshold;
		if (petFirst)
		{
			return chase != null ? chase : valuable ? bestTradeable : null;
		}
		return valuable ? bestTradeable : chase;
	}
}
