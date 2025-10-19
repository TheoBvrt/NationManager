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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class QuantityInterface extends BaseOwoScreen<FlowLayout> {
    private FlowLayout quantityPage;
    private ItemStack itemStack;
    private final boolean sellMod;
    private final int maxQuantity;
    private final String itemId;
    private final int basePrice;

    public QuantityInterface(ItemStack itemStack, boolean sellMod, int maxQuantity, String itemId, int basePrice) {
        super(); // Appel du constructeur parent (important)
        this.itemStack = itemStack;
        this.sellMod = sellMod;
        this.maxQuantity = maxQuantity;
        this.itemId = itemId;
        this.basePrice = basePrice;
    }

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

        String texturePath = sellMod ? "sell_quantity_interface.png" : "buy_quantity_interface.png";
        FlowLayout shopTemplate = QuantityTemplate.create(sellMod, texturePath, itemStack, maxQuantity, itemId, basePrice);
        quantityPage = shopTemplate;

        root.child(shopTemplate);
    }

    public void updatePrice() {
        List<ServerShopItemData> serverShopItemDataList = ClientCache.getServerShopItemDataList();

        ScaledTextComponent scaledTextComponent = quantityPage.childById(ScaledTextComponent.class, "sellPrice");
        if (scaledTextComponent != null)
        {
            ServerShopItemData itemData = serverShopItemDataList.stream()
                    .filter(item -> item.itemId().equals(itemId))
                    .findFirst()
                    .orElse(null);

            if (sellMod) {
                assert itemData != null;
                scaledTextComponent.setText(UITools.formatPrice(itemData.sellPrice()));
            } else {
                assert itemData != null;
                scaledTextComponent.setText(UITools.formatPrice(itemData.buyPrice()));
            }
        }
    }
}
