package com.prodigytown.chibi;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Compatibility registry for the baby. This is NOT another @Mod.
 * Retains prodigychibi:stegosaurus_chibi so existing worlds load.
 * One real mod entrypoint: prodigycreatures.
 */
public final class ChibiAlpha {
    public static final String LEGACY_NAMESPACE = "prodigychibi";
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LEGACY_NAMESPACE);
    public static final DeferredRegister<Item> LEGACY_ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, LEGACY_NAMESPACE);

    public static final RegistryObject<EntityType<ChibiStego>> STEGO =
        ENTITIES.register("stegosaurus_chibi", () ->
            EntityType.Builder.of(ChibiStego::new, MobCategory.CREATURE)
                .sized(1.04F, 1.24F).clientTrackingRange(10)
                .build(LEGACY_NAMESPACE + ":stegosaurus_chibi"));

    // Saved legacy eggs remain valid, but do not appear in the creative menu.
    public static final RegistryObject<Item> LEGACY_EGG =
        LEGACY_ITEMS.register("stegosaurus_chibi_spawn_egg", () ->
            new ForgeSpawnEggItem(STEGO, 0x607D32, 0xE7C79D, new Item.Properties()));

    private ChibiAlpha() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        LEGACY_ITEMS.register(modBus);
    }
}
