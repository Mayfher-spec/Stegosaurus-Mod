package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Vanilla/Forge ModelPart renderer, same rendering family as ROR's T-Rex. */
public final class StegosaurusRenderer extends MobRenderer<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> {
    private static final ResourceLocation[] SKINS = {
        texture("cybernetic"), texture("golden"), texture("magma"), texture("glacial")
    };
    private static ResourceLocation texture(String name) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_" + name + ".png");
    }
    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new StegosaurusModel<>(context.bakeLayer(StegosaurusModel.LAYER_LOCATION)), 3.0F);
        this.addLayer(new StegosaurusGlowLayer(this));
    }
    @Override public ResourceLocation getTextureLocation(StegosaurusEntity entity) {
        return SKINS[Math.max(0, Math.min(SKINS.length - 1, entity.getVariant()))];
    }
}
