package ch.swaford.servermanager;

import oshi.util.tuples.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Classe utilitaire EconomyManager
 *
 * Cette classe gère toute la logique économique liée aux joueurs :
 * - Ajout, retrait et définition directe du solde d’un joueur.
 * - Récupération du solde d’un joueur.
 * - Génération d’un classement global des soldes des joueurs (baltop).
 *
 * Rôles principaux :
 * - Manipuler la variable balance de chaque PlayerData en fonction de l’UUID.
 * - Sauvegarder automatiquement les modifications dans le fichier "players.json"
 *   via PlayerDataBase.
 * - Fournir une vue d’ensemble des soldes sous forme de Map (nom → balance).
 *
 * Notes :
 * - Chaque opération parcourt la liste des joueurs en mémoire, puis appelle
 *   PlayerDataBase.savePlayers() pour persister les changements.
 * - La méthode baltop() retourne une map non triée : l’affichage devra être
 *   ordonné (par solde) côté appelant si nécessaire.
 * - La méthode getPlayerBalance() suppose que le joueur existe (assert non nul).
 */

public class EconomyManager {
    public static void addMoney(String uuid, int amount) {
        List<PlayerData> playerDataList = PlayerDataBase.loadPlayers();

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                playerData.balance += amount;
            }
        }
        PlayerDataBase.savePlayers(playerDataList);
    }

    public static void subMoney(String uuid, int amount) {
        List<PlayerData> playerDataList = PlayerDataBase.loadPlayers();

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                playerData.balance -= amount;
            }
        }
        PlayerDataBase.savePlayers(playerDataList);
    }

    public static void setMoney(String uuid, int amount) {
        List<PlayerData> playerDataList = PlayerDataBase.loadPlayers();

        for (PlayerData playerData : playerDataList) {
            if (playerData.uuid.equals(uuid)) {
                playerData.balance = amount;
            }
        }
        PlayerDataBase.savePlayers(playerDataList);
    }

    public static Map<String, Integer> baltop() {
        List<PlayerData> playerDataList = PlayerDataBase.loadPlayers();
        Map<String, Integer> map = new HashMap<>();
        for (PlayerData playerData : playerDataList) {
            map.put(playerData.name, playerData.balance);
        }
        return map;
    }

    public static int getPlayerBalance(String uuid) {
        PlayerData playerdata = PlayerDataBase.getPlayerData(uuid);
        assert playerdata != null;
        return playerdata.balance;
    }
}
