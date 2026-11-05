package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.egg.StegosaurusEggBlock;
import com.prodigytown.creatures.egg.StegosaurusEggBlockEntity;
import com.prodigytown.creatures.egg.StegosaurusEggs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dibuja el huevo apoyado sobre la fogata. Mientras se incuba se mueve de vez en cuando (mas seguido cuanto mas avanzado),
 * se va agrietando y en los ultimos 10 s se sacude con fuerza antes de eclosionar.
 */
public final class StegosaurusEggRenderer implements BlockEntityRenderer<StegosaurusEggBlockEntity> {
    private static final ResourceLocation[][] TEX = new ResourceLocation[5][4];
    static {
        for (int v = 0; v < 5; v++) for (int s = 0; s < 4; s++)
            TEX[v][s] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_egg_" + StegosaurusEggs.NAMES[v] + "_" + s + ".png");
    }
    private final StegosaurusEggModel model;

    public StegosaurusEggRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new StegosaurusEggModel(ctx.bakeLayer(StegosaurusEggModel.LAYER));
    }

    @Override public void render(StegosaurusEggBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(StegosaurusEggBlock.VARIANT)) return;
        int v = Math.max(0, Math.min(4, st.getValue(StegosaurusEggBlock.VARIANT)));
        float prog = be.shown + (be.warm ? pt : 0.0F);
        float left = be.total > 0 ? be.total - prog : 1.0E9F;
        float frac = be.total > 0 ? Mth.clamp(prog / be.total, 0.0F, 1.0F) : 0.0F;
        int stage = be.total <= 0 ? 0 : left <= StegosaurusEggBlockEntity.HATCH_ANIM ? 3 : frac > 0.72F ? 2 : frac > 0.38F ? 1 : 0;
        float t = (be.getLevel() != null ? be.getLevel().getGameTime() : 0L) + pt;
        float rx = 0.0F, rz = 0.0F, sc = 1.0F;
        if (be.warm && be.total > 0) {
            if (left <= StegosaurusEggBlockEntity.HATCH_ANIM) { // eclosion: sacudidas cada vez mas fuertes y un pequeno latido de tamano
                float k = 1.0F + (StegosaurusEggBlockEntity.HATCH_ANIM - Math.max(0.0F, left)) / StegosaurusEggBlockEntity.HATCH_ANIM * 1.4F;
                rz = 0.16F * k * Mth.sin(t * 1.9F);
                rx = 0.13F * k * Mth.cos(t * 2.3F);
                sc = 1.0F + 0.03F * k * Mth.sin(t * 0.9F);
            } else { // movimientos esporadicos: algo vive dentro
                float period = Mth.lerp(frac, 220.0F, 55.0F);
                float phase = (t + (be.getBlockPos().hashCode() & 127)) % period;
                float dur = 24.0F;
                if (phase < dur) {
                    float u = phase / dur, env = (1.0F - u) * (1.0F - u);
                    float amp = Mth.lerp(frac, 0.10F, 0.24F);
                    rz = amp * env * Mth.sin(u * Mth.TWO_PI * 3.0F);
                    rx = amp * 0.6F * env * Mth.sin(u * Mth.TWO_PI * 2.3F + 1.0F);
                }
            }
        }
        pose.pushPose();
        pose.translate(0.5, -0.53, 0.5); // apoyado sobre la fogata (el bloque esta una unidad mas arriba)
        pose.mulPose(Axis.ZP.rotation(rz));
        pose.mulPose(Axis.XP.rotation(rx));
        pose.scale(sc, sc, sc);
        pose.translate(0.0, 1.5, 0.0);
        pose.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX[v][stage]));
        model.root.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override public boolean shouldRenderOffScreen(StegosaurusEggBlockEntity be) { return false; }
}
