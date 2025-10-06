package ch.swaford.servermanager.networktransfer;

import ch.swaford.servermanager.classement.NationScoreData;

import java.util.List;

public record ServerClassementData (
        String classementName,
        String description,
        boolean enable,
        boolean isPeriodicUpdate,
        int scoreReductionRate,
        List<ServerNationScoreData> serverNationScoreData
) {}
