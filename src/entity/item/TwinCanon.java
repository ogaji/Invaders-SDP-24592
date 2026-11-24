package entity.item;

import engine.DrawManager.SpriteType;

/**
 * Twin Cannon item (double barrel).
 * When collected by the ship, the single shot is replaced by a parallel
 * volley of two bullets for 15 seconds. The timer is handled by the
 * BuffManager, and the buff expires immediately if the player loses a life.
 *
 * @author Clarisse Majourau
 *
 */
public class TwinCanon extends Item {

    /** Duration of the buff, in milliseconds. */
    private static final long DURATION = 15000;
    /** Width of the item (same size as the ship). */
    private static final int WIDTH = 13 * 2;
    /** Height of the item (same size as the ship). */
    private static final int HEIGHT = 8 * 2;

    /**
     * Constructor, the item falls from the given position.
     *
     * @param positionX
     *            Initial position of the item in the X axis.
     * @param positionY
     *            Initial position of the item in the Y axis.
     */
    public TwinCanon(final int positionX, final int positionY) {
        // TODO: replace by a dedicated item sprite when one exists.
        super(positionX, positionY, WIDTH, HEIGHT, SpriteType.Ship);
    }

    @Override
    public PickupMode getPickupMode() {
        return PickupMode.SHOT;
    }

    @Override
    public void applyEffect(final ItemEffectContext context) {
        context.getShip().setTwinCannonActive(true);
    }

    @Override
    public void removeEffect(final ItemEffectContext context) {
        context.getShip().setTwinCannonActive(false);
    }

    @Override
    public long getDuration() {
        return DURATION;
    }

    @Override
    public String getName() {
        return "Twin Cannon";
    }
}
