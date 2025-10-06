package ch.swaford.servermanager.networktransfer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Payload réseau ServerFactionDataPayload
 *
 * Sert à transférer les informations détaillées d’une faction
 * du serveur vers le client via le système de CustomPacketPayload.
 *
 * Contenu transféré :
 * - Nom de la faction (name)
 * - UUID du propriétaire (ownerUuid)
 * - Balance (balance)
 * - Puissance bonus (bonusPower)
 * - Puissance totale (power)
 * - Nombre de chunks possédés (totalChunk)
 * - Liste des officiers (officers)
 * - Liste des membres (members)
 *
 * Fonctionnement :
 * - La sérialisation/désérialisation se fait via un StreamCodec.
 * - Les tailles des listes sont écrites avec writeVarInt() avant
 *   chaque itération pour officers et members.
 *
 * Utilisation :
 * - Envoyé par le serveur lorsqu’un client demande des informations
 *   sur une faction.
 * - Le client peut ensuite utiliser ces données pour mettre à jour
 *   son interface ou son cache local.
 */


public record ServerFactionDataPayload(ServerFactionData data, int screen, ServerClientData serverClientData) implements CustomPacketPayload {
    public static final Type<ServerFactionDataPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "faction_data_payload"));

    public static final StreamCodec<FriendlyByteBuf, ServerFactionDataPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.data.name());
                        buf.writeUtf(payload.data.ownerUuid());
                        buf.writeInt(payload.data.balance());
                        buf.writeInt(payload.data.bonusPower());
                        buf.writeInt(payload.data.power());
                        buf.writeInt(payload.data.totalChunk());
                        List<String> officers = payload.data.officers();
                        buf.writeVarInt(officers.size());
                        for (String officer : officers) {
                            buf.writeUtf(officer);
                        }
                        List<String> members = payload.data.members();
                        buf.writeVarInt(members.size());
                        for (String member : members) {
                            buf.writeUtf(member);
                        }
                        buf.writeInt(payload.data.rank());
                        buf.writeUtf(payload.data.description());
                        buf.writeUtf(payload.data.bannerTag());

                        buf.writeVarInt(payload.data.playerList().size());
                        Map<String, String> playerList = payload.data().playerList();
                        for (Map.Entry<String, String> entry : playerList.entrySet()) {
                            buf.writeUtf(entry.getKey());
                            buf.writeUtf(entry.getValue());
                        }
                        buf.writeInt(payload.screen());

                        buf.writeUtf(payload.serverClientData().playerUuid());
                        buf.writeUtf(payload.serverClientData().playerName());
                        buf.writeUtf(payload.serverClientData().playerFaction());
                        buf.writeInt(payload.serverClientData().balance());
                    },
                    buf -> {
                        String name = buf.readUtf();
                        String ownerUuid = buf.readUtf();
                        int balance = buf.readInt();
                        int bonusPower = buf.readInt();
                        int power = buf.readInt();
                        int totalChunk = buf.readInt();
                        int officerCount = buf.readVarInt();
                        List<String> officers = new ArrayList<>(officerCount);
                        for (int i = 0; i < officerCount; i++) {
                            officers.add(buf.readUtf());
                        }
                        int memberCount = buf.readVarInt();
                        List<String> members = new ArrayList<>(memberCount);
                        for (int i = 0; i < memberCount; i++) {
                            members.add(buf.readUtf());
                        }
                        int rank = buf.readInt();
                        String description = buf.readUtf();
                        String bannerTag = buf.readUtf();

                        int count = buf.readVarInt();
                        Map<String, String> playerList = new HashMap<>(count);
                        for (int i = 0; i < count; i++) {
                            String uuid =  buf.readUtf();
                            String playerName =  buf.readUtf();
                            playerList.put(uuid, playerName);
                        }
                        int screen = buf.readInt();

                        String playerUUID = buf.readUtf();
                        String playerName = buf.readUtf();
                        String playerFaction = buf.readUtf();
                        int playerBalance = buf.readInt();

                        return new ServerFactionDataPayload(
                                new ServerFactionData(name, ownerUuid, balance, bonusPower, power, totalChunk, officers, members, rank, description, bannerTag, playerList),
                                screen,
                                new ServerClientData(playerUUID, playerName, playerFaction, playerBalance)
                        );
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
