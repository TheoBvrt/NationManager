package ch.swaford.servermanager.classement.classementinterface;

import ch.swaford.servermanager.networktransfer.ClientCache;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.NotNull;

public class PuissanceInterface extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        // Layout vertical centré
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .padding(Insets.of(10));

        FlowLayout classementTemplate = ClassementTemplate.create(
                "puissance_page.png",
                "nation_puissance",
                "Puissance",
                ClientCache.getServerClientData().playerFaction(),
                ""
                );

        FlowLayout mainPanel = classementTemplate.childById(FlowLayout.class, "mainPanel");

        root.child(classementTemplate);
    }
}
