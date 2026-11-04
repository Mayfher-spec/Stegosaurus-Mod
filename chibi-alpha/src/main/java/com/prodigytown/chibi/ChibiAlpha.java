package com.prodigytown.chibi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import software.bernie.geckolib.GeckoLib;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;

@Mod(ChibiAlpha.MODID)
public class ChibiAlpha {
    public static final String MODID = "prodigychibi";
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<EntityType<ChibiStego>> STEGO =
        ENTITIES.register("stegosaurus_chibi", () ->
            EntityType.Builder.of(ChibiStego::new, MobCategory.CREATURE)
                .sized(1.65f, 1.45f).clientTrackingRange(10)
                .build(MODID + ":stegosaurus_chibi"));
    public static final RegistryObject<Item> EGG =
        ITEMS.register("stegosaurus_chibi_spawn_egg", () ->
            new ForgeSpawnEggItem(STEGO, 0x607D32, 0xE7C79D, new Item.Properties()));

    public ChibiAlpha() {
        GeckoLib.initialize();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::creative);
    }

    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(EGG.get());
    }

    @Mod.EventBusSubscriber(modid=MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Events {
        @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
            event.put(STEGO.get(), ChibiStego.attributes().build());
        }
    }
}

/** Juvenile, deliberately non-ridable. Mounting unlocks only in a future adult stage. */
class ChibiStego extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(ChibiStego.class, EntityDataSerializers.INT);
    public static final int NATURAL=0, CYBER=1, MAGMA=2, GOLD=3;
    private static final RawAnimation IDLE =
        RawAnimation.begin().thenLoop("animation.chibi_stegosaurus.idle");
    private static final RawAnimation WALK =
        RawAnimation.begin().thenLoop("animation.chibi_stegosaurus.walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ChibiStego(EntityType<? extends ChibiStego> type, Level world) {
        super(type, world);
        setMaxUpStep(0.65f);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, NATURAL);
    }

    public int variant() { return entityData.get(VARIANT); }
    public void setVariant(int variant) { entityData.set(VARIANT, Math.max(0, Math.min(GOLD, variant))); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChibiVariant", variant());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChibiVariant")) setVariant(tag.getInt("ChibiVariant"));
    }

    @Nullable @Override public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor world, DifficultyInstance difficulty,
        MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, data, tag);
        int roll = random.nextInt(100);
        setVariant(roll < 72 ? NATURAL : roll < 85 ? CYBER : roll < 95 ? MAGMA : GOLD);
        return result;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new RandomStrollGoal(this, .75));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // The baby can eat and be healed but never accepts a saddle or a rider.
        if (stack.is(Items.WHEAT) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                heal(5.0f);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (stack.is(Items.SADDLE)) return InteractionResult.PASS;
        return super.mobInteract(player, hand);
    }

    @Override protected boolean canAddPassenger(Entity passenger) { return false; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 6,
            state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
