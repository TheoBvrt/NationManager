package ch.swaford.servermanager.networktransfer;

public record ServerClientData(
        String playerUuid,
        String playerName,
        String playerFaction,
        int balance
) {}
