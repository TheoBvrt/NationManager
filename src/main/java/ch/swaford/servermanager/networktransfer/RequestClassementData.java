package ch.swaford.servermanager.networktransfer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/*
 * Payload réseau RequestPlayerData
 *
 * Sert de requête envoyée par le client au serveur
 * pour demander l’envoi des informations liées à un joueur.
 *
 * Caractéristiques :
 * - Ce payload ne contient aucun champ (structure vide).
 * - Il agit uniquement comme un signal de demande.
 *
 * Fonctionnement :
 * - Le client envoie RequestPlayerData → le serveur intercepte la requête.
 * - Le serveur répond en envoyant un payload contenant les infos du joueur
 *   (exemple : ServerClientDataPayload).
 *
 * Notes :
 * - Le StreamCodec est défini mais ne sérialise aucun champ
 *   car la requête est vide.
 * - La méthode type() identifie ce payload via un ResourceLocation unique.
 */


public record RequestClassementData() implements CustomPacketPayload {

    public static final Type<RequestClassementData> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "request_classement_data"));

    public static final StreamCodec<FriendlyByteBuf, RequestClassementData> CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        // écrire les champs de RequestPlayerData dans buf
                    },
                    buf -> new RequestClassementData() // ← doit matcher le type générique
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
