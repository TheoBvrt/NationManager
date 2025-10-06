package ch.swaford.servermanager.networktransfer;

import java.util.List;
import java.util.Map;

public record ServerFactionData(
        String name,
        String ownerUuid,
        int balance,
        int bonusPower,
        int power,
        int totalChunk,
        List<String> officers,
        List<String> members,
        int rank,
        String description,
        String bannerTag,
        Map<String, String> playerList
) {}
