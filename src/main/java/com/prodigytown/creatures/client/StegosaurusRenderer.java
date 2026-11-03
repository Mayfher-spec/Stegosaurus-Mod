package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StegosaurusRenderer extends MobRenderer<StegosaurusEntity,StegosaurusModel> {
    private static final ResourceLocation NORMAL = new ResourceLocation(ProdigyCreatures.MODID,"textures/entity/stegosaurus_normal.png");
    private static final ResourceLocation ALBINO = new ResourceLocation(ProdigyCreatures.MODID,"textures/entity/stegosaurus_albino.png");
    private static final ResourceLocation MAGMA = new ResourceLocation(ProdigyCreatures.MODID,"textures/entity/stegosaurus_magma.png");
    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context,new StegosaurusModel(context.bakeLayer(StegosaurusModel.LAYER)),1.5f);
    }
    @Override public ResourceLocation getTextureLocation(StegosaurusEntity entity) {
        return switch (entity.getVariant()) {case StegosaurusEntity.ALBINO -> ALBINO; case StegosaurusEntity.MAGMA -> MAGMA; default -> NORMAL;};
    }
}
