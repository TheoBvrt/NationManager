package ch.swaford.servermanager.clientinterface;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import oshi.util.tuples.Pair;

import java.util.List;

public class BannerComponent extends BaseComponent {
    private final ItemStack bannerStack;

    public BannerComponent(ItemStack bannerStack) {
        this.bannerStack = bannerStack;
    }

    @Override
    public void draw(OwoUIDrawContext ctx, int mx, int my, float delta, float partialTick) {
        if (!(bannerStack.getItem() instanceof BannerItem bannerItem)) {
            return; // si ce n'est pas une bannière
        }

        var mc = Minecraft.getInstance();
        var buffer = mc.renderBuffers().bufferSource();
        PoseStack poseStack = ctx.pose();

        poseStack.pushPose();
        poseStack.translate(this.x() + this.width() / 2f, this.y() + this.height() / 2f, 200);

        float scaleX = this.width() / 20f;
        float scaleY = this.height() / 20f;
        float scale = Math.min(scaleX, scaleY);

        float zoom = 25;
        poseStack.scale(scale * zoom, -scale * zoom, scale * zoom);

        var bannerEntity = new BannerBlockEntity(BlockPos.ZERO, bannerItem.getBlock().defaultBlockState());
        bannerEntity.fromItem(bannerStack, bannerItem.getColor());

        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(bannerEntity);

        if (renderer != null) {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    bannerStack,
                    ItemDisplayContext.GUI,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    mc.level,
                    0
            );

            var whiteOverlay = new WhiteOverlayBufferSource(buffer, 64); // alpha 128/255
            mc.getItemRenderer().renderStatic(
                    bannerStack,
                    ItemDisplayContext.GUI,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    whiteOverlay,
                    mc.level,
                    0
            );
        }

        poseStack.popPose();
        buffer.endBatch();
    }


    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 200; // par défaut, 40 px large
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return 200; // donc 80 px haut (ratio 1:2)
    }

}
