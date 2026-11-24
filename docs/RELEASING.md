# Releasing the game

The [CI workflow](../.github/workflows/ci.yml) checks every pull request and `main` push on Windows, Linux, and macOS with Java 17, including launcher update and process tests. After all checks pass on `main`, it builds one distribution ZIP and uploads it as a seven-day Actions artifact. The [release workflow](../.github/workflows/release.yml) publishes a GitHub Release when a `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-rcN` tag is pushed. The tag must point to a commit in `main`. A release ZIP is built once, after the tagged commit passes all platform checks.

The release contains `space-invaders-VERSION.zip`, the standalone `space-invaders-launcher.jar`, a `.sha256` checksum for each, and automatically generated release notes. The ZIP includes the game JAR with its resources, the independent launcher JAR, and launch scripts for Windows, macOS, and Linux. Players need Java 17 or later; the ZIP does not bundle a JRE.

## Before the first release

The [README](../README.md#attribution-and-licensing) records the maintainer's confirmation of redistribution permission. Release tags are managed by the Dev Lead (`HyunseungJe`) and repository owner (`ogaji`). The active `v*` tag ruleset restricts creation, updates, and deletion to these accounts. Published version tags must not be moved or reused, including by these maintainers.

## Create a release

After the release changes have passed review and merged into `main`, make the first pipeline check a pre-release. Tag the current remote `main` commit:

```bash
git fetch origin main
git tag -a v0.2.0-rc1 origin/main -m "Release v0.2.0-rc1"
git push origin v0.2.0-rc1
```

Replace `v0.2.0-rc1` with the intended version. The tag push triggers the workflow. It checks the tagged commit on all three platforms, builds the ZIP after those checks, and publishes an `-rcN` tag as a GitHub pre-release. Stable version tags publish regular releases. Do not move or reuse a published version tag.

To check a downloaded archive on Linux or macOS, place its `.sha256` file beside it and run:

```bash
sha256sum -c space-invaders-0.2.0-rc1.zip.sha256
```

For the first pre-release, download and unzip the release on a Windows machine with Java 17 installed. Start `Launch.bat`, enable **Include pre-releases**, and press **Play**. Play one level with sound, then restart and confirm that records and purchases persist. Check `sh launch.sh` on Linux and macOS as well. Record the result in the release PR or Jira issue.

To verify a later update, keep the old launcher and data directory, publish the next version, and reopen the launcher. It should install the newer game JAR, preserve progress, and leave the launcher JAR unchanged. Repeat with the network unavailable to verify that the installed game remains playable. A release must contain the exact ZIP and checksum names above. Keep the game self-contained: additional runtime JARs require a new launcher/package contract. See [Launcher](LAUNCHER.md).
