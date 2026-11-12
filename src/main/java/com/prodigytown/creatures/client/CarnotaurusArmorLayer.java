package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Armadura del Brutus (hierro / oro / diamante / netherita): la misma malla del carnotauro con una textura transparente que solo trae las
 * piezas de armadura (yelmo, gola, peto, laminas, grebas, cola); sigue toda su animacion. Las gemas brillan (textura comun tenida por nivel).
 * GENERADA junto a la malla por tools/carnotaurus/export.py.
 */
public final class CarnotaurusArmorLayer extends RenderLayer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    private static final String[] TIERS = {"iron", "gold", "diamond", "netherite"};
    private static final ResourceLocation GLOW = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_armor_glow.png");
    /** Color de las gemas por nivel (se multiplica por la textura de brillo, que es blanca). */
    private static final float[][] GEM = {{1.0F, 0.28F, 0.26F}, {0.26F, 1.0F, 0.50F}, {0.34F, 0.90F, 1.0F}, {0.78F, 0.34F, 1.0F}};

    public CarnotaurusArmorLayer(RenderLayerParent<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> parent) { super(parent); }

    private static ResourceLocation texture(int tier) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_armor_" + TIERS[Math.max(1, Math.min(4, tier)) - 1] + ".png");
    }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, CarnotaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        int tier = entity.getArmorTier();
        if (tier <= 0 || !entity.isAdultStage() || entity.isInvisible()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture(tier)));
        this.getParentModel().renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        float[] g = GEM[tier - 1];
        this.getParentModel().renderToBuffer(stack, buffers.getBuffer(RenderType.eyes(GLOW)), 15728640, OverlayTexture.NO_OVERLAY, g[0], g[1], g[2], 1.0F);
    }
}
