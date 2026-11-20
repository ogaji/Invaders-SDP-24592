package engine;

import java.awt.Color;
import java.awt.Graphics;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import records.Achievement;
import records.RecordsService;
import records.RunRecord;
import screen.Screen;
import vfx.RankBadge;

/** Draws score results, high scores, records, and achievements. */
final class ResultsRecordsRenderer {
    private final DrawContext context;

    ResultsRecordsRenderer(final DrawContext context) {
        this.context = context;
    }

    private int getNormalGameOverTitleY(final Screen screen) {
        int regularHeight = context.regularMetrics().getHeight();
        int bigHeight = context.bigMetrics().getHeight();
        int promptY = screen.getHeight() / 2 + regularHeight * 10;
        int titleToAccuracy = bigHeight + regularHeight * 8;

        return (promptY - context.regularMetrics().getAscent() - titleToAccuracy
                - context.regularMetrics().getDescent()
                + context.bigMetrics().getAscent()) / 2;
    }

    private int getAchievementResultsTitleY(final Screen screen,
            final boolean isNewRecord, final int failedStage,
            final int achievementCount) {
        int spacing = context.regularMetrics().getHeight();
        int lastLineOffset = 0;

        if (failedStage > 0) {
            lastLineOffset += spacing;
        }
        lastLineOffset += spacing * 2;
        lastLineOffset += spacing * 4;
        if (isNewRecord) {
            lastLineOffset += spacing;
            lastLineOffset += spacing * 3;
        }
        lastLineOffset += spacing;
        lastLineOffset += spacing * achievementCount;

        int promptY = screen.getHeight() - spacing;
        int availableTop = spacing;
        int availableBottom = promptY - spacing * 2;
        int blockHeight = context.bigMetrics().getAscent() + lastLineOffset
                + context.regularMetrics().getDescent();
        int availableHeight = availableBottom - availableTop;
        int blockTop = availableTop
                + Math.max(0, (availableHeight - blockHeight) / 2);

        return blockTop + context.bigMetrics().getAscent();
    }

    void drawResults(final Screen screen, final int score,
            final int livesRemaining, final int shipsDestroyed,
            final float accuracy, final boolean isNewRecord) {
        String scoreString = String.format("score %04d", score);
        String livesRemainingString = "lives remaining " + livesRemaining;
        String shipsDestroyedString = "enemies destroyed " + shipsDestroyed;
        String accuracyString = String.format("accuracy %.2f%%", accuracy * 100);

        int resultsY = isNewRecord ? screen.getHeight() / 4
                : getNormalGameOverTitleY(screen)
                        + context.bigMetrics().getHeight()
                        + context.regularMetrics().getHeight() * 2;

        context.graphics().setColor(Color.WHITE);
        context.drawCenteredRegularString(screen, scoreString, resultsY);
        context.drawCenteredRegularString(screen, livesRemainingString,
                resultsY + context.regularMetrics().getHeight() * 2);
        context.drawCenteredRegularString(screen, shipsDestroyedString,
                resultsY + context.regularMetrics().getHeight() * 4);
        context.drawCenteredRegularString(screen, accuracyString,
                resultsY + context.regularMetrics().getHeight() * 6);
    }

    void drawGameOver(final Screen screen, final boolean acceptsInput,
            final boolean isNewRecord, final boolean gameCleared) {
        String gameOverString = gameCleared ? "Game Clear" : "Game Over";
        int gameOverY = isNewRecord ? screen.getHeight() / 4
                - context.bigMetrics().getHeight() * 2
                : getNormalGameOverTitleY(screen);

        context.graphics().setColor(Color.GREEN);
        context.drawCenteredBigString(screen, gameOverString, gameOverY);
        context.graphics().setColor(acceptsInput ? Color.GREEN : Color.GRAY);
        context.drawCenteredRegularString(screen,
                "Press Space to play again, Escape to exit",
                screen.getHeight() / 2
                        + context.regularMetrics().getHeight() * 10);
    }

    void drawStageFailed(final Screen screen, final int stage,
            final boolean isNewRecord) {
        int stageFailedY = isNewRecord ? screen.getHeight() / 4
                - context.bigMetrics().getHeight()
                : getNormalGameOverTitleY(screen)
                        + context.bigMetrics().getHeight();
        context.graphics().setColor(Color.RED);
        context.drawCenteredRegularString(screen, "Stage " + stage + " Failed",
                stageFailedY);
    }

    void drawModeLabel(final Screen screen, final String modeLabel,
            final boolean isNewRecord) {
        int height = isNewRecord ? 4 : 2;
        context.graphics().setColor(Color.WHITE);
        context.drawCenteredRegularString(screen, modeLabel,
                screen.getHeight() / height
                        + context.regularMetrics().getHeight() * 8);
    }

    void drawHighScoreMenu(final Screen screen) {
        context.graphics().setColor(Color.GREEN);
        context.drawCenteredBigString(screen, "High Scores",
                screen.getHeight() / 8);
        context.graphics().setColor(Color.GRAY);
        context.drawCenteredRegularString(screen, "Press Space to return",
                screen.getHeight() / 5);
    }

    void drawHighScores(final Screen screen, final List<Score> highScores) {
        context.graphics().setColor(Color.WHITE);
        int i = 0;
        for (Score score : highScores) {
            String scoreString = String.format("%s        %04d", score.getName(),
                    score.getScore());
            int rowY = screen.getHeight() / 4
                    + context.regularMetrics().getHeight() * (i + 1) * 2;
            context.drawCenteredRegularString(screen, scoreString, rowY);
            drawColorGrid(RankBadge.forRank(i + 1), screen.getWidth() / 2
                    - context.regularMetrics().stringWidth(scoreString) / 2
                    - RankBadge.WIDTH * 2 - 12,
                    rowY - 5 - RankBadge.HEIGHT - 4, 2);
            i++;
        }
    }

    private void drawColorGrid(final Color[][] pixels, final int positionX,
            final int positionY, final int scale) {
        Graphics graphics = context.graphics();
        Color previous = graphics.getColor();
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                if (pixels[i][j] != null) {
                    graphics.setColor(pixels[i][j]);
                    graphics.fillRect(positionX + i * scale,
                            positionY + j * scale, scale, scale);
                }
            }
        }
        graphics.setColor(previous);
    }

    void drawNameInput(final Screen screen, final char[] name,
            final int nameCharSelected) {
        drawNameInputAt(screen, name, nameCharSelected,
                screen.getHeight() / 4
                        + context.regularMetrics().getHeight() * 10,
                context.regularMetrics().getHeight() * 2);
    }

    private void drawNameInputAt(final Screen screen, final char[] name,
            final int nameCharSelected, final int y, final int spacing) {
        Graphics graphics = context.graphics();
        graphics.setColor(Color.GREEN);
        context.drawCenteredRegularString(screen, "New Record!", y);
        graphics.setColor(Color.WHITE);
        context.drawCenteredRegularString(screen, "Introduce name:", y + spacing);

        int positionX = screen.getWidth() / 2
                - (context.regularMetrics().getWidths()[name[0]]
                        + context.regularMetrics().getWidths()[name[1]]
                        + context.regularMetrics().getWidths()[name[2]]
                        + context.regularMetrics().getWidths()[' ']) / 2;
        for (int i = 0; i < 3; i++) {
            graphics.setColor(i == nameCharSelected ? Color.GREEN : Color.WHITE);
            positionX += context.regularMetrics().getWidths()[name[i]] / 2;
            positionX = i == 0 ? positionX
                    : positionX + (context.regularMetrics().getWidths()[name[i - 1]]
                            + context.regularMetrics().getWidths()[' ']) / 2;
            graphics.drawString(Character.toString(name[i]), positionX,
                    y + spacing * 2);
        }
    }

    void drawAchievementNotice(final Screen screen,
            final Achievement achievement) {
        context.graphics().setColor(Color.GREEN);
        context.drawCenteredRegularString(screen,
                "Achievement unlocked: " + achievement.getName(),
                screen.getHeight() / 7);
        context.graphics().setColor(Color.WHITE);
        context.drawCenteredRegularString(screen, achievement.getCondition(),
                screen.getHeight() / 7 + context.regularMetrics().getHeight());
    }

    void drawAchievementResults(final Screen screen, final int score,
            final int lives, final int enemies, final float accuracy,
            final boolean isNewRecord, final char[] name, final int selected,
            final List<Achievement> achievements, final boolean acceptsInput,
            final int failedStage) {
        drawAchievementResults(screen, score, lives, enemies, accuracy,
                isNewRecord, name, selected, achievements, acceptsInput,
                failedStage, false);
    }

    void drawAchievementResults(final Screen screen, final int score,
            final int lives, final int enemies, final float accuracy,
            final boolean isNewRecord, final char[] name, final int selected,
            final List<Achievement> achievements, final boolean acceptsInput,
            final int failedStage, final boolean gameCleared) {
        drawAchievementResults(screen, score, lives, enemies, accuracy,
                isNewRecord, name, selected, achievements, acceptsInput,
                failedStage, gameCleared, null);
    }

    void drawAchievementResults(final Screen screen, final int score,
            final int lives, final int enemies, final float accuracy,
            final boolean isNewRecord, final char[] name, final int selected,
            final List<Achievement> achievements, final boolean acceptsInput,
            final int failedStage, final boolean gameCleared,
            final String modeLabel) {
        int spacing = context.regularMetrics().getHeight();
        int y = getAchievementResultsTitleY(screen, isNewRecord, failedStage,
                achievements.size());
        context.graphics().setColor(Color.GREEN);
        context.drawCenteredBigString(screen,
                gameCleared ? "Game Cleared!" : "Game Over", y);
        if (failedStage > 0) {
            y += spacing;
            context.graphics().setColor(Color.RED);
            context.drawCenteredRegularString(screen,
                    "Stage " + failedStage + " Failed", y);
        }
        y += spacing * 2;
        context.graphics().setColor(Color.WHITE);
        String[] results = { "Score " + score, "Lives remaining " + lives,
                "Enemies destroyed " + enemies,
                String.format("Accuracy %.2f%%", accuracy * 100) };
        if (modeLabel != null) {
            results = new String[] { results[0], results[1], results[2],
                    results[3], modeLabel };
        }
        for (String result : results) {
            context.drawCenteredRegularString(screen, result, y);
            y += spacing;
        }
        if (isNewRecord) {
            y += spacing;
            drawNameInputAt(screen, name, selected, y, spacing);
            y += spacing * 3;
        }
        y += spacing;
        context.graphics().setColor(Color.GREEN);
        context.drawCenteredRegularString(screen, "New achievements:", y);
        context.graphics().setColor(Color.WHITE);
        for (Achievement achievement : achievements) {
            y += spacing;
            context.drawCenteredRegularString(screen, achievement.getName(), y);
        }
        context.graphics().setColor(acceptsInput ? Color.GREEN : Color.GRAY);
        context.drawCenteredRegularString(screen,
                "Space: play again   Esc: menu", screen.getHeight() - spacing);
    }

    void drawAchievementList(final Screen screen, final RecordsService service,
            int y) {
        for (Achievement achievement : Achievement.values()) {
            Long unlocked = service.getUnlockTime(achievement);
            context.graphics().setColor(
                    unlocked == null ? Color.WHITE : Color.GREEN);
            context.drawCenteredRegularString(screen,
                    achievement.getName() + (unlocked == null ? " [Locked]"
                            : " [Unlocked]"),
                    y);
            context.graphics().setColor(Color.GRAY);
            context.drawCenteredRegularString(screen, achievement.getCondition(),
                    y + context.regularMetrics().getHeight());
            if (unlocked != null) {
                context.drawCenteredRegularString(screen, "Unlocked: "
                        + DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                DateFormat.SHORT)
                                .format(new Date(unlocked)),
                        y + context.regularMetrics().getHeight() * 2);
            }
            y += context.regularMetrics().getHeight() * 3 + 4;
        }
    }

    void drawRecordsPage(final Screen screen, final RecordsService service,
            final int tab) {
        context.graphics().setColor(Color.GREEN);
        context.drawCenteredBigString(screen,
                tab == 0 ? "Run Records" : "Achievements",
                screen.getHeight() / 9);
        context.graphics().setColor(Color.GRAY);
        context.drawCenteredRegularString(screen,
                "Left/Right: switch   Esc/Space: return",
                screen.getHeight() / 6);
        int y = screen.getHeight() / 6
                + context.regularMetrics().getHeight() * 2;
        if (tab == 0) {
            context.graphics().setColor(Color.WHITE);
            context.drawCenteredRegularString(screen, "Personal bests", y);
            if (service.getRecordCount() == 0) {
                context.drawCenteredRegularString(screen, "No completed runs yet.",
                        y + context.regularMetrics().getHeight());
            } else {
                context.drawCenteredRegularString(screen,
                        "Highest score: " + service.getHighestScore(),
                        y + context.regularMetrics().getHeight());
                context.drawCenteredRegularString(screen,
                        "Most enemies: " + service.getMostEnemiesDefeated(),
                        y + context.regularMetrics().getHeight() * 2);
                context.drawCenteredRegularString(screen,
                        "Longest survival: "
                                + service.getLongestSurvivalMillis() / 1000.0
                                + "s",
                        y + context.regularMetrics().getHeight() * 3);
            }
            y += context.regularMetrics().getHeight() * 4;
            context.drawCenteredRegularString(screen, "Top completed runs", y);
            List<RunRecord> runs = service.getTopRuns();
            if (runs.isEmpty()) {
                context.drawCenteredRegularString(screen, "No completed runs yet.",
                        y + context.regularMetrics().getHeight() * 2);
            } else {
                int index = 1;
                for (RunRecord run : runs) {
                    y += context.regularMetrics().getHeight();
                    context.drawCenteredRegularString(screen, String.format(
                            "%2d. %d pts  %d enemies  %.1fs", index++,
                            run.getScore(), run.getEnemiesDefeated(),
                            run.getActiveDurationMillis() / 1000.0), y);
                }
            }
        } else {
            drawAchievementList(screen, service, y);
        }
        context.graphics().setColor(Color.GRAY);
        String status = service.getSaveError();
        String message = status == null
                ? service.getLoadWarning() == null ? "Profile ready"
                        : "Profile restored from backup"
                : status.startsWith("Saving") ? "Saving profile..."
                        : "Profile error - check game log";
        context.drawCenteredRegularString(screen, message,
                screen.getHeight() - context.regularMetrics().getHeight());
    }
}
