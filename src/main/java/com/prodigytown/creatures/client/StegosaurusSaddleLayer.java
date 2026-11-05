package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Dibuja la silla sobre el lomo cuando el estegosaurio la lleva puesta (sin brillo, sin GeckoLib). */
public final class StegosaurusSaddleLayer extends RenderLayer<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> {
    private final StegosaurusSaddleModel saddle;
    private ModelPart[] theirs;

    public StegosaurusSaddleLayer(RenderLayerParent<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> renderer, EntityModelSet models) {
        super(renderer);
        this.saddle = new StegosaurusSaddleModel(models.bakeLayer(StegosaurusSaddleModel.LAYER_LOCATION));
    }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, StegosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        if (!entity.isSaddled() || entity.isInvisible()) return;
        // el equipo copia la pose de cada hueso (cuello, cola, patas...) para moverse con el estegosaurio
        if (theirs == null) {
            theirs = new ModelPart[StegosaurusSaddleModel.TRACK.length];
            for (int i = 0; i < theirs.length; i++) theirs[i] = StegosaurusSaddleModel.walk(this.getParentModel().root(), StegosaurusSaddleModel.TRACK[i]);
        }
        saddle.sync(theirs);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(StegosaurusSaddleModel.TEXTURE));
        saddle.renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
