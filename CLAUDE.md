# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Keep it up to
date - when making changes that affect architecture, data flow, build setup, testing conventions, or key constraints,
update the relevant sections here.

## What this is

TeleSign (successor of atsSignTeleport) - a Paper plugin. A sign whose first line is `[TELESIGN]` becomes a teleport
sign; right-clicking it teleports the player to the stored destination. Paper-only on purpose: Spigot is not
supported, Paper API (Adventure, MiniMessage) is fine to use.

## Commands

```sh
./gradlew build                      # compile, test, shadowJar -> build/libs/TeleSign-<version>.jar
./gradlew test                       # JUnit + MockBukkit tests, then the JaCoCo XML report and coverage summary
./gradlew test --tests "eu.andret.telesign.TeleSignServiceTest.parseSignDataWithCoords"
```

- **100% instruction coverage** is enforced by Codecov (`codecov.yml`), not locally - every new branch needs a test.
  The plugin class is covered too (MockBukkit loads the real plugin), nothing is excluded.
- Java toolchain, Paper API and all dependency versions live in `gradle/libs.versions.toml`. Project `version`,
  `group`, `artifact` and `minecraftVersions` (what releases are marked as supporting) live in `gradle.properties`;
  `${version}` is expanded into `plugin.yml`. MockBukkit (`mockbukkit-v26.2`) must match the Paper API version.
- The `jar` task is disabled on purpose: the shadow jar (bStats relocated to `eu.andret.telesign.bstats`) is the only
  jar. There is no Maven publishing.

## Architecture

Three-layer structure in `eu.andret.telesign`:

- **`TeleSignPlugin`** - entry point only; wires the other classes together in `onEnable()`: saves the default config,
  `service.reload()` (an invalid config throws and stops the plugin), registers the listener, `updateSigns()`,
  sets the command executor, starts bStats (ID 16239).
- **`TeleSignListener`** - thin Bukkit event router for `PlayerInteractEvent`, `BlockBreakEvent`, `SignChangeEvent`,
  `ChunkLoadEvent`. Does permission checks, block-state extraction and turning the sign's `Component` lines into plain
  text, then delegates to the service. Every handler of a cancellable event has `ignoreCancelled = true`, so region
  protections keep working; the click and the sign change run at `HIGH`, after protections cancelling at `NORMAL` or
  lower. A click uses the destination of the side the player faces (`Sign#getInteractableSideFor`, Paper API); a click
  on a side with a destination is always cancelled (no sign editing); a destination in a missing world gets a message
  instead of a teleport.
- **`TeleSignService`** - all business logic. Each side of a sign has its own destination in the sign's
  `PersistentDataContainer`: six `NamespacedKey`s per side (`Keys` record), `telesign:front/world`, `telesign:front/x`,
  ... and `telesign:back/...`; the namespace is the plugin name, so renaming the plugin orphans every existing sign.
  `isTeleportSign` is true when any side has a destination. Holds the four line templates: `reload()` parses
  `config.yml` into a fresh `YamlConfiguration` and only replaces the templates when the file is valid and `lines`
  has exactly four entries, otherwise it throws `IllegalArgumentException` with a readable message. Templates are
  MiniMessage, the values are inserted with `Placeholder.unparsed`, so a world name is never parsed as tags.
- **`TeleSignCommand`** - `/telesign reload` (alias `/tsigns`, declared in `plugin.yml`): `service.reload()` then
  `service.updateSigns()`; a config error is sent to the sender in red and nothing changes.

## Data flow

**Sign creation** (`SignChangeEvent`): `parseSignData` validates line 0 is `[TELESIGN]` (any case), line 1 is either
`[worldName]` of an existing world or `@playerName` (online player snapshot, x/y/z rounded to 0.5), line 2 is
`[x, y, z]` or `[x, y, z, yaw, pitch]`, line 3 optionally `[yaw, pitch]`. On success, `applySignData` writes the
destination to the keys of the event's side (`SignChangeEvent#getSide`) and replaces the event's lines with the
rendered templates.

**Teleportation** (`PlayerInteractEvent`, right-click): reads the world and coordinates of the side the player faces
from the PDC, `player.teleport()`.

**Sign display refresh**: on plugin start and `/telesign reload`, `updateSigns()` goes through all loaded chunks;
`updateChunk()` also runs on `ChunkLoadEvent`. Both render the templates on each side that has a destination and
leave the other side alone.

## Tests

- JUnit 6 + AssertJ + MockBukkit, `// given` / `// when` / `// then` structure. No Mockito.
- Tests extend `helper/PluginTest`, which starts `MockBukkit.mock()`, loads the real plugin with the shipped
  `config.yml` and adds the world `world` before every test. `placeSign` puts a standing sign whose front faces south
  (+z), `placeTeleportSign`/`addDestination` write a destination of a side straight into its PDC, `writeSign` fires a
  `SignChangeEvent` as a player finishing a side, `writeConfig` replaces the plugin's `config.yml` on disk.
- Events are built by hand and fired with `server.getPluginManager().callEvent`. Region protections are simulated by
  registering a listener that cancels at the default priority.
- MockBukkit leaves `Chunk#getTileEntities()` unimplemented, so the test world is `helper/TileEntityWorld`, whose
  chunks implement it. It also leaves `Sign#getInteractableSideFor` unimplemented, so `placeSign` gives the block a
  `helper/SidedSignStateMock`: the front is the side the sign is rotated towards. A new MockBukkit state starts with
  default block data, which `update()` writes back to the block, so the rotation is set on the state too. A block is
  only in `getLoadedChunks()` once its chunk was accessed (`placeSign` loads it).
- MockBukkit throws `UnimplementedOperationException`, a `TestAbortedException`, from what it does not implement, and
  JUnit reports that as **skipped**. The `test` task fails the build when any test is skipped, so a test never passes
  without running.

## Conventions

- Tabs for indentation, LF line endings (`.gitattributes`), always braces, no wildcard imports, no `var`, `final` on
  parameters and locals, `@NotNull`/`@Nullable` from `org.jetbrains.annotations`. No copyright headers in files.
- Messages are Adventure `Component`s; never `ChatColor` or `&` codes.
- User-facing changes go under `## Unreleased` in `CHANGELOG.md` (`org.jetbrains.changelog` format); release notes are
  extracted from it. Never bump the version or add version sections by hand.
- Apache 2.0: the jar's `META-INF` carries `LICENSE`/`NOTICE` renamed with the `-tele-sign` suffix so shaded
  libraries' files do not overwrite them.
- Build script reads project properties through `project.group`/`project.version` and
  `providers.gradleProperty(...)`, never `project.properties[...]`.
- CI is GitHub Actions. `build.yml` builds every push and PR and uploads the JaCoCo XML report to Codecov with the
  `CODECOV_TOKEN` secret. Releasing takes two workflows - releases are never made by pushing tags:
  - `release.yml` is run by hand from the Actions tab on `main` with a `version` input. It sets the version in
    `gradle.properties`, builds and tests, runs `patchChangelog` (fails when "Unreleased" is empty), commits both files
    as `Released <version>.` by github-actions, and creates a **draft** GitHub release of that commit with the jar.
  - `publish.yml` runs when that draft is published (which creates the `v<version>` tag). It downloads the jar from the
    release and uploads that same file, with the release description as the changelog, to Modrinth (`mc-publish`,
    project ID `7VN6bx7n` in the workflow, `MODRINTH_TOKEN` secret) and Hangar (`hangarPublish` in
    `build.gradle.kts` with `-PhangarJar`, project `andret2344/TeleSign`, `HANGAR_API_TOKEN` secret).

  Each upload is skipped without its secret; a token is passed only to the step that needs it. A version with a
  `-suffix` is a pre-release (Modrinth beta, Hangar "Snapshot" channel).
