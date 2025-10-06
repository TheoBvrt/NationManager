package ch.swaford.servermanager.clientinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ScaledTextComponent extends BaseComponent {
    private String text;
    private final int color;
    private float scale;
    private boolean bold;

    public ScaledTextComponent(String text, int color, float scale, boolean bold) {
        this.text = text;
        this.color = color;
        this.scale = scale;
        this.bold = bold;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        int baseWidth = Minecraft.getInstance().font.width(text);
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return Math.round((float) baseWidth * scale / (float) guiScale * 2);
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        int baseHeight = Minecraft.getInstance().font.lineHeight;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return Math.round((float) baseHeight * scale / (float) guiScale * 2);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        PoseStack pose = context.pose();
        pose.pushPose();

        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        // repère physique : on annule le guiScale global
        pose.scale(1f / (float) guiScale, 1f / (float) guiScale, 1);

        // translate en pixels physiques
        pose.translate(this.x * guiScale, this.y * guiScale, 0);

        // applique ton scale custom (toujours en pixels physiques)
        pose.scale(scale * 2, scale * 2, 1);

        if (text != null) {
            context.drawString(
                    Minecraft.getInstance().font,
                    Component.literal(text).withStyle(style -> style.withBold(bold)),
                    0, 0,
                    color
            );
        }
        pose.popPose();
    }

    public void setText(String text) {
        this.text = text;
    }
}
