package ch.swaford.servermanager.shop;

import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ShopTemplate {
    public static FlowLayout create(boolean sellMod, String background) {
        ServerClientData serverClientData = ClientCache.getServerClientData();
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();

        int imgW = 960;
        int imgH = 600;

        int targetW = (int) (imgW / guiScale);
        int targetH = (int) (imgH / guiScale);
        final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
                "servermanager",
                "textures/gui/" + background
        );

        FlowLayout mainPanel = (FlowLayout) Containers.verticalFlow(Sizing.fixed(targetW), Sizing.fixed(targetH))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(0,  0, (int)(targetW * 0.078f), (int)(targetW * 0.08f)))
                .surface((context, component) -> {
                    context.blit(
                            BG_TEXTURE,
                            component.x(), component.y(),
                            component.width(), component.height(),
                            0, 0,
                            960, 600,
                            960, 600
                    );
                });

        FlowLayout menu = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100),  Sizing.fill(20))
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of((int)(targetH * 0.06f), 0, 0, 0));

        FlowLayout content = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .gap((int)(targetW * 0.02f))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout rightContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(48), Sizing.content())
                .gap(UITools.logicalH(0.02f))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.TOP);
        FlowLayout leftContent = (FlowLayout) Containers.verticalFlow(Sizing.fill(48), Sizing.content())
                .gap(UITools.logicalH(0.02f))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.TOP);

        List<ServerShopItemData> serverShopItemDataList = ClientCache.getServerShopItemDataList();
        int i = 0;
        for (ServerShopItemData serverShopItemData : serverShopItemDataList) {
            int price = sellMod ? serverShopItemData.sellPrice() : serverShopItemData.buyPrice();
            if (i % 2 == 0) {
                leftContent.child(ItemContainer.createItemContainer(
                        targetH,
                        targetW,
                        sellMod,
                        serverShopItemData.itemId(),
                        UITools.stackFromString(serverShopItemData.itemId()),
                        price,
                        serverShopItemData.quantity(),
                        serverShopItemData.limited()
                ));
            } else {
                rightContent.child(ItemContainer.createItemContainer(
                        targetH,
                        targetW,
                        sellMod,
                        serverShopItemData.itemId(),
                        UITools.stackFromString(serverShopItemData.itemId()),
                        price,
                        serverShopItemData.quantity(),
                        serverShopItemData.limited()
                ));
            }
            i ++;
        }

        content.child(rightContent);
        content.child(leftContent);

        ScrollContainer<FlowLayout> shop = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.fill(70),
                content
        );

        shop.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x00000000)));
        shop.padding(Insets.of((int)(targetH * 0.1f), (int)(targetH * 0.12f), 0, 0));

        FlowLayout sellMenuButton = (FlowLayout) Containers.horizontalFlow(Sizing.fill(16),  Sizing.fill(100));

        FlowLayout buyMenuButton = (FlowLayout) Containers.horizontalFlow(Sizing.fill(16),  Sizing.fill(100));

        sellMenuButton.mouseDown().subscribe((mx, my, btn) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (mc.player != null && mc.getConnection() != null) {
                mc.getConnection().send(new RequestPlayerData(4));
            }
            return true;
        });

        buyMenuButton.mouseDown().subscribe((mx, my, btn) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (mc.player != null && mc.getConnection() != null) {
                mc.getConnection().send(new RequestPlayerData(3));
            }
            return true;
        });

        FlowLayout bottom = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100),  Sizing.fill(10))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout leftBottom = (FlowLayout) Containers.horizontalFlow(Sizing.fill(50),  Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        leftBottom.child(new ScaledTextComponent("Argent : " + NumberFormat.getInstance(Locale.FRENCH).format(serverClientData.balance()) + "€", 0xFFFFB000, 1, false));

        FlowLayout rightBottom = (FlowLayout) Containers.horizontalFlow(Sizing.fill(50),  Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.RIGHT);

        FlowLayout menuButton = (FlowLayout) Containers.horizontalFlow(Sizing.fill( 30),  Sizing.fill(100));

        menuButton.mouseDown().subscribe((mx, my, btn) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestFactionData("server", 0));
            return true;
        });

        bottom.child(leftBottom);
        bottom.child(rightBottom);
            rightBottom.child(menuButton);

        mainPanel.child(menu);
            menu.child(buyMenuButton);
            menu.child(sellMenuButton);
        mainPanel.child(shop);
        mainPanel.child(bottom);

        return mainPanel;
    }
}
