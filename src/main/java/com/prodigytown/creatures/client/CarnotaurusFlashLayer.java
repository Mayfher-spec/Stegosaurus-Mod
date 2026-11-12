package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CarnotaurusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Destello al terminar la evolucion o al volver a ser cria: arranca en blanco puro y se apaga pasando por el color de la variante
 * (rojo, verde, acero o arena), con un doble latido al principio. Al encoger es mas corto y de un blanco azulado.
 */
public final class CarnotaurusFlashLayer extends RenderLayer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    /** Tinte de apagado por variante (rojo, verde, acero, arena). */
    private static final float[][] TINT = {{1.00F, 0.42F, 0.28F}, {0.42F, 1.00F, 0.52F}, {0.58F, 0.78F, 1.00F}, {1.00F, 0.84F, 0.46F}};

    public CarnotaurusFlashLayer(RenderLayerParent<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> parent) { super(parent); }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, CarnotaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        float f = entity.flashFade - partialTick;
        if (f <= 0.0F || entity.getMorph() > 0 || entity.isInvisible()) return;
        boolean shrink = entity.flashKind == 1;
        float life = Mth.clamp(f / 24.0F, 0.0F, 1.0F);          // 1 al empezar .. 0 al terminar
        float alpha = shrink ? life * life : life;
        alpha *= 0.85F + 0.15F * Mth.cos((1.0F - life) * 22.0F); // doble latido al principio
        float[] t = TINT[Math.max(0, Math.min(3, entity.getVariant()))];
        float k = Mth.clamp(1.0F - life, 0.0F, 1.0F);           // 0 = blanco puro, 1 = color de la variante
        float r, g, b;
        if (shrink) { r = Mth.lerp(k, 1.0F, 0.70F); g = Mth.lerp(k, 1.0F, 0.88F); b = 1.0F; }
        else { r = Mth.lerp(k, 1.0F, t[0]); g = Mth.lerp(k, 1.0F, t[1]); b = Mth.lerp(k, 1.0F, t[2]); }
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(StegosaurusRenderer.WHITE));
        this.getParentModel().renderToBuffer(stack, vc, 15728640, OverlayTexture.NO_OVERLAY, r, g, b, Mth.clamp(alpha, 0.0F, 1.0F));
    }
}
