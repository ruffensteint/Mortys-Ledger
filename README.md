# Morty's Ledger

A RuneLite plugin for automatically tracking Mortimer Slayer assignments and
publishing task announcements to Discord.

## Features

- Detects new Mortimer Slayer assignments and completed tasks.
- Records the task number, monster, assigned amount, modifier, and Slayer XP.
- Splits modifier-boosted Slayer XP into base and bonus XP.
- Counts superior spawns and records their dropped items.
- Counts clue-scroll drops only when the assignment has a clue modifier.
- Stores task history in `.runelite/mortimer-slayer-tracker/task-history.json`.
- Optionally posts new-task and completion embeds to Discord.
- Includes an automatically resolved OSRS Wiki monster thumbnail in Discord
  posts without transmitting the character name.
- Optionally downloads OSRS Wiki monster thumbnails for local history cards.
- Supports customizable new-task and completion announcement titles.

## Discord Webhook Setup

Discord integration is disabled by default.

1. In Discord, open the destination channel's settings.
2. Select **Integrations**, then **Webhooks**.
3. Create a webhook, choose its name and avatar, and copy its webhook URL.
4. In RuneLite, open **Configuration** and select **Morty's Ledger**.
5. Paste one or more URLs into **Discord webhook URLs**. Separate multiple URLs
   with commas.
6. Optionally customize **New task title** and **Completed task title**.
7. Enable **Discord webhook** and accept RuneLite's third-party warning.

Enabling the webhook immediately posts the currently active task, when one is
available. The plugin uses each webhook's configured Discord name and avatar.
It posts task data only to the supplied Discord webhooks. Monster thumbnails are
resolved and downloaded from the OSRS Wiki, then attached to the Discord post.
Both services will see the source IP address of their respective HTTPS requests.

If a thumbnail cannot be resolved or downloaded, the text announcement is still
sent normally.

## Wiki Thumbnails

History-card thumbnails are disabled by default. To enable them, open Morty's
Ledger settings, enable **Wiki thumbnails**, and accept RuneLite's third-party
warning. This makes HTTPS requests to the OSRS Wiki, which exposes your IP
address to that service. Disabling the setting prevents the local history panel
from requesting Wiki images; cards and locally stored history continue to work.

## Local History

Task history is stored beneath RuneLite's data directory:

```text
.runelite/mortimer-slayer-tracker/task-history.json
```

## Development

This project targets Java 11. Build and test it on Windows with:

```powershell
.\gradlew.bat test
```

Launch a developer RuneLite client with:

```powershell
.\gradlew.bat run
```

Windows users can also double-click `launch-dev-client.cmd`. It launches the
development client from the repository using the JDK configured by `JAVA_HOME`.

The Gradle terminal remains open and may show the `:run` task below 100% while
RuneLite is running. This is expected; the task finishes when the client closes.
