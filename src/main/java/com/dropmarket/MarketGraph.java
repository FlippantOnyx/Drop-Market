package com.dropmarket;

import java.util.Random;

class MarketGraph
{
	static final int POINTS = 40;
	private static final double MIN = 1;
	// Below LOW_MARK for several steps in a row the line is nudged back up; above HIGH_MARK it is pulled back down.
	private static final double LOW_MARK = 9;
	private static final double HIGH_MARK = 74;
	private static final int LOW_DWELL_STEPS = 6;
	private static final int HIGH_DWELL_STEPS = 6;

	private final Random random;
	private final double[] points = new double[POINTS];
	private int head;
	private int count;
	private double anchor;
	private long volume;

	private DriftProfile profile = DriftProfile.STEADY;
	// Per-step anchor change (CLIMBER up, FADER down) and where the climb ends.
	private double trend;
	private double climbTarget;
	private int spikeLeft;
	private double spikeBase;
	private int lowDwell;
	private int liftSteps;
	private int highDwell;

	MarketGraph(Random random)
	{
		this.random = random;
	}

	DriftProfile profile()
	{
		return profile;
	}

	/** Opens a fresh market with a believable-looking price history already on the chart. */
	void reset()
	{
		reset(DriftProfile.roll(random));
	}

	void reset(DriftProfile forced)
	{
		head = 0;
		count = 0;
		profile = forced;
		anchor = profile.rollAnchor(random);
		trend = 0;
		spikeLeft = 0;
		lowDwell = 0;
		highDwell = 0;
		liftSteps = 0;
		if (profile == DriftProfile.CLIMBER)
		{
			trend = 0.3 + random.nextDouble() * 0.25;
			climbTarget = 84 + random.nextDouble() * 6;
		}
		else if (profile == DriftProfile.FADER)
		{
			trend = -(0.25 + random.nextDouble() * 0.15);
		}
		volume = 50_000L + (long) (random.nextDouble() * 5_000_000L);
		push(clamp(anchor + random.nextGaussian() * 6, anchor));
		for (int i = 1; i < POINTS; i++)
		{
			step();
		}
	}

	void step()
	{
		double last = current();
		advanceProfile();
		double drift = (anchor - last) * 0.08;
		double move = random.nextGaussian() * profile.noise;
		if (liftSteps > 0)
		{
			// Climbing out of a low patch: a steady push on top of the anchor pull.
			move += 2.5;
			liftSteps--;
		}
		if (profile.jumpChance > 0 && random.nextDouble() < profile.jumpChance)
		{
			move += (random.nextBoolean() ? 1 : -1) * (profile.jumpMin + random.nextDouble() * profile.jumpSpan);
		}
		double next = clamp(last + drift + move, last);
		trackDwell(next);
		push(next);
		volume += (long) (random.nextDouble() * 180_000L);
		if (profile.rerollChance > 0 && random.nextDouble() < profile.rerollChance)
		{
			anchor = profile.rollAnchor(random);
		}
	}

	/** Moves the anchor the line is pulled toward, for the profiles that change over time. */
	private void advanceProfile()
	{
		switch (profile)
		{
			case CLIMBER:
				anchor += trend;
				if (anchor >= climbTarget)
				{
					// The fight is over: settle back to everyday movement from a mid level.
					profile = DriftProfile.STEADY;
					anchor = 28 + random.nextDouble() * 20;
				}
				break;
			case FADER:
				anchor += trend;
				if (anchor <= 16)
				{
					profile = DriftProfile.STEADY;
					anchor = profile.rollAnchor(random);
				}
				break;
			case SPIKE:
				if (spikeLeft > 0)
				{
					spikeLeft--;
					if (spikeLeft == 0)
					{
						anchor = spikeBase;
					}
				}
				else if (random.nextDouble() < 0.03)
				{
					spikeLeft = 6 + random.nextInt(5);
					spikeBase = anchor;
					anchor = Math.min(anchor + 25 + random.nextDouble() * 10, 70);
				}
				break;
			default:
				break;
		}
	}

	/** Keeps the line from sitting very low, or very high, for long. */
	private void trackDwell(double value)
	{
		lowDwell = value < LOW_MARK ? lowDwell + 1 : 0;
		if (lowDwell >= LOW_DWELL_STEPS)
		{
			anchor = Math.max(anchor, 18 + random.nextDouble() * 22);
			liftSteps = 6;
			lowDwell = 0;
		}
		highDwell = value > HIGH_MARK && profile != DriftProfile.CLIMBER ? highDwell + 1 : 0;
		if (highDwell >= HIGH_DWELL_STEPS)
		{
			anchor = Math.min(anchor, 30 + random.nextDouble() * 15);
			highDwell = 0;
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

	/** Inside the profile's ceiling, but never snapping down: a line already above it (end of a climb) just can't rise. */
	private double clamp(double v, double last)
	{
		double upper = Math.max(profile.ceiling, last);
		return Math.max(MIN, Math.min(upper, v));
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
