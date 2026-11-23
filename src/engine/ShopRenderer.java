package engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.List;

import shop.PermanentUpgrade;
import currency.PlayerAssetsState;
import currency.ShopCatalog.Category;
import entity.ShipType;
import screen.Screen;
import shop.ShopController.SaveStatus;
import shop.ShopProduct;
import vfx.Icon;
import vfx.ShipSprites;

/** Draws the shop screen and its cards. */
final class ShopRenderer {
    private final DrawContext context;

    ShopRenderer(final DrawContext context) {
        this.context = context;
    }

    void drawShop(final Screen screen, final List<ShopProduct> upgrades,
            final List<ShopProduct> unlocks, final PlayerAssetsState assets,
            final int section, final int upgradeIndex, final int unlockIndex,
            final int actionIndex, final String feedback,
            final boolean purchaseSucceeded, final SaveStatus saveStatus) {
        Graphics graphics = context.graphics();
        int width = screen.getWidth();
        int height = screen.getHeight();
        graphics.setColor(Color.GREEN);
        context.drawCenteredBigString(screen, "Shop", 34);
        graphics.setColor(Color.WHITE);
        String credits = "Credits: " + assets.getBalance();
        context.drawCenteredRegularString(screen, credits, 61);
        context.drawIcon(Icon.COIN, width / 2
                - context.regularMetrics().stringWidth(credits)
                / 2 - Icon.COIN.getWidth() * 2 - 6, 47, 2);

        int cardSize = Math.max(76, Math.min(120,
                Math.min((width - 80) / 3, (height - 248) / 2)));
        drawShopShelf(screen, upgrades, assets, "Upgrades",
                "No upgrades available", 96, cardSize, upgradeIndex,
                section == 0);
        drawShopShelf(screen, unlocks, assets, "Unlocks",
                "No unlocks available", 132 + cardSize, cardSize,
                unlockIndex, section == 1);

        int actionsY = height - 96;
        drawShopText("Back (Esc)", 31, actionsY, 12,
                section == 2 && actionIndex == 0 ? Color.GREEN : Color.WHITE,
                120);
        if (saveStatus == SaveStatus.FAILED) {
            drawShopText("Retry save (R)", width / 2, actionsY, 12,
                    section == 2 && actionIndex == 1 ? Color.GREEN
                            : Color.WHITE,
                    width / 2 - 24);
        }
        graphics.setColor(purchaseSucceeded ? Color.GREEN : Color.ORANGE);
        drawShopCentered(screen, feedback, height - 70);
        String saveMessage = saveStatus == SaveStatus.FAILED
                ? "Save failed. Progress kept this session."
                : saveStatus == SaveStatus.SAVED ? "Progress saved." : "";
        graphics.setColor(saveStatus == SaveStatus.FAILED ? Color.ORANGE
                : Color.GRAY);
        drawShopCentered(screen, saveMessage, height - 52);
        graphics.setColor(Color.GRAY);
        drawShopCentered(screen,
                "Up/Down: section   Left/Right: product", height - 37);
        drawShopCentered(screen,
                "WASD also works   Space/Enter: confirm", height - 21);
    }

    private void drawShopShelf(final Screen screen,
            final List<ShopProduct> products, final PlayerAssetsState assets,
            final String heading, final String emptyText, final int top,
            final int size, final int index, final boolean active) {
        Graphics graphics = context.graphics();
        int width = screen.getWidth();
        drawShopText(heading, 31, top - 12, 12,
                active ? Color.GREEN : Color.WHITE, width - 110);
        String count = products.isEmpty() ? "0 / 0"
                : (index + 1) + " / " + products.size();
        drawShopText(count, width - 70, top - 12, 10, Color.GRAY, 52);
        if (products.isEmpty()) {
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRect(31, top, width - 63, size);
            graphics.setColor(Color.GRAY);
            drawShopCentered(screen, emptyText, top + size / 2);
            return;
        }

        int first = index / 3 * 3;
        int end = Math.min(products.size(), first + 3);
        int rowWidth = (end - first) * size + (end - first - 1) * 10;
        int startX = (width - rowWidth) / 2;
        for (int productIndex = first; productIndex < end; productIndex++) {
            drawShopCard(products.get(productIndex), assets,
                    startX + (productIndex - first) * (size + 10), top, size,
                    active && productIndex == index);
        }
        if (products.size() > 1) {
            int cy = top + size / 2;
            graphics.setColor(active ? Color.GREEN : Color.GRAY);
            graphics.fillPolygon(new int[] { 12, 18, 18 },
                    new int[] { cy, cy - 5, cy + 5 }, 3);
            graphics.fillPolygon(
                    new int[] { width - 13, width - 19, width - 19 },
                    new int[] { cy, cy - 5, cy + 5 }, 3);
        }
    }

    private void drawShopCard(final ShopProduct product,
            final PlayerAssetsState assets, final int x, final int y,
            final int size, final boolean selected) {
        Graphics graphics = context.graphics();
        graphics.setColor(selected ? Color.GREEN : Color.DARK_GRAY);
        graphics.drawRect(x, y, size, size);
        boolean owned = assets.getUnlockedContentIds().contains(product.getId());
        boolean available = product.isAvailableByDefault();
        String badge = available ? "Available" : owned ? "Owned" : "";
        drawShopText(badge, x + 7, y + 12, 9, Color.GRAY, size - 14);
        graphics.setColor(selected ? Color.GREEN : Color.WHITE);
        drawShopProductIcon(product, x + size / 2, y + 18);
        drawShopText(product.getName(), x + 9, y + 60, 11,
                selected ? Color.GREEN : Color.WHITE, size - 18);
        drawShopDescription(product.getDescription(), x + 9, y + 75,
                size - 18);
        graphics.setColor(Color.DARK_GRAY);
        graphics.drawLine(x + 9, y + size - 25, x + size - 9,
                y + size - 25);
        String price = available || owned ? "Unlocked"
                : product.getPrice() + " C";
        drawShopText(price, x + 9, y + size - 9, 11,
                available || owned ? Color.GRAY : Color.WHITE, size - 18);
    }

    private void drawShopProductIcon(final ShopProduct product,
            final int centerX, final int y) {
        Graphics graphics = context.graphics();
        ShipType shipType = product.getShipType();
        if (shipType != null) {
            boolean[][] sprite = ShipSprites.forType(shipType);
            graphics.setColor(Color.WHITE);
            for (int x = 0; x < sprite.length; x++) {
                for (int row = 0; row < sprite[x].length; row++) {
                    if (sprite[x][row]) {
                        graphics.fillRect(centerX - sprite.length + x * 2,
                                y + 8 + row * 2, 2, 2);
                    }
                }
            }
            return;
        }
        PermanentUpgrade upgrade = PermanentUpgrade.forProduct(product.getId());
        if (upgrade == null) {
            drawShopCategoryIcon(product.getCategory() == Category.UNLOCKABLE,
                    centerX - 10, y + 2);
            return;
        }
        Icon icon = switch (upgrade) {
        case HULL -> Icon.UPGRADE_HULL;
        case ENGINE -> Icon.UPGRADE_ENGINE;
        case RELOADER -> Icon.UPGRADE_RELOADER;
        };
        context.drawIcon(icon, centerX - icon.getWidth(), y, 2);
    }

    private void drawShopCategoryIcon(final boolean permanent, final int x,
            final int y) {
        Graphics graphics = context.graphics();
        if (permanent) {
            graphics.drawRect(x + 5, y, 10, 9);
            graphics.fillRect(x, y + 8, 21, 15);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(x + 9, y + 12, 3, 7);
        } else {
            graphics.drawRect(x, y + 3, 20, 19);
            graphics.drawRect(x + 1, y + 4, 18, 17);
            graphics.drawLine(x + 1, y + 9, x + 19, y + 9);
            graphics.drawRect(x + 7, y + 1, 6, 4);
        }
    }

    private void drawShopDescription(final String description, final int x,
            final int y, final int width) {
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont().deriveFont(10f));
        graphics.setColor(Color.GRAY);
        FontMetrics metrics = graphics.getFontMetrics();
        String remaining = description.trim();
        for (int line = 0; line < 2 && !remaining.isEmpty(); line++) {
            String text = remaining;
            if (line == 0 && metrics.stringWidth(text) > width) {
                int end = text.length();
                while (end > 1
                        && metrics.stringWidth(text.substring(0, end)) > width) {
                    end--;
                }
                int space = text.lastIndexOf(' ', end);
                if (space > 0) {
                    end = space;
                }
                text = remaining.substring(0, end);
                remaining = remaining.substring(end).trim();
            } else {
                remaining = "";
            }
            drawShopLabel(text, x, y + line * 12, width);
        }
    }

    private void drawShopText(final String text, final int x, final int y,
            final int size, final Color color, final int width) {
        Graphics graphics = context.graphics();
        graphics.setFont(context.regularFont().deriveFont((float) size));
        graphics.setColor(color);
        drawShopLabel(text, x, y, width);
    }

    private void drawShopCentered(final Screen screen, final String text,
            final int y) {
        Graphics graphics = context.graphics();
        Font fitted = context.regularFont().deriveFont(11f);
        while (fitted.getSize() > 9 && graphics.getFontMetrics(fitted)
                .stringWidth(text) > screen.getWidth() - 32) {
            fitted = fitted.deriveFont((float) fitted.getSize() - 1);
        }
        graphics.setFont(fitted);
        int x = Math.max(16, (screen.getWidth()
                - graphics.getFontMetrics().stringWidth(text)) / 2);
        drawShopLabel(text, x, y, screen.getWidth() - x - 16);
    }

    private void drawShopLabel(final String text, final int x, final int y,
            final int maxWidth) {
        Graphics graphics = context.graphics();
        String label = text;
        FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(label) > maxWidth) {
            while (!label.isEmpty()
                    && metrics.stringWidth(label + "...") > maxWidth) {
                label = label.substring(0, label.length() - 1);
            }
            label += "...";
        }
        graphics.drawString(label, x, y);
    }
}
