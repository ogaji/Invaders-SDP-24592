package launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Launcher {
    private Launcher() { }

    public static void main(String[] args) {
        boolean console = java.util.Arrays.stream(args).anyMatch(arg ->
                arg.equals("--check-only") || arg.equals("--play") || arg.equals("--help") || arg.equals("-h"));
        try {
            Properties config = new Properties();
            try (InputStream input = Launcher.class.getResourceAsStream("/launcher.properties")) {
                if (input == null) throw new IOException("Launcher configuration is missing.");
                config.load(input);
            }
            LaunchOptions options = LaunchOptions.parse(args, config.getProperty("repository"));
            if (options.help()) {
                System.out.println("""
                        Space Invaders Launcher — Java 17+
                        java -jar space-invaders-launcher.jar [options]
                          --repository OWNER/REPO   GitHub repository to update from
                          --data-dir PATH           Game cache and save directory
                          --include-prereleases     Include vMAJOR.MINOR.PATCH-rcN releases
                          --offline                 Use the bundled or installed game without network access
                          --check-only              Update without opening a window or starting the game
                          --play                    Update, then start the game without the launcher window
                          --help                    Show this help
                        Without --check-only or --play, the launcher checks for updates and shows a Play button.
                        """);
                return;
            }
            if (!console && GraphicsEnvironment.isHeadless()) {
                throw new IOException("No graphical display is available. Use --check-only to install an update.");
            }
            Installation installation = new Installation(options.directory(), options.repository());
            try {
                Transfer transfer = new Transfer();
                String bundledVersion = config.getProperty("bundledVersion");
                Path location = Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path bundle = location.getParent().resolve("lib/space-invaders-" + bundledVersion + ".jar");
                // A launcher pointed at another repository must not seed it with this repository's bundle.
                if (!options.repository().equalsIgnoreCase(config.getProperty("repository"))) bundle = null;
                Updater updater = new Updater(installation, new GitHubReleases(options.repository(), transfer),
                        transfer, bundle, bundledVersion);
                if (console) {
                    int code;
                    try (installation) {
                        Updater.Result result = updater.refresh(options.offline(), options.prereleases(), System.out::println);
                        System.out.println(result.message() + " (" + result.installed().version() + ")");
                        if (result.warning() != null) System.err.println(result.warning());
                        code = options.play() ? GameRunner.start(result.installed(), installation.directory).waitFor() : 0;
                    }
                    if (code != 0) System.exit(code);
                } else {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    SwingUtilities.invokeLater(() -> new Window(installation, updater, options).open());
                }
            } catch (Exception e) {
                installation.close();
                throw e;
            }
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.toString() : e.getMessage();
            System.err.println("Launcher: " + message);
            if (!console && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, message, "Space Invaders Launcher", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }
    }

    private static final class Window {
        private final Installation installation;
        private final Updater updater;
        private final LaunchOptions options;
        private final ExecutorService worker = Executors.newSingleThreadExecutor();
        private final JFrame frame = new JFrame("Space Invaders Launcher");
        private final JLabel version = new JLabel("Preparing your game");
        private final JLabel status = new JLabel("Starting...");
        private final JTextArea details = new JTextArea(4, 38);
        private final JProgressBar progress = new JProgressBar();
        private final JButton play = new JButton("Play");
        private final JButton refresh = new JButton("Check for updates");
        private final JCheckBox prereleases = new JCheckBox("Include pre-releases");
        private Installation.Installed game;
        private boolean playing;
        private boolean closing;

        Window(Installation installation, Updater updater, LaunchOptions options) {
            this.installation = installation;
            this.updater = updater;
            this.options = options;
        }

        void open() {
            JPanel panel = new JPanel(new BorderLayout(0, 20));
            panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
            JPanel heading = new JPanel(new BorderLayout(0, 8));
            JLabel title = new JLabel("SPACE INVADERS");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 28));
            heading.add(title, BorderLayout.NORTH);
            heading.add(version, BorderLayout.CENTER);
            heading.add(new JLabel(options.repository()), BorderLayout.SOUTH);
            panel.add(heading, BorderLayout.NORTH);
            JPanel body = new JPanel(new BorderLayout(0, 10));
            body.add(status, BorderLayout.NORTH);
            details.setEditable(false);
            details.setLineWrap(true);
            details.setWrapStyleWord(true);
            details.setBackground(panel.getBackground());
            details.setForeground(new Color(70, 70, 70));
            details.setText("Your game will update automatically when a new release is available.");
            body.add(new JScrollPane(details), BorderLayout.CENTER);
            progress.setIndeterminate(true);
            body.add(progress, BorderLayout.SOUTH);
            panel.add(body, BorderLayout.CENTER);
            JPanel footer = new JPanel(new BorderLayout(0, 12));
            prereleases.setSelected(options.prereleases() || savedPrereleases());
            prereleases.addActionListener(event -> check());
            footer.add(prereleases, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            refresh.addActionListener(event -> check());
            play.setPreferredSize(new Dimension(130, 38));
            play.addActionListener(event -> play());
            buttons.add(refresh);
            buttons.add(play);
            footer.add(buttons, BorderLayout.SOUTH);
            panel.add(footer, BorderLayout.SOUTH);
            frame.setContentPane(panel);
            frame.setMinimumSize(new Dimension(540, 370));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    if (playing) {
                        JOptionPane.showMessageDialog(frame, "Close the game before closing its launcher.");
                        return;
                    }
                    closing = true;
                    frame.dispose();
                    worker.shutdownNow();
                    new Thread(() -> {
                        try {
                            while (!worker.awaitTermination(1, TimeUnit.SECONDS)) { /* Retain the install lock until IO stops. */ }
                            installation.close();
                        } catch (Exception e) { System.err.println(e.getMessage()); }
                    }, "launcher-close").start();
                }
            });
            frame.setVisible(true);
            check();
        }

        private void busy(boolean value) {
            progress.setIndeterminate(value);
            progress.setVisible(value);
            refresh.setEnabled(!value);
            prereleases.setEnabled(!value);
            play.setEnabled(!value && game != null);
        }

        private void check() {
            busy(true);
            boolean candidates = prereleases.isSelected();
            worker.submit(() -> {
                try {
                    try { savePrereleases(candidates); }
                    catch (IOException e) { System.err.println("Could not save launcher preferences: " + e.getMessage()); }
                    Updater.Result result = updater.refresh(options.offline(), candidates,
                            message -> SwingUtilities.invokeLater(() -> status.setText(message)));
                    SwingUtilities.invokeLater(() -> {
                        if (closing) return;
                        game = result.installed();
                        version.setText("Installed version: " + game.version());
                        status.setText(result.message());
                        details.setText(result.warning() == null
                                ? "Ready. Your progress is kept when the game updates." : result.warning());
                        busy(false);
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        if (closing) return;
                        status.setText("Unable to prepare the game");
                        details.setText(e.getMessage());
                        busy(false);
                    });
                }
            });
        }

        private void play() {
            playing = true;
            busy(true);
            progress.setVisible(false);
            status.setText("Game is running");
            worker.submit(() -> {
                String message;
                try {
                    int code = GameRunner.start(game, installation.directory).waitFor();
                    message = code == 0 ? "Game closed. Ready to play again."
                            : "The game exited with code " + code + ". See " + installation.directory.resolve("game.log");
                } catch (IOException | InterruptedException e) { message = "Could not run the game: " + e.getMessage(); }
                String result = message;
                SwingUtilities.invokeLater(() -> {
                    playing = false;
                    status.setText("Ready to play");
                    details.setText(result);
                    busy(false);
                });
            });
        }

        private boolean savedPrereleases() {
            Properties values = new Properties();
            try (InputStream input = Files.newInputStream(installation.directory.resolve("settings.properties"))) {
                values.load(input);
                return Boolean.parseBoolean(values.getProperty("prereleases"));
            } catch (IOException e) { return false; }
        }

        private void savePrereleases(boolean value) throws IOException {
            Properties values = new Properties();
            values.setProperty("prereleases", Boolean.toString(value));
            try (OutputStream output = Files.newOutputStream(installation.directory.resolve("settings.properties"))) {
                values.store(output, "Launcher preferences");
            }
        }
    }
}
