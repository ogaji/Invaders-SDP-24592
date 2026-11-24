package launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class GameRunner {
    static Process start(Installation.Installed game, Path directory) throws IOException {
        Path saves = directory.resolve("saves");
        Files.createDirectories(saves);
        // Separate JVM: the launcher never loads game classes or locks the active game JAR itself.
        ProcessBuilder builder = new ProcessBuilder(command(game.jar(), saves));
        builder.directory(saves.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("game.log").toFile()));
        return builder.start();
    }

    static List<String> command(Path jar, Path saves) {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
        // Windows Java 17 can lose characters outside the active code page in command-line
        // arguments. The working directory is passed through the Unicode process API;
        // paths within this installation can therefore stay relative and ASCII here.
        Path relativeJar = saves.toAbsolutePath().normalize().relativize(jar.toAbsolutePath().normalize());
        return List.of(java.toString(), "-Dfile.encoding=UTF-8", "-Dinvaders.storageDir=.",
                "-jar", relativeJar.toString());
    }
}
