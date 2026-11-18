package com.prodigytown.creatures.quests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** El Cronista de Prodigy: un NPC humanoide con cola de dinosaurio que reparte misiones y recompensas. No se puede danar ni despawnea. */
public class CronistaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(CronistaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WAVING = SynchedEntityData.defineId(CronistaEntity.class, EntityDataSerializers.BOOLEAN);
    private int talkTicks, waveTicks, waveCooldown = 60;
    /** Mezcla suave (0-1) de los gestos para la animacion del cliente: actual y del tick anterior. */
    private float talkBlend, prevTalkBlend, waveBlend, prevWaveBlend;
    /** Hogar: tras la mision del hogar se muda a su cama, duerme de noche y no se aleja mas de {@link #HOME_RADIUS} bloques. */
    private static final int HOME_RADIUS = 14;
    private boolean settled;
    @Nullable private BlockPos home;
    /** Visitante: tiempo restante en ticks antes de marcharse (-1 = permanente: huevo, comando o ya instalada). */
    private int visitTicks = -1;
    private final List<UUID> companions = new ArrayList<>();

    public CronistaEntity(EntityType<? extends CronistaEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (this.getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);   // abre puertas como un aldeano
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.38).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TALKING, false);
        this.entityData.define(WAVING, false);
    }

    public boolean isTalking() { return this.entityData.get(TALKING); }
    public boolean isWaving() { return this.entityData.get(WAVING); }
    public float talkBlend(float partial) { return Mth.lerp(partial, prevTalkBlend, talkBlend); }
    public float waveBlend(float partial) { return Mth.lerp(partial, prevWaveBlend, waveBlend); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new SleepAtHomeGoal());
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.7D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
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
        if (this.level().isClientSide) return;
        if (this.visitTicks > 0 && --this.visitTicks == 0) {   // el visitante se marcha, pero no mientras alguien esta cerca hablando con ella
            if (this.talkTicks > 0 || this.level().getNearestPlayer(this, 12.0) != null) this.visitTicks = 200;
            else { farewell(); return; }
        }
        if (this.isSleeping() && !(this.settled && this.home != null && isBedtime(this.level()))) this.stopSleeping();
        if (this.settled && this.home != null && this.tickCount % 200 == 0 && !this.isSleeping()
                && this.distanceToSqr(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5) > 48.0 * 48.0
                && this.level() instanceof ServerLevel sl && sl.isLoaded(this.home)) {   // lejos de casa: vuelve de golpe
            this.teleportTo(this.home.getX() + 0.5, this.home.getY() + 1.0, this.home.getZ() + 0.5);
            this.getNavigation().stop();
        }
        if (this.talkTicks > 0) {
            this.talkTicks--;
            this.getNavigation().stop();   // se queda quieto mientras hablas con el
        }
        // saluda con la mano cuando alguien se acerca (con enfriamiento para que no sea pesada)
        if (this.waveCooldown > 0) this.waveCooldown--;
        if (this.waveTicks > 0) {
            this.waveTicks--;
            this.getNavigation().stop();
        } else if (this.waveCooldown == 0 && this.talkTicks == 0 && this.tickCount % 10 == 0) {
            Player near = this.level().getNearestPlayer(this, 6.0);
            if (near != null && !near.isSpectator() && this.hasLineOfSight(near)) {
                this.waveTicks = 55;
                this.waveCooldown = 500 + this.random.nextInt(400);
                this.getLookControl().setLookAt(near, 40.0F, 40.0F);
            }
        }
        boolean t = this.talkTicks > 0, w = this.waveTicks > 0;
        if (t != this.isTalking()) this.entityData.set(TALKING, t);
        if (w != this.isWaving()) this.entityData.set(WAVING, w);
        if (!this.getMainHandItem().isEmpty()) this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);   // versiones antiguas llevaban un libro
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {   // los gestos entran y salen suavemente, sin saltos
            prevTalkBlend = talkBlend; prevWaveBlend = waveBlend;
            talkBlend = Mth.approach(talkBlend, isTalking() ? 1.0F : 0.0F, 0.12F);
            waveBlend = Mth.approach(waveBlend, isWaving() && !isTalking() ? 1.0F : 0.0F, 0.12F);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
            if (this.isSleeping()) this.stopSleeping();
            this.talkTicks = 80;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
            this.playSound(com.prodigytown.creatures.ModSounds.CRONISTA_HELLO.get(), 1.0F, this.getVoicePitch());
            QuestData.sync(sp, this.getId());
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // ------------------------------------------------------------------ visitas y hogar

    public boolean isSettled() { return settled; }
    public boolean isVisitor() { return visitTicks >= 0; }

    /** La Cronista llega de visita (como el mercader errante): se queda {@code ticks} y luego se marcha si nadie le ha dado un hogar. */
    public void makeVisitor(int ticks) { this.visitTicks = ticks; }

    public void addCompanion(UUID id) { this.companions.add(id); }

    /** Se muda a la cama indicada: ya no se marcha, duerme de noche y se queda cerca. El Compy se queda como regalo (domado, sin correa, con 1 o 2 compys mas como soldados); un caballo de partidas antiguas, domado y ensillado. */
    public void settle(BlockPos bed, @Nullable ServerPlayer owner) {
        this.settled = true;
        this.home = bed.immutable();
        this.visitTicks = -1;
        this.restrictTo(this.home, HOME_RADIUS);
        releaseCompanions(false, owner);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 14, 0.4, 0.6, 0.4, 0.02);
            this.playSound(com.prodigytown.creatures.ModSounds.CRONISTA_HAPPY.get(), 1.0F, this.getVoicePitch());
        }
    }

    private void releaseCompanions(boolean discard, @Nullable ServerPlayer owner) {
        if (this.level() instanceof ServerLevel sl) {
            for (UUID id : this.companions) {
                Entity e = sl.getEntity(id);
                if (e instanceof Mob m) {
                    if (m.isLeashed()) m.dropLeash(true, false);
                    if (!discard && owner != null && m instanceof AbstractHorse horse) { horse.tameWithName(owner); horse.equipSaddle(SoundSource.NEUTRAL); }
                    if (!discard && owner != null && m instanceof com.prodigytown.creatures.CompyEntity compy) compy.giftTo(owner, 1 + this.random.nextInt(2));
                    if (discard && m instanceof com.prodigytown.creatures.CompyEntity tamed && tamed.isTame()) continue;   // el que ya domaste es tuyo: no se va con ella
                    if (discard) { sl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.8, m.getZ(), 10, 0.3, 0.4, 0.3, 0.02); m.discard(); }
                }
            }
        }
        this.companions.clear();
    }

    private void farewell() {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.3, 0.7, 0.3, 0.03);
            for (ServerPlayer sp : sl.players()) {
                if (sp.distanceToSqr(this) < 96.0 * 96.0) sp.sendSystemMessage(Component.literal("El Cronista Mutante sigue su camino. Volverá a visitarte si aún no tiene un hogar.").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            releaseCompanions(true, null);
        }
        this.discard();
    }

    private static boolean isBedtime(Level level) {
        if (level.dimension() != Level.OVERWORLD) return false;
        long t = level.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    private boolean wantsSleep() {
        return this.settled && this.home != null && this.talkTicks == 0 && isBedtime(this.level()) && this.level().getBlockState(this.home).getBlock() instanceof BedBlock;
    }

    @Override
    public void startSleeping(BlockPos pos) {
        super.startSleeping(pos);
        this.setPos(pos.getX() + 0.5D, pos.getY() + 0.6875D, pos.getZ() + 0.5D);   // tumbada sobre la cama
    }

    @Override protected boolean isImmobile() { return super.isImmobile() || this.isSleeping(); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Settled", this.settled);
        if (this.home != null) tag.putLong("Home", this.home.asLong());
        tag.putInt("VisitTicks", this.visitTicks);
        ListTag list = new ListTag();
        for (UUID id : this.companions) list.add(NbtUtils.createUUID(id));
        tag.put("Companions", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.settled = tag.getBoolean("Settled");
        this.home = tag.contains("Home", 4) ? BlockPos.of(tag.getLong("Home")) : null;
        this.visitTicks = tag.contains("VisitTicks", 3) ? tag.getInt("VisitTicks") : -1;
        this.companions.clear();
        for (Tag t : tag.getList("Companions", 11)) this.companions.add(NbtUtils.loadUUID(t));
        if (this.settled && this.home != null) this.restrictTo(this.home, HOME_RADIUS);   // la restriccion no se guarda sola
    }

    /** Camina hasta su cama al caer la noche y se acuesta; por la manana (o si alguien le habla) se levanta. */
    private final class SleepAtHomeGoal extends Goal {
        SleepAtHomeGoal() { this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }

        @Override public boolean canUse() { return wantsSleep(); }
        @Override public boolean canContinueToUse() { return wantsSleep(); }
        @Override public void start() { goToBed(); }

        private void goToBed() {
            if (home != null) getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8D);
        }

        @Override
        public void tick() {
            if (home == null || isSleeping()) return;
            if (distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) < 4.0) {
                getNavigation().stop();
                startSleeping(home);
            } else if (getNavigation().isDone()) goToBed();
        }

        @Override public void stop() { if (isSleeping()) stopSleeping(); }
    }

    // voz propia (sin sonidos de aldeano): saluda al hablarle, celebra al instalarse, se queja al recibir dano
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.prodigytown.creatures.ModSounds.CRONISTA_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return com.prodigytown.creatures.ModSounds.CRONISTA_DEATH.get(); }
    @Override public float getVoicePitch() { return 0.97F + this.random.nextFloat() * 0.06F; }
}
