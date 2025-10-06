package ch.swaford.servermanager.clientinterface.newinterface;

import ch.swaford.servermanager.clientinterface.BannerComponent;
import ch.swaford.servermanager.clientinterface.MemberComponent;
import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.ClientCache;
import ch.swaford.servermanager.networktransfer.RequestClassementData;
import ch.swaford.servermanager.networktransfer.ServerFactionData;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.TagParser;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Flow;

public class FactionMembersInterface extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        // Layout vertical centré
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        List<String> playersUuid = new ArrayList<>();
        Map<String, String> playersList = new HashMap<>();
        ServerFactionData serverFactionData = ClientCache.getServerFactionData();

        assert Minecraft.getInstance().player != null;
        String playerUuid = Minecraft.getInstance().player.getStringUUID();
        if(serverFactionData != null) {
            playersUuid.add(serverFactionData.ownerUuid());
            playersUuid.addAll(serverFactionData.officers());
            playersUuid.addAll(serverFactionData.members());
            playersList = serverFactionData.playerList();
        }


        root
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(10));

        FlowLayout factionTemplate = FactionInterfaceTemplate.create(
                "members_page.png"
        );

        int rouge = 0x88FF0000;
        int vert = 0x8800FF00;

        FlowLayout mainPanel = factionTemplate.childById(FlowLayout.class, "mainPanel");
        mainPanel.padding(Insets.of(UITools.logicalH(0.25), UITools.logicalH(0.1), UITools.logicalW(0.1), UITools.logicalW(0.1)));

        FlowLayout panel = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout content = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        int permissionLevel = 0;
        assert serverFactionData != null;
        if (serverFactionData.ownerUuid().equals(playerUuid))
            permissionLevel = 2;
        else if (serverFactionData.officers().contains(playerUuid))
            permissionLevel = 1;
        for (String uuid : playersUuid) {
            String playerName = playersList.get(uuid);
            content.child(MemberComponent.newMember(uuid, playerName, permissionLevel, serverFactionData.officers(), serverFactionData.ownerUuid()));
        }
        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), content);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x00000000)));
        scroll.scrollbarThiccness(6);

        mainPanel.child(panel);
        panel.child(scroll);

        root.child(factionTemplate);
    }
}