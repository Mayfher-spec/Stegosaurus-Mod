package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Brillo: en los adultos, ojos y punta de los cuernos (en el cria solo los colores distintos del rojo); con un pulso suave, apagado si esta caido. */
public final class CarnotaurusGlowLayer extends RenderLayer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    private static final ResourceLocation GREEN = glow("green");
    private static final ResourceLocation STEEL = glow("steel");
    private static final ResourceLocation ARENA = glow("arena");
    private static final ResourceLocation SCARLET = glow("scarlet");
    private static ResourceLocation glow(String n) { return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_" + n + "_glow.png"); }

    public CarnotaurusGlowLayer(RenderLayerParent<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> parent) { super(parent); }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, CarnotaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        int v = entity.getVariant();
        if ((v == CarnotaurusEntity.SCARLET && !entity.isAdultStage()) || entity.isInvisible() || entity.getMorph() > 0) return;
        float k = 0.82F + 0.18F * Mth.sin(ageInTicks * 0.12F);
        if (entity.isFallen()) k *= 0.3F;
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(v == CarnotaurusEntity.GREEN ? GREEN : v == CarnotaurusEntity.ARENA ? ARENA : v == CarnotaurusEntity.STEEL ? STEEL : SCARLET));
        this.getParentModel().renderToBuffer(stack, vc, 15728640, OverlayTexture.NO_OVERLAY, k, k, k, 1.0F);
    }
}
