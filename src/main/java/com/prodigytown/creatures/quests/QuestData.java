package com.prodigytown.creatures.quests;

import com.prodigytown.creatures.net.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Estado de las misiones de cada jugador: autoritativo en el servidor y guardado en sus datos persistentes (sobrevive a la muerte). */
public final class QuestData {
    public static final int LOCKED = 0, AVAILABLE = 1, ACTIVE = 2, READY = 3, CLAIMED = 4;
    public static final String ROOT = "ProdigyQuestsV2";
    private static final double BOSS_RADIUS_SQ = 96.0 * 96.0;

    private QuestData() {}

    private static CompoundTag root(ServerPlayer p) {
        CompoundTag persistent = p.getPersistentData();
        if (!persistent.contains(ROOT, 10)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    private static CompoundTag st(ServerPlayer p, String id) {
        CompoundTag r = root(p);
        if (!r.contains(id, 10)) r.put(id, new CompoundTag());
        return r.getCompound(id);
    }

    private static boolean claimedFlag(ServerPlayer p, String id) { return root(p).contains(id, 10) && root(p).getCompound(id).getBoolean("claimed"); }

    public static int progress(ServerPlayer p, QuestDef q) { return root(p).contains(q.id, 10) ? root(p).getCompound(q.id).getInt("progress") : 0; }

    public static int state(ServerPlayer p, QuestDef q) {
        if (claimedFlag(p, q.id)) return CLAIMED;
        if (root(p).contains(q.id, 10) && root(p).getCompound(q.id).getBoolean("accepted")) return progress(p, q) >= q.target ? READY : ACTIVE;
        if (q.requires != null && QuestManager.get(q.requires) != null && !claimedFlag(p, q.requires)) return LOCKED;
        return AVAILABLE;
    }

    public static boolean accept(ServerPlayer p, QuestDef q) {
        if (state(p, q) != AVAILABLE) return false;
        CompoundTag s = st(p, q.id);
        s.putBoolean("accepted", true);
        s.putInt("progress", 0);
        return true;
    }

    public static boolean abandon(ServerPlayer p, QuestDef q) {
        if (state(p, q) != ACTIVE) return false;
        root(p).remove(q.id);
        return true;
    }

    public static void reset(ServerPlayer p) { p.getPersistentData().remove(ROOT); }

    /** Suma progreso (solo si la mision esta aceptada y no completada). Devuelve true si cambio algo. */
    public static boolean add(ServerPlayer p, QuestDef q, int amount) {
        if (state(p, q) != ACTIVE || amount <= 0) return false;
        CompoundTag s = st(p, q.id);
        int now = Math.min(q.target, s.getInt("progress") + amount);
        s.putInt("progress", now);
        if (now >= q.target) {
            p.displayClientMessage(Component.literal("Mision lista: " + q.title + " — vuelve con el Cronista").withStyle(ChatFormatting.GOLD), true);
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        return true;
    }

    /** Fuerza una mision a completada (comando de operador). */
    public static void complete(ServerPlayer p, QuestDef q) {
        if (state(p, q) == AVAILABLE || state(p, q) == LOCKED) { CompoundTag s = st(p, q.id); s.putBoolean("accepted", true); }
        if (state(p, q) == ACTIVE) add(p, q, q.target);
    }

    public static boolean claim(ServerPlayer p, QuestDef q) {
        if (state(p, q) != READY) return false;
        st(p, q.id).putBoolean("claimed", true);   // primero se marca: pedirlo dos veces nunca duplica el botin
        for (ItemStack proto : q.items) {
            ItemStack stack = proto.copy();
            if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
        }
        if (q.xp > 0) p.giveExperiencePoints(q.xp);
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.0F);
        p.displayClientMessage(Component.literal("Recompensa reclamada: " + q.title).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // ------------------------------------------------------------------ eventos del juego

    /** Muerte de una criatura: credito para quien la mato (o su companero domado) y, en los jefes, para todos los jugadores cercanos. */
    public static void onKill(ServerLevel level, EntityType<?> victimType, Set<ServerPlayer> credited, double x, double y, double z) {
        for (QuestDef q : QuestManager.all()) {
            if (q.type != QuestDef.Type.KILL || !q.matches(victimType)) continue;
            Set<ServerPlayer> who = new LinkedHashSet<>(credited);
            if (q.boss) for (ServerPlayer sp : level.players()) if (sp.distanceToSqr(x, y, z) <= BOSS_RADIUS_SQ) who.add(sp);
            for (ServerPlayer sp : who) if (add(sp, q, 1)) sync(sp, -1);
        }
    }

    public static void onTame(ServerPlayer p, Entity animal) {
        boolean changed = false;
        for (QuestDef q : QuestManager.all()) if (q.type == QuestDef.Type.TAME && q.matches(animal.getType())) changed |= add(p, q, 1);
        if (changed) sync(p, -1);
    }

    public static void onRaidWon(ServerPlayer p) {
        boolean changed = false;
        for (QuestDef q : QuestManager.all()) if (q.type == QuestDef.Type.RAID) changed |= add(p, q, 1);
        if (changed) sync(p, -1);
    }

    // ------------------------------------------------------------------ sincronizacion con el cliente

    public static List<QuestPackets.QuestView> views(ServerPlayer p) {
        List<QuestPackets.QuestView> out = new ArrayList<>();
        for (QuestDef q : QuestManager.all()) {
            QuestDef req = q.requires == null ? null : QuestManager.get(q.requires);
            out.add(new QuestPackets.QuestView(q.id, q.title, q.description, q.category, q.type.ordinal(), q.target,
                Math.min(q.target, progress(p, q)), state(p, q), req == null ? "" : req.title, q.items, q.xp));
        }
        return out;
    }

    /** npcId >= 0 abre la pantalla de misiones; -1 solo refresca los datos. */
    public static void sync(ServerPlayer p, int npcId) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new QuestPackets.QuestSync(npcId, views(p)));
    }
}
