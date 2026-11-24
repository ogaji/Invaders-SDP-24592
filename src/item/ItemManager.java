package item;

import audio.AudioManager;
import engine.DrawManager;
import entity.Bullet;
import entity.Entity;
import entity.Ship;
import entity.item.Item;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ItemManager {

    private final List<Item> activeItems;
    private final int screenHeight;
    private final ItemPickupReceiver pickupReceiver;

    public ItemManager(final int screenHeight,
                       final ItemPickupReceiver pickupReceiver) {
        this.activeItems = new ArrayList<>();
        this.screenHeight = screenHeight;
        this.pickupReceiver = pickupReceiver;
    }

    public void spawnItem(final Item item) {
        this.activeItems.add(item);
    }

    public void update() {
        Iterator<Item> iterator = this.activeItems.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            item.update();

            if (item.getPositionY() > this.screenHeight) {
                iterator.remove();
            }
        }
    }

    public void checkCollisions(final Ship ship) {
        Iterator<Item> iterator = this.activeItems.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.getPickupMode() == Item.PickupMode.TOUCH
                && checkCollision(item, ship)
                && acceptPickup(item)) {
                iterator.remove();
            }
        }
    }

    /**
     * Checks player bullets against shot-collected items.
     *
     * @return Bullets that collected an item and should be recycled.
     */
    public Set<Bullet> checkBulletCollisions(final Set<Bullet> bullets,
                                             final Set<Bullet> alreadyConsumed) {
        Set<Bullet> hitBullets = new HashSet<>();
        for (Bullet bullet : bullets) {
            if (bullet.isEnemyBullet() || alreadyConsumed.contains(bullet)) {
                continue;
            }
            Iterator<Item> iterator = this.activeItems.iterator();
            while (iterator.hasNext()) {
                Item item = iterator.next();
                if (item.getPickupMode() == Item.PickupMode.SHOT
                    && checkCollision(item, bullet)
                    && acceptPickup(item)) {
                    iterator.remove();
                    hitBullets.add(bullet);
                    break;
                }
            }
        }
        return hitBullets;
    }

    private boolean acceptPickup(final Item item) {
        if (this.pickupReceiver == null
            || !this.pickupReceiver.onItemPickedUp(item)) {
            return false;
        }
        item.setCollected(true);
        AudioManager.playSFX("audio/sfx/item_pickup.wav");
        return true;
    }

    private boolean checkCollision(final Entity a, final Entity b) {
        return a.getPositionX() < b.getPositionX() + b.getWidth()
            && a.getPositionX() + a.getWidth() > b.getPositionX()
            && a.getPositionY() < b.getPositionY() + b.getHeight()
            && a.getPositionY() + a.getHeight() > b.getPositionY();
    }

    public void draw(final DrawManager drawManager) {
        for (Item item : this.activeItems) {
            item.draw(drawManager);
        }
    }
}
