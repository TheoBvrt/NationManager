package ch.swaford.servermanager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.WString;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/*
 * Classe utilitaire BankManager
 *
 * Cette classe gère la logique et la persistance des comptes bancaires dans le fichier JSON
 * "bank_accounts.json". Elle fournit des méthodes pour créer, modifier et interagir avec des comptes,
 * tout en appliquant des règles de gestion (limite de comptes, vérification des accès, etc.).
 *
 * Rôles principaux :
 * - Charger et sauvegarder la liste des comptes bancaires via Gson.
 * - Créer ou supprimer un compte bancaire (max. 2 comptes par joueur).
 * - Lister les comptes possédés par un joueur et ceux auxquels il a accès.
 * - Vérifier et modifier le solde d’un compte :
 *      -> Dépôt : transfert d’argent du joueur vers le compte.
 *      -> Retrait : transfert d’argent du compte vers le joueur.
 * - Ajouter ou retirer des membres autorisés à accéder à un compte.
 * - Transférer la propriété d’un compte à un autre joueur.
 * - Vérifier si un joueur possède les droits sur un compte.
 *
 * Codes de retour des méthodes :
 * -  1  : succès de l’opération.
 * -  0  : échec générique (par ex. accès refusé).
 * - -1  : cas d’erreur spécifique (par ex. solde insuffisant, trop de comptes).
 *
 * Notes :
 * - Toutes les données sont chargées en mémoire, modifiées, puis sauvegardées dans le fichier JSON.
 * - Les comptes sont identifiés par leur nom (String), sensible à la casse pour la recherche exacte.
 * - Les soldes joueurs sont gérés via EconomyManager.
 */


public class BankManager {
    private static final String FILE_PATH = "bank_accounts.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<BankAccountData>>(){}.getType();

    public static List<BankAccountData> loadBankAccountData() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            List<BankAccountData> bankAccountDataList = gson.fromJson(reader, LIST_TYPE);
            if (bankAccountDataList != null) {
                return bankAccountDataList;
            }
            else {
                return new ArrayList<>();
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveBankData(List<BankAccountData> bankAccountDataList) {
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(bankAccountDataList, writer);
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

    public static int createBankAccount(BankAccountData newBankAccountData) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();
        String playerUuid = newBankAccountData.ownerUuid;
        int totalAccountOfPlayer = 0;

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.ownerUuid.equals(playerUuid)) {
                totalAccountOfPlayer++;
            }
            if (totalAccountOfPlayer >= 2) {
                return -1;
            }
        }

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equalsIgnoreCase(newBankAccountData.name)) {
                return 0;
            }
        }
        bankAccountDataList.add(newBankAccountData);
        saveBankData(bankAccountDataList);
        return 1;
    }

    public static int deleteBankAccount(String bankAccountToDelete, String playerUuid) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (int i = 0; i < bankAccountDataList.size(); i++) {
            BankAccountData bankAccountData = bankAccountDataList.get(i);

            if (bankAccountData.name.equals(bankAccountToDelete)) {
                if (!bankAccountData.ownerUuid.equals(playerUuid)) {
                    return 0;
                }
                bankAccountDataList.remove(bankAccountData);
                saveBankData(bankAccountDataList);
            }
        }
        return 1;
    }

    public static List<String> listPlayersAccounts(String playerUuid){
        List<BankAccountData> bankAccountDataList = loadBankAccountData();
        List<String> accountList = new ArrayList<>();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if  (bankAccountData.ownerUuid.equals(playerUuid)) {
                accountList.add(bankAccountData.name);
            }
        }
        return accountList;
    }

    public static List<String> listOthersAccounts(String playerUuid){
        List<BankAccountData> bankAccountDataList = loadBankAccountData();
        List<String> accountList = new ArrayList<>();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.playersUuid.contains(playerUuid)) {
                accountList.add(bankAccountData.name);
            }
        }
        return accountList;
    }

    public static int getAccountBalance(String accountName){
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName)) {
                return bankAccountData.balance;
            }
        }
        return -1;
    }

    public static int addAccountMember(String accountName, String sourcePlayerUuid, String targetPlayerUuid) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName) && bankAccountData.ownerUuid.equals(sourcePlayerUuid)) {
                bankAccountData.playersUuid.add(targetPlayerUuid);
                saveBankData(bankAccountDataList);
                return 1;
            }
        }
        return 0;
    }

    public static int removeAccountMember(String accountName, String sourcePlayerUuid, String targetPlayerUuid) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();
        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName) && bankAccountData.ownerUuid.equals(sourcePlayerUuid)) {
                if (bankAccountData.playersUuid.contains(targetPlayerUuid)) {
                    bankAccountData.playersUuid.remove(targetPlayerUuid);
                    saveBankData(bankAccountDataList);
                    return 1;
                }
                else {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static int checkAccountBalance(String accountName, String playerUuid) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();
        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName)) {
                if (bankAccountData.ownerUuid.equals(playerUuid) || bankAccountData.playersUuid.contains(playerUuid)) {
                    return bankAccountData.balance;
                }
                else {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static int deposit(String accountName, String playerUuid, int amount) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName) && checkPlayerAccess(accountName, playerUuid)) {
                if (EconomyManager.getPlayerBalance(playerUuid) >= amount) {
                    EconomyManager.setMoney(playerUuid, EconomyManager.getPlayerBalance(playerUuid) - amount);
                    setAccountBalance(accountName, getAccountBalance(accountName) +  amount);
                    return 1;
                }
                else {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static int withdraw(String accountName, String playerUuid, int amount) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName) && checkPlayerAccess(accountName, playerUuid)) {
                if (getAccountBalance(accountName) >= amount) {
                    EconomyManager.setMoney(playerUuid, EconomyManager.getPlayerBalance(playerUuid) + amount);
                    setAccountBalance(accountName, getAccountBalance(accountName) -  amount);
                    return 1;
                }
                else {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static void setAccountBalance(String accountName, int amount) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName)) {
                bankAccountData.balance = amount;
            }
        }
        saveBankData(bankAccountDataList);
    }

    public static boolean checkPlayerAccess(String accountName, String playerUuid) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (bankAccountData.name.equals(accountName) && (bankAccountData.ownerUuid.equals(playerUuid) || bankAccountData.playersUuid.contains(playerUuid)) ) {
                return true;
            }
        }
        return false;
    }

    public static int setOwner(String accountName, String sourcePlayerUuid, String targetPlayerUuid) {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();

        for (BankAccountData bankAccountData : bankAccountDataList) {
            if (accountName.equals(bankAccountData.name)) {
                if (bankAccountData.ownerUuid.equals(sourcePlayerUuid)) {
                    bankAccountData.ownerUuid = targetPlayerUuid;
                    saveBankData(bankAccountDataList);
                    return 1;
                }
                else {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static void updateJsonFile() {
        List<BankAccountData> bankAccountDataList = loadBankAccountData();
        saveBankData(bankAccountDataList);
    }

}
