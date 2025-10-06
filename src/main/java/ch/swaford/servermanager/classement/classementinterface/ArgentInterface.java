package ch.swaford.servermanager.classement.classementinterface;

import ch.swaford.servermanager.networktransfer.ClientCache;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.VerticalAlignment;
import org.jetbrains.annotations.NotNull;

public class ArgentInterface extends BaseOwoScreen<FlowLayout> {

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
                "argent_page.png",
                "nation_balance_score",
                "Banque",
                ClientCache.getServerClientData().playerFaction(),
                "€"
                );

        FlowLayout mainPanel = classementTemplate.childById(FlowLayout.class, "mainPanel");

        root.child(classementTemplate);
    }
}
