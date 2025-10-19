package ch.swaford.servermanager.shop;

import ch.swaford.servermanager.clientinterface.ConfirmationPopup;
import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.RequestPlayerData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import io.wispforest.owo.ui.component.ItemComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.Flow;

public class ItemContainer {
    public static FlowLayout createItemContainer(int targetH, int targetW, Boolean sellMod, String itemId, ItemStack stack, int defaultPrice, int quantity, boolean limited) {
        FlowLayout container = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(29))
                .surface(Surface.flat(0xFF252A40));

        FlowLayout iconContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(25), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        ItemComponent itemComp = (ItemComponent) Components.item(stack)
                .sizing(Sizing.fixed((int)(targetH * 0.13f)), Sizing.fixed((int)(targetH * 0.13f)));

        itemComp.showOverlay(true);
        itemComp.setTooltipFromStack(true);

        iconContainer.child(itemComp);

        FlowLayout priceContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(35), Sizing.fill(100))
                .gap(1)
                .padding(Insets.left(5))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        FlowLayout purchaseContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(40), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        String pngFile = sellMod ? "sell_button.png" : "buy_button.png";
        final ResourceLocation buttonTexture = ResourceLocation.fromNamespaceAndPath(
                "servermanager",
                "textures/gui/" + pngFile
        );

        FlowLayout button = (FlowLayout) Containers.horizontalFlow(Sizing.fill(70), Sizing.fill(55))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .surface((ctx, component) -> {
                    ctx.blit(
                            buttonTexture,
                            component.x(), component.y(),
                            component.width(), component.height(), // taille affichée
                            0, 0,
                            384, 192, // taille de ta texture d'origine
                            384, 191
                    );
                });

        priceContainer.child(new ScaledTextComponent(UITools.formatPrice(defaultPrice), 0xFFFFB000, 1.2f, true).id("sellPrice"));
        priceContainer.child(new ScaledTextComponent(quantity + "x", 0xFFFFFFFF, 1, true).margins(Insets.left(2)));

        button.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft minecraftInstance = Minecraft.getInstance();
            String command = sellMod ? "shop sell " : "shop buy ";
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            Item item = BuiltInRegistries.ITEM.get(id);
            if ( limited || quantity == 1 || (sellMod && ShopManager.floorDivPos(10L * defaultPrice, quantity) <= 0)) {
                int newQuantity = limited ? 1 : quantity;
                minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                assert minecraftInstance.player != null;
                minecraftInstance.player.connection.send(new ServerboundChatCommandPacket(command + "\"" + itemId + "\" " + newQuantity));
                if (sellMod)
                    Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestPlayerData(4));
                else
                    Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestPlayerData(3));
            } else
            {
                if (sellMod)
                    minecraftInstance.setScreen(new QuantityInterface(stack, true, quantity, itemId, defaultPrice));
                else
                    minecraftInstance.setScreen(new QuantityInterface(stack, false, quantity, itemId, defaultPrice));
            }
            return true;
        });

        container.id(itemId);

        container.child(iconContainer);
        container.child(priceContainer);
        container.child(purchaseContainer);
            purchaseContainer.child(button);

        return container;
    }
}
