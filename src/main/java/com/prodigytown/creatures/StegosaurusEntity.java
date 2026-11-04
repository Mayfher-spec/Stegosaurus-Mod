package com.prodigytown.creatures;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.AnimationState;
import org.jetbrains.annotations.Nullable;

/** Original Stegosaurus; does not import, modify or redistribute ROR code or artwork. */
public class StegosaurusEntity extends TamableAnimal {
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState tailWhipAnimationState = new AnimationState();
    public final AnimationState biteAnimationState = new AnimationState();
    public final AnimationState roarAnimationState = new AnimationState();
    @Override public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.idleAnimationState.startIfStopped(this.tickCount);
            if (this.tailWhipAnimationState.isStarted() && this.tailWhipAnimationState.getAccumulatedTime() > 1800L) this.tailWhipAnimationState.stop();
            if (this.biteAnimationState.isStarted() && this.biteAnimationState.getAccumulatedTime() > 1200L) this.biteAnimationState.stop();
            if (this.roarAnimationState.isStarted() && this.roarAnimationState.getAccumulatedTime() > 3000L) this.roarAnimationState.stop();
        } else if (this.getTarget() != null && !this.isOrderedToSit() && this.tickCount % 240 == 0 && this.random.nextInt(4) == 0) {
            // Periodic roar while in combat, synced to nearby clients. Not every tick.
            this.level().broadcastEntityEvent(this, (byte) 63);
        }
    }
    @Override public void handleEntityEvent(byte id) {
        if (id == 61) this.tailWhipAnimationState.start(this.tickCount);
        else if (id == 62) this.biteAnimationState.start(this.tickCount);
        else if (id == 63) this.roarAnimationState.start(this.tickCount);
        else super.handleEntityEvent(id);
    }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    private static final ResourceLocation SALAD_ID = new ResourceLocation("ror", "redwood_salad");
    // Preserve previously saved NBT variant numbers: 0 -> cyber, 1 -> golden, 2 -> magma.
    public static final int CYBERNETIC = 0, GOLDEN = 1, MAGMA = 2, GLACIAL = 3, NATURAL = 4;
    public static final int NORMAL = CYBERNETIC, ALBINO = GOLDEN; // deprecated aliases

    public StegosaurusEntity(EntityType<? extends StegosaurusEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 270.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 27.0)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, CYBERNETIC);
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int variant) { this.entityData.set(VARIANT, Math.max(0, Math.min(NATURAL, variant))); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkinVariant", getVariant());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("SkinVariant"));
    }

    @Nullable
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType reason, @Nullable SpawnGroupData previous, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, previous, tag);
        int chance = this.random.nextInt(100);
        // New naturally-colored variant is common. Legacy IDs 0..3 remain unchanged in saved worlds.
        // 65% natural, 20% cybernetic, 9% golden, 3% magma, 3% glacial.
        setVariant(chance < 3 ? MAGMA : chance < 12 ? GOLDEN : chance < 15 ? GLACIAL : chance < 35 ? CYBERNETIC : NATURAL);
        return result;
    }

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.22, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.25, 7.0f, 3.0f, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    private boolean isSalad(ItemStack stack) {
        Item salad = BuiltInRegistries.ITEM.get(SALAD_ID);
        return salad != net.minecraft.world.item.Items.AIR && stack.is(salad);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isSalad(held)) {
            if (!this.level().isClientSide) {
                if (!this.isTame()) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.tame(player);
                    this.setOrderedToSit(false);
                    this.level().broadcastEntityEvent(this, (byte)7);
                    this.heal(this.getMaxHealth());
                } else if (this.isOwnedBy(player)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(40.0f);
                } else return InteractionResult.PASS;
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && !player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
                this.setTarget(null);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override public boolean isFood(ItemStack stack) { return isSalad(stack); }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == this.getOwner()) return false;
        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        // Defensive tail strike: slower than a rex, but heavy impact.
        boolean hit = entity.hurt(this.damageSources().mobAttack(this), damage);
        if (hit) {
            if (!this.level().isClientSide) this.level().broadcastEntityEvent(this, (byte)(this.random.nextInt(5) == 0 ? 62 : 61));
            living.knockback(1.9, this.getX()-entity.getX(), this.getZ()-entity.getZ());
        }
        return hit;
    }

    @Override public boolean isInvulnerableTo(DamageSource damage) {
        return (this.getVariant() == MAGMA && damage.is(DamageTypeTags.IS_FIRE)) || super.isInvulnerableTo(damage);
    }

    @Nullable @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }

    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // Incubation/breeding intentionally planned for v0.2.
    }
}
