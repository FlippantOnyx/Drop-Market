package com.dropmarket;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class GuardTest
{
	private final Guard guard = new Guard();

	@Test
	public void swallowsNullPointers()
	{
		guard.run("npe", () -> ((String) null).length());
	}

	@Test
	public void swallowsLinkageErrors()
	{
		guard.run("access", () ->
		{
			throw new IllegalAccessError("failed to access class");
		});
	}

	@Test
	public void swallowsStackOverflow()
	{
		guard.run("recursion", new Runnable()
		{
			@Override
			public void run()
			{
				run();
			}
		});
	}

	@Test
	public void callFallsBackWhenItFails()
	{
		Assert.assertEquals("fallback", guard.call("call", () -> ((String) null).trim(), "fallback"));
		Assert.assertEquals("fine", guard.call("call ok", () -> "fine", "fallback"));
	}

	@Test
	public void keepsRunningAfterAFailure()
	{
		AtomicInteger runs = new AtomicInteger();
		for (int i = 0; i < 5; i++)
		{
			guard.run("repeat", () ->
			{
				runs.incrementAndGet();
				throw new IllegalStateException("again");
			});
		}
		Assert.assertEquals(5, runs.get());
	}
}
