package com.prodigytown.creatures;

import com.prodigytown.chibi.ChibiAlpha;
import com.prodigytown.chibi.ChibiStego;
import software.bernie.geckolib.GeckoLib;
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
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MODID);
    public static final RegistryObject<net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> NEST_FEATURE =
        FEATURES.register("stegosaurus_nest", () -> new com.prodigytown.creatures.worldgen.StegosaurusNestFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<net.minecraft.world.level.block.Block> NEST_BLOCK = BLOCKS.register("stegosaurus_nest", () ->
        new com.prodigytown.creatures.egg.StegosaurusNestBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .strength(0.6F).sound(net.minecraft.world.level.block.SoundType.GRASS).noOcclusion()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.prodigytown.creatures.egg.StegosaurusNestBlockEntity>> NEST_BE =
        BLOCK_ENTITIES.register("stegosaurus_nest", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
            .of(com.prodigytown.creatures.egg.StegosaurusNestBlockEntity::new, NEST_BLOCK.get()).build(null));
    public static final RegistryObject<Item> NEST_ITEM = ITEMS.register("stegosaurus_nest", () ->
        new net.minecraft.world.item.BlockItem(NEST_BLOCK.get(), new Item.Properties()));
    /** EGG_ITEMS.get(v) es el huevo de la variante adulta v (0 cibernetico, 1 dorado, 2 magma, 3 glacial, 4 natural). */
    public static final java.util.List<RegistryObject<Item>> EGG_ITEMS = new java.util.ArrayList<>();
    static {
        for (int i = 0; i < com.prodigytown.creatures.egg.StegosaurusEggs.NAMES.length; i++) {
            final int v = i;
            EGG_ITEMS.add(ITEMS.register("stegosaurus_egg_" + com.prodigytown.creatures.egg.StegosaurusEggs.NAMES[i], () ->
                new com.prodigytown.creatures.egg.StegosaurusEggItem(v, new Item.Properties().stacksTo(16))));
        }
    }

    public static final RegistryObject<EntityType<StegosaurusEntity>> STEGOSAURUS = ENTITIES.register("stegosaurus", () ->
        EntityType.Builder.of(StegosaurusEntity::new, MobCategory.CREATURE)
            .sized(4.0f, 5.0f).clientTrackingRange(12).build(MODID + ":stegosaurus"));

    public static final RegistryObject<EntityType<CarnotaurusEntity>> CARNOTAURUS = ENTITIES.register("carnotaurus", () ->
        EntityType.Builder.of(CarnotaurusEntity::new, MobCategory.CREATURE)
            .sized(2.0f, 2.7f).clientTrackingRange(10).build(MODID + ":carnotaurus"));
    public static final RegistryObject<Item> CARNOTAURUS_EGG = ITEMS.register("carnotaurus_spawn_egg", () ->
        new ForgeSpawnEggItem(() -> CARNOTAURUS.get(), 0x8E2F25, 0xE0C9A0, new Item.Properties()));
    /** Carne curada: reanima al carnotauro caido y lo cura mucho. */
    public static final RegistryObject<Item> CARNO_JERKY = ITEMS.register("carnotaurus_jerky", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> STEGOSAURUS_EGG = ITEMS.register("stegosaurus_spawn_egg", () ->
        new ProdigyStegosaurusEgg(new Item.Properties()));

    /** Ensalada de domesticacion (manzana dorada rodeada de zanahorias y papas) y pienso (alimento y crecimiento). */
    public static final RegistryObject<Item> TAMING_SALAD = ITEMS.register("stegosaurus_taming_salad", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FEED = ITEMS.register("stegosaurus_feed", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final String[] ARMOR_NAMES = {"iron", "gold", "diamond", "netherite"};
    /** ARMOR_ITEMS.get(tier - 1) es la armadura de ese nivel (1 hierro .. 4 netherite). */
    public static final java.util.List<RegistryObject<Item>> ARMOR_ITEMS = new java.util.ArrayList<>();
    static {
        for (int i = 0; i < ARMOR_NAMES.length; i++) {
            final int tier = i + 1;
            ARMOR_ITEMS.add(ITEMS.register("stegosaurus_armor_" + ARMOR_NAMES[i],
                () -> new StegosaurusArmorItem(tier, new Item.Properties().stacksTo(1))));
        }
    }

    public ProdigyCreatures() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        GeckoLib.initialize();
        com.prodigytown.creatures.net.ModNetwork.register();
        // Legacy baby registry is initialized by this SAME mod, not by a second @Mod.
        ChibiAlpha.register(bus);
        ENTITIES.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModSounds.SOUNDS.register(bus);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(STEGOSAURUS_EGG.get());
                event.accept(CARNOTAURUS_EGG.get());
                event.accept(NEST_ITEM.get());
                for (RegistryObject<Item> egg : EGG_ITEMS) event.accept(egg.get());
            }
            if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
                event.accept(TAMING_SALAD.get());
                event.accept(FEED.get());
                event.accept(CARNO_JERKY.get());
            }
            if (event.getTabKey() == CreativeModeTabs.COMBAT) {
                for (RegistryObject<Item> armor : ARMOR_ITEMS) event.accept(armor.get());
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
                    && StegosaurusEntity.roomFor(level, pos)
                    && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.REPLACE
            );
        }

        @SubscribeEvent
        public static void registerCarnoSpawn(SpawnPlacementRegisterEvent event) {
            event.register(CARNOTAURUS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    && CarnotaurusEntity.roomFor(level, pos) && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(STEGOSAURUS.get(), StegosaurusEntity.createAttributes().build());
            event.put(CARNOTAURUS.get(), CarnotaurusEntity.createAttributes().build());
            event.put(ChibiAlpha.STEGO.get(), ChibiStego.attributes().build());
        }
    }
}
