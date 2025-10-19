package ch.swaford.servermanager.clientinterface.newinterface;

import ch.swaford.servermanager.FactionManager;
import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.ClientCache;
import ch.swaford.servermanager.networktransfer.RequestFactionData;
import ch.swaford.servermanager.networktransfer.ServerFactionData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Flow;

public class FactionBankInterface extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        // Layout vertical centré
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        ServerFactionData serverFactionData = ClientCache.getServerFactionData();

        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        int imgW = 960;
        int imgH = 600;

        int targetW = (int) (imgW / guiScale);
        int targetH = (int) (imgH / guiScale);

        root
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(10));

        FlowLayout factionTemplate = FactionInterfaceTemplate.create(
                "bank_page.png"
        );

        FlowLayout mainPanel = factionTemplate.childById(FlowLayout.class, "mainPanel");
        mainPanel.padding(Insets.of(UITools.logicalH(0.40), UITools.logicalH(0.27), UITools.logicalW(0.2), UITools.logicalW(0.2)));

        FlowLayout panel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
                .gap((int)(targetH * 0.04))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout soldePanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(50))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        soldePanel.child(new ScaledTextComponent(NumberFormat.getInstance(Locale.FRENCH).format(serverFactionData.balance()) + "€", 0xFFFFFFFF, 1.7f, true).margins(Insets.top((int)(targetH * 0.05f))));

        FlowLayout bottomPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(50))
                .gap((int)(targetW * 0.02))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout bottomLeftPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(60), Sizing.fill(80))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        TextBoxComponent textComponent = Components.textBox(Sizing.fill(80));
        textComponent.text("montant...");
        textComponent.setBordered(false);

        bottomLeftPanel.child((Component) textComponent);

        FlowLayout button = (FlowLayout) Containers.horizontalFlow(Sizing.fill(40), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        button.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            String value = textComponent.getValue();
            try {
                int n = Integer.parseInt(value);
                assert minecraftInstance.player != null;
                minecraftInstance.player.connection.send(new ServerboundChatCommandPacket("nation deposit " + n));
                Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestFactionData("server", 2));
            } catch (NumberFormatException e) {
                textComponent.text("Montant invalide");
            }
            return true;
        });

        mainPanel.child(panel);
        panel.child(soldePanel);
        panel.child(bottomPanel);
            bottomPanel.child(bottomLeftPanel);
            bottomPanel.child(button);

        root.child(factionTemplate);
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}