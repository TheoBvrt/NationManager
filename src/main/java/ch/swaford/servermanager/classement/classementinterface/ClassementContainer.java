package ch.swaford.servermanager.classement.classementinterface;

import ch.swaford.servermanager.clientinterface.ScaledTextComponent;
import ch.swaford.servermanager.clientinterface.UITools;
import ch.swaford.servermanager.networktransfer.RequestFactionData;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jline.utils.Colors;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.Flow;

public class ClassementContainer {
    public static FlowLayout createClassementContainer(String factionName, int rank, int score, int memberCount, String scoreName, boolean fixed, String unit) {
        int backgroundColor = fixed ? 0xFF228246 : 0xFF2E3349;
        int color = 0xFFFFFFFF;
        if (!fixed) {
            color = 0xFFBFBFBF;
            if (rank == 1)
                color = 0xFFFFD700;
            else if (rank == 2)
                color = 0xFFFFFFFF;
            else if (rank == 3)
                color = 0xFFCD7F32;
        }

        FlowLayout container = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(UITools.logicalH(0.16)))
                .surface(Surface.flat(backgroundColor));

        container.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (mc.player != null && mc.getConnection() != null) {
                mc.getConnection().send(new RequestFactionData(factionName, 0));
            }
            return true;
        });

        FlowLayout rankContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(20), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.top(UITools.logicalH(0.02)));

        FlowLayout nationContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(80), Sizing.fill(100));
        FlowLayout nationNameContainer = (FlowLayout) Containers.verticalFlow(Sizing.fill(100), Sizing.fill(50))
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        FlowLayout nationStatsContainer = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(50))
                .padding(Insets.top(UITools.logicalH(0.02)));

        FlowLayout nationStatScore = (FlowLayout) Containers.horizontalFlow(Sizing.fill(60), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.TOP)
                .horizontalAlignment(HorizontalAlignment.LEFT);

        FlowLayout nationMemberNumber = (FlowLayout) Containers.horizontalFlow(Sizing.fill(40), Sizing.fill(100))
                .verticalAlignment(VerticalAlignment.TOP)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        rankContainer.child(new ScaledTextComponent("#" + rank, color,2.5f, false));
        nationNameContainer.child(new ScaledTextComponent(factionName, color,1.1f, true));
        nationStatScore.child(new ScaledTextComponent(scoreName + " →  " + NumberFormat.getInstance(Locale.FRENCH).format(score) + unit, 0xFFFFFFFF, 0.9f, true));
        nationMemberNumber.child(new ScaledTextComponent("Membre →  " + memberCount, 0xFFFFFFFF, 0.9f, true));

        container.child(rankContainer);
        container.child(nationContainer);
            nationContainer.child(nationNameContainer);
            nationContainer.child(nationStatsContainer );
                nationStatsContainer.child(nationStatScore);
                nationStatsContainer.child(nationMemberNumber);
        return (container);
    }
}
