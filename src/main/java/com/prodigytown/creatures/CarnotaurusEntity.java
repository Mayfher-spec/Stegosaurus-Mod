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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Carnotauro: segundo dinosaurio del mod. Carnivoro de ataque (mas rapido y mas agil que el estegosaurio, con menos vida).
 * Una sola entidad con dos etapas: cria (chibi) y adulto. Se doma con carne cruda; la cria crece comiendo carne.
 * Adulto: montable con silla, embestida (V), cabezazo con cuernos (B / clic derecho), mordida (clic izquierdo) y rugido (R).
 * Comparte con el estegosaurio las ordenes y el estado "caido" (ver PetOrders y PetEvents).
 */
public class CarnotaurusEntity extends TamableAnimal implements PetOrders.Holder {
    public static final int SCARLET = 0, MAGMA = 1, EMERALD = 2, VARIANTS = 3;
    public static final String[] VARIANT_NAMES = {"Rojo", "Magma", "Esmeralda"};
    public static final int CHARGE_LEN = 24, BUTT_LEN = 14, ROAR_LEN = 40;
    /** Carnes crudas que necesita la cria para convertirse en adulto. */
    public static final int FEEDS_TO_GROW = 6;
    private static final EntityDimensions ADULT_DIM = EntityDimensions.fixed(2.3F, 3.0F);
    private static final EntityDimensions CHIBI_DIM = EntityDimensions.fixed(0.95F, 1.15F);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ADULT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FALLEN = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ABILITY_KIND = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ABILITY_LEFT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);

    /** Solo cliente: suavizado de las poses de sentado y dormido. */
    public float sitAnim, sleepAnim;
    private int growth;
    @Nullable private BlockPos orderHome;
    private int cdCharge, cdButt, cdRoar, cdBite;
    private final Set<Integer> chargeHit = new HashSet<>();

    public CarnotaurusEntity(EntityType<? extends CarnotaurusEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.moveControl = new SmoothMoveControl(this, 6.0F);
        this.lookControl = new PetOrders.PetLookControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 110.0)
            .add(Attributes.MOVEMENT_SPEED, 0.34)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    // ------------------------------------------------------------------ datos

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, SCARLET);
        this.entityData.define(ADULT, Boolean.TRUE);
        this.entityData.define(SADDLED, Boolean.FALSE);
        this.entityData.define(ORDER, PetOrders.FOLLOW);
        this.entityData.define(FALLEN, Boolean.FALSE);
        this.entityData.define(ABILITY_KIND, 0);
        this.entityData.define(ABILITY_LEFT, 0);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putBoolean("Adult", isAdultStage());
        tag.putInt("Growth", growth);
        tag.putBoolean("Saddled", isSaddled());
        tag.putInt("PetOrder", getOrder());
        tag.putBoolean("PetFallen", isFallen());
        if (orderHome != null) tag.putLong("OrderHome", orderHome.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        this.entityData.set(ADULT, !tag.contains("Adult") || tag.getBoolean("Adult"));
        this.growth = tag.getInt("Growth");
        this.entityData.set(SADDLED, tag.getBoolean("Saddled"));
        this.entityData.set(ORDER, PetOrders.clamp(tag.getInt("PetOrder")));
        this.entityData.set(FALLEN, tag.getBoolean("PetFallen"));
        this.orderHome = tag.contains("OrderHome") ? BlockPos.of(tag.getLong("OrderHome")) : null;
        applyStageStats(false);
        if (isTame()) this.setPersistenceRequired();
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int v) { this.entityData.set(VARIANT, Math.max(0, Math.min(VARIANTS - 1, v))); }
    public boolean isAdultStage() { return this.entityData.get(ADULT); }
    public boolean isSaddled() { return this.entityData.get(SADDLED); }
    public int getAbilityKind() { return this.entityData.get(ABILITY_KIND); }
    public int getAbilityLeft() { return this.entityData.get(ABILITY_LEFT); }
    public boolean isCharging() { return getAbilityKind() == 1 && getAbilityLeft() > 0; }
    public float getAbilityProgress() {
        int len = switch (getAbilityKind()) { case 1 -> CHARGE_LEN; case 2 -> BUTT_LEN; case 3 -> ROAR_LEN; default -> 1; };
        return Mth.clamp(1.0F - getAbilityLeft() / (float) len, 0.0F, 1.0F);
    }

    // PetOrders.Holder
    @Override public int getOrder() { return this.entityData.get(ORDER); }
    @Override public void setOrderRaw(int order) { this.entityData.set(ORDER, order); }
    @Override public boolean isFallen() { return this.entityData.get(FALLEN); }
    @Override public void setFallenRaw(boolean fallen) { this.entityData.set(FALLEN, fallen); }
    @Nullable @Override public BlockPos orderHome() { return orderHome; }
    @Override public void setOrderHome(@Nullable BlockPos pos) { this.orderHome = pos; }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ADULT.equals(key)) this.refreshDimensions();
    }

    @Override public EntityDimensions getDimensions(Pose pose) { return isAdultStage() ? ADULT_DIM : CHIBI_DIM; }
    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) { return dims.height * 0.86F; }

    /** Ajusta vida, ataque y velocidad a la etapa; conserva la fraccion de vida. */
    private void applyStageStats(boolean keepFraction) {
        boolean adult = isAdultStage();
        float frac = this.getMaxHealth() <= 0 ? 1.0F : this.getHealth() / this.getMaxHealth();
        setBase(Attributes.MAX_HEALTH, adult ? 110.0 : 28.0);
        setBase(Attributes.ATTACK_DAMAGE, adult ? 16.0 : 4.0);
        setBase(Attributes.MOVEMENT_SPEED, adult ? 0.34 : 0.30);
        setBase(Attributes.ARMOR, adult ? 4.0 : 0.0);
        if (keepFraction) this.setHealth(Math.max(1.0F, this.getMaxHealth() * frac));
        else if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
        this.refreshDimensions();
    }
    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attr, double v) {
        var inst = this.getAttribute(attr);
        if (inst != null) inst.setBaseValue(v);
    }

    public void setAdultStage(boolean adult) {
        this.entityData.set(ADULT, adult);
        applyStageStats(true);
    }

    @Nullable @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData previous, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, previous, tag);
        int roll = this.random.nextInt(100);
        var biome = level.getBiome(this.blockPosition());
        int magmaChance = 15, emeraldChance = 15;
        if (biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS)) magmaChance = 35;
        if (biome.is(net.minecraft.tags.BiomeTags.IS_FOREST) || biome.is(net.minecraft.tags.BiomeTags.IS_JUNGLE)) emeraldChance = 35;
        int v = roll < magmaChance ? MAGMA : roll < magmaChance + emeraldChance ? EMERALD : SCARLET;
        setVariant(v);
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.STRUCTURE) {
            this.entityData.set(ADULT, this.random.nextInt(100) < 35); // la mayoria de los silvestres son crias que se pueden domesticar
        }
        applyStageStats(false);
        this.setHealth(this.getMaxHealth());
        return result;
    }

    // ------------------------------------------------------------------ IA

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override public boolean canUse() { return !isFallen() && stayOk() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isFallen() && stayOk() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(3, new FollowGoal());
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.9));
        this.goalSelector.addGoal(5, new PetOrders.PatrolGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9) {
            @Override public boolean canUse() { return !isTame() || (getOrder() != PetOrders.STAY && getOrder() != PetOrders.PATROL && getOrder() != PetOrders.FOLLOW && !isFallen() && super.canUse()); }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, PetOrders.patrolTargeting(this));
        // el adulto silvestre caza ganado (vacas, ovejas, cerdos...), no mascotas domadas
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Animal.class, 40, true, false, e -> !(e instanceof TamableAnimal)) {
            @Override public boolean canUse() { return !isTame() && isAdultStage() && super.canUse(); }
        });
    }

    private boolean stayOk() {
        if (!isTame() || getOrder() != PetOrders.STAY) return true;
        LivingEntity t = getTarget();
        return t != null && this.distanceToSqr(t) < 36.0;
    }

    /** Seguir al dueno: camina, corre si esta lejos, guarda distancia y solo se teletransporta muy lejos (96 bloques). */
    private final class FollowGoal extends Goal {
        private int recalc;
        FollowGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        private double stopDist() { return isAdultStage() ? 8.0 : 3.5; }
        private double startDist() { return isAdultStage() ? 12.0 : 6.0; }
        private LivingEntity owner() { return CarnotaurusEntity.this.getOwner(); }
        @Override public boolean canUse() {
            LivingEntity o = owner();
            if (o == null || o.isSpectator() || !isTame() || isOrderedToSit() || isLeashed() || getOrder() != PetOrders.FOLLOW || isFallen()) return false;
            return CarnotaurusEntity.this.distanceToSqr(o) >= startDist() * startDist();
        }
        @Override public boolean canContinueToUse() {
            LivingEntity o = owner();
            if (o == null || isOrderedToSit() || getOrder() != PetOrders.FOLLOW || isFallen()) return false;
            return CarnotaurusEntity.this.distanceToSqr(o) > stopDist() * stopDist();
        }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { CarnotaurusEntity.this.getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity o = owner();
            if (o == null) return;
            CarnotaurusEntity.this.getLookControl().setLookAt(o, 10.0F, (float) CarnotaurusEntity.this.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 10;
            double dist = Math.sqrt(CarnotaurusEntity.this.distanceToSqr(o));
            if (dist >= 96.0 && CarnotaurusEntity.this.level() instanceof ServerLevel) {
                CarnotaurusEntity.this.teleportTo(o.getX(), o.getY(), o.getZ());
                CarnotaurusEntity.this.getNavigation().stop();
                return;
            }
            double sp = dist > 24.0 ? 1.55 : 1.0;
            if (dist > 56.0) {
                double k = 48.0 / dist;
                CarnotaurusEntity.this.getNavigation().moveTo(getX() + (o.getX() - getX()) * k, o.getY(), getZ() + (o.getZ() - getZ()) * k, sp);
            } else CarnotaurusEntity.this.getNavigation().moveTo(o, sp);
        }
    }

    // ------------------------------------------------------------------ tick

    @Override public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            sitAnim += ((getOrder() == PetOrders.SIT ? 1.0F : 0.0F) - sitAnim) * 0.12F;
            sleepAnim += (((getOrder() == PetOrders.SLEEP || isFallen()) ? 1.0F : 0.0F) - sleepAnim) * 0.08F;
        } else {
            if (cdCharge > 0) cdCharge--;
            if (cdButt > 0) cdButt--;
            if (cdRoar > 0) cdRoar--;
            if (cdBite > 0) cdBite--;
            abilityTick();
            PetOrders.serverTick(this);
        }
    }

    private void abilityTick() {
        int left = getAbilityLeft();
        if (left <= 0) {
            if (getAbilityKind() != 0) this.entityData.set(ABILITY_KIND, 0);
            return;
        }
        this.entityData.set(ABILITY_LEFT, left - 1);
        if (getAbilityKind() == 1) chargeHits();
        if (left - 1 <= 0) { this.entityData.set(ABILITY_KIND, 0); chargeHit.clear(); }
    }

    private void startAbility(int kind, int len) {
        this.entityData.set(ABILITY_KIND, kind);
        this.entityData.set(ABILITY_LEFT, len);
    }

    private boolean isAlly(Entity e) {
        if (e == this || e == this.getOwner() || this.hasPassenger(e)) return true;
        if (e instanceof TamableAnimal t && t.isTame() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(t.getOwnerUUID())) return true;
        return e instanceof Player p && this.isTame() && this.isOwnedBy(p);
    }

    private void chargeHits() {
        Vec3 dir = Vec3.directionFromRotation(0.0F, this.getYRot());
        AABB box = this.getBoundingBox().inflate(0.7, 0.2, 0.7).move(dir.scale(1.4));
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, x -> !isAlly(x))) {
            if (!chargeHit.add(e.getId())) continue;
            e.hurt(this.damageSources().mobAttack(this), dmg);
            e.knockback(1.6, -dir.x, -dir.z);
            this.playSound(ModSounds.STEGO_CHARGE_HIT.get(), 2.4F, 0.85F + this.random.nextFloat() * 0.2F);
            if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 12, 0.4, 0.4, 0.4, 0.3);
        }
    }

    /** Habilidades del adulto: 0 embestida, 1 cabezazo, 2 mordida, 3 rugido. Solo servidor. */
    public void useAbility(Player user, int ability) {
        if (this.level().isClientSide || !isAdultStage() || isFallen() || isOrderedToSit()) return;
        if (!(this.level() instanceof ServerLevel server)) return;
        Vec3 look = Vec3.directionFromRotation(0.0F, this.getControllingPassenger() instanceof Player r ? r.getYRot() : this.getYRot());
        float atk = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (ability) {
            case 0 -> {
                if (cdCharge > 0 || getAbilityLeft() > 0) return;
                cdCharge = 160;
                chargeHit.clear();
                if (this.getControllingPassenger() instanceof Player r) this.setYRot(r.getYRot());
                startAbility(1, CHARGE_LEN);
                this.playSound(ModSounds.STEGO_CHARGE_HIT.get(), 2.0F, 0.6F);
            }
            case 1 -> {
                if (cdButt > 0 || getAbilityLeft() > 0) return;
                cdButt = 70;
                startAbility(2, BUTT_LEN);
                this.playSound(ModSounds.CARNO_ROAR.get(), 1.0F, 1.5F);
                Vec3 c = this.position().add(look.scale(2.6)).add(0, 1.2, 0);
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(2.4, 1.6, 2.4), x -> !isAlly(x))) {
                    e.hurt(this.damageSources().mobAttack(this), atk * 1.3F);
                    e.knockback(1.3, -look.x, -look.z);
                    e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.5, 0.0));
                    e.hurtMarked = true;
                    server.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 14, 0.4, 0.4, 0.4, 0.35);
                }
                this.playSound(ModSounds.STEGO_CHARGE_HIT.get(), 2.2F, 1.15F);
            }
            case 2 -> {
                if (cdBite > 0) return;
                cdBite = 12;
                LivingEntity best = null;
                double bd = 4.2 * 4.2;
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0, 1.5, 4.0), x -> !isAlly(x))) {
                    Vec3 to = e.position().subtract(this.position());
                    if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) < 0.35) continue;
                    double d = this.distanceToSqr(e);
                    if (d < bd) { bd = d; best = e; }
                }
                this.swing(InteractionHand.MAIN_HAND);
                this.playSound(ModSounds.CARNO_BITE.get(), 1.4F, 0.95F + this.random.nextFloat() * 0.1F);
                if (best != null) {
                    best.hurt(this.damageSources().mobAttack(this), atk);
                    best.knockback(0.5, -look.x, -look.z);
                }
            }
            case 3 -> {
                if (cdRoar > 0 || getAbilityLeft() > 0) return;
                cdRoar = 500;
                startAbility(3, ROAR_LEN);
                this.playSound(ModSounds.CARNO_ROAR.get(), 4.0F, 1.0F);
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0), x -> x != this)) {
                    if (isAlly(e)) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
                    else if (e.isAlive()) {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                    }
                }
                for (int k = 0; k < 40; k++) {
                    double a = k * Math.PI * 2.0 / 40.0;
                    server.sendParticles(ParticleTypes.SMOKE, getX() + Math.cos(a) * 3.0, getY() + 0.3, getZ() + Math.sin(a) * 3.0, 3, 0.2, 0.1, 0.2, 0.05);
                    server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX() + Math.cos(a) * 1.5, getY() + 2.8, getZ() + Math.sin(a) * 1.5, 0, 0, 0, 0, 0);
                }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ combate

    @Override public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || isAlly(entity)) return false;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean hit = entity.hurt(this.damageSources().mobAttack(this), damage);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            this.playSound(ModSounds.CARNO_BITE.get(), isAdultStage() ? 1.3F : 0.8F, (isAdultStage() ? 0.95F : 1.5F) + this.random.nextFloat() * 0.1F);
            living.knockback(0.5, this.getX() - entity.getX(), this.getZ() - entity.getZ());
        }
        return hit;
    }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        if (isFallen() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return super.isInvulnerableTo(source);
    }

    // ------------------------------------------------------------------ interaccion

    private static boolean isMeat(ItemStack s) {
        return s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON) || s.is(Items.CHICKEN) || s.is(Items.RABBIT)
            || s.is(Items.COOKED_BEEF) || s.is(Items.COOKED_PORKCHOP) || s.is(Items.COOKED_MUTTON) || s.is(Items.COOKED_CHICKEN) || s.is(Items.COOKED_RABBIT);
    }
    private static boolean isRaw(ItemStack s) {
        return s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON) || s.is(Items.CHICKEN) || s.is(Items.RABBIT);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = this.level().isClientSide;
        if (isFallen()) {
            if (this.isTame() && this.isOwnedBy(player) && held.is(ProdigyCreatures.CARNO_JERKY.get())) {
                if (!client) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    PetOrders.revive(this);
                    this.playSound(ModSounds.STEGO_ADULT_HAPPY.get(), 1.4F, 1.2F);
                }
            } else if (!client && this.isOwnedBy(player)) {
                player.displayClientMessage(Component.literal("Esta fuera de combate: reanimalo con carne curada"), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (!this.isTame()) {
            if (!isMeat(held)) return InteractionResult.PASS;
            if (!client) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                int odds = isAdultStage() ? 8 : 3;
                if (isRaw(held) && (player.getAbilities().instabuild || this.random.nextInt(odds) == 0)) {
                    this.tame(player);
                    this.setPersistenceRequired();
                    PetOrders.setOrder(this, PetOrders.FOLLOW);
                    this.setTarget(null);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else this.level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (!this.isOwnedBy(player)) return super.mobInteract(player, hand);
        if (isMeat(held) || held.is(ProdigyCreatures.CARNO_JERKY.get())) {
            boolean needsHeal = this.getHealth() < this.getMaxHealth();
            boolean grows = !isAdultStage() && isRaw(held);
            if (needsHeal || grows) {
                if (!client) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(held.is(ProdigyCreatures.CARNO_JERKY.get()) ? 30.0F : isRaw(held) ? 8.0F : 14.0F);
                    this.playSound(ModSounds.STEGO_ADULT_EAT.get(), 1.0F, isAdultStage() ? 0.9F : 1.4F);
                    if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 4, 0.4, 0.3, 0.4, 0.02);
                    if (grows && ++growth >= FEEDS_TO_GROW) grow();
                    else if (grows) player.displayClientMessage(Component.literal("Crece: " + growth + "/" + FEEDS_TO_GROW + " carnes crudas"), true);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS;
        }
        if (isAdultStage() && held.is(Items.SADDLE) && !isSaddled()) {
            if (!client) {
                this.entityData.set(SADDLED, true);
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.playSound(net.minecraft.sounds.SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isShiftKeyDown() && held.is(Items.SADDLE) && isSaddled()) {
            if (!client) { this.entityData.set(SADDLED, false); this.ejectPassengers(); this.spawnAtLocation(Items.SADDLE); }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isShiftKeyDown() && held.isEmpty()) { // Mayus + clic con la mano vacia: cambia la orden
            if (!client) {
                int next = (getOrder() + 1) % PetOrders.COUNT;
                PetOrders.setOrder(this, next);
                player.displayClientMessage(Component.literal("Orden: " + PetOrders.NAMES[next]), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (isAdultStage() && isSaddled() && !player.isShiftKeyDown() && held.isEmpty() && !isOrderedToSit()) {
            if (!client) player.startRiding(this);
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    private void grow() {
        growth = 0;
        setAdultStage(true);
        this.playSound(ModSounds.CARNO_ROAR.get(), 2.5F, 1.1F);
        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.2, getZ(), 40, 0.8, 1.0, 0.8, 0.3);
        if (this.getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Tu carnotauro es ahora un adulto"), true);
    }

    // ------------------------------------------------------------------ montura

    @Override protected boolean canAddPassenger(Entity passenger) {
        return isSaddled() && isAdultStage() && this.getPassengers().isEmpty() && passenger instanceof Player p && this.isOwnedBy(p);
    }
    @Nullable @Override public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof Player p && this.isOwnedBy(p) ? p : null;
    }
    @Override public double getPassengersRidingOffset() { return 2.75; }
    @Override public void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        Vec3 off = new Vec3(0.0, 0.0, -0.05).yRot(-this.yBodyRot * ((float) Math.PI / 180.0F));
        callback.accept(passenger, this.getX() + off.x, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + off.z);
    }

    @Override public void travel(Vec3 travelVector) {
        if (isFallen()) { this.calculateEntityAnimation(false); return; }
        if (this.isAlive() && this.isVehicle() && this.getControllingPassenger() instanceof Player rider) {
            float turnCap = isCharging() ? 2.5F : (rider.zza > 0.0F ? 5.0F : 9.0F);
            this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(rider.getYRot() - this.getYRot()), -turnCap, turnCap));
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.4F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;
            if (isCharging()) {
                if (this.isControlledByLocalInstance()) {
                    Vec3 d = Vec3.directionFromRotation(0.0F, this.getYRot()), mv = this.getDeltaMovement();
                    this.setDeltaMovement(d.x * 0.95, mv.y, d.z * 0.95);
                    this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                    this.setDeltaMovement(d.x * 0.95, (mv.y - 0.08) * 0.98, d.z * 0.95);
                }
                this.calculateEntityAnimation(false);
                return;
            }
            float strafe = rider.xxa * 0.4F, forward = rider.zza;
            if (forward <= 0.0F) forward *= 0.3F;
            if (this.isControlledByLocalInstance()) {
                this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.95F);
                super.travel(new Vec3(strafe, travelVector.y, forward));
            } else this.setDeltaMovement(Vec3.ZERO);
            this.calculateEntityAnimation(false);
        } else if (isCharging() && !this.isVehicle()) { // embestida sin jinete: recta hacia donde mira
            Vec3 d = Vec3.directionFromRotation(0.0F, this.getYRot()), mv = this.getDeltaMovement();
            this.setDeltaMovement(d.x * 0.8, mv.y, d.z * 0.8);
            super.travel(Vec3.ZERO.add(0.0, travelVector.y, 0.0));
        } else super.travel(travelVector);
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (isSaddled()) this.spawnAtLocation(Items.SADDLE);
    }

    // ------------------------------------------------------------------ sonidos y varios

    @Override protected SoundEvent getHurtSound(DamageSource source) { return isAdultStage() ? ModSounds.CARNO_HURT.get() : ModSounds.CARNO_HURT_BABY.get(); }
    @Override protected SoundEvent getDeathSound() { return isAdultStage() ? ModSounds.CARNO_DEATH.get() : ModSounds.CARNO_HURT_BABY.get(); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.CARNO_STEP.get(), isAdultStage() ? 0.8F : 0.35F, 0.9F + this.random.nextFloat() * 0.2F);
    }
    @Override protected float getSoundVolume() { return isAdultStage() ? 1.4F : 1.0F; }
    @Nullable @Override protected SoundEvent getAmbientSound() { return null; }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean removeWhenFarAway(double distance) { return !isTame() && super.removeWhenFarAway(distance); }

    public static boolean roomFor(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
