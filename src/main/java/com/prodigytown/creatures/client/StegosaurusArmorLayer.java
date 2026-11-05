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

/** Dibuja la armadura (hierro / oro / diamante / netherita) del estegosaurio; sin brillo, sin GeckoLib. */
public final class StegosaurusArmorLayer extends RenderLayer<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> {
    private final StegosaurusArmorModel armor;
    private ModelPart[] theirs;

    public StegosaurusArmorLayer(RenderLayerParent<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> renderer, EntityModelSet models) {
        super(renderer);
        this.armor = new StegosaurusArmorModel(models.bakeLayer(StegosaurusArmorModel.LAYER_LOCATION));
    }

    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, StegosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        int tier = entity.getArmorTier();
        if (tier <= 0 || entity.isInvisible() || entity.getEvolveTicks() > 0) return;
        if (theirs == null) {
            theirs = new ModelPart[StegosaurusArmorModel.TRACK.length];
            for (int i = 0; i < theirs.length; i++) theirs[i] = StegosaurusArmorModel.walk(this.getParentModel().root(), StegosaurusArmorModel.TRACK[i]);
        }
        armor.sync(theirs);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(StegosaurusArmorModel.texture(tier)));
        armor.renderToBuffer(stack, vc, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
