package com.prodigytown.creatures.quests;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;
import java.util.Set;

/** Seguimiento de misiones: muertes, domesticaciones y asedios ganados. */
@Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID)
public final class QuestEvents {
    private QuestEvents() {}

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent event) { event.addListener(new QuestManager()); }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (victim instanceof TamableAnimal ta && ta.isTame()) return;   // matar a la mascota de alguien no cuenta
        Set<ServerPlayer> credited = new LinkedHashSet<>();
        Entity src = event.getSource().getEntity();
        if (src instanceof ServerPlayer sp) credited.add(sp);
        else if (src instanceof OwnableEntity oe && oe.getOwner() instanceof ServerPlayer owner) credited.add(owner);
        if (victim.getKillCredit() instanceof ServerPlayer kc) credited.add(kc);
        QuestData.onKill(level, victim.getType(), credited, victim.getX(), victim.getY(), victim.getZ());
    }

    @SubscribeEvent
    public static void tame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer sp) QuestData.onTame(sp, event.getAnimal());
    }

    /** Ganar un asedio / raid da "Heroe de la aldea": ese efecto es la senal de victoria. */
    @SubscribeEvent
    public static void effect(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() == MobEffects.HERO_OF_THE_VILLAGE && event.getEntity() instanceof ServerPlayer sp) QuestData.onRaidWon(sp);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 100 == 17) QuestData.poll(sp);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        var old = event.getOriginal().getPersistentData();
        if (old.contains(QuestData.ROOT, 10)) event.getEntity().getPersistentData().put(QuestData.ROOT, old.getCompound(QuestData.ROOT).copy());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) QuestData.sync(sp, -1);
    }
}
