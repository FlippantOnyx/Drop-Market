package com.dropmarket;

/** Where line 2's big odds number goes in the Full and Compact layouts. */
enum SecondNumberPlacement
{
	AFTER_CHANGE("After line 1's change"),
	BESIDE_FIRST("Right of line 1's number");

	private final String label;

	SecondNumberPlacement(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
