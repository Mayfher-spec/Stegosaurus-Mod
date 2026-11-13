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
        event.registerEntityRenderer(ProdigyCreatures.CARNOTAURUS.get(), CarnotaurusRenderer::new);
        event.registerEntityRenderer(ProdigyCreatures.DILOPHOSAURUS.get(), DilophosaurusRenderer::new);
        event.registerBlockEntityRenderer(ProdigyCreatures.NEST_BE.get(), StegosaurusNestRenderer::new);
    }
    @SubscribeEvent public static void registerParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.prodigytown.creatures.ModParticles.SPECTRAL_FLAME.get(), net.minecraft.client.particle.FlameParticle.Provider::new);
        event.registerSpriteSet(com.prodigytown.creatures.ModParticles.SPECTRAL_FLAME_BIG.get(), sprites -> big(sprites, 8.0F));
        event.registerSpriteSet(com.prodigytown.creatures.ModParticles.ZZZ.get(), sprites -> zzz(sprites));
        event.registerSpriteSet(com.prodigytown.creatures.ModParticles.BIG_FLAME.get(), sprites -> big(sprites, 8.0F));
    }
    /** La "Z" del sueno: el tamano viaja en la velocidad x (el servidor lo manda con count 0). */
    private static net.minecraft.client.particle.ParticleProvider<net.minecraft.core.particles.SimpleParticleType> zzz(net.minecraft.client.particle.SpriteSet sprites) {
        net.minecraft.client.particle.HeartParticle.Provider base = new net.minecraft.client.particle.HeartParticle.Provider(sprites);
        return (type, level, x, y, z, dx, dy, dz) -> {
            net.minecraft.client.particle.Particle p = base.createParticle(type, level, x, y, z, 0.0, 0.0, 0.0);
            return p == null ? null : (dx > 0.0 ? p.scale((float) dx) : p);
        };
    }
    private static net.minecraft.client.particle.ParticleProvider<net.minecraft.core.particles.SimpleParticleType> big(net.minecraft.client.particle.SpriteSet sprites, float scale) {
        net.minecraft.client.particle.FlameParticle.Provider base = new net.minecraft.client.particle.FlameParticle.Provider(sprites);
        return (type, level, x, y, z, dx, dy, dz) -> {
            net.minecraft.client.particle.Particle p = base.createParticle(type, level, x, y, z, dx, dy, dz);
            return p == null ? null : p.scale(scale);
        };
    }
    @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CarnotaurusModel.LAYER_ADULT, CarnotaurusModel::createAdultLayer);
        event.registerLayerDefinition(CarnotaurusModel.LAYER_CHIBI, CarnotaurusModel::createChibiLayer);
        event.registerLayerDefinition(DilophosaurusModel.LAYER_ADULT, DilophosaurusModel::createAdultLayer);
        event.registerLayerDefinition(DilophosaurusModel.LAYER_CHIBI, DilophosaurusModel::createChibiLayer);
        event.registerLayerDefinition(StegosaurusModel.LAYER_LOCATION, StegosaurusModel::createBodyLayer);
        event.registerLayerDefinition(StegosaurusSaddleModel.LAYER_LOCATION, StegosaurusSaddleModel::createBodyLayer);
        event.registerLayerDefinition(StegosaurusArmorModel.LAYER_LOCATION, StegosaurusArmorModel::createBodyLayer);
        event.registerLayerDefinition(StegosaurusEggModel.LAYER, StegosaurusEggModel::createBodyLayer);
    }
}
