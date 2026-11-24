package launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LauncherTest {
    @TempDir Path temporary;
    private HttpServer server;
    private URI api;
    private Installation installation;
    private final Map<String, byte[]> responses = new ConcurrentHashMap<>();
    private final Map<String, Integer> requests = new ConcurrentHashMap<>();
    private final Transfer transfer = new Transfer(true);

    @BeforeEach void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().toString();
            requests.merge(path, 1, Integer::sum);
            byte[] bytes = responses.get(path);
            exchange.sendResponseHeaders(bytes == null ? 404 : 200, bytes == null ? -1 : bytes.length);
            if (bytes != null) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        api = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        installation = new Installation(temporary.resolve("게임 with spaces"), "test/game");
    }

    @AfterEach void tearDown() throws Exception {
        installation.close();
        server.stop(0);
    }

    private Updater updater() {
        return new Updater(installation, new GitHubReleases(api, "test/game", transfer), transfer, null, "1.0-SNAPSHOT");
    }

    private Updater.Result refresh() throws IOException { return updater().refresh(false, false, ignored -> { }); }

    @Test void installsAnUpdateAndDoesNotDownloadItAgain() throws Exception {
        publish("1.0.0");
        Updater.Result first = refresh();
        assertEquals("1.0.0", first.installed().version());
        assertNull(first.warning());
        assertEquals(first.installed(), installation.current().orElseThrow());
        assertEquals(first.installed(), refresh().installed());
        assertEquals(1, requests.get("/download/space-invaders-1.0.0.zip"));
    }

    @Test void updateKeepsOldJarAndSavedProgressAndLaunchesInTheSameSaveDirectory() throws Exception {
        publish("1.0.0");
        Installation.Installed old = refresh().installed();
        Path saves = installation.directory.resolve("saves");
        Files.createDirectories(saves);
        Files.writeString(saves.resolve("player_assets.properties"), "credits=123\n");
        Files.writeString(saves.resolve("scores"), "saved-score\n");
        publish("1.1.0");
        Installation.Installed updated = refresh().installed();
        assertEquals("1.1.0", updated.version());
        assertEquals(old, installation.previous().orElseThrow());
        assertTrue(Files.isRegularFile(old.jar()));
        assertEquals("credits=123\n", Files.readString(saves.resolve("player_assets.properties")));
        assertEquals("saved-score\n", Files.readString(saves.resolve("scores")));
        Process game = GameRunner.start(updated, installation.directory);
        assertTrue(game.waitFor(15, TimeUnit.SECONDS));
        assertEquals(0, game.exitValue(), Files.readString(installation.directory.resolve("game.log")));
        assertTrue(Files.isSameFile(saves, Path.of(Files.readString(saves.resolve("child-process.txt")))));
    }

    @Test void checksumFailureKeepsThePlayableVersionAndCleansTemporaryFiles() throws Exception {
        publish("1.0.0");
        var old = refresh().installed();
        publish("2.0.0");
        responses.put("/download/space-invaders-2.0.0.zip.sha256",
                ("0".repeat(64) + "  space-invaders-2.0.0.zip\n").getBytes(StandardCharsets.UTF_8));
        var result = refresh();
        assertEquals(old, result.installed());
        assertTrue(result.warning().contains("checksum"));
        assertEquals(old, installation.current().orElseThrow());
        try (var files = Files.list(installation.directory)) {
            assertTrue(files.noneMatch(file -> file.toString().endsWith(".part")));
        }
    }

    @Test void truncatedDownloadCannotActivateAnUpdate() throws Exception {
        publish("1.0.0");
        var old = refresh().installed();
        publish("2.0.0");
        responses.put("/download/space-invaders-2.0.0.zip", new byte[] { 1, 2, 3 });
        var result = refresh();
        assertEquals(old, result.installed());
        assertTrue(result.warning().contains("incomplete"));
    }

    @Test void verifiedZipWithInvalidJarCannotActivateAnUpdate() throws Exception {
        publish("1.0.0");
        var old = refresh().installed();
        JsonObject broken = release("2.0.0", zip("2.0.0", new byte[] { 1, 2, 3 }, Map.of()));
        setLatest(broken);
        var result = refresh();
        assertEquals(old, result.installed());
        assertNotNull(result.warning());
    }

    @Test void onlyTheExactGameEntryIsCopiedFromTheArchive() throws Exception {
        JsonObject release = release("1.0.0", zip("1.0.0", gameJar("1.0.0"),
                Map.of("../../escaped.txt", "bad".getBytes(StandardCharsets.UTF_8))));
        setLatest(release);
        assertEquals("1.0.0", refresh().installed().version());
        assertFalse(Files.exists(temporary.resolve("escaped.txt")));
        assertFalse(Files.exists(installation.directory.resolve("escaped.txt")));
    }

    @Test void missingOrAdditionalRuntimeJarsAreRejected() throws Exception {
        JsonObject release = release("1.0.0", zip("wrong-version", gameJar("1.0.0"), Map.of()));
        setLatest(release);
        assertThrows(IOException.class, this::refresh);
        release = release("1.0.0", zip("1.0.0", gameJar("1.0.0"),
                Map.of("space-invaders-1.0.0/lib/dependency.jar", new byte[] { 1 })));
        setLatest(release);
        assertThrows(IOException.class, this::refresh);
        assertTrue(installation.current().isEmpty());
    }

    @Test void missingChecksumAndMalformedMetadataLeaveCurrentVersionIntact() throws Exception {
        publish("1.0.0");
        var old = refresh().installed();
        JsonObject next = publish("2.0.0");
        next.getAsJsonArray("assets").remove(1);
        setLatest(next);
        assertEquals(old, refresh().installed());
        responses.put("/repos/test/game/releases/latest", "{broken".getBytes(StandardCharsets.UTF_8));
        assertEquals(old, refresh().installed());
    }

    @Test void stableAndPrereleaseChannelsChooseCorrectVersions() throws Exception {
        JsonObject stable = publish("1.0.0");
        JsonObject rc2 = release("2.0.0-rc2", zip("2.0.0-rc2", gameJar("rc2"), Map.of()));
        JsonObject rc10 = release("2.0.0-rc10", zip("2.0.0-rc10", gameJar("rc10"), Map.of()));
        JsonObject draft = release("9.0.0", zip("9.0.0", gameJar("draft"), Map.of()));
        draft.addProperty("draft", true);
        JsonArray list = new JsonArray();
        list.add(rc2); list.add(draft); list.add(stable); list.add(rc10);
        responses.put("/repos/test/game/releases?per_page=100&page=1", list.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals("1.0.0", refresh().installed().version());
        assertEquals("2.0.0-rc10", updater().refresh(false, true, ignored -> { }).installed().version());
        // Switching back to stable must not silently downgrade an existing release candidate.
        assertEquals("2.0.0-rc10", refresh().installed().version());
    }

    @Test void prereleaseSearchChecksLaterPages() throws Exception {
        JsonObject low = release("1.0.0", zip("1.0.0", gameJar("one"), Map.of()));
        JsonObject high = release("3.0.0", zip("3.0.0", gameJar("three"), Map.of()));
        JsonArray first = new JsonArray();
        for (int i = 0; i < 100; i++) first.add(low);
        JsonArray second = new JsonArray(); second.add(high);
        responses.put("/repos/test/game/releases?per_page=100&page=1", first.toString().getBytes(StandardCharsets.UTF_8));
        responses.put("/repos/test/game/releases?per_page=100&page=2", second.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals("3.0.0", updater().refresh(false, true, ignored -> { }).installed().version());
    }

    @Test void offlineBundleBootstrapsAndNetworkFailureRetainsIt() throws Exception {
        Path bundle = temporary.resolve("bundle.jar"); Files.write(bundle, gameJar("bundle"));
        Updater updater = new Updater(installation, new GitHubReleases(api, "test/game", transfer), transfer, bundle, "1.0-SNAPSHOT");
        assertEquals("1.0-SNAPSHOT", updater.refresh(true, false, ignored -> { }).installed().version());
        assertTrue(requests.isEmpty());
        assertEquals("1.0-SNAPSHOT", updater.refresh(false, false, ignored -> { }).installed().version());
        assertThrows(IOException.class, () -> new Transfer().text(api, 100));
    }

    @Test void noBundleAndNoReleaseProducesActionableError() {
        IOException error = assertThrows(IOException.class, this::refresh);
        assertTrue(error.getMessage().contains("No release"));
        assertTrue(assertThrows(IOException.class, () -> updater().refresh(true, false, ignored -> { }))
                .getMessage().contains("No game is installed"));
    }

    @Test void recoversThePreviousVersionWhenActiveJarIsDamaged() throws Exception {
        publish("1.0.0"); var old = refresh().installed();
        publish("2.0.0"); var current = refresh().installed();
        Files.writeString(current.jar(), "broken");
        var recovered = updater().refresh(true, false, ignored -> { });
        assertEquals(old, recovered.installed());
        assertEquals(old, installation.current().orElseThrow());
    }

    @Test void concurrentLauncherAndRepositoryMixingAreRejected() throws Exception {
        assertThrows(IOException.class, () -> new Installation(installation.directory, "test/game"));
        Path separate = temporary.resolve("another-folder");
        try (Installation ignored = new Installation(separate, "test/game")) { }
        assertThrows(IOException.class, () -> new Installation(separate, "other/game"));
    }

    @Test void semanticVersionsAndPlatformPathsArePredictable() {
        assertTrue(ReleaseVersion.parse("v1.10.0").orElseThrow().compareTo(ReleaseVersion.parse("1.2.0").orElseThrow()) > 0);
        assertTrue(ReleaseVersion.parse("1.0.0").orElseThrow().compareTo(ReleaseVersion.parse("1.0.0-rc99").orElseThrow()) > 0);
        assertTrue(ReleaseVersion.parse("1.01.0").isEmpty());
        assertTrue(ReleaseVersion.parse("../../evil").isEmpty());
        Path home = temporary.resolve("home");
        assertEquals(home.resolve("Library/Application Support/SpaceInvaders/test-game"),
                LaunchOptions.defaultDirectory("Mac OS X", home, Map.of(), "test/game"));
        assertEquals(home.resolve(".local/share/space-invaders/test-game"),
                LaunchOptions.defaultDirectory("Linux", home, Map.of("XDG_DATA_HOME", "relative"), "test/game"));
        assertEquals(temporary.resolve("local/SpaceInvaders/test-game"),
                LaunchOptions.defaultDirectory("Windows 11", home, Map.of("LOCALAPPDATA", temporary.resolve("local").toString()), "test/game"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[] { "--data-dir" }, "test/game"));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[] { "--play", "--check-only" }, "test/game"));
    }

    @Test void standaloneLauncherJarBootstrapsTheBundleWithoutExternalLibraries() throws Exception {
        Path distribution = temporary.resolve("배포 ZIP with spaces");
        Files.createDirectories(distribution.resolve("lib"));
        Path launcher = distribution.resolve("space-invaders-launcher.jar");
        Files.copy(Path.of(System.getProperty("launcher.test.jar")), launcher);
        try (JarFile jar = new JarFile(launcher.toFile())) {
            assertNotNull(jar.getJarEntry("com/google/gson/JsonParser.class"));
            assertNotNull(jar.getJarEntry("META-INF/licenses/gson-LICENSE.txt"));
        }
        String version = System.getProperty("launcher.test.version");
        Files.write(distribution.resolve("lib/space-invaders-" + version + ".jar"), gameJar(version));
        Path data = temporary.resolve("standalone-data");
        String java = GameRunner.command(launcher, data).get(0);
        String hash = Installation.digest(launcher);
        Process process = new ProcessBuilder(java, "-jar", launcher.getFileName().toString(), "--offline", "--check-only",
                "--data-dir", "../standalone-data").directory(distribution.toFile())
                .redirectErrorStream(true).redirectOutput(temporary.resolve("cli.log").toFile()).start();
        assertTrue(process.waitFor(20, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), Files.readString(temporary.resolve("cli.log")));
        assertEquals(hash, Installation.digest(launcher));
        try (Installation installed = new Installation(data, "ogaji/Invaders-SDP-24592")) {
            assertEquals(version, installed.current().orElseThrow().version());
        }
    }

    private JsonObject publish(String version) throws Exception {
        JsonObject release = release(version, zip(version, gameJar(version), Map.of()));
        setLatest(release);
        return release;
    }

    private void setLatest(JsonObject release) {
        responses.put("/repos/test/game/releases/latest", release.toString().getBytes(StandardCharsets.UTF_8));
    }

    private JsonObject release(String version, byte[] zip) throws Exception {
        String name = "space-invaders-" + version + ".zip";
        byte[] checksum = (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(zip))
                + "  " + name + "\n").getBytes(StandardCharsets.UTF_8);
        responses.put("/download/" + name, zip);
        responses.put("/download/" + name + ".sha256", checksum);
        JsonObject release = new JsonObject();
        release.addProperty("tag_name", "v" + version);
        release.addProperty("draft", false);
        release.addProperty("prerelease", version.contains("-rc"));
        JsonArray assets = new JsonArray();
        assets.add(asset(name, zip.length));
        assets.add(asset(name + ".sha256", checksum.length));
        release.add("assets", assets);
        return release;
    }

    private JsonObject asset(String name, int size) {
        JsonObject asset = new JsonObject();
        asset.addProperty("name", name);
        asset.addProperty("browser_download_url", api.resolve("download/" + name).toString());
        asset.addProperty("size", size);
        return asset;
    }

    private static byte[] gameJar(String marker) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "engine.Core");
        try (JarOutputStream jar = new JarOutputStream(bytes, manifest)) {
            jar.putNextEntry(new JarEntry("engine/Core.class"));
            try (var input = LauncherTest.class.getResourceAsStream("/engine/Core.class")) { input.transferTo(jar); }
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("version.txt"));
            jar.write(marker.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static byte[] zip(String version, byte[] jar, Map<String, byte[]> extra) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("space-invaders-" + version + "/lib/space-invaders-" + version + ".jar"));
            zip.write(jar); zip.closeEntry();
            for (var entry : extra.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
