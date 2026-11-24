package engine;

/** A real child JVM entry point for testing launcher arguments and persistent storage. */
public final class Core {
    public static void main(String[] args) throws Exception {
        java.nio.file.Path saves = java.nio.file.Path.of(System.getProperty("invaders.storageDir"));
        java.nio.file.Files.writeString(saves.resolve("child-process.txt"),
                java.nio.file.Path.of("").toAbsolutePath().toString());
    }
}
