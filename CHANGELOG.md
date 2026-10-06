# TeleSign changelog

## Unreleased

### Changed

- Usage statistics are sent to the new [TeleSign page on bStats](https://bstats.org/plugin/bukkit/TeleSign/34536)
  instead of the one of atsSignTeleport.

## 1.0.0 - 2026-10-06

TeleSign is the successor of atsSignTeleport 0.2.1. The changes below are relative to it.

### Added

- Both sides of a sign can teleport, each to its own destination: write `[TELESIGN]` on the front, the back or both.
  Clicking a sign uses the side the player is facing.

### Changed

- Renamed from atsSignTeleport to TeleSign. The plugin folder is `plugins/TeleSign`, the command is `/telesign reload`
  with the `/tsigns` alias, and the permissions are `telesign.use`, `telesign.create`, `telesign.break` and
  `telesign.reload` (all part of `telesign.*`). Teleport signs made with atsSignTeleport are not recognized anymore.
- The first line that turns a sign into a teleport sign is `[TELESIGN]` instead of `[TELEPORT]`.
- Requires Paper (or a fork such as Purpur) 26.2 or newer and Java 25. Spigot is no longer supported.
- The sign lines in `config.yml` use the [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/) format
  instead of `&` color codes, with the `<world>`, `<x>`, `<y>`, `<z>`, `<yaw>` and `<pitch>` placeholders instead of
  `%WORLD%`, `%X%` and the like. A world name is always shown as written, never read as formatting.
- `/telesign reload` checks the config before applying it. An invalid file or a `lines` list without exactly four
  lines is not applied and the command says what is wrong with it; on startup it stops the plugin with that message.
  Before, fewer than four lines silently stopped signs from being created and refreshed.
- Licensed under the Apache License 2.0.

### Fixed

- Refreshing the signs no longer overwrites the front of a sign that was made a teleport sign on its back.
- Clicking a sign whose world does not exist anymore tells the player so instead of failing with an error.
- Region protection plugins are respected: clicking a teleport sign where such a plugin blocks the interaction no
  longer teleports, and a sign does not become a teleport sign where it blocks the change.
- The version shown by the server is the released version; it was fixed at 0.2.1 in `plugin.yml`.
- The server no longer downloads the JetBrains annotations library on startup; it is only needed to compile.
