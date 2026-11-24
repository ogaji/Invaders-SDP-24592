Space Invaders

Install Java 17 or later, then extract this entire ZIP.

Windows: double-click Launch.bat
macOS:   open Launch.command, or run: sh launch.sh
Linux:   run: sh launch.sh
Any OS:  java -jar space-invaders-launcher.jar

The launcher checks GitHub Releases when it opens. Press Play when it is ready.
If you are offline, the bundled or previously installed game can still run.
Enable "Include pre-releases" to receive release candidates.

The launcher downloads a release ZIP, verifies its SHA-256 checksum, and installs
only the game JAR. It keeps your saves outside the version directories. A failed
update leaves the installed version available. The launcher itself is updated
by downloading a new launcher JAR or distribution ZIP.

Run java -jar space-invaders-launcher.jar --help for command-line options.
The bin/space-invaders scripts run the bundled game directly without updating.

The launcher includes Gson (https://github.com/google/gson), licensed under
Apache License 2.0. Gson's license is retained inside the launcher JAR.
