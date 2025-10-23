package ch.swaford.servermanager;

import ch.swaford.servermanager.networktransfer.ServerClientData;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PlayerDataBase {
    private static final String FILE_PATH = "players.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<PlayerData>>(){}.getType();
    private static List<PlayerData> cache;

    public static List<PlayerData> loadPlayers() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            List<PlayerData> playerDataList = gson.fromJson(reader, LIST_TYPE);
            if (playerDataList != null) {
                return playerDataList;
            }
            else {
                return new ArrayList<>();
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void savePlayers(List<PlayerData> playerDataList) {
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(playerDataList, writer);
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
        cache = new ArrayList<>(playerDataList);
    }

    public static void addPlayer(PlayerData data) {
        PlayerData playerData = new PlayerData(data.uuid, data.name);
        List<PlayerData> playerDataList = loadPlayers();
        playerDataList.add(playerData);
        savePlayers(playerDataList);
    }

    public static boolean playerExists(String uuid) {
        for (PlayerData playerData : cache) {
            if (playerData.uuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public static PlayerData getPlayerData(String uuid) {
        for (PlayerData playerData : cache) {
            if (playerData.uuid.equals(uuid)) {
                return playerData;
            }
        }
        return null;
    }

    public static void setPlayerFaction(String uuid, String factionName) {
        List<PlayerData> playerDataList = loadPlayers();
        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                playerData.faction = factionName;
                savePlayers(playerDataList);
                break;
            }
        }
    }

    public static boolean playerHasFaction(String uuid) {
        List<PlayerData> playerDataList = cache;

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                return !playerData.faction.equals("server");
            }
        }
        return false;
    }

    public static boolean getVote(String playerUuid) {
        List<PlayerData> playerDataList = cache;
        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(playerUuid)) {
                return playerData.voteStatus;
            }
        }
        return true;
    }

    public static void setVoteStatus(boolean value, String playerUuid) {
        List<PlayerData> playerDataList = cache;

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(playerUuid)) {
                playerData.voteStatus = value;
                savePlayers(playerDataList);
                return;
            }
        }
    }

    public static void resetVoteStatus() {
        List<PlayerData> playerDataList = cache;

        for (PlayerData playerData : playerDataList) {
            playerData.voteStatus = false;
        }
        savePlayers(playerDataList);
    }

    public static String getPlayerFaction(String uuid) {
        List<PlayerData> playerDataList = cache;
        String playerFaction = "";

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                playerFaction = playerData.faction;
                break;
            }
        }
        return playerFaction;
    }

    public static void removePlayerFaction(String uuid) {
        List<PlayerData> playerDataList = loadPlayers();
        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                playerData.faction = "server";
                savePlayers(playerDataList);
                break;
            }
        }
    }

    public static String getPlayerNameByUuid(String uuid) {
        List<PlayerData> playerDataList = cache;
        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                return playerData.name;
            }
        }
        return null;
    }

    public static String getPlayerUuidByName(String playerName) {
        List<PlayerData> playerDataList = cache;
        for (PlayerData playerData : playerDataList) {
            if (playerData.name.equals(playerName)) {
                return playerData.uuid;
            }
        }
        return null;
    }

    public static ServerClientData getServerClientData(String uuid) {
        List<PlayerData> playerDataList = cache;

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                return new ServerClientData(playerData.uuid, playerData.name, playerData.faction, playerData.balance);
            }
        }
        return new ServerClientData("null", "null", "null", 0);
    }

    public static void updateJsonFile() {
        List<PlayerData> playerDataList = loadPlayers();
        savePlayers(playerDataList);
    }
}

/*
 * Classe utilitaire PlayerDataBase
 *
 * Cette classe gère la persistance des données liées aux joueurs dans le fichier JSON "players.json".
 * Elle utilise Gson pour sérialiser/désérialiser une liste de PlayerData.
 *
 * Rôles principaux :
 * - Charger et sauvegarder la liste des joueurs depuis/vers le fichier JSON.
 * - Ajouter un nouveau joueur avec son UUID et son nom.
 * - Vérifier l’existence d’un joueur (via son UUID).
 * - Récupérer un objet PlayerData complet pour un joueur.
 * - Gérer l’appartenance des joueurs à une faction :
 *      -> Affecter une faction à un joueur.
 *      -> Vérifier si un joueur possède une faction.
 *      -> Obtenir ou supprimer la faction d’un joueur.
 * - Récupérer le nom d’un joueur à partir de son UUID.
 * - Maintenir l’intégrité du fichier JSON via updateJsonFile().
 *
 * Notes :
 * - Les méthodes utilisent toujours loadPlayers() et savePlayers() afin de garantir
 *   la lecture/écriture cohérente des données.
 * - La faction par défaut est "server", ce qui indique qu’un joueur n’appartient à aucune faction.
 * - Toutes les opérations sont faites en mémoire (liste), puis sauvegardées dans le fichier.
 */

