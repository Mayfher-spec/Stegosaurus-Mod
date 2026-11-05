package com.prodigytown.creatures;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private ModParticles() {}
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ProdigyCreatures.MODID);
    /** Llama verde como la del fuego de un bloc de magma o una antorcha, usada por el fuego espectral. */
    public static final RegistryObject<SimpleParticleType> SPECTRAL_FLAME = PARTICLES.register("spectral_flame", () -> new SimpleParticleType(false));
}
