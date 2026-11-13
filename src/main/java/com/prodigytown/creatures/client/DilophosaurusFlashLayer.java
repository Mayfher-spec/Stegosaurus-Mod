package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.DilophosaurusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Destello al terminar la evolucion o al volver a ser cria: blanco puro que se apaga pasando por el color de la variante. */
public final class DilophosaurusFlashLayer extends RenderLayer<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> {
    /** Natural, azul electrico, magma, desierto, esmeralda. */
    private static final float[][] TINT = {{0.72F, 1.00F, 0.42F}, {0.42F, 0.80F, 1.00F}, {1.00F, 0.42F, 0.22F}, {1.00F, 0.84F, 0.46F}, {0.42F, 1.00F, 0.72F}};

    public DilophosaurusFlashLayer(RenderLayerParent<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> parent) { super(parent); }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, DilophosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        float f = entity.flashFade - partialTick;
        if (f <= 0.0F || entity.getMorph() > 0 || entity.isInvisible()) return;
        boolean shrink = entity.flashKind == 1;
        float life = Mth.clamp(f / 24.0F, 0.0F, 1.0F);
        float alpha = shrink ? life * life : life;
        alpha *= 0.85F + 0.15F * Mth.cos((1.0F - life) * 22.0F);
        float[] t = TINT[Math.max(0, Math.min(TINT.length - 1, entity.getVariant()))];
        float k = Mth.clamp(1.0F - life, 0.0F, 1.0F);
        float r, g, b;
        if (shrink) { r = Mth.lerp(k, 1.0F, 0.70F); g = Mth.lerp(k, 1.0F, 0.88F); b = 1.0F; }
        else { r = Mth.lerp(k, 1.0F, t[0]); g = Mth.lerp(k, 1.0F, t[1]); b = Mth.lerp(k, 1.0F, t[2]); }
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(StegosaurusRenderer.WHITE));
        this.getParentModel().renderToBuffer(stack, vc, 15728640, OverlayTexture.NO_OVERLAY, r, g, b, Mth.clamp(alpha, 0.0F, 1.0F));
    }
}
