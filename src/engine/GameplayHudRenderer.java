package engine;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import entity.Entity;
import entity.Grenade;
import entity.GrenadeFragment;
import entity.Ship;
import entity.item.ClusterGrenadeItem;
import hud.FloatingScoreHud;
import screen.Screen;
import vfx.Icon;
import vfx.Particle;
import vfx.ParticleSystem;

/** Draws in-game HUD elements, overlays, and effects. */
final class GameplayHudRenderer {
    private static final String[] CREDIT_SUFFIXES = { "", "K", "M", "B",
            "T", "Q" };

    private final DrawContext context;

    GameplayHudRenderer(final DrawContext context) {
        this.context = context;
    }

    void drawScore(final Screen screen, final int score) {
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont());
        graphics.setColor(Color.WHITE);
        String scoreString = String.format("%04d", score);
        int scoreX = screen.getWidth() - 60;
        context.drawIcon(Icon.SCORE, scoreX - Icon.SCORE.getWidth() * 2 - 6,
                12, 2);
        graphics.drawString(scoreString, scoreX, 25);
    }

    void drawCredits(final Screen screen, final long credits) {
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont());
        graphics.setColor(Color.WHITE);
        String text = shortenCredits(credits);
        int textX = screen.getWidth() - 60 - Icon.SCORE.getWidth() * 2 - 14
                - graphics.getFontMetrics().stringWidth(text);
        context.drawIcon(Icon.COIN,
                textX - 4 - Icon.COIN.getWidth() * 2, 12, 2);
        graphics.drawString(text, textX, 25);
    }

    private static String shortenCredits(final long credits) {
        if (credits < 10000) {
            return Long.toString(credits);
        }
        long unit = 1;
        int suffix = 0;
        while (suffix < CREDIT_SUFFIXES.length - 1
                && credits / unit >= 1000) {
            unit *= 1000;
            suffix++;
        }
        long whole = credits / unit;
        return whole + (whole < 100 ? "." + credits % unit / (unit / 10) : "")
                + CREDIT_SUFFIXES[suffix];
    }

    void drawLives(final int lives) {
        drawLives(lives, lives);
    }

    void drawLives(final int lives, final int maxLives) {
        for (int i = 0; i < Math.max(lives, maxLives); i++) {
            context.drawIcon(i < lives ? Icon.LIFE : Icon.LIFE_LOST,
                    20 + 22 * i, 12, 2);
        }
    }

    void drawLevel(final Screen screen, final int level, final int totalLevels) {
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont());
        graphics.setColor(Color.WHITE);
        String levelString = "LEVEL " + level + "/" + totalLevels;
        int textWidth = graphics.getFontMetrics().stringWidth(levelString);
        int levelX = (screen.getWidth() - textWidth) / 2;
        graphics.drawString(levelString, levelX, 25);
    }

    void drawHorizontalLine(final Screen screen, final int positionY) {
        Graphics graphics = context.graphics();
        graphics.setColor(Color.GREEN);
        graphics.drawLine(0, positionY, screen.getWidth(), positionY);
        graphics.drawLine(0, positionY + 1, screen.getWidth(), positionY + 1);
    }

    void drawOrbitalAnnihilator(final Ship ship, final int beamWidth,
            final int topY, final boolean firing, final float chargeProgress) {
        Graphics graphics = context.graphics();
        int centerX = ship.getPositionX() + ship.getWidth() / 2;
        int startY = ship.getPositionY();
        int beamX = centerX - beamWidth / 2;

        if (firing) {
            graphics.setColor(new Color(255, 0, 0, 120));
            graphics.fillRect(beamX - 4, topY, beamWidth + 8, startY - topY);
            graphics.setColor(Color.RED);
            graphics.fillRect(beamX, topY, beamWidth, startY - topY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(centerX - beamWidth / 4, topY, beamWidth / 2,
                    startY - topY);
        } else {
            graphics.setColor(new Color(255, 0, 0, 90));
            graphics.fillRect(centerX - 1, topY, 2, startY - topY);
            int radius = 2 + (int) (chargeProgress * beamWidth / 2);
            boolean flicker = ((int) (chargeProgress * 20)) % 2 == 0;
            graphics.setColor(flicker ? Color.WHITE : Color.RED);
            graphics.fillOval(centerX - radius, startY - radius, radius * 2,
                    radius * 2);
        }
    }

    void drawAnnihilatorGauge(final Screen screen, final float progress) {
        Graphics graphics = context.graphics();
        int barWidth = 80;
        int barX = screen.getWidth() - barWidth - 20;
        int barY = 29;
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(barX, barY, barWidth, 6);
        graphics.setColor(progress >= 1f ? Color.GREEN : Color.RED);
        graphics.fillRect(barX, barY, (int) (barWidth * progress), 6);
    }

    void drawPaused(final Screen screen) {
        int scale = 4;
        context.drawIcon(Icon.PAUSE,
                screen.getWidth() / 2 - Icon.PAUSE.getWidth() * scale / 2,
                screen.getHeight() / 2 - 50, scale);
        context.graphics().setColor(GameColors.TEXT);
        context.drawCenteredBigString(screen, "Paused", screen.getHeight() / 2 + 20);
    }

    void drawCountDown(final Screen screen, final int level, final int number,
            final boolean bonusLife) {
        String title = "Level " + level;
        if (bonusLife) {
            title += " - Bonus life!";
        }
        drawCountDown(screen, title, number);
    }

    void drawCountDown(final Screen screen, final String title,
            final int number) {
        Graphics graphics = context.graphics();
        int rectWidth = screen.getWidth();
        int rectHeight = screen.getHeight() / 6;
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, screen.getHeight() / 2 - rectHeight / 2,
                rectWidth, rectHeight);
        graphics.setColor(Color.GREEN);
        int titleY = screen.getHeight() / 2 + context.bigMetrics().getHeight() / 3;
        if (number >= 4) {
            context.drawCenteredBigString(screen, title, titleY);
        } else if (number != 0) {
            context.drawCenteredBigString(screen, Integer.toString(number),
                    titleY);
        } else {
            context.drawCenteredBigString(screen, "GO!", titleY);
        }
    }

    void drawGrenadeItem(final int x, final int y) {
        context.graphics().setColor(Color.WHITE);
        drawSprite(ClusterGrenadeItem.getSprite(), x, y);
    }

    void drawGrenade(final Grenade grenade) {
        Graphics graphics = context.graphics();
        int x = grenade.getPositionX();
        int y = grenade.getPositionY();
        int w = grenade.getWidth();
        int h = grenade.getHeight();
        graphics.setColor(new Color(255, 140, 0, 50));
        graphics.fillOval(x - 10, y - 10, w + 20, h + 20);
        graphics.setColor(new Color(255, 170, 0, 100));
        graphics.fillOval(x - 5, y - 5, w + 10, h + 10);
        graphics.setColor(grenade.getColor());
        drawSprite(Grenade.getSprite(), x, y);
    }

    void drawGrenadeFragment(final GrenadeFragment fragment) {
        Graphics graphics = context.graphics();
        int x = fragment.getPositionX();
        int y = fragment.getPositionY();
        int cx = x + fragment.getWidth() / 2;
        int cy = y + fragment.getHeight() / 2;
        double fade = 1.0 - 0.6 * fragment.getProgress();
        graphics.setColor(new Color(255, 140, 0, (int) (60 * fade)));
        graphics.fillOval(cx - 11, cy - 11, 22, 22);
        graphics.setColor(new Color(255, 190, 0, (int) (110 * fade)));
        graphics.fillOval(cx - 7, cy - 7, 14, 14);
        graphics.setColor(fragment.getColor());
        drawSprite(GrenadeFragment.getSprite(), x, y);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(cx - 1, cy - 1, 2, 2);
    }

    private void drawSprite(final boolean[][] image, final int x, final int y) {
        Graphics graphics = context.graphics();
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (image[i][j]) {
                    graphics.drawRect(x + i * 2, y + j * 2, 1, 1);
                }
            }
        }
    }

    void drawGrenadeCount(final int grenades) {
        if (grenades <= 0) {
            return;
        }
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont());
        graphics.setColor(Color.ORANGE);
        graphics.drawString("GRENADE [G] x" + grenades, 10, 55);
    }

    void drawBuffs(final Screen screen, final List<String> lines) {
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont());
        graphics.setColor(Color.CYAN);
        int y = 62;
        for (String line : lines) {
            int textWidth = graphics.getFontMetrics().stringWidth(line);
            graphics.drawString(line, screen.getWidth() - textWidth - 10, y);
            y += 20;
        }
    }

    void drawAnnihilatorReady(final Screen screen) {
        boolean blink = (System.currentTimeMillis() / 300) % 2 == 0;
        context.graphics().setColor(blink ? Color.RED : Color.WHITE);
        context.drawCenteredRegularString(screen, "HOLD SPACE = LASER", 78);
    }

    void drawShield(final Entity ship) {
        Graphics graphics = context.graphics();
        graphics.setColor(new Color(0, 200, 255, 120));
        int padding = 4;
        int x = ship.getPositionX() - padding;
        int y = ship.getPositionY() - padding;
        int width = ship.getWidth() + padding * 2;
        int height = ship.getHeight() + padding * 2;
        graphics.fillOval(x, y, width, height);
        graphics.setColor(Color.CYAN);
        graphics.drawOval(x, y, width, height);
    }

    void drawParticles(final ParticleSystem particleSystem) {
        Graphics graphics = context.graphics();
        for (Particle particle : particleSystem.getParticles()) {
            graphics.setColor(particle.getColor());
            graphics.fillRect(particle.getX(), particle.getY(),
                    particle.getSize(), particle.getSize());
        }
    }

    void drawFloatingScores(final FloatingScoreHud hud, final Screen screen) {
        hud.draw((java.awt.Graphics2D) context.graphics(), screen.getWidth(),
                screen.getHeight());
    }
}
