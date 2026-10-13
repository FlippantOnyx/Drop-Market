package com.dropmarket;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.util.LinkBrowser;

/**
 * Plays the player's own sound files on a win, the way other RuneLite plugins do custom sounds: a folder for the plugin
 * inside the .runelite directory, plain WAV files the player drops in under fixed names (one per kind of win, with
 * win.wav as the fallback for all of them), played through Java's audio on RuneLite's own executor, never the client
 * thread. Nothing is bundled, downloaded or deleted; folders inside the sound folder are left alone for spare sounds.
 */
@Slf4j
@Singleton
class CustomSound
{
	static final String FOLDER_NAME = "prediction-market-drops";
	static final String FILE_NAME = SoundEvent.WIN.fileName();
	static final String README_NAME = "HOW-TO.txt";
	// A win sound is a second or two; this stops a huge file being read into memory by mistake.
	private static final long MAX_BYTES = 8L * 1024 * 1024;
	// PipeWire on Linux cuts off sounds shorter than about two seconds, so short ones are padded with silence.
	static final double MIN_SECONDS = 2.0;

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

	/** win.wav, the sound every win falls back to. */
	File file()
	{
		return new File(folder, FILE_NAME);
	}

	/** The file for this kind of win if the player made one, else win.wav (which may not exist). */
	File fileFor(SoundEvent event)
	{
		if (event != null && event != SoundEvent.WIN)
		{
			File specific = new File(folder, event.fileName());
			if (specific.isFile())
			{
				return specific;
			}
		}
		return file();
	}

	/** Makes sure the folder is there with the how-to in it, so the player has somewhere obvious to drop the files. */
	void prepareFolder()
	{
		try
		{
			Files.createDirectories(folder.toPath());
			File readme = new File(folder, README_NAME);
			boolean write = !readme.exists();
			if (!write)
			{
				// An older copy of our own how-to is refreshed; one the player has edited or replaced is left alone.
				String existing = new String(Files.readAllBytes(readme.toPath()), StandardCharsets.UTF_8);
				write = existing.startsWith(README_TITLE) && !existing.equals(README);
			}
			if (write)
			{
				Files.write(readme.toPath(), README.getBytes(StandardCharsets.UTF_8));
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not prepare the sound folder", e);
		}
	}

	/** Opens the sound folder in the system's file browser, creating it first. Never throws. */
	void openFolder()
	{
		prepareFolder();
		try
		{
			LinkBrowser.open(folder.getAbsolutePath());
		}
		catch (Exception | LinkageError e)
		{
			log.debug("Could not open the sound folder", e);
		}
	}

	/** Decibels of gain for a 0-100 volume: 100 is the file as it is, lower is quieter, 0 is silent. */
	static float gainDb(int percent)
	{
		int clamped = Math.max(0, Math.min(100, percent));
		return clamped == 0 ? -80f : (float) (20 * Math.log10(clamped / 100.0));
	}

	/**
	 * The audio with silence added on the end so it lasts at least minSeconds. Only uncompressed PCM can be padded this
	 * way; anything else, or a sound that is long enough already, comes back unchanged.
	 */
	static byte[] padWithSilence(byte[] data, AudioFormat format, double minSeconds)
	{
		int frameSize = format.getFrameSize();
		AudioFormat.Encoding encoding = format.getEncoding();
		boolean pcm = encoding == AudioFormat.Encoding.PCM_SIGNED || encoding == AudioFormat.Encoding.PCM_UNSIGNED;
		if (!pcm || frameSize <= 0 || format.getFrameRate() <= 0)
		{
			return data;
		}
		long wanted = (long) Math.ceil(format.getFrameRate() * minSeconds) * frameSize;
		if (wanted <= data.length || wanted > MAX_BYTES * 2)
		{
			return data;
		}
		byte[] padded = Arrays.copyOf(data, (int) wanted);
		if (encoding == AudioFormat.Encoding.PCM_UNSIGNED)
		{
			if (format.getSampleSizeInBits() != 8)
			{
				// Unsigned wider than 8 bits is vanishingly rare; leave it alone rather than guess the silence level.
				return data;
			}
			// 8-bit unsigned samples are silent at the midpoint, not at zero.
			Arrays.fill(padded, data.length, padded.length, (byte) 0x80);
		}
		return padded;
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

	private void playNow(File file, int percent, Runnable onFailure)
	{
		try
		{
			if (!file.isFile() || file.length() > MAX_BYTES)
			{
				fail(onFailure);
				return;
			}
			Clip clip = AudioSystem.getClip();
			clip.addLineListener(event ->
			{
				if (event.getType() == LineEvent.Type.STOP)
				{
					clip.close();
				}
			});
			try (AudioInputStream in = AudioSystem.getAudioInputStream(file))
			{
				AudioFormat format = in.getFormat();
				byte[] data = padWithSilence(readAll(in), format, MIN_SECONDS);
				clip.open(format, data, 0, data.length);
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
			fail(onFailure);
		}
	}

	private static byte[] readAll(AudioInputStream in) throws IOException
	{
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = in.read(buffer)) != -1)
		{
			out.write(buffer, 0, read);
			if (out.size() > MAX_BYTES)
			{
				throw new IOException("sound too large");
			}
		}
		return out.toByteArray();
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
