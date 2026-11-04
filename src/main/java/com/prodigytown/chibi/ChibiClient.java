package com.prodigytown.chibi;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
    @Override public ResourceLocation getTextureResource(ChibiStego entity) {
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
