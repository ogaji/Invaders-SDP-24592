package entity.item;

import entity.OrbitalAnnihilator;
import entity.Ship;

/** Context passed to an item when its effect is applied or removed. */
public final class ItemEffectContext {

    private final Ship ship;
    private final OrbitalAnnihilator annihilator;

    public ItemEffectContext(final Ship ship) {
        this(ship, null);
    }

    public ItemEffectContext(final Ship ship,
                             final OrbitalAnnihilator annihilator) {
        this.ship = ship;
        this.annihilator = annihilator;
    }

    public Ship getShip() {
        return this.ship;
    }

    public OrbitalAnnihilator getAnnihilator() {
        return this.annihilator;
    }
}
