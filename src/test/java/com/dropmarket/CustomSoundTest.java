package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CustomSoundTest
{
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@After
	public void tearDown()
	{
		executor.shutdownNow();
	}

	private boolean fails(CustomSound sound) throws Exception
	{
		CountDownLatch failed = new CountDownLatch(1);
		sound.play(SoundEvent.WIN, 100, failed::countDown);
		return failed.await(5, TimeUnit.SECONDS);
	}

	@Test
	public void aMissingFileFallsBack() throws Exception
	{
		assertTrue(fails(new CustomSound(executor, new File(temp.getRoot(), "nothing-here"))));
	}

	@Test
	public void aFileThatIsNotAudioFallsBack() throws Exception
	{
		File folder = temp.newFolder("sounds");
		Files.write(new File(folder, CustomSound.FILE_NAME).toPath(), "this is not a wav".getBytes(StandardCharsets.UTF_8));
		assertTrue(fails(new CustomSound(executor, folder)));
	}

	@Test
	public void silentVolumePlaysNothingAndDoesNotFallBack() throws Exception
	{
		CountDownLatch failed = new CountDownLatch(1);
		new CustomSound(executor, new File(temp.getRoot(), "nothing-here")).play(SoundEvent.WIN, 0, failed::countDown);
		assertFalse(failed.await(300, TimeUnit.MILLISECONDS));
	}

	@Test
	public void preparingTheFolderLeavesAHowToAndKeepsAnExistingOne() throws Exception
	{
		File folder = new File(temp.getRoot(), "plugin-folder");
		CustomSound sound = new CustomSound(executor, folder);
		sound.prepareFolder();
		File howTo = new File(folder, CustomSound.README_NAME);
		assertTrue(howTo.isFile());
		String text = new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8);
		assertTrue(text.contains(CustomSound.FILE_NAME));
		assertTrue(text.contains("WAV"));
		Files.write(howTo.toPath(), "mine".getBytes(StandardCharsets.UTF_8));
		sound.prepareFolder();
		assertEquals("mine", new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void eachKindOfWinUsesItsOwnFileThenFallsBackToWinWav() throws Exception
	{
		File folder = temp.newFolder("events");
		CustomSound sound = new CustomSound(executor, folder);
		File win = new File(folder, "win.wav");
		// Nothing there at all: the fallback is win.wav, even though it doesn't exist (so playing falls back to the chime).
		assertEquals(win, sound.fileFor(SoundEvent.PET));
		Files.write(new File(folder, "pet.wav").toPath(), new byte[]{1});
		assertEquals(new File(folder, "pet.wav"), sound.fileFor(SoundEvent.PET));
		assertEquals(win, sound.fileFor(SoundEvent.COLLECTION_LOG));
		assertEquals(win, sound.fileFor(SoundEvent.WIN));
		assertEquals(win, sound.fileFor(null));
	}

	@Test
	public void shortSoundsArePaddedWithSilenceAndLongOnesAreNot()
	{
		AudioFormat signed16 = new AudioFormat(8000f, 16, 1, true, false);
		byte[] tiny = new byte[100];
		java.util.Arrays.fill(tiny, (byte) 7);
		byte[] padded = CustomSound.padWithSilence(tiny, signed16, 2.0);
		// Two seconds of 8000 Hz 16-bit mono is 32000 bytes; the original is kept and the rest is silent.
		assertEquals(32_000, padded.length);
		assertEquals(7, padded[99]);
		assertEquals(0, padded[100]);
		assertEquals(0, padded[31_999]);
		byte[] longEnough = new byte[40_000];
		assertTrue(CustomSound.padWithSilence(longEnough, signed16, 2.0) == longEnough);

		// 8-bit unsigned is silent at 0x80, not 0.
		AudioFormat unsigned8 = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 8000f, 8, 1, 1, 8000f, false);
		byte[] pad8 = CustomSound.padWithSilence(new byte[10], unsigned8, 2.0);
		assertEquals(16_000, pad8.length);
		assertEquals((byte) 0x80, pad8[15_999]);

		// Compressed audio can't be padded this way and is returned as it was.
		AudioFormat ulaw = new AudioFormat(AudioFormat.Encoding.ULAW, 8000f, 8, 1, 1, 8000f, false);
		byte[] original = new byte[10];
		assertTrue(CustomSound.padWithSilence(original, ulaw, 2.0) == original);
	}

	@Test
	public void anOldCopyOfOurHowToIsRefreshedButAPlayersOwnIsKept() throws Exception
	{
		File folder = temp.newFolder("howto");
		File howTo = new File(folder, CustomSound.README_NAME);
		Files.write(howTo.toPath(), "Prediction Market Drops - custom win sounds\nold wording".getBytes(StandardCharsets.UTF_8));
		new CustomSound(executor, folder).prepareFolder();
		String refreshed = new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8);
		assertTrue(refreshed.contains("collection-log.wav"));
		Files.write(howTo.toPath(), "my own notes".getBytes(StandardCharsets.UTF_8));
		new CustomSound(executor, folder).prepareFolder();
		assertEquals("my own notes", new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void volumeOnlyEverTurnsTheFileDown()
	{
		assertEquals(0f, CustomSound.gainDb(100), 0.0001f);
		assertEquals(-6.02f, CustomSound.gainDb(50), 0.01f);
		assertEquals(0f, CustomSound.gainDb(250), 0.0001f);
		assertTrue(CustomSound.gainDb(0) <= -80f);
	}
}
