package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Silla ligera: la misma malla con una textura transparente que solo trae la silla. */
public final class DilophosaurusSaddleLayer extends RenderLayer<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_saddle.png");

    public DilophosaurusSaddleLayer(RenderLayerParent<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> parent) { super(parent); }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, DilophosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        if (!entity.isSaddled() || !entity.isAdultStage() || entity.isInvisible() || entity.getMorph() > 0) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.getParentModel().renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
