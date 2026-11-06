package com.prodigytown.creatures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sonidos propios (assets/prodigycreatures/sounds.json). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ProdigyCreatures.MODID);

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ProdigyCreatures.MODID, name)));
    }

    public static final RegistryObject<SoundEvent> STEGO_AMBIENT = reg("stegosaurus.ambient");
    public static final RegistryObject<SoundEvent> STEGO_HURT = reg("stegosaurus.hurt");
    public static final RegistryObject<SoundEvent> STEGO_DEATH = reg("stegosaurus.death");
    public static final RegistryObject<SoundEvent> STEGO_ROAR = reg("stegosaurus.roar");
    public static final RegistryObject<SoundEvent> STEGO_BITE = reg("stegosaurus.bite");
    public static final RegistryObject<SoundEvent> STEGO_TAIL = reg("stegosaurus.tail");
    public static final RegistryObject<SoundEvent> STEGO_BITE_POWER = reg("stegosaurus.bite_power");
    public static final RegistryObject<SoundEvent> STEGO_TAIL_POWER = reg("stegosaurus.tail_power");
    public static final RegistryObject<SoundEvent> STEGO_CHARGE_POWER = reg("stegosaurus.charge_power");
    public static final RegistryObject<SoundEvent> STEGO_ULT_ROAR = reg("stegosaurus.ult_roar");
    public static final RegistryObject<SoundEvent> STEGO_TAIL_TREE = reg("stegosaurus.tail_tree");
    public static final RegistryObject<SoundEvent> STEGO_CHARGE_HIT = reg("stegosaurus.charge_hit");
    public static final RegistryObject<SoundEvent> STEGO_STEP = reg("stegosaurus.step");
    public static final RegistryObject<SoundEvent> STEGO_EVOLVE = reg("stegosaurus.evolve");
    public static final RegistryObject<SoundEvent> CHIBI_EVOLVE = reg("chibi.evolve");
    public static final RegistryObject<SoundEvent> CHIBI_AMBIENT = reg("chibi.ambient");
    public static final RegistryObject<SoundEvent> CHIBI_HURT = reg("chibi.hurt");
    public static final RegistryObject<SoundEvent> CHIBI_DEATH = reg("chibi.death");

    private ModSounds() {}
}
