package com.dropmarket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.util.Filepath;
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

	/** A custom sound pointed at a folder, the way RuneLite's getPluginDirectory would hand it over. */
	private CustomSound soundIn(File folder)
	{
		CustomSound sound = new CustomSound(executor, new AudioPlayer());
		sound.setFolder(Filepath.Unchecked.getRooted(folder.toPath()));
		return sound;
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
		assertTrue(fails(soundIn(new File(temp.getRoot(), "nothing-here"))));
	}

	@Test
	public void noFolderAtAllFallsBackToo() throws Exception
	{
		CustomSound sound = new CustomSound(executor, new AudioPlayer());
		assertEquals("", sound.folderPath());
		assertNull(sound.fileFor(SoundEvent.WIN));
		assertTrue(fails(sound));
		// Nothing to prepare, and nothing blows up.
		sound.prepareFolder();
	}

	@Test
	public void aFileThatIsNotAudioFallsBack() throws Exception
	{
		File folder = temp.newFolder("sounds");
		Files.write(new File(folder, CustomSound.FILE_NAME).toPath(), "this is not a wav".getBytes(StandardCharsets.UTF_8));
		assertTrue(fails(soundIn(folder)));
	}

	@Test
	public void silentVolumePlaysNothingAndDoesNotFallBack() throws Exception
	{
		CountDownLatch failed = new CountDownLatch(1);
		soundIn(new File(temp.getRoot(), "nothing-here")).play(SoundEvent.WIN, 0, failed::countDown);
		assertFalse(failed.await(300, TimeUnit.MILLISECONDS));
	}

	@Test
	public void preparingTheFolderLeavesAHowToAndKeepsAnExistingOne() throws Exception
	{
		File folder = new File(temp.getRoot(), "plugin-folder");
		CustomSound sound = soundIn(folder);
		sound.prepareFolder();
		File howTo = new File(folder, CustomSound.README_NAME);
		assertTrue(howTo.isFile());
		String text = new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8);
		assertTrue(text.contains(CustomSound.FILE_NAME));
		assertTrue(text.contains("WAV"));
		assertTrue(text.contains("collection-log.wav"));
		Files.write(howTo.toPath(), "mine".getBytes(StandardCharsets.UTF_8));
		sound.prepareFolder();
		assertEquals("mine", new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void anOldCopyOfOurHowToIsRefreshedButAPlayersOwnIsKept() throws Exception
	{
		File folder = temp.newFolder("howto");
		File howTo = new File(folder, CustomSound.README_NAME);
		Files.write(howTo.toPath(), "Prediction Market Drops - custom win sounds\nold wording".getBytes(StandardCharsets.UTF_8));
		soundIn(folder).prepareFolder();
		String refreshed = new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8);
		assertTrue(refreshed.contains("collection-log.wav"));
		Files.write(howTo.toPath(), "my own notes".getBytes(StandardCharsets.UTF_8));
		soundIn(folder).prepareFolder();
		assertEquals("my own notes", new String(Files.readAllBytes(howTo.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void eachKindOfWinUsesItsOwnFileThenFallsBackToWinWav() throws Exception
	{
		File folder = temp.newFolder("events");
		CustomSound sound = soundIn(folder);
		String win = sound.file().toString();
		// Nothing there at all: the fallback is win.wav, even though it doesn't exist (so playing falls back to the chime).
		assertEquals(win, sound.fileFor(SoundEvent.PET).toString());
		Files.write(new File(folder, "pet.wav").toPath(), new byte[]{1});
		assertTrue(sound.fileFor(SoundEvent.PET).toString().endsWith("pet.wav"));
		assertEquals(win, sound.fileFor(SoundEvent.COLLECTION_LOG).toString());
		assertEquals(win, sound.fileFor(SoundEvent.WIN).toString());
		assertEquals(win, sound.fileFor(null).toString());
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
