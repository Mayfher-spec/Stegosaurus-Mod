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

/** Armadura ligera (hierro / oro / diamante / netherita): piezas que no cubren las crestas ni la membrana; las gemas brillan con el color de su nivel. */
public final class DilophosaurusArmorLayer extends RenderLayer<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> {
    private static final String[] TIERS = {"iron", "gold", "diamond", "netherite"};
    private static final ResourceLocation GLOW = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_armor_glow.png");
    private static final float[][] GEM = {{1.0F, 0.28F, 0.26F}, {0.26F, 1.0F, 0.50F}, {0.34F, 0.90F, 1.0F}, {0.78F, 0.34F, 1.0F}};

    public DilophosaurusArmorLayer(RenderLayerParent<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> parent) { super(parent); }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, DilophosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        int tier = entity.getArmorTier();
        if (tier <= 0 || !entity.isAdultStage() || entity.isInvisible() || entity.getMorph() > 0) return;
        ResourceLocation tex = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_armor_" + TIERS[Math.max(1, Math.min(4, tier)) - 1] + ".png");
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        this.getParentModel().renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        float[] g = GEM[tier - 1];
        this.getParentModel().renderToBuffer(stack, buffers.getBuffer(RenderType.eyes(GLOW)), 15728640, OverlayTexture.NO_OVERLAY, g[0], g[1], g[2], 1.0F);
    }
}
