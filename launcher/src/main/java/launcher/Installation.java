package launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;

/** Immutable game files and a small, atomically replaced pointer to the active one. */
final class Installation implements AutoCloseable {
    record Installed(String version, Path jar, String sha256) { }
    final Path directory;
    private final String repository;
    private final FileChannel channel;
    private final FileLock lock;

    Installation(Path directory, String repository) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        this.repository = repository;
        Files.createDirectories(this.directory);
        channel = FileChannel.open(this.directory.resolve("launcher.lock"), StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            acquired = null;
        } catch (IOException e) {
            channel.close();
            throw e;
        }
        if (acquired == null) {
            channel.close();
            throw new IOException("Another launcher is using this game folder. Close it first.");
        }
        lock = acquired;
        try {
            Path owner = this.directory.resolve("repository.txt");
            if (Files.exists(owner)) {
                if (!Files.readString(owner).strip().equalsIgnoreCase(repository)) {
                    throw new IOException("This game folder belongs to a different repository. Use another --data-dir.");
                }
            } else {
                Files.writeString(owner, repository, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    Optional<Installed> current() throws IOException { return readPointer("current.properties"); }
    Optional<Installed> previous() throws IOException { return readPointer("previous.properties"); }

    private Optional<Installed> readPointer(String name) throws IOException {
        Path pointer = directory.resolve(name);
        if (!Files.exists(pointer)) return Optional.empty();
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(pointer)) { values.load(input); }
        if (!repository.equalsIgnoreCase(values.getProperty("repository", ""))) {
            throw new IOException("This game folder belongs to a different repository. Use another --data-dir.");
        }
        String version = values.getProperty("version", "");
        String hash = values.getProperty("sha256", "");
        if (!safeVersion(version) || !hash.matches("[a-f0-9]{64}")) throw new IOException("Invalid installed version data.");
        Path jar = gamePath(version, hash);
        if (!Files.isRegularFile(jar) || !hash.equals(digest(jar))) {
            throw new IOException("The installed game is missing or damaged.");
        }
        validateJar(jar);
        return Optional.of(new Installed(version, jar, hash));
    }

    Installed install(Path source, String version) throws IOException {
        if (!safeVersion(version)) throw new IOException("Invalid game version.");
        validateJar(source);
        String hash = digest(source);
        Path target = gamePath(version, hash);
        Files.createDirectories(target.getParent());
        if (!Files.exists(target)) {
            Path staged = Files.createTempFile(target.getParent(), "game-", ".part");
            try {
                Files.copy(source, staged, StandardCopyOption.REPLACE_EXISTING);
                if (!hash.equals(digest(staged))) throw new IOException("Game copy failed verification.");
                Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(staged); }
        } else if (!hash.equals(digest(target))) {
            throw new IOException("An installed game file is damaged. Choose a new --data-dir to repair it.");
        }
        Installed installed = new Installed(version, target, hash);
        activate(installed);
        return installed;
    }

    void activate(Installed installed) throws IOException {
        Optional<Installed> old;
        try { old = current(); } catch (IOException e) { old = Optional.empty(); }
        if (old.isPresent() && !old.get().equals(installed)) writePointer("previous.properties", old.get());
        writePointer("current.properties", installed);
    }

    private void writePointer(String name, Installed installed) throws IOException {
        Properties values = new Properties();
        values.setProperty("repository", repository);
        values.setProperty("version", installed.version());
        values.setProperty("sha256", installed.sha256());
        Path staged = Files.createTempFile(directory, "version-", ".part");
        try {
            try (OutputStream output = Files.newOutputStream(staged)) { values.store(output, "Installed game"); }
            try (FileChannel file = FileChannel.open(staged, StandardOpenOption.WRITE)) { file.force(true); }
            Files.move(staged, directory.resolve(name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(staged); }
    }

    private Path gamePath(String version, String hash) {
        return directory.resolve("versions").resolve(version).resolve(hash + ".jar");
    }

    static boolean safeVersion(String value) { return value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}"); }

    static String digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    static void validateJar(Path path) throws IOException {
        if (Files.size(path) > Transfer.MAX_ARCHIVE) throw new IOException("Game JAR is too large.");
        try (JarFile jar = new JarFile(path.toFile())) {
            if (jar.getManifest() == null || !"engine.Core".equals(
                    jar.getManifest().getMainAttributes().getValue("Main-Class"))
                    || jar.getJarEntry("engine/Core.class") == null) {
                throw new IOException("The archive does not contain an executable Space Invaders game.");
            }
            long expanded = 0;
            var entries = jar.entries();
            byte[] buffer = new byte[64 * 1024];
            while (entries.hasMoreElements()) {
                try (InputStream input = jar.getInputStream(entries.nextElement())) {
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        expanded += count;
                        if (expanded > 512L * 1024 * 1024) throw new IOException("Expanded game JAR is too large.");
                    }
                }
            }
        } catch (SecurityException e) { throw new IOException("Game JAR signature verification failed.", e); }
    }

    @Override
    public void close() throws IOException {
        try { lock.release(); } finally { channel.close(); }
    }
}
