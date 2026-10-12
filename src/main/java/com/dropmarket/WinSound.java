package com.dropmarket;

import net.runelite.api.SoundEffectID;

/** Built-in game sounds that can play on a win (no audio files are bundled; volume follows the game's sound effects). */
public enum WinSound
{
	OFF("Off", -1),
	COIN_TINKLE("Coin tinkle", SoundEffectID.GE_COIN_TINKLE),
	OFFER_DING("Offer ding", SoundEffectID.GE_ADD_OFFER_DINGALING),
	COLLECT_BLOOP("Collect bloop", SoundEffectID.GE_COLLECT_BLOOP),
	BELL_DING("Town crier bell", SoundEffectID.TOWN_CRIER_BELL_DING);

	private final String label;
	private final int soundId;

	WinSound(String label, int soundId)
	{
		this.label = label;
		this.soundId = soundId;
	}

	int soundId()
	{
		return soundId;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
