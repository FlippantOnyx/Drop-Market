package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Random;
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
			assertTrue(graph.current() >= 1 && graph.current() <= 97);
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
}
