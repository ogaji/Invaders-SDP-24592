package launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class Updater {
    record Result(Installation.Installed installed, String message, String warning) { }
    private final Installation installation;
    private final GitHubReleases releases;
    private final Transfer transfer;
    private final Path bundledJar;
    private final String bundledVersion;

    Updater(Installation installation, GitHubReleases releases, Transfer transfer,
            Path bundledJar, String bundledVersion) {
        this.installation = installation;
        this.releases = releases;
        this.transfer = transfer;
        this.bundledJar = bundledJar;
        this.bundledVersion = bundledVersion;
    }

    Result refresh(boolean offline, boolean prereleases, Consumer<String> status) throws IOException {
        Installation.Installed installed = null;
        String warning = null;
        try { installed = installation.current().orElse(null); }
        catch (IOException e) {
            warning = e.getMessage();
            try {
                installed = installation.previous().orElse(null);
                if (installed != null) installation.activate(installed);
            } catch (IOException ignored) { /* A fresh download or the bundle may recover this installation. */ }
        }
        if (bundledJar != null && Files.isRegularFile(bundledJar) && newer(bundledVersion, installed)) {
            status.accept("Preparing the bundled game...");
            try { installed = installation.install(bundledJar, bundledVersion); }
            catch (IOException e) { warning = e.getMessage(); }
        }
        if (offline) {
            if (installed == null) throw new IOException("No game is installed. Connect to download a release or extract the full game ZIP.");
            return new Result(installed, "Ready to play offline", warning);
        }
        status.accept("Checking GitHub Releases...");
        try {
            Optional<GitHubReleases.Release> latest = releases.latest(prereleases);
            if (latest.isEmpty()) {
                if (installed == null) throw new IOException("No release is available. For an RC release, enable Include pre-releases.");
                return new Result(installed, "Ready to play", "No release is available on this update channel.");
            }
            GitHubReleases.Release release = latest.get();
            if (!newer(release.version().value(), installed)) {
                return new Result(installed, "Ready to play — no newer release", warning);
            }
            status.accept("Downloading version " + release.version().value() + "...");
            installed = download(release, status);
            return new Result(installed, "Updated to " + installed.version(), null);
        } catch (IOException e) {
            if (installed == null) throw e;
            return new Result(installed, "Ready to play the installed version", "Update unavailable: " + e.getMessage());
        }
    }

    private static boolean newer(String candidate, Installation.Installed installed) {
        if (installed == null) return true;
        Optional<ReleaseVersion> next = ReleaseVersion.parse(candidate);
        Optional<ReleaseVersion> current = ReleaseVersion.parse(installed.version());
        if (next.isEmpty()) return false; // Never replace a cached game with a development bundle.
        return current.isEmpty() || next.get().compareTo(current.get()) > 0;
    }

    private Installation.Installed download(GitHubReleases.Release release, Consumer<String> status) throws IOException {
        String checksumText = transfer.text(release.checksum().url(), 8192).strip();
        Matcher checksum = Pattern.compile("([a-fA-F0-9]{64})[ \\t]+\\*?"
                + Pattern.quote(release.zip().name())).matcher(checksumText);
        if (!checksum.matches()) throw new IOException("The release checksum file is invalid.");
        Path zip = Files.createTempFile(installation.directory, "download-", ".zip.part");
        Path game = Files.createTempFile(installation.directory, "game-", ".jar.part");
        try {
            transfer.download(release.zip().url(), zip, release.zip().size(), status);
            status.accept("Verifying the update...");
            if (!Installation.digest(zip).equalsIgnoreCase(checksum.group(1))) {
                throw new IOException("The ZIP checksum does not match. The installed game has been kept.");
            }
            extractGame(zip, game, release.version().value());
            return installation.install(game, release.version().value());
        } finally {
            Files.deleteIfExists(zip);
            Files.deleteIfExists(game);
        }
    }

    private static void extractGame(Path zipPath, Path target, String version) throws IOException {
        String prefix = "space-invaders-" + version + "/lib/";
        String expected = prefix + "space-invaders-" + version + ".jar";
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry game = null;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(expected)) {
                    if (game != null) throw new IOException("The release contains duplicate game JARs.");
                    game = entry;
                } else if (entry.getName().startsWith(prefix) && entry.getName().endsWith(".jar")) {
                    throw new IOException("This release needs a newer launcher supporting additional game libraries.");
                }
            }
            if (game == null || game.isDirectory() || game.getSize() > Transfer.MAX_ARCHIVE) {
                throw new IOException("The release ZIP does not contain the expected game JAR.");
            }
            // Never extract entry paths; copy only the exact expected JAR to a fixed temporary file.
            try (InputStream input = zip.getInputStream(game); OutputStream output = Files.newOutputStream(target)) {
                long bytes = 0;
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    bytes += count;
                    if (bytes > Transfer.MAX_ARCHIVE) throw new IOException("Expanded game JAR is too large.");
                    output.write(buffer, 0, count);
                }
            }
        }
    }
}
