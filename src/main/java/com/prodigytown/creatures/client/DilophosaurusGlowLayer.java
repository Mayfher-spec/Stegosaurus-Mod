package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Brillo propio de cada variante (grietas, venas, ojos, membrana); late mas fuerte durante el frenesi y se apaga si esta caido o con los ojos cerrados. */
public final class DilophosaurusGlowLayer extends RenderLayer<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> {
    private static final ResourceLocation[] GLOW = new ResourceLocation[DilophosaurusRenderer.VARIANT_IDS.length];
    private static final ResourceLocation[] GLOW_CLOSED = new ResourceLocation[DilophosaurusRenderer.VARIANT_IDS.length];
    static {
        for (int i = 0; i < GLOW.length; i++) {
            GLOW[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_" + DilophosaurusRenderer.VARIANT_IDS[i] + "_glow.png");
            GLOW_CLOSED[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_" + DilophosaurusRenderer.VARIANT_IDS[i] + "_closed_glow.png");
        }
    }

    public DilophosaurusGlowLayer(RenderLayerParent<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> parent) { super(parent); }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, DilophosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || entity.getMorph() > 0) return;
        float k = 0.80F + 0.20F * Mth.sin(ageInTicks * 0.12F);
        if (entity.isFrenzied()) k = 1.0F + 0.25F * Mth.sin(ageInTicks * 0.6F);
        if (entity.isFallen()) k *= 0.3F;
        int v = Math.max(0, Math.min(GLOW.length - 1, entity.getVariant()));
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(entity.eyesClosed() ? GLOW_CLOSED[v] : GLOW[v]));
        this.getParentModel().renderToBuffer(stack, vc, 15728640, OverlayTexture.NO_OVERLAY, k, k, k, 1.0F);
    }
}
