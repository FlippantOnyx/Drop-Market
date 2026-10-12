package com.dropmarket;

/** How much of the market the overlay shows: the full card, a two-row card, or a single-line ticker. */
enum OverlayLayout
{
	FULL("Full"),
	COMPACT("Compact"),
	TICKER("Ticker");

	private final String label;

	OverlayLayout(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
