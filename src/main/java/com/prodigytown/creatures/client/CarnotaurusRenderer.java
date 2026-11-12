package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer del carnotauro: textura segun variante, escala segun etapa (cria / adulto). */
public final class CarnotaurusRenderer extends MobRenderer<CarnotaurusEntity, CarnotaurusModel<CarnotaurusEntity>> {
    private static final ResourceLocation[] SKINS = {tex("scarlet"), tex("green"), tex("steel"), tex("arena")};
    private static ResourceLocation tex(String n) { return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_" + n + ".png"); }

    /** El esqueleto esta dibujado a ~300 unidades de largo (1 texel por unidad): se reduce para que el adulto (Brutus) encaje en su hitbox de 2.7 x 3.8 bloques. */
    private static final float ADULT_SCALE = 0.36F, CHIBI_SCALE = 0.38F;

    private final CarnotaurusModel<CarnotaurusEntity> adultModel, chibiModel;

    public CarnotaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new CarnotaurusModel<>(context.bakeLayer(CarnotaurusModel.LAYER_ADULT), true), 1.5F);
        this.adultModel = this.model;
        this.chibiModel = new CarnotaurusModel<>(context.bakeLayer(CarnotaurusModel.LAYER_CHIBI), false);
        this.addLayer(new CarnotaurusSaddleLayer(this));
        this.addLayer(new CarnotaurusArmorLayer(this));
        this.addLayer(new CarnotaurusGlowLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(CarnotaurusEntity e) {
        return SKINS[Math.max(0, Math.min(SKINS.length - 1, e.getVariant()))];
    }

    /** La cria y el adulto son mallas distintas con los mismos huesos: se elige antes de dibujar (las capas usan el modelo del renderer). */
    @Override public void render(CarnotaurusEntity e, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
        this.model = e.isAdultStage() ? adultModel : chibiModel;
        super.render(e, yaw, partialTick, stack, buffers, light);
    }

    @Override protected void scale(CarnotaurusEntity e, PoseStack stack, float partialTick) {
        float sc = e.isAdultStage() ? ADULT_SCALE : CHIBI_SCALE;
        stack.scale(sc, sc, sc);
        this.shadowRadius = e.isAdultStage() ? 2.0F : 0.75F;
    }
}
