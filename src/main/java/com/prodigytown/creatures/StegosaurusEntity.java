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
import net.minecraft.world.entity.animal.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Original Stegosaurus; does not import, modify or redistribute ROR code or artwork. */
public class StegosaurusEntity extends TamableAnimal {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    private static final ResourceLocation SALAD_ID = new ResourceLocation("ror", "redwood_salad");
    public static final int NORMAL = 0, ALBINO = 1, MAGMA = 2;

    public StegosaurusEntity(EntityType<? extends StegosaurusEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 270.0)
            .add(Attributes.MOVEMENT_SPEED, 0.19)
            .add(Attributes.ATTACK_DAMAGE, 27.0)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, NORMAL);
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int variant) { this.entityData.set(VARIANT, Math.max(0, Math.min(MAGMA, variant))); }

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
        setVariant(chance < 3 ? MAGMA : chance < 12 ? ALBINO : NORMAL); // 88/9/3
        return result;
    }

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15, 7.0f, 3.0f, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.75));
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
