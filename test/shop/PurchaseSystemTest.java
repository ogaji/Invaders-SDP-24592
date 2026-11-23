package shop;

import currency.PlayerAssets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.ShopProduct.Category;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseSystemTest {
    private PlayerAssets assets;
    private ShopController store;

    @BeforeEach
    void setUp() {
        assets = ShopTestFixtures.fundedAssets();
        store = new ShopController(assets, List.of(
                new ShopProduct("upgrade", "Upgrade", Category.UPGRADE, 100),
                new ShopProduct("stage_02", "Stage 2", Category.UNLOCKABLE, 500),
                new ShopProduct("expensive", "Expensive", Category.UPGRADE, Integer.MAX_VALUE)),
                state -> { });
    }

    @Test
    void upgradePurchaseGrantsPermanentOwnership() {
        assertEquals(PurchaseResult.SUCCESS, store.purchase("upgrade"));
        assertEquals(900, assets.getBalance());
        assertTrue(assets.isUnlocked("upgrade"));
        assertTrue(assets.snapshot().getItemQuantities().isEmpty());
    }

    @Test
    void permanentUnlockPurchaseRemainsAvailable() {
        assertEquals(PurchaseResult.SUCCESS, store.purchase("stage_02"));
        assertTrue(assets.isUnlocked("stage_02"));
        assertEquals(500, assets.getBalance());
    }

    @Test
    void neitherCategoryCanBeChargedTwice() {
        assertEquals(PurchaseResult.SUCCESS, store.purchase("upgrade"));
        assertEquals(PurchaseResult.SUCCESS, store.purchase("stage_02"));
        assertEquals(PurchaseResult.ALREADY_AVAILABLE, store.purchase("upgrade"));
        assertEquals(PurchaseResult.ALREADY_AVAILABLE, store.purchase("stage_02"));
        assertEquals(400, assets.getBalance());
    }

    @Test
    void insufficientFundsDoNotGrantOwnership() {
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, store.purchase("expensive"));
        assertEquals(1000, assets.getBalance());
        assertFalse(assets.isUnlocked("expensive"));
    }

    @Test
    void invalidAndUnknownIdsDoNotCharge() {
        assertEquals(PurchaseResult.INVALID_PRODUCT_ID, store.purchase(null));
        assertEquals(PurchaseResult.INVALID_PRODUCT_ID, store.purchase("   "));
        assertEquals(PurchaseResult.UNKNOWN_PRODUCT, store.purchase("unknown"));
        assertEquals(1000, assets.getBalance());
        assertTrue(assets.snapshot().getUnlockedContentIds().isEmpty());
    }
}
