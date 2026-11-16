package com.prodigytown.creatures.quests;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Definicion de una mision (cargada desde data/<ns>/prodigy_quests/*.json). */
public final class QuestDef {
    public enum Type { KILL, TAME, RAID, MANUAL }

    public final String id, title, description, category;
    public final int order, target, xp;
    public final Type type;
    /** Objetivo: id exacto ("minecraft:zombie"), etiqueta ("#minecraft:raiders"), subcadena ("*dragon*") o "*" (cualquiera). */
    public final String entity;
    /** Los jefes dan credito a todos los jugadores cercanos, no solo a quien da el golpe final. */
    public final boolean boss;
    @Nullable public final String requires;
    public final List<ItemStack> items;
    @Nullable private final TagKey<EntityType<?>> tag;

    public QuestDef(String id, String title, String description, String category, int order, Type type, String entity,
                    int target, boolean boss, @Nullable String requires, List<ItemStack> items, int xp) {
        this.id = id; this.title = title; this.description = description; this.category = category; this.order = order;
        this.type = type; this.entity = entity == null ? "*" : entity; this.target = Math.max(1, target); this.boss = boss;
        this.requires = requires; this.items = items; this.xp = Math.max(0, xp);
        TagKey<EntityType<?>> t = null;
        if (this.entity.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(this.entity.substring(1));
            if (rl != null) t = TagKey.create(Registries.ENTITY_TYPE, rl);
        }
        this.tag = t;
    }

    public boolean matches(EntityType<?> t) {
        if (entity.isEmpty() || entity.equals("*")) return true;
        if (tag != null) return t.is(tag);
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(t);
        if (key == null) return false;
        if (entity.length() > 2 && entity.startsWith("*") && entity.endsWith("*")) return key.getPath().contains(entity.substring(1, entity.length() - 1));
        return key.toString().equals(entity);
    }
}
