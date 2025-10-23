package ch.swaford.servermanager;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class FactionData {
    String name;
    String ownerUuid;
    String description;
    String bannerTag;
    int color;
    int balance;
    int bonusPower;
    int power;
    int totalChunk;
    int rank;
    List<String> officers;
    List<String> members;
    public FactionData(String name, String ownerUuid, int color) {
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.balance = 0;
        this.power = 0;
        this.bonusPower = 0;
        this.officers = new ArrayList<>();
        this.members = new ArrayList<>();
        this.totalChunk = 0;
        this.rank = 0;
        this.description = "Changer votre description avec /nation description";
        this.bannerTag = "";
        this.color = color;
    }
}
