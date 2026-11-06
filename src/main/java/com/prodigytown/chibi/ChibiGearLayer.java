package com.prodigytown.chibi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Equipo del chibi: silla y armadura conservadas al volver a chibi. Cada pieza es un modelo GeckoLib propio con los mismos huesos
 * que el chibi; en cada cuadro se copia la pose animada de cada hueso y se vuelve a dibujar con su textura.
 */
public class ChibiGearLayer extends GeoRenderLayer<ChibiStego> {
    private static final String[] BONES = {"root", "body", "neck", "head", "jaw", "leg_front_right", "leg_front_left",
        "leg_back_right", "leg_back_left", "plates", "tail_base", "tail_tip", "saddle_anchor"};
    private static final ResourceLocation ARMOR_GEO = new ResourceLocation(ProdigyCreatures.MODID, "geo/stegosaurus_chibi_armor.geo.json");
    private static final ResourceLocation SADDLE_GEO = new ResourceLocation(ProdigyCreatures.MODID, "geo/stegosaurus_chibi_saddle.geo.json");
    private static final ResourceLocation SADDLE_TEX = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/chibi_saddle.png");
    private static final String[] TIERS = {"iron", "gold", "diamond", "netherite"};
    private boolean failed = false;

    public ChibiGearLayer(GeoRenderer<ChibiStego> renderer) { super(renderer); }

    @Override
    public void render(PoseStack poseStack, ChibiStego animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (failed || animatable.isInvisible() || animatable.chargeTicks() > 0) return;
        try {
            if (animatable.isSaddled()) draw(SADDLE_GEO, SADDLE_TEX, poseStack, animatable, bakedModel, bufferSource, partialTick, packedLight, packedOverlay);
            int tier = animatable.armorTier();
            if (tier > 0 && tier <= TIERS.length)
                draw(ARMOR_GEO, new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/chibi_armor_" + TIERS[tier - 1] + ".png"),
                    poseStack, animatable, bakedModel, bufferSource, partialTick, packedLight, packedOverlay);
        } catch (RuntimeException e) {
            failed = true; // si algo falla una vez, se apaga la capa en vez de cerrar el juego
            com.mojang.logging.LogUtils.getLogger().error("Chibi gear layer disabled", e);
        }
    }

    private void draw(ResourceLocation geo, ResourceLocation tex, PoseStack poseStack, ChibiStego animatable, BakedGeoModel animated,
                      MultiBufferSource bufferSource, float partialTick, int light, int overlay) {
        BakedGeoModel gear = GeckoLibCache.getBakedModels().get(geo);
        if (gear == null) return;
        for (String n : BONES) {
            GeoBone src = animated.getBone(n).orElse(null), dst = gear.getBone(n).orElse(null);
            if (src == null || dst == null) continue;
            dst.setPosX(src.getPosX()); dst.setPosY(src.getPosY()); dst.setPosZ(src.getPosZ());
            dst.setRotX(src.getRotX()); dst.setRotY(src.getRotY()); dst.setRotZ(src.getRotZ());
            dst.setScaleX(src.getScaleX()); dst.setScaleY(src.getScaleY()); dst.setScaleZ(src.getScaleZ());
        }
        RenderType rt = RenderType.entityCutoutNoCull(tex);
        getRenderer().reRender(gear, poseStack, bufferSource, animatable, rt, bufferSource.getBuffer(rt),
            partialTick, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
