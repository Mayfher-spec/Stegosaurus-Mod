package com.prodigytown.creatures;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(ProdigyCreatures.MODID)
public class ProdigyCreatures {
    public static final String MODID = "prodigycreatures";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<StegosaurusEntity>> STEGOSAURUS = ENTITIES.register("stegosaurus", () ->
        EntityType.Builder.of(StegosaurusEntity::new, MobCategory.CREATURE)
            .sized(4.0f, 5.0f).clientTrackingRange(12).build(MODID + ":stegosaurus"));

    public static final RegistryObject<Item> STEGOSAURUS_EGG = ITEMS.register("stegosaurus_spawn_egg", () ->
        new ForgeSpawnEggItem(STEGOSAURUS, 0x43533c, 0x9d4b34, new Item.Properties()));

    public ProdigyCreatures() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(STEGOSAURUS_EGG.get());
            }
        }

        /**
         * Permite la aparicion natural en terreno firme de Badlands.
         * El biome modifier (JSON) restringe los tres biomas; esta regla
         * evita usar Animal.checkAnimalSpawnRules, que suele exigir hierba
         * y bloquearia el spawn sobre arena roja o terracota.
         */
        @SubscribeEvent
        public static void registerSpawnPlacement(SpawnPlacementRegisterEvent event) {
            event.register(
                STEGOSAURUS.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                    level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.REPLACE
            );
        }

        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(STEGOSAURUS.get(), StegosaurusEntity.createAttributes().build());
        }
    }
}
