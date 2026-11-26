package shop;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import shop.ShopProduct.Category;
import static org.junit.jupiter.api.Assertions.*;

class PermanentUpgradeTest {
    @ParameterizedTest
    @EnumSource(PermanentUpgrade.class)
    void readsOnlyConsecutiveOwnedTiers(PermanentUpgrade upgrade) {
        PlayerAssetsState incomplete = new PlayerAssetsState(42,
                new HashSet<String>(Arrays.asList(upgrade.getProductId(1),
                        upgrade.getProductId(3), "other.content")), null);
        assertEquals(1, upgrade.getLevel(incomplete));
        assertEquals(0, upgrade.getLevel(new PlayerAssetsState(42,
                Collections.singleton(upgrade.getProductId(3)), null)));
        assertEquals(0, upgrade.getBonus(0));
        assertEquals(3, upgrade.getMaxLevel());
        assertThrows(IllegalArgumentException.class, () -> upgrade.getProductId(0));
        assertThrows(IllegalArgumentException.class, () -> upgrade.getPrice(4));
    }

    @Test
    void prerequisitesRejectSkippedTiersBeforeCharging() {
        ShopProduct first = new ShopProduct("tier1", "Tier 1", Category.UPGRADE, 100);
        ShopProduct second = new ShopProduct("tier2", "Tier 2", Category.UPGRADE, 250, "Tier 2", "tier1");
        ShopProduct third = new ShopProduct("tier3", "Tier 3", Category.UPGRADE, 500, "Tier 3", "tier2");
        ShopProduct cycle = new ShopProduct("tier1", "Tier 1", Category.UPGRADE, 100, "Tier 1", "tier3");
        ShopProduct missing = new ShopProduct("bad", "Bad", Category.UPGRADE, 10, "Bad", "missing");
        PlayerAssets assets = ShopTestFixtures.fundedAssets();
        ShopController.AssetSaver ignoreSave = state -> { };
        assertThrows(IllegalArgumentException.class,
                () -> new ShopController(assets, List.of(second, first), ignoreSave));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopController(assets, List.of(cycle, second, third), ignoreSave));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopProduct("loop", "Loop", Category.UPGRADE, 100, "Loop", "loop"));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopController(assets, List.of(missing), ignoreSave));
        ShopController shop = new ShopController(assets, List.of(first, second, third), ignoreSave);
        assertEquals(PurchaseResult.PREREQUISITE_NOT_MET, shop.purchase("tier2"));
        assertEquals(PurchaseResult.PREREQUISITE_NOT_MET, shop.purchase("tier3"));
        assertEquals(1000, assets.getBalance());
        // A damaged/imported save containing tier2 must not bypass tier1 either.
        assertTrue(assets.restore(new PlayerAssetsState(1000, Collections.singleton("tier2"), null)));
        assertEquals(PurchaseResult.PREREQUISITE_NOT_MET, shop.purchase("tier3"));
        assertEquals(PurchaseResult.SUCCESS, shop.purchase("tier1"));
        assertEquals(PurchaseResult.SUCCESS, shop.purchase("tier3"));
        assertEquals(400, assets.getBalance());
        assertEquals(PurchaseResult.ALREADY_AVAILABLE, shop.purchase("tier3"));
        assertEquals(400, assets.getBalance());
    }
}
