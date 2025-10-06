package ch.swaford.servermanager.shop;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class test extends BaseComponent {
    private final String text;
    private final int color;
    private float scale;
    private final String fontFile;

    public test(String text, int color, float scale, String fontFile) {
        this.text = text;
        this.color = color;
        this.scale = scale;
        this.fontFile = fontFile;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        int baseWidth = Minecraft.getInstance().font.width(text);
        return Math.round(baseWidth * scale);
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        int baseHeight = 9; // hauteur de la font vanilla
        return Math.round(baseHeight * scale);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        ResourceLocation font = ResourceLocation.parse("servermanager:" + fontFile);
        PoseStack pose = context.pose();
        pose.pushPose();

        pose.scale(scale, scale, 1);

        context.drawString(
                Minecraft.getInstance().font,
                Component.literal(text).withStyle(style -> style
                        .withFont(font)
                        .withBold(true)
                ),
                (int) (this.x / scale),
                (int) (this.y / scale),
                color,
                false
        );

        pose.popPose();
    }
}