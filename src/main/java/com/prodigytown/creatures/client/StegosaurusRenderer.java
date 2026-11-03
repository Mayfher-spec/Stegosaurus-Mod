package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Color texture plus masked fullbright surface. Never changes biome lighting. */
public class StegosaurusRenderer extends MobRenderer<StegosaurusEntity,StegosaurusModel> {
    private static ResourceLocation res(String path) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_" + path + ".png");
    }
    private static final ResourceLocation CYBER = res("cybernetic");
    private static final ResourceLocation GOLD = res("golden");
    private static final ResourceLocation MAGMA = res("magma");
    private static final ResourceLocation GLACIAL = res("glacial");
    private static final ResourceLocation CYBER_LIGHT = res("cybernetic_emissive");
    private static final ResourceLocation GOLD_LIGHT = res("golden_emissive");
    private static final ResourceLocation MAGMA_LIGHT = res("magma_emissive");
    private static final ResourceLocation GLACIAL_LIGHT = res("glacial_emissive");

    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context,new StegosaurusModel(context.bakeLayer(StegosaurusModel.LAYER)),1.85f);
        this.addLayer(new EmissiveLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(StegosaurusEntity entity) {
        return switch(entity.getVariant()) {
            case StegosaurusEntity.ALBINO -> GOLD;
            case StegosaurusEntity.MAGMA -> MAGMA;
            case StegosaurusEntity.GLACIAL -> GLACIAL;
            default -> CYBER;
        };
    }

    private static ResourceLocation getGlowTexture(StegosaurusEntity entity) {
        return switch(entity.getVariant()) {
            case StegosaurusEntity.ALBINO -> GOLD_LIGHT;
            case StegosaurusEntity.MAGMA -> MAGMA_LIGHT;
            case StegosaurusEntity.GLACIAL -> GLACIAL_LIGHT;
            default -> CYBER_LIGHT;
        };
    }

    private static final class EmissiveLayer extends RenderLayer<StegosaurusEntity,StegosaurusModel> {
        private EmissiveLayer(RenderLayerParent<StegosaurusEntity,StegosaurusModel> parent) {
            super(parent);
        }
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int packedLight,
                                     StegosaurusEntity dinosaur,float limbSwing,float limbAmount,
                                     float partialTick,float ageTicks,float netHeadYaw,float headPitch) {
            // Only alpha pixels in the _emissive atlas are lit: eye highlights,
            // dorsal cores and thin decorative lines. The base atlas remains shaded.
            ResourceLocation glow=getGlowTexture(dinosaur);
            this.getParentModel().renderToBuffer(pose,buffers.getBuffer(RenderType.eyes(glow)),
                0xF000F0,OverlayTexture.NO_OVERLAY,1f,1f,1f,1f);
        }
    }
}
