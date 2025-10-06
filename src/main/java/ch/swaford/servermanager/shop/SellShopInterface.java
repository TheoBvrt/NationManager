package ch.swaford.servermanager.shop;

import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.ClientCache;
import ch.swaford.servermanager.networktransfer.ServerShopItemData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.VerticalAlignment;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class SellShopInterface extends BaseOwoScreen<FlowLayout> {
    private FlowLayout shop;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        // Layout vertical centré
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(10));

        FlowLayout shopTemplate = ShopTemplate.create(true, "sell_shop_menu.png");

        shop = shopTemplate;
        root.child(shopTemplate);
    }

    public void updatePrice() {
        List<ServerShopItemData> serverShopItemDataList = ClientCache.getServerShopItemDataList();

        for (ServerShopItemData serverShopItemData : serverShopItemDataList) {
            FlowLayout container = shop.childById(FlowLayout.class, serverShopItemData.itemId());
            if (container != null) {
                ScaledTextComponent scaledTextComponent = container.childById(ScaledTextComponent.class, "sellPrice");
                if (scaledTextComponent != null) {
                    scaledTextComponent.setText(UITools.formatPrice(serverShopItemData.sellPrice()));
                }
            }
        }
    }
}
