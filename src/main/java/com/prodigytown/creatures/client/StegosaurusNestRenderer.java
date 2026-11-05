package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.egg.StegosaurusEggs;
import com.prodigytown.creatures.egg.StegosaurusNestBlock;
import com.prodigytown.creatures.egg.StegosaurusNestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Dibuja el huevo dentro del nido. Mientras se incuba se mueve con balanceos suaves y esporadicos (mas seguidos cuanto mas avanzado),
 * se agrieta por etapas y en los ultimos 12 s se balancea cada vez mas fuerte antes de eclosionar. Encima muestra el tiempo restante.
 */
public final class StegosaurusNestRenderer implements BlockEntityRenderer<StegosaurusNestBlockEntity> {
    private static final ResourceLocation[][] TEX = new ResourceLocation[5][4];
    static {
        for (int v = 0; v < 5; v++) for (int s = 0; s < 4; s++)
            TEX[v][s] = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_egg_" + StegosaurusEggs.NAMES[v] + "_" + s + ".png");
    }
    /** Suelo interior del nido (3/16) donde se apoya el huevo. */
    private static final double FLOOR = 0.1875;
    private final StegosaurusEggModel model;
    private final Font font;

    public StegosaurusNestRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new StegosaurusEggModel(ctx.bakeLayer(StegosaurusEggModel.LAYER));
        this.font = ctx.getFont();
    }

    private static float smooth(float x) { x = Mth.clamp(x, 0.0F, 1.0F); return x * x * (3.0F - 2.0F * x); }

    @Override public void render(StegosaurusNestBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!st.hasProperty(StegosaurusNestBlock.EGG)) return;
        int egg = st.getValue(StegosaurusNestBlock.EGG);
        if (egg <= 0) return;
        int v = Math.max(0, Math.min(4, egg - 1));
        float prog = Math.min(be.total, be.shown + pt);
        float left = be.total > 0 ? Math.max(0.0F, be.total - prog) : 1.0E9F;
        float frac = be.total > 0 ? Mth.clamp(prog / be.total, 0.0F, 1.0F) : 0.0F;
        int stage = be.total <= 0 ? 0 : left <= StegosaurusNestBlockEntity.HATCH_ANIM ? 3 : frac > 0.72F ? 2 : frac > 0.38F ? 1 : 0;
        float t = (be.getLevel() != null ? be.getLevel().getGameTime() : 0L) + pt;
        float rx = 0.0F, rz = 0.0F, sc = 1.0F;
        if (be.total > 0) {
            if (left <= StegosaurusNestBlockEntity.HATCH_ANIM) { // eclosion: balanceo continuo cuya amplitud crece con suavidad
                float k = smooth((StegosaurusNestBlockEntity.HATCH_ANIM - left) / StegosaurusNestBlockEntity.HATCH_ANIM);
                float amp = 0.05F + 0.17F * k;
                rz = amp * (Mth.sin(t * 0.42F) + 0.4F * Mth.sin(t * 0.97F + 1.3F));
                rx = amp * (0.8F * Mth.sin(t * 0.35F + 0.7F) + 0.3F * Mth.sin(t * 0.83F));
                sc = 1.0F + 0.025F * k * Mth.sin(t * 0.6F) + 0.05F * smooth((24.0F - left) / 24.0F);
            } else { // movimientos esporadicos: una ondulacion suave y larga que empieza y termina en reposo
                float period = Mth.lerp(frac, 260.0F, 90.0F);
                float phase = (t + (be.getBlockPos().hashCode() & 127)) % period;
                float dur = 56.0F;
                if (phase < dur) {
                    float u = phase / dur, env = Mth.sin(u * Mth.PI);
                    env *= env;
                    float amp = Mth.lerp(frac, 0.06F, 0.15F);
                    rz = amp * env * Mth.sin(u * Mth.TWO_PI * 2.0F);
                    rx = amp * 0.6F * env * Mth.sin(u * Mth.TWO_PI * 1.5F + 1.0F);
                }
            }
        }
        pose.pushPose();
        pose.translate(0.5, FLOOR - 0.01, 0.5);
        pose.mulPose(Axis.ZP.rotation(rz));
        pose.mulPose(Axis.XP.rotation(rx));
        pose.scale(sc, sc, sc);
        pose.translate(0.0, 1.5, 0.0);
        pose.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX[v][stage]));
        model.root.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();

        if (be.total > 0) renderTimer(be, left, frac, pose, buffers);
    }

    /** Cartel que mira a la camara: tiempo restante y barra de avance. Solo si el jugador esta cerca. */
    private void renderTimer(StegosaurusNestBlockEntity be, float leftTicks, float frac, PoseStack pose, MultiBufferSource buffers) {
        Minecraft mc = Minecraft.getInstance();
        double dx = mc.gameRenderer.getMainCamera().getPosition().x - (be.getBlockPos().getX() + 0.5);
        double dy = mc.gameRenderer.getMainCamera().getPosition().y - (be.getBlockPos().getY() + 1.2);
        double dz = mc.gameRenderer.getMainCamera().getPosition().z - (be.getBlockPos().getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 10.0 * 10.0) return;
        int secs = (int) Math.ceil(leftTicks / 20.0F);
        String time = String.format("%d:%02d", secs / 60, secs % 60);
        int cells = 14, filled = Math.round(frac * cells);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < cells; i++) bar.append(i < filled ? '|' : '.');
        bar.append(']');
        pose.pushPose();
        pose.translate(0.5, FLOOR + 1.12, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.022F, -0.022F, 0.022F);
        Matrix4f m = pose.last().pose();
        int full = 0xF000F0;
        float tw = font.width(time);
        font.drawInBatch(time, -tw / 2.0F, 0.0F, 0xFFFFE9A8, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, full);
        float bw = font.width(bar.toString());
        font.drawInBatch(bar.toString(), -bw / 2.0F, 10.0F, 0xFFB8E986, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0x66000000, full);
        pose.popPose();
    }

    @Override public boolean shouldRenderOffScreen(StegosaurusNestBlockEntity be) { return false; }
}
