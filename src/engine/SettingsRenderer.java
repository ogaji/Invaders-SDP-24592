package engine;

import java.awt.Color;
import java.awt.Graphics;

import screen.Screen;
import vfx.Icon;

/** Draws settings screens while DrawManager retains their hit testing. */
final class SettingsRenderer {
    static final String[] SETTINGS_MENU_OPTIONS = { "Audio", "Controls",
            "Gameplay", "Back" };
    static final String[] GAMEPLAY_SETTINGS_OPTIONS = { "Easy", "Normal",
            "Hard" };
    static final int AUDIO_COLUMN_WIDTH = 200;
    static final int AUDIO_BAR_GAP = 6;
    static final int AUDIO_BAR_HEIGHT = 8;

    private final DrawContext context;

    SettingsRenderer(final DrawContext context) {
        this.context = context;
    }

    void drawSettingsMenu(final Screen screen, final int selectedOption) {
        Graphics graphics = context.graphics();
        Icon[] icons = { Icon.AUDIO, Icon.CONTROLS, Icon.GAMEPLAY, Icon.BACK };

        graphics.setColor(Color.GREEN);
        context.drawCenteredBigString(screen, "Settings", screen.getHeight() / 5);
        context.drawIcon(Icon.SETTINGS, screen.getWidth() / 2
                - context.bigMetrics().stringWidth("Settings") / 2
                - Icon.SETTINGS.getWidth() * 3 - 10,
                screen.getHeight() / 5 - 22, 3, Color.GREEN);

        for (int i = 0; i < SETTINGS_MENU_OPTIONS.length; i++) {
            boolean selected = i == selectedOption;
            int labelY = screen.getHeight() / 2
                    + i * context.regularMetrics().getHeight() * 2;
            graphics.setColor(selected ? Color.GREEN : Color.WHITE);
            context.drawCenteredRegularString(screen, SETTINGS_MENU_OPTIONS[i],
                    labelY);
            context.drawMenuIcon(screen, icons[i], SETTINGS_MENU_OPTIONS[i],
                    labelY, selected);
        }

        graphics.setColor(Color.GRAY);
        context.drawCenteredRegularString(screen,
                "Up/Down: select  Space: confirm", screen.getHeight() / 8 * 7);
    }

    void drawControlsSettings(final Screen screen, final int selectedOption,
            final String[] labels) {
        Graphics graphics = context.graphics();
        graphics.setColor(Color.GREEN);
        context.drawCenteredBigString(screen, "Controls", screen.getHeight() / 5);

        for (int i = 0; i < labels.length; i++) {
            graphics.setColor(i == selectedOption ? Color.GREEN : Color.WHITE);
            context.drawCenteredRegularString(screen, labels[i],
                    screen.getHeight() / 2
                            + i * context.regularMetrics().getHeight() * 2);
        }

        graphics.setColor(Color.GRAY);
        context.drawCenteredRegularString(screen,
                "Up/Down: select  Space: change", screen.getHeight() / 8 * 7);
    }

    void drawGameplaySettings(final Screen screen, final int selectedOption) {
        Graphics graphics = context.graphics();
        graphics.setColor(Color.GREEN);
        context.drawCenteredBigString(screen, "Gameplay", screen.getHeight() / 5);

        for (int i = 0; i < GAMEPLAY_SETTINGS_OPTIONS.length; i++) {
            graphics.setColor(i == selectedOption ? Color.GREEN : Color.WHITE);
            context.drawCenteredRegularString(screen,
                    GAMEPLAY_SETTINGS_OPTIONS[i], screen.getHeight() / 2
                            + i * context.regularMetrics().getHeight() * 2);
        }

        graphics.setColor(Color.GRAY);
        context.drawCenteredRegularString(screen,
                "Up/Down: select  Space: confirm", screen.getHeight() / 8 * 7);
    }

    void drawAudioSettings(final Screen screen, final int option,
            final int bgmVolume, final int sfxVolume, final boolean muted) {
        Graphics graphics = context.graphics();
        String[] optionStrings = { "BGM Volume", "SFX Volume", "Mute" };
        String[] valueStrings = { Integer.toString(bgmVolume),
                Integer.toString(sfxVolume), muted ? "ON" : "OFF" };
        int[] barVolumes = { bgmVolume, sfxVolume };

        int columnLeft = screen.getWidth() / 2 - AUDIO_COLUMN_WIDTH / 2;
        int firstOptionY = screen.getHeight() / 3;
        int optionSpacing = context.regularMetrics().getHeight() * 4;
        int muteIndex = optionStrings.length - 1;
        int valueRight = columnLeft + AUDIO_COLUMN_WIDTH;
        int valueLeft = valueRight
                - context.regularMetrics().stringWidth("< 100 >");
        int valueCenter = (valueLeft + valueRight) / 2;

        graphics.setColor(Color.GREEN);
        context.drawCenteredBigString(screen, "Audio Settings",
                screen.getHeight() / 8);

        graphics.setColor(Color.GRAY);
        context.drawCenteredRegularString(screen, "Up/Down: select",
                screen.getHeight() / 5);
        context.drawCenteredRegularString(screen, "Left/Right: adjust  M: mute",
                screen.getHeight() / 5 + context.regularMetrics().getHeight());

        graphics.setFont(context.regularFont());
        for (int i = 0; i < optionStrings.length; i++) {
            graphics.setColor(i == option ? Color.GREEN : Color.WHITE);
            graphics.drawString(optionStrings[i], columnLeft,
                    firstOptionY + optionSpacing * i);
            graphics.drawString(valueStrings[i], valueCenter
                    - context.regularMetrics().stringWidth(valueStrings[i]) / 2,
                    firstOptionY + optionSpacing * i);
            if (i != muteIndex) {
                graphics.drawString("<", valueLeft,
                        firstOptionY + optionSpacing * i);
                graphics.drawString(">", valueRight
                        - context.regularMetrics().stringWidth(">"),
                        firstOptionY + optionSpacing * i);
            }
            if (muted && i != muteIndex) {
                int numberWidth = context.regularMetrics()
                        .stringWidth(valueStrings[i]);
                graphics.fillRect(valueCenter - numberWidth / 2 - 2,
                        firstOptionY + optionSpacing * i
                                - context.regularMetrics().getAscent() / 2,
                        numberWidth + 4, 2);
            }
            if (i < barVolumes.length) {
                Color barColor = muted ? Color.GRAY
                        : i == option ? Color.GREEN : Color.WHITE;
                drawVolumeBar(columnLeft,
                        firstOptionY + optionSpacing * i + AUDIO_BAR_GAP,
                    barVolumes[i],
                        barColor);
            }
        }

        graphics.setColor(Color.GRAY);
        context.drawCenteredRegularString(screen, "Press Esc to return",
                screen.getHeight() / 8 * 7);
    }

    private void drawVolumeBar(final int positionX, final int positionY,
                               final int volume,
            final Color color) {
        Graphics graphics = context.graphics();
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(positionX, positionY, SettingsRenderer.AUDIO_COLUMN_WIDTH, SettingsRenderer.AUDIO_BAR_HEIGHT);
        graphics.setColor(color);
        graphics.fillRect(positionX, positionY, SettingsRenderer.AUDIO_COLUMN_WIDTH * volume / 100, SettingsRenderer.AUDIO_BAR_HEIGHT);
    }
}
