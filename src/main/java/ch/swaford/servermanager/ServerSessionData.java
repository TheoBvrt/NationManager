package ch.swaford.servermanager;

import java.util.ArrayList;
import java.util.List;

public class ServerSessionData {
    List<String> playersList;
    int totalPlayerOfSession = 0;
    public ServerSessionData() {
        this.playersList = new ArrayList<>();
    }
}
