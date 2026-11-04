package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Emissive textures drawn at full brightness with alpha cutout; no GeckoLib. */
public final class StegosaurusGlowLayer extends RenderLayer<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> {
    private static final ResourceLocation[] GLOWS = {
        glow("cybernetic"), glow("golden"), glow("magma"), glow("glacial")
    };
    private static ResourceLocation glow(String name) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_" + name + "_emissive.png");
    }
    public StegosaurusGlowLayer(RenderLayerParent<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> renderer) {
        super(renderer);
    }
    @Override public void render(PoseStack stack, MultiBufferSource buffers, int packedLight, StegosaurusEntity entity,
                                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                float netHeadYaw, float headPitch) {
        ResourceLocation glow = GLOWS[Math.max(0, Math.min(GLOWS.length - 1, entity.getVariant()))];
        VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(glow));
        this.getParentModel().renderToBuffer(stack, consumer, 15728640, OverlayTexture.NO_OVERLAY,
            1.0F, 1.0F, 1.0F, 1.0F);
    }
}
