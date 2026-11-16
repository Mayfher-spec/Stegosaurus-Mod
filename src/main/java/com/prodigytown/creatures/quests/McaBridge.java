package com.prodigytown.creatures.quests;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lectura de aldeas de MCA Reborn por reflexion: el mod no depende de MCA al compilar ni al arrancar.
 * Si MCA no esta instalado o su API cambia, simplemente no hay datos de aldea y esas misiones no avanzan.
 */
public final class McaBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** MCA 7.6 para Forge publica sus clases bajo "forge.net.mca"; otras compilaciones usan "net.mca". Se prueban ambas. */
    private static final String[] PKGS = {"forge.net.mca.server.world.data.", "net.mca.server.world.data."};
    private static boolean failed;
    private static Method mgrGet, findNear, vPop, vBuildings, bType, bComplete;
    // Townstead (opcional): espiritu de aldea. El nivel de aldea es el mayor nivel (1-5) entre sus espiritus.
    private static boolean townsteadChecked;
    private static Method spTotals, spTier, spPerSpirit, spTotal;

    private McaBridge() {}

    public static boolean available() { return !failed && ModList.get().isLoaded("mca"); }

    /** Resumen de la aldea de MCA mas cercana al jugador. */
    public record VillageInfo(int population, int buildings, int buildingTypes, Map<String, Integer> byType, int level, Map<String, Integer> spiritTier, int spiritPoints) {

        public int metric(QuestDef q) {
            return switch (q.metric) {
                case "buildings" -> buildings;
                case "building_types" -> buildingTypes;
                case "level" -> level;
                case "spirit_points" -> spiritPoints;
                case "spirit" -> spiritTier.getOrDefault(q.building, 0);
                case "building" -> byType.getOrDefault(q.building, 0);
                default -> population;
            };
        }
    }

    private static Method find(Class<?> c, String name, int params) {
        for (Method m : c.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == params) return m;
        throw new IllegalStateException("MCA: falta " + c.getSimpleName() + "." + name + "/" + params);
    }

    private static void init() throws Exception {
        if (mgrGet != null) return;
        String pkg = null;
        for (String candidate : PKGS) { try { Class.forName(candidate + "VillageManager"); pkg = candidate; break; } catch (ClassNotFoundException ignored) { } }
        if (pkg == null) throw new ClassNotFoundException("MCA VillageManager");
        Class<?> vm = Class.forName(pkg + "VillageManager"), village = Class.forName(pkg + "Village"), building = Class.forName(pkg + "Building");
        mgrGet = find(vm, "get", 1);
        findNear = find(vm, "findNearestVillage", 2);   // (BlockPos, margen)
        vPop = find(village, "getPopulation", 0);
        vBuildings = find(village, "getBuildings", 0);
        bType = find(building, "getType", 0);
        bComplete = find(building, "isComplete", 0);
    }

    /** Townstead (opcional) por reflexion: niveles 1-5 por espiritu segun sus puntos. */
    private static void initTownstead() {
        if (townsteadChecked) return;
        townsteadChecked = true;
        if (!ModList.get().isLoaded("townstead")) return;
        try {
            String base = "com.aetherianartificer.townstead.spirit.";
            Class<?> agg = Class.forName(base + "VillageSpiritAggregator"), totals = Class.forName(base + "SpiritTotals");
            spTotals = find(agg, "totalsFor", 1);
            spTier = find(agg, "tierForSpirit", 1);
            spPerSpirit = find(totals, "perSpirit", 0);
            spTotal = find(totals, "total", 0);
        } catch (Throwable t) {
            spTotals = null;
            LOGGER.warn("[ProdigyQuests] no se pudo leer el espiritu de aldea de Townstead: {}", t.toString());
        }
    }

    @Nullable
    public static VillageInfo near(ServerPlayer sp) {
        if (!available()) return null;
        try {
            init();
            Object mgr = mgrGet.invoke(null, sp.serverLevel());
            Optional<?> v = (Optional<?>) findNear.invoke(mgr, sp.blockPosition(), 48);   // (BlockPos, margen)
            if (v.isEmpty()) return null;
            Object village = v.get();
            int pop = ((Number) vPop.invoke(village)).intValue(), total = 0;
            Map<String, Integer> byType = new HashMap<>();
            Set<String> types = new HashSet<>();
            for (Object b : ((Map<?, ?>) vBuildings.invoke(village)).values()) {
                if (!((Boolean) bComplete.invoke(b))) continue;
                String t = String.valueOf(bType.invoke(b));
                total++;
                types.add(t);
                byType.merge(t, 1, Integer::sum);
            }
            int level = 0, points = 0;
            Map<String, Integer> tiers = new HashMap<>();
            initTownstead();
            if (spTotals != null) {
                try {
                    Object t = spTotals.invoke(null, village);
                    points = ((Number) spTotal.invoke(t)).intValue();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) spPerSpirit.invoke(t)).entrySet()) {
                        int tier = ((Number) spTier.invoke(null, ((Number) e.getValue()).intValue())).intValue();
                        tiers.put(String.valueOf(e.getKey()), tier);
                        level = Math.max(level, tier);
                    }
                } catch (Throwable t) { spTotals = null; LOGGER.warn("[ProdigyQuests] Townstead: {}", t.toString()); }
            }
            return new VillageInfo(pop, total, types.size(), byType, level, tiers, points);
        } catch (Throwable t) {
            failed = true;   // no insistir: se registra una sola vez
            LOGGER.warn("[ProdigyQuests] no se pudo leer la aldea de MCA (las misiones de aldea quedan inactivas): {}", t.toString());
            return null;
        }
    }
}
