package com.dropmarket;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(DropMarketConfig.GROUP)
public interface DropMarketConfig extends Config
{
	String GROUP = "dropmarket";
	Color KALSHI_GREEN = new Color(0x00D991);

	@ConfigItem(
		keyName = "graphColor",
		name = "Graph color",
		description = "Color of the odds line",
		position = 1
	)
	default Color graphColor()
	{
		return KALSHI_GREEN;
	}

	@ConfigItem(
		keyName = "priorityItems",
		name = "Priority drops",
		description = "One per line as 'Source: Item' to bet on that item instead of the most expensive drop.<br>"
			+ "e.g. 'Chambers of Xeric: Dexterous prayer scroll' or 'Zulrah: Serpentine visage'.<br>"
			+ "Raids also accept CoX / ToB / ToA.",
		position = 2
	)
	default String priorityItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "minimumDropValue",
		name = "Minimum drop value",
		description = "Monsters whose best tradeable drop is worth less than this show LIVE ODDS,<br>"
			+ "unless they drop an untradeable collection log item you haven't unlocked yet",
		position = 3
	)
	default int minimumDropValue()
	{
		return 250_000;
	}

	@ConfigItem(
		keyName = "runeProfileSync",
		name = "RuneProfile collection log",
		description = "Read your collection log from RuneProfile (requires the RuneProfile plugin to have synced it).<br>"
			+ "Without this, unlocks are learned by browsing your collection log in-game.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 4
	)
	default boolean runeProfileSync()
	{
		return false;
	}

	@ConfigItem(
		keyName = "wikiLookup",
		name = "OSRS Wiki drop tables",
		description = "Look up drop tables on the OSRS Wiki for monsters that aren't built into the plugin",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 5
	)
	default boolean wikiLookup()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showKalshiBar",
		name = "Show Kalshi bar",
		description = "Show a Kalshi-branded bar across the top of the overlay",
		position = 6
	)
	default boolean showKalshiBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPolymarketBar",
		name = "Show Polymarket bar",
		description = "Show a Polymarket-branded bar across the top of the overlay",
		position = 7
	)
	default boolean showPolymarketBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDualLine",
		name = "Show dual line",
		description = "Display a second independent price line below the main line",
		position = 8
	)
	default boolean showDualLine()
	{
		return true;
	}

	@ConfigItem(
		keyName = "kalshiBarText",
		name = "Kalshi bar text",
		description = "Custom text to display in the Kalshi bar (leave blank for no text)",
		position = 9
	)
	default String kalshiBarText()
	{
		return "";
	}

	@ConfigItem(
		keyName = "polymarketBarText",
		name = "Polymarket bar text",
		description = "Custom text to display in the Polymarket bar (leave blank for no text)",
		position = 10
	)
	default String polymarketBarText()
	{
		return "";
	}
}
