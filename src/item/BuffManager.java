package item;

import audio.AudioManager;
import entity.item.Item;
import entity.item.ItemEffectContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BuffManager {

    private static final class ActiveBuff {
        private final Item item;
        private long expirationTime;

        private ActiveBuff(final Item item, final long expirationTime) {
            this.item = item;
            this.expirationTime = expirationTime;
        }
    }

    private final Map<Class<? extends Item>, ActiveBuff> activeBuffs;

    public BuffManager() {
        this.activeBuffs = new HashMap<>();
    }

    /** Starts or refreshes a timed effect. Instant items are ignored. */
    public void activate(final Item item, final ItemEffectContext context) {
        if (item == null || !item.isTimed()) {
            return;
        }

        Class<? extends Item> kind = item.getClass();
        ActiveBuff existing = this.activeBuffs.get(kind);
        if (existing != null
            && item.getReapplyPolicy() == Item.ReapplyPolicy.IGNORE) {
            return;
        }

        item.applyEffect(context);
        long expirationTime = System.currentTimeMillis() + item.getDuration();
        this.activeBuffs.put(kind, new ActiveBuff(item, expirationTime));
    }

    public void update(final ItemEffectContext context) {
        long currentTime = System.currentTimeMillis();
        Iterator<ActiveBuff> iterator = this.activeBuffs.values().iterator();

        while (iterator.hasNext()) {
            ActiveBuff buff = iterator.next();
            if (currentTime >= buff.expirationTime) {
                AudioManager.playSFX(buff.item.getExpirySoundPath());
                buff.item.removeEffect(context);
                iterator.remove();
            }
        }
    }

    public void resetAllBuffs(final ItemEffectContext context) {
        for (ActiveBuff buff : this.activeBuffs.values()) {
            buff.item.removeEffect(context);
        }
        this.activeBuffs.clear();
    }

    public Map<String, Long> getActiveBuffsRemainingTime() {
        Map<String, Long> remainingTimes = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        for (ActiveBuff buff : this.activeBuffs.values()) {
            long remaining = Math.max(
                0,
                (buff.expirationTime - currentTime) / 1000
            );
            remainingTimes.put(buff.item.getName(), remaining);
        }

        return remainingTimes;
    }
}
