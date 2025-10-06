package ch.swaford.servermanager.networktransfer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/*
 * Payload réseau ServerCientDataPayload
 *
 * Sert à transférer les informations de base d’un joueur
 * du serveur vers le client via le système de CustomPacketPayload.
 *
 * Contenu transféré :
 * - UUID du joueur (playerUuid)
 * - Nom du joueur (playerName)
 * - Nom de la faction du joueur (playerFaction)
 * - Solde actuel du joueur (balance)
 *
 * Fonctionnement :
 * - La sérialisation/désérialisation se fait via un StreamCodec.
 * - Les champs sont écrits et lus dans l’ordre exact : UUID, nom, faction, solde.
 *
 * Utilisation :
 * - Envoyé par le serveur lorsqu’un client demande ses informations.
 * - Permet au client de mettre à jour son interface (par ex. solde affiché,
 *   appartenance à une faction, etc.).
 */


public record ServerCientDataPayload(ServerClientData data, int screen) implements CustomPacketPayload {
    public static final Type<ServerCientDataPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "server_info"));

    public static final StreamCodec<FriendlyByteBuf, ServerCientDataPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.data.playerUuid());
                        buf.writeUtf(payload.data.playerName());
                        buf.writeUtf(payload.data.playerFaction());
                        buf.writeInt(payload.data.balance());
                        buf.writeInt(payload.screen());
                    },
                    buf -> {
                        String playerUuid = buf.readUtf();
                        String playerName = buf.readUtf();
                        String playerFaction = buf.readUtf();
                        int balance = buf.readInt();
                        int screen = buf.readInt();
                        return new ServerCientDataPayload(
                                new ServerClientData(playerUuid, playerName, playerFaction,  balance),
                                screen
                        );
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
