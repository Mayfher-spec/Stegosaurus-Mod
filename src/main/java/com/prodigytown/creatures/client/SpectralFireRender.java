package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.prodigytown.creatures.ModEffects;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Llamas verdes sobre quien tenga el efecto de fuego espectral: mismo dibujo que el fuego normal, con textura repintada. */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SpectralFireRender {
    private SpectralFireRender() {}
    private static final Material FIRE_0 = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(ProdigyCreatures.MODID, "block/spectral_fire_0"));
    private static final Material FIRE_1 = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation(ProdigyCreatures.MODID, "block/spectral_fire_1"));

    /** Llamas verdes que suben del cuerpo, como el fuego de un mob quemandose (aparecen aunque el dibujo plano no se vea desde arriba). */
    @SubscribeEvent public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        try {
            for (net.minecraft.world.entity.Entity ent : mc.level.entitiesForRendering()) {
                if (!(ent instanceof net.minecraft.world.entity.LivingEntity le) || !le.hasEffect(ModEffects.SPECTRAL_BURN.get())) continue;
                if (le.distanceToSqr(mc.player) > 64.0 * 64.0) continue;
                int n = 2 + (int) (le.getBbWidth() * le.getBbHeight() * 0.35F);
                n = Math.min(n, 14);
                for (int i = 0; i < n; i++) {
                    double x = le.getX() + (mc.level.random.nextDouble() - 0.5) * le.getBbWidth() * 0.9;
                    double y = le.getY() + mc.level.random.nextDouble() * le.getBbHeight();
                    double z = le.getZ() + (mc.level.random.nextDouble() - 0.5) * le.getBbWidth() * 0.9;
                    mc.level.addParticle(com.prodigytown.creatures.ModParticles.SPECTRAL_FLAME.get(), x, y, z, 0.0, 0.03 + mc.level.random.nextDouble() * 0.03, 0.0);
                }
            }
        } catch (RuntimeException ex) { /* decorativo */ }
    }

    /** Se dibuja tras las entidades (y no en RenderLivingEvent) para que tambien funcione con mobs de otros mods, p. ej. los de GeckoLib. */
    @SubscribeEvent public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            Vec3 cam = event.getCamera().getPosition();
            float pt = event.getPartialTick();
            MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
            boolean any = false;
            for (net.minecraft.world.entity.Entity ent : mc.level.entitiesForRendering()) {
                if (!(ent instanceof net.minecraft.world.entity.LivingEntity le) || le.isInvisible() || !le.hasEffect(ModEffects.SPECTRAL_BURN.get())) continue;
                double x = Mth.lerp(pt, le.xOld, le.getX()) - cam.x, y = Mth.lerp(pt, le.yOld, le.getY()) - cam.y, z = Mth.lerp(pt, le.zOld, le.getZ()) - cam.z;
                if (x * x + y * y + z * z > 96.0 * 96.0) continue;
                PoseStack ps = event.getPoseStack();
                ps.pushPose();
                ps.translate(x, y, z);
                flame(ps, buf, le);
                ps.popPose();
                any = true;
            }
            if (any) buf.endBatch(Sheets.cutoutBlockSheet());
        } catch (RuntimeException ex) { /* un fallo grafico no debe tirar el juego */ }
    }

    private static void flame(PoseStack ps, MultiBufferSource buf, net.minecraft.world.entity.Entity e) {
        TextureAtlasSprite s0 = FIRE_0.sprite(), s1 = FIRE_1.sprite();
        ps.pushPose();
        float f = e.getBbWidth() * 1.4F;
        ps.scale(f, f, f);
        float half = 0.5F, h = e.getBbHeight() / f, y0 = 0.0F, z = 0.0F;
        ps.mulPose(Axis.YP.rotationDegrees(-Minecraft.getInstance().gameRenderer.getMainCamera().getYRot()));
        ps.translate(0.0F, 0.0F, -0.3F + (float) ((int) h) * 0.02F);
        VertexConsumer vc = buf.getBuffer(Sheets.cutoutBlockSheet());
        int i = 0;
        for (PoseStack.Pose pose = ps.last(); h > 0.0F; i++) {
            TextureAtlasSprite s = i % 2 == 0 ? s0 : s1;
            float u0 = s.getU0(), v0 = s.getV0(), u1 = s.getU1(), v1 = s.getV1();
            if (i / 2 % 2 == 0) { float t = u1; u1 = u0; u0 = t; }
            vertex(pose, vc, -half, 0.0F - y0, z, u1, v1);
            vertex(pose, vc, half, 0.0F - y0, z, u0, v1);
            vertex(pose, vc, half, 1.4F - y0, z, u0, v0);
            vertex(pose, vc, -half, 1.4F - y0, z, u1, v0);
            h -= 0.45F; y0 -= 0.45F; half *= 0.9F; z += 0.03F;
        }
        ps.popPose();
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v) {
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(0, 10).uv2(240).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
