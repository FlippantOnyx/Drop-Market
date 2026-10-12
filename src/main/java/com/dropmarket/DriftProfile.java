package com.dropmarket;

import java.util.Random;

/**
 * How a market's odds tend to wander. STEADY is the everyday movement; the rest are rarer flavors. The numbers are
 * the anchor range the line is pulled toward, the noise, occasional jumps, and a soft ceiling (CLIMBER may go higher).
 */
enum DriftProfile
{
	// label, weight, anchor min, anchor span, noise, jump chance, jump min, jump span, anchor re-roll chance, ceiling
	STEADY("Steady", 55, 10, 46, 2.6, 0.07, 6, 10, 0.03, 82),
	// Creeps upward toward ~85-90 as the raid or boss fight nears its end, then falls back.
	CLIMBER("Climber", 15, 8, 20, 2.4, 0.04, 5, 8, 0.0, 91),
	CHOPPY("Choppy", 10, 12, 43, 4.2, 0.14, 8, 10, 0.06, 82),
	// Quiet and low-ish, barely moves.
	SLEEPER("Sleeper", 8, 10, 20, 1.2, 0.02, 4, 6, 0.01, 70),
	// Sits low, then now and then pops up for a few steps before settling back.
	SPIKE("Spike", 7, 10, 15, 2.0, 0.0, 0, 0, 0.0, 82),
	// Starts high and bleeds away.
	FADER("Fader", 5, 45, 20, 2.4, 0.04, 5, 8, 0.0, 82);

	final String label;
	final int weight;
	final double anchorMin;
	final double anchorSpan;
	final double noise;
	final double jumpChance;
	final double jumpMin;
	final double jumpSpan;
	final double rerollChance;
	final double ceiling;

	DriftProfile(String label, int weight, double anchorMin, double anchorSpan, double noise, double jumpChance,
		double jumpMin, double jumpSpan, double rerollChance, double ceiling)
	{
		this.label = label;
		this.weight = weight;
		this.anchorMin = anchorMin;
		this.anchorSpan = anchorSpan;
		this.noise = noise;
		this.jumpChance = jumpChance;
		this.jumpMin = jumpMin;
		this.jumpSpan = jumpSpan;
		this.rerollChance = rerollChance;
		this.ceiling = ceiling;
	}

	double rollAnchor(Random random)
	{
		return anchorMin + random.nextDouble() * anchorSpan;
	}

	/** A random profile by weight: STEADY most of the time, CLIMBER uncommon, the rest occasionally. */
	static DriftProfile roll(Random random)
	{
		int total = 0;
		for (DriftProfile p : values())
		{
			total += p.weight;
		}
		int pick = random.nextInt(total);
		for (DriftProfile p : values())
		{
			pick -= p.weight;
			if (pick < 0)
			{
				return p;
			}
		}
		return STEADY;
	}
}
