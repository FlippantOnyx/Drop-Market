package com.dropmarket;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/** Our settings that belong to one RuneScape account rather than the profile, read and written without ever throwing. */
@Slf4j
@Singleton
class ProfileStore
{
	private final ConfigManager configManager;

	@Inject
	ProfileStore(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/** The saved value, or null if there is none (or the account isn't loaded yet). */
	String get(String key)
	{
		try
		{
			return configManager.getRSProfileConfiguration(DropMarketConfig.GROUP, key);
		}
		catch (Exception e)
		{
			log.debug("Could not read {}", key, e);
			return null;
		}
	}

	/** A setting from the profile itself (not the RuneScape account), e.g. one an older version saved; null if unset. */
	String getSetting(String key)
	{
		try
		{
			return configManager.getConfiguration(DropMarketConfig.GROUP, key);
		}
		catch (Exception e)
		{
			log.debug("Could not read setting {}", key, e);
			return null;
		}
	}

	void setSetting(String key, String value)
	{
		try
		{
			configManager.setConfiguration(DropMarketConfig.GROUP, key, value);
		}
		catch (Exception e)
		{
			log.debug("Could not save setting {}", key, e);
		}
	}

	void set(String key, String value)
	{
		try
		{
			configManager.setRSProfileConfiguration(DropMarketConfig.GROUP, key, value);
		}
		catch (Exception e)
		{
			log.debug("Could not save {}", key, e);
		}
	}
}
