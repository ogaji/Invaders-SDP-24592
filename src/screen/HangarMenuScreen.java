package screen;

import java.awt.event.KeyEvent;

import audio.AudioManager;
import engine.Core;
import engine.Cooldown;

/** Menu for choosing a Hangar action. */
public class HangarMenuScreen extends Screen {

    private static final int SELECTION_TIME = 200;

    private final Cooldown selectionCooldown;
    private int selectedOption;

    public HangarMenuScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.nextDestination = Destination.TITLE;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
        this.selectedOption = 0;
    }

    @Override
    public final Destination run() {
        super.run();
        return this.nextDestination;
    }

    @Override
    protected void update() {
        super.update();
        draw();

        if (!this.inputDelay.checkFinished()
                || !this.selectionCooldown.checkFinished()) {
            return;
        }

        if (this.inputManager.isKeyDown(KeyEvent.VK_UP)
                || this.inputManager.isKeyDown(KeyEvent.VK_W)) {
            this.selectedOption = (this.selectedOption + 2) % 3;
            this.selectionCooldown.reset();
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        } else if (this.inputManager.isKeyDown(KeyEvent.VK_DOWN)
                || this.inputManager.isKeyDown(KeyEvent.VK_S)) {
            this.selectedOption = (this.selectedOption + 1) % 3;
            this.selectionCooldown.reset();
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        } else if (this.inputManager.isKeyDown(KeyEvent.VK_SPACE)
                || this.inputManager.isKeyDown(KeyEvent.VK_ENTER)) {
            selectOption();
        } else if (this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.nextDestination = Destination.TITLE;
            this.isRunning = false;
            AudioManager.playSFX("audio/sfx/menu_back.wav");
        }
    }

    private void selectOption() {
        if (this.selectedOption == 0) {
            this.nextDestination = Destination.SHIP_SELECTION;
        } else if (this.selectedOption == 1) {
            this.nextDestination = Destination.SHIP_CUSTOMIZATION;
        } else {
            this.nextDestination = Destination.TITLE;
        }

        this.isRunning = false;
        if (this.nextDestination == Destination.TITLE) {
            AudioManager.playSFX("audio/sfx/menu_back.wav");
        } else {
            AudioManager.playSFX("audio/sfx/menu_select.wav");
        }
    }

    private void draw() {
        this.drawManager.initDrawing(this);
        this.drawManager.drawHangarMenu(this, this.selectedOption);
        this.drawManager.completeDrawing(this);
    }
}
