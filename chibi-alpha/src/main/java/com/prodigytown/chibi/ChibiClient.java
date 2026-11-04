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
 private static ResourceLocation p(String s){return new ResourceLocation(ChibiAlpha.MODID,s);}
 @Override public ResourceLocation getModelResource(ChibiStego entity){return p(entity.saddled()?"geo/stegosaurus_chibi_saddled.geo.json":"geo/stegosaurus_chibi.geo.json");}
 @Override public ResourceLocation getTextureResource(ChibiStego entity){return p("textures/entity/stegosaurus_chibi_natural.png");}
 @Override public ResourceLocation getAnimationResource(ChibiStego entity){return p("animations/stegosaurus_chibi.animation.json");}
}
class ChibiRenderer extends GeoEntityRenderer<ChibiStego> {
 public ChibiRenderer(EntityRendererProvider.Context ctx){super(ctx,new ChibiModel());shadowRadius=.7f;}
}
@Mod.EventBusSubscriber(modid=ChibiAlpha.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
class ChibiClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(ChibiAlpha.STEGO.get(),ChibiRenderer::new);}
}