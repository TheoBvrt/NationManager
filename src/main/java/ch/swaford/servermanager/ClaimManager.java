package ch.swaford.servermanager;

import ch.swaford.servermanager.networktransfer.ServerClaimDataPayload;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

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


public class ClaimManager {
    private static final String FILE_PATH = "claims.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<ClaimData>>(){}.getType();
    private static List<ClaimData> cache;

    public static List<ClaimData> loadClaimData() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            List<ClaimData> claimDataList = gson.fromJson(reader, LIST_TYPE);
            if (claimDataList != null) {
                return claimDataList;
            }
            else {
                return new ArrayList<>();
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveClaimData(List<ClaimData> claimDataList) {
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(claimDataList, writer);
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
        cache = new ArrayList<>(claimDataList);
    }

    public static boolean checkIfChunkIsClaimed(int x, int z) {
        List<ClaimData> claimDataList = cache;

        for (ClaimData claimData : claimDataList) {
            if (claimData.x == x && claimData.z == z) {
                return true;
            }
        }
        return false;
    }

    public static String getClaimOwner(int x, int z) {
        List<ClaimData> claimDataList = cache;

        for (ClaimData claimData : claimDataList) {
            if (claimData.x == x && claimData.z == z) {
                return claimData.factionName;
            }
        }
        return "server";
    }

    public static void changeClaimOwner(int x, int z, String factionName, int color) {
        List<ClaimData> claimDataList = ClaimManager.loadClaimData();

        for (ClaimData claimData : claimDataList) {
            if (claimData.x == x && claimData.z == z) {
                claimData.factionName = factionName;
                claimData.color = color;
                saveClaimData(claimDataList);
                break;
            }
        }
        ServerClaimDataPayload payload = new ServerClaimDataPayload(ClaimManager.getClaimList());
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static boolean claimHaveNeighbors(int x, int z, String factionName) {
        List<ClaimData> claimDataList = cache;

        for (ClaimData claimData : claimDataList) {
            String currentFaction = claimData.factionName;
            if (claimData.x == x - 1 && claimData.z == z && currentFaction.equals(factionName)) {
                return true;
            }
            if (claimData.x == x + 1 && claimData.z == z && currentFaction.equals(factionName)) {
                return true;
            }
            if (claimData.x == x && claimData.z == z - 1 && currentFaction.equals(factionName)) {
                return true;
            }
            if (claimData.x == x && claimData.z == z + 1 && currentFaction.equals(factionName)) {
                return true;
            }
        }
        return false;
    }

    public static List<ClaimData> getClaimList() {
        return cache;
    }

    public static void deleteClaim(int x, int z) {
        List<ClaimData> claimDataList = ClaimManager.loadClaimData();
        claimDataList.removeIf(claimData -> claimData.x == x && claimData.z == z);
        saveClaimData(claimDataList);
        ServerClaimDataPayload payload = new ServerClaimDataPayload(claimDataList);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void createClaim(ClaimData claimData) {
        List<ClaimData> claimDataList = loadClaimData();
        claimDataList.add(claimData);
        saveClaimData(claimDataList);
        ServerClaimDataPayload payload = new ServerClaimDataPayload(claimDataList);
        PacketDistributor.sendToAllPlayers(payload);
    }

    public static void updateJsonFile() {
        List<ClaimData> claimDataList = loadClaimData();
        saveClaimData(claimDataList);
    }

    public static void deleteFactionClaims(String factionName) {
        List<ClaimData> claimDataList = ClaimManager.loadClaimData();
        claimDataList.removeIf(claimData -> claimData.factionName.equals(factionName));
        saveClaimData(claimDataList);
    }
}
