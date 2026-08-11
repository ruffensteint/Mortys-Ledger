# Mortimer Slayer Tracker

A RuneLite plugin for automatically tracking Mortimer Slayer assignments and
publishing a configurable Road to 99 Slayer report to Discord.

## Planned V1

- Detect new Mortimer Slayer assignments.
- Record task monster, assigned amount, modifier, and starting Slayer XP.
- Calculate Slayer XP gained when a task completes.
- Count superior spawns and clue-scroll drops.
- Parse Mortimer completion messages for the completed-task count.
- Store task history locally under the RuneLite directory.
- Optionally post a formatted Discord report when the next task is assigned.

Discord integration will be disabled by default and will clearly disclose that
enabling it sends report data to the configured third-party webhook.

## Development

Build and test the plugin on Windows with:

```powershell
.\gradlew.bat test
```

Launch a developer RuneLite client with:

```powershell
.\gradlew.bat run
```
