package ch.swaford.servermanager.clientinterface;

import ch.swaford.servermanager.networktransfer.RequestFactionData;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.NinePatchTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.sounds.SoundEvents;
import org.w3c.dom.Text;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow;

public class MemberComponent {
    public static FlowLayout newMember(String playerUuid, String playerName, int permissionLevel, List<String> officers, String owner) {
        FlowLayout panel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(80), Sizing.fixed(UITools.logicalH(0.1f)))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.flat(0xFF181E36))
                .margins(Insets.bottom(UITools.logicalH(0.01f)));

        int size = (permissionLevel >= 1) ? 70 : 100;
        FlowLayout commonPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(size), Sizing.fill(100))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        commonPanel.child(new ScaledTextComponent(playerName, 0xFFFFFF, 1.5f, false));

        panel.child(commonPanel);
        int panelSize = (permissionLevel == 2) ? 50 : 100;
        if (permissionLevel >= 1 && !owner.equals(playerUuid)) {
            if (permissionLevel == 2 || (permissionLevel == 1 && !officers.contains(playerUuid)))
            {
                FlowLayout opPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(30), Sizing.fill(100))
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .verticalAlignment(VerticalAlignment.CENTER);
                FlowLayout removePlayerPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(panelSize), Sizing.fill(100))
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .surface(Surface.flat(0xFF181E36));
                removePlayerPanel.child(new ScaledTextComponent("✘", 0xffcb3d, 1.5f, false));
                removePlayerPanel.mouseDown().subscribe((mx, my, btn) -> {
                    var minecraftInstance = Minecraft.getInstance();
                    minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    assert minecraftInstance.player != null;
                    minecraftInstance.player.connection.send(new ServerboundChatCommandPacket("nation removeplayer " + playerName));
                    Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestFactionData("server", 1));
                    return true;
                });
                removePlayerPanel.tooltip(Component.literal("Exclure"));
                opPanel.child(removePlayerPanel);
                if (permissionLevel == 2) {
                    int color = 0xFF00FF00;
                    String actionButton = "↑";
                    String toolTipText = "Promouvoir";
                    Boolean demotePlayer;
                    FlowLayout promoteDemotePanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(50), Sizing.fill(100))
                            .horizontalAlignment(HorizontalAlignment.CENTER)
                            .verticalAlignment(VerticalAlignment.CENTER)
                            .surface(Surface.flat(0xFF181E36));
                    if (officers.contains(playerUuid)) {
                        demotePlayer = true;
                        actionButton = "↓";
                        toolTipText = "Rétrogarder";
                        color = 0xFFFF0000;
                    } else {
                        demotePlayer = false;
                    }
                    promoteDemotePanel.child(new ScaledTextComponent(actionButton, color, 1.5f, false));
                    promoteDemotePanel.mouseDown().subscribe((mx, my, btn) -> {
                        var minecraftInstance = Minecraft.getInstance();
                        minecraftInstance.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                        if (!demotePlayer) {
                            assert minecraftInstance.player != null;
                            minecraftInstance.player.connection.send(new ServerboundChatCommandPacket("nation promote " + playerName));
                        } else {
                            assert minecraftInstance.player != null;
                            minecraftInstance.player.connection.send(new ServerboundChatCommandPacket("nation demote " + playerName));
                        }
                        Objects.requireNonNull(minecraftInstance.getConnection()).send(new RequestFactionData("server", 1));
                        return true;
                    });
                    promoteDemotePanel.tooltip(Component.literal(toolTipText));
                    opPanel.child(promoteDemotePanel);
                }
                panel.child(opPanel);
            }
        }

        return panel;
    }
}
