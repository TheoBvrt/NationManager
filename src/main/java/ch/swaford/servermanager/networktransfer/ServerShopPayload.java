package ch.swaford.servermanager.networktransfer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ServerShopPayload(List<ServerShopItemData> serverShopItemData) implements CustomPacketPayload {
    public static final Type<ch.swaford.servermanager.networktransfer.ServerShopPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath("servermanager", "server_shop_payload"));

    public static final StreamCodec<FriendlyByteBuf, ch.swaford.servermanager.networktransfer.ServerShopPayload> CODEC =
            StreamCodec.of(
                    // encode
                    (buf, payload) -> {
                        buf.writeVarInt(payload.serverShopItemData.size());
                        for (ServerShopItemData item : payload.serverShopItemData()) {
                            buf.writeUtf(item.itemId());
                            buf.writeUtf(item.itemCategory());
                            buf.writeInt(item.sellPrice());
                            buf.writeInt(item.buyPrice());
                            buf.writeInt(item.quantity());
                            buf.writeBoolean(item.limited());
                        }
                    },
                    // decode
                    buf -> {
                        int size = buf.readVarInt();
                        List<ServerShopItemData> serverShopItemDataList = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            String itemId = buf.readUtf();
                            String itemCategory = buf.readUtf();
                            int sellPrice = buf.readInt();
                            int buyPrice = buf.readInt();
                            int quantity = buf.readInt();
                            boolean limited = buf.readBoolean();
                            serverShopItemDataList.add(new ServerShopItemData(itemId, itemCategory, sellPrice, buyPrice, quantity, limited));
                        }
                        return new ServerShopPayload(serverShopItemDataList);
                    }
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
