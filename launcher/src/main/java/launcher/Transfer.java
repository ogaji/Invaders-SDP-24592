package launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Bounded downloads with connect/read timeouts, including redirect validation. */
final class Transfer {
    static final long MAX_ARCHIVE = 256L * 1024 * 1024;
    private final boolean allowLoopbackHttp;

    Transfer() { this(false); }
    Transfer(boolean allowLoopbackHttp) { this.allowLoopbackHttp = allowLoopbackHttp; }

    String text(URI uri, int maximum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(uri, output, maximum, ignored -> { });
        return output.toString(StandardCharsets.UTF_8);
    }

    void download(URI uri, Path target, long expectedSize, Consumer<String> status) throws IOException {
        if (expectedSize < 1 || expectedSize > MAX_ARCHIVE) {
            throw new IOException("The release ZIP has an unsupported size.");
        }
        try (OutputStream output = Files.newOutputStream(target)) {
            long received = copy(uri, output, expectedSize, status);
            if (received != expectedSize) throw new IOException("The ZIP download was incomplete.");
        }
    }

    private long copy(URI uri, OutputStream output, long maximum, Consumer<String> status) throws IOException {
        HttpURLConnection connection = open(uri);
        try (InputStream input = connection.getInputStream()) {
            if (connection.getContentLengthLong() > maximum) throw new IOException("Download is too large.");
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            long deadline = System.nanoTime() + java.time.Duration.ofMinutes(5).toNanos();
            long nextProgress = 1024 * 1024;
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Download cancelled.");
                if (System.nanoTime() > deadline) throw new IOException("Download timed out.");
                total += count;
                if (total > maximum) throw new IOException("Download is too large.");
                output.write(buffer, 0, count);
                if (total >= nextProgress) {
                    status.accept("Downloading update: " + total / (1024 * 1024) + " MiB");
                    nextProgress = total + 1024 * 1024;
                }
            }
            return total;
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(URI initial) throws IOException {
        URI uri = initial;
        for (int redirect = 0; redirect <= 5; redirect++) {
            boolean loopback = allowLoopbackHttp && "http".equals(uri.getScheme())
                    && ("127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()));
            if ((!"https".equals(uri.getScheme()) && !loopback) || uri.getHost() == null
                    || uri.getUserInfo() != null) throw new IOException("Update URLs must use HTTPS.");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Space-Invaders-Launcher");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            try {
                int code = connection.getResponseCode();
                if (code == 200) return connection;
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Missing download redirect.");
                    uri = uri.resolve(location);
                } else {
                    throw new HttpFailure(code);
                }
            } catch (IOException | IllegalArgumentException e) {
                connection.disconnect();
                if (e instanceof IOException io) throw io;
                throw new IOException("Invalid download redirect.", e);
            }
            connection.disconnect();
        }
        throw new IOException("Too many download redirects.");
    }

    static final class HttpFailure extends IOException {
        final int status;
        HttpFailure(int status) {
            super(status == 403 || status == 429 ? "GitHub is limiting update requests. Try again later."
                    : "Update server returned HTTP " + status + ".");
            this.status = status;
        }
    }
}
