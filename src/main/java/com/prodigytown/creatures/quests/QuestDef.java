package com.prodigytown.creatures.quests;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Definicion de una mision (cargada desde data/<ns>/prodigy_quests/*.json). */
public final class QuestDef {
    public enum Type { KILL, TAME, RAID, MANUAL, VILLAGE }

    public final String id, title, description, category;
    public final int order, target, xp;
    public final Type type;
    /**
     * Objetivo. Admite varios terminos separados por coma; basta que coincida uno:
     * id exacto ("minecraft:zombie"), etiqueta ("#minecraft:raiders"), subcadena del id sin espacio de nombres ("*dragon*"),
     * "mod:*" (cualquier criatura de ese mod), "mod:*sub*" (subcadena dentro de ese mod) o "*" (cualquiera).
     */
    public final String entity;
    /** Los jefes dan credito a todos los jugadores cercanos, no solo a quien da el golpe final. */
    public final boolean boss;
    @Nullable public final String requires;
    public final List<ItemStack> items;
    /** Solo para el tipo VILLAGE (aldeas de MCA): "population", "buildings", "building_types", "level" o "building" (con {@link #building}). */
    public final String metric, building;
    /** Presentacion: pestana (0 Fundacion, 1 Asedios, 2 Dinosaurios, 3 Jefes), icono (id de item) y texto del objetivo. Los rellena QuestManager. */
    public int tab;
    public String iconId = "", objective = "";
    private final List<String> terms = new ArrayList<>();
    private final List<TagKey<EntityType<?>>> tags = new ArrayList<>();

    public QuestDef(String id, String title, String description, String category, int order, Type type, String entity,
                    int target, boolean boss, @Nullable String requires, List<ItemStack> items, int xp, String metric, String building) {
        this.id = id; this.title = title; this.description = description; this.category = category; this.order = order;
        this.type = type; this.entity = entity == null ? "*" : entity; this.target = Math.max(1, target); this.boss = boss;
        this.requires = requires; this.items = items; this.xp = Math.max(0, xp);
        this.metric = metric == null ? "population" : metric; this.building = building == null ? "" : building;
        for (String raw : this.entity.split(",")) {
            String term = raw.trim();
            if (term.isEmpty()) continue;
            if (term.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(term.substring(1));
                if (rl != null) tags.add(TagKey.create(Registries.ENTITY_TYPE, rl));
            } else terms.add(term);
        }
    }

    public boolean matches(EntityType<?> t) {
        if (terms.contains("*")) return true;
        for (TagKey<EntityType<?>> tag : tags) if (t.is(tag)) return true;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(t);
        if (key == null) return false;
        for (String term : terms) if (matchTerm(term, key)) return true;
        return false;
    }

    private static boolean matchTerm(String term, ResourceLocation key) {
        int colon = term.indexOf(':');
        if (colon < 0) {   // "*sub*" sin espacio de nombres: subcadena del id
            return term.length() > 2 && term.startsWith("*") && term.endsWith("*") && key.getPath().contains(term.substring(1, term.length() - 1));
        }
        String ns = term.substring(0, colon), path = term.substring(colon + 1);
        if (!ns.equals(key.getNamespace())) return false;
        if (path.equals("*")) return true;
        if (path.length() > 2 && path.startsWith("*") && path.endsWith("*")) return key.getPath().contains(path.substring(1, path.length() - 1));
        return path.equals(key.getPath());
    }
}
