package ch.swaford.servermanager;

public class ClaimData {
    public String factionName;
    public int x;
    public int z;
    public int color;

    public ClaimData(String factionName, int x, int z, int color) {
        this.factionName = factionName;
        this.x = x;
        this.z = z;
        this.color = color;
    }
}
