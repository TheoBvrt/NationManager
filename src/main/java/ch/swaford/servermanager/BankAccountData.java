package ch.swaford.servermanager;

import java.util.ArrayList;
import java.util.List;

public class BankAccountData {
    String name;
    int balance;
    String ownerUuid;
    List<String> playersUuid = new ArrayList<>();

    public BankAccountData(String name, String ownerUuid) {
        this.name = name;
        this.ownerUuid = ownerUuid;
    }
}
