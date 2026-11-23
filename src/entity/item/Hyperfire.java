
package entity.item;

import engine.DrawManager.SpriteType;
import audio.AudioManager;
/**
 * Hyperfire item.
 * Reduces the player's shooting cooldown by 60%
 * for 8 seconds.
 */
public class Hyperfire extends Item {

    private static final long DURATION_MS = 8_000;

    /**
     * Creates a Hyperfire item at the given position.
     */
    public Hyperfire(final int positionX, final int positionY) {
        super(positionX, positionY, 16, 24, SpriteType.Hyperfire);

    }

    @Override
    public void applyEffect(final ItemEffectContext context) {
        context.getShip().setHyperfireEnabled(true);
        AudioManager.playSFX("audio/sfx/item_hyperfire_activate.wav");
    }

    @Override
    public void removeEffect(final ItemEffectContext context) {
        context.getShip().setHyperfireEnabled(false);
    }

    /**
     * Returns the duration of the buff in milliseconds.
     */
    @Override
    public long getDuration() {
        return DURATION_MS;
    }

    /**
     * Returns the name of the item.
     */
    @Override
    public String getName() {
        return "Hyperfire";
    }
}
