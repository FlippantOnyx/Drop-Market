package com.dropmarket;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(DropMarketConfig.GROUP)
public interface DropMarketConfig extends Config
{
	String GROUP = "dropmarket";
	Color DEFAULT_LINE_COLOR = new Color(0x00D991);
	Color DEFAULT_LINE2_COLOR = new Color(0x3D7BFF);
	Color DEFAULT_BAR1_COLOR = new Color(0x00D991);
	Color DEFAULT_BAR2_COLOR = new Color(0x1652F0);
	// Classic stock-ticker red for a negative move.
	Color DEFAULT_NEGATIVE_DELTA_COLOR = new Color(0xE5484D);

	@ConfigItem(
		keyName = "graphColor",
		name = "Line 1 Color",
		description = "Color of the main odds line",
		position = 1
	)
	default Color graphColor()
	{
		return DEFAULT_LINE_COLOR;
	}

	@ConfigItem(
		keyName = "line2Color",
		name = "Line 2 Color",
		description = "Color of the second, independent odds line",
		position = 2
	)
	default Color line2Color()
	{
		return DEFAULT_LINE2_COLOR;
	}

	@ConfigItem(
		keyName = "negativeDeltaColor1",
		name = "Line 1 Negative Delta Color",
		description = "Color of line 1's change indicator when it drops",
		position = 3
	)
	default Color negativeDeltaColor1()
	{
		return DEFAULT_NEGATIVE_DELTA_COLOR;
	}

	@ConfigItem(
		keyName = "negativeDeltaColor2",
		name = "Line 2 Negative Delta Color",
		description = "Color of line 2's change indicator when it drops",
		position = 4
	)
	default Color negativeDeltaColor2()
	{
		return DEFAULT_NEGATIVE_DELTA_COLOR;
	}

	@ConfigItem(
		keyName = "priorityItems",
		name = "Priority Drops",
		description = "One per line as 'Source: Item' to bet on that item instead of the most expensive drop.<br>"
			+ "e.g. 'Chambers of Xeric: Dexterous prayer scroll' or 'Zulrah: Serpentine visage'.<br>"
			+ "Raids also accept CoX / ToB / ToA. Multiple lines for the same source are rolled between evenly.",
		position = 5
	)
	default String priorityItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "blacklistItems",
		name = "Blacklisted Drops",
		description = "One per line as 'Source: Item' to never bet on or celebrate that item.<br>"
			+ "e.g. 'Zulrah: Tanzanite fang' or 'CoX: Dexterous prayer scroll'.<br>"
			+ "Raids also accept CoX / ToB / ToA. Takes precedence over priority drops.",
		position = 6
	)
	default String blacklistItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "valuableDropThreshold",
		name = "Valuable Drop Min Threshold",
		description = "Minimum value for a success hit on the odds: a tradeable drop worth less than this<br>"
			+ "won't resolve the market or flash. Untradeable collection log items always count.",
		position = 7
	)
	default int valuableDropThreshold()
	{
		return 2_000_000;
	}

	@ConfigItem(
		keyName = "minimumDropValue",
		name = "Lowest Value GP From Monster",
		description = "Monsters whose best tradeable drop is worth less than this show LIVE ODDS,<br>"
			+ "unless they drop an untradeable collection log item you haven't unlocked yet",
		position = 8
	)
	default int minimumDropValue()
	{
		return 250_000;
	}

	@ConfigItem(
		keyName = "runeProfileSync",
		name = "RuneProfile Collection Log",
		description = "Read your collection log from RuneProfile (requires the RuneProfile plugin to have synced it).<br>"
			+ "Without this, unlocks are learned by browsing your collection log in-game.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 9
	)
	default boolean runeProfileSync()
	{
		return false;
	}

	@ConfigItem(
		keyName = "wikiLookup",
		name = "OSRS Wiki Drop Tables",
		description = "Look up drop tables on the OSRS Wiki for monsters that aren't built into the plugin",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 10
	)
	default boolean wikiLookup()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showBar1",
		name = "Show Bar 1",
		description = "Show the top colored bar across the top of the overlay",
		position = 11
	)
	default boolean showBar1()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showBar2",
		name = "Show Bar 2",
		description = "Show the second colored bar across the top of the overlay",
		position = 12
	)
	default boolean showBar2()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDualLine",
		name = "Show Dual Line",
		description = "Display a second independent price line alongside the main line",
		position = 13
	)
	default boolean showDualLine()
	{
		return false;
	}

	@ConfigItem(
		keyName = "flashOnDrop",
		name = "Flash On Drop",
		description = "Flash the overlay purple when a drop hits",
		position = 14
	)
	default boolean flashOnDrop()
	{
		return true;
	}

	@ConfigItem(
		keyName = "popOffOnEveryDrop",
		name = "Pop Off on Every Drop",
		description = "Treat every drop you receive as a success, ignoring the valuable drop threshold and blacklist",
		position = 15
	)
	default boolean popOffOnEveryDrop()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bar1Color",
		name = "Bar 1 Color",
		description = "Color of the top bar",
		position = 16
	)
	default Color bar1Color()
	{
		return DEFAULT_BAR1_COLOR;
	}

	@ConfigItem(
		keyName = "bar2Color",
		name = "Bar 2 Color",
		description = "Color of the second bar",
		position = 17
	)
	default Color bar2Color()
	{
		return DEFAULT_BAR2_COLOR;
	}

	@ConfigItem(
		keyName = "bar1TextColor",
		name = "Bar 1 Text Color",
		description = "Color of the text displayed in the top bar",
		position = 18
	)
	default Color bar1TextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "bar2TextColor",
		name = "Bar 2 Text Color",
		description = "Color of the text displayed in the second bar",
		position = 19
	)
	default Color bar2TextColor()
	{
		return Color.BLACK;
	}

	@ConfigItem(
		keyName = "bar1Text",
		name = "Bar 1 Text",
		description = "Custom text to display in the top bar (leave blank for no text)",
		position = 20
	)
	default String bar1Text()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bar2Text",
		name = "Bar 2 Text",
		description = "Custom text to display in the second bar (leave blank for no text)",
		position = 21
	)
	default String bar2Text()
	{
		return "";
	}
}
