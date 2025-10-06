package ch.swaford.servermanager.clientinterface;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class WhiteOverlayBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final int overlayAlpha; // 0–255

    public WhiteOverlayBufferSource(MultiBufferSource delegate, int alpha) {
        this.delegate = delegate;
        this.overlayAlpha = alpha;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        VertexConsumer original = delegate.getBuffer(type);

        return new VertexConsumer() {
            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                return original.addVertex(x, y, z);
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                // Conversion RGB -> HSB
                float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);

                // augmenter la saturation
                float saturationBoost = 1.3f; // 30% plus saturé
                hsb[1] = Math.min(1.0f, hsb[1] * saturationBoost);

                // optionnel : booster la luminosité aussi
                // hsb[2] = Math.min(1.0f, hsb[2] * 1.1f);

                // Conversion HSB -> RGB
                int newRgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);

                int nr = (newRgb >> 16) & 0xFF;
                int ng = (newRgb >> 8) & 0xFF;
                int nb = newRgb & 0xFF;

                return original.setColor(nr, ng, nb, a);
            }

            @Override public VertexConsumer setUv(float u, float v) { return original.setUv(u, v); }
            @Override public VertexConsumer setUv1(int u, int v) { return original.setUv1(u, v); }
            @Override public VertexConsumer setUv2(int u, int v) { return original.setUv2(u, v); }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return original.setNormal(x, y, z); }
        };
    }
}