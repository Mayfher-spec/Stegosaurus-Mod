package com.prodigytown.creatures;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Efectos propios. El fuego espectral es un efecto para que el cliente sepa que debe dibujar llamas verdes. */
public final class ModEffects {
    private ModEffects() {}
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ProdigyCreatures.MODID);
    public static final RegistryObject<MobEffect> SPECTRAL_BURN = EFFECTS.register("spectral_burn", SpectralBurn::new);

    /** 2 de dano por segundo (el doble que el fuego normal), ignorado si el objetivo es inmune al fuego. */
    public static final class SpectralBurn extends MobEffect {
        public SpectralBurn() { super(MobEffectCategory.HARMFUL, 0x33FF77); }

        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }

        @Override public void applyEffectTick(LivingEntity e, int amplifier) {
            if (e.level().isClientSide || e.fireImmune()) return;
            e.hurt(e.damageSources().onFire(), 2.0F);
            if (e.level() instanceof ServerLevel sl)
                sl.sendParticles(new DustParticleOptions(new org.joml.Vector3f(0.2F, 1.0F, 0.45F), 1.3F),
                    e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 5, 0.25, 0.4, 0.25, 0.01);
        }
    }
}
