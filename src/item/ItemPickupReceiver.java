package item;

import entity.item.Item;

/** Receives items picked up from the field. Returning false leaves the item on the field. */
@FunctionalInterface
public interface ItemPickupReceiver {
    boolean onItemPickedUp(Item item);
}
