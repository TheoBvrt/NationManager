//package ch.swaford.servermanager.clientinterface;
//
//import ch.swaford.servermanager.networktransfer.ClientCache;
//import ch.swaford.servermanager.networktransfer.ServerFactionData;
//import io.wispforest.owo.ui.base.BaseOwoScreen;
//import io.wispforest.owo.ui.component.Components;
//import io.wispforest.owo.ui.container.Containers;
//import io.wispforest.owo.ui.container.FlowLayout;
//import io.wispforest.owo.ui.core.*;
//import io.wispforest.owo.ui.core.Insets;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.resources.sounds.SimpleSoundInstance;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.sounds.SoundEvents;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Flow;
//
//
//public class FactionParametersMenu extends BaseOwoScreen<FlowLayout> {
//
//    @Override
//    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
//        // Layout vertical centré
//        return OwoUIAdapter.create(this, Containers::verticalFlow);
//    }
//
//    @Override
//    protected void build(FlowLayout root) {
//        List<String> playersUuid = new ArrayList<>();
//        ServerFactionData serverFactionData = ClientCache.getServerFactionData();
//
//        assert Minecraft.getInstance().player != null;
//        String playerUuid = Minecraft.getInstance().player.getStringUUID();
//        if(serverFactionData != null) {
//            playersUuid.add(serverFactionData.ownerUuid());
//            playersUuid.addAll(serverFactionData.officers());
//            playersUuid.addAll(serverFactionData.members());
//        }
//
//        int permissionLevel = 0;
//        assert serverFactionData != null;
//        if (serverFactionData.ownerUuid().equals(playerUuid))
//            permissionLevel = 2;
//        else if (serverFactionData.officers().contains(playerUuid))
//            permissionLevel = 1;
//
//        root
//                .horizontalAlignment(HorizontalAlignment.CENTER)
//                .verticalAlignment(VerticalAlignment.CENTER)
//                .padding(Insets.of(10));
//
//
//        int imgW = 816;
//        int imgH = 512;
//        float ratio = (float) imgH / imgW;
//        int targetW = (int) (this.width * 0.6);
//        int targetH = (int) (this.height * ratio);
//
//        //Load la template du menu de faction avec l'image de fond
//        FlowLayout factionMenuBase = FactionTemplate.create(this.width, this.height, "faction_parameters_page.png", false);
//
//        FlowLayout mainDisplayPanel = factionMenuBase.childById(FlowLayout.class, "mainDisplayPanel");
//        FlowLayout mainPanel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
//                .horizontalAlignment(HorizontalAlignment.CENTER)
//                .verticalAlignment(VerticalAlignment.TOP)
//                .margins(Insets.of((int)(targetH * 0.05), (int)(targetH * 0.05), (int)(targetW * 0.1), (int)(targetW * 0.1)));
//
//        FlowLayout leavePanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(20))
//                .horizontalAlignment(HorizontalAlignment.CENTER)
//                .verticalAlignment(VerticalAlignment.CENTER)
//                .surface(Surface.flat(0xFF181E36));
//
//        leavePanel.child(new ScaledTextComponent("Quitter la nation", 0xFFFFFF, (float)(2.5 * targetW / 812), false));
//
//        FlowLayout deletePanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(20))
//                .horizontalAlignment(HorizontalAlignment.CENTER)
//                .verticalAlignment(VerticalAlignment.CENTER)
//                .surface(Surface.flat(0xFF181E36));
//
//        deletePanel.child(new ScaledTextComponent("Supprimer la nation", 0xFFFFFF, (float)(2.5 * targetW / 812), false));
//
//
//        leavePanel.mouseDown().subscribe((mx, my, btn) -> {
//            var minecraftInstance = Minecraft.getInstance();
//            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
//
//
//            ConfirmationPopup popup = new ConfirmationPopup(
//                    "Quitter la nation ?",
//                    () -> {
//                        minecraftInstance.player.connection.send(new ServerboundChatCommandPacket("nation leave"));
//                    },
//                    () -> {},
//                    targetW
//            );
//            root.child(popup);
//            return true;
//        });
//
//        deletePanel.mouseDown().subscribe((mx, my, btn) -> {
//            var minecraftInstance = Minecraft.getInstance();
//            minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
//
//
//            ConfirmationPopup popup = new ConfirmationPopup(
//                    "Supprimer la nation ?",
//                    () -> {
//                        minecraftInstance.player.connection.send(new ServerboundChatCommandPacket("nation delete " + serverFactionData.name()));
//                    },
//                    () -> {},
//                    targetW
//            );
//            root.child(popup);
//            return true;
//        });
//
//        mainPanel.child(leavePanel);
//
//        mainDisplayPanel.child(mainPanel);
//        root.child(factionMenuBase);
//    }
//}
