package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
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
        this.addLayer(new CarnotaurusFlashLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(CarnotaurusEntity e) {
        if (e.getMorph() > 0) return StegosaurusRenderer.WHITE;
        return SKINS[Math.max(0, Math.min(SKINS.length - 1, e.getVariant()))];
    }

    /** La cria y el adulto son mallas distintas con los mismos huesos: se elige antes de dibujar (las capas usan el modelo del renderer). */
    @Override public void render(CarnotaurusEntity e, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
        this.model = e.isAdultStage() || e.getMorph() > 0 && e.getMorphKind() == 1 ? adultModel : chibiModel;
        super.render(e, yaw, partialTick, stack, buffers, light);
    }

    /** Transformacion: silueta blanca y brillante, sin textura (como la del estegosaurio). */
    @Nullable @Override protected RenderType getRenderType(CarnotaurusEntity e, boolean visible, boolean translucent, boolean glowing) {
        if (e.getMorph() > 0) return RenderType.eyes(StegosaurusRenderer.WHITE);
        return super.getRenderType(e, visible, translucent, glowing);
    }

    /** Del tamano de la cria (1,8 / 3,8 de alto) al del adulto, o al reves, con ease y pulsos cada vez mas rapidos al crecer. */
    private static final float CHIBI_RATIO = 0.47F;

    @Override protected void scale(CarnotaurusEntity e, PoseStack stack, float partialTick) {
        int left = e.getMorph();
        float sc = e.isAdultStage() ? ADULT_SCALE : CHIBI_SCALE;
        float shadow = e.isAdultStage() ? 2.0F : 0.75F;
        if (left > 0) {
            boolean up = e.getMorphKind() == 1;
            int len = up ? CarnotaurusEntity.GROW_LEN : CarnotaurusEntity.SHRINK_LEN;
            float el = len - left + partialTick;
            float t = Mth.clamp(el / len, 0.0F, 1.0F), ease = t * t * (3.0F - 2.0F * t);
            float k = up ? Mth.lerp(ease, CHIBI_RATIO, 1.0F) : Mth.lerp(ease, 1.0F, CHIBI_RATIO);
            k *= 1.0F + 0.05F * (up ? 1.0F - 0.5F * t : 0.6F) * Mth.sin(el * (0.5F + 1.2F * t));
            sc = ADULT_SCALE * k;
            shadow = 2.0F * k;
        }
        stack.scale(sc, sc, sc);
        this.shadowRadius = shadow;
    }
}
