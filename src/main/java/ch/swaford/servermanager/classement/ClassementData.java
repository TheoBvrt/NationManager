package ch.swaford.servermanager.classement;

import java.util.ArrayList;
import java.util.List;

public class ClassementData {
    public String classementName;
    public String description;
    public boolean enable = false;
    public boolean isPeriodicUpdate = false;
    public int scoreReductionRate;
    public List<NationScoreData> nationScoreDataList = new ArrayList<>();

    public ClassementData() {
        this.classementName = "default";
        this.description = "default";
        this.enable = false;
        this.isPeriodicUpdate = false;
        this.scoreReductionRate = 0;
        this.scoreReductionRate = 0;
    }
}
