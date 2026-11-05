package com.prodigytown.creatures.progress;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** XP por combate (S: matar) y amortiguacion de caidas (S4). Bus FORGE. */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID)
public final class ProgressEvents {
    private ProgressEvents() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (victim instanceof Player || victim instanceof ProdigyPet) return;
        if (victim instanceof TamableAnimal t && t.isTame()) return;
        int xp = Math.max(1, (int) (victim.getMaxHealth() / 2.0F));
        var killer = event.getSource().getEntity();
        if (killer instanceof ProdigyPet pet) {
            pet.gainXp(xp);
        } else if (killer instanceof Player player) {
            List<TamableAnimal> pets = player.level().getEntitiesOfClass(TamableAnimal.class,
                player.getBoundingBox().inflate(24.0), e -> e instanceof ProdigyPet && e.isTame() && e.isOwnedBy(player));
            for (TamableAnimal pet : pets) ((ProdigyPet) pet).gainXp(xp);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        List<StegosaurusEntity> pets = player.level().getEntitiesOfClass(StegosaurusEntity.class,
            player.getBoundingBox().inflate(8.0), e -> e.isTame() && e.isOwnedBy(player));
        float best = 0.0F;
        for (StegosaurusEntity pet : pets) best = Math.max(best, pet.fallReduction());
        if (best > 0.0F) event.setDamageMultiplier(event.getDamageMultiplier() * (1.0F - best));
    }

    /** D5 Guardia: montado, el estegosaurio absorbe parte del dano dirigido al jinete (una sola vez). */
    @SubscribeEvent
    public static void onRiderHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (!(player.getVehicle() instanceof StegosaurusEntity stego)) return;
        int pct = stego.guardPercent();
        if (pct <= 0) return;
        var src = event.getSource();
        if (src.getEntity() == null || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float redirected = event.getAmount() * pct / 100.0F;
        event.setAmount(event.getAmount() - redirected);
        stego.takeRedirected(src, redirected);
    }
}
