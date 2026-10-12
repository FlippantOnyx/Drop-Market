# Changelog

## 1.3.0

- The odds lines are drawn rigid like a real market chart: flat holds, straight runs, sharp corners and sudden jumps instead of a smooth wiggle
- Long titles never get cut off: in Compact and Full a title that doesn't fit scrolls like the ticker (Full still wraps to two lines first), instead of "DROPS DRAGON"
- Compact with the second odds number shrinks the numbers like the ticker does, so the sparkline is visible again
- New "Show Both Delta Numbers" toggle (Graph Line 2); off shows only line 1's change
- Full card: the change indicators sit 1px higher
- "Pop Off on Every Drop" is now "Resolve Win on Every Drop" and lives in Celebration
- The two drop values are entered in k by default, with a Value Units setting (k, m, b); your old gp values are carried over
- Settings sections reordered by importance (Data Sources, Display, Drop Lists, Market Value, Celebration), with Graph Line 1/2 and Header Bar 1/2 split into their own sections

## 1.2.1

- Fix: the overlay crashed the client when the plugin was installed (the config used package-private types RuneLite couldn't read)
- Every event handler, the overlay, start/stop and the network callbacks now run behind a guard, so a bug in the plugin is logged once and skipped instead of reaching the client
- Item, price and per-account settings access go through small wrappers that never throw
- A test now runs the whole plugin through simulated sessions (normal, null-filled and throwing game state) and checks the structure rules

## 1.2.0

- Abyssal Sire's market is the Unsired; at the Font of Consumption with an Unsired you get an "UNSIRED DROPS X" market valued by the rewards you can still get
- Multi-part drops (Noxious halberd, Abyssal bludgeon) split the finished item's value across the parts you're missing and skip parts you already hold
- Fix: a win no longer keeps flashing while you keep killing the same monster; it ends after Celebration Seconds
- Repeat untradeable uniques (e.g. Xeric's talisman) no longer count as wins unless they are new to your log, or a pet (or a drop that crafts into something expensive, see below)
- Untradeable drops that craft into expensive items (vestiges, Araxyte fang, Mokhaiotl cloth...) are valued at the price of what they become, so Vardorvis counts the Ultor vestige as a 110m drop. Soulreaper axe parts just count as worth celebrating
- "New to your log" is only trusted when RuneProfile sync has the whole log; otherwise the game's own new-collection-log message is used, so an item you already own no longer flashes as new
- A second big odds number (Show Second Odds Number) shown at the right of the box; Steady now ranges about 6-72%
- Drift profiles last 10-15 minutes on an internal clock and only change after a finished kill or action, whatever the activity
- Odds now move in "drift profiles": Steady (everyday), Climber (rises toward ~90 as a fight nears its end), Choppy, Sleeper, Spike and Fader; everyday highs top out around 82
- Overlay Layout setting: Full, Compact (two rows with a sparkline, wiki abbreviations like ToB) or Ticker (a single scrolling line)
- Optional Win Sound using built-in game sounds
- Both lines now flash purple on a win when the second line is shown
- Settings are grouped into collapsible sections with clearer names; saved settings are kept
- Removed Pop Off Min Value (Win Minimum Drop Value covers value gating)
- Renamed to Prediction Market Drops

## 1.1.0

- Blacklist and priority drop lists; Win Minimum Drop Value; Flash on Win; Celebration Seconds; Pop Off on Every Drop
- Wins: collection log items new to the account, items with no GE price yet, or drops over the value threshold
- Most drop tables now ship inside the plugin (OSRS Wiki snapshot); live lookups and RuneProfile sync stay opt-in
