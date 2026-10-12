package com.dropmarket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Keeps a bug on our side from ever reaching the game client. Everything RuneLite calls into (events, the overlay,
 * start and stop) runs through here: a failure is logged once per place and swallowed, and the plugin carries on.
 */
@Slf4j
@Singleton
class Guard
{
	private final Set<String> reported = ConcurrentHashMap.newKeySet();
	private final AtomicInteger failures = new AtomicInteger();

	@Inject
	Guard()
	{
	}

	void run(String where, Runnable action)
	{
		try
		{
			action.run();
		}
		catch (Exception | LinkageError | StackOverflowError e)
		{
			report(where, e);
		}
	}

	<T> T call(String where, Supplier<T> action, T fallback)
	{
		try
		{
			return action.get();
		}
		catch (Exception | LinkageError | StackOverflowError e)
		{
			report(where, e);
			return fallback;
		}
	}

	/** How many failures have been swallowed so far; the integration tests expect this to stay at 0. */
	int failures()
	{
		return failures.get();
	}

	/** The places that have failed, for test output. */
	Set<String> failedPlaces()
	{
		return reported;
	}

	// A broken overlay would fail 50 times a second, so each place is only logged the first time.
	private void report(String where, Throwable e)
	{
		failures.incrementAndGet();
		if (reported.add(where))
		{
			log.warn("Prediction Market Drops hit a problem in {} and kept going", where, e);
		}
	}
}
