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

/** Silla: adulto = la misma malla con una textura transparente que solo trae la silla; cria = modelo propio simplificado (LAYER_SADDLE_CHIBI). */
public final class CarnotaurusSaddleLayer extends RenderLayer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_saddle.png");

    private static final ResourceLocation TEXTURE_CHIBI = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_saddle_chibi.png");
    private final CarnotaurusModel<CarnotaurusEntity> chibiModel;

    public CarnotaurusSaddleLayer(RenderLayerParent<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> parent, EntityModelSet models) {
        super(parent);
        this.chibiModel = new CarnotaurusModel<>(models.bakeLayer(CarnotaurusModel.LAYER_SADDLE_CHIBI), false);
    }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, CarnotaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        if (!entity.isSaddled() || entity.isInvisible() || entity.getMorph() > 0) return;
        boolean adult = entity.isAdultStage();
        CarnotaurusModel<CarnotaurusEntity> m = adult ? this.getParentModel() : this.chibiModel;
        if (!adult) m.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(adult ? TEXTURE : TEXTURE_CHIBI));
        m.renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
