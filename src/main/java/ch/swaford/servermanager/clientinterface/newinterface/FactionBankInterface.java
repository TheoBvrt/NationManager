package ch.swaford.servermanager.clientinterface.newinterface;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.VerticalAlignment;
import org.jetbrains.annotations.NotNull;

public class FactionBankInterface extends BaseOwoScreen<FlowLayout> {

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

        FlowLayout factionTemplate = FactionInterfaceTemplate.create(
                "bank_page.png"
        );

        FlowLayout mainPanel = factionTemplate.childById(FlowLayout.class, "mainPanel");

        root.child(factionTemplate);
    }
}