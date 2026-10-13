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
		sound.play(100, failed::countDown);
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
		new CustomSound(executor, new File(temp.getRoot(), "nothing-here")).play(0, failed::countDown);
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
	public void volumeOnlyEverTurnsTheFileDown()
	{
		assertEquals(0f, CustomSound.gainDb(100), 0.0001f);
		assertEquals(-6.02f, CustomSound.gainDb(50), 0.01f);
		assertEquals(0f, CustomSound.gainDb(250), 0.0001f);
		assertTrue(CustomSound.gainDb(0) <= -80f);
	}
}
