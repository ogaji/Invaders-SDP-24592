package launcher;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

record LaunchOptions(String repository, Path directory, boolean offline, boolean prereleases,
        boolean checkOnly, boolean play, boolean help) {
    static LaunchOptions parse(String[] args, String defaultRepository) {
        String repository = defaultRepository;
        Path directory = null;
        boolean offline = false, prereleases = false, checkOnly = false, play = false, help = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--repository" -> {
                    if (++i >= args.length) throw new IllegalArgumentException("--repository needs owner/repository.");
                    repository = args[i];
                }
                case "--data-dir" -> {
                    if (++i >= args.length) throw new IllegalArgumentException("--data-dir needs a folder path.");
                    directory = Path.of(args[i]);
                }
                case "--offline" -> offline = true;
                case "--include-prereleases" -> prereleases = true;
                case "--check-only" -> checkOnly = true;
                case "--play" -> play = true;
                case "--help", "-h" -> help = true;
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Use a repository name in owner/repository format.");
        }
        if (checkOnly && play) throw new IllegalArgumentException("Choose either --check-only or --play.");
        if (directory == null) directory = defaultDirectory(System.getProperty("os.name"),
                Path.of(System.getProperty("user.home")), System.getenv(), repository);
        return new LaunchOptions(repository, directory.toAbsolutePath().normalize(), offline,
                prereleases, checkOnly, play, help);
    }

    static Path defaultDirectory(String os, Path home, Map<String, String> environment, String repository) {
        String platform = os.toLowerCase(Locale.ROOT);
        Path base;
        if (platform.startsWith("windows")) {
            base = absoluteOr(environment.get("LOCALAPPDATA"), home.resolve("AppData/Local")).resolve("SpaceInvaders");
        } else if (platform.startsWith("mac")) {
            base = home.resolve("Library/Application Support/SpaceInvaders");
        } else {
            base = absoluteOr(environment.get("XDG_DATA_HOME"), home.resolve(".local/share")).resolve("space-invaders");
        }
        return base.resolve(repository.toLowerCase(Locale.ROOT).replace('/', '-'));
    }

    private static Path absoluteOr(String value, Path fallback) {
        return value != null && !value.isBlank() && Path.of(value).isAbsolute() ? Path.of(value) : fallback;
    }
}
