package ch.swaford.servermanager.networktransfer;

public record ServerShopItemData (
    String itemId,
    String itemCategory,
    int sellPrice,
    int buyPrice,
    int quantity
) {}