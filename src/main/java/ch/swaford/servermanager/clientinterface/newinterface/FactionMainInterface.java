package ch.swaford.servermanager.clientinterface.newinterface;

import ch.swaford.servermanager.classement.classementinterface.ClassementTemplate;
import ch.swaford.servermanager.clientinterface.BannerComponent;
import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.ClientCache;
import ch.swaford.servermanager.networktransfer.RequestClassementData;
import ch.swaford.servermanager.networktransfer.ServerFactionData;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.TagParser;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Flow;

public class FactionMainInterface extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        // Layout vertical centré
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {

        String factionDescription = "default";
        int totalChunk = 0;
        int factionRank = 0;
        int balance = 0;
        int power = 0;
        String factionName = "server";
        String bannerTag = "";
        String ownerName = "";
        int population = 0;

        ServerFactionData serverFactionData = ClientCache.getServerFactionData();
        if(serverFactionData != null) {
            factionName = serverFactionData.name();
            factionDescription = serverFactionData.description();
            factionRank = serverFactionData.rank();
            totalChunk = serverFactionData.totalChunk();
            balance = serverFactionData.balance();
            power = serverFactionData.power();
            bannerTag = serverFactionData.bannerTag();
            ownerName = serverFactionData.playerList().get(serverFactionData.ownerUuid());
            population = serverFactionData.playerList().size();
        }

        int rouge = 0x88FF0000;
        int vert = 0x8800FF00;

        root
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(10));

        FlowLayout factionTemplate = FactionInterfaceTemplate.create(
                "faction_main_menu.png"
        );

        FlowLayout mainPanel = factionTemplate.childById(FlowLayout.class, "mainPanel");

        FlowLayout topPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(39))
                .padding(Insets.of(UITools.logicalH(0.05f), 0, 0, 0));

        FlowLayout rightTopPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(20), Sizing.fill(100));

        FlowLayout leftTopPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(80), Sizing.fill(100));

        ItemStack bannerStack;
        if (bannerTag == null || bannerTag.isEmpty()){
            bannerStack = new ItemStack(Items.WHITE_BANNER);
        } else {
            try {
                bannerStack = ItemStack.parseOptional(Minecraft.getInstance().player.level().registryAccess(), TagParser.parseTag(bannerTag));
            } catch (CommandSyntaxException e) {
                bannerStack = new ItemStack(Items.WHITE_BANNER);
            }
        }
        BannerComponent bannerRenderer = (BannerComponent) new BannerComponent(bannerStack)
                .sizing(Sizing.fill(100), Sizing.fill(100));

        rightTopPanel.child(bannerRenderer);

        FlowLayout titleContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(30))
                .verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout descriptionContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(50))
                .gap(UITools.logicalH(0.01f))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.LEFT);
        FlowLayout statsContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(20))
                .gap(UITools.logicalW(0.01f))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.LEFT);


        titleContainer.child(new ScaledTextComponent(factionName, 0xFFFFFFFF, 1.8f, true));

        List<String> list =  UITools.TextFormatter(factionDescription, 49);
        if (list.size() == 1) {
            descriptionContainer.child(new ScaledTextComponent("", 0xFFDDD9D9, 1.0f, false));
            descriptionContainer.child(new ScaledTextComponent(list.getFirst(), 0xFFDDD9D9, 1.0f, false));
            descriptionContainer.child(new ScaledTextComponent("", 0xFFDDD9D9, 1.0f, false));
        }

        if (list.size() == 2) {
            descriptionContainer.child(new ScaledTextComponent(list.getFirst(), 0xFFDDD9D9, 1.0f, false));
            descriptionContainer.child(new ScaledTextComponent(list.get(1), 0xFFDDD9D9, 1.0f, false));
            descriptionContainer.child(new ScaledTextComponent("", 0xFFDDD9D9, 1.0f, false));
        }

        if (list.size() == 3) {
            descriptionContainer.child(new ScaledTextComponent(list.getFirst(), 0xFFDDD9D9, 1.0f, false));
            descriptionContainer.child(new ScaledTextComponent(list.get(1), 0xFFDDD9D9, 1.0f, false));
            descriptionContainer.child(new ScaledTextComponent(list.get(2), 0xFFDDD9D9, 1.0f, false));
        }

        FlowLayout powerRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(25), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.LEFT);
        powerRow.child(new ScaledTextComponent("Puissance : ", 0xFFFFFFFF, 0.9f, false));
        powerRow.child(new ScaledTextComponent(formatShortFR(power), 0xFFFFC700, 0.9f, false));
        statsContainer.child(powerRow);

        FlowLayout claimRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(22), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);
        claimRow.child(new ScaledTextComponent("Territoires : ", 0xFFFFFFFF, 0.9f, false));
        claimRow.child(new ScaledTextComponent(formatShortFR(totalChunk), 0xFFFFC700, 0.9f, false));
        statsContainer.child(claimRow);

        FlowLayout populationRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(20), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);
        populationRow.child(new ScaledTextComponent("Population : ", 0xFFFFFFFF, 0.9f, false));
        populationRow.child(new ScaledTextComponent("" + population, 0xFFFFC700, 0.9f, false));
        statsContainer.child(populationRow);

        FlowLayout balanceRow = (FlowLayout) Containers.horizontalFlow(Sizing.fill(25), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.RIGHT);
        balanceRow.child(new ScaledTextComponent("Argent : ", 0xFFFFFFFF, 0.9f, false));
        balanceRow.child(new ScaledTextComponent(formatShortFR(balance) + "€", 0xFFFFC700, 0.9f, false));
        statsContainer.child(balanceRow);

        topPanel.child(leftTopPanel);
            leftTopPanel.child(titleContainer);
            leftTopPanel.child(descriptionContainer);
            leftTopPanel.child(statsContainer);

        topPanel.child(rightTopPanel);

        FlowLayout bottomPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(61))
                .padding(Insets.of(UITools.logicalH(0.07), UITools.logicalH(0.07), 0, UITools.logicalW(0.025)));

        FlowLayout leftBottomPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(29), Sizing.fill(100))
                .gap(UITools.logicalH(0.03));

        FlowLayout rankPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(48))
                .padding(Insets.of(UITools.logicalH(0.02), 0, UITools.logicalW(0.1), 0))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        rankPanel.child(new ScaledTextComponent("#" + factionRank, 0xFFFFFFFF, 3f, false));

        FlowLayout ownerPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(52))
                .gap(UITools.logicalH(0.019))
                .padding(Insets.of(UITools.logicalH(0.015), 0, 0, 0))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        ownerPanel.child(new ScaledTextComponent(ownerName, 0xFFFFFFFF, 1.2f, false));
        ownerPanel.child(new ScaledTextComponent("Créé il y a 27j", 0xFFFFC700, 0.9f, false));


        leftBottomPanel.child(rankPanel);
        leftBottomPanel.child(ownerPanel);

        FlowLayout rightBottomPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(69), Sizing.fill(100))
                .padding(Insets.of(0, UITools.logicalH(0.03), 0, 0))
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.left(UITools.logicalW(0.045)));

        FlowLayout openClassementButton = (FlowLayout) Containers.verticalFlow(Sizing.fill(85), Sizing.fill(21));

        openClassementButton.mouseDown().subscribe((mx, my, btn) -> {
            var minecraftInstance = Minecraft.getInstance();
            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestClassementData());
            return true;
        });

        rightBottomPanel.child(openClassementButton);

        mainPanel.child(topPanel);
        mainPanel.child(bottomPanel);
            bottomPanel.child(leftBottomPanel);
            bottomPanel.child(rightBottomPanel);

        root.child(factionTemplate);
    }

    private static FlowLayout statContainer(int fill) {
        FlowLayout container = (FlowLayout)  Containers.horizontalFlow(Sizing.fill(fill), Sizing.fill(100))
                .padding(Insets.bottom(5))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.LEFT);
        return container;
    }

    public static String formatShortFR(long value) {
        if (value >= 1_000_000_000) {
            long billions = value / 1_000_000_000; // division entière
            return billions + "G";
        } else if (value >= 1_000_000) {
            long millions = value / 1_000_000;
            return millions + "M";
        } else if (value >= 1000) {
            long thousands = value / 1000;
            return thousands + "k";
        } else {
            return String.valueOf(value);
        }
    }
}