package screen;

import java.awt.event.KeyEvent;
import audio.AudioManager;

import engine.Cooldown;
import engine.Core;

/**
 * Implements the title screen.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class TitleScreen extends Screen {

    private static final Destination[] MENU_DESTINATIONS = {
        Destination.GAME_MODE, Destination.SHOP, Destination.HANGAR_MENU,
        Destination.HIGH_SCORES, Destination.PLAYER_RECORDS, Destination.SETTINGS,
        Destination.EXIT
    };

    /** Index of the highlighted menu entry. */
    private int selectedMenuOption = 0;

    /** Milliseconds between changes in user selection. */
    private static final int SELECTION_TIME = 200;

    /** Time between changes in user selection. */
    private Cooldown selectionCooldown;
    private boolean wasMousePressed = false;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width
     *               Screen width.
     * @param height
     *               Screen height.
     * @param fps
     *               Frames per second, frame rate at which the game is run.
     */
    public TitleScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        // Defaults to play.
        this.nextDestination = Destination.GAME_MODE;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();

        AudioManager.playBGM("audio/bgm/music_menu.wav");
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();
        // Get mouse position relative to the game drawing area.
        int mouseX = inputManager.getMouseX();
        int mouseY = inputManager.getMouseY();

        int hoveredOption = drawManager.getMenuOptionAt(this, mouseX, mouseY);

        // Update selection when hovering over a different option.
        if (hoveredOption != -1 && hoveredOption != this.selectedMenuOption) {
            this.selectedMenuOption = hoveredOption;
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        }

        boolean mousePressed = inputManager.isMousePressed();
        boolean newMouseClick = mousePressed && !wasMousePressed;

        wasMousePressed = mousePressed;

        if (newMouseClick
                && hoveredOption != -1
                && this.inputDelay.checkFinished()) {

            this.selectedMenuOption = hoveredOption;
            this.nextDestination = MENU_DESTINATIONS[this.selectedMenuOption];
            AudioManager.playSFX("audio/sfx/menu_select.wav");
            this.isRunning = false;
        }

        draw();
        if (this.selectionCooldown.checkFinished()
                && this.inputDelay.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP)
                    || inputManager.isKeyDown(KeyEvent.VK_W)) {
                previousMenuItem();
                this.selectionCooldown.reset();
                AudioManager.playSFX("audio/sfx/menu_move.wav");
            }
            if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                    || inputManager.isKeyDown(KeyEvent.VK_S)) {
                nextMenuItem();
                this.selectionCooldown.reset();
                AudioManager.playSFX("audio/sfx/menu_move.wav");
            }
            if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                this.nextDestination = MENU_DESTINATIONS[this.selectedMenuOption];
                AudioManager.playSFX("audio/sfx/menu_select.wav");
                this.isRunning = false;
            }
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawTitle(this);
        drawManager.drawMenu(this, this.selectedMenuOption);

        drawManager.completeDrawing(this);
    }

    private void nextMenuItem() {
        this.selectedMenuOption = (this.selectedMenuOption + 1) % MENU_DESTINATIONS.length;
    }

    private void previousMenuItem() {
        this.selectedMenuOption =
                (this.selectedMenuOption - 1 + MENU_DESTINATIONS.length) % MENU_DESTINATIONS.length;
    }
}
