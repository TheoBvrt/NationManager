package ch.swaford.servermanager.shop;

import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.*;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Flow;

import static ch.swaford.servermanager.shop.ShopManager.ceilDivPos;
import static ch.swaford.servermanager.shop.ShopManager.floorDivPos;

public class QuantityTemplate {
    public static FlowLayout create(boolean sellMod, String background, ItemStack itemStack, int maxQuantity, String itemId, int basePrice) {
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
                .padding(Insets.of((int)(targetH * 0.11f),  (int)(targetH * 0.13f), (int)(targetW * 0.19f), (int)(targetW * 0.19f)))
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

        FlowLayout topMenu = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(10))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.RIGHT);

        FlowLayout backButton = (FlowLayout) Containers.verticalFlow(Sizing.fill(20), Sizing.fill(100));

        backButton.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (sellMod)
                Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestPlayerData(4));
            else
                Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestPlayerData(3));
            return true;
        });

        FlowLayout centerPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(75))
                .gap((int)(targetW * 0.05f))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout leftCenterPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(50), Sizing.fill(100))
                .horizontalAlignment(HorizontalAlignment.RIGHT)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout rightCenterPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(50), Sizing.fill(100))
                .gap((int)(targetH * 0.02f))
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.CENTER);

        ItemComponent itemComp = (ItemComponent) Components.item(itemStack)
                .sizing(Sizing.fill(40),  Sizing.fill(40));

        itemComp.showOverlay(true);
        itemComp.setTooltipFromStack(true);

        leftCenterPanel.child(itemComp);

        centerPanel.child(leftCenterPanel);
        centerPanel.child(rightCenterPanel);

        rightCenterPanel.child(new ScaledTextComponent(UITools.formatPrice(basePrice), 0xFFFFB000, 1.7f, true).id("sellPrice").margins(Insets.top((int)(targetH * 0.07f))));
        rightCenterPanel.child(new ScaledTextComponent(maxQuantity + "x", 0xFFFFFFFF, 1.2f, true).margins(Insets.left(2)));


        FlowLayout bottomMenu = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(15))
                .gap((int)(targetW * 0.03f))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        if (sellMod)
        {
            bottomMenu.child(actionButton(10, sellMod, itemId, basePrice, maxQuantity));
            bottomMenu.child(actionButton(maxQuantity, sellMod, itemId, basePrice, maxQuantity));
        } else {
            bottomMenu.child(actionButton(1, sellMod, itemId, basePrice, maxQuantity));
            bottomMenu.child(actionButton(10, sellMod, itemId, basePrice, maxQuantity));
            bottomMenu.child(actionButton(maxQuantity, sellMod, itemId, basePrice, maxQuantity));
        }

        mainPanel.child(topMenu);
            topMenu.child(backButton);
        mainPanel.child(centerPanel);
        mainPanel.child(bottomMenu);

        return mainPanel;
    }

    private static FlowLayout actionButton(int quantity, boolean sellMod, String itemId, int price, int maxQuantity) {
        FlowLayout button = (FlowLayout)  Containers.horizontalFlow(Sizing.fill(30), Sizing.fill(100));

        String command = sellMod ? "shop sell " : "shop buy ";
        button.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            assert minecraftInstance.player != null;
            minecraftInstance.player.connection.send(new ServerboundChatCommandPacket(command + "\"" + itemId + "\" " + quantity));
            return true;
        });

        if (sellMod)
            button.tooltip(Component.literal(UITools.formatPrice(floorDivPos((long) quantity * price, maxQuantity))));
        else
            button.tooltip(Component.literal(UITools.formatPrice(ceilDivPos((long) quantity * price, maxQuantity))));
        return button;
    }
}
