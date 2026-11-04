package com.prodigytown.chibi;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

class ChibiModel extends GeoModel<ChibiStego> {
    private static ResourceLocation resource(String path) { return new ResourceLocation(ChibiAlpha.MODID, path); }

    @Override public ResourceLocation getModelResource(ChibiStego entity) {
        return resource("geo/stegosaurus_chibi.geo.json");
    }

    @Override public ResourceLocation getTextureResource(ChibiStego entity) {
        String name = switch (entity.variant()) {
            case ChibiStego.CYBER -> "cibernetico";
            case ChibiStego.MAGMA -> "magma";
            case ChibiStego.GOLD -> "dorado";
            default -> "natural";
        };
        return resource("textures/entity/stegosaurus_chibi_" + name + ".png");
    }

    @Override public ResourceLocation getAnimationResource(ChibiStego entity) {
        return resource("animations/stegosaurus_chibi.animation.json");
    }
}

class ChibiRenderer extends GeoEntityRenderer<ChibiStego> {
    public ChibiRenderer(EntityRendererProvider.Context context) {
        super(context, new ChibiModel());
        shadowRadius = 0.55f;
    }
}

@Mod.EventBusSubscriber(modid=ChibiAlpha.MODID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
class ChibiClient {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChibiAlpha.STEGO.get(), ChibiRenderer::new);
    }
}
