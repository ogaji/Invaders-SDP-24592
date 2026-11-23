package entity.item;

import engine.DrawManager;
import entity.Entity;

public abstract class Item extends Entity {

    /** How this item is collected from the field. */
    public enum PickupMode {
        TOUCH,
        SHOT
    }

    /** What happens when a timed effect is picked up while already active. */
    public enum ReapplyPolicy {
        REFRESH,
        IGNORE
    }

    private static final String DEFAULT_EXPIRY_SOUND =
        "audio/sfx/item_buff_expire.wav";

    /** Falling speed of the item on screen. */
    protected int speed;
    /** Whether the item has been collected. */
    protected boolean isCollected;

    public Item(final int positionX, final int positionY, final int width,
                final int height, final DrawManager.SpriteType spriteType) {
        super(positionX, positionY, width, height, java.awt.Color.WHITE);
        this.spriteType = spriteType;
        this.speed = 2;
        this.isCollected = false;
    }

    public void update() {
        this.positionY += this.speed;
    }

    public void draw(final DrawManager drawManager) {
        drawManager.drawEntity(this, this.positionX, this.positionY);
    }

    public PickupMode getPickupMode() {
        return PickupMode.TOUCH;
    }

    public ReapplyPolicy getReapplyPolicy() {
        return ReapplyPolicy.REFRESH;
    }

    public String getExpirySoundPath() {
        return DEFAULT_EXPIRY_SOUND;
    }

    public boolean isTimed() {
        return getDuration() > 0;
    }

    public abstract void applyEffect(final ItemEffectContext context);

    public abstract void removeEffect(final ItemEffectContext context);

    public abstract long getDuration();

    public abstract String getName();

    public boolean isCollected() {
        return this.isCollected;
    }

    public void setCollected(final boolean collected) {
        this.isCollected = collected;
    }
}
