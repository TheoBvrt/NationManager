package ch.swaford.servermanager.networktransfer;

import ch.swaford.servermanager.ClaimData;

import java.util.List;

public class ClientCache {
    private static ServerClientData serverClientData;
    private static ServerFactionData serverFactionData;
    private static List<ClaimData> claimDataList;
    private static List<ServerShopItemData> serverShopItemDataList;
    private static List<ServerClassementData> serverClassementDataList;

    public static void setServerClientData(ServerClientData newServerClientData) {
        serverClientData = newServerClientData;
    }

    public static ServerClientData getServerClientData() {
        return serverClientData;
    }

    public static void setServerFactionData(ServerFactionData newServerFactionData) {
        serverFactionData = newServerFactionData;
    }

    public static ServerFactionData getServerFactionData() {
        return serverFactionData;
    }

    public static void  setClaimDataList(List<ClaimData> newClaimDataList) {
        claimDataList = newClaimDataList;
    }

    public static List<ClaimData> getClaimDataList() {
        return claimDataList;
    }

    public static void setServerShopItemDataList(List<ServerShopItemData> newServerShopItemDataList) {
        serverShopItemDataList = newServerShopItemDataList;
    }

    public  static List<ServerShopItemData> getServerShopItemDataList() {
        return serverShopItemDataList;
    }

    public static void setServerClassementDataList(List<ServerClassementData> newServerClassementDataList) {
        serverClassementDataList = newServerClassementDataList;
    }

    public static List<ServerClassementData> getServerClassementDataList() {
        return serverClassementDataList;
    }
}
