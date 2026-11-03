package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** V2 uses four 512px original texture atlases, one per skin. */
public class StegosaurusRenderer extends MobRenderer<StegosaurusEntity,StegosaurusModel> {
    private static final ResourceLocation CYBERNETIC = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_cybernetic.png");
    private static final ResourceLocation GOLDEN = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_golden.png");
    private static final ResourceLocation MAGMA = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_magma.png");
    private static final ResourceLocation GLACIAL = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_glacial.png");

    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new StegosaurusModel(context.bakeLayer(StegosaurusModel.LAYER)), 2.1f);
    }

    @Override public ResourceLocation getTextureLocation(StegosaurusEntity entity) {
        return switch (entity.getVariant()) {
            case StegosaurusEntity.GOLDEN -> GOLDEN;
            case StegosaurusEntity.MAGMA -> MAGMA;
            case StegosaurusEntity.GLACIAL -> GLACIAL;
            default -> CYBERNETIC;
        };
    }
}
