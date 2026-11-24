package item;

import engine.Core;
import entity.EnemyShip;
import entity.OrbitalAnnihilator;
import entity.ShipType;
import entity.item.AnnihilatorItem;
import entity.item.ChameleonHull;
import entity.item.ClusterGrenadeItem;
import entity.item.Hyperfire;
import entity.item.ShieldItem;
import entity.item.TwinCanon;
import java.util.Random;
import java.util.logging.Logger;

/** Decides which items to spawn during one level. */
public final class ItemDropper {

    private static final int CHAMELEON_REQUIRED_LEVEL = 1;
    private static final double CHAMELEON_DROP_CHANCE = 0.03;
    private static final double ANNIHILATOR_DROP_CHANCE = 0.05;
    private static final double GRENADE_DROP_CHANCE = 0.15;
    private static final int MAX_GRENADE_DROPS_PER_LEVEL = 2;
    private static final double SHIELD_DROP_CHANCE = 0.15;
    private static final int TWIN_CANNON_REQUIRED_LEVEL = 3;
    private static final int TWIN_CANNON_SPAWN_KILLS = 10;
    private static final int TWIN_CANNON_SPAWN_MARGIN = 30;

    private final Logger logger = Core.getLogger();
    private final ItemManager itemManager;
    private final Random random;
    private final int level;
    private final int shipsDestroyedAtStart;

    private boolean chameleonDropped;
    private boolean annihilatorDropped;
    private int grenadeDrops;
    private boolean shieldDropped;
    private boolean hyperfireDropped;
    private boolean twinCannonSpawned;

    public ItemDropper(final ItemManager itemManager, final int level,
                       final int shipsDestroyedAtStart) {
        this(itemManager, level, shipsDestroyedAtStart, new Random());
    }

    public ItemDropper(final ItemManager itemManager, final int level,
                       final int shipsDestroyedAtStart, final Random random) {
        this.itemManager = itemManager;
        this.level = level;
        this.shipsDestroyedAtStart = shipsDestroyedAtStart;
        this.random = random;
    }

    public void onEnemyDestroyed(final EnemyShip enemy,
            final boolean playerAlive, final ShipType shipType,
            final boolean formationEmpty) {
        tryDropChameleonHull(enemy, playerAlive);
        tryDropAnnihilator(enemy);
        tryDropGrenade(enemy);
        tryDropShield(enemy, shipType, formationEmpty);
    }

    public void onBonusEnemyDestroyed(final EnemyShip bonusShip,
            final boolean playerAlive) {
        tryDropChameleonHull(bonusShip, playerAlive);
        if (!this.hyperfireDropped && bonusShip != null) {
            this.itemManager.spawnItem(new Hyperfire(
                    bonusShip.getPositionX(), bonusShip.getPositionY()));
            this.hyperfireDropped = true;
            this.logger.info("Hyperfire dropped from red UFO.");
        }
    }

    /** Checks the Twin Cannon spawn once per frame before items move. */
    public void update(final int shipsDestroyed, final int screenWidth,
                       final int spawnY) {
        if (!this.twinCannonSpawned && this.level >= TWIN_CANNON_REQUIRED_LEVEL
                && shipsDestroyed - this.shipsDestroyedAtStart
                        >= TWIN_CANNON_SPAWN_KILLS) {
            this.twinCannonSpawned = true;
            int spawnX = TWIN_CANNON_SPAWN_MARGIN + (int) (this.random.nextDouble()
                    * (screenWidth - 2 * TWIN_CANNON_SPAWN_MARGIN));
            this.itemManager.spawnItem(new TwinCanon(spawnX, spawnY));
        }
    }

    private void tryDropChameleonHull(final EnemyShip enemy,
            final boolean playerAlive) {
        if (!this.chameleonDropped && this.level >= CHAMELEON_REQUIRED_LEVEL
                && playerAlive && this.random.nextDouble() < CHAMELEON_DROP_CHANCE) {
            this.itemManager.spawnItem(new ChameleonHull(
                    enemy.getPositionX(), enemy.getPositionY()));
            this.chameleonDropped = true;
        }
    }

    private void tryDropAnnihilator(final EnemyShip enemy) {
        if (!this.annihilatorDropped
                && this.level == OrbitalAnnihilator.REQUIRED_LEVEL
                && this.random.nextDouble() < ANNIHILATOR_DROP_CHANCE) {
            this.itemManager.spawnItem(new AnnihilatorItem(
                    enemy.getPositionX(), enemy.getPositionY()));
            this.annihilatorDropped = true;
            this.logger.info("Orbital Annihilator dropped from enemy ship.");
        }
    }

    private void tryDropGrenade(final EnemyShip enemy) {
        if (this.grenadeDrops < MAX_GRENADE_DROPS_PER_LEVEL
                && this.random.nextDouble() < GRENADE_DROP_CHANCE) {
            this.grenadeDrops++;
            this.itemManager.spawnItem(new ClusterGrenadeItem(
                    enemy.getPositionX(), enemy.getPositionY()));
            this.logger.info("Grenade item dropped from enemy ship.");
        }
    }

    private void tryDropShield(final EnemyShip enemy, final ShipType shipType,
                               final boolean formationEmpty) {
        if (!this.shieldDropped && shipType.isShieldCapable()
                && (this.random.nextDouble() < SHIELD_DROP_CHANCE || formationEmpty)) {
            this.itemManager.spawnItem(new ShieldItem(
                    enemy.getPositionX(), enemy.getPositionY()));
            this.shieldDropped = true;
            this.logger.info("Shield item dropped from enemy ship at ("
                    + enemy.getPositionX() + ", " + enemy.getPositionY() + ").");
        }
    }
}
