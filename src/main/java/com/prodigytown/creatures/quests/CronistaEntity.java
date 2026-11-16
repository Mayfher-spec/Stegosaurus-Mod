package com.prodigytown.creatures.quests;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** El Cronista de Prodigy: un NPC humanoide con cola de dinosaurio que reparte misiones y recompensas. No se puede danar ni despawnea. */
public class CronistaEntity extends PathfinderMob {
    private int talkTicks;

    public CronistaEntity(EntityType<? extends CronistaEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.45D, 0.003F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override public boolean isInvulnerableTo(DamageSource source) { return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean isPushable() { return talkTicks <= 0 && super.isPushable(); }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.talkTicks > 0) {
            this.talkTicks--;
            this.getNavigation().stop();   // se queda quieto mientras hablas con el
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
            this.talkTicks = 80;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
            this.playSound(SoundEvents.VILLAGER_YES, 0.9F, 0.7F);
            QuestData.sync(sp, this.getId());
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VILLAGER_HURT; }
    @Override public float getVoicePitch() { return 0.72F + this.random.nextFloat() * 0.08F; }
}
