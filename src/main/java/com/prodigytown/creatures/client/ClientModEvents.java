package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ProdigyCreatures.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ProdigyCreatures.STEGOSAURUS.get(),StegosaurusRenderer::new);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StegosaurusModel.LAYER,StegosaurusModel::createBodyLayer);
    }
}
