package engine;

import entity.ShipType;
import entity.item.ChameleonHull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import screen.Screen;
import hud.FloatingScoreHud;
import currency.PlayerAssetsState;
import shop.ShopController.SaveStatus;
import shop.ShopProduct;
import records.Achievement;
import records.RecordsService;
import entity.Entity;
import entity.Grenade;
import entity.GrenadeFragment;
import entity.item.AnnihilatorItem;
import entity.Ship;
import entity.Bullet;
import vfx.Icon;
import vfx.ShipSprites;

/**
 * Manages screen drawing.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class DrawManager {
    /** Singleton instance of the class. */
    private static DrawManager instance;
    /** Current frame. */
    private static Frame frame;
    /** FileManager instance. */
    private static FileManager fileManager;
    /** Application logger. */
    private static Logger logger;
    /** Graphics context. */
    private static Graphics graphics;
    /** Buffer Graphics. */
    private static Graphics backBufferGraphics;
    /** Buffer image. */
    private static BufferedImage backBuffer;
    /** Normal sized font. */
    private static Font fontRegular;
    /** Normal sized font properties. */
    private static FontMetrics fontRegularMetrics;
    /** Big sized font. */
    private static Font fontBig;
    /** Big sized font properties. */
    private static FontMetrics fontBigMetrics;

    /** Sprite types mapped to their images. */
    private static Map<SpriteType, boolean[][]> spriteMap;

    /** Shared access to the live drawing surface for feature renderers. */
    private final DrawContext drawContext;
    /** Shop screen renderer. */
    private final ShopRenderer shopRenderer;
    /** Settings screens renderer. */
    private final SettingsRenderer settingsRenderer;
    /** Results, records, and achievements renderer. */
    private final ResultsRecordsRenderer resultsRecordsRenderer;
    /** In-game HUD, effects, and overlays renderer. */
    private final GameplayHudRenderer gameplayHudRenderer;

    /** Sprite types. */
    public static enum SpriteType {
        /** Player ship. */
        Ship,
        /** Destroyed player ship. */
        ShipDestroyed,
        /** Player bullet. */
        Bullet,
        /** Enemy bullet. */
        EnemyBullet,
        /** First enemy ship - first form. */
        EnemyShipA1,
        /** First enemy ship - second form. */
        EnemyShipA2,
        /** Second enemy ship - first form. */
        EnemyShipB1,
        /** Second enemy ship - second form. */
        EnemyShipB2,
        /** Third enemy ship - first form. */
        EnemyShipC1,
        /** Third enemy ship - second form. */
        EnemyShipC2,
        /** Bonus ship. */
        EnemyShipSpecial,
        /** Hyperfire falling power-up. */
        Hyperfire,
        /** Destroyed enemy ship. */
        Explosion,
        /** Falling hazard. */
        FallingObstacle
    }

    /**
     * Private constructor.
     */
    private DrawManager() {
        drawContext = new DrawContext(this);
        shopRenderer = new ShopRenderer(drawContext);
        settingsRenderer = new SettingsRenderer(drawContext);
        resultsRecordsRenderer = new ResultsRecordsRenderer(drawContext);
        gameplayHudRenderer = new GameplayHudRenderer(drawContext);
        fileManager = Core.getFileManager();
        logger = Core.getLogger();
        logger.info("Started loading resources.");

        try {
            spriteMap = new LinkedHashMap<SpriteType, boolean[][]>();

            spriteMap.put(SpriteType.Ship, new boolean[13][8]);
            spriteMap.put(SpriteType.ShipDestroyed, new boolean[13][8]);
            spriteMap.put(SpriteType.Bullet, new boolean[3][5]);
            spriteMap.put(SpriteType.EnemyBullet, new boolean[3][5]);
            spriteMap.put(SpriteType.EnemyShipA1, new boolean[12][8]);
            spriteMap.put(SpriteType.EnemyShipA2, new boolean[12][8]);
            spriteMap.put(SpriteType.EnemyShipB1, new boolean[12][8]);
            spriteMap.put(SpriteType.EnemyShipB2, new boolean[12][8]);
            spriteMap.put(SpriteType.EnemyShipC1, new boolean[12][8]);
            spriteMap.put(SpriteType.EnemyShipC2, new boolean[12][8]);
            spriteMap.put(SpriteType.EnemyShipSpecial, new boolean[16][7]);
            spriteMap.put(SpriteType.Explosion, new boolean[13][7]);
            spriteMap.put(SpriteType.FallingObstacle, new boolean[13][7]);

            fileManager.loadSprite(spriteMap);
            logger.info("Finished loading the sprites.");

            // Hyperfire lightning bolt sprite (8 x 12 pixels), built in code
            // because it is not part of the graphics file.
            boolean[][] hyperfireSprite = new boolean[8][12];
            String[] lightning = {
                    "....##..",
                    "...###..",
                    "...##...",
                    "..###...",
                    "..##....",
                    ".######.",
                    "...##...",
                    "...##...",
                    "..##....",
                    "..##....",
                    ".##.....",
                    ".#......"
            };
            for (int y = 0; y < lightning.length; y++)
                for (int x = 0; x < lightning[y].length(); x++)
                    hyperfireSprite[x][y] = lightning[y].charAt(x) == '#';
            spriteMap.put(SpriteType.Hyperfire, hyperfireSprite);

            // Font loading.
            fontRegular = fileManager.loadFont(14f);
            fontBig = fileManager.loadFont(24f);
            logger.info("Finished loading the fonts.");

        } catch (IOException e) {
            logger.warning("Loading failed.");
        } catch (FontFormatException e) {
            logger.warning("Font formating failed.");
        }
    }

    /**
     * Returns shared instance of DrawManager.
     *
     * @return Shared instance of DrawManager.
     */
    static DrawManager getInstance() {
        if (instance == null)
            instance = new DrawManager();
        return instance;
    }

    /**
     * Sets the frame to draw the image on.
     *
     * @param currentFrame
     *                     Frame to draw on.
     */
    public void setFrame(final Frame currentFrame) {
        frame = currentFrame;
    }

    /**
     * Draws an entity, using the appropriate image.
     *
     * @param entity
     *                  Entity to be drawn.
     * @param positionX
     *                  Coordinates for the left side of the image.
     * @param positionY
     *                  Coordinates for the upper side of the image.
     */
    public void drawEntity(final Entity entity, final int positionX,
            final int positionY) {
        if (entity instanceof AnnihilatorItem) {
            drawFallingLaser(entity, positionX, positionY);
            return;
        }

        boolean[][] image = spriteMap.get(entity.getSpriteType());

        if (entity instanceof ChameleonHull) {
            image = ChameleonHull.getStarSprite();
        }

        if (entity instanceof Ship
                && entity.getSpriteType() == SpriteType.Ship) {
            image = ShipSprites.forType(((Ship) entity).getShipType());
        }

        if (entity instanceof Bullet
                && entity.getSpriteType() == SpriteType.Bullet) {
            drawPixels(ShipSprites.playerBullet(), positionX, positionY);
            return;
        }

        if (entity instanceof Bullet
                && entity.getSpriteType() == SpriteType.EnemyBullet) {
            drawPixels(ShipSprites.enemyBullet(), positionX, positionY);
            return;
        }

        if (entity instanceof Ship
                && ((Ship) entity).isChameleonHullActive()) {
            image = ChameleonHull.getBallSprite();
        }

        backBufferGraphics.setColor(entity.getColor());
        for (int i = 0; i < image.length; i++)
            for (int j = 0; j < image[i].length; j++)
                if (image[i][j])
                    backBufferGraphics.drawRect(positionX + i * 2, positionY
                            + j * 2, 1, 1);
    }

    /**
     * Draws multicolored sprite pixels at the gameplay sprite scale.
     *
     * @param pixels
     *                  Pixel colors indexed [x][y]; null where empty.
     * @param positionX
     *                  Coordinates for the left side of the image.
     * @param positionY
     *                  Coordinates for the upper side of the image.
     */
    private void drawPixels(final Color[][] pixels, final int positionX,
            final int positionY) {
        for (int i = 0; i < pixels.length; i++)
            for (int j = 0; j < pixels[i].length; j++)
                if (pixels[i][j] != null) {
                    backBufferGraphics.setColor(pixels[i][j]);
                    backBufferGraphics.drawRect(positionX + i * 2,
                            positionY + j * 2, 1, 1);
                }
    }

    /**
     * For debugging purposes, draws the canvas borders.
     *
     * @param screen
     *               Screen to draw in.
     */
    @SuppressWarnings("unused")
    private void drawBorders(final Screen screen) {
        backBufferGraphics.setColor(Color.GREEN);
        backBufferGraphics.drawLine(0, 0, screen.getWidth() - 1, 0);
        backBufferGraphics.drawLine(0, 0, 0, screen.getHeight() - 1);
        backBufferGraphics.drawLine(screen.getWidth() - 1, 0,
                screen.getWidth() - 1, screen.getHeight() - 1);
        backBufferGraphics.drawLine(0, screen.getHeight() - 1,
                screen.getWidth() - 1, screen.getHeight() - 1);
    }

    /**
     * For debugging purposes, draws a grid over the canvas.
     *
     * @param screen
     *               Screen to draw in.
     */
    @SuppressWarnings("unused")
    private void drawGrid(final Screen screen) {
        backBufferGraphics.setColor(Color.DARK_GRAY);
        for (int i = 0; i < screen.getHeight() - 1; i += 2)
            backBufferGraphics.drawLine(0, i, screen.getWidth() - 1, i);
        for (int j = 0; j < screen.getWidth() - 1; j += 2)
            backBufferGraphics.drawLine(j, 0, j, screen.getHeight() - 1);
    }

    /**
     * Draws current score on screen.
     *
     * @param screen
     *               Screen to draw on.
     * @param score
     *               Current score.
     */
    public void drawScore(final Screen screen, final int score) {
        gameplayHudRenderer.drawScore(screen, score);
    }

    /**
     * Draws credits with a coin icon right-aligned just left of the score.
     * Up to 4 digits fit between the level label and the score.
     */
    public void drawCredits(final Screen screen, final long credits) {
        gameplayHudRenderer.drawCredits(screen, credits);
    }

    /**
     * Draws remaining lives as heart icons on screen.
     *
     * @param screen
     *               Screen to draw on.
     * @param lives
     *               Current lives.
     */
    public void drawLives(final Screen screen, final int lives) {
        gameplayHudRenderer.drawLives(lives);
    }

    /**
     * Draws remaining lives as heart icons and lost lives as broken hearts.
     *
     * @param screen
     *                 Screen to draw on.
     * @param lives
     *                 Current lives.
     * @param maxLives
     *                 Lives the player started with.
     */
    public void drawLives(final Screen screen, final int lives,
            final int maxLives) {
        gameplayHudRenderer.drawLives(lives, maxLives);
    }

    /**
     * Draws a UI icon in its own colors.
     *
     * @param icon
     *                  Icon to draw.
     * @param positionX
     *                  Coordinates for the left side of the icon.
     * @param positionY
     *                  Coordinates for the upper side of the icon.
     * @param scale
     *                  Size in screen pixels of each icon pixel.
     */
    public void drawIcon(final Icon icon, final int positionX,
            final int positionY, final int scale) {
        drawIconPixels(icon, positionX, positionY, scale, null, false);
    }

    /**
     * Draws a UI icon in its own colors, darkened to show it is inactive.
     *
     * @param icon
     *                  Icon to draw.
     * @param positionX
     *                  Coordinates for the left side of the icon.
     * @param positionY
     *                  Coordinates for the upper side of the icon.
     * @param scale
     *                  Size in screen pixels of each icon pixel.
     */
    public void drawDimmedIcon(final Icon icon, final int positionX,
            final int positionY, final int scale) {
        drawIconPixels(icon, positionX, positionY, scale, null, true);
    }

    /**
     * Draws a UI icon in the given color.
     *
     * @param icon
     *                  Icon to draw.
     * @param positionX
     *                  Coordinates for the left side of the icon.
     * @param positionY
     *                  Coordinates for the upper side of the icon.
     * @param scale
     *                  Size in screen pixels of each icon pixel.
     * @param color
     *                  Color of the icon.
     */
    public void drawIcon(final Icon icon, final int positionX,
            final int positionY, final int scale, final Color color) {
        drawIconPixels(icon, positionX, positionY, scale, color, false);
    }

    private void drawIconPixels(final Icon icon, final int positionX,
            final int positionY, final int scale, final Color override,
            final boolean dimmed) {
        Color previous = backBufferGraphics.getColor();
        for (int i = 0; i < icon.getWidth(); i++)
            for (int j = 0; j < icon.getHeight(); j++) {
                Color color = icon.getColor(i, j);
                if (color == null)
                    continue;
                if (override != null)
                    color = override;
                if (dimmed)
                    color = new Color(color.getRed() / 2,
                            color.getGreen() / 2, color.getBlue() / 2);
                backBufferGraphics.setColor(color);
                backBufferGraphics.fillRect(positionX + i * scale,
                        positionY + j * scale, scale, scale);
            }
        backBufferGraphics.setColor(previous);
    }

    /**
     * Draws the pause overlay: a pause icon with a "Paused" label.
     *
     * @param screen
     *               Screen to draw on.
     */
    public void drawPaused(final Screen screen) {
        gameplayHudRenderer.drawPaused(screen);
    }

    /**
     * Draws an icon to the left of a centered menu label. The selected
     * option's icon is in full color and bobs; the others are dimmed.
     *
     * @param screen
     *                 Screen to draw on.
     * @param icon
     *                 Icon to draw.
     * @param label
     *                 Centered label the icon sits next to.
     * @param labelY
     *                 Baseline of the label.
     * @param selected
     *                 Whether the option is selected.
     */
    void drawMenuIcon(final Screen screen, final Icon icon,
            final String label, final int labelY, final boolean selected) {
        int iconX = screen.getWidth() / 2
                - fontRegularMetrics.stringWidth(label) / 2
                - icon.getWidth() * 2 - 8;
        int iconY = labelY - 5 - icon.getHeight();
        if (selected)
            drawIcon(icon, iconX, iconY + (int) Math.round(
                    Math.sin(System.currentTimeMillis() / 150.0) * 2), 2);
        else
            drawDimmedIcon(icon, iconX, iconY, 2);
    }

    /**
     * Draws current level progress on screen.
     *
     * @param screen
     *                    Screen to draw on.
     * @param level
     *                    Current level.
     * @param totalLevels
     *                    Total number of levels.
     */
    public void drawLevel(final Screen screen, final int level,
            final int totalLevels) {
        gameplayHudRenderer.drawLevel(screen, level, totalLevels);
    }

    /**
     * Draws a thick line from side to side of the screen.
     *
     * @param screen
     *                  Screen to draw on.
     * @param positionY
     *                  Y coordinate of the line.
     */
    public void drawHorizontalLine(final Screen screen, final int positionY) {
        gameplayHudRenderer.drawHorizontalLine(screen, positionY);
    }

    /**
     * Draws the Orbital Annihilator: a growing energy charge on the ship's
     * nose while charging, then a full vertical beam going up from the ship.
     */
    public void drawOrbitalAnnihilator(final Ship ship, final int beamWidth,
            final int topY, final boolean firing, final float chargeProgress) {
        gameplayHudRenderer.drawOrbitalAnnihilator(ship, beamWidth, topY,
                firing, chargeProgress);
    }

    /**
     * Draws the Orbital Annihilator recharge gauge under the score.
     *
     * @param screen
     *                 Screen to draw on.
     * @param progress
     *                 Recharge progress between 0 and 1.
     */
    public void drawAnnihilatorGauge(final Screen screen, final float progress) {
        gameplayHudRenderer.drawAnnihilatorGauge(screen, progress);
    }

    /**
     * Draws game title.
     *
     * @param screen
     *               Screen to draw on.
     */
    public void drawTitle(final Screen screen) {
        String titleString = "Invaders";
        String instructionsString = "select with w+s / arrows, confirm with space";

        backBufferGraphics.setColor(Color.GRAY);
        drawCenteredRegularString(screen, instructionsString,
                screen.getHeight() / 2);

        backBufferGraphics.setColor(Color.GREEN);
        drawCenteredBigString(screen, titleString, screen.getHeight() / 3);

        long time = System.currentTimeMillis();
        int scale = 4;
        int shipX = screen.getWidth() / 2 - Icon.TITLE_SHIP.getWidth()
                * scale / 2;
        int shipY = screen.getHeight() / 3 - 40
                - (Icon.TITLE_SHIP.getHeight() + 3) * scale
                + (int) Math.round(Math.sin(time / 400.0) * 3);
        drawIcon(Icon.TITLE_SHIP, shipX, shipY, scale);
        drawIcon((time / 90) % 2 == 0 ? Icon.TITLE_FLAME_LONG
                : Icon.TITLE_FLAME_SHORT, shipX,
                shipY + Icon.TITLE_SHIP.getHeight() * scale, scale);
    }

    /**
     * Draws game results.
     *
     * @param screen
     *                       Screen to draw on.
     * @param score
     *                       Score obtained.
     * @param livesRemaining
     *                       Lives remaining when finished.
     * @param shipsDestroyed
     *                       Total ships destroyed.
     * @param accuracy
     *                       Total accuracy.
     * @param isNewRecord
     *                       If the score is a new high score.
     */
    public void drawResults(final Screen screen, final int score,
            final int livesRemaining, final int shipsDestroyed,
            final float accuracy, final boolean isNewRecord) {
        resultsRecordsRenderer.drawResults(screen, score, livesRemaining,
                shipsDestroyed, accuracy, isNewRecord);
    }

    /**
     * Draws basic content of game over screen.
     *
     * @param screen
     *                     Screen to draw on.
     * @param acceptsInput
     *                     If the screen accepts input.
     * @param isNewRecord
     *                     If the score is a new high score.
     * @param gameCleared
     *                     If the game was cleared successfully.
     */
    public void drawGameOver(final Screen screen, final boolean acceptsInput,
            final boolean isNewRecord, final boolean gameCleared) {
        resultsRecordsRenderer.drawGameOver(screen, acceptsInput, isNewRecord,
                gameCleared);
    }

    /**
     * Draws which stage was failed, right below the game over title.
     *
     * @param screen
     *                    Screen to draw on.
     * @param stage
     *                    Number of the stage that was failed.
     * @param isNewRecord
     *                    If the score is a new high score (changes the layout).
     */
    public void drawStageFailed(final Screen screen, final int stage,
            final boolean isNewRecord) {
        resultsRecordsRenderer.drawStageFailed(screen, stage, isNewRecord);
    }

    /** Draws the game mode as one more row of the results list. */
    public void drawModeLabel(final Screen screen, final String modeLabel,
            final boolean isNewRecord) {
        resultsRecordsRenderer.drawModeLabel(screen, modeLabel, isNewRecord);
    }

    /**
     * Draws high score screen title and instructions.
     *
     * @param screen
     *               Screen to draw on.
     */
    public void drawHighScoreMenu(final Screen screen) {
        resultsRecordsRenderer.drawHighScoreMenu(screen);
    }

    /**
     * Draws high scores.
     *
     * @param screen
     *                   Screen to draw on.
     * @param highScores
     *                   List of high scores.
     */
    public void drawHighScores(final Screen screen,
            final List<Score> highScores) {
        resultsRecordsRenderer.drawHighScores(screen, highScores);
    }

    /**
     * Draws a centered string on regular font.
     *
     * @param screen
     *               Screen to draw on.
     * @param string
     *               String to draw.
     * @param height
     *               Height of the drawing.
     */
    public void drawCenteredRegularString(final Screen screen,
            final String string, final int height) {
        backBufferGraphics.setFont(fontRegular);
        backBufferGraphics.drawString(string, screen.getWidth() / 2
                - fontRegularMetrics.stringWidth(string) / 2, height);
    }

    /**
     * Draws a centered string on big font.
     *
     * @param screen
     *               Screen to draw on.
     * @param string
     *               String to draw.
     * @param height
     *               Height of the drawing.
     */
    public void drawCenteredBigString(final Screen screen, final String string,
            final int height) {
        backBufferGraphics.setFont(fontBig);
        backBufferGraphics.drawString(string, screen.getWidth() / 2
                - fontBigMetrics.stringWidth(string) / 2, height);
    }

    /**
     * Countdown to game start.
     *
     * @param screen
     *                  Screen to draw on.
     * @param level
     *                  Level number.
     * @param number
     *                  Countdown number.
     * @param bonusLife
     *                  Checks if a bonus life is received.
     */
    public void drawCountDown(final Screen screen, final int level,
            final int number, final boolean bonusLife) {
        gameplayHudRenderer.drawCountDown(screen, level, number, bonusLife);
    }

    /**
     * Draws the countdown with the given title.
     *
     * @param screen
     *               Screen to draw on.
     * @param title
     *               Title shown before the countdown numbers.
     * @param number
     *               Countdown number.
     */
    public void drawCountDown(final Screen screen, final String title,
            final int number) {
        gameplayHudRenderer.drawCountDown(screen, title, number);
    }

    /** Draws the shop cards and purchase/save feedback. */
    public void drawShop(final Screen screen, final List<ShopProduct> upgrades,
            final List<ShopProduct> unlocks, final PlayerAssetsState assets, final int section,
            final int upgradeIndex, final int unlockIndex, final int actionIndex,
            final String feedback, final boolean purchaseSucceeded, final SaveStatus saveStatus) {
        shopRenderer.drawShop(screen, upgrades, unlocks, assets, section,
                upgradeIndex, unlockIndex, actionIndex, feedback,
                purchaseSucceeded, saveStatus);
    }

    public void drawSettingsMenu(final Screen screen, final int selectedOption) {
        settingsRenderer.drawSettingsMenu(screen, selectedOption);
    }

    public void drawControlsSettings(final Screen screen,
            final int selectedOption, final boolean waitingForKey) {
        settingsRenderer.drawControlsSettings(screen, selectedOption,
                getControlsSettingsLabels(selectedOption, waitingForKey));
    }

    public void drawGameplaySettings(final Screen screen, final int selectedOption) {
        settingsRenderer.drawGameplaySettings(screen, selectedOption);
    }

    /**
     * Draws the audio settings screen.
     *
     * @param screen
     *                  Screen to draw on.
     * @param option
     *                  Option selected.
     * @param bgmVolume
     *                  Current BGM volume, from 0 to 100.
     * @param sfxVolume
     *                  Current SFX volume, from 0 to 100.
     * @param muted
     *                  If the audio is muted.
     */
    public void drawAudioSettings(final Screen screen, final int option,
            final int bgmVolume, final int sfxVolume, final boolean muted) {
        settingsRenderer.drawAudioSettings(screen, option, bgmVolume,
                sfxVolume, muted);
    }

    /**
     * Draws the Orbital Annihilator item as a short laser bolt that falls:
     * the tail fades out at the top, and a bright pointed head blinks at the
     * front (bottom).
     *
     * @param entity
     *                  Item to be drawn.
     * @param positionX
     *                  Coordinates for the left side of the item.
     * @param positionY
     *                  Coordinates for the upper side of the item.
     */
    private void drawFallingLaser(final Entity entity, final int positionX,
            final int positionY) {
        int w = entity.getWidth();
        int h = entity.getHeight();
        boolean blink = (System.currentTimeMillis() / 150) % 2 == 0;

        // Body: red beam with a white core, more opaque towards the head.
        int slices = h / 2;
        for (int i = 0; i < slices; i++) {
            int y = positionY + i * 2;
            int alpha = 60 + (int) (195.0 * i / slices);

            backBufferGraphics.setColor(new Color(255, 0, 0, alpha / 3));
            backBufferGraphics.fillRect(positionX, y, w, 2);

            backBufferGraphics.setColor(new Color(255, 0, 0, alpha));
            backBufferGraphics.fillRect(positionX + 3, y, w - 6, 2);

            backBufferGraphics.setColor(new Color(255, 255, 255, alpha));
            backBufferGraphics.fillRect(positionX + w / 2 - 1, y, 2, 2);
        }

        // Head: bright triangle pointing down, blinking.
        int headTop = positionY + h - 8;
        int[] xs = { positionX + 1, positionX + w - 1, positionX + w / 2 };
        int[] ys = { headTop, headTop, positionY + h };
        backBufferGraphics.setColor(blink ? Color.WHITE : Color.YELLOW);
        backBufferGraphics.fillPolygon(xs, ys, 3);
    }

    /**
     * Draws the Cluster Grenade item with its pixel-art sprite, so that
     * players recognize it when it falls.
     *
     * @param x Coordinate for the left side of the item.
     * @param y Coordinate for the upper side of the item.
     */
    public void drawGrenadeItem(final int x, final int y) {
        gameplayHudRenderer.drawGrenadeItem(x, y);
    }

    /**
     * Draws a grenade launched by the player ship: a pixel-art grenade
     * with a halo.
     *
     * @param grenade Grenade to draw.
     */
    public void drawGrenade(final Grenade grenade) {
        gameplayHudRenderer.drawGrenade(grenade);
    }

    /**
     * Draws a fragment of an exploded grenade: a pixel-art spark with a halo
     * that fades out.
     *
     * @param fragment Fragment to draw.
     */
    public void drawGrenadeFragment(final GrenadeFragment fragment) {
        gameplayHudRenderer.drawGrenadeFragment(fragment);
    }

    /**
     * Draws the number of grenades left, when the ship has some.
     *
     * @param screen Screen to draw on.
     * @param grenades Number of grenades left.
     */
    public void drawGrenadeCount(final Screen screen, final int grenades) {
        gameplayHudRenderer.drawGrenadeCount(grenades);
    }

    /**
     * Draws the names of the active buffs, top right, under the separator.
     *
     * @param screen Screen to draw on.
     * @param lines Buff names to show.
     */
    public void drawBuffs(final Screen screen, final List<String> lines) {
        gameplayHudRenderer.drawBuffs(screen, lines);
    }

    /**
     * Draws a blinking message in the top bar when the Orbital Annihilator
     * is ready to be used.
     *
     * @param screen
     *               Screen to draw on.
     */
    public void drawAnnihilatorReady(final Screen screen) {
        gameplayHudRenderer.drawAnnihilatorReady(screen);
    }

    /**
     * Dessine un bouclier protecteur autour du vaisseau.
     *
     * @param screen Écran de jeu cible.
     * @param ship   Vaisseau à entourer.
     */
    public void drawShield(final Screen screen, final Entity ship) {
        gameplayHudRenderer.drawShield(ship);
    }

    private String getKeyDisplayText(final int keyCode) {
        switch (keyCode) {
            case java.awt.event.KeyEvent.VK_LEFT:
                return "Left Arrow";
            case java.awt.event.KeyEvent.VK_RIGHT:
                return "Right Arrow";
            case java.awt.event.KeyEvent.VK_UP:
                return "Up Arrow";
            case java.awt.event.KeyEvent.VK_DOWN:
                return "Down Arrow";
            case java.awt.event.KeyEvent.VK_SPACE:
                return "Space";
            default:
                if (keyCode >= java.awt.event.KeyEvent.VK_A
                        && keyCode <= java.awt.event.KeyEvent.VK_Z) {
                    return String.valueOf((char) keyCode);
                }
                return java.awt.event.KeyEvent.getKeyText(keyCode);
        }
    }

    public void initDrawing(final Screen screen) {
        backBuffer = new BufferedImage(screen.getWidth(), screen.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        graphics = frame == null ? null : frame.getGraphics();
        backBufferGraphics = backBuffer.getGraphics();

        backBufferGraphics.setColor(Color.BLACK);
        backBufferGraphics
                .fillRect(0, 0, screen.getWidth(), screen.getHeight());
        screen.drawBackground(backBufferGraphics);

        fontRegularMetrics = backBufferGraphics.getFontMetrics(fontRegular);
        fontBigMetrics = backBufferGraphics.getFontMetrics(fontBig);

        // drawBorders(screen);
        // drawGrid(screen);
    }

    public void completeDrawing(final Screen screen) {
        try {
            if (graphics != null)
                graphics.drawImage(backBuffer, frame.getInsets().left,
                        frame.getInsets().top, frame);
        } finally {
            backBufferGraphics.dispose();
            if (graphics != null)
                graphics.dispose();
        }
    }

    Graphics getBackBufferGraphics() {
        return backBufferGraphics;
    }

    Font getRegularFont() {
        return fontRegular;
    }

    Font getBigFont() {
        return fontBig;
    }

    FontMetrics getRegularFontMetrics() {
        return fontRegularMetrics;
    }

    FontMetrics getBigFontMetrics() {
        return fontBigMetrics;
    }

    public void drawNameInput(final Screen screen, final char[] name,
            final int nameCharSelected) {
        resultsRecordsRenderer.drawNameInput(screen, name, nameCharSelected);
    }

    public void drawAchievementNotice(final Screen screen,
            final Achievement achievement) {
        resultsRecordsRenderer.drawAchievementNotice(screen, achievement);
    }
    public void drawAchievementResults(final Screen screen, final int score,
            final int lives, final int enemies, final float accuracy,
            final boolean isNewRecord, final char[] name, final int selected,
            final List<Achievement> achievements, final boolean acceptsInput, final int failedStage) {
        resultsRecordsRenderer.drawAchievementResults(screen, score, lives,
                enemies, accuracy, isNewRecord, name, selected, achievements,
                acceptsInput, failedStage);
    }

    public void drawAchievementResults(final Screen screen, final int score,
            final int lives, final int enemies, final float accuracy,
            final boolean isNewRecord, final char[] name, final int selected,
            final List<Achievement> achievements, final boolean acceptsInput, final int failedStage,
            final boolean gameCleared) {
        resultsRecordsRenderer.drawAchievementResults(screen, score, lives,
                enemies, accuracy, isNewRecord, name, selected, achievements,
                acceptsInput, failedStage, gameCleared);
    }

    public void drawAchievementResults(final Screen screen, final int score,
            final int lives, final int enemies, final float accuracy,
            final boolean isNewRecord, final char[] name, final int selected,
            final List<Achievement> achievements, final boolean acceptsInput,
            final int failedStage, final boolean gameCleared, final String modeLabel) {
        resultsRecordsRenderer.drawAchievementResults(screen, score, lives,
                enemies, accuracy, isNewRecord, name, selected, achievements,
                acceptsInput, failedStage, gameCleared, modeLabel);
    }

    public void drawAchievementList(final Screen screen, final RecordsService service, int y) {
        resultsRecordsRenderer.drawAchievementList(screen, service, y);
    }

    public void drawMenu(final Screen screen, final int option) {
        String[] labels = {
            "Play", "Shop", "Hangar", "High scores",
            "Records & Achievements", "Settings", "Exit"
        };
        int[] codes = { 2, 10, 11, 3, 9, 4, 0 };
        Icon[] icons = {
                Icon.PLAY, Icon.SHOP, Icon.HANGAR, Icon.HIGH_SCORES,
                Icon.RECORDS, Icon.SETTINGS, Icon.EXIT
        };
        int spacing = fontRegularMetrics.getHeight() * 3 / 2;
        int firstY = screen.getHeight() / 2 + fontRegularMetrics.getHeight() * 2;
        for (int i = 0; i < labels.length; i++) {
            boolean selected = option == codes[i];
            int labelY = firstY + i * spacing;
            backBufferGraphics.setColor(selected ? Color.GREEN : Color.WHITE);
            drawCenteredRegularString(screen, labels[i], labelY);
            if (icons[i] != null)
                drawMenuIcon(screen, icons[i], labels[i], labelY, selected);
        }
    }

    public int getMenuOptionAt(final Screen screen,
            final int mouseX, final int mouseY) {
        if (fontRegularMetrics == null)
            return -1;

        String[] labels = {
                "Play", "Shop", "Hangar", "High scores",
                "Records & Achievements", "Settings", "Exit"
        };
        int[] codes = { 2, 10, 11, 3, 9, 4, 0 };
        int fontHeight = fontRegularMetrics.getHeight();
        int spacing = fontHeight * 3 / 2;
        int firstY = screen.getHeight() / 2 + fontHeight * 2;

        for (int i = 0; i < labels.length; i++) {
            int textWidth = fontRegularMetrics.stringWidth(labels[i]);
            int left = screen.getWidth() / 2 - textWidth / 2;
            int baseline = firstY + i * spacing;
            int top = baseline - fontRegularMetrics.getAscent();
            int bottom = baseline + fontRegularMetrics.getDescent();

            if (mouseX >= left && mouseX <= left + textWidth
                    && mouseY >= top && mouseY <= bottom)
                return codes[i];
        }

        return -1;
    }

    /**
     * Returns the settings menu option under the mouse.
     *
     * @return Index of the hovered option, or -1 if none.
     */
    public int getSettingsMenuOptionAt(final Screen screen,
            final int mouseX, final int mouseY) {
        if (fontRegularMetrics == null)
            return -1;

        return getCenteredOptionAt(screen, SettingsRenderer.SETTINGS_MENU_OPTIONS,
                screen.getHeight() / 2, fontRegularMetrics.getHeight() * 2,
                mouseX, mouseY);
    }

    /**
     * Returns the controls settings option under the mouse.
     *
     * @return Index of the hovered option, or -1 if none.
     */
    public int getControlsSettingsOptionAt(final Screen screen,
            final int selectedOption, final boolean waitingForKey,
            final int mouseX, final int mouseY) {
        if (fontRegularMetrics == null)
            return -1;

        return getCenteredOptionAt(screen,
                getControlsSettingsLabels(selectedOption, waitingForKey),
                screen.getHeight() / 2, fontRegularMetrics.getHeight() * 2,
                mouseX, mouseY);
    }

    /**
     * Returns the gameplay settings option under the mouse.
     *
     * @return Index of the hovered option, or -1 if none.
     */
    public int getGameplaySettingsOptionAt(final Screen screen,
            final int mouseX, final int mouseY) {
        if (fontRegularMetrics == null)
            return -1;

        return getCenteredOptionAt(screen, SettingsRenderer.GAMEPLAY_SETTINGS_OPTIONS,
                screen.getHeight() / 2, fontRegularMetrics.getHeight() * 2,
                mouseX, mouseY);
    }

    /**
     * Returns the audio settings option under the mouse. A volume option
     * covers its whole row, from its name down to its volume bar.
     *
     * @param optionCount
     *                    Number of options on the audio settings screen.
     * @return Index of the hovered option, or -1 if none.
     */
    public int getAudioSettingsOptionAt(final Screen screen,
            final int optionCount, final int mouseX, final int mouseY) {
        if (fontRegularMetrics == null)
            return -1;

        int columnLeft = screen.getWidth() / 2
                - SettingsRenderer.AUDIO_COLUMN_WIDTH / 2;
        int firstOptionY = screen.getHeight() / 3;
        int optionSpacing = fontRegularMetrics.getHeight() * 4;
        int muteIndex = optionCount - 1;

        if (mouseX < columnLeft
                || mouseX > columnLeft + SettingsRenderer.AUDIO_COLUMN_WIDTH)
            return -1;

        for (int i = 0; i < optionCount; i++) {
            int baseline = firstOptionY + optionSpacing * i;
            int top = baseline - fontRegularMetrics.getAscent();
            int bottom = i == muteIndex
                    ? baseline + fontRegularMetrics.getDescent()
                    : baseline + SettingsRenderer.AUDIO_BAR_GAP
                            + SettingsRenderer.AUDIO_BAR_HEIGHT;

            if (mouseY >= top && mouseY <= bottom)
                return i;
        }

        return -1;
    }

    /**
     * Returns the index of the horizontally centered option under the mouse.
     *
     * @param labels
     *                Option texts, drawn one under the other.
     * @param firstY
     *                Baseline of the first option.
     * @param spacing
     *                Vertical space between two option baselines.
     * @return Index of the hovered option, or -1 if none.
     */
    private int getCenteredOptionAt(final Screen screen, final String[] labels,
            final int firstY, final int spacing,
            final int mouseX, final int mouseY) {
        for (int i = 0; i < labels.length; i++) {
            int textWidth = fontRegularMetrics.stringWidth(labels[i]);
            int left = screen.getWidth() / 2 - textWidth / 2;
            int baseline = firstY + i * spacing;
            int top = baseline - fontRegularMetrics.getAscent();
            int bottom = baseline + fontRegularMetrics.getDescent();

            if (mouseX >= left && mouseX <= left + textWidth
                    && mouseY >= top && mouseY <= bottom)
                return i;
        }

        return -1;
    }

    /**
     * Builds the texts of the controls settings options, with their keys.
     *
     * @return One text per option: Move Left, Move Right, Fire, Back.
     */
    private String[] getControlsSettingsLabels(final int selectedOption,
            final boolean waitingForKey) {
        String[] labels = { "Move Left", "Move Right", "Fire", "Back" };
        String[] keys = {
                getKeyDisplayText(ControlsConfig.getKey(ControlsConfig.LEFT)),
                getKeyDisplayText(ControlsConfig.getKey(ControlsConfig.RIGHT)),
                getKeyDisplayText(ControlsConfig.getKey(ControlsConfig.FIRE))
        };

        for (int i = 0; i < keys.length; i++) {
            labels[i] += ": ";
            labels[i] += waitingForKey && i == selectedOption
                    ? "Press a key (Esc to cancel)"
                    : keys[i];
        }

        return labels;
    }

    public void drawRecordsPage(final Screen screen,
            final RecordsService service, final int tab) {
        resultsRecordsRenderer.drawRecordsPage(screen, service, tab);
    }

    /**
     * Draws every live explosion particle.
     */
    public void drawParticles(final vfx.ParticleSystem particleSystem) {
        gameplayHudRenderer.drawParticles(particleSystem);
    }

    /** Draws earned points without changing the existing score HUD. */
    public void drawFloatingScores(final FloatingScoreHud hud, final Screen screen) {
        gameplayHudRenderer.drawFloatingScores(hud, screen);
    }

    /**
     * Draws the ship selection screen, listing every selectable ship with its
     * stats and marking the highlighted one.
     *
     * @param screen
     *                      Screen to draw on.
     * @param selectedIndex
     *                      Index of the currently highlighted ship.
     * @param shipTypes
     *                      Ships available for selection, in display order.
     */
    public void drawShipSelection(final Screen screen, final ShipType[] shipTypes,
            final int selectedIndex) {
        String titleString = "SELECT SHIP";
        String instructionsString = Core.getShopController().isShipAvailable(shipTypes[selectedIndex])
                ? "SPACE: SELECT   ESC: BACK" : "UNLOCK IN SHOP   ESC: BACK";

        backBufferGraphics.setColor(Color.GREEN);
        drawCenteredRegularString(screen, titleString, screen.getHeight() / 6);

        int startY = screen.getHeight() / 3;
        int spacing = 85;

        for (int i = 0; i < shipTypes.length; i++) {
            ShipType type = shipTypes[i];
            boolean isSelected = (i == selectedIndex);
            boolean available = Core.getShopController().isShipAvailable(type);

            if (!available) {
                int scale = 3;
                drawIcon(Icon.LOCK, screen.getWidth() / 2 - Icon.LOCK.getWidth() * scale / 2,
                        startY + i * spacing - 50, scale, Color.GRAY);
            } else if (isSelected) {
                entity.Ship previewShip = new entity.Ship(screen.getWidth() / 2 - 13,
                        startY + i * spacing - 40, type);
                drawEntity(previewShip, screen.getWidth() / 2 - 13, startY + i * spacing - 40);
            }

            String name = (isSelected ? "> " : "") + type.getName()
                    + (available ? "" : " [LOCKED]") + (isSelected ? " <" : "");
            String stats = String.format("SPEED: %d | HP: %d | RELOAD: %dMS",
                    type.getSpeed(), type.getMaxLives(), type.getShootingInterval());

            backBufferGraphics.setColor(isSelected ? Color.GREEN : Color.GRAY);
            drawCenteredRegularString(screen, name, startY + i * spacing);
            drawCenteredRegularString(screen, stats, startY + i * spacing + 20);
        }

        backBufferGraphics.setColor(Color.GRAY);
        drawCenteredRegularString(screen, instructionsString, screen.getHeight() - 60);
    }

    /**
     * Returns the index of the ship under the mouse cursor.
     * Returns -1 if the cursor is outside the ship options.
     */
    public int getShipSelectionOptionAt(final Screen screen,
            final ShipType[] shipTypes, final int mouseX, final int mouseY) {

        if (fontRegularMetrics == null) {
            return -1;
        }

        int startY = screen.getHeight() / 3;
        int spacing = 85;

        for (int i = 0; i < shipTypes.length; i++) {
            int baseline = startY + i * spacing;

            // Detect the mouse within the ship name and stats area.
            int top = baseline - fontRegularMetrics.getAscent();
            int bottom = baseline + 20 + fontRegularMetrics.getDescent();

            if (mouseX >= screen.getWidth() / 4
                    && mouseX <= screen.getWidth() * 3 / 4
                    && mouseY >= top && mouseY <= bottom) {
                return i;
            }
        }

        return -1;
    }

    public void drawHangarMenu(final Screen screen, final int selectedOption) {
        String[] labels = { "Choose ship", "Choose color", "Back" };

        backBufferGraphics.setColor(Color.GREEN);
        drawCenteredBigString(screen, "Hangar", screen.getHeight() / 5);

        int firstY = screen.getHeight() / 2;
        int spacing = fontRegularMetrics.getHeight() * 3;
        for (int i = 0; i < labels.length; i++) {
            backBufferGraphics.setColor(
                    i == selectedOption ? Color.GREEN : Color.WHITE);
            drawCenteredRegularString(screen, labels[i], firstY + i * spacing);
        }

        backBufferGraphics.setColor(Color.GRAY);
        drawCenteredRegularString(screen,
                "Up/Down: select   Space/Enter: confirm",
                screen.getHeight() - 50);
        drawCenteredRegularString(screen, "Esc: main menu",
                screen.getHeight() - 28);
    }

    public void drawGameModeMenu(final Screen screen, final int option) {
        String[] labels = { "Campaign Mode", "Back" };

        backBufferGraphics.setFont(fontBig);
        backBufferGraphics.setColor(Color.GREEN);
        drawCenteredBigString(screen, "Game Mode", screen.getHeight() / 3);

        backBufferGraphics.setFont(fontRegular);
        for (int i = 0; i < labels.length; i++) {
            backBufferGraphics.setColor(i == option ? Color.GREEN : Color.WHITE);
            drawCenteredRegularString(screen, labels[i],
                screen.getHeight() / 3 + 60 + i * 40);
        }
    }
}
