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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Standalone model/animation, single unified Forge mod entrypoint.
 * Keeps original baby NBT "ChibiVariant" and 0..3 variant IDs.
 */
public class ChibiStego extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(ChibiStego.class, EntityDataSerializers.INT);
    public static final int NATURAL = 0, CYBER = 1, MAGMA = 2, GOLD = 3;

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop("animation.chibi_stegosaurus." + name);
    }
    private static RawAnimation play(String name) {
        return RawAnimation.begin().thenPlay("animation.chibi_stegosaurus." + name);
    }

    private static final RawAnimation IDLE = loop("idle");
    private static final RawAnimation WALK = loop("walk");
    private static final RawAnimation LOOK = play("look_around");
    private static final RawAnimation SNIFF = play("sniff");
    private static final RawAnimation STARTLE = play("startle");
    private static final RawAnimation STOMP = play("stomp");
    private static final RawAnimation YAWN = play("yawn");
    private static final RawAnimation TAIL_SWIPE = play("tail_swipe");
    private static final RawAnimation SLEEP = loop("sleep");
    private static final RawAnimation HAPPY = play("happy");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ChibiStego(EntityType<? extends ChibiStego> type, Level world) {
        super(type, world);
        setMaxUpStep(0.65F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.18D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, NATURAL);
    }

    public int variant() { return entityData.get(VARIANT); }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.max(NATURAL, Math.min(GOLD, v)));
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChibiVariant", variant());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChibiVariant")) setVariant(tag.getInt("ChibiVariant"));
    }

    @Nullable @Override public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData old, @Nullable CompoundTag nbt) {
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, old, nbt);
        int chance = random.nextInt(100);
        setVariant(chance < 72 ? NATURAL : chance < 85 ? CYBER : chance < 95 ? MAGMA : GOLD);
        return result;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new RandomStrollGoal(this, 0.75));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.WHEAT) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                heal(5.0F);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (stack.is(Items.SADDLE)) return InteractionResult.PASS;
        return super.mobInteract(player, hand);
    }

    @Override protected boolean canAddPassenger(Entity passenger) { return false; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 2, state -> {
            // Walking always wins: do not replace gait with idle personality clips while moving.
            boolean moving = state.isMoving() || getDeltaMovement().horizontalDistanceSqr() > 0.0005D;
            if (moving) return state.setAndContinue(WALK);
            if (hurtTime > 0) return state.setAndContinue(STARTLE);

            int phase = Math.floorMod(tickCount + getId() * 37, 600);
            if (variant() == GOLD) {
                if (phase >= 190 && phase < 325) return state.setAndContinue(SLEEP);
                if (phase >= 175 && phase < 190) return state.setAndContinue(YAWN);
            }
            if (variant() == MAGMA) {
                if (phase >= 120 && phase < 145) return state.setAndContinue(STOMP);
                if (phase >= 360 && phase < 386) return state.setAndContinue(TAIL_SWIPE);
            }
            if (variant() == CYBER) {
                if (phase >= 100 && phase < 118) return state.setAndContinue(STARTLE);
                if (phase >= 315 && phase < 360) return state.setAndContinue(LOOK);
                if (phase >= 440 && phase < 480) return state.setAndContinue(SNIFF);
            }
            if (variant() == NATURAL) {
                if (phase >= 100 && phase < 150) return state.setAndContinue(LOOK);
                if (phase >= 290 && phase < 325) return state.setAndContinue(SNIFF);
                if (phase >= 475 && phase < 510) return state.setAndContinue(HAPPY);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
