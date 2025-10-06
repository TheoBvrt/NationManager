package ch.swaford.servermanager.shop;

public class ShopItemData {
    public String itemId;
    public String itemCategory;
    public int sellPrice;
    public int buyPrice;
    public int quantity;
    int basePrice;
    float totalSell;
    float totalBuy;
    public boolean limited;
    public int maxBuyValue;
    public int defaultBuyValue;
    public boolean enabled;

    public ShopItemData(String itemId, int basePrice, String itemCategory, int quantity, boolean limited, int maxQuantity) {
        this.itemId = itemId;
        this.buyPrice = (int) (basePrice * 1.1f);
        this.basePrice = basePrice;
        this.sellPrice = basePrice;
        this.itemCategory = itemCategory;
        this.totalSell = 1000;
        this.totalBuy = 1000;
        this.enabled = false;
        this.quantity = quantity;
        this.limited = limited;
        this.maxBuyValue = maxQuantity;
        this.defaultBuyValue = maxQuantity;
    }
}
