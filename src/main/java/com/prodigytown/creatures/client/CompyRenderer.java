package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.CompyEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer del Compy: una malla, ocho texturas (con ojos cerrados al parpadear y al dormir). */
public final class CompyRenderer extends MobRenderer<CompyEntity, CompyModel<CompyEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[CompyEntity.VARIANTS];
    private static final ResourceLocation[] SKINS_CLOSED = new ResourceLocation[CompyEntity.VARIANTS];
    static {
        for (int i = 0; i < CompyEntity.VARIANTS; i++) {
            SKINS[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/compy_" + CompyEntity.VARIANT_IDS[i] + ".png");
            SKINS_CLOSED[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/compy_" + CompyEntity.VARIANT_IDS[i] + "_closed.png");
        }
    }

    /** El esqueleto mide ~95 unidades de largo y ~42 de alto (1 unidad = 1/16 de bloque antes de escalar): a esta escala mide ~1,1 x 0,48 bloques (tamano de pollo) y encaja en su hitbox de 0,4 x 0,5. */
    public static final float SCALE = 0.185F;

    public CompyRenderer(EntityRendererProvider.Context context) {
        super(context, new CompyModel<>(context.bakeLayer(CompyModel.LAYER)), 0.22F);
    }

    @Override public ResourceLocation getTextureLocation(CompyEntity e) {
        int v = Math.max(0, Math.min(SKINS.length - 1, e.getVariant()));
        return e.eyesClosed() ? SKINS_CLOSED[v] : SKINS[v];
    }

    @Override protected void scale(CompyEntity e, PoseStack stack, float partialTick) {
        float sc = e.isLeader() ? SCALE * CompyEntity.LEADER_SCALE : SCALE;   // el lider es un 20 % mas grande
        stack.scale(sc, sc, sc);
        this.shadowRadius = e.isLeader() ? 0.27F : 0.22F;
    }
}
