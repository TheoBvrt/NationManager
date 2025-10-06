package ch.swaford.servermanager.shop;

import ch.swaford.servermanager.EconomyManager;
import ch.swaford.servermanager.classement.ClassementEvent;
import ch.swaford.servermanager.networktransfer.ServerShopItemData;
import ch.swaford.servermanager.networktransfer.ServerShopPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ShopManager {
    private static final String FILE_PATH = "shop.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static ShopData cache = new ShopData();

    public static ShopData loadShop() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            ShopData shopData = gson.fromJson(reader, ShopData.class);
            if (shopData != null) {
                return shopData;
            }
            else {
                return new ShopData();
            }
        } catch (IOException e) {
            return new ShopData();
        }
    }

    public static void saveShopData(ShopData shopData) {
        ServerShopPayload payload = new ServerShopPayload(getServerShopItemData());

        PacketDistributor.sendToAllPlayers(payload);
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(shopData, writer);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            // Remplace le fichier original par le temporaire de façon atomique
            Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cache = shopData;
    }

    public static void addShopItem(ShopItemData shopItemData) {
        ShopData shopData = loadShop();
        List<ShopItemData> shopItemDataList = shopData.items;
        shopItemDataList.add(shopItemData);
        saveShopData(shopData);
    }

    public static boolean itemIsRegistered(String itemId) {
        ShopData shopData = cache;
        List<ShopItemData> shopItemDataList = shopData.items;
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    public static int getCountOfItem(ServerPlayer player, Item item) {
        int count = 0;

        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.is(item)) {
                count += itemStack.getCount();
            }
        }
        return count;
    }

    public static boolean isItemAvailableForBuy (Item item) {
        ShopData shopData = cache;
        List<ShopItemData> shopItemDataList = shopData.items;
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(item.toString())) {
                if (shopItemData.limited && shopItemData.maxBuyValue < shopItemData.quantity)
                    return false;
                return (shopItemData.enabled);
            }
        }
        return false;
    }

    public static boolean isItemAvailableForSell (Item item) {
        ShopData shopData = cache;
        List<ShopItemData> shopItemDataList = shopData.items;
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(item.toString())) {
                return (shopItemData.enabled);
            }
        }
        return false;
    }

    public static void sellItem(ServerPlayer player, Item itemToSell) {
        int itemCount = getQuantity(itemToSell);
        player.getInventory().clearOrCountMatchingItems(
                item -> item.is(itemToSell),
                itemCount,
                player.inventoryMenu.getCraftSlots()
        );
        ShopData shopData = loadShop();
        List<ShopItemData> shopItemDataList = shopData.items;

        String displayName = new ItemStack(itemToSell).getHoverName().getString();
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(itemToSell.toString())) {
                if (!shopItemData.limited)
                    shopItemData.totalSell += shopItemData.quantity;
                EconomyManager.addMoney(player.getStringUUID(), shopItemData.sellPrice);
                player.sendSystemMessage(Component.literal("§lShop : vous avez vendu §e§lx" + itemCount + " §e§l" + displayName + "§f§l pour §e§l" + shopItemData.sellPrice + "€"));
                ClassementEvent.onSell(player, shopItemData.quantity);
                if (shopItemData.limited)
                    shopItemData.maxBuyValue += shopItemData.quantity;
                else {
                    shopItemData.sellPrice = (int) (shopItemData.basePrice * ((double)shopItemData.totalBuy / shopItemData.totalSell));
                    shopItemData.buyPrice = (int) (shopItemData.sellPrice * 1.1f);
                }
            }
        }
        saveShopData(shopData);
        ServerShopPayload payload = new ServerShopPayload(getServerShopItemData());
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void buyItem(ServerPlayer player, Item itemToBuy) {
        int quantity = getQuantity(itemToBuy);
        ItemStack itemStack = new ItemStack(itemToBuy, quantity);

        player.getInventory().add(itemStack);
        if (!itemStack.isEmpty()) {
            player.drop(itemStack, false);
        }

        ShopData shopData = loadShop();
        List<ShopItemData> shopItemDataList = shopData.items;

        String displayName = new ItemStack(itemToBuy).getHoverName().getString();
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(itemToBuy.toString())) {
                if (!shopItemData.limited)
                    shopItemData.totalBuy += quantity;
                EconomyManager.subMoney(player.getStringUUID(), shopItemData.buyPrice);
                player.sendSystemMessage(Component.literal("§lShop : vous avez acheté §e§lx" + quantity + " §e§l" + displayName + "§f§l pour §e§l" + shopItemData.buyPrice + "€"));
                if (shopItemData.limited)
                    shopItemData.maxBuyValue -= quantity;
                else {
                    shopItemData.sellPrice = (int) (shopItemData.basePrice * ((double)shopItemData.totalBuy / shopItemData.totalSell));
                    shopItemData.buyPrice = (int) (shopItemData.sellPrice * 1.1f);
                }
            }
        }
        saveShopData(shopData);
        ServerShopPayload payload = new ServerShopPayload(getServerShopItemData());
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void priceFluctuation() {
        ShopData shopData = loadShop();
        List<ShopItemData> shopItemDataList = shopData.items;

        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.limited)
                continue;
            boolean update = false;
            double rand = Math.random();
            if (rand < (double) 21 /42) {
                shopItemData.totalBuy ++;
                update = true;
            }
            if (shopItemData.totalSell > shopItemData.totalBuy) {
                shopItemData.totalBuy += 21 * (shopItemData.totalSell - shopItemData.totalBuy) / 43200;
                update = true;
            }
            if (update) {
                shopItemData.sellPrice = (int) (shopItemData.basePrice * ((double)shopItemData.totalBuy / shopItemData.totalSell));
                shopItemData.buyPrice = (int) (shopItemData.sellPrice * 1.1f);
            }
        }

        saveShopData(shopData);
        ServerShopPayload payload = new ServerShopPayload(getServerShopItemData());
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void reloadShopItemCategory(String categorie) {
        ShopData shopData = loadShop();
        List<ShopItemData> shopItemDataList = new ArrayList<>(getShopItemDataByCategory(categorie, shopData));

        Random rand = new Random();
        Set<Integer> set = new HashSet<>();
        int max =  shopItemDataList.size();

        if (shopItemDataList.size() <= 4) {
            for (ShopItemData shopItemData : shopItemDataList) {
                shopItemData.enabled = true;
            }
            return;
        }
        while (set.size() < 4) {
            set.add(rand.nextInt(max));
        }
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.limited)
                shopItemData.maxBuyValue = shopItemData.defaultBuyValue;
            shopItemData.enabled = false;
        }

        for (Integer integer : set) {
            ShopItemData shopItemData = shopItemDataList.get(integer);
            shopItemData.enabled = true;
        }
        saveShopData(shopData);
    }

    public static void updateShopItem() {
        ShopData shopData = loadShop();
        List<List<ShopItemData>> listOfItemByCategory = new ArrayList<>();
        for (String category : shopData.categories) {
            listOfItemByCategory.add(getShopItemDataByCategory(category, shopData));
        }

        for (List<ShopItemData> shopItemDataList : listOfItemByCategory) {
            Random rand = new Random();
            Set<Integer> set = new HashSet<>();
            int max =  shopItemDataList.size();

            if (shopItemDataList.size() <= 4) {
                for (ShopItemData shopItemData : shopItemDataList) {
                    shopItemData.enabled = true;
                }
                continue;
            }
            while (set.size() < 4) {
                set.add(rand.nextInt(max));
            }
            for (ShopItemData shopItemData : shopItemDataList) {
                if (shopItemData.limited)
                    shopItemData.maxBuyValue = shopItemData.defaultBuyValue;
                shopItemData.enabled = false;
            }

            for (Integer integer : set) {
                ShopItemData shopItemData = shopItemDataList.get(integer);
                shopItemData.enabled = true;
            }
        }
        
//        List<ShopItemData> shopItemDataList = shopData.items;
//        Random rand = new Random();
//        Set<Integer> set = new HashSet<>();
//        int max =  shopItemDataList.size();
//
//        while (set.size() < (2 * listOfItemByCategory.size())) {
//            int n = rand.nextInt(max);
//            set.add(n);
//        }
//
//        for (Integer i : set) {
//            ShopItemData shopItemData = shopItemDataList.get(i);
//            shopItemData.enabled = true;
//        }

        saveShopData(shopData);
        ServerShopPayload payload = new ServerShopPayload(getServerShopItemData());
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static List<ServerShopItemData> getServerShopItemData() {
        ShopData shopData = cache;
        List<ShopItemData> shopItemDataList = shopData.items;
        List<ServerShopItemData> serverShopItemDataList = new ArrayList<>();
        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.enabled) {
                int quantity = shopItemData.limited ? shopItemData.maxBuyValue : shopItemData.quantity;
                serverShopItemDataList.add(new ServerShopItemData(
                        shopItemData.itemId,
                        shopItemData.itemCategory,
                        shopItemData.sellPrice,
                        shopItemData.buyPrice,
                        quantity
                ));
            }
        }
        return serverShopItemDataList;
    }

    private static List<ShopItemData> getShopItemDataByCategory(String category, ShopData shopData) {
        List<ShopItemData> shopItemDataList = new ArrayList<>();

        for (ShopItemData shopItemData : shopData.items) {
            if (shopItemData.itemCategory.equals(category)) {
                shopItemDataList.add(shopItemData);
            }
        }
        return shopItemDataList;
    }

    public static int getQuantity(Item item) {
        ShopData shopData = cache;
        List<ShopItemData> shopItemDataList = shopData.items;

        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(item.toString())) {
                return shopItemData.quantity;
            }
        }
        return 0;
    }

    public static int getBuyPrice(Item item) {
        ShopData shopData = cache;
        List<ShopItemData> shopItemDataList = shopData.items;

        for (ShopItemData shopItemData : shopItemDataList) {
            if (shopItemData.itemId.equals(item.toString())) {
                return shopItemData.buyPrice;
            }
        }
        return 0;
    }

    public static void resetPrice() {
        ShopData shopData = loadShop();
        List<ShopItemData> shopItemDataList = shopData.items;

        for (ShopItemData shopItemData : shopItemDataList) {
            shopItemData.totalBuy = 1000;
            shopItemData.totalSell = 1000;
            shopItemData.sellPrice = (int) (shopItemData.basePrice * ((double)shopItemData.totalBuy / shopItemData.totalSell));
            shopItemData.buyPrice = (int) (shopItemData.sellPrice * 1.1f);
        }
        saveShopData(shopData);
    }

    public static List<String> getShopCategories() {
        ShopData shopData = cache;
        return new ArrayList<>(shopData.categories);
    }

    public static void updateJsonFile() {
        ShopData shopData = loadShop();
        saveShopData(shopData);
    }
}
