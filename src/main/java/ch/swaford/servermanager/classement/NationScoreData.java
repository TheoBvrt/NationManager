package ch.swaford.servermanager.classement;

public class NationScoreData {
    public String nationName;
    public int score;
    public int rank;

    public NationScoreData(String nationName) {
        this.nationName = nationName;
        this.score = 0;
        this.rank = 0;
    }
}
