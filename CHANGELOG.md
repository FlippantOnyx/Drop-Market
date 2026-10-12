# Changelog

## 1.2.0

- Fix: a win no longer keeps flashing while you keep killing the same monster; it ends after Celebration Seconds
- Repeat untradeable uniques (e.g. Xeric's talisman) no longer count as wins unless they are new to your log, a pet, or a part of an expensive item (icons, fangs, Mokhaiotl cloth...)
- "New to your log" is only trusted when RuneProfile sync has the whole log; otherwise the game's own new-collection-log message is used, so an item you already own no longer flashes as new
- A second big odds number (Show Second Odds Number) with a position setting; Steady now ranges about 6-72%
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
