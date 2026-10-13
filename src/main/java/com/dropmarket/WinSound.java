package com.dropmarket;

import net.runelite.api.SoundEffectID;

/**
 * Built-in game sounds that can play on a win (no audio files are bundled). A sound is one or more game sound effects
 * played in a row, which is how the town crier's bell rings up and down.
 */
public enum WinSound
{
	OFF("Off"),
	COIN_TINKLE("Coin tinkle", SoundEffectID.GE_COIN_TINKLE),
	OFFER_DING("Offer ding", SoundEffectID.GE_ADD_OFFER_DINGALING),
	// The Grand Exchange's own sound effects in a row (offer ding, then two coin tinkles) as the nearest thing to its
	// trade jingle, which is music and can't be played by a plugin.
	GE_CHIME("Grand Exchange chime",
		SoundEffectID.GE_ADD_OFFER_DINGALING, SoundEffectID.GE_COIN_TINKLE, SoundEffectID.GE_COIN_TINKLE),
	// "sotn_muspah_unique_loot_02", the sound of a unique drop.
	COLLECTION_LOG("Collection log", 6765),
	// "found_gem", the gem alert chime.
	GEM_ALERT("Gem alert", 2655),
	// The bell rung up and down in succession, six notes.
	BELL_DING("Town crier bell",
		SoundEffectID.TOWN_CRIER_BELL_DING, SoundEffectID.TOWN_CRIER_BELL_DONG,
		SoundEffectID.TOWN_CRIER_BELL_DING, SoundEffectID.TOWN_CRIER_BELL_DONG,
		SoundEffectID.TOWN_CRIER_BELL_DING, SoundEffectID.TOWN_CRIER_BELL_DONG),
	// The player's own win.wav in the plugin's sound folder (see CustomSound); it has no built-in notes.
	CUSTOM("Custom file");

	private final String label;
	private final int[] notes;

	WinSound(String label, int... notes)
	{
		this.label = label;
		this.notes = notes;
	}

	/** The sound effects to play, in order; empty for Off. */
	int[] notes()
	{
		return notes.clone();
	}

	@Override
	public String toString()
	{
		return label;
	}
}
