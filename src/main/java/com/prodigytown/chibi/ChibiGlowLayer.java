package com.prodigytown.chibi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Brillo del chibi: vuelve a dibujar el modelo con una textura emisiva (RenderType.eyes, brillo maximo).
 * Mismo patron de capa que usan otros mods GeckoLib 4. No se dibuja durante el parpadeo blanco de la evolucion.
 */
public class ChibiGlowLayer extends GeoRenderLayer<ChibiStego> {
    private boolean failed = false;
    public ChibiGlowLayer(GeoRenderer<ChibiStego> renderer) {
        super(renderer);
    }
    @Override
    public void render(PoseStack poseStack, ChibiStego animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isInvisible() || animatable.chargeTicks() > 0) return;
        if (failed) return;
        try {
            RenderType glow = RenderType.eyes(ChibiModel.glow(animatable));
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow),
                partialTick, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } catch (RuntimeException e) {
            failed = true; // si algo falla una vez, se apaga el brillo en vez de cerrar el juego
            com.mojang.logging.LogUtils.getLogger().error("Chibi glow layer disabled", e);
        }
    }
}
