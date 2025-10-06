package ch.swaford.servermanager.networktransfer;

import ch.swaford.servermanager.ClaimData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ServerClaimDataPayload(List<ClaimData> claims) implements CustomPacketPayload {
    public static final Type<ch.swaford.servermanager.networktransfer.ServerClaimDataPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "server_claims"));

    public static final StreamCodec<FriendlyByteBuf, ch.swaford.servermanager.networktransfer.ServerClaimDataPayload> CODEC =
            StreamCodec.of(
                    // encode
                    (buf, payload) -> {
                        buf.writeVarInt(payload.claims().size());
                        for (ClaimData claim : payload.claims()) {
                            buf.writeUtf(claim.factionName);
                            buf.writeInt(claim.x);
                            buf.writeInt(claim.z);
                            buf.writeInt(claim.color);
                        }
                    },
                    // decode
                    buf -> {
                        int size = buf.readVarInt();
                        List<ClaimData> claims = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            String factionName = buf.readUtf();
                            int x = buf.readInt();
                            int z = buf.readInt();
                            int color = buf.readInt();
                            claims.add(new ClaimData(factionName, x, z, color));
                        }
                        return new ServerClaimDataPayload(claims);
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
