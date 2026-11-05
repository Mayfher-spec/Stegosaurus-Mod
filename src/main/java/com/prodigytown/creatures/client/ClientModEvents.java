package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ProdigyCreatures.STEGOSAURUS.get(), StegosaurusRenderer::new);
        event.registerBlockEntityRenderer(ProdigyCreatures.EGG_BE.get(), StegosaurusEggRenderer::new);
    }
    @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StegosaurusModel.LAYER_LOCATION, StegosaurusModel::createBodyLayer);
        event.registerLayerDefinition(StegosaurusSaddleModel.LAYER_LOCATION, StegosaurusSaddleModel::createBodyLayer);
        event.registerLayerDefinition(StegosaurusArmorModel.LAYER_LOCATION, StegosaurusArmorModel::createBodyLayer);
        event.registerLayerDefinition(StegosaurusEggModel.LAYER, StegosaurusEggModel::createBodyLayer);
    }
}
