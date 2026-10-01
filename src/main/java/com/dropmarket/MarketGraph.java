package com.dropmarket;

import java.util.Random;

class MarketGraph
{
	static final int POINTS = 40;
	private static final double MIN = 1;
	private static final double MAX = 97;

	private final Random random;
	private final double[] points = new double[POINTS];
	private int head;
	private int count;
	private double anchor;
	private long volume;

	MarketGraph(Random random)
	{
		this.random = random;
	}

	/** Opens a fresh market with a believable-looking price history already on the chart. */
	void reset()
	{
		head = 0;
		count = 0;
		anchor = 6 + random.nextDouble() * 50;
		volume = 50_000L + (long) (random.nextDouble() * 5_000_000L);
		push(clamp(anchor + random.nextGaussian() * 6));
		for (int i = 1; i < POINTS; i++)
		{
			step();
		}
	}

	void step()
	{
		double last = current();
		double drift = (anchor - last) * 0.08;
		double move = random.nextGaussian() * 2.6;
		if (random.nextDouble() < 0.07)
		{
			move += (random.nextBoolean() ? 1 : -1) * (6 + random.nextDouble() * 10);
		}
		push(clamp(last + drift + move));
		volume += (long) (random.nextDouble() * 180_000L);
		if (random.nextDouble() < 0.03)
		{
			anchor = 6 + random.nextDouble() * 50;
		}
	}

	void addVolume(long amount)
	{
		volume += amount;
	}

	void pinToCertain()
	{
		push(100);
		volume += 25_000_000L + (long) (random.nextDouble() * 50_000_000L);
	}

	private void push(double value)
	{
		points[head] = value;
		head = (head + 1) % POINTS;
		count = Math.min(count + 1, POINTS);
	}

	private static double clamp(double v)
	{
		return Math.max(MIN, Math.min(MAX, v));
	}

	int size()
	{
		return count;
	}

	/** Oldest-first access: get(0) is the oldest point, get(size() - 1) the newest. */
	double get(int i)
	{
		int start = (head - count + POINTS) % POINTS;
		return points[(start + i) % POINTS];
	}

	double current()
	{
		return count == 0 ? 0 : get(count - 1);
	}

	double previous()
	{
		return count < 2 ? current() : get(count - 2);
	}

	long volume()
	{
		return volume;
	}
}
