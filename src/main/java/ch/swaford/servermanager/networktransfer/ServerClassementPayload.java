package ch.swaford.servermanager.networktransfer;

import ch.swaford.servermanager.classement.NationScoreData;
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


public record ServerClassementPayload(List<ServerClassementData> data) implements CustomPacketPayload {
    public static final Type<ServerClassementPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "classement_payload"));

    public static final StreamCodec<FriendlyByteBuf, ServerClassementPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.data.size());
                        for (ServerClassementData data : payload.data) {
                            buf.writeUtf(data.classementName());
                            buf.writeUtf(data.description());
                            buf.writeBoolean(data.enable());
                            buf.writeBoolean(data.isPeriodicUpdate());
                            buf.writeInt(data.scoreReductionRate());
                            buf.writeVarInt(data.serverNationScoreData().size());
                            for (ServerNationScoreData serverNationScoreData : data.serverNationScoreData()) {
                                buf.writeUtf(serverNationScoreData.name());
                                buf.writeInt(serverNationScoreData.score());
                                buf.writeInt(serverNationScoreData.rank());
                                buf.writeInt(serverNationScoreData.memberCount());
                            }
                        }
                    },
                    buf -> {
                        int classementSize = buf.readVarInt();
                        List<ServerClassementData> data = new ArrayList<>(classementSize);
                        for (int i = 0; i < classementSize; i++) {
                            String classementName = buf.readUtf();
                            String description = buf.readUtf();
                            boolean enable = buf.readBoolean();
                            boolean isPeriodicUpdate = buf.readBoolean();
                            int scoreReductionRate = buf.readInt();
                            int nationScoreDataListSize = buf.readVarInt();
                            List<ServerNationScoreData> serverNationScoreData = new ArrayList<>(nationScoreDataListSize);
                            for (int j = 0; j < nationScoreDataListSize; j++) {
                                String name =  buf.readUtf();
                                int score = buf.readInt();
                                int rank = buf.readInt();
                                int memberCount = buf.readInt();
                                serverNationScoreData.add(new ServerNationScoreData(name, score, rank, memberCount));
                            }
                            data.add(new ServerClassementData(classementName, description, enable, isPeriodicUpdate, scoreReductionRate, serverNationScoreData));
                        }

                        return new ServerClassementPayload(data);
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
