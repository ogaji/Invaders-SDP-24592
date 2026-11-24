# Launcher

The Java 17 launcher runs independently of the game. Each time it opens, it checks the configured GitHub repository for a newer release, downloads the release ZIP and its SHA-256 file, and installs only the game JAR. The game starts in a separate JVM when you press **Play**. The launcher stays open until the game closes.

## Installation

Download and extract `space-invaders-VERSION.zip`. It includes a playable game even before the first online update:

| Platform | Start |
| --- | --- |
| Windows | Double-click `Launch.bat` |
| macOS | Open `Launch.command`, or run `sh launch.sh` |
| Linux | Run `sh launch.sh` |
| Any supported platform | `java -jar space-invaders-launcher.jar` |

Java 17 or newer is required. If macOS blocks a downloaded command script, use `sh launch.sh` from Terminal. These are portable Java distributions, not signed native installers or bundled Java runtimes.

You can instead download only `space-invaders-launcher.jar`; on its first online run it installs the latest game from GitHub Releases. With no release and no bundled/cached game, it shows an error instead of launching an incomplete installation. The default repository is `ogaji/Invaders-SDP-24592`.

The default update channel follows GitHub's latest stable release. **Include pre-releases** also considers `vMAJOR.MINOR.PATCH-rcN` tags and picks the highest version across release pages. The UI remembers this choice. Switching channels never downgrades an installed version. Development ZIPs can bootstrap offline, then move to a published version on the next successful check.

## Updates and saved progress

The launcher keeps its installation in the following per-user location, with a repository-specific subfolder:

| Platform | Base folder |
| --- | --- |
| Windows | `%LOCALAPPDATA%/SpaceInvaders/` |
| macOS | `~/Library/Application Support/SpaceInvaders/` |
| Linux | `$XDG_DATA_HOME/space-invaders/`, or `~/.local/share/space-invaders/` |

The default repository subfolder is `ogaji-invaders-sdp-24592`. It contains:

- `versions/<version>/<sha256>.jar`: verified game files, kept immutable during updates.
- `current.properties` and `previous.properties`: active and previous game versions.
- `saves/`: high scores, purchases, and game working files; passed to the game through `invaders.storageDir`.
- `game.log`: output from the game process.

Records, achievements, and Java Preferences retain the game's existing per-user locations. The launcher does not delete or replace them. Existing scores/purchases from direct `bin/space-invaders` launches are not imported automatically: with both applications closed, copy `scores` and `player_assets.properties` from the old `lib/` folder into the launcher's `saves/` folder if needed. Keep a copy of the originals.

A ZIP is staged in the installation folder, size-limited, checked against its SHA-256 file, and inspected for the expected executable game JAR. Archive paths are never extracted into user directories. The game JAR is validated and copied into a version directory before an atomic pointer change activates it. The installation must be on a filesystem supporting atomic file moves. A failed download, missing asset, invalid checksum, or invalid game archive keeps the previous installation available. If the active file is damaged, startup attempts the previous version. An installation lock prevents two launchers from using the same folder simultaneously.

Checksums detect incomplete or changed downloads; they are not an independent signature if the release account itself is compromised. Only the designated release maintainers should publish assets. HTTPS is required for production downloads.

Game updates do not overwrite the running launcher. To update the launcher itself, download a new launcher JAR or ZIP and reopen it; it reuses the existing per-user installation and saves. The launcher only installs higher numbered versions, so publish a new version rather than replacing assets under an existing tag.

## Command-line use

```sh
java -jar space-invaders-launcher.jar --help
java -jar space-invaders-launcher.jar --include-prereleases
java -jar space-invaders-launcher.jar --offline
java -jar space-invaders-launcher.jar --check-only --data-dir "/path/to/game data"
java -jar space-invaders-launcher.jar --play
java -jar space-invaders-launcher.jar --repository OWNER/REPO
```

`--check-only` prints the update result without opening a window or launching a game. Exit code zero means a playable installation is available, including an offline fallback; warnings explain unsuccessful update checks. `--play` checks for updates, runs the game, waits for it to close, and returns its exit code. `--offline` skips network access. CLI pre-release selection is explicit; only the UI persists the checkbox preference. A custom repository uses its own cache and never imports the default repository's bundled game.

## Release contract

For tag `v1.2.3`, publish `space-invaders-1.2.3.zip` and `space-invaders-1.2.3.zip.sha256`. The checksum file uses `sha256sum` format and names that exact ZIP. The game must be at `space-invaders-1.2.3/lib/space-invaders-1.2.3.jar`, with `Main-Class: engine.Core` and all game resources/dependencies inside it. Other JARs under `lib/` are rejected to avoid launching an incomplete classpath. The release workflow produces this layout and also publishes the independent launcher JAR with its checksum.

## Verification

Run `:launcher:test` for local HTTP fixture tests covering installation, newer versions, stable/RC selection, pagination, no downgrade, offline bootstrap, checksums, truncation, invalid archives, path traversal, cache recovery, locking, Unicode/spaced paths, and real child-JVM execution. A subprocess test also runs the packaged standalone launcher without an external library folder. CI runs these checks on Windows, Linux, and macOS with Java 17. The game still needs the manual play/sound/persistence checks described in [Releasing](RELEASING.md).
