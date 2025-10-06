package ch.swaford.servermanager.clientinterface;

import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class ConfirmationPopup extends FlowLayout {

    public ConfirmationPopup(String message, Runnable onConfirm, Runnable onCancel, int targetW) {
        super(Sizing.fill(30), Sizing.fill(30), Algorithm.VERTICAL);

        // Fond semi-transparent
        this.surface(Surface.blur(4, 8).and(Surface.flat(0x80101010)));
        this.zIndex(500);
        this.horizontalAlignment(HorizontalAlignment.CENTER);
        this.verticalAlignment(VerticalAlignment.CENTER);

        // Texte
        FlowLayout textPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(70))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);
        textPanel.child(new ScaledTextComponent(message, 0xFFFFFF, (float)(2.1 * targetW / 816), false));
        FlowLayout buttonPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(30))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout confirmationPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(50), Sizing.fill(100))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout cancelPanel = (FlowLayout) Containers.horizontalFlow(Sizing.fill(50), Sizing.fill(100))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        confirmationPanel.child(new ScaledTextComponent("✔ Confirmer", 0xFF00FF00, (float)(2.1 * targetW / 816), false));
        cancelPanel.child(new ScaledTextComponent("✘ Annuler", 0xFFFF0000, (float)(2.1 * targetW / 816), false));
        buttonPanel.child(confirmationPanel);
        buttonPanel.child(cancelPanel);

        confirmationPanel.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (onConfirm != null) onConfirm.run();
            if (this.parent() != null) this.parent().removeChild(this); // ferme la popup
            return true;
        });

        cancelPanel.mouseDown().subscribe((mx, my, btn) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            if (onCancel != null) onCancel.run();
            if (this.parent() != null) this.parent().removeChild(this); // ferme la popup
            return true;
        });

        this.child(textPanel);
        this.child(buttonPanel);
    }

    @Override
    public void layout(Size space) {
        super.layout(space);

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        this.positioning(Positioning.absolute(
                (screenW / 2) - (this.width() / 2),
                (screenH / 2) - (this.height() / 2)
        ));
    }
}
