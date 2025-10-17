package ch.swaford.servermanager;

import ch.swaford.servermanager.classement.ClassementManager;
import ch.swaford.servermanager.classement.ClassementData;
import ch.swaford.servermanager.clientinterface.ColorCode;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class FactionManager {
    private static final String FILE_PATH = "factions.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<FactionData>>(){}.getType();
    private static List<FactionData> cache = new ArrayList<>();
    public static List<String[]> invitations = new ArrayList<>();

    public static List<FactionData> loadFactionData() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            List<FactionData> factionDataList = gson.fromJson(reader, LIST_TYPE);
            if (factionDataList != null) {
                return factionDataList;
            }
            else {
                return new ArrayList<>();
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveFactionData(List<FactionData> factionDataList) {
//        try (Writer writer = new FileWriter(FILE_PATH)){
//            gson.toJson(factionDataList, writer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Path path = Paths.get(FILE_PATH);
        Path tmpPath = Paths.get(FILE_PATH + ".tmp");

        try (Writer writer = new FileWriter(tmpPath.toFile())) {
            gson.toJson(factionDataList, writer);
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
        cache = new ArrayList<>(factionDataList);
    }

    public static int getClaimPrice(String factionName) {
        int nbClaim = FactionManager.getTotalChunks(factionName);
        int targetPriceFor30 = ServerVariable.targetPriceFor30;
        double pente = (double) (targetPriceFor30 - ServerVariable.claimBasePrice) / 30;
        return (int) (ServerVariable.claimBasePrice + pente * nbClaim);
    }

    public static void createNewFaction(FactionData factionData) {
        List<FactionData> factionDataList = loadFactionData();
        factionDataList.add(factionData);
        saveFactionData(factionDataList);
        updatePower(factionData.name);
        ClassementManager.updateNationList();
    }

    public static void setVoteStatus(boolean value, String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.voted = value;
                saveFactionData(factionDataList);
                return;
            }
        }
    }

    public static void resetVoteStatus() {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            factionData.voted = false;
        }
        saveFactionData(factionDataList);
    }

    public static boolean getVote(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.voted;
            }
        }
        return true;
    }

    public static void deleteFaction(String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (int i = 0; i < factionDataList.size(); i++) {
            FactionData factionData = factionDataList.get(i);
            if (factionData.name.equals(factionName)) {
                for (String memberUuid : factionData.members) {
                    PlayerDataBase.removePlayerFaction(memberUuid);
                }
                for (String officersUuid : factionData.officers) {
                    PlayerDataBase.removePlayerFaction(officersUuid);
                }
                ClaimManager.deleteFactionClaims(factionName);
                PlayerDataBase.removePlayerFaction(factionData.ownerUuid);
                factionDataList.remove(factionData);
                saveFactionData(factionDataList);
                break ;
            }
        }
        ClassementManager.updateNationList();
    }

    public static boolean factionExist(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equalsIgnoreCase(factionName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean playerIsOwner(String playerUuid, String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName) && factionData.ownerUuid.equals(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    public static boolean playerIsOpInFaction(String playerUuid, String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName) && (factionData.ownerUuid.equals(playerUuid) || factionData.officers.contains(playerUuid))) {
                return true;
            }
        }
        return false;
    }

    public static boolean playerIsMember(String playerUuid, String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName) && (factionData.members.contains(playerUuid))) {
                return true;
            }
        }
        return false;
    }

    public static boolean playerIsOfficer(String playerUuid, String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName) && factionData.officers.contains(playerUuid)) {
                return true;
            }
        }
        return false;
    }


    public static boolean playerIsInFaction(String playerUuid, String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                if (factionData.members.contains(playerUuid) || factionData.officers.contains(playerUuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void addPlayerToFaction(String playerUuid, String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.members.add(playerUuid);
                saveFactionData(factionDataList);
                return;
            }
        }
    }

    public static void invitePlayerToFaction(String playerUuid, String factionName) {
        invitations.add(new String[] {factionName, playerUuid});
    }

    public static void deleteInvitation(String playerUuid, String factionName) {
        invitations.removeIf(inv ->
                inv[0].equals(factionName) && inv[1].equals(playerUuid)
        );
    }

    public static boolean playerIsInvited(String playerUuid, String factionName) {
        return invitations.stream().anyMatch(inv -> inv[0].equals(factionName) && inv[1].equals(playerUuid));
    }

    public static void removePlayer(String playerUuid, String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.officers.remove(playerUuid);
                factionData.members.remove(playerUuid);
                saveFactionData(factionDataList);
                break;
            }
        }
    }

    public static void promotePlayer(String playerUuid, String factionName) {
        List<FactionData> factionDataList = loadFactionData();
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.members.remove(playerUuid);
                factionData.officers.add(playerUuid);
                saveFactionData(factionDataList);
                break;
            }
        }
    }

    public static void demotePlayer(String playerUuid, String factionName) {
        List<FactionData> factionDataList = loadFactionData();
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.officers.remove(playerUuid);
                factionData.members.add(playerUuid);
                saveFactionData(factionDataList);
                break;
            }
        }
    }

    public static void setFactionOwner(String playerUuid, String factionName) {
        List<FactionData> factionDataList = loadFactionData();
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                String currentOwnerUuid = factionData.ownerUuid;
                factionData.ownerUuid = playerUuid;
                factionData.officers.add(currentOwnerUuid);
                factionData.members.remove(playerUuid);
                factionData.officers.remove(playerUuid);
                saveFactionData(factionDataList);
                break;
            }
        }
    }

    public static void setFactionDesription(String description, String factionName) {
        List<FactionData> factionDataList = loadFactionData();
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.description = description;
                saveFactionData(factionDataList);
                return;
            }
        }
    }

    public static void setBannerTag(String tag, String factionName) {
        List<FactionData> factionDataList = loadFactionData();
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.bannerTag = tag;
                saveFactionData(factionDataList);
                return;
            }
        }
    }

    public static String getBannerTag(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.bannerTag;
            }
        }
        return null;
    }

    public static List<String> getFactionOfficers(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.officers;
            }
        }
        return null;
    }

    public static int getFactionRank(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.rank;
            }
        }
        return 0;
    }

    public static String getFactionDescription(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.description;
            }
        }
        return null;
    }

    public static List<String> getFactionMembers(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.members;
            }
        }
        return null;
    }

    public static Map<String, String> getPlayerList(String factionName) {
        Map<String, String> playerList = new HashMap<>();
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                String ownerName = PlayerDataBase.getPlayerNameByUuid(factionData.ownerUuid);
                playerList.put(factionData.ownerUuid, ownerName);
                for (String officer : factionData.officers) {
                    String name = PlayerDataBase.getPlayerNameByUuid(officer);
                    if (name != null) {
                        playerList.put(officer, name);
                    }
                }
                for (String member : factionData.members) {
                    String name = PlayerDataBase.getPlayerNameByUuid(member);
                    if (name != null) {
                        playerList.put(member, name);
                    }
                }
                break;
            }
        }
        return playerList;
    }

    public static int getMemberCount(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return (factionData.members.size() + factionData.officers.size() + 1);
            }
        }
        return 0;
    }

    public static String getFactionOwner(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.ownerUuid;
            }
        }
        return null;
    }

    public static int getBonusPower(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.bonusPower;
            }
        }
        return 0;
    }

    public static String getFactionInfos(String factionName) {
        List<FactionData> factionDataList = cache;
        StringBuilder stringBuilder = new StringBuilder();
        FactionData currentFaction = null;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                currentFaction = factionData;
                break;
            }
        }
        if (currentFaction != null) {
            String message = "§a§lNom : §r§b" + currentFaction.name + "\n§r§a§lPropriétaire : §r§b" +
                    PlayerDataBase.getPlayerNameByUuid(currentFaction.ownerUuid) + "\n§r§a§lPuissance : §r§b"
                    + currentFaction.power + "\n§r§a§lBonus : §l§b" + currentFaction.bonusPower + "\n§r§a§lTerritoires : §l§b" + currentFaction.totalChunk + "\n§r§a§lArgent : §l§b" + currentFaction.balance + " euros\n";
            stringBuilder.append(message).append("§f§l----------------------------\n\n§r§fOfficiers : ");
            for (String officer : currentFaction.officers) {
                stringBuilder.append(PlayerDataBase.getPlayerNameByUuid(officer)).append(" ");
            }
            stringBuilder.append("\n\nMembres : ");
            for (String member : currentFaction.members) {
                stringBuilder.append(PlayerDataBase.getPlayerNameByUuid(member)).append(" ");
            }
            return (stringBuilder.toString());
        }
        return null;
    }

    public static String getFactionList() {
        StringBuilder stringBuilder = new StringBuilder();
        List<FactionData> factionDataList = cache;

        stringBuilder.append(" ".repeat(200));
        stringBuilder.append("\n§a§lListe des nations : \n").append("----------------------------\n\n§r§f");
        int index = 0;
        for (FactionData factionData : factionDataList) {
            if (index >= 10) {
                break;
            }
            stringBuilder.append("- ").append(factionData.name).append("\n");
            index++;
        }
        return (stringBuilder.toString());
    }

    public static List<String> getFactionNames() {
        List<FactionData> factionDataList = cache;
        List<String> factionNames = new ArrayList<>();

        for (FactionData factionData : factionDataList) {
            factionNames.add(factionData.name);
        }
        return factionNames;
    }

    public static void addMoney(String factionName, int amount) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.balance += amount;
                saveFactionData(factionDataList);
                updatePower(factionName);
                //lassementManager.setFactionMoneyScore(factionName);
                break;
            }
        }
    }

    public static void subMoney(String factionName, int amount) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.balance -= amount;
                saveFactionData(factionDataList);
                updatePower(factionName);
                //ClassementManager.setFactionMoneyScore(factionName);
                break;
            }
        }
    }

    public static void setBalance(String factionName, int amount) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.balance = amount;
                saveFactionData(factionDataList);
                updatePower(factionName);
                break;
            }
        }
    }

    public static int getBalance(String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.balance;
            }
        }
        return 0;
    }

    public static int getTotalChunks(String factionName) {
        List<FactionData> factionDataList = cache;

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.totalChunk;
            }
        }
        return 0;
    }

    public static int getTotalNation() {
        List<FactionData> factionDataList = cache;
        return factionDataList.size();
    }

    public static void updatePower(String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                double rawPower = Math.pow(factionData.totalChunk, 2.0/3.0) * Math.sqrt(factionData.balance + 1) * (100 + factionData.bonusPower);
                factionData.power = (int)Math.round(rawPower);
                //ClassementManager.setFactionPuissanceScore(factionName, factionData.power);
                saveFactionData(factionDataList);
                break;
            }
        }
        updateRank();
    }

    public static void updateBonusPower(String factionName, int bonusPower) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.bonusPower = bonusPower;
                saveFactionData(factionDataList);
                updatePower(factionName);
                break;
            }
        }
    }

    public static void updateRank() {
        List<FactionData> factionDataList = loadFactionData();
        Map<String, Integer> factionRank = Maps.newHashMap();

        for (FactionData factionData : factionDataList) {
            factionRank.put(factionData.name, factionData.power);
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(factionRank.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        int rank = entries.size();
        for (Map.Entry<String, Integer> entry : entries) {
            for (FactionData factionData : factionDataList) {
                if (factionData.name.equals(entry.getKey())) {
                    factionData.rank = rank;
                }
            }
            rank --;
        }
        saveFactionData(factionDataList);
    }

    public static int getPower(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.power;
            }
        }
        return 0;
    }

    public static void incrementTotalChunk(String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.totalChunk ++;
                saveFactionData(factionDataList);
                updatePower(factionName);
                return;
            }
        }
    }

    public static void decrementTotalChunk(String factionName) {
        List<FactionData> factionDataList = loadFactionData();

        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                factionData.totalChunk --;
                saveFactionData(factionDataList);
                updatePower(factionName);
                return;
            }
        }
    }

    public static int getUniqueColor() {
        List<FactionData> factionDataList = cache;
        List<Integer> usedColor = new ArrayList<>();

        for (FactionData factionData : factionDataList) {
            usedColor.add(factionData.color);
        }

        List<Integer> colors = new ArrayList<>(Arrays.asList(ColorCode.COLORS));
        Collections.shuffle(colors);

        for (int color : colors) {
            if (!usedColor.contains(color)) {
                return color;
            }
        }
        return 0;
    }

    public static int getFactionColor(String factionName) {
        List<FactionData> factionDataList = cache;
        for (FactionData factionData : factionDataList) {
            if (factionData.name.equals(factionName)) {
                return factionData.color;
            }
        }
        return 0;
    }

    public static void adminGenerateNation(int count) {
        List<FactionData> factionDataList = loadFactionData();

        String[] nomsNations = {
                "Royaume d'Asgard",
                "Fédération de Valoria",
                "Empire d'Eldoria",
                "République d'Auren",
                "Principauté de Solaria",
                "Duché d'Obran",
                "Cité-État de Myria",
                "Alliance de Norvind",
                "Union d'Avalen",
                "Coalition de Tharion",
                "Confédération de Lumeris",
                "Dynastie d'Orvan",
                "Protectorat de Veyron",
                "Seigneurie d'Almara",
                "Territoire de Brisden",
                "Nation de Kaelor",
                "Communauté d'Ilmaren",
                "Ordre de Zephyros",
                "Clan d'Urgan",
                "Conglomérat de Nerys"
        };


        for (int i = 0; i < count; i++) {
            int index = new Random().nextInt(nomsNations.length);
            String name =  nomsNations[index];

            String uuid = new Random()
                    .ints(10, 'A', 'Z' + 1)
                    .collect(StringBuilder::new,
                            StringBuilder::appendCodePoint,
                            StringBuilder::append)
                    .toString();

            factionDataList.add(new FactionData(name, uuid, 0));
        }
        saveFactionData(factionDataList);
    }

    public static void updateJsonFile() {
        List<FactionData> factionDataList = loadFactionData();
        for (FactionData factionData : factionDataList) {
            if (factionData.color == 0)
                factionData.color = 0xB3FFFFFF;
        }
        saveFactionData(factionDataList);
    }
}

/*
 * Classe utilitaire FactionManager
 *
 * Cette classe gère toutes les données liées aux factions dans le fichier JSON "factions.json".
 * Elle utilise Gson pour sérialiser/désérialiser une liste de FactionData.
 *
 * Rôles principaux :
 * - Charger et sauvegarder les données de toutes les factions.
 * - Créer ou supprimer une faction, en gérant également :
 *      -> la réinitialisation des claims via ClaimManager,
 *      -> la mise à jour de l’appartenance des joueurs via PlayerDataBase.
 * - Vérifier l’existence d’une faction et le statut des joueurs :
 *      -> propriétaire, officier, membre ou simple membre de la faction.
 * - Gérer les membres :
 *      -> ajout/retrait de joueurs,
 *      -> promotion/dégradation (officier ↔ membre),
 *      -> transfert de propriété.
 * - Gérer l’économie des factions :
 *      -> consultation et modification du solde (addMoney, subMoney, setBalance),
 *      -> calcul de la puissance (power) en fonction du solde, des chunks et des bonus,
 *      -> mise à jour de la puissance (updatePower).
 * - Fournir des informations formatées pour l’affichage (getFactionInfos, getFactionList).
 * - Gérer les territoires (chunks) :
 *      -> incrémenter/décrémenter le nombre total de chunks,
 *      -> récupérer le total.
 *
 * Notes :
 * - Chaque méthode charge toujours les données (loadFactionData), les modifie en mémoire,
 *   puis les sauvegarde (saveFactionData).
 * - La puissance (power) d’une faction est calculée avec la formule :
 *      power = (totalChunk^(2/3)) * sqrt(balance + 1) * (100 + bonusPower)
 * - Les chaînes de caractères sont sensibles à la casse sauf mention contraire (equalsIgnoreCase).
 * -
*/