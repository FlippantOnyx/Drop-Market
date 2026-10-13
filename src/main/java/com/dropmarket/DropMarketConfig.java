package com.dropmarket;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

// Keep every keyName stable: it is what a player's saved settings are stored under.
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

	// Sections run top to bottom in order of importance.
	String SECTION_DATA = "dataSources";
	String SECTION_DISPLAY = "display";
	String SECTION_DROPS = "dropLists";
	String SECTION_WINS = "winRules";
	String SECTION_CELEBRATION = "celebration";
	String SECTION_LINE1 = "graphLine1";
	String SECTION_LINE2 = "graphLine2";
	String SECTION_BAR1 = "headerBar1";
	String SECTION_BAR2 = "headerBar2";

	// ---- Data sources (opt-in, third party) ---------------------------------------------------------------------

	@ConfigSection(
		name = "Data Sources",
		description = "Optional lookups that contact third-party servers",
		position = 1,
		closedByDefault = true
	)
	String dataSourcesSection = SECTION_DATA;

	@ConfigItem(
		keyName = "runeProfileSync",
		name = "RuneProfile Collection Log",
		description = "Read your collection log from RuneProfile (requires the RuneProfile plugin to have synced it).<br>"
			+ "Without this, unlocks are learned by browsing your collection log in-game.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		section = SECTION_DATA,
		position = 1
	)
	default boolean runeProfileSync()
	{
		return false;
	}

	@ConfigItem(
		keyName = "wikiLookup",
		name = "OSRS Wiki Drop Tables",
		description = "Live-look up drop tables on the OSRS Wiki for monsters missing from the tables bundled in the plugin<br>"
			+ "(most monsters are bundled and need no lookup)",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		section = SECTION_DATA,
		position = 2
	)
	default boolean wikiLookup()
	{
		return false;
	}

	// ---- Display ----------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Display",
		description = "How big the overlay is",
		position = 2,
		closedByDefault = true
	)
	String displaySection = SECTION_DISPLAY;

	@ConfigItem(
		keyName = "overlayLayout",
		name = "Overlay Layout",
		description = "Full: the whole card with chart, volume and axis.<br>"
			+ "Compact: a short card with the title, odds and a sparkline.<br>"
			+ "Ticker: a single line, like a stock ticker (scrolls long titles; header bars are hidden).",
		section = SECTION_DISPLAY,
		position = 1
	)
	default OverlayLayout overlayLayout()
	{
		return OverlayLayout.FULL;
	}

	// ---- Drop lists ---------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Drop Lists",
		description = "Drops to always bet on, and drops to never count",
		position = 3,
		closedByDefault = true
	)
	String dropListsSection = SECTION_DROPS;

	@ConfigItem(
		keyName = "priorityItems",
		name = "Priority Drops",
		description = "One per line as 'Source: Item' to bet on that item instead of the most expensive drop.<br>"
			+ "A priority drop always counts as a win when you get it, whatever it's worth.<br>"
			+ "e.g. 'Chambers of Xeric: Dexterous prayer scroll' or 'Zulrah: Serpentine visage'.<br>"
			+ "Raids also accept CoX / ToB / ToA. Multiple lines for the same source are rolled between evenly.",
		section = SECTION_DROPS,
		position = 1
	)
	default String priorityDrops()
	{
		return "";
	}

	@ConfigItem(
		keyName = "blacklistItems",
		name = "Blacklisted Drops",
		description = "One per line as 'Source: Item' to never bet on or celebrate that item.<br>"
			+ "e.g. 'Zulrah: Tanzanite fang' or 'CoX: Dexterous prayer scroll'.<br>"
			+ "Raids also accept CoX / ToB / ToA. Trumps everything, including priority drops and Resolve Win on Every Drop.",
		section = SECTION_DROPS,
		position = 2
	)
	default String blacklistedDrops()
	{
		return "";
	}

	// ---- Market value -------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Market Value",
		description = "Which monsters get a market, and what a drop is worth to count as a win",
		position = 4,
		closedByDefault = true
	)
	String winRulesSection = SECTION_WINS;

	@ConfigItem(
		keyName = "valueUnit",
		name = "Value Units",
		description = "The denomination the two values below are entered in (k = thousands, m = millions, b = billions)",
		section = SECTION_WINS,
		position = 1
	)
	default ValueUnit valueUnit()
	{
		return ValueUnit.K;
	}

	@ConfigItem(
		keyName = "marketMinimumAmount",
		name = "Market Minimum Drop Value",
		description = "Which monsters get a market (what the market bets on), in the units chosen above: if a monster's<br>"
			+ "best tradeable drop is worth less than this it shows LIVE ODDS, unless it drops a collection log item<br>"
			+ "you haven't unlocked. Not the same as Win Minimum Drop Value, which decides what counts as a win.",
		section = SECTION_WINS,
		position = 2
	)
	@Range(min = 0, max = 100_000)
	default int marketMinimumAmount()
	{
		return 250;
	}

	@ConfigItem(
		keyName = "winMinimumAmount",
		name = "Win Minimum Drop Value",
		description = "Minimum value for a success hit on the odds (what counts as a win, once a market is showing), in<br>"
			+ "the units chosen above: a drop worth less than this won't resolve the market or flash.<br>"
			+ "A collection log item new to your account, or one with no GE price, still counts.<br>"
			+ "Not the same as Market Minimum Drop Value, which decides what the market bets on.",
		section = SECTION_WINS,
		position = 3
	)
	@Range(min = 0, max = 100_000)
	default int winMinimumAmount()
	{
		return 2_000;
	}

	// ---- Celebration --------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Celebration",
		description = "What counts as a win, and how a win looks and how long it lasts",
		position = 5,
		closedByDefault = true
	)
	String celebrationSection = SECTION_CELEBRATION;

	@ConfigItem(
		keyName = "popOffOnEveryDrop",
		name = "Resolve Win on Every Drop",
		description = "Treat every drop you receive as a win, ignoring Win Minimum Drop Value.<br>"
			+ "Blacklisted drops still never count, and priority drops always do.",
		section = SECTION_CELEBRATION,
		position = 1
	)
	default boolean resolveWinOnEveryDrop()
	{
		return false;
	}

	@ConfigItem(
		keyName = "flashOnDrop",
		name = "Flash Purple on Win",
		description = "Flash the overlay purple when a drop hits (both lines too, when the second line is shown)",
		section = SECTION_CELEBRATION,
		position = 2
	)
	default boolean flashOnWin()
	{
		return true;
	}

	@ConfigItem(
		keyName = "winSound",
		name = "Win Sound",
		description = "A built-in game sound to play on a win.<br>"
			+ "Custom file plays your own sound: choose it, tick Open Sound Folder, put a WAV file named win.wav in the<br>"
			+ "folder that opens (a HOW-TO.txt is there too), then tick Test Win Sound to hear it.",
		section = SECTION_CELEBRATION,
		position = 3
	)
	default WinSound winSound()
	{
		return WinSound.GE_CHIME;
	}

	@ConfigItem(
		keyName = "winSoundVolume",
		name = "Win Sound Volume",
		description = "How loud the win sound plays, from 0 to 100. This is its own volume, so it is not affected by<br>"
			+ "your in-game sound effect slider (but muting the game's sound effects still silences it).",
		section = SECTION_CELEBRATION,
		position = 4
	)
	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	default int winSoundVolume()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "testWinSound",
		name = "Test Win Sound",
		description = "Tick to play the chosen win sound now (the box un-ticks itself)",
		section = SECTION_CELEBRATION,
		position = 5
	)
	default boolean testWinSound()
	{
		return false;
	}

	@ConfigItem(
		keyName = "openSoundFolder",
		name = "Open Sound Folder",
		description = "Tick to open the folder where your custom win.wav goes (the box un-ticks itself)",
		section = SECTION_CELEBRATION,
		position = 6
	)
	default boolean openSoundFolder()
	{
		return false;
	}

	@ConfigItem(
		keyName = "celebrationSeconds",
		name = "Celebration Seconds",
		description = "How long a win stays on screen before the overlay goes back to normal",
		section = SECTION_CELEBRATION,
		position = 7
	)
	@Range(min = 5, max = 120)
	@Units(Units.SECONDS)
	default int celebrationSeconds()
	{
		return 15;
	}

	// ---- Graph line 1 -------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Graph Line 1",
		description = "Color of the main odds line and its change indicator",
		position = 6,
		closedByDefault = true
	)
	String graphLine1Section = SECTION_LINE1;

	@ConfigItem(
		keyName = "graphColor",
		name = "Line 1 Color",
		description = "Color of the main odds line",
		section = SECTION_LINE1,
		position = 1
	)
	default Color line1Color()
	{
		return DEFAULT_LINE_COLOR;
	}

	@ConfigItem(
		keyName = "negativeDeltaColor1",
		name = "Line 1 Falling Color",
		description = "Color of line 1's change indicator when it drops",
		section = SECTION_LINE1,
		position = 2
	)
	default Color line1FallingColor()
	{
		return DEFAULT_NEGATIVE_DELTA_COLOR;
	}

	// ---- Graph line 2 -------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Graph Line 2",
		description = "An optional second odds line, its number and its change indicator",
		position = 7,
		closedByDefault = true
	)
	String graphLine2Section = SECTION_LINE2;

	@ConfigItem(
		keyName = "showDualLine",
		name = "Show Second Line",
		description = "Display a second independent odds line alongside the main line",
		section = SECTION_LINE2,
		position = 1
	)
	default boolean showSecondLine()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showSecondNumber",
		name = "Show Second Odds Number",
		description = "Show line 2's big odds number along with its change (hidden if the box is too narrow).<br>"
			+ "Off: only line 2's change is shown, after line 1's.",
		section = SECTION_LINE2,
		position = 2
	)
	default boolean showSecondNumber()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBothDeltas",
		name = "Show Both Delta Numbers",
		description = "Show the change next to both odds numbers.<br>"
			+ "Off: only line 1's change is shown, and line 2 has just its line and number.",
		section = SECTION_LINE2,
		position = 3
	)
	default boolean showBothDeltas()
	{
		return true;
	}

	@ConfigItem(
		keyName = "line2Color",
		name = "Line 2 Color",
		description = "Color of the second, independent odds line",
		section = SECTION_LINE2,
		position = 4
	)
	default Color line2Color()
	{
		return DEFAULT_LINE2_COLOR;
	}

	@ConfigItem(
		keyName = "negativeDeltaColor2",
		name = "Line 2 Falling Color",
		description = "Color of line 2's change indicator when it drops",
		section = SECTION_LINE2,
		position = 5
	)
	default Color line2FallingColor()
	{
		return DEFAULT_NEGATIVE_DELTA_COLOR;
	}

	// ---- Header bar 1 -------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Header Bar 1",
		description = "An optional colored bar across the top of the overlay",
		position = 8,
		closedByDefault = true
	)
	String headerBar1Section = SECTION_BAR1;

	@ConfigItem(
		keyName = "showBar1",
		name = "Show Bar 1",
		description = "Show the top colored bar across the top of the overlay",
		section = SECTION_BAR1,
		position = 1
	)
	default boolean showBar1()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bar1Color",
		name = "Bar 1 Color",
		description = "Color of the top bar",
		section = SECTION_BAR1,
		position = 2
	)
	default Color bar1Color()
	{
		return DEFAULT_BAR1_COLOR;
	}

	@ConfigItem(
		keyName = "bar1Text",
		name = "Bar 1 Text",
		description = "Custom text to display in the top bar (leave blank for no text)",
		section = SECTION_BAR1,
		position = 3
	)
	default String bar1Text()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bar1TextColor",
		name = "Bar 1 Text Color",
		description = "Color of the text displayed in the top bar",
		section = SECTION_BAR1,
		position = 4
	)
	default Color bar1TextColor()
	{
		return Color.BLACK;
	}

	// ---- Header bar 2 -------------------------------------------------------------------------------------------

	@ConfigSection(
		name = "Header Bar 2",
		description = "A second optional colored bar under the first",
		position = 9,
		closedByDefault = true
	)
	String headerBar2Section = SECTION_BAR2;

	@ConfigItem(
		keyName = "showBar2",
		name = "Show Bar 2",
		description = "Show the second colored bar across the top of the overlay",
		section = SECTION_BAR2,
		position = 1
	)
	default boolean showBar2()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bar2Color",
		name = "Bar 2 Color",
		description = "Color of the second bar",
		section = SECTION_BAR2,
		position = 2
	)
	default Color bar2Color()
	{
		return DEFAULT_BAR2_COLOR;
	}

	@ConfigItem(
		keyName = "bar2Text",
		name = "Bar 2 Text",
		description = "Custom text to display in the second bar (leave blank for no text)",
		section = SECTION_BAR2,
		position = 3
	)
	default String bar2Text()
	{
		return "";
	}

	@ConfigItem(
		keyName = "bar2TextColor",
		name = "Bar 2 Text Color",
		description = "Color of the text displayed in the second bar",
		section = SECTION_BAR2,
		position = 4
	)
	default Color bar2TextColor()
	{
		return Color.WHITE;
	}
}
