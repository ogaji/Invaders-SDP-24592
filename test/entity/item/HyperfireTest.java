package entity.item;

import entity.Ship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperfireTest {

    @Test
    void activatesAndRemovesHyperfire() {
        Ship ship = new Ship(100, 100);
        ItemEffectContext context = new ItemEffectContext(ship);
        Hyperfire hyperfire = new Hyperfire(100, 100);

        assertFalse(ship.isHyperfireEnabled(), "Hyperfire should start disabled");

        hyperfire.applyEffect(context);
        assertTrue(ship.isHyperfireEnabled(), "Applying Hyperfire should enable it");

        hyperfire.removeEffect(context);
        assertFalse(ship.isHyperfireEnabled(), "Removing Hyperfire should disable it");
    }
}
