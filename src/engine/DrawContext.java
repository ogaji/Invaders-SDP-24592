package engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import screen.Screen;
import vfx.Icon;

/**
 * Package-private access to DrawManager's current drawing surface and common
 * drawing helpers. A context never retains a frame-specific Graphics object.
 */
final class DrawContext {
    private final DrawManager drawManager;

    DrawContext(final DrawManager drawManager) {
        this.drawManager = drawManager;
    }

    Graphics graphics() {
        return drawManager.getBackBufferGraphics();
    }

    Font regularFont() {
        return drawManager.getRegularFont();
    }

    Font bigFont() {
        return drawManager.getBigFont();
    }

    FontMetrics regularMetrics() {
        return drawManager.getRegularFontMetrics();
    }

    FontMetrics bigMetrics() {
        return drawManager.getBigFontMetrics();
    }

    void drawCenteredRegularString(final Screen screen, final String string,
            final int height) {
        drawManager.drawCenteredRegularString(screen, string, height);
    }

    void drawCenteredBigString(final Screen screen, final String string,
            final int height) {
        drawManager.drawCenteredBigString(screen, string, height);
    }

    void drawIcon(final Icon icon, final int positionX, final int positionY,
            final int scale) {
        drawManager.drawIcon(icon, positionX, positionY, scale);
    }

    void drawIcon(final Icon icon, final int positionX, final int positionY,
            final int scale, final Color color) {
        drawManager.drawIcon(icon, positionX, positionY, scale, color);
    }

    void drawMenuIcon(final Screen screen, final Icon icon,
            final String label, final int labelY, final boolean selected) {
        drawManager.drawMenuIcon(screen, icon, label, labelY, selected);
    }
}
