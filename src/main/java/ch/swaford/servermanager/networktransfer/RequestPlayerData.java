package ch.swaford.servermanager.networktransfer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/*
 * Payload réseau RequestFactionData
 *
 * Sert de requête envoyée par le client au serveur
 * pour demander les informations concernant une nation/faction précise.
 *
 * Caractéristiques :
 * - Contient un seul champ : factionName (nom de la faction demandée).
 * - Permet au client d’obtenir les données liées à une faction
 *   (exemple : owner, membres, power, etc.) via une réponse du serveur.
 *
 * Fonctionnement :
 * - Le client envoie RequestFactionData avec un nom de faction.
 * - Le serveur reçoit et vérifie si la faction existe :
 *      -> Si oui : renvoie un ServerFactionDataPayload avec les infos complètes.
 *      -> Si non : renvoie une réponse avec valeurs par défaut ou ignore.
 *
 * Notes :
 * - Le StreamCodec sérialise/désérialise le champ factionName.
 * - La méthode type() identifie ce payload via un ResourceLocation unique.
 */


public record RequestPlayerData(int screen) implements CustomPacketPayload {

    public static final Type<RequestPlayerData> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "request_player_data"));

    public static final StreamCodec<FriendlyByteBuf, RequestPlayerData> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.screen());
                    },
                    buf -> new RequestPlayerData(buf.readInt())
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
