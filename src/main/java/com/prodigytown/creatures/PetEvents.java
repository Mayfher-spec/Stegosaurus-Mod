package com.prodigytown.creatures;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Una mascota domada no muere: queda "caida" (ver PetOrders). Solo /kill y el vacio la matan. Bus FORGE. */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID)
public final class PetEvents {
    private PetEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(victim instanceof PetOrders.Holder holder) || !(victim instanceof TamableAnimal pet)) return;
        if (!pet.isTame() || holder.isFallen() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        event.setCanceled(true);
        PetOrders.enterFallen(pet);
    }
}
