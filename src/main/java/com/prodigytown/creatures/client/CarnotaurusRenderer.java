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
    private static final ResourceLocation[] SKINS = {tex("scarlet"), tex("magma"), tex("steel")};
    private static ResourceLocation tex(String n) { return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/carnotaurus_" + n + ".png"); }

    /** El esqueleto esta dibujado a ~300 unidades de largo (1 texel por unidad): se reduce para que el adulto mida ~6 bloques de largo y ~2.9 de alto. */
    private static final float MODEL_SCALE = 0.34F;

    private final CarnotaurusModel<CarnotaurusEntity> adultModel, chibiModel;

    public CarnotaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new CarnotaurusModel<>(context.bakeLayer(CarnotaurusModel.LAYER_ADULT), true), 1.5F);
        this.adultModel = this.model;
        this.chibiModel = new CarnotaurusModel<>(context.bakeLayer(CarnotaurusModel.LAYER_CHIBI), false);
        this.addLayer(new CarnotaurusSaddleLayer(this));
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
        stack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        this.shadowRadius = e.isAdultStage() ? 1.5F : 0.75F;
    }
}
