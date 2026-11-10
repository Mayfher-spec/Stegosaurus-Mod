package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer del carnotauro: textura segun variante, escala segun etapa (cria / adulto). */
public final class CarnotaurusRenderer extends MobRenderer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    private static final ResourceLocation[] SKINS = {tex("scarlet"), tex("magma"), tex("emerald")};
    private static ResourceLocation tex(String n) { return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_" + n + ".png"); }

    public CarnotaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new CarnotaurusModel<>(context.bakeLayer(CarnotaurusModel.LAYER_LOCATION)), 1.6F);
        this.addLayer(new CarnotaurusSaddleLayer(this));
        this.addLayer(new CarnotaurusGlowLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(CarnotaurusEntity e) {
        return SKINS[Math.max(0, Math.min(SKINS.length - 1, e.getVariant()))];
    }

    @Override protected void scale(CarnotaurusEntity e, PoseStack stack, float partialTick) {
        float s = e.isAdultStage() ? 0.85F : 0.38F;
        stack.scale(s, s, s);
        this.shadowRadius = e.isAdultStage() ? 1.6F : 0.6F;
    }
}
