package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib 4 model with four texture variants and one original animated skeleton. */
public final class StegosaurusGeoModel extends GeoModel<StegosaurusEntity> {
    private static ResourceLocation asset(String path) {
        return new ResourceLocation(ProdigyCreatures.MODID, path);
    }
    private static final ResourceLocation MODEL = asset("geo/stegosaurus.geo.json");
    private static final ResourceLocation ANIMATIONS = asset("animations/stegosaurus.animation.json");

    @Override public ResourceLocation getModelResource(StegosaurusEntity entity) { return MODEL; }
    @Override public ResourceLocation getAnimationResource(StegosaurusEntity entity) { return ANIMATIONS; }
    @Override public ResourceLocation getTextureResource(StegosaurusEntity entity) {
        String variant = switch (entity.getVariant()) {
            case StegosaurusEntity.GOLDEN -> "golden";
            case StegosaurusEntity.MAGMA -> "magma";
            case StegosaurusEntity.GLACIAL -> "glacial";
            default -> "cybernetic";
        };
        return asset("textures/entity/stegosaurus_" + variant + ".png");
    }
}
