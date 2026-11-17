package com.prodigytown.creatures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;

/**
 * Compy (Compsognathus): el cuarto dinosaurio. Diminuto (~1,1 bloques de largo, tamano de pollo), vive en manadas de 3 a 8 y no se puede montar.
 * UNA sola malla y ocho variantes de textura (verde, rojo, azul, gris, amarillo, rosa, albino, melanico).
 * MANADAS: cada grupo tiene UN lider (mas grande, con cresta de plumas y gorguera de color). Solo el lider se puede domar; al hacerlo, el resto de la
 * manada se une como sus SOLDADOS: no tienen interfaz propia, su orden queda bloqueada en "seguidor" (siguen al lider y copian Quieto/Sentarse/Dormir),
 * atacan lo que ataca el lider y, si el lider cae, el soldado mas cercano asciende a lider. Habilidades, talentos y HUD llegan despues.
 */
public class CompyEntity extends TamableAnimal implements PetOrders.Holder {
    public static final String[] VARIANT_IDS = {"verde", "rojo", "azul", "gris", "amarillo", "rosa", "albino", "melanico"};
    public static final String[] VARIANT_NAMES = {"Verde", "Rojo", "Azul", "Gris", "Amarillo", "Rosa", "Albino", "Melánico"};
    public static final int VERDE = 0, ROJO = 1, AZUL = 2, GRIS = 3, AMARILLO = 4, ROSA = 5, ALBINO = 6, MELANICO = 7, VARIANTS = 8;
    public static final int BITE_LEN = 10, EAT_LEN = 36;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.INT);
    /** Lider de su manada (solo uno por manada): mas grande, con distintivos, domable y con interfaz propia. */
    private static final EntityDataAccessor<Boolean> LEADER = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.world.entity.EntityDimensions LEADER_DIM = net.minecraft.world.entity.EntityDimensions.scalable(0.48F, 0.6F);
    /** Tamano del lider respecto al resto de la manada. */
    public static final float LEADER_SCALE = 1.2F;

    // ---- solo cliente: estado de animacion suavizado (cada valor guarda su version del tick anterior ...O para interpolar) ----
    public float sitAnim, sitAnimO, sleepAnim, sleepAnimO;
    public float gait, gaitO, speedAnim, speedAnimO, runAnim, runAnimO, aggroAnim, aggroAnimO, turnAnim, turnAnimO, airAnim, airAnimO;
    public float airVel;
    /** Mordida visual: ticks desde que empezo (-1 = sin golpe). Comer visual: idem. */
    public int biteT = -1, biteTO = -1, eatT = -1, eatTO = -1;

    @Nullable private BlockPos orderHome;
    private int trust, lastFeedTick = -100000;
    private boolean variantFixed = false, syncingOrder = false;
    /** Manada a la que pertenece (los miembros comparten id). Solo servidor. */
    private java.util.UUID packId = java.util.UUID.randomUUID();
    @Nullable private CompyEntity leaderRef;
    private int leaderCheckAt = 0;

    public CompyEntity(EntityType<? extends CompyEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.moveControl = new SmoothMoveControl(this, 16.0F);
        this.lookControl = new PetOrders.PetLookControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    // ------------------------------------------------------------------ datos

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, VERDE);
        this.entityData.define(ORDER, PetOrders.FOLLOW);
        this.entityData.define(LEADER, Boolean.TRUE);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Trust", trust);
        tag.putInt("PetOrder", getOrder());
        tag.putBoolean("Leader", isLeader());
        tag.putUUID("PackId", packId);
        if (orderHome != null) tag.putLong("OrderHome", orderHome.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) { setVariant(tag.getInt("Variant")); variantFixed = true; }   // /summon ... {Variant:3} o huevo con variante concreta
        this.trust = tag.getInt("Trust");
        this.entityData.set(ORDER, PetOrders.clamp(tag.getInt("PetOrder")));
        if (tag.contains("Leader")) this.entityData.set(LEADER, tag.getBoolean("Leader"));
        if (tag.hasUUID("PackId")) this.packId = tag.getUUID("PackId");
        applyRoleStats(false);
        this.orderHome = tag.contains("OrderHome") ? BlockPos.of(tag.getLong("OrderHome")) : null;
        if (isTame()) this.setPersistenceRequired();
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int v) { this.entityData.set(VARIANT, Math.max(0, Math.min(VARIANTS - 1, v))); }
    public int getTrust() { return trust; }
    public boolean isLeader() { return this.entityData.get(LEADER); }
    /** Soldado: domado pero no lider (su orden esta bloqueada en "seguidor" del lider). */
    public boolean isFollower() { return isTame() && !isLeader(); }
    public boolean isSyncingOrder() { return syncingOrder; }
    public java.util.UUID getPackId() { return packId; }
    private void setLeader(boolean leader) { this.entityData.set(LEADER, leader); applyRoleStats(true); }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LEADER.equals(key)) this.refreshDimensions();
    }
    @Override public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) { return isLeader() ? LEADER_DIM : super.getDimensions(pose); }

    /** Vida segun el papel: lider 14 (20 domado), soldado 10 (12 domado). Conserva la fraccion de vida. */
    private void applyRoleStats(boolean keepFraction) {
        if (this.level().isClientSide) return;
        double max = isLeader() ? (isTame() ? 20.0 : 14.0) : (isTame() ? 12.0 : 10.0);
        var attr = this.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null || attr.getBaseValue() == max) return;
        float frac = keepFraction ? this.getHealth() / this.getMaxHealth() : 1.0F;
        attr.setBaseValue(max);
        this.setHealth(Math.max(1.0F, (float) max * Math.min(1.0F, frac)));
    }

    /** El lider de mi manada (el mas cercano con mi id), con cache. Solo servidor. */
    @Nullable public CompyEntity findLeader() {
        if (isLeader()) return this;
        if (leaderRef != null && leaderRef.isAlive() && leaderRef.isLeader() && leaderRef.packId.equals(packId) && !leaderRef.isRemoved()) return leaderRef;
        if (this.tickCount < leaderCheckAt) return null;
        leaderCheckAt = this.tickCount + 20;
        leaderRef = null;
        double best = Double.MAX_VALUE;
        for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(64.0), e -> e != this && e.isLeader() && e.isAlive() && e.packId.equals(packId))) {
            double d = this.distanceToSqr(o);
            if (d < best) { best = d; leaderRef = o; }
        }
        return leaderRef;
    }

    /** Al domar al lider, toda la manada cercana se une a su dueno como soldados. */
    private void recruitPack(Player owner) {
        for (CompyEntity m : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(64.0), e -> e != this && !e.isTame() && e.isAlive() && e.packId.equals(packId))) {
            m.tame(owner);
            m.setPersistenceRequired();
            m.setTarget(null);
            m.leaderRef = this;
            m.applyRoleStats(false);
            m.setPackOrder(PetOrders.FOLLOW);
            m.level().broadcastEntityEvent(m, (byte) 7);
        }
    }

    /** Orden de grupo (solo la fija el propio sistema de la manada: los soldados no aceptan ordenes sueltas). */
    private void setPackOrder(int order) {
        syncingOrder = true;
        try { PetOrders.setOrder(this, order); } finally { syncingOrder = false; }
    }

    /** Si el lider muere, el soldado mas cercano asciende (siempre hay un solo lider por manada). */
    private void promoteSuccessor() {
        CompyEntity best = null; double bd = Double.MAX_VALUE;
        for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(64.0), e -> e != this && !e.isLeader() && e.isAlive() && e.packId.equals(packId))) {
            double d = this.distanceToSqr(o);
            if (d < bd) { bd = d; best = o; }
        }
        if (best == null) return;
        best.setLeader(true);
        best.leaderRef = null;
        if (best.isTame()) {
            best.setPackOrder(this.getOrder());
            if (best.getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Un soldado asciende a líder de la manada"), true);
        }
        this.level().broadcastEntityEvent(best, (byte) 7);
    }

    @Override public void die(DamageSource source) {
        if (!this.level().isClientSide && isLeader()) promoteSuccessor();
        super.die(source);
    }

    // PetOrders.Holder (el Compy no "cae": si muere, muere)
    @Override public int getOrder() { return this.entityData.get(ORDER); }
    @Override public void setOrderRaw(int order) { this.entityData.set(ORDER, order); }
    @Override public boolean isFallen() { return false; }
    @Override public void setFallenRaw(boolean fallen) { }
    @Nullable @Override public BlockPos orderHome() { return orderHome; }
    @Override public void setOrderHome(@Nullable BlockPos pos) { this.orderHome = pos; }

    /** Ojos cerrados: durmiendo, muriendo y en un parpadeo breve cada ~4 s (cada animal en un momento distinto). */
    public boolean eyesClosed() {
        if (getOrder() == PetOrders.SLEEP && isTame() || this.deathTime > 0) return true;
        return (this.tickCount + this.getId() * 17) % 90 < 3;
    }

    // ------------------------------------------------------------------ aparicion y variantes

    /** Los miembros de una manada comparten la variante de su bioma (cada uno tira aparte las raras). */
    private static final class PackData implements SpawnGroupData {
        final int base; final java.util.UUID id = java.util.UUID.randomUUID();
        PackData(int base) { this.base = base; }
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        PackData pd;
        if (spawnData instanceof PackData existing) { pd = existing; this.packId = pd.id; this.entityData.set(LEADER, false); }   // el resto de la manada: soldados
        else { pd = new PackData(baseVariantFor(level.getBiome(this.blockPosition()))); data = pd; this.packId = pd.id; this.entityData.set(LEADER, true); }   // el primero: lider
        if (!variantFixed) setVariant(rareRoll(level, pd.base));
        applyRoleStats(false);
        return data;
    }

    /** Variante de cada bioma: bosques verde, selvas roja, rios y humedales azul, montanas gris, sabanas amarilla, biomas florales rosa. */
    public static int baseVariantFor(Holder<Biome> b) {
        if (b.is(BiomeTags.IS_JUNGLE)) return ROJO;
        if (b.is(BiomeTags.IS_RIVER) || b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP)) return AZUL;
        if (b.is(BiomeTags.IS_MOUNTAIN)) return GRIS;
        if (b.is(BiomeTags.IS_SAVANNA)) return AMARILLO;
        if (b.is(Biomes.FLOWER_FOREST) || b.is(Biomes.MEADOW) || b.is(Biomes.CHERRY_GROVE) || b.is(Biomes.SUNFLOWER_PLAINS)) return ROSA;
        return VERDE;
    }

    /** Albino: rarisimo (mas durante una tormenta). Melanico: rarisimo y solo de noche. */
    private int rareRoll(ServerLevelAccessor level, int base) {
        Level lv = level.getLevel();
        int albino = lv.isThundering() ? CompyConfig.get(CompyConfig.ALBINO_STORM_PERCENT, 15) : CompyConfig.get(CompyConfig.ALBINO_PERCENT, 2);
        if (this.random.nextInt(100) < albino) return ALBINO;
        if (lv.isNight() && this.random.nextInt(100) < CompyConfig.get(CompyConfig.MELANIC_NIGHT_PERCENT, 4)) return MELANICO;
        return base;
    }

    public static boolean roomFor(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    // ------------------------------------------------------------------ IA

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D) {
            @Override public boolean canUse() { return !isTame() && !CompyEntity.this.isAggressive() && super.canUse(); }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, Ingredient.of(Items.CHICKEN, Items.RABBIT, Items.COD, Items.SALMON, Items.BEEF, Items.PORKCHOP, Items.MUTTON), false) {
            @Override public boolean canUse() { return !isTame() && super.canUse(); }
        });
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.15D, 3.5F, 10.0F, false) {
            @Override public boolean canUse() { return getOrder() == PetOrders.FOLLOW && (!isFollower() || findLeader() == null) && super.canUse(); }
            @Override public boolean canContinueToUse() { return getOrder() == PetOrders.FOLLOW && (!isFollower() || findLeader() == null) && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(4, new FollowLeaderGoal());
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new PetOrders.PatrolGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7D) {
            @Override public boolean canUse() { return (!isTame() || getOrder() == PetOrders.WANDER && isLeader()) && super.canUse(); }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {   // la manada acude en ayuda de un compy salvaje herido
            @Override public boolean canUse() { return !isTame() && super.canUse(); }
        }.setAlertOthers());
        this.targetSelector.addGoal(4, PetOrders.patrolTargeting(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Animal.class, 30, true, false, e -> e instanceof Chicken || e instanceof Rabbit) {
            @Override public boolean canUse() { return !isTame() && CompyConfig.get(CompyConfig.HUNT_SMALL_PREY, true) && super.canUse(); }
        });
    }

    /** La manada se mantiene unida: los soldados (salvajes o domados) siguen a su lider cuando se alejan. */
    private final class FollowLeaderGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private int recalc;
        FollowLeaderGoal() { this.setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        private boolean away(double min) {
            if (isLeader() || isOrderedToSit() || isPassenger()) return false;
            if (isTame() && getOrder() != PetOrders.FOLLOW) return false;
            CompyEntity l = findLeader();
            return l != null && distanceToSqr(l) > min * min;
        }
        @Override public boolean canUse() { return away(isTame() ? 4.5 : 10.0); }
        @Override public boolean canContinueToUse() { return away(isTame() ? 2.8 : 5.0) && getTarget() == null; }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { getNavigation().stop(); }
        @Override public void tick() {
            CompyEntity l = findLeader();
            if (l == null) return;
            getLookControl().setLookAt(l, 10.0F, getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 8;
            if (isTame() && distanceToSqr(l) > 24.0 * 24.0) { this.teleportTo(l.getX(), l.getY(), l.getZ()); return; }
            getNavigation().moveTo(l, isTame() ? 1.25D : 1.0D);
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        this.level().broadcastEntityEvent(this, (byte) 67);
        this.playSound(ModSounds.COMPY_BITE.get(), 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
        float dmg = (float) CompyConfig.get(CompyConfig.BITE_DAMAGE, 2.0);
        boolean ok = target.hurt(this.damageSources().mobAttack(this), isTame() ? dmg * 1.5F : dmg);
        if (ok && target instanceof LivingEntity le && !le.isAlive()) { this.heal(2.0F); this.level().broadcastEntityEvent(this, (byte) 66); }
        return ok;
    }

    @Override public void handleEntityEvent(byte id) {
        if (id == 67) { this.biteT = 0; this.biteTO = 0; }
        else if (id == 66) { this.eatT = 0; this.eatTO = 0; }
        else super.handleEntityEvent(id);
    }

    @Override public void tick() {
        super.tick();
        if (this.level().isClientSide) animTick();
        else { PetOrders.serverTick(this); packTick(); }
    }

    /** Solo servidor: los soldados copian la orden del lider y atacan lo que el ataca; un solo lider por manada. */
    private void packTick() {
        if (this.tickCount % 10 != 3) return;
        if (isFollower()) {
            CompyEntity l = findLeader();
            if (l == null) return;
            int want = l.getOrder();
            if (want == PetOrders.WANDER || want == PetOrders.PATROL) want = PetOrders.FOLLOW;   // el lider pasea; los soldados lo acompanan
            if (getOrder() != want) setPackOrder(want);
            LivingEntity t = l.getTarget();
            if (getTarget() == null && t != null && t.isAlive() && t != this && !(t instanceof Player) && !(t instanceof TamableAnimal ta && ta.isTame()) && getOrder() != PetOrders.SLEEP) setTarget(t);
        } else if (isLeader() && this.tickCount % 100 == 3) {
            for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(48.0), e -> e != this && e.isLeader() && e.packId.equals(packId)))
                if (o.getUUID().compareTo(this.getUUID()) < 0) { setLeader(false); break; }   // por si dos lideres coinciden: queda uno solo
        }
    }

    private static float approach(float v, float to, float rate) { return v < to ? Math.min(to, v + rate) : Math.max(to, v - rate); }

    private void animTick() {
        sitAnimO = sitAnim; sleepAnimO = sleepAnim; gaitO = gait; speedAnimO = speedAnim; runAnimO = runAnim; aggroAnimO = aggroAnim; turnAnimO = turnAnim; airAnimO = airAnim;
        int o = getOrder(); boolean tame = isTame();
        sitAnim = approach(sitAnim, tame && o == PetOrders.SIT ? 1.0F : 0.0F, 0.10F);
        sleepAnim = approach(sleepAnim, tame && o == PetOrders.SLEEP ? 1.0F : 0.0F, 0.06F);
        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        float sp = (float) Math.sqrt(dx * dx + dz * dz);
        speedAnim += (sp - speedAnim) * 0.4F;
        if (this.onGround()) gait += speedAnim * 7.0F;
        if (gait > 628.3F) { gait -= 628.3F; gaitO -= 628.3F; }
        runAnim = approach(runAnim, speedAnim > 0.19F ? 1.0F : 0.0F, 0.15F);
        aggroAnim = approach(aggroAnim, this.isAggressive() ? 1.0F : 0.0F, 0.12F);
        turnAnim += (Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO) - turnAnim) * 0.3F;
        airAnim = approach(airAnim, !this.onGround() && !this.isInWater() && !this.isPassenger() ? 1.0F : 0.0F, 0.25F);
        airVel = (float) this.getDeltaMovement().y;
        biteTO = biteT; if (biteT >= 0 && ++biteT > BITE_LEN) biteT = -1;
        eatTO = eatT; if (eatT >= 0 && ++eatT > EAT_LEN) eatT = -1;
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && isTame() && getOrder() == PetOrders.SLEEP) {
            net.minecraft.world.phys.Vec3 mv = this.getDeltaMovement();
            this.setDeltaMovement(0.0, mv.y, 0.0);
        }
    }

    @Override public boolean isPushable() { return super.isPushable() && !(isTame() && getOrder() == PetOrders.SLEEP); }

    // ------------------------------------------------------------------ interaccion y domesticacion

    public static boolean isMeat(ItemStack s) {
        return s.is(Items.CHICKEN) || s.is(Items.RABBIT) || s.is(Items.COD) || s.is(Items.SALMON) || s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON)
            || s.is(Items.COOKED_CHICKEN) || s.is(Items.COOKED_RABBIT) || s.is(Items.COOKED_COD) || s.is(Items.COOKED_SALMON) || s.is(Items.COOKED_BEEF) || s.is(Items.COOKED_PORKCHOP) || s.is(Items.COOKED_MUTTON);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = this.level().isClientSide;
        if (!this.isTame()) {
            if (!isMeat(held)) return InteractionResult.PASS;
            if (!isLeader()) {
                if (!client) player.displayClientMessage(Component.literal("Solo el líder de la manada (con cresta) puede confiar en ti"), true);
                return InteractionResult.sidedSuccess(client);
            }
            if (!client) feedWild(player, held);
            return InteractionResult.sidedSuccess(client);
        }
        if (!this.isOwnedBy(player)) return super.mobInteract(player, hand);
        if (isMeat(held) && this.getHealth() < this.getMaxHealth()) {
            if (!client) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.heal(4.0F);
                this.playSound(ModSounds.COMPY_EAT.get(), 0.9F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 66);
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {   // Mayus + clic con la mano vacia: cambia la orden
            if (isFollower()) {
                if (!client) player.displayClientMessage(Component.literal("Es un soldado: sigue al líder de la manada (interfaz bloqueada)"), true);
                return InteractionResult.sidedSuccess(client);
            }
            if (!client) {
                int next = (getOrder() + 1) % PetOrders.COUNT;
                PetOrders.setOrder(this, next);
                player.displayClientMessage(Component.literal("Orden: " + PetOrders.NAMES[next]), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;   // el Compy es demasiado pequeno para montarlo
    }

    /** Cada carne sube su confianza (con una pausa entre bocados); al llegar al total se doma. En Creativo, al instante. */
    private void feedWild(Player player, ItemStack held) {
        if (this.tickCount - lastFeedTick < 20 && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.literal("Aún está masticando..."), true);
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        lastFeedTick = this.tickCount;
        this.playSound(ModSounds.COMPY_EAT.get(), 0.9F, 1.0F);
        this.level().broadcastEntityEvent(this, (byte) 66);
        this.setTarget(null);
        this.heal(2.0F);
        trust++;
        int need = CompyConfig.get(CompyConfig.TAME_FEEDS, 3);
        if (player.getAbilities().instabuild || trust >= need) {
            this.tame(player);
            if (player instanceof net.minecraft.server.level.ServerPlayer qsp) com.prodigytown.creatures.quests.QuestData.onTame(qsp, this);
            this.setPersistenceRequired();
            applyRoleStats(false);
            PetOrders.setOrder(this, PetOrders.FOLLOW);
            recruitPack(player);
            this.trust = 0;
            this.playSound(ModSounds.COMPY_HAPPY.get(), 1.0F, 1.0F);
            this.level().broadcastEntityEvent(this, (byte) 7);   // corazones
            player.displayClientMessage(Component.literal("¡El líder confía en ti: su manada te sigue como soldados!"), true);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);   // humo: todavia desconfia
            player.displayClientMessage(Component.literal("Confianza: " + trust + "/" + need), true);
        }
    }

    // ------------------------------------------------------------------ sonidos y varios

    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.COMPY_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.COMPY_DEATH.get(); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.COMPY_STEP.get(), 0.25F, 0.9F + this.random.nextFloat() * 0.3F);
    }
    @Nullable @Override protected SoundEvent getAmbientSound() {
        return isTame() && getOrder() == PetOrders.SLEEP ? null : ModSounds.COMPY_AMBIENT.get();
    }
    @Override protected float getSoundVolume() { return 0.8F; }
    @Override public int getAmbientSoundInterval() { return 160; }
    @Override public int getMaxSpawnClusterSize() { return 8; }
    @Override protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dims) { return dims.height * 0.85F; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean removeWhenFarAway(double distance) { return !isTame() && super.removeWhenFarAway(distance); }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!isTame() && this.random.nextInt(3) == 0) this.spawnAtLocation(new ItemStack(Items.FEATHER, 1 + this.random.nextInt(2)));
    }
}
