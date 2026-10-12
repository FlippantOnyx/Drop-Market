package com.dropmarket;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.runelite.api.SoundEffectID;
import org.junit.Test;

public class WinSoundTest
{
	@Test
	public void offPlaysNothingAndOthersHaveTheirIds()
	{
		assertEquals(0, WinSound.OFF.notes().length);
		assertArrayEquals(new int[]{6765}, WinSound.COLLECTION_LOG.notes());
		assertArrayEquals(new int[]{2655}, WinSound.GEM_ALERT.notes());
		assertArrayEquals(new int[]{SoundEffectID.GE_COIN_TINKLE}, WinSound.COIN_TINKLE.notes());
	}

	@Test
	public void geChimeIsTheGrandExchangeSoundsInARow()
	{
		assertArrayEquals(new int[]{SoundEffectID.GE_ADD_OFFER_DINGALING, SoundEffectID.GE_COIN_TINKLE,
			SoundEffectID.GE_COIN_TINKLE}, WinSound.GE_CHIME.notes());
	}

	@Test
	public void bellRingsUpAndDownSixTimes()
	{
		int[] bell = WinSound.BELL_DING.notes();
		assertEquals(6, bell.length);
		for (int i = 0; i < bell.length; i++)
		{
			assertEquals(i % 2 == 0 ? SoundEffectID.TOWN_CRIER_BELL_DING : SoundEffectID.TOWN_CRIER_BELL_DONG, bell[i]);
		}
	}

	@Test
	public void volumeMapsTheSettingOntoTheGamesRange()
	{
		assertEquals(0, DropMarketPlugin.soundVolume(0));
		assertEquals(127, DropMarketPlugin.soundVolume(100));
		assertEquals(64, DropMarketPlugin.soundVolume(50));
		assertEquals(127, DropMarketPlugin.soundVolume(500));
		assertEquals(0, DropMarketPlugin.soundVolume(-5));
	}

	@Test
	public void defaultIsFullVolumeAndOff()
	{
		DropMarketConfig config = new DropMarketConfig()
		{
		};
		assertEquals(100, config.winSoundVolume());
		assertTrue(config.winSound() == WinSound.OFF);
	}
}
