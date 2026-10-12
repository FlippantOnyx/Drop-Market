package com.dropmarket;

/** When a win's celebration (the flash, the "RESOLVED" state) ends. */
final class Celebration
{
	private Celebration()
	{
	}

	/**
	 * Over once its time is up, or as soon as you move to a different source. Staying on the same monster does NOT keep
	 * it going: killing the same boss over and over used to leave the celebration flashing indefinitely.
	 */
	static boolean isOver(int ticksSinceWin, int celebrationTicks, boolean differentSource)
	{
		return differentSource || ticksSinceWin >= celebrationTicks;
	}
}
