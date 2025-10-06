package ch.swaford.servermanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ServerDataManager {
    private static final String FILE_PATH = "server_data.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static ServerData loadServerData() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            ServerData serverData = gson.fromJson(reader, ServerData.class);
            if (serverData != null) {
                return serverData;
            }
            else {
                return new ServerData();
            }
        } catch (IOException e) {
            return new ServerData();
        }
    }

    public static void saveServerData(ServerData serverData) {
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(serverData, writer);
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
    }

    public static boolean isPlayerConnectedToday(String playerUuid) {
        ServerData serverData = loadServerData();
        return serverData.serverSessionData.playersList.contains(playerUuid);
    }

    public static void addPlayerConnectedToday(String playerUuid) {
        ServerData serverData = loadServerData();
        serverData.serverSessionData.playersList.add(playerUuid);
        serverData.serverSessionData.totalPlayerOfSession += 1;
        saveServerData(serverData);
    }

    public static void clearSession() {
        ServerData serverData = loadServerData();
        serverData.serverSessionData.totalPlayerOfSession = 0;
        serverData.serverSessionData.playersList.clear();
        saveServerData(serverData);
    }

    public static int getNumberOfPlayersConnected() {
        ServerData serverData = loadServerData();
        return serverData.serverSessionData.playersList.size();
    }

    public static void addBankBalance(int amount) {
        ServerData serverData = loadServerData();
        serverData.serverEconomyData.currentBankBalance += amount;
        saveServerData(serverData);
    }

    public static void updatePrime() {
        ServerData serverData = loadServerData();
        serverData.serverEconomyData.currentPrime = Math.max(0, serverData.serverEconomyData.currentBankBalance / Math.max(1, getNumberOfPlayersConnected()));
        clearSession();
        saveServerData(serverData);

        //bankbalance -= prime playerBalance += prime + 100
    }

    public static void setNextUpdate(String lastUpdate) {
        ServerData serverData = loadServerData();
        serverData.lastUpdate = lastUpdate;
        saveServerData(serverData);
    }

    public static String getLastUpdate() {
        ServerData serverData = loadServerData();
        return serverData.lastUpdate;
    }

    public static void giveSalary(MinecraftServer server) {
        ServerData serverData = loadServerData();
        int prime = Math.max(0, serverData.serverEconomyData.currentBankBalance / Math.max(1, getNumberOfPlayersConnected()));

        for (String playerUuid : serverData.serverSessionData.playersList) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(UUID.fromString(playerUuid));
            if (serverPlayer != null) {
                serverPlayer.sendSystemMessage(Component.literal("§lServeur : vous avez reçu votre salaire : §e" + (prime + ServerVariable.baseSalary) + "€ "));
            }
            serverData.serverEconomyData.currentBankBalance -= prime;
            EconomyManager.addMoney(playerUuid, prime + ServerVariable.baseSalary);
        }
        saveServerData(serverData);
    }

    public static void updateJsonFile() {
        ServerData serverData = loadServerData();
        saveServerData(serverData);
    }
}

/*
 * Classe utilitaire ServerDataManager
 *
 * Cette classe gère la persistance et la manipulation des données globales du serveur
 * dans le fichier JSON "server_data.json".
 *
 * Rôles principaux :
 * - Charger et sauvegarder les données du serveur via Gson.
 * - Suivre les connexions journalières des joueurs :
 *      -> Vérifie si un joueur s’est connecté aujourd’hui.
 *      -> Ajoute un joueur à la liste des connectés du jour.
 *      -> Réinitialise la session quotidienne (clearSession).
 *      -> Compte le nombre total de joueurs connectés sur la session.
 * - Gérer l’économie serveur :
 *      -> Ajouter un montant au solde global de la banque.
 *      -> Calculer et mettre à jour la prime journalière selon le solde de la banque
 *         et le nombre de joueurs connectés.
 *      -> Distribuer un salaire aux joueurs connectés (prime + 100),
 *         en retirant les fonds de la banque et en créditant le joueur.
 * - Maintenir l’intégrité du fichier JSON en l’actualisant via updateJsonFile().
 *
 * Notes :
 * - Les méthodes utilisent toujours loadServerData() et saveServerData() afin de garantir
 *   la lecture/écriture cohérente des données.
 * - La prime est calculée comme suit :
 *      prime = bankBalance / max(1, nombre de joueurs connectés)
 *   (protection contre la division par zéro).
 * - Lors du versement du salaire, chaque joueur reçoit (prime + 100),
 *   et la banque est débitée de "prime".
 */
