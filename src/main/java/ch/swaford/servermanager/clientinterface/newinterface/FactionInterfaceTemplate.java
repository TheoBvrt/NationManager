package ch.swaford.servermanager.clientinterface.newinterface;

import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.RequestClassementData;
import ch.swaford.servermanager.networktransfer.RequestFactionData;
import ch.swaford.servermanager.networktransfer.RequestPlayerData;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.awt.*;
import java.util.Objects;

import static ch.swaford.servermanager.clientinterface.UITools.logicalH;

public class FactionInterfaceTemplate {

    public static FlowLayout create(String background) {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();

        int imgW = 1056;
        int imgH = 660;

        int targetW = (int) (imgW / guiScale);
        int targetH = (int) (imgH / guiScale);



        final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
                "servermanager",
                "textures/gui/" + background
        );

        FlowLayout windows = (FlowLayout) Containers.horizontalFlow(Sizing.fixed(UITools.logicalW(1.0)), Sizing.fixed(logicalH(1.0)))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface((context, component) -> {
                    context.blit(
                            BG_TEXTURE,
                            component.x(), component.y(),
                            component.width(), component.height(),
                            0, 0,
                            1056, 660,
                            1056, 660
                    );
                });

        FlowLayout menu = (FlowLayout) Containers.verticalFlow(Sizing.fill(7), Sizing.fill(100));
        FlowLayout menuTop = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(80))
                .verticalAlignment(VerticalAlignment.TOP)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.top(logicalH(0.031f)));

        FlowLayout menuBottom = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(20))
                .verticalAlignment(VerticalAlignment.TOP)
                .padding(Insets.top(20));
        menuBottom.child(button(0, 50));

        menuTop.child(button(0, 11)); //main
        menuTop.child(button(1, 11)); //membre
        menuTop.child(button(4, 11)); //classement
        menuTop.child(button(2, 11)); //bank
        menuTop.child(button(3, 11)); //shop

        FlowLayout mainPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(93), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.TOP)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(0, 0, UITools.logicalW(0.082), UITools.logicalW(0.03)))
                .id("mainPanel");

        windows.child(menu);
            menu.child(menuTop);
            menu.child(menuBottom);
        windows.child(mainPanel);

        return windows;
    }

    private static FlowLayout button(int screen, int height) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        FlowLayout button = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(height));
        button.mouseDown().subscribe((mx, my, ctx) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (screen == 4) {
                Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestClassementData());
                return true;
            }

            if (screen == 3) {
                Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestPlayerData(3));
            }

            Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestFactionData("server", screen));
            return true;
        });
        return button;
    }
}
