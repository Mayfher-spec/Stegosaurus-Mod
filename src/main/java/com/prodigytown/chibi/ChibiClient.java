package com.prodigytown.chibi;

import com.prodigytown.creatures.ProdigyCreatures;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The chibi keeps its original resource namespace for compatibility. */
class ChibiModel extends GeoModel<ChibiStego> {
    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ChibiAlpha.LEGACY_NAMESPACE, path);
    }
    @Override public ResourceLocation getModelResource(ChibiStego entity) {
        return resource("geo/stegosaurus_chibi.geo.json");
    }
    /** Textura 256x256 toda blanca para el parpadeo de la evolucion. */
    static final ResourceLocation WHITE = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/chibi_white.png");
    @Override public ResourceLocation getTextureResource(ChibiStego entity) {
        int c = entity.chargeTicks();
        if (c > 0) { // parpadea entre su color y blanco, cada vez mas rapido, y termina fijo en blanco
            int e = ChibiStego.EVOLVE_PRE - c;
            if (c <= 8 || (e / Math.max(2, 10 - e / 6)) % 2 == 0) return WHITE;
        }
        String texture = switch (entity.variant()) {
            case ChibiStego.CYBER -> "cibernetico";
            case ChibiStego.MAGMA -> "magma";
            case ChibiStego.GOLD -> "dorado";
            default -> "natural";
        };
        return resource("textures/entity/stegosaurus_chibi_" + texture + ".png");
    }
    @Override public ResourceLocation getAnimationResource(ChibiStego entity) {
        return resource("animations/stegosaurus_chibi.animation.json");
    }
}
class ChibiRenderer extends GeoEntityRenderer<ChibiStego> {
    public ChibiRenderer(EntityRendererProvider.Context context) {
        super(context, new ChibiModel());
        shadowRadius = 0.55F;
    }
    /** El parpadeo blanco se dibuja brillante (sin sombreado), como una silueta de luz. */
    @Override public RenderType getRenderType(ChibiStego animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (texture.equals(ChibiModel.WHITE)) return RenderType.eyes(texture);
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }
}

/** Single Forge MOD event bus, under prodigycreatures. */
@Mod.EventBusSubscriber(modid=ProdigyCreatures.MODID,
    bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ChibiClient {
    private ChibiClient() {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChibiAlpha.STEGO.get(), ChibiRenderer::new);
    }
}
