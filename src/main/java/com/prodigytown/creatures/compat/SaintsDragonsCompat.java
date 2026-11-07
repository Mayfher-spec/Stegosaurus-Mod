package com.prodigytown.creatures.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibilidad opcional con Saint's Dragons (por reflexion: no hace falta tenerlo para compilar ni para jugar).
 * Encoge a los dragones domados a su forma bebe (usa su propio "crecimiento detenido") y los vuelve a hacer adultos.
 */
public final class SaintsDragonsCompat {
    private static final String DRAGON_CLASS = "com.leon.saintsdragons.server.entity.base.DragonEntity";
    private static Class<?> dragonClass;
    private static Method isStunted, setStunted;
    private static boolean init;

    private SaintsDragonsCompat() {}

    private static synchronized boolean ready() {
        if (!init) {
            init = true;
            try {
                try { dragonClass = Class.forName(DRAGON_CLASS); }
                catch (ClassNotFoundException e) { dragonClass = Class.forName(DRAGON_CLASS, false, Thread.currentThread().getContextClassLoader()); }
                isStunted = dragonClass.getMethod("isGrowthStunted");
                setStunted = dragonClass.getMethod("setGrowthStunted", boolean.class);
            } catch (Throwable t) {
                dragonClass = null;
            }
        }
        return dragonClass != null;
    }

    public static boolean isDragon(Entity e) { return e != null && ready() && dragonClass.isInstance(e); }

    private static boolean stunted(Entity e) {
        try { return (boolean) isStunted.invoke(e); } catch (Throwable t) { return false; }
    }

    private static void apply(Entity e, boolean small) {
        try {
            if (small) {
                e.ejectPassengers();
                setStunted.invoke(e, true); // el propio mod pone edad -24000 y lo marca bebe
            } else {
                setStunted.invoke(e, false);
                if (e instanceof AgeableMob a) a.setAge(0); // adulto al instante
            }
        } catch (Throwable ignored) {}
    }

    /** targetId: dragon elegido (-1 = el que montas o el mas cercano). all: todos los tuyos en 48 bloques. */
    public static void toggle(ServerPlayer sp, int targetId, boolean all) {
        if (!ready()) { sp.displayClientMessage(Component.literal("Saint's Dragons no esta instalado"), true); return; }
        List<TamableAnimal> targets = new ArrayList<>();
        if (all) {
            for (TamableAnimal t : sp.level().getEntitiesOfClass(TamableAnimal.class, sp.getBoundingBox().inflate(48.0), e -> isDragon(e) && e.isTame() && e.isOwnedBy(sp))) targets.add(t);
        } else {
            Entity e = targetId >= 0 ? sp.level().getEntity(targetId) : null;
            if (!(e instanceof TamableAnimal) || !isDragon(e)) e = sp.getVehicle();
            if (!(e instanceof TamableAnimal) || !isDragon(e)) {
                e = null;
                double best = 20.0 * 20.0;
                for (TamableAnimal t : sp.level().getEntitiesOfClass(TamableAnimal.class, sp.getBoundingBox().inflate(20.0), x -> isDragon(x) && x.isTame() && x.isOwnedBy(sp))) {
                    double d = t.distanceToSqr(sp);
                    if (d < best) { best = d; e = t; }
                }
            }
            if (e instanceof TamableAnimal t && t.isTame() && t.isOwnedBy(sp) && t.distanceTo(sp) <= 40.0F) targets.add(t);
        }
        if (targets.isEmpty()) { sp.displayClientMessage(Component.literal("No hay un dragon tuyo cerca"), true); return; }
        boolean shrink = false;
        for (TamableAnimal t : targets) if (!stunted(t)) { shrink = true; break; }
        for (TamableAnimal t : targets) apply(t, shrink);
        sp.displayClientMessage(Component.literal(targets.size() + (shrink ? " dragon(es) ahora pequeno(s)" : " dragon(es) vuelven a ser adultos")), true);
    }
}
