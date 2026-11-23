package item;

import entity.item.Item;
import entity.item.ItemEffectContext;

/** Applies picked-up items immediately, routing timed ones through BuffManager. */
public class ItemApplier implements ItemPickupReceiver {

    private final BuffManager buffManager;
    private final ItemEffectContext context;

    public ItemApplier(final BuffManager buffManager,
                       final ItemEffectContext context) {
        this.buffManager = buffManager;
        this.context = context;
    }

    @Override
    public boolean onItemPickedUp(final Item item) {
        if (item == null) {
            return false;
        }
        if (item.isTimed()) {
            this.buffManager.activate(item, this.context);
        } else {
            item.applyEffect(this.context);
        }
        return true;
    }
}
