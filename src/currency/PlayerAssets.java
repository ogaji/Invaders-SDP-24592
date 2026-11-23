package currency;

import java.util.Collections;
import java.util.Map;

/** Shared balance and permanent ownership for purchases, rewards and saving. */
public class PlayerAssets {
    public enum UnlockResult { UNLOCKED, INSUFFICIENT_CURRENCY, UNLOCK_REJECTED }

    private CurrencyBalance currencyBalance = new CurrencyBalance();
    private UnlockManager unlockManager = new UnlockManager();
    // Preserve old save fields without exposing an inventory purchase or use path.
    private Map<String, Integer> legacyItemQuantities = Collections.emptyMap();

    public int getBalance() {
        return currencyBalance.getBalance();
    }

    public boolean canAfford(int amount) {
        return currencyBalance.canAfford(amount);
    }

    public boolean isUnlocked(String contentId) {
        return unlockManager.isUnlocked(contentId);
    }

    boolean addCurrency(int amount) {
        return currencyBalance.add(amount);
    }

    boolean deductCurrency(int amount) {
        return currencyBalance.deduct(amount);
    }

    boolean unlock(String contentId) {
        return unlockManager.unlock(contentId);
    }

    /** Deducts currency for an unlock and refunds it if ownership cannot be granted. */
    public UnlockResult unlockWithCurrency(String contentId, int amount) {
        if (!deductCurrency(amount)) {
            return UnlockResult.INSUFFICIENT_CURRENCY;
        }
        if (!unlock(contentId)) {
            addCurrency(amount);
            return UnlockResult.UNLOCK_REJECTED;
        }
        return UnlockResult.UNLOCKED;
    }

    public PlayerAssetsState snapshot() {
        return new PlayerAssetsState(getBalance(), unlockManager.copyUnlockedContentIds(),
                legacyItemQuantities);
    }

    /** Restore the validated snapshot atomically, retaining legacy save data. */
    public boolean restore(PlayerAssetsState state) {
        if (state == null) return false;
        CurrencyBalance restoredBalance;
        try {
            restoredBalance = new CurrencyBalance(state.getBalance());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        UnlockManager restoredUnlocks = new UnlockManager();
        for (String id : state.getUnlockedContentIds()) {
            if (!restoredUnlocks.unlock(id)) return false;
        }
        currencyBalance = restoredBalance;
        unlockManager = restoredUnlocks;
        legacyItemQuantities = state.getItemQuantities();
        return true;
    }
}
