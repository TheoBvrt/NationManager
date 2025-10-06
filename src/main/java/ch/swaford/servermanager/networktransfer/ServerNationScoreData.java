package ch.swaford.servermanager.networktransfer;

public record ServerNationScoreData (
        String name,
        int score,
        int rank,
        int memberCount
) {}