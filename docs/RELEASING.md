# Releasing the game

The [CI workflow](../.github/workflows/ci.yml) checks every pull request and `main` push on Windows and Linux with Java 17. After both checks pass on `main`, it builds one distribution ZIP and uploads it as a seven-day Actions artifact. The [release workflow](../.github/workflows/release.yml) publishes a GitHub Release when a `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-rcN` tag is pushed. The tag must point to a commit in `main`. A release ZIP is built once, after the tagged commit passes both platform checks.

The release contains `space-invaders-VERSION.zip`, its `.sha256` checksum, and automatically generated release notes. The ZIP includes the application JAR, resources, and launch scripts for Windows and Unix-like systems. Players need Java 17 or later; the ZIP does not bundle a JRE.

## Before the first release

Confirm that the upstream code and bundled assets may be redistributed in a public release. The [README](../README.md#attribution-and-licensing) records the current licensing status. Only the Dev Lead creates release tags; restrict creation of `v*` tags to that role in the repository's tag ruleset.

## Create a release

After the release changes have passed review and merged into `main`, make the first pipeline check a pre-release. Tag the current remote `main` commit:

```bash
git fetch origin main
git tag -a v0.2.0-rc1 origin/main -m "Release v0.2.0-rc1"
git push origin v0.2.0-rc1
```

Replace `v0.2.0-rc1` with the intended version. The tag push triggers the workflow. It checks the tagged commit on both platforms, builds the ZIP after those checks, and publishes an `-rcN` tag as a GitHub pre-release. Stable version tags publish regular releases. Do not move or reuse a published version tag.

To check a downloaded archive on Linux or macOS, place its `.sha256` file beside it and run:

```bash
sha256sum -c space-invaders-0.2.0-rc1.zip.sha256
```

For the first pre-release, manually download and unzip the release on a Windows machine with only Java 17 installed. Start `bin\space-invaders.bat`, play one level with sound, then restart and confirm that records and purchases persist. Record the result in the release PR or Jira issue. Later update delivery can read the latest GitHub Release and its versioned ZIP and checksum; the game does not yet include an updater.
