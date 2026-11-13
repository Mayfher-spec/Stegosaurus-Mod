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

    public static final RegistryObject<SoundEvent> CARNO_HURT = reg("carnotaurus.hurt");
    public static final RegistryObject<SoundEvent> CARNO_HURT_BABY = reg("carnotaurus.hurt_baby");
    public static final RegistryObject<SoundEvent> CARNO_DEATH = reg("carnotaurus.death");
    public static final RegistryObject<SoundEvent> CARNO_ROAR = reg("carnotaurus.roar");
    public static final RegistryObject<SoundEvent> CARNO_BITE = reg("carnotaurus.bite");
    public static final RegistryObject<SoundEvent> CARNO_STEP = reg("carnotaurus.step");
    public static final RegistryObject<SoundEvent> CARNO_AMBIENT = reg("carnotaurus.ambient");
    public static final RegistryObject<SoundEvent> CARNO_AMBIENT_BABY = reg("carnotaurus.ambient_baby");
    public static final RegistryObject<SoundEvent> CARNO_BREATHE = reg("carnotaurus.breathe");
    public static final RegistryObject<SoundEvent> CARNO_SIT = reg("carnotaurus.sit");
    public static final RegistryObject<SoundEvent> CARNO_SLEEP = reg("carnotaurus.sleep");
    public static final RegistryObject<SoundEvent> CARNO_WAKE = reg("carnotaurus.wake");
    public static final RegistryObject<SoundEvent> CARNO_HAPPY = reg("carnotaurus.happy");
    public static final RegistryObject<SoundEvent> CARNO_EAT = reg("carnotaurus.eat");
    public static final RegistryObject<SoundEvent> CARNO_CHARGE_HIT = reg("carnotaurus.charge_hit");
    public static final RegistryObject<SoundEvent> CARNO_ULT_ROAR = reg("carnotaurus.ult_roar");
    public static final RegistryObject<SoundEvent> CARNO_EVOLUTION = reg("carnotaurus.evolution");
    public static final RegistryObject<SoundEvent> CARNO_REVERT = reg("carnotaurus.revert");
    public static final RegistryObject<SoundEvent> DILO_AMBIENT = reg("dilophosaurus.ambient");
    public static final RegistryObject<SoundEvent> DILO_AMBIENT_BABY = reg("dilophosaurus.ambient_baby");
    public static final RegistryObject<SoundEvent> DILO_HURT = reg("dilophosaurus.hurt");
    public static final RegistryObject<SoundEvent> DILO_HURT_BABY = reg("dilophosaurus.hurt_baby");
    public static final RegistryObject<SoundEvent> DILO_DEATH = reg("dilophosaurus.death");
    public static final RegistryObject<SoundEvent> DILO_BITE = reg("dilophosaurus.bite");
    public static final RegistryObject<SoundEvent> DILO_CLAW = reg("dilophosaurus.claw");
    public static final RegistryObject<SoundEvent> DILO_POUNCE = reg("dilophosaurus.pounce");
    public static final RegistryObject<SoundEvent> DILO_FRILL_OPEN = reg("dilophosaurus.frill_open");
    public static final RegistryObject<SoundEvent> DILO_FRILL_CLOSE = reg("dilophosaurus.frill_close");
    public static final RegistryObject<SoundEvent> DILO_ROAR = reg("dilophosaurus.roar");
    public static final RegistryObject<SoundEvent> DILO_FRENZY = reg("dilophosaurus.frenzy");
    public static final RegistryObject<SoundEvent> DILO_EAT = reg("dilophosaurus.eat");
    public static final RegistryObject<SoundEvent> DILO_HAPPY = reg("dilophosaurus.happy");
    public static final RegistryObject<SoundEvent> DILO_SLEEP = reg("dilophosaurus.sleep");
    public static final RegistryObject<SoundEvent> DILO_BREATHE = reg("dilophosaurus.breathe");
    public static final RegistryObject<SoundEvent> DILO_SIT = reg("dilophosaurus.sit");
    public static final RegistryObject<SoundEvent> DILO_WAKE = reg("dilophosaurus.wake");
    public static final RegistryObject<SoundEvent> DILO_EVOLUTION = reg("dilophosaurus.evolution");
    public static final RegistryObject<SoundEvent> DILO_REVERT = reg("dilophosaurus.revert");
    public static final RegistryObject<SoundEvent> DILO_STEP = reg("dilophosaurus.step");
    public static final RegistryObject<SoundEvent> STEGO_AMBIENT = reg("stegosaurus.ambient");
    public static final RegistryObject<SoundEvent> STEGO_HURT = reg("stegosaurus.hurt");
    public static final RegistryObject<SoundEvent> STEGO_DEATH = reg("stegosaurus.death");
    public static final RegistryObject<SoundEvent> STEGO_ROAR = reg("stegosaurus.roar");
    public static final RegistryObject<SoundEvent> STEGO_BITE = reg("stegosaurus.bite");
    public static final RegistryObject<SoundEvent> STEGO_TAIL = reg("stegosaurus.tail");
    public static final RegistryObject<SoundEvent> STEGO_ULT_ROAR = reg("stegosaurus.ult_roar");
    public static final RegistryObject<SoundEvent> STEGO_TAIL_TREE = reg("stegosaurus.tail_tree");
    public static final RegistryObject<SoundEvent> STEGO_CHARGE_HIT = reg("stegosaurus.charge_hit");
    public static final RegistryObject<SoundEvent> STEGO_ADULT_SIT = reg("stegosaurus.adult_sit");
    public static final RegistryObject<SoundEvent> STEGO_ADULT_SLEEP = reg("stegosaurus.adult_sleep");
    public static final RegistryObject<SoundEvent> STEGO_ADULT_WAKE = reg("stegosaurus.adult_wake");
    public static final RegistryObject<SoundEvent> STEGO_ADULT_HAPPY = reg("stegosaurus.adult_happy");
    public static final RegistryObject<SoundEvent> STEGO_ADULT_EAT = reg("stegosaurus.adult_eat");
    public static final RegistryObject<SoundEvent> STEGO_ADULT_BREATHE = reg("stegosaurus.adult_breathe");
    public static final RegistryObject<SoundEvent> STEGO_ULT_ROAR_CYBER = reg("stegosaurus.ult_roar_cyber");
    public static final RegistryObject<SoundEvent> STEGO_ULT_ROAR_GOLD = reg("stegosaurus.ult_roar_gold");
    public static final RegistryObject<SoundEvent> STEGO_ULT_ROAR_MAGMA = reg("stegosaurus.ult_roar_magma");
    public static final RegistryObject<SoundEvent> STEGO_ULT_ROAR_GLACIAL = reg("stegosaurus.ult_roar_glacial");
    public static final RegistryObject<SoundEvent> STEGO_EVOLUTION = reg("stegosaurus.evolution");
    public static final RegistryObject<SoundEvent> STEGO_STEP = reg("stegosaurus.step");
    public static final RegistryObject<SoundEvent> STEGO_EVOLVE = reg("stegosaurus.evolve");
    public static final RegistryObject<SoundEvent> CHIBI_EVOLVE = reg("chibi.evolve");
    public static final RegistryObject<SoundEvent> CHIBI_AMBIENT = reg("chibi.ambient");
    public static final RegistryObject<SoundEvent> CHIBI_HURT = reg("chibi.hurt");
    public static final RegistryObject<SoundEvent> CHIBI_DEATH = reg("chibi.death");

    private ModSounds() {}
}
