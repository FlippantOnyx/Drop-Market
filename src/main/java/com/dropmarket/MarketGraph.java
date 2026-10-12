package com.dropmarket;

import java.util.Random;

class MarketGraph
{
	static final int POINTS = 40;
	/** The plugin steps a live graph every 5 game ticks (3 seconds); profile timing below is counted in those steps. */
	static final int STEP_SECONDS = 3;
	private static final double MIN = 1;
	// Below LOW_MARK for several steps in a row the line is nudged back up; above HIGH_MARK it is pulled back down.
	private static final double LOW_MARK = 9;
	private static final double HIGH_MARK = 74;
	private static final int LOW_DWELL_STEPS = 6;
	private static final int HIGH_DWELL_STEPS = 6;
	// A drift profile lasts 10-15 minutes of live stepping, whatever the player is doing; after that it eases into the
	// next one at the next finished kill or action.
	private static final int PROFILE_MIN_STEPS = 10 * 60 / STEP_SECONDS;
	private static final int PROFILE_EXTRA_STEPS = 5 * 60 / STEP_SECONDS;
	private static final int TRANSITION_STEPS = 20;
	// Each step is a straight run (sharp corners, now and then flat) plus the lively jitter the line always had.
	private static final double JITTER = 0.6;
	private static final double RUN_SIZE = 0.75;

	private final Random random;
	private final double[] points = new double[POINTS];
	private int head;
	private int count;
	private double anchor;
	private long volume;

	private DriftProfile profile;
	private int profileStepsLeft;
	private boolean switchPending;
	// While easing into a new profile the anchor glides to this goal instead of jumping.
	private double anchorGoal;
	private int transitionLeft;
	// Per-step anchor change (CLIMBER up, FADER down) and where the climb ends.
	private double trend;
	private double climbTarget;
	private int spikeLeft;
	private double spikeBase;
	private int lowDwell;
	private int highDwell;
	private int liftSteps;
	// Like a real market chart the line moves in straight runs with sharp corners and flat stretches, not constant jitter.
	private double slope;
	private int slopeLeft;

	MarketGraph(Random random)
	{
		this.random = random;
	}

	DriftProfile profile()
	{
		return profile;
	}

	/**
	 * Opens a fresh market with a believable-looking price history already on the chart. The drift profile is NOT
	 * re-rolled here: it runs on its own clock and carries over when the player switches boss, raid or skill.
	 */
	void reset()
	{
		if (profile == null)
		{
			beginProfile(DriftProfile.roll(random), false);
		}
		rebuildHistory();
	}

	/** Starts over with a specific profile (and a fresh clock); used by tests. */
	void reset(DriftProfile forced)
	{
		beginProfile(forced, false);
		rebuildHistory();
	}

	private void rebuildHistory()
	{
		head = 0;
		count = 0;
		lowDwell = 0;
		highDwell = 0;
		liftSteps = 0;
		slopeLeft = 0;
		volume = 50_000L + (long) (random.nextDouble() * 5_000_000L);
		push(clamp(anchor + random.nextGaussian() * 6, anchor));
		for (int i = 1; i < POINTS; i++)
		{
			step(false);
		}
	}

	/** Switches to a profile. Graceful switches glide the anchor over to the new profile's level. */
	private void beginProfile(DriftProfile next, boolean graceful)
	{
		profile = next;
		profileStepsLeft = PROFILE_MIN_STEPS + random.nextInt(PROFILE_EXTRA_STEPS + 1);
		switchPending = false;
		trend = 0;
		spikeLeft = 0;
		if (profile == DriftProfile.CLIMBER)
		{
			trend = 0.45 + random.nextDouble() * 0.35;
			climbTarget = 84 + random.nextDouble() * 6;
		}
		else if (profile == DriftProfile.FADER)
		{
			trend = -(0.25 + random.nextDouble() * 0.15);
		}
		double start = profile.rollAnchor(random);
		if (graceful)
		{
			anchorGoal = start;
			transitionLeft = TRANSITION_STEPS;
		}
		else
		{
			anchor = start;
			transitionLeft = 0;
		}
	}

	void step()
	{
		step(true);
	}

	/** A live step also runs the profile clock and its dynamics; history prefill steps only wander. */
	private void step(boolean live)
	{
		double last = current();
		if (live)
		{
			if (profileStepsLeft > 0 && --profileStepsLeft == 0)
			{
				// The clock has run out, but the switch waits for the current kill or action to finish.
				switchPending = true;
			}
			advanceProfile();
		}
		double drift = (anchor - last) * 0.08;
		// A climb out of a low patch replaces the current run, so a downward run can't fight it.
		double move = (liftSteps > 0 ? 0 : nextRun()) + random.nextGaussian() * profile.noise * JITTER;
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
		if (live && profile.rerollChance > 0 && random.nextDouble() < profile.rerollChance)
		{
			anchor = profile.rollAnchor(random);
		}
	}

	/** The per-step move of the current straight run; when it ends, a new one starts (flat about a quarter of the time). */
	private double nextRun()
	{
		if (slopeLeft <= 0)
		{
			slopeLeft = 2 + random.nextInt(4);
			slope = random.nextDouble() < 0.12 ? 0 : random.nextGaussian() * profile.noise * RUN_SIZE;
		}
		slopeLeft--;
		return slope;
	}

	/**
	 * Call when one unit of the activity finishes (a kill, a loot, a skilling action). If the profile's 10-15 minutes
	 * are up, this is the moment it eases into the next one, so a profile never changes mid-attempt.
	 */
	void completeActivity()
	{
		if (switchPending)
		{
			beginProfile(nextProfile(), true);
		}
	}

	/** True once a profile's time is up and it is only waiting for the next finished kill or action. */
	boolean switchPending()
	{
		return switchPending;
	}

	/** A different profile from the current one, by the usual odds. */
	private DriftProfile nextProfile()
	{
		DriftProfile next = DriftProfile.roll(random);
		for (int tries = 0; next == profile && tries < 8; tries++)
		{
			next = DriftProfile.roll(random);
		}
		return next;
	}

	/** Moves the anchor the line is pulled toward, for the profiles that change over time. */
	private void advanceProfile()
	{
		if (transitionLeft > 0)
		{
			anchor += (anchorGoal - anchor) / transitionLeft;
			transitionLeft--;
			return;
		}
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
