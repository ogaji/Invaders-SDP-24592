package shop;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import engine.Core;
import entity.ShipType;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coordinates permanent purchases and saving on the gameplay thread. */
public final class ShopController {
    public enum SaveStatus { NOT_NEEDED, SAVED, FAILED }

    public interface AssetSaver {
        void save(PlayerAssetsState state) throws IOException;
    }

    private final PlayerAssets assets;
    private final AssetSaver saver;
    private final PurchaseSystem purchases;
    private final Map<String, ShopProduct> productsById;
    private SaveStatus saveStatus = SaveStatus.NOT_NEEDED;

    public ShopController(PlayerAssets assets, List<ShopProduct> definitions, AssetSaver saver) {
        if (assets == null || definitions == null || saver == null) {
            throw new IllegalArgumentException("Shop dependencies cannot be null.");
        }
        this.assets = assets;
        this.saver = saver;
        Map<String, ShopProduct> indexed = new LinkedHashMap<>();
        for (ShopProduct product : definitions) {
            if (product == null) throw new IllegalArgumentException("Product cannot be null.");
            if (indexed.containsKey(product.getId())) {
                throw new IllegalArgumentException("Duplicate product: " + product.getId());
            }
            String requiredId = product.getRequiredUnlockId();
            // An earlier definition is required so prerequisite chains cannot cycle.
            if (requiredId != null && !indexed.containsKey(requiredId)) {
                throw new IllegalArgumentException("Prerequisite for " + product.getId()
                        + " must be listed earlier: " + requiredId);
            }
            indexed.put(product.getId(), product);
        }
        productsById = Collections.unmodifiableMap(indexed);
        purchases = new PurchaseSystem(assets, productsById);
    }

    public synchronized List<ShopProduct> getProducts() {
        return ShopProducts.visibleProducts(productsById.values(), assets.snapshot());
    }

    /** Current spendable Credits, without copying ownership or legacy save data. */
    public synchronized int getBalance() {
        return assets.getBalance();
    }

    /** Uses the same ship availability and saved ownership as the Shop. */
    public synchronized boolean isShipAvailable(ShipType type) {
        if (type == null) return false;
        ShopProduct product = productsById.get(ShopProduct.shipId(type));
        return product != null && product.getShipType() == type && purchases.isAvailable(product);
    }

    public synchronized PlayerAssetsState snapshot() {
        return assets.snapshot();
    }

    /** Reward saves are tracked by CampaignRewards separately. */
    public synchronized SaveStatus getSaveStatus() {
        return saveStatus;
    }

    public synchronized PurchaseResult purchase(String productId) {
        PurchaseResult result = purchases.purchase(productId);
        if (result == PurchaseResult.SUCCESS) saveLatestAssets();
        return result;
    }

    /** Save the latest state without buying or granting the upgrade again. */
    public synchronized boolean retrySave() {
        return saveStatus != SaveStatus.FAILED || saveLatestAssets();
    }

    private boolean saveLatestAssets() {
        try {
            saver.save(assets.snapshot());
            saveStatus = SaveStatus.SAVED;
            return true;
        } catch (IOException | SecurityException exception) {
            saveStatus = SaveStatus.FAILED;
            Core.getLogger().warning("Asset save failed; runtime assets retained: " + exception);
            return false;
        }
    }
}
