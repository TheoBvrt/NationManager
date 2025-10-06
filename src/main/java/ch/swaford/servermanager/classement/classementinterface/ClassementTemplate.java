package ch.swaford.servermanager.classement.classementinterface;

import ca.weblite.objc.Client;
import ch.swaford.servermanager.classement.ClassementData;
import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.ClientCache;
import ch.swaford.servermanager.networktransfer.RequestFactionData;
import ch.swaford.servermanager.networktransfer.ServerClassementData;
import ch.swaford.servermanager.networktransfer.ServerNationScoreData;
import ch.swaford.servermanager.shop.SellShopInterface;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ClassementTemplate {
    public static FlowLayout create(String background, String classementName, String scoreLabel, String playerFaction, String scoreUnit) {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();

        int imgW = 1056;
        int imgH = 660;

        int targetW = (int) (imgW / guiScale);
        int targetH = (int) (imgH / guiScale);

        ServerClassementData serverClassementData = ClientCache.getServerClassementDataList()
                .stream()
                .filter(c -> c.classementName().equals(classementName))
                .findFirst()
                .orElse(null);

        final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
                "servermanager",
                "textures/gui/" + background
        );

        FlowLayout windows = (FlowLayout) Containers.horizontalFlow(Sizing.fixed(targetW), Sizing.fixed(targetH))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of((int)(targetH * 0.1f),  (int)(targetH * 0.1f), (int)(targetW * 0.06f), (int)(targetW * 0.06f)))
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

        FlowLayout leftMenu = (FlowLayout) Containers.verticalFlow(Sizing.fill(15), Sizing.fill(100))
                .gap(UITools.logicalH(0.026f));

        leftMenu.child(MenuButton(new PuissanceInterface(), 8));
        leftMenu.child(MenuButton(new DiplomatieInterface(), 8));
        leftMenu.child(MenuButton(new PopulationPage(), 8));
        leftMenu.child(MenuButton(new TailleInterface(), 8));
        leftMenu.child(MenuButton(new ArgentInterface(), 8));
        leftMenu.child(MenuButton(new AgricultureInterface(), 8));
        leftMenu.child(MenuButton(new MinageInterface(), 8));

        FlowLayout mainPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(70), Sizing.fill(100))
                .padding(Insets.of(0, 0, (int)(targetW * 0.03f), (int)(targetW * 0.03f)))
                .verticalAlignment(VerticalAlignment.TOP)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .id("mainPanel");

        FlowLayout rightMenu = (FlowLayout) Containers.verticalFlow(Sizing.fill(15), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.TOP)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout rightTopMenu = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(50))
            .verticalAlignment(VerticalAlignment.TOP)
            .horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout rightBottomMenu = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(50))
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout nationButton = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(18));

        nationButton.mouseDown().subscribe((mx, my, btn) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestFactionData("server", 0));
            return true;
        });

        rightTopMenu.child(MenuButton(new CommerceInterface(), 16));
        rightBottomMenu.child(nationButton);


        FlowLayout scrollContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        scrollContent.gap(UITools.logicalH(0.02f));

        assert serverClassementData != null;
        List<ServerNationScoreData> serverNationScoreDataList = new ArrayList<>(serverClassementData.serverNationScoreData());
        serverNationScoreDataList.sort(Comparator.comparingInt(ServerNationScoreData::rank));

        for (ServerNationScoreData serverNationScoreData : serverNationScoreDataList) {
            scrollContent.child(ClassementContainer.createClassementContainer(
                    serverNationScoreData.name(),
                    serverNationScoreData.rank(),
                    serverNationScoreData.score(),
                    serverNationScoreData.memberCount(),
                    scoreLabel,
                    false,
                    scoreUnit
            ));
        }

        ScrollContainer<FlowLayout> scrollab = Containers.verticalScroll(
                Sizing.fill(100),
                Sizing.fill(70),
                scrollContent
        );

        scrollab.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xFF181E36)));

        windows.child(leftMenu);
        windows.child(mainPanel);
            mainPanel.child(scrollab);
        windows.child(rightMenu);
            rightMenu.child(rightTopMenu);
            rightMenu.child(rightBottomMenu);

        FlowLayout fixed = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(30))
                .gap(UITools.logicalH(0.02f))
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        if (!playerFaction.equals("server")) {

            int rank = 0;
            int score = 0;
            int memberCount = 0;

            for (ServerNationScoreData serverNationScoreData : serverNationScoreDataList) {
                if (serverNationScoreData.name().equals(playerFaction)) {
                    rank = serverNationScoreData.rank();
                    score = serverNationScoreData.score();
                    memberCount = serverNationScoreData.memberCount();
                }
            }

            fixed.child(ClassementContainer.createClassementContainer(playerFaction, rank, score, memberCount, scoreLabel, true, scoreUnit));
            mainPanel.child(fixed);
        }

        if (serverClassementData.scoreReductionRate() > 0) {
            fixed.child(new ScaledTextComponent(
                    "-" + serverClassementData.scoreReductionRate() + "% appliqué aux scores chaque jour à 00h00",
                    0xFFFFB000,
                    0.8f,
                    false
            ));
        }


        return windows;
    }

    private static FlowLayout MenuButton(Screen screen, int size) {
        FlowLayout button = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(size));

        button.mouseDown().subscribe((mx, my, ctx) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            minecraftInstance.setScreen(screen);
            return true;
        });
        return button;
    }
}
