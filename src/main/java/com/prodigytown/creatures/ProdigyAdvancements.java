package com.prodigytown.creatures;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Logros del mod (data/prodigycreatures/advancements): todos usan el criterio "done" y los concede el codigo. */
public final class ProdigyAdvancements {
    private static final String[] TAMES = {"tame_stego", "tame_carno", "tame_dilo", "tame_compy"};

    private ProdigyAdvancements() {}

    /** Concede el logro a un jugador (si la entidad no es un jugador de servidor, no hace nada). */
    public static void grant(@Nullable Entity who, String id) {
        if (!(who instanceof ServerPlayer sp) || sp.getServer() == null) return;
        Advancement adv = sp.getServer().getAdvancements().getAdvancement(new ResourceLocation(ProdigyCreatures.MODID, id));
        if (adv == null || sp.getAdvancements().getOrStartProgress(adv).isDone()) return;
        sp.getAdvancements().award(adv, "done");
        if (id.startsWith("tame_") && !id.equals("tame_all") && done(sp, TAMES)) grant(sp, "tame_all");
    }

    public static void grant(ServerLevel level, @Nullable UUID player, String id) {
        if (player != null) grant(level.getPlayerByUUID(player), id);
    }

    private static boolean done(ServerPlayer sp, String[] ids) {
        for (String id : ids) {
            Advancement adv = sp.getServer().getAdvancements().getAdvancement(new ResourceLocation(ProdigyCreatures.MODID, id));
            if (adv == null || !sp.getAdvancements().getOrStartProgress(adv).isDone()) return false;
        }
        return true;
    }

    /** Logro segun la criatura domada (lo llama QuestData.onTame, que usan las cuatro). */
    public static void onTame(ServerPlayer p, Entity animal) {
        if (animal instanceof StegosaurusEntity || animal instanceof com.prodigytown.chibi.ChibiStego) grant(p, "tame_stego");
        else if (animal instanceof CarnotaurusEntity) grant(p, "tame_carno");
        else if (animal instanceof DilophosaurusEntity) grant(p, "tame_dilo");
        else if (animal instanceof CompyEntity) grant(p, "tame_compy");
    }
}
