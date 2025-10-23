package ch.swaford.servermanager;

public class PlayerData {
    String uuid;
    String name;
    int balance;
    String faction;
    boolean voteStatus = false;

    PlayerData(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.balance = 0;
        this.faction = "server";
    }
}
