package com.dropmarket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.util.Filepath;

/**
 * Plays the player's own sound files on a win. The files live in the plugin's own data folder (the Filepath RuneLite
 * hands the plugin, which can't reach anywhere else) as plain WAVs under fixed names, one per kind of win with win.wav
 * as the fallback for all of them. They are played with RuneLite's AudioPlayer on RuneLite's executor, never the client
 * thread. Nothing is bundled, downloaded or deleted, and the plugin never opens the folder for the player: the how-to
 * and a chat message give them its path.
 */
@Slf4j
@Singleton
class CustomSound
{
	static final String FILE_NAME = SoundEvent.WIN.fileName();
	static final String README_NAME = "HOW-TO.txt";
	// A win sound is a second or two; this stops a huge file being played by mistake.
	private static final long MAX_BYTES = 8L * 1024 * 1024;

	private static final String README_TITLE = "Prediction Market Drops - custom win sounds";

	private static final String README = String.join(System.lineSeparator(),
		README_TITLE,
		"",
		"1. Find or make a short sound as a WAV file (.wav). MP3 and OGG files won't work, and renaming one doesn't",
		"   change what it is: convert it, for example in Audacity with File > Export > Export as WAV.",
		"2. Name it exactly " + SoundEvent.WIN.fileName() + " and put it in this folder (replace the file if one is there).",
		"3. In the plugin settings choose Win Sound > Custom file, then tick Test Win Sound to hear it.",
		"",
		"Different sounds for different wins (all optional, anything missing just uses " + SoundEvent.WIN.fileName() + "):",
		"  " + SoundEvent.COLLECTION_LOG.fileName() + "   a new collection log item",
		"  " + SoundEvent.PET.fileName() + "   a pet drop",
		"",
		"Tips: keep sounds short. The Win Sound Volume setting can only turn a file down, so make it as loud as you",
		"want it. Changes are picked up the next time a sound plays. To go back to the built-in sound, choose another",
		"Win Sound. If a file is missing or can't be read, the Grand Exchange chime plays instead.",
		"Nothing in this folder is ever deleted, and folders inside it are ignored, so you can keep spare sounds there.",
		"");

	private final ScheduledExecutorService executor;
	private final AudioPlayer audioPlayer;
	// The plugin's data folder, handed over by the plugin when it starts; null until then (or if RuneLite couldn't make it).
	private volatile Filepath folder;

	@Inject
	CustomSound(ScheduledExecutorService executor, AudioPlayer audioPlayer)
	{
		this.executor = executor;
		this.audioPlayer = audioPlayer;
	}

	void setFolder(Filepath folder)
	{
		this.folder = folder;
	}

	/** Where the sound files go, for showing to the player; empty if the folder isn't available. */
	String folderPath()
	{
		Filepath current = folder;
		return current == null ? "" : current.toString();
	}

	/** win.wav, the sound every win falls back to; null if there is no folder. */
	Filepath file()
	{
		Filepath current = folder;
		return current == null ? null : current.joinSegment(FILE_NAME);
	}

	/** The file for this kind of win if the player made one, else win.wav (which may not exist); null with no folder. */
	Filepath fileFor(SoundEvent event)
	{
		Filepath current = folder;
		if (current == null)
		{
			return null;
		}
		if (event != null && event != SoundEvent.WIN)
		{
			Filepath specific = current.joinSegment(event.fileName());
			if (specific.isFile())
			{
				return specific;
			}
		}
		return current.joinSegment(FILE_NAME);
	}

	/** Makes sure the folder is there with the how-to in it, so the player has somewhere obvious to drop the files. */
	void prepareFolder()
	{
		Filepath current = folder;
		if (current == null)
		{
			return;
		}
		try
		{
			current.createDirectories();
			Filepath readme = current.joinSegment(README_NAME);
			boolean write = !readme.exists();
			if (!write)
			{
				// An older copy of our own how-to is refreshed; one the player has edited or replaced is left alone.
				try (InputStream in = readme.openInputStream())
				{
					String existing = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					write = existing.startsWith(README_TITLE) && !existing.equals(README);
				}
			}
			if (write)
			{
				readme.write(README);
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
	 * Plays the sound for this kind of win in the background. If there is no usable file (missing, too big, not a WAV,
	 * no audio device) onFailure runs, on the background thread, so the caller can fall back to a built-in sound.
	 */
	void play(SoundEvent event, int percent, Runnable onFailure)
	{
		if (percent <= 0)
		{
			return;
		}
		try
		{
			executor.submit(() -> playNow(fileFor(event), percent, onFailure));
		}
		catch (RuntimeException e)
		{
			log.debug("Could not start the custom sound", e);
			fail(onFailure);
		}
	}

	private void playNow(Filepath file, int percent, Runnable onFailure)
	{
		try
		{
			if (file == null || !file.isFile() || file.size() > MAX_BYTES)
			{
				fail(onFailure);
				return;
			}
			audioPlayer.play(file, gainDb(percent));
		}
		catch (Exception | LinkageError e)
		{
			// Includes the unsupported-format and no-audio-device errors the player reports.
			log.debug("Custom win sound failed", e);
			fail(onFailure);
		}
	}

	private void fail(Runnable onFailure)
	{
		try
		{
			onFailure.run();
		}
		catch (Exception | LinkageError e)
		{
			log.debug("Custom sound fallback failed", e);
		}
	}
}
