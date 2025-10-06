package ch.swaford.servermanager.shop;

import java.util.ArrayList;
import java.util.List;

public class ShopData {
    List<String> categories;
    List<ShopItemData> items;

    public ShopData() {
        categories = new ArrayList<>();
        items = new ArrayList<>();
    }
}
