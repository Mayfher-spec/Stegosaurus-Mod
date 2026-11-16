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
    private static final String PKG = "net.mca.server.world.data.";
    private static boolean failed;
    private static Method mgrGet, findNear, vPop, vBuildings, bType, bComplete;

    private McaBridge() {}

    public static boolean available() { return !failed && ModList.get().isLoaded("mca"); }

    /** Resumen de la aldea de MCA mas cercana al jugador. */
    public record VillageInfo(int population, int buildings, int buildingTypes, Map<String, Integer> byType) {
        /** Nivel de aldea de Prodigy: 1 + poblacion/4 + tipos de edificio distintos/2. */
        public int level() { return 1 + population / 4 + buildingTypes / 2; }

        public int metric(QuestDef q) {
            return switch (q.metric) {
                case "buildings" -> buildings;
                case "building_types" -> buildingTypes;
                case "level" -> level();
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
        Class<?> vm = Class.forName(PKG + "VillageManager"), village = Class.forName(PKG + "Village"), building = Class.forName(PKG + "Building");
        mgrGet = find(vm, "get", 1);
        findNear = find(vm, "findNearestVillage", 2);   // (BlockPos, margen)
        vPop = find(village, "getPopulation", 0);
        vBuildings = find(village, "getBuildings", 0);
        bType = find(building, "getType", 0);
        bComplete = find(building, "isComplete", 0);
    }

    @Nullable
    public static VillageInfo near(ServerPlayer sp) {
        if (!available()) return null;
        try {
            init();
            Object mgr = mgrGet.invoke(null, sp.serverLevel());
            Optional<?> v = (Optional<?>) findNear.invoke(mgr, sp.blockPosition(), 48);
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
            return new VillageInfo(pop, total, types.size(), byType);
        } catch (Throwable t) {
            failed = true;   // no insistir: se registra una sola vez
            LOGGER.warn("[ProdigyQuests] no se pudo leer la aldea de MCA (las misiones de aldea quedan inactivas): {}", t.toString());
            return null;
        }
    }
}
