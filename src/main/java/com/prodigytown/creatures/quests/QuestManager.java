package com.prodigytown.creatures.quests;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga las misiones desde los datapacks: data/&lt;namespace&gt;/prodigy_quests/&lt;id&gt;.json. Asi se pueden anadir o cambiar misiones sin tocar el codigo.
 * <pre>
 * { "title": "...", "description": "...", "category": "Jefes", "order": 100,
 *   "type": "kill" | "tame" | "raid" | "manual" | "village", "entity": "minecraft:wither" (o "#tag", "*dragon*", "mod:*", varios con coma), "count": 1, "boss": true,
 *   "requires": "otra_mision", "requires_mod": "iceandfire" (varios con coma: basta uno),
 *   (village, requiere MCA) "metric": "population" | "buildings" | "building_types" | "level" | "building", "building": "blacksmith",
 *   "rewards": { "xp": 500, "items": [ { "item": "minecraft:diamond", "count": 4 } ] } }
 * </pre>
 */
public final class QuestManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile List<QuestDef> QUESTS = List.of();
    private static volatile Map<String, QuestDef> BY_ID = Map.of();

    public QuestManager() { super(new Gson(), "prodigy_quests"); }

    public static List<QuestDef> all() { return QUESTS; }
    @Nullable public static QuestDef get(String id) { return BY_ID.get(id); }

    /** Las misiones del propio mod usan solo la ruta como id ("zombis"); las de otros espacios, el id completo. */
    private static String idOf(ResourceLocation rl) { return rl.getNamespace().equals("prodigycreatures") ? rl.getPath() : rl.toString(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        List<QuestDef> list = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : map.entrySet()) {
            try {
                QuestDef d = parse(idOf(e.getKey()), GsonHelper.convertToJsonObject(e.getValue(), "quest"));
                if (d != null) list.add(d);
            } catch (RuntimeException ex) {
                LOGGER.warn("[ProdigyQuests] mision invalida {}: {}", e.getKey(), ex.getMessage());
            }
        }
        list.sort(Comparator.comparingInt((QuestDef q) -> q.order).thenComparing(q -> q.id));
        Map<String, QuestDef> by = new LinkedHashMap<>();
        for (QuestDef q : list) by.put(q.id, q);
        QUESTS = List.copyOf(list);
        BY_ID = by;
        LOGGER.info("[ProdigyQuests] {} misiones cargadas", list.size());
    }

    @Nullable
    private static QuestDef parse(String id, JsonObject o) {
        String mod = GsonHelper.getAsString(o, "requires_mod", "");   // uno o varios ids separados por coma: basta que este cargado uno
        if (!mod.isEmpty()) {
            boolean any = false;
            for (String m : mod.split(",")) if (!m.trim().isEmpty() && ModList.get().isLoaded(m.trim())) any = true;
            if (!any) return null;
        }
        QuestDef.Type type = QuestDef.Type.valueOf(GsonHelper.getAsString(o, "type", "manual").toUpperCase(java.util.Locale.ROOT));
        List<ItemStack> items = new ArrayList<>();
        int xp = 0;
        if (o.has("rewards")) {
            JsonObject r = GsonHelper.getAsJsonObject(o, "rewards");
            xp = GsonHelper.getAsInt(r, "xp", 0);
            if (r.has("items")) {
                for (JsonElement el : GsonHelper.getAsJsonArray(r, "items")) {
                    JsonObject io = GsonHelper.convertToJsonObject(el, "item");
                    ResourceLocation rl = new ResourceLocation(GsonHelper.getAsString(io, "item"));
                    if (!ForgeRegistries.ITEMS.containsKey(rl)) { LOGGER.warn("[ProdigyQuests] {}: item desconocido {}", id, rl); continue; }
                    Item item = ForgeRegistries.ITEMS.getValue(rl);
                    items.add(new ItemStack(item, Math.max(1, GsonHelper.getAsInt(io, "count", 1))));
                }
            }
        }
        return new QuestDef(id, GsonHelper.getAsString(o, "title"), GsonHelper.getAsString(o, "description", ""),
            GsonHelper.getAsString(o, "category", "General"), GsonHelper.getAsInt(o, "order", 1000), type,
            GsonHelper.getAsString(o, "entity", "*"), GsonHelper.getAsInt(o, "count", 1), GsonHelper.getAsBoolean(o, "boss", false),
            o.has("requires") ? GsonHelper.getAsString(o, "requires") : null, List.copyOf(items), xp,
            GsonHelper.getAsString(o, "metric", "population"), GsonHelper.getAsString(o, "building", ""));
    }
}
