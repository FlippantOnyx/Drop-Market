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
	Color DEFAULT_BAR1_COLOR = new Color(0x1652F0);
	Color DEFAULT_BAR2_COLOR = new Color(0x00D991);
	// Classic stock-ticker red for a negative move.
	Color DEFAULT_NEGATIVE_DELTA_COLOR = new Color(0xE5484D);

	@ConfigItem(
		keyName = "graphColor",
		name = "Line 1 color",
		description = "Color of the main odds line",
		position = 1
	)
	default Color graphColor()
	{
		return DEFAULT_LINE_COLOR;
	}

	@ConfigItem(
		keyName = "line2Color",
		name = "Line 2 color",
		description = "Color of the second, independent odds line",
		position = 2
	)
	default Color line2Color()
	{
		return DEFAULT_LINE2_COLOR;
	}

	@ConfigItem(
		keyName = "negativeDeltaColor1",
		name = "Line 1 negative delta color",
		description = "Color of line 1's change indicator when it drops",
		position = 3
	)
	default Color negativeDeltaColor1()
	{
		return DEFAULT_NEGATIVE_DELTA_COLOR;
	}

	@ConfigItem(
		keyName = "negativeDeltaColor2",
		name = "Line 2 negative delta color",
		description = "Color of line 2's change indicator when it drops",
		position = 4
	)
	default Color negativeDeltaColor2()
	{
		return DEFAULT_NEGATIVE_DELTA_COLOR;
	}

	@ConfigItem(
		keyName = "priorityItems",
		name = "Priority drops",
		description = "One per line as 'Source: Item' to bet on that item instead of the most expensive drop.<br>"
			+ "e.g. 'Chambers of Xeric: Dexterous prayer scroll' or 'Zulrah: Serpentine visage'.<br>"
			+ "Raids also accept CoX / ToB / ToA.",
		position = 5
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
		position = 6
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
		position = 7
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
		position = 8
	)
	default boolean wikiLookup()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showBar1",
		name = "Show bar 1",
		description = "Show the top colored bar across the top of the overlay",
		position = 9
	)
	default boolean showBar1()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showBar2",
		name = "Show bar 2",
		description = "Show the second colored bar across the top of the overlay",
		position = 10
	)
	default boolean showBar2()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDualLine",
		name = "Show dual line",
		description = "Display a second independent price line alongside the main line",
		position = 11
	)
	default boolean showDualLine()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bar1Color",
		name = "Bar 1 color",
		description = "Color of the top bar",
		position = 12
	)
	default Color bar1Color()
	{
		return DEFAULT_BAR1_COLOR;
	}

	@ConfigItem(
		keyName = "bar2Color",
		name = "Bar 2 color",
		description = "Color of the second bar",
		position = 13
	)
	default Color bar2Color()
	{
		return DEFAULT_BAR2_COLOR;
	}

	@ConfigItem(
		keyName = "bar1TextColor",
		name = "Bar 1 text color",
		description = "Color of the text displayed in the top bar",
		position = 14
	)
	default Color bar1TextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "bar2TextColor",
		name = "Bar 2 text color",
		description = "Color of the text displayed in the second bar",
		position = 15
	)
	default Color bar2TextColor()
	{
		return Color.BLACK;
	}

	@ConfigItem(
		keyName = "bar1Text",
		name = "Bar 1 text",
		description = "Custom text to display in the top bar (leave blank for no text)",
		position = 16
	)
	default String bar1Text()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bar2Text",
		name = "Bar 2 text",
		description = "Custom text to display in the second bar (leave blank for no text)",
		position = 17
	)
	default String bar2Text()
	{
		return "";
	}
}
