package com.dropmarket;

/** The denomination the two drop-value settings are entered in, so big values stay short enough for the box. */
public enum ValueUnit
{
	K("k (thousands)", 1_000L),
	M("m (millions)", 1_000_000L),
	B("b (billions)", 1_000_000_000L);

	private final String label;
	private final long gp;

	ValueUnit(String label, long gp)
	{
		this.label = label;
		this.gp = gp;
	}

	/** An amount in this denomination as gp; a missing unit counts as thousands, the default. */
	static long toGp(ValueUnit unit, int amount)
	{
		return Math.max(0, amount) * (unit == null ? K : unit).gp;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
