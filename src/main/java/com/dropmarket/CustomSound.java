package com.dropmarket;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Plays the player's own sound file on a win, the way other RuneLite plugins do custom sounds: a folder for the plugin
 * inside the .runelite directory, a plain WAV file the player drops in, played through Java's audio on RuneLite's own
 * executor (never the client thread). Nothing is bundled and nothing is downloaded.
 */
@Slf4j
@Singleton
class CustomSound
{
	static final String FOLDER_NAME = "prediction-market-drops";
	static final String FILE_NAME = "win.wav";
	static final String README_NAME = "HOW-TO.txt";
	// A win sound is a second or two; this stops a huge file being read into memory by mistake.
	private static final long MAX_BYTES = 8L * 1024 * 1024;

	private static final String README = String.join(System.lineSeparator(),
		"Prediction Market Drops - custom win sound",
		"",
		"1. Find or make a short sound as a WAV file (.wav). MP3 and OGG files won't work: convert them first,",
		"   for example by opening them in Audacity and choosing File > Export > Export as WAV.",
		"2. Name it exactly: " + FILE_NAME,
		"3. Put it in this folder, replacing any file that is already there.",
		"4. In the plugin settings choose Win Sound > Custom file, then tick Test Win Sound to hear it.",
		"",
		"Tips: keep it short (a few seconds). The Win Sound Volume setting can only turn it down, so make the file",
		"as loud as you want it to be. Changes to the file are picked up the next time it plays.",
		"If the file is missing or can't be read, the Grand Exchange chime plays instead.",
		"");

	private final ScheduledExecutorService executor;
	private final File folder;
	private Clip current;

	@Inject
	CustomSound(ScheduledExecutorService executor)
	{
		this(executor, new File(RuneLite.RUNELITE_DIR, FOLDER_NAME));
	}

	CustomSound(ScheduledExecutorService executor, File folder)
	{
		this.executor = executor;
		this.folder = folder;
	}

	File folder()
	{
		return folder;
	}

	File file()
	{
		return new File(folder, FILE_NAME);
	}

	/** Makes sure the folder is there with the how-to in it, so the player has somewhere obvious to drop the file. */
	void prepareFolder()
	{
		try
		{
			Files.createDirectories(folder.toPath());
			File readme = new File(folder, README_NAME);
			if (!readme.exists())
			{
				Files.write(readme.toPath(), README.getBytes(StandardCharsets.UTF_8));
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not prepare the sound folder", e);
		}
	}

	/** Decibels of gain for a 0-100 volume: 100 is the file as it is, lower is quieter, 0 is silent. */
	static float gainDb(int percent)
	{
		int clamped = Math.max(0, Math.min(100, percent));
		return clamped == 0 ? -80f : (float) (20 * Math.log10(clamped / 100.0));
	}

	/**
	 * Plays the file in the background. If there is no usable file (missing, too big, not a WAV, no audio device)
	 * onFailure runs, on the background thread, so the caller can fall back to a built-in sound.
	 */
	void play(int percent, Runnable onFailure)
	{
		if (percent <= 0)
		{
			return;
		}
		try
		{
			executor.submit(() -> playNow(percent, onFailure));
		}
		catch (RuntimeException e)
		{
			log.debug("Could not start the custom sound", e);
			onFailure.run();
		}
	}

	private void playNow(int percent, Runnable onFailure)
	{
		File file = file();
		try
		{
			if (!file.isFile() || file.length() > MAX_BYTES)
			{
				onFailure.run();
				return;
			}
			Clip clip = AudioSystem.getClip();
			try (AudioInputStream in = AudioSystem.getAudioInputStream(file))
			{
				clip.addLineListener(event ->
				{
					if (event.getType() == LineEvent.Type.STOP)
					{
						clip.close();
					}
				});
				clip.open(in);
			}
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), gainDb(percent))));
			}
			synchronized (this)
			{
				if (current != null)
				{
					current.close();
				}
				current = clip;
			}
			clip.start();
		}
		catch (Exception | LinkageError e)
		{
			log.debug("Custom win sound failed", e);
			onFailure.run();
		}
	}

	/** Cuts off a sound that is still playing (when the plugin is turned off). */
	synchronized void stop()
	{
		if (current != null)
		{
			current.close();
			current = null;
		}
	}
}
