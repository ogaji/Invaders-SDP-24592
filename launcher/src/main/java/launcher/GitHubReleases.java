package launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

final class GitHubReleases {
    record Asset(String name, URI url, long size) { }
    record Release(ReleaseVersion version, Asset zip, Asset checksum) { }

    private final Transfer transfer;
    private final URI releases;

    GitHubReleases(String repository, Transfer transfer) {
        this(URI.create("https://api.github.com/"), repository, transfer);
    }

    GitHubReleases(URI api, String repository, Transfer transfer) {
        if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Use a repository name in owner/repository format.");
        }
        this.transfer = transfer;
        this.releases = api.resolve("repos/" + repository + "/releases");
    }

    Optional<Release> latest(boolean prereleases) throws IOException {
        try {
            if (!prereleases) {
                try {
                    JsonObject json = JsonParser.parseString(transfer.text(
                            URI.create(releases + "/latest"), 4 * 1024 * 1024)).getAsJsonObject();
                    if (!eligible(json, false)) throw new IOException("The latest release has no supported stable version tag.");
                    return Optional.of(decode(json));
                } catch (Transfer.HttpFailure e) {
                    if (e.status == 404) return Optional.empty();
                    throw e;
                }
            }
            JsonObject newest = null;
            // GitHub sorts by creation date, so compare versions across all pages.
            for (int page = 1; page <= 20; page++) {
                JsonArray entries = JsonParser.parseString(transfer.text(
                        URI.create(releases + "?per_page=100&page=" + page), 8 * 1024 * 1024)).getAsJsonArray();
                for (JsonElement entry : entries) {
                    JsonObject json = entry.getAsJsonObject();
                    if (eligible(json, true) && (newest == null || version(json).compareTo(version(newest)) > 0)) {
                        newest = json;
                    }
                }
                if (entries.size() < 100) return newest == null ? Optional.empty() : Optional.of(decode(newest));
            }
            throw new IOException("Too many release pages. Try the stable update channel.");
        } catch (com.google.gson.JsonParseException | IllegalArgumentException | IllegalStateException | NullPointerException e) {
            throw new IOException("GitHub returned invalid release information.", e);
        }
    }

    private static boolean eligible(JsonObject json, boolean prereleases) {
        if (json.get("draft").getAsBoolean()) return false;
        Optional<ReleaseVersion> parsed = ReleaseVersion.parse(json.get("tag_name").getAsString());
        return parsed.isPresent() && (prereleases || (!json.get("prerelease").getAsBoolean()
                && parsed.get().candidate() == null));
    }

    private static ReleaseVersion version(JsonObject json) {
        return ReleaseVersion.parse(json.get("tag_name").getAsString()).orElseThrow();
    }

    private static Release decode(JsonObject json) throws IOException {
        ReleaseVersion version = version(json);
        String name = "space-invaders-" + version.value() + ".zip";
        Asset zip = null;
        Asset checksum = null;
        for (JsonElement entry : json.getAsJsonArray("assets")) {
            JsonObject asset = entry.getAsJsonObject();
            String assetName = asset.get("name").getAsString();
            if (assetName.equals(name) || assetName.equals(name + ".sha256")) {
                Asset parsed = new Asset(assetName, URI.create(asset.get("browser_download_url").getAsString()),
                        asset.get("size").getAsLong());
                if (assetName.equals(name)) {
                    if (zip != null) throw new IOException("Duplicate release ZIP.");
                    zip = parsed;
                } else {
                    if (checksum != null) throw new IOException("Duplicate release checksum.");
                    checksum = parsed;
                }
            }
        }
        if (zip == null || checksum == null) throw new IOException("The release ZIP or SHA-256 checksum is missing.");
        return new Release(version, zip, checksum);
    }
}
