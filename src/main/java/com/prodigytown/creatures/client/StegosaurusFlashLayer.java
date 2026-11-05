package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Destello blanco que se desvanece sobre la textura final al terminar la evolucion. */
public final class StegosaurusFlashLayer extends RenderLayer<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> {
    public StegosaurusFlashLayer(RenderLayerParent<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> renderer) {
        super(renderer);
    }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, StegosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        float f = entity.flashFade - partialTick;
        if (f <= 0.0F || entity.getEvolveTicks() > 0) return;
        float alpha = Mth.clamp(f / 24.0F, 0.0F, 1.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(StegosaurusRenderer.WHITE));
        this.getParentModel().renderToBuffer(stack, vc, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
    }
}
