package ch.swaford.servermanager;

public class ServerData {
    String lastUpdate;
    ServerSessionData serverSessionData;
    ServerEconomyData serverEconomyData;

    public ServerData() {
        this.serverSessionData = new ServerSessionData();
        this.serverEconomyData = new ServerEconomyData();
    }
}

