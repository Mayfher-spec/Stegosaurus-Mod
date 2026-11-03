package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The GeckoLib renderer animates geometry bones and adds masked glow on every variant. */
public final class StegosaurusRenderer extends GeoEntityRenderer<StegosaurusEntity> {
    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new StegosaurusGeoModel());
        this.shadowRadius = 1.85f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
