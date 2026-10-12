package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Random;
import java.util.function.DoubleConsumer;
import org.junit.Test;

public class MarketGraphTest
{
	@Test
	public void randomWalkStaysInBoundsAndBelowCertain()
	{
		MarketGraph graph = new MarketGraph(new Random(42));
		graph.reset();
		assertEquals(MarketGraph.POINTS, graph.size());
		for (int i = 0; i < 10_000; i++)
		{
			graph.step();
			assertTrue(graph.current() >= 1 && graph.current() <= 91);
		}
	}

	@Test
	public void pinToCertainHitsOneHundred()
	{
		MarketGraph graph = new MarketGraph(new Random(1));
		graph.reset();
		long volumeBefore = graph.volume();
		graph.pinToCertain();
		assertEquals(100, graph.current(), 0);
		assertEquals(MarketGraph.POINTS, graph.size());
		assertTrue(graph.volume() > volumeBefore);
	}

	@Test
	public void historyIsOldestFirst()
	{
		MarketGraph graph = new MarketGraph(new Random(7));
		graph.reset();
		double newestBefore = graph.current();
		graph.pinToCertain();
		assertEquals(newestBefore, graph.get(MarketGraph.POINTS - 2), 0);
		assertEquals(newestBefore, graph.previous(), 0);
	}

	/** Runs many markets and reports every value, so shape rules can be checked statistically. */
	private static void simulate(DriftProfile profile, int markets, int steps, DoubleConsumer each)
	{
		for (int seed = 0; seed < markets; seed++)
		{
			MarketGraph graph = new MarketGraph(new Random(seed));
			graph.reset(profile);
			for (int i = 0; i < steps; i++)
			{
				graph.step();
				each.accept(graph.current());
			}
		}
	}

	@Test
	public void everyProfileOtherThanClimberStaysUnderEightyTwo()
	{
		for (DriftProfile profile : DriftProfile.values())
		{
			if (profile != DriftProfile.CLIMBER)
			{
				simulate(profile, 200, 400, v -> assertTrue(profile + " reached " + v, v <= 82));
			}
		}
	}

	@Test
	public void steadyRarelyGetsHighAndRarelyFloorsOut()
	{
		int[] counts = new int[3];
		simulate(DriftProfile.STEADY, 300, 400, v ->
		{
			counts[0]++;
			if (v > 70)
			{
				counts[1]++;
			}
			if (v < 9)
			{
				counts[2]++;
			}
		});
		assertTrue("high share " + counts[1] / (double) counts[0], counts[1] < counts[0] * 0.03);
		assertTrue("low share " + counts[2] / (double) counts[0], counts[2] < counts[0] * 0.10);
	}

	@Test
	public void noProfileHoversLowForLong()
	{
		for (DriftProfile profile : DriftProfile.values())
		{
			int[] run = {0};
			int[] longest = {0};
			simulate(profile, 100, 600, v ->
			{
				run[0] = v < 9 ? run[0] + 1 : 0;
				longest[0] = Math.max(longest[0], run[0]);
			});
			assertTrue(profile + " longest low run " + longest[0], longest[0] <= 16);
		}
	}

	@Test
	public void climberDriftsUpToAroundNinety()
	{
		double[] peak = {0};
		simulate(DriftProfile.CLIMBER, 100, 500, v -> peak[0] = Math.max(peak[0], v));
		assertTrue("peak " + peak[0], peak[0] >= 84 && peak[0] <= 91);
	}

	@Test
	public void profileMixIsMostlySteadyWithClimberUncommon()
	{
		Random random = new Random(5);
		int steady = 0;
		int climber = 0;
		int n = 20_000;
		for (int i = 0; i < n; i++)
		{
			DriftProfile p = DriftProfile.roll(random);
			steady += p == DriftProfile.STEADY ? 1 : 0;
			climber += p == DriftProfile.CLIMBER ? 1 : 0;
		}
		assertTrue(steady > n * 0.45);
		assertTrue(climber > n * 0.10 && climber < n * 0.20);
	}
}
