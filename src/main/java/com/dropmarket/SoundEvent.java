package com.dropmarket;

/**
 * The kinds of win that can have their own custom sound. Each is a file in the plugin's sound folder; a win with no file
 * of its own uses win.wav, the same way other plugins let a player swap sounds by file name.
 */
enum SoundEvent
{
	WIN("win.wav"),
	COLLECTION_LOG("collection-log.wav"),
	PET("pet.wav");

	private final String fileName;

	SoundEvent(String fileName)
	{
		this.fileName = fileName;
	}

	String fileName()
	{
		return fileName;
	}
}
