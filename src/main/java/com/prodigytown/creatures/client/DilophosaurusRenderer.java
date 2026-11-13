package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Renderer del dilofosaurio: textura segun variante (con ojos cerrados al parpadear y al dormir), escala segun etapa (cria / adulto). */
public final class DilophosaurusRenderer extends MobRenderer<DilophosaurusEntity, DilophosaurusModel<DilophosaurusEntity>> {
    public static final String[] VARIANT_IDS = {"natural", "electric", "magma", "desert", "spectral"};
    private static final ResourceLocation[] SKINS = new ResourceLocation[VARIANT_IDS.length];
    private static final ResourceLocation[] SKINS_CLOSED = new ResourceLocation[VARIANT_IDS.length];
    static {
        for (int i = 0; i < VARIANT_IDS.length; i++) {
            SKINS[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_" + VARIANT_IDS[i] + ".png");
            SKINS_CLOSED[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/dilophosaurus_" + VARIANT_IDS[i] + "_closed.png");
        }
    }

    /** El esqueleto adulto mide ~108 unidades de alto y ~210 de largo (1 unidad = 1/16 de bloque antes de escalar): a esta escala mide ~2,8 x 5,5 bloques y encaja en su hitbox de 1,2 x 2,6. */
    public static final float ADULT_SCALE = 0.42F, CHIBI_SCALE = 0.31F;
    /** Del tamano de la cria al del adulto (relacion de alturas). */
    private static final float CHIBI_RATIO = 0.46F;

    private final DilophosaurusModel<DilophosaurusEntity> adultModel, chibiModel;

    public DilophosaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new DilophosaurusModel<>(context.bakeLayer(DilophosaurusModel.LAYER_ADULT), true), 1.0F);
        this.adultModel = this.model;
        this.chibiModel = new DilophosaurusModel<>(context.bakeLayer(DilophosaurusModel.LAYER_CHIBI), false);
        this.addLayer(new DilophosaurusGlowLayer(this));
        this.addLayer(new DilophosaurusFlashLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(DilophosaurusEntity e) {
        if (e.getMorph() > 0) return StegosaurusRenderer.WHITE;
        int v = Math.max(0, Math.min(SKINS.length - 1, e.getVariant()));
        return e.eyesClosed() ? SKINS_CLOSED[v] : SKINS[v];
    }

    @Override public void render(DilophosaurusEntity e, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
        this.model = e.isAdultStage() || e.getMorph() > 0 && e.getMorphKind() == 1 ? adultModel : chibiModel;
        super.render(e, yaw, partialTick, stack, buffers, light);
    }

    @Nullable @Override protected RenderType getRenderType(DilophosaurusEntity e, boolean visible, boolean translucent, boolean glowing) {
        if (e.getMorph() > 0) return RenderType.eyes(StegosaurusRenderer.WHITE);
        return super.getRenderType(e, visible, translucent, glowing);
    }

    @Override protected void scale(DilophosaurusEntity e, PoseStack stack, float partialTick) {
        int left = e.getMorph();
        float sc = e.isAdultStage() ? ADULT_SCALE : CHIBI_SCALE;
        float shadow = e.isAdultStage() ? 1.0F : 0.5F;
        if (left > 0) {
            boolean up = e.getMorphKind() == 1;
            int len = up ? DilophosaurusEntity.GROW_LEN : DilophosaurusEntity.SHRINK_LEN;
            float el = len - left + partialTick;
            float t = Mth.clamp(el / len, 0.0F, 1.0F), ease = t * t * (3.0F - 2.0F * t);
            float k = up ? Mth.lerp(ease, CHIBI_RATIO, 1.0F) : Mth.lerp(ease, 1.0F, CHIBI_RATIO);
            k *= 1.0F + 0.05F * (up ? 1.0F - 0.5F * t : 0.6F) * Mth.sin(el * (0.5F + 1.2F * t));
            sc = ADULT_SCALE * k;
            shadow = 1.0F * k;
        }
        stack.scale(sc, sc, sc);
        this.shadowRadius = shadow;
    }
}
