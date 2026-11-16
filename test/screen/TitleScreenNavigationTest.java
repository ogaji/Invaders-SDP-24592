package screen;

import audio.AudioManager;
import engine.Core;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class TitleScreenNavigationTest {
    private final Canvas inputSource = new Canvas();
    private TitleScreen screen;
    private Field selectionCooldown;
    private boolean previousMute;

    @BeforeEach
    void setUp() throws Exception {
        previousMute = AudioManager.isMuted();
        AudioManager.setMuted(true);
        releaseInput();
        screen = new TitleScreen(448, 520, 60);
        screen.inputDelay = Core.getCooldown(0);
        selectionCooldown = TitleScreen.class.getDeclaredField("selectionCooldown");
        selectionCooldown.setAccessible(true);
        selectionCooldown.set(screen, Core.getCooldown(0));
        screen.isRunning = true;
        screen.update();
    }

    @AfterEach
    void cleanUp() {
        releaseInput();
        AudioManager.setMuted(previousMute);
    }

    @ParameterizedTest
    @CsvSource({"0, GAME_MODE", "1, SHOP", "2, HANGAR_MENU", "3, HIGH_SCORES",
            "4, PLAYER_RECORDS", "5, SETTINGS", "6, EXIT"})
    void keyboardOpensTheDestinationOfTheHighlightedEntry(int index, Destination expected) throws Exception {
        for (int i = 0; i < index; i++) tap(KeyEvent.VK_DOWN);
        assertTrue(screen.isRunning);
        tap(KeyEvent.VK_SPACE);

        assertFalse(screen.isRunning);
        assertSame(expected, screen.nextDestination);
    }

    @ParameterizedTest
    @CsvSource({"0, GAME_MODE", "1, SHOP", "2, HANGAR_MENU", "3, HIGH_SCORES",
            "4, PLAYER_RECORDS", "5, SETTINGS", "6, EXIT"})
    void mouseHitTestingOpensTheSameDestinationAsTheKeyboard(int index, Destination expected) {
        int y = menuEntryY(index);
        Core.getInputManager().mouseMoved(mouse(MouseEvent.MOUSE_MOVED, y));
        screen.update();
        assertTrue(screen.isRunning);
        Core.getInputManager().mousePressed(mouse(MouseEvent.MOUSE_PRESSED, y));
        screen.update();

        assertFalse(screen.isRunning);
        assertSame(expected, screen.nextDestination);
    }

    @Test
    void movingTheHighlightDoesNotChangeThePendingDestination() throws Exception {
        tap(KeyEvent.VK_DOWN);

        assertTrue(screen.isRunning);
        assertSame(Destination.GAME_MODE, screen.nextDestination);
        tap(KeyEvent.VK_SPACE);
        assertSame(Destination.SHOP, screen.nextDestination);
    }

    @Test
    void upwardNavigationWrapsFromPlayToExit() throws Exception {
        tap(KeyEvent.VK_UP);
        tap(KeyEvent.VK_SPACE);

        assertSame(Destination.EXIT, screen.nextDestination);
    }

    @Test
    void downwardNavigationWrapsBackToPlay() throws Exception {
        for (int i = 0; i < 7; i++) tap(KeyEvent.VK_DOWN);
        tap(KeyEvent.VK_SPACE);

        assertSame(Destination.GAME_MODE, screen.nextDestination);
    }

    @Test
    void infiniteShortcutWorksWithAnotherMenuEntryHighlighted() throws Exception {
        tap(KeyEvent.VK_DOWN);
        tap(KeyEvent.VK_I);

        assertFalse(screen.isRunning);
        assertSame(Destination.INFINITE, screen.nextDestination);
    }

    @Test
    void clickingOutsideTheMenuDoesNotNavigate() {
        Core.getInputManager().mousePressed(mouse(MouseEvent.MOUSE_PRESSED, -1));
        screen.update();

        assertTrue(screen.isRunning);
        assertSame(Destination.GAME_MODE, screen.nextDestination);
    }

    private int menuEntryY(int index) {
        for (int y = 0; y < screen.getHeight(); y++) {
            if (Core.getDrawManager().getMenuOptionAt(screen, screen.getWidth() / 2, y) == index) return y;
        }
        throw new AssertionError("Menu entry has no hit area: " + index);
    }

    private void tap(int code) throws Exception {
        selectionCooldown.set(screen, Core.getCooldown(0));
        key(code, true);
        screen.update();
        key(code, false);
    }

    private void key(int code, boolean pressed) {
        KeyEvent event = new KeyEvent(inputSource,
                pressed ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                0, 0, code, KeyEvent.CHAR_UNDEFINED);
        if (pressed) Core.getInputManager().keyPressed(event);
        else Core.getInputManager().keyReleased(event);
    }

    private MouseEvent mouse(int id, int y) {
        return new MouseEvent(inputSource, id, 0, 0, 224, y, 1, false, MouseEvent.BUTTON1);
    }

    private void releaseInput() {
        for (int code = 0; code < 256; code++) key(code, false);
        Core.getInputManager().mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, -1));
        Core.getInputManager().mouseExited(mouse(MouseEvent.MOUSE_EXITED, -1));
        Core.getInputManager().clearLastPressedKey();
    }
}
