package ch.swaford.servermanager.classement;

import ch.swaford.servermanager.FactionData;
import ch.swaford.servermanager.FactionManager;
import ch.swaford.servermanager.classement.ClassementData;
import ch.swaford.servermanager.classement.NationScoreData;
import ch.swaford.servermanager.networktransfer.ServerClassementData;
import ch.swaford.servermanager.networktransfer.ServerNationScoreData;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/*
 * Classe utilitaire ClaimManager
 *
 * Cette classe gère la persistance et la manipulation des données de territoires
 * (claims) enregistrées dans le fichier JSON "claims.json".
 * Elle utilise Gson pour sérialiser/désérialiser une liste de ClaimData.
 *
 * Rôles principaux :
 * - Charger et sauvegarder les données de claims.
 * - Vérifier si un chunk est déjà revendiqué.
 * - Récupérer ou modifier le propriétaire d’un chunk.
 * - Vérifier si un chunk possède des voisins appartenant à la même faction
 *   (utile pour la règle des claims adjacents).
 * - Créer ou supprimer un claim spécifique.
 * - Supprimer tous les claims d’une faction (lors de sa suppression).
 * - Forcer la réécriture du fichier JSON avec updateJsonFile().
 *
 * Notes :
 * - Chaque méthode charge toujours les données (loadClaimData),
 *   modifie la liste en mémoire, puis sauvegarde (saveClaimData).
 * - Le propriétaire par défaut d’un chunk non revendiqué est "server".
 * - La logique de voisinage se limite aux 4 directions cardinales (N, S, E, O).
 */


public class ClassementManager {
    private static final String FILE_PATH = "classement.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<ClassementData>>(){}.getType();
    private static List<ClassementData> cache =  new ArrayList<>();

    public static List<ClassementData> loadClassementData() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            List<ClassementData> classementDataList = gson.fromJson(reader, LIST_TYPE);
            if (classementDataList != null) {
                return classementDataList;
            }
            else {
                return new ArrayList<>();
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveClassementData(List<ClassementData> classementDataList) {
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(classementDataList, writer);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            // Remplace le fichier original par le temporaire de façon atomique
            Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cache = new  ArrayList<>(classementDataList);
    }

    //met à jours les nations et leur rank dans chaque classement
    public static void updateNationList() {
        List<ClassementData> classementDataList = loadClassementData();
        List<String> nationList = FactionManager.getFactionNames();

        //Ajoute ou retire une nation en fonction du json nation.
        for (ClassementData classementData : classementDataList) {
            classementData.nationScoreDataList.removeIf(nationScoreData ->
                    !nationList.contains(nationScoreData.nationName));
            for (String nationName : nationList) {
                boolean found = classementData.nationScoreDataList.stream()
                        .anyMatch(nationScoreData -> nationScoreData.nationName.equals(nationName));
                if (!found) {
                    classementData.nationScoreDataList.add(new NationScoreData(nationName));
                }
            }
        }

        //met à jours les rangs en fonction des score de chaque classement
        for (ClassementData classementData : classementDataList) {
            Map<String, Integer> ranking = new HashMap<>();
            for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                ranking.put(nationScoreData.nationName, nationScoreData.score);
            }

            List<Map.Entry<String, Integer>> entries = new ArrayList<>(ranking.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            int rank = entries.size();
            for (Map.Entry<String, Integer> entry : entries) {
                for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                    if (nationScoreData.nationName.equals(entry.getKey())) {
                        nationScoreData.rank = rank;
                    }
                }
                rank --;
            }
        }

        //Recalcule le bonus power de chaque nation
        for (String nationName : nationList) {
            int newPower = 0;
            for (ClassementData classementData : classementDataList) {
                if (!classementData.classementName.equals("nation_puissance") && classementData.enable) {
                    for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                        if (nationScoreData.nationName.equals(nationName)) {
                            int rank = nationScoreData.rank;
                            if (rank == 1)
                                newPower += 100;
                            else if (rank == 2)
                                newPower += 40;
                            else if (rank == 3)
                                newPower += 15;
                        }
                    }
                }
            }
            FactionManager.updateBonusPower(nationName, newPower);
        }

        saveClassementData(classementDataList);
    }

    //incrémente le score d'une nation sur un certain classement
    public static void updateNationScore(String classementName, String nationName, int score) {
        List<ClassementData> classementDataList = loadClassementData();

        for (ClassementData classementData : classementDataList) {
            if (classementData.classementName.equals(classementName)) {
                for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                    if (nationScoreData.nationName.equals(nationName)) {
                        nationScoreData.score += score;
                    }
                }
            }
        }
        saveClassementData(classementDataList);
        ClassementManager.updateNationList();
    }

    public static void addVote(String factionName) {
        List<ClassementData> classementDataList = loadClassementData();

        for (ClassementData classementData : classementDataList) {
            if (classementData.classementName.equals("nation_diplomatie")) {
                for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                    if (nationScoreData.nationName.equals(factionName)) {
                        nationScoreData.score += 1;
                        saveClassementData(classementDataList);
                        updateNationList();
                        return;
                    }
                }
            }
        }
    }

    public static void setFactionMoneyScore (String factionName) {
        List<ClassementData> classementDataList = loadClassementData();

        for (ClassementData classementData : classementDataList) {
            if (classementData.classementName.equals("nation_balance_score")) {
                if (!ClassementManager.getStatusOfClassement("nation_balance_score")) return;
                for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                    if (nationScoreData.nationName.equals(factionName)) {
                        nationScoreData.score = FactionManager.getBalance(factionName);
                        saveClassementData(classementDataList);
                        updateNationList();
                        return;
                    }
                }
            }
        }
    }

    public static void updateNationScore () {
        List<String> factionList = FactionManager.getFactionNames();
        List<ClassementData> classementDataList = loadClassementData();

        for (String factionName : factionList) {
            int puissance = FactionManager.getPower(factionName);
            int balance = FactionManager.getBalance(factionName);
            int totalChunk = FactionManager.getTotalChunks(factionName);
            int memberCount = FactionManager.getMemberCount(factionName);

            for (ClassementData classementData : classementDataList) {
                if (classementData.classementName.equals("nation_puissance") && classementData.enable) {
                    NationScoreData match = classementData.nationScoreDataList.stream().filter(
                            n -> n.nationName.equals(factionName)).findFirst().orElse(null);
                    assert match != null;
                    match.score = puissance;
                }

                if (classementData.classementName.equals("nation_balance_score") && classementData.enable) {
                    NationScoreData match = classementData.nationScoreDataList.stream().filter(
                            n -> n.nationName.equals(factionName)).findFirst().orElse(null);
                    assert match != null;
                    match.score = balance;
                }

                if (classementData.classementName.equals("nation_claim") && classementData.enable) {
                    NationScoreData match = classementData.nationScoreDataList.stream().filter(
                            n -> n.nationName.equals(factionName)).findFirst().orElse(null);
                    assert match != null;
                    match.score = totalChunk;
                }

                if (classementData.classementName.equals("nation_member") && classementData.enable) {
                    NationScoreData match = classementData.nationScoreDataList.stream().filter(
                            n -> n.nationName.equals(factionName)).findFirst().orElse(null);
                    assert match != null;
                    match.score = memberCount;
                }
            }
        }
        saveClassementData(classementDataList);
    }

    public static void setFactionPuissanceScore (String factionName, int puissance) {
        List<ClassementData> classementDataList = loadClassementData();

        for (ClassementData classementData : classementDataList) {
            if (classementData.classementName.equals("nation_puissance")) {
                if (!ClassementManager.getStatusOfClassement("nation_puissance")) return;
                for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                    if (nationScoreData.nationName.equals(factionName)) {
                        nationScoreData.score = puissance;
                        saveClassementData(classementDataList);
                        return;
                    }
                }
            }
        }
    }

    public static void applyScoreReductionRate () {
        List<ClassementData> classementDataList = loadClassementData();
        for (ClassementData classementData : classementDataList) {
            int scoreReductionRate = classementData.scoreReductionRate;
            if (scoreReductionRate > 0 && classementData.enable && classementData.isPeriodicUpdate) {
                for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                    nationScoreData.score = (nationScoreData.score * (100 - scoreReductionRate) / 100);
                }
            }
        }
        saveClassementData(classementDataList);
        updateNationList();
    }

    public static boolean getStatusOfClassement(String classementName) {
        List<ClassementData> classementDataList = cache;

        for (ClassementData classementData : classementDataList) {
            if (classementData.classementName.equals(classementName)) {
                return classementData.enable;
            }
        }
        return false;
    }

    public static List<ServerClassementData> getClassementDataList() {
        List<ClassementData> classementDataList = cache;
        List<ServerClassementData> serverClassementDataList = new ArrayList<>();

        for (ClassementData classementData : classementDataList) {
            String classementName = classementData.classementName;
            String classementDescription = classementData.description;
            boolean enable = classementData.enable;
            boolean isPeriodicUpdate = classementData.isPeriodicUpdate;
            int scoreReductionRate = classementData.scoreReductionRate;

            List<ServerNationScoreData> serverNationScoreDataList = new ArrayList<>();
            for (NationScoreData nationScoreData : classementData.nationScoreDataList) {
                String nationName = nationScoreData.nationName;
                int score = nationScoreData.score;
                int rank = nationScoreData.rank;
                int memberCount = FactionManager.getMemberCount(nationName);
                serverNationScoreDataList.add(new ServerNationScoreData(nationName, score, rank, memberCount));
            }
            serverClassementDataList.add(new ServerClassementData(
                    classementName,
                    classementDescription,
                    enable,
                    isPeriodicUpdate,
                    scoreReductionRate,
                    serverNationScoreDataList
            ));
        }
        return serverClassementDataList;
    }

    public static void createClassementData(ClassementData classementData) {
        List<ClassementData> classementDataList = ClassementManager.loadClassementData();
        classementDataList.add(classementData);
        saveClassementData(classementDataList);
    }

    public static void updateJsonFile() {
        List<ClassementData> classementDataList = loadClassementData();
        saveClassementData(classementDataList);
    }
}
