package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Armadura ligera del Brutus (hierro / oro / diamante / netherita): yelmo, tejas dorsales, hombreras, arnes y estandarte.
 * Usa un modelo PROPIO con los mismos huesos que el adulto (CarnotaurusModel.LAYER_ARMOR) y su propio atlas; en cada fotograma
 * se anima con el mismo estado del carnotauro, asi sigue toda su animacion.
 * GENERADA junto a la malla por tools/carnotaurus/armor2.py.
 */
public final class CarnotaurusArmorLayer extends RenderLayer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    private static final String[] TIERS = {"iron", "gold", "diamond", "netherite"};
    private final CarnotaurusModel<CarnotaurusEntity> armorModel;

    public CarnotaurusArmorLayer(RenderLayerParent<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> parent, EntityModelSet models) {
        super(parent);
        this.armorModel = new CarnotaurusModel<>(models.bakeLayer(CarnotaurusModel.LAYER_ARMOR), true);
    }

    private static ResourceLocation texture(int tier) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_armor_" + TIERS[Math.max(1, Math.min(4, tier)) - 1] + ".png");
    }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, CarnotaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        int tier = entity.getArmorTier();
        if (tier <= 0 || !entity.isAdultStage() || entity.isInvisible() || entity.getMorph() > 0) return;
        this.armorModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture(tier)));
        this.armorModel.renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
