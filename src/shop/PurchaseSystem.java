package shop;

import currency.PlayerAssets;
import java.util.Map;

/** Checks product availability and applies purchases to the player's assets. */
final class PurchaseSystem {
    private final PlayerAssets assets;
    private final Map<String, ShopProduct> productsById;

    PurchaseSystem(PlayerAssets assets, Map<String, ShopProduct> productsById) {
        this.assets = assets;
        this.productsById = productsById;
    }

    boolean isAvailable(ShopProduct product) {
        return product.isAvailableByDefault() || assets.isUnlocked(product.getId());
    }

    PurchaseResult purchase(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return PurchaseResult.INVALID_PRODUCT_ID;
        }
        ShopProduct product = productsById.get(productId);
        if (product == null) {
            return PurchaseResult.UNKNOWN_PRODUCT;
        }
        if (isAvailable(product)) {
            return PurchaseResult.ALREADY_AVAILABLE;
        }

        for (String requiredId = product.getRequiredUnlockId(); requiredId != null;) {
            ShopProduct prerequisite = productsById.get(requiredId);
            if (!isAvailable(prerequisite)) return PurchaseResult.PREREQUISITE_NOT_MET;
            requiredId = prerequisite.getRequiredUnlockId();
        }

        return switch (assets.unlockWithCurrency(productId, product.getPrice())) {
            case UNLOCKED -> PurchaseResult.SUCCESS;
            case INSUFFICIENT_CURRENCY -> PurchaseResult.INSUFFICIENT_FUNDS;
            case UNLOCK_REJECTED -> PurchaseResult.UNLOCK_FAILED;
        };
    }
}
