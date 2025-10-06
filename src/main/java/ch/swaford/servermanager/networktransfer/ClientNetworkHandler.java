package ch.swaford.servermanager.networktransfer;

import ch.swaford.servermanager.classement.classementinterface.PuissanceInterface;
import ch.swaford.servermanager.clientinterface.newinterface.FactionBankInterface;
import ch.swaford.servermanager.clientinterface.newinterface.FactionMainInterface;
import ch.swaford.servermanager.clientinterface.newinterface.FactionMembersInterface;
import ch.swaford.servermanager.shop.BuyShopInterface;
import ch.swaford.servermanager.shop.SellShopInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/*
 * Classe ClientNetworkHandler
 *
 * Cette classe centralise la gestion des payloads réseau reçus par le client.
 * Elle agit comme point d’entrée pour mettre à jour le cache client et
 * ouvrir les interfaces correspondantes.
 *
 * Méthodes :
 * - handleServerClientData(ServerClientData data) :
 *      → Appelée quand le serveur envoie les infos spécifiques au joueur
 *        (UUID, nom, faction, solde).
 *      → Met à jour le ClientCache avec ces données et ouvre le menu FactionMainMenu.
 *
 * - handleServerFactionData(ServerFactionData data) :
 *      → Appelée quand le serveur envoie les infos détaillées d’une faction.
 *      → Met à jour le ClientCache avec ces données et ouvre également FactionMainMenu.
 *
 * Notes :
 * - Les actions sont exécutées via mc.execute(), garantissant leur exécution
 *   sur le thread principal du client (sécurité graphique).
 * - Cette classe ne contient aucune logique métier, elle se limite à la
 *   synchronisation des données et à la navigation vers les écrans UI.
 */


public class ClientNetworkHandler {

    public static void handleServerFactionData(ServerFactionData data, int screen, ServerClientData clientData) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.setServerFactionData(data);
            ClientCache.setServerClientData(clientData);
            if (screen == 0)
                mc.setScreen(new FactionMainInterface());
            if (screen == 1)
                mc.setScreen(new FactionMembersInterface());
            if (screen == 2)
                mc.setScreen(new FactionBankInterface());
            if (screen == 3)
                mc.setScreen(new BuyShopInterface());
        });
    }

    public static void handleServerClassementData(List<ServerClassementData> classementData) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.setServerClassementDataList(classementData);
            mc.setScreen(new PuissanceInterface());
        });
    }

    public static void handleServerClientData(ServerClientData clientData, int screen) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientCache.setServerClientData(clientData);
            if (screen == -1)
                return;
            if (screen == 3)
                mc.setScreen(new BuyShopInterface());
            if (screen == 4)
                mc.setScreen(new SellShopInterface());
        });
    }
}
