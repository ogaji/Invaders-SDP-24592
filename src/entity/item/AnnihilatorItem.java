package entity.item;

import engine.DrawManager.SpriteType;

public class AnnihilatorItem extends Item {

    private static final int ITEM_WIDTH = 14;
    private static final int ITEM_HEIGHT = 28;

    public AnnihilatorItem(final int positionX, final int positionY) {
        super(positionX, positionY, ITEM_WIDTH, ITEM_HEIGHT,
            SpriteType.Explosion);
    }

    @Override
    public void applyEffect(final ItemEffectContext context) {
        if (context != null && context.getAnnihilator() != null) {
            context.getAnnihilator().arm();
        }
    }

    @Override
    public void removeEffect(final ItemEffectContext context) {
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public String getName() {
        return "Orbital Annihilator";
    }
}
