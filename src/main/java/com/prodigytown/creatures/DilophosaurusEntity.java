package com.prodigytown.creatures;

import com.prodigytown.creatures.progress.GrowthTable;
import com.prodigytown.creatures.progress.ProdigyPet;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dilofosaurio: primer carnivoro domesticable y montable del mod. Cazador agil (mas rapido que el estegosaurio y el carnotauro, con menos vida).
 * Dos etapas: cria (chibi) y adulto (nivel 30 en adelante). Salvaje: territorial (intimida antes de pelear, huye si esta muy herido).
 * Se doma dandole carne varias veces (confianza). Poderes del adulto: Mordida rapida, Zarpazo doble, Salto depredador y Frenesi jurasico
 * (membrana del cuello desplegada). Comparte con los otros dinosaurios las ordenes y el estado "caido" (ver PetOrders y PetEvents).
 */
public class DilophosaurusEntity extends TamableAnimal implements PetOrders.Holder, ProdigyPet {
    /** Variantes: Natural, Azul electrico, Magma rojo, Arena del desierto y Esmeralda espectral (misma geometria y mismas estadisticas). */
    public static final int NATURAL = 0, ELECTRIC = 1, MAGMA = 2, DESERT = 3, SPECTRAL = 4, VARIANTS = 5;
    public static final String[] VARIANT_NAMES = {"Natural", "Azul eléctrico", "Magma rojo", "Arena del desierto", "Esmeralda espectral"};
    /** Tipos de habilidad en curso (ABILITY_KIND): 1 mordida, 2 zarpazo doble, 3 salto depredador, 4 activacion del frenesi, 5 rugido intimidante. */
    public static final int K_BITE = 1, K_CLAW = 2, K_POUNCE = 3, K_FRENZY = 4, K_ROAR = 5, K_SPIT = 6;
    public static final int BITE_LEN = 12, CLAW_LEN = 18, POUNCE_LEN = 24, FRENZY_LEN = 44, ROAR_LEN = 34, SPIT_LEN = 26, LAND_LEN = 8, EAT_LEN = 22;
    /** Ticks de la animacion de cada habilidad (los usa el modelo para sincronizar poses con los impactos reales). */
    public static int abilityLen(int kind) {
        return switch (kind) { case K_BITE -> BITE_LEN; case K_CLAW -> CLAW_LEN; case K_POUNCE -> POUNCE_LEN; case K_FRENZY -> FRENZY_LEN; case K_ROAR -> ROAR_LEN; case K_SPIT -> SPIT_LEN; default -> 1; };
    }
    /** Habilidades para el HUD y la red (0 mordida, 1 zarpazo, 2 salto, 3 frenesi). */
    public static final String[] ABILITY_NAMES = {"Mordida rápida", "Zarpazo doble", "Salto depredador", "Frenesí jurásico", "Escupitajo venenoso"};

    private static final UUID M_HP_ADD = UUID.fromString("d1a0f0de-0001-4d1e-9a10-5e90a5a20001");
    private static final UUID M_DMG_ADD = UUID.fromString("d1a0f0de-0002-4d1e-9a10-5e90a5a20002");
    private static final UUID M_DMG_MUL = UUID.fromString("d1a0f0de-0003-4d1e-9a10-5e90a5a20003");
    private static final UUID M_ARM_ADD = UUID.fromString("d1a0f0de-0004-4d1e-9a10-5e90a5a20004");
    private static final UUID M_KB_ADD = UUID.fromString("d1a0f0de-0005-4d1e-9a10-5e90a5a20005");
    private static final UUID M_SPD_MUL = UUID.fromString("d1a0f0de-0006-4d1e-9a10-5e90a5a20006");
    private static final UUID M_ARM_GEAR = UUID.fromString("d1a0f0de-0007-4d1e-9a10-5e90a5a20007");
    private static final UUID M_TGH_GEAR = UUID.fromString("d1a0f0de-0008-4d1e-9a10-5e90a5a20008");
    private static final UUID FRENZY_DMG_ID = UUID.fromString("d1a0f0de-0009-4d1e-9a10-5e90a5a20009");
    private static final UUID FRENZY_SPD_ID = UUID.fromString("d1a0f0de-000a-4d1e-9a10-5e90a5a2000a");
    private static final UUID CHASE_SPD_ID = UUID.fromString("d1a0f0de-000b-4d1e-9a10-5e90a5a2000b");
    /**
     * Cifras del adulto a nivel 30 (la tabla de crecimiento compartida se escala con K_*): 85 de vida (entre 70 y 100), menos que el
     * estegosaurio (270) y el carnotauro (140), pero mas rapido que ambos.
     */
    private static final double K_HP = 40.0 / 450.0, K_DMG = 4.5 / 27.0;            // 24 PV y 4,5 de ataque a nivel 30; 40 PV y 7,5 de ataque a nivel 100
    private static final double ADULT_HP = 270.0 * K_HP, ADULT_DMG = 27.0 * K_DMG, ADULT_ARM = 1.0, K_ARM = ADULT_ARM / 14.0;
    private static final EntityDimensions ADULT_DIM = EntityDimensions.fixed(0.9F, 1.9F);
    private static final EntityDimensions CHIBI_DIM = EntityDimensions.fixed(0.5F, 0.95F);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ADULT = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    /** Armadura equipada: 0 = ninguna, 1 hierro, 2 oro, 3 diamante, 4 netherita (solo el adulto la lleva). */
    private static final EntityDataAccessor<Integer> ARMOR_TIER = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FALLEN = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ABILITY_KIND = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ABILITY_LEFT = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> PROGRESS = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** Transformacion (cinematica): ticks que faltan y tipo (1 = crece a adulto, 2 = vuelve a cria). */
    private static final EntityDataAccessor<Integer> MORPH = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MORPH_KIND = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    public static final int GROW_LEN = 56, SHRINK_LEN = 36;
    /** Membrana del cuello desplegada (true = abierta). */
    private static final EntityDataAccessor<Boolean> FRILL = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    /** Ticks que le quedan al Frenesi jurasico (0 = inactivo). */
    private static final EntityDataAccessor<Integer> FRENZY = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.INT);
    /** Recargas para el HUD: (tick de fin << 12) | duracion. 0 mordida, 1 zarpazo, 2 salto, 3 frenesi. */
    private static final EntityDataAccessor<Long> CD_BITE = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_CLAW = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_POUNCE = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_FRENZY = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_SPIT = SynchedEntityData.defineId(DilophosaurusEntity.class, EntityDataSerializers.LONG);

    // ---- solo cliente: estado de animacion suavizado (cada valor guarda su version del tick anterior ...O para interpolar) ----
    public float sitAnim, sleepAnim, sitAnimO, sleepAnimO;
    public float gait, gaitO, speedAnim, speedAnimO, runAnim, runAnimO, aggroAnim, aggroAnimO, turnAnim, turnAnimO;
    public float airAnim, airAnimO, swimAnim, swimAnimO, frillAnim, frillAnimO;
    /** Velocidad vertical (bloques/tick) para pose de salto/caida y ticks restantes de la pose de aterrizaje. */
    public float airVel;
    public int landT;
    private int airTicks;
    /** Destello tras transformarse (0 = sin destello) y su tipo (0 = crecer, 1 = encoger). */
    public int flashFade, flashKind;
    /** Mordida visual: ticks desde que empezo el golpe (-1 = sin golpe). */
    public int biteT = -1, biteTO = -1;
    /** Comer visual: ticks desde que empezo (-1 = no come). */
    public int eatT = -1, eatTO = -1;

    /** Lo pone el cliente cada tick: la tecla de salto esta pulsada mientras monta (solo la lee el cliente que controla la montura). */
    public static boolean clientJumpHeld;
    private int growth;
    @Nullable private BlockPos orderHome;
    private int cdBite, cdClaw, cdPounce, cdFrenzy, cdSpit;
    private ProdigyProgress progress = ProdigyProgress.atLevel(ProdigyProgress.ADULT_START_LEVEL, Talents.DILO);
    private boolean sexAssigned = false;
    private int lastCombatTick = -100000, aidReadyAt = 0, frillTicks = 0, frenzyTotal = 0;
    /** IA: pausas entre habilidades, objetivo inalcanzable (se ignora un rato), objetivo ordenado y confianza (domesticacion). */
    private int cdAiPounce, cdAiFrenzy, cdWarn, unreachableId = -1, unreachableUntil, attackTargetId = -1, warnedPlayerId = -1;
    private int trust, lastFeedTick = -100000, fleeUntil;
    private boolean pounceLeap, pounceLanded, leapApplied;
    private Vec3 pounceDir = Vec3.ZERO;
    private final Set<Integer> pounceHit = new HashSet<>();
    private final Map<Integer, Integer> bleeding = new HashMap<>();
    private float frozenYaw = Float.NaN;
    private final SmoothMoveControl smoothMove;

    public DilophosaurusEntity(EntityType<? extends DilophosaurusEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.smoothMove = new SmoothMoveControl(this, 9.0F);
        this.moveControl = this.smoothMove;
        this.lookControl = new PetOrders.PetLookControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, ADULT_HP)
            .add(Attributes.MOVEMENT_SPEED, 0.38)
            .add(Attributes.ATTACK_DAMAGE, ADULT_DMG)
            .add(Attributes.ARMOR, ADULT_ARM)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    // ------------------------------------------------------------------ datos

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, NATURAL);
        this.entityData.define(FEMALE, Boolean.FALSE);
        this.entityData.define(ADULT, Boolean.TRUE);
        this.entityData.define(SADDLED, Boolean.FALSE);
        this.entityData.define(ARMOR_TIER, 0);
        this.entityData.define(ORDER, PetOrders.FOLLOW);
        this.entityData.define(FALLEN, Boolean.FALSE);
        this.entityData.define(ABILITY_KIND, 0);
        this.entityData.define(ABILITY_LEFT, 0);
        this.entityData.define(PROGRESS, ProdigyProgress.atLevel(ProdigyProgress.ADULT_START_LEVEL, Talents.DILO).save());
        this.entityData.define(MORPH, 0);
        this.entityData.define(MORPH_KIND, 0);
        this.entityData.define(FRILL, Boolean.FALSE);
        this.entityData.define(FRENZY, 0);
        this.entityData.define(CD_BITE, 0L);
        this.entityData.define(CD_CLAW, 0L);
        this.entityData.define(CD_POUNCE, 0L);
        this.entityData.define(CD_FRENZY, 0L);
        this.entityData.define(CD_SPIT, 0L);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Morph", getMorph());
        tag.putInt("MorphKind", getMorphKind());
        tag.putBoolean("Adult", isAdultStage());
        tag.putBoolean("Female", isFemale());
        tag.putBoolean("SexAssigned", sexAssigned);
        tag.putInt("Growth", growth);
        tag.putInt("Trust", trust);
        tag.put("Progress", progress.save());
        tag.putBoolean("Saddled", isSaddled());
        tag.putInt("ArmorTier", getArmorTier());
        tag.putInt("PetOrder", getOrder());
        tag.putBoolean("PetFallen", isFallen());
        if (orderHome != null) tag.putLong("OrderHome", orderHome.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        this.entityData.set(ADULT, !tag.contains("Adult") || tag.getBoolean("Adult"));
        this.entityData.set(FEMALE, tag.getBoolean("Female"));
        this.sexAssigned = tag.getBoolean("SexAssigned");
        this.growth = tag.getInt("Growth");
        this.trust = tag.getInt("Trust");
        this.entityData.set(MORPH, tag.getInt("Morph"));
        this.entityData.set(MORPH_KIND, tag.getInt("MorphKind"));
        this.progress = ProdigyProgress.load(tag.getCompound("Progress"), isAdultStage() ? ProdigyProgress.ADULT_START_LEVEL : 1, Talents.DILO);
        this.entityData.set(PROGRESS, progress.save());
        this.entityData.set(SADDLED, tag.getBoolean("Saddled"));
        this.entityData.set(ARMOR_TIER, isAdultStage() ? Math.max(0, Math.min(4, tag.getInt("ArmorTier"))) : 0);
        this.entityData.set(ORDER, PetOrders.clampFor(this, tag.getInt("PetOrder")));
        this.entityData.set(FALLEN, tag.getBoolean("PetFallen"));
        this.orderHome = tag.contains("OrderHome") ? BlockPos.of(tag.getLong("OrderHome")) : null;
        applyStageStats(false);
        if (isTame()) this.setPersistenceRequired();
    }

    public boolean isFemale() { return this.entityData.get(FEMALE); }
    /** 70 % machos, 30 % hembras. */
    private void assignSex() { this.entityData.set(FEMALE, this.random.nextFloat() < 0.30F); this.sexAssigned = true; }
    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int v) { this.entityData.set(VARIANT, Math.max(0, Math.min(VARIANTS - 1, v))); }
    public boolean isAdultStage() { return this.entityData.get(ADULT); }
    public int getMorph() { return this.entityData.get(MORPH); }
    public int getMorphKind() { return this.entityData.get(MORPH_KIND); }
    public boolean isSaddled() { return this.entityData.get(SADDLED); }
    public int getArmorTier() { return this.entityData.get(ARMOR_TIER); }
    public int getAbilityKind() { return this.entityData.get(ABILITY_KIND); }
    public int getAbilityLeft() { return this.entityData.get(ABILITY_LEFT); }
    public boolean isFrillOpen() { return this.entityData.get(FRILL); }
    public int frenzyLeft() { return this.entityData.get(FRENZY); }
    public boolean isFrenzied() { return frenzyLeft() > 0; }
    public int getTrust() { return trust; }
    public int trustNeeded() { return DiloConfig.get(isAdultStage() ? DiloConfig.TAME_FEEDS_ADULT : DiloConfig.TAME_FEEDS_CHIBI, isAdultStage() ? 4 : 2); }
    /** Nivel maximo de la etapa chibi (el adulto empieza en el 30; el juvenil intermedio no existe en esta version). */
    public boolean isPouncing() { return getAbilityKind() == K_POUNCE && getAbilityLeft() > 0; }

    /** Ojos cerrados: durmiendo, caido y en un parpadeo breve cada ~4 s (cada animal en un momento distinto). */
    public boolean eyesClosed() {
        if (isFallen() || getOrder() == PetOrders.SLEEP) return true;
        return getMorph() <= 0 && (this.tickCount + this.getId() * 17) % 90 < 3;
    }

    /** Dormido (orden Dormir) o caido: no se mueve, no gira y nadie lo empuja; solo respira y salen las Zzz. */
    private boolean isSleepingNow() { return isFallen() || getOrder() == PetOrders.SLEEP; }
    @Override protected boolean isImmobile() { return super.isImmobile() || getMorph() > 0; }
    @Override public boolean isPushable() { return super.isPushable() && !isSleepingNow() && getMorph() <= 0; }
    private void freezeWhileSleeping() {
        if (!isSleepingNow() || this.isPassenger()) { frozenYaw = Float.NaN; return; }
        if (Float.isNaN(frozenYaw)) frozenYaw = this.yBodyRot;
        this.setYRot(frozenYaw); this.yRotO = frozenYaw;
        this.yBodyRot = frozenYaw; this.yBodyRotO = frozenYaw;
        this.yHeadRot = frozenYaw; this.yHeadRotO = frozenYaw;
        this.setXRot(0.0F); this.xRotO = 0.0F;
        Vec3 mv = this.getDeltaMovement();
        this.setDeltaMovement(0.0, mv.y, 0.0);
    }
    @Override public void handleEntityEvent(byte id) {
        if (id == 64 || id == 65) { this.flashFade = 24; this.flashKind = id == 64 ? 0 : 1; }
        else if (id == 66) { this.eatT = 0; this.eatTO = 0; }
        else super.handleEntityEvent(id);
    }
    /** Multiplicador de tono: la cria suena mas aguda. */
    private float vp() { return isAdultStage() ? 1.0F : 1.35F; }

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
        if (PROGRESS.equals(key)) this.progress = ProdigyProgress.load(this.entityData.get(PROGRESS), ProdigyProgress.ADULT_START_LEVEL, Talents.DILO);
    }

    @Override public EntityDimensions getDimensions(Pose pose) { return isAdultStage() ? ADULT_DIM : CHIBI_DIM; }
    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) { return dims.height * 0.82F; }

    // ------------------------------------------------------------------ estadisticas

    /** Ajusta vida, ataque y velocidad a la etapa; conserva la fraccion de vida. */
    private void applyStageStats(boolean keepFraction) {
        boolean adult = isAdultStage();
        float frac = this.getMaxHealth() <= 0 ? 1.0F : this.getHealth() / this.getMaxHealth();
        setBase(Attributes.MAX_HEALTH, adult ? ADULT_HP : 12.0);
        setBase(Attributes.ATTACK_DAMAGE, adult ? ADULT_DMG : 2.0);
        setBase(Attributes.MOVEMENT_SPEED, adult ? 0.38 : 0.32);
        setBase(Attributes.ARMOR, adult ? ADULT_ARM : 0.0);
        setBase(Attributes.KNOCKBACK_RESISTANCE, 0.3);
        applyProgressStats(adult);
        if (keepFraction) this.setHealth(Math.max(1.0F, this.getMaxHealth() * frac));
        else if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
        this.refreshDimensions();
    }

    /** Nivel y talentos -> modificadores de atributos (solo el adulto; la cria no usa la tabla). */
    private void applyProgressStats(boolean adult) {
        int lv = progress.level();
        setMod(Attributes.MAX_HEALTH, M_HP_ADD, adult ? GrowthTable.HEALTH[lv] * K_HP - ADULT_HP : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG_ADD, adult ? GrowthTable.DAMAGE[lv] * K_DMG - ADULT_DMG : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG_MUL, adult ? 0.10 * progress.rank("P1") : 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        setMod(Attributes.ARMOR, M_ARM_ADD, adult ? GrowthTable.ARMOR[lv] * K_ARM - ADULT_ARM + progress.rank("S1") : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.KNOCKBACK_RESISTANCE, M_KB_ADD, adult ? 0.05 * progress.rank("S5") : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.MOVEMENT_SPEED, M_SPD_MUL, adult ? 0.02 * progress.rank("M1") + 0.05 * progress.rank("P1") : 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        int gear = adult ? getArmorTier() : 0;
        setMod(Attributes.ARMOR, M_ARM_GEAR, StegosaurusEntity.ARMOR_VALUE[gear], AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ARMOR_TOUGHNESS, M_TGH_GEAR, StegosaurusEntity.TOUGHNESS_VALUE[gear], AttributeModifier.Operation.ADDITION);
    }
    private void setMod(Attribute attr, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, "prodigy_progress", amount, op));
    }
    private void setTransient(Attribute attr, UUID id, double amount, AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (on) { if (cur == null || cur.getAmount() != amount) { inst.removeModifier(id); inst.addTransientModifier(new AttributeModifier(id, "dilo_buff", amount, op)); } }
        else if (cur != null) inst.removeModifier(id);
    }
    private void setBase(Attribute attr, double v) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst != null) inst.setBaseValue(v);
    }

    /** Recien nacido de un huevo: chibi del color del huevo, domado y a tu lado si hay dueno. */
    public void initFromEgg(int color, @Nullable UUID owner) {
        setVariant(color);
        this.progress = ProdigyProgress.atLevel(1, Talents.DILO);
        this.entityData.set(PROGRESS, progress.save());
        setAdultStage(false);
        this.growth = 0;
        if (owner != null) {
            this.setOwnerUUID(owner);
            this.setTame(true);
            PetOrders.setOrder(this, PetOrders.FOLLOW);
            this.setPersistenceRequired();
        }
        this.setHealth(this.getMaxHealth());
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
        int desertChance = 8, electricChance = 12, magmaChance = 8, spectralChance = 5;
        if (biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS)) { desertChance = 40; magmaChance = 10; }
        if (biome.is(net.minecraft.tags.BiomeTags.IS_FOREST) || biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)) { spectralChance = 8; electricChance = 14; }
        int v = roll < spectralChance ? SPECTRAL
            : roll < spectralChance + magmaChance ? MAGMA
            : roll < spectralChance + magmaChance + electricChance ? ELECTRIC
            : roll < spectralChance + magmaChance + electricChance + desertChance ? DESERT : NATURAL;
        setVariant(v);
        if (!sexAssigned) assignSex();
        // en estado salvaje solo aparecen adultos: las crias vienen de los huevos
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.STRUCTURE) this.entityData.set(ADULT, true);
        applyStageStats(false);
        this.setHealth(this.getMaxHealth());
        return result;
    }

    // ------------------------------------------------------------------ IA

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(1, new WarnGoal());          // salvaje: intimida con la membrana antes de pelear
        this.goalSelector.addGoal(1, new FleeWhenHurtGoal());  // salvaje muy herido: huye
        this.goalSelector.addGoal(2, new HuntGoal());          // persecucion, mordidas, zarpazos, saltos y frenesi
        this.goalSelector.addGoal(2, new WaryGoal());          // salvaje con carne delante: desconfia, pero se deja acercar
        this.goalSelector.addGoal(3, new FollowGoal());
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.9));
        this.goalSelector.addGoal(5, new PetOrders.PatrolGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6, 50) {
            @Override public boolean canUse() {
                if (!isTame()) return super.canUse();
                int o = getOrder();
                return o == PetOrders.WANDER && !isFallen() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this, DilophosaurusEntity.class).setAlertOthers());
        this.targetSelector.addGoal(4, new OrderTargetGoal());
        this.targetSelector.addGoal(5, new PreyGoal(this));
    }

    private static boolean holdingMeat(Player p) { return isMeat(p.getMainHandItem()) || isMeat(p.getOffhandItem()); }

    /** Valido para golpear: ni dueno, ni aliados, ni mascotas domadas; domado tampoco jugadores, aldeanos ni golems. */
    public boolean canHitPublic(Entity e) { return canHit(e); }
    private boolean canHit(Entity e) {
        if (!(e instanceof LivingEntity le) || isAlly(e) || !le.isAlive() || e instanceof ArmorStand) return false;
        if (isTame()) {
            if (e instanceof Player || e instanceof AbstractVillager || e instanceof IronGolem) return false;
            if (e instanceof TamableAnimal t && t.isTame()) return false;
        }
        return true;
    }

    private boolean isAlly(Entity e) {
        if (e == this || e == this.getOwner() || this.hasPassenger(e)) return true;
        if (e instanceof TamableAnimal t && t.isTame() && this.getOwnerUUID() != null && this.getOwnerUUID().equals(t.getOwnerUUID())) return true;
        return e instanceof Player p && this.isTame() && this.isOwnedBy(p);
    }

    /**
     * Presas. Salvaje: solo animales pequenos (gallinas y conejos); no ataca indiscriminadamente (a los jugadores solo tras intimidarlos).
     * Domado: caza monstruos cerca de su dueno (segun la orden) y, deambulando, animales pequenos; nunca jugadores, aldeanos, golems ni mascotas.
     */
    private static boolean isPrey(DilophosaurusEntity self, LivingEntity e) {
        if (e == self || e instanceof DilophosaurusEntity || !e.isAlive()) return false;
        if (e instanceof ArmorStand || e instanceof Bat || e instanceof WaterAnimal || e instanceof FlyingMob || e instanceof FlyingAnimal) return false;
        if (e.getY() - self.getY() > 4.0) return false;
        if (e.getId() == self.unreachableId && self.tickCount < self.unreachableUntil) return false;
        if (self.isAlly(e) || (e instanceof TamableAnimal t && t.isTame())) return false;
        if (!self.isTame()) return e instanceof net.minecraft.world.entity.animal.Chicken || e instanceof Rabbit;
        if (e instanceof Player || e instanceof AbstractVillager || e instanceof IronGolem) return false;
        int order = self.getOrder();
        if (order == PetOrders.SIT || order == PetOrders.SLEEP || order == PetOrders.ATTACK || self.isFallen()) return false;
        LivingEntity owner = self.getOwner();
        double range = self.isAdultStage() ? 20.0 : 10.0;
        if (order == PetOrders.STAY) range = 6.0;
        if (order == PetOrders.DEFEND) range = 16.0;
        if (owner != null && e.distanceToSqr(owner) > range * range) return false;
        if (e instanceof Enemy) return true;
        return order == PetOrders.WANDER && (e instanceof net.minecraft.world.entity.animal.Chicken || e instanceof Rabbit);
    }

    private static final class PreyGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private final DilophosaurusEntity self;
        PreyGoal(DilophosaurusEntity self) {
            super(self, LivingEntity.class, 6, true, false, e -> isPrey(self, e));
            this.self = self;
        }
        private boolean active() {
            if (self.isFallen() || self.getMorph() > 0 || (self.isTame() && PetOrders.isSittingOrder(self.getOrder()))) return false;
            if (self.isTame() && PetOrders.weak(self)) return false;   // asustado: no caza
            return !self.isTame() ? self.isAdultStage() : true;
        }
        @Override public boolean canUse() { return active() && super.canUse(); }
        @Override public boolean canContinueToUse() { return active() && super.canContinueToUse(); }
    }

    /** Orden "Atacar objetivo": persigue a la criatura designada hasta abatirla (o perderla) y luego vuelve a seguir. */
    private final class OrderTargetGoal extends Goal {
        OrderTargetGoal() { this.setFlags(EnumSet.of(Goal.Flag.TARGET)); }
        @Nullable private LivingEntity designated() {
            if (attackTargetId < 0 || !(level() instanceof ServerLevel sl)) return null;
            Entity e = sl.getEntity(attackTargetId);
            return e instanceof LivingEntity le && le.isAlive() && canHit(le) ? le : null;
        }
        @Override public boolean canUse() { return isTame() && getOrder() == PetOrders.ATTACK && !isFallen(); }
        @Override public void start() { LivingEntity t = designated(); if (t != null) setTarget(t); }
        @Override public boolean canContinueToUse() { return canUse() && getTarget() != null && getTarget().isAlive(); }
        @Override public void tick() {
            LivingEntity t = designated();
            if (t == null) { attackTargetId = -1; setTarget(null); PetOrders.setOrder(DilophosaurusEntity.this, PetOrders.FOLLOW); }
            else if (getTarget() != t) setTarget(t);
        }
    }

    /** Orden "Atacar objetivo": se llama desde la red con el jugador que la dio; el objetivo es la criatura que apunta. */
    public void commandAttack(Player player) {
        if (this.level().isClientSide || !isTame() || isFallen()) return;
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1.0F), end = eye.add(look.scale(40.0));
        AABB box = player.getBoundingBox().expandTowards(look.scale(40.0)).inflate(1.5);
        LivingEntity best = null; double bd = Double.MAX_VALUE;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, x -> x != player && x != this && canHit(x))) {
            var hit = e.getBoundingBox().inflate(0.35).clip(eye, end);
            if (hit.isPresent()) { double d = eye.distanceToSqr(hit.get()); if (d < bd) { bd = d; best = e; } }
        }
        if (best == null) { player.displayClientMessage(Component.literal("Apunta a una criatura antes de dar la orden de atacar"), true); return; }
        PetOrders.setOrder(this, PetOrders.ATTACK);
        attackTargetId = best.getId();
        this.setTarget(best);
        player.displayClientMessage(Component.literal("Orden: atacar a " + best.getName().getString()), true);
    }

    private boolean stayOk() {
        if (!isTame() || getOrder() != PetOrders.STAY) return true;
        LivingEntity t = getTarget();
        return t != null && this.distanceToSqr(t) < 36.0;
    }

    private static float turnToward(float cur, float want, float step) { return cur + Mth.clamp(Mth.wrapDegrees(want - cur), -step, step); }
    private float yawTo(LivingEntity t) { return (float) (Mth.atan2(t.getZ() - this.getZ(), t.getX() - this.getX()) * 57.29577951308232) - 90.0F; }

    /** Persecucion y combate: corre hacia la presa y elige la habilidad (frenesi si la pelea es dura, salto si esta lejos, zarpazo o mordida de cerca). */
    private final class HuntGoal extends Goal {
        private int repath, noPath, decide;
        HuntGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        private boolean valid(@Nullable LivingEntity t) {
            return t != null && t.isAlive() && canHit(t) && !isFallen() && stayOk() && !isOrderedToSit() && getMorph() <= 0 && !isVehicle()
                && !(t instanceof Player p && (p.isCreative() || p.isSpectator()));
        }
        @Override public boolean canUse() { return valid(DilophosaurusEntity.this.getTarget()); }
        @Override public boolean canContinueToUse() { return valid(DilophosaurusEntity.this.getTarget()); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { repath = 0; noPath = 0; decide = 10; }
        @Override public void stop() { DilophosaurusEntity.this.getNavigation().stop(); }
        @Override public void tick() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            LivingEntity t = me.getTarget();
            if (t == null) return;
            me.getLookControl().setLookAt(t, 50.0F, 50.0F);
            if (me.getAbilityLeft() > 0) { // en plena habilidad: no se desplaza por su cuenta
                if (me.getAbilityKind() != K_POUNCE) me.getNavigation().stop();
                if (me.getAbilityKind() != K_POUNCE) me.setYRot(turnToward(me.getYRot(), me.yawTo(t), 14.0F));
                return;
            }
            boolean adult = isAdultStage();
            double d2 = me.distanceToSqr(t.getX(), t.getY(), t.getZ());
            double reach = 2.0 + me.getBbWidth() * 0.5 + (adult ? 0.0 : -0.4) + t.getBbWidth() * 0.4;
            double reach2 = reach * reach;
            if (--repath <= 0) {
                repath = 3 + me.random.nextInt(4);
                if (d2 > reach2 * 0.55) {
                    double sp = adult ? (d2 > 80.0 ? 1.35 : 1.2) : 1.1;
                    if (me.getNavigation().moveTo(t, sp)) noPath = 0;
                    else if (++noPath >= 6 && d2 > reach2) { me.unreachableId = t.getId(); me.unreachableUntil = me.tickCount + 200; me.setTarget(null); return; }
                } else me.getNavigation().stop();
            }
            if (--decide > 0) return;
            decide = 4;
            boolean los = me.getSensing().hasLineOfSight(t);
            if (!los) return;
            if (adult) {
                // frenesi: pelea dura (poca vida o varios enemigos o presa fuerte) con la membrana lista
                if (me.cdFrenzy <= 0 && me.cdAiFrenzy <= 0 && d2 < 14.0 * 14.0 && me.isFrenzyWorth(t)) { me.cdAiFrenzy = 100; me.useAbility(null, 3); return; }
                if (d2 > 25.0 && d2 < 13.0 * 13.0 && me.cdPounce <= 0 && me.cdAiPounce <= 0 && me.onGround() && Math.abs(t.getY() - me.getY()) < 3.0
                        && Math.abs(Mth.wrapDegrees(me.yawTo(t) - me.getYRot())) < 35.0F && me.random.nextInt(3) == 0) { me.cdAiPounce = 60 + me.random.nextInt(60); me.useAbility(null, 2); return; }
                if (me.progress.rank("S7") > 0 && me.cdSpit <= 0 && d2 > 16.0 && d2 < 14.0 * 14.0 && me.random.nextInt(3) == 0
                        && Math.abs(Mth.wrapDegrees(me.yawTo(t) - me.getYRot())) < 25.0F) { me.useAbility(null, 4); return; }
                if (d2 <= reach2 * 1.05 && me.cdClaw <= 0 && me.random.nextInt(3) == 0) { me.useAbility(null, 1); return; }
            }
            if (d2 <= reach2 && me.cdBite <= 0) me.useAbility(null, 0);
        }
    }

    /** Hay motivo para activar el frenesi: la vida baja, varios enemigos cerca o una presa mucho mas dura que el. */
    private boolean isFrenzyWorth(LivingEntity t) {
        if (this.getHealth() < this.getMaxHealth() * 0.6F) return true;
        if (t.getMaxHealth() > this.getMaxHealth() * 0.8F) return true;
        int foes = 0;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7.0), x -> canHit(x) && x instanceof Enemy)) foes++;
        return foes >= 2;
    }

    /**
     * Territorial: un adulto salvaje que ve a un jugador cerca (sin carne en la mano) se detiene, abre la membrana y ruge una vez para avisarle.
     * Si el jugador sigue acercandose, ataca; si se retira, se calma.
     */
    private final class WarnGoal extends Goal {
        private int waited;
        @Nullable private Player player;
        WarnGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        @Override public boolean canUse() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            if (me.isTame() || !isAdultStage() || me.isFallen() || me.cdWarn > 0 || me.getTarget() != null || me.getAbilityLeft() > 0 || me.getMorph() > 0) return false;
            Player p = me.level().getNearestPlayer(me, 9.0);
            if (p == null || p.isCreative() || p.isSpectator() || holdingMeat(p) || !me.getSensing().hasLineOfSight(p) || p.getId() == me.warnedPlayerId && me.cdWarn > 0) return false;
            this.player = p;
            return true;
        }
        @Override public boolean canContinueToUse() { return waited < ROAR_LEN + 20 && player != null && player.isAlive() && getTarget() == null; }
        @Override public void start() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            waited = 0; me.cdWarn = 500; me.warnedPlayerId = player != null ? player.getId() : -1;
            me.getNavigation().stop();
            me.startAbility(K_ROAR, ROAR_LEN);
            me.frillTicks = ROAR_LEN + 30;
        }
        @Override public void tick() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            waited++;
            me.getNavigation().stop();
            if (player != null) {
                me.getLookControl().setLookAt(player, 40.0F, 40.0F);
                me.setYRot(turnToward(me.getYRot(), me.yawTo(player), 8.0F));
            }
        }
        @Override public void stop() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            // pasado el aviso, si el jugador sigue cerca (menos de 7 bloques) lo ataca
            if (player != null && player.isAlive() && !player.isCreative() && me.distanceToSqr(player) < 49.0 && !holdingMeat(player)) me.setTarget(player);
            player = null;
        }
    }

    /** Salvaje con carne delante: no ataca; si tiene confianza se deja acercar, si no mantiene la distancia y vigila. */
    private final class WaryGoal extends Goal {
        @Nullable private Player player;
        WaryGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        @Override public boolean canUse() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            if (me.isTame() || me.isFallen() || me.getTarget() != null || me.getAbilityLeft() > 0 || me.getMorph() > 0) return false;
            Player p = me.level().getNearestPlayer(me, 10.0);
            if (p == null || p.isSpectator() || !holdingMeat(p)) return false;
            this.player = p;
            return true;
        }
        @Override public boolean canContinueToUse() { return player != null && player.isAlive() && holdingMeat(player) && getTarget() == null && distanceToSqr(player) < 144.0; }
        @Override public void stop() { player = null; getNavigation().stop(); }
        @Override public void tick() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            if (player == null) return;
            me.getLookControl().setLookAt(player, 30.0F, 30.0F);
            double d = Math.sqrt(me.distanceToSqr(player));
            double want = me.trust > 0 ? 2.0 : 4.5;
            if (me.tickCount % 10 != 0) return;
            if (d > want + 1.0) me.getNavigation().moveTo(player, 0.55);
            else if (d < want - 1.5) {
                Vec3 away = LandRandomPos.getPosAway(me, 6, 3, player.position());
                if (away != null) me.getNavigation().moveTo(away.x, away.y, away.z, 0.8);
            } else me.getNavigation().stop();
        }
    }

    /** Salvaje gravemente herido (menos del 25 % de vida): huye del que lo ataco en vez de seguir peleando. */
    private final class FleeWhenHurtGoal extends Goal {
        FleeWhenHurtGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE)); }
        @Override public boolean canUse() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            return !me.isTame() && !me.isFallen() && me.getHealth() < me.getMaxHealth() * 0.25F && me.getLastHurtByMob() != null && me.tickCount - me.getLastHurtByMobTimestamp() < 200;
        }
        @Override public boolean canContinueToUse() { return DilophosaurusEntity.this.tickCount < DilophosaurusEntity.this.fleeUntil && !DilophosaurusEntity.this.getNavigation().isDone(); }
        @Override public void start() {
            DilophosaurusEntity me = DilophosaurusEntity.this;
            me.fleeUntil = me.tickCount + 120;
            me.setTarget(null);
            LivingEntity from = me.getLastHurtByMob();
            Vec3 away = from == null ? null : LandRandomPos.getPosAway(me, 18, 8, from.position());
            if (away != null) me.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
        }
    }

    /** Seguir al dueno (Seguir y Defender): camina, corre si esta lejos, guarda distancia y solo se teletransporta muy lejos. */
    private final class FollowGoal extends Goal {
        private int recalc;
        FollowGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        private boolean active() { int o = getOrder(); return o == PetOrders.FOLLOW || o == PetOrders.DEFEND; }
        private double stopDist() { return getOrder() == PetOrders.DEFEND ? 4.0 : isAdultStage() ? 6.0 : 3.0; }
        private double startDist() { return getOrder() == PetOrders.DEFEND ? 8.0 : isAdultStage() ? 10.0 : 5.0; }
        private LivingEntity owner() { return DilophosaurusEntity.this.getOwner(); }
        @Override public boolean canUse() {
            LivingEntity o = owner();
            if (o == null || o.isSpectator() || !isTame() || isOrderedToSit() || isLeashed() || !active() || isFallen()) return false;
            return DilophosaurusEntity.this.distanceToSqr(o) >= startDist() * startDist();
        }
        @Override public boolean canContinueToUse() {
            LivingEntity o = owner();
            if (o == null || isOrderedToSit() || !active() || isFallen()) return false;
            return DilophosaurusEntity.this.distanceToSqr(o) > stopDist() * stopDist();
        }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { DilophosaurusEntity.this.getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity o = owner();
            if (o == null) return;
            DilophosaurusEntity.this.getLookControl().setLookAt(o, 10.0F, (float) DilophosaurusEntity.this.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 10;
            double dist = Math.sqrt(DilophosaurusEntity.this.distanceToSqr(o));
            if (dist >= rejoinDistance() && DilophosaurusEntity.this.level() instanceof ServerLevel) {
                DilophosaurusEntity.this.teleportTo(o.getX(), o.getY(), o.getZ());
                DilophosaurusEntity.this.getNavigation().stop();
                return;
            }
            double sp = dist > 18.0 ? 1.5 : dist > 10.0 ? 1.1 : 0.85;
            if (dist > 48.0) {
                double k = 40.0 / dist;
                DilophosaurusEntity.this.getNavigation().moveTo(getX() + (o.getX() - getX()) * k, o.getY(), getZ() + (o.getZ() - getZ()) * k, sp);
            } else DilophosaurusEntity.this.getNavigation().moveTo(o, sp);
        }
    }

    /** Distancia a partir de la cual se teletransporta junto al dueno. */
    public double rejoinDistance() { return 64.0; }

    // ------------------------------------------------------------------ tick

    @Override public void tick() {
        super.tick();
        freezeWhileSleeping();
        if (this.level().isClientSide) {
            clientAnimTick();
        } else {
            if (!sexAssigned) assignSex();
            if (cdBite > 0) cdBite--;
            if (cdClaw > 0) cdClaw--;
            if (cdPounce > 0) cdPounce--;
            if (cdFrenzy > 0) cdFrenzy--;
            if (cdSpit > 0) cdSpit--;
            if (cdAiPounce > 0) cdAiPounce--;
            if (cdAiFrenzy > 0) cdAiFrenzy--;
            if (cdWarn > 0) cdWarn--;
            morphTick();
            abilityTick();
            frenzyTick();
            frillTick();
            bleedTick();
            huntTick();
            serverProgressTick();
            PetOrders.serverTick(this);
        }
    }

    /** Cliente: estado de animacion suavizado. La fase de la zancada avanza con la distancia realmente recorrida (los pies no resbalan). */
    private void clientAnimTick() {
        sitAnimO = sitAnim; sleepAnimO = sleepAnim; airAnimO = airAnim; swimAnimO = swimAnim; frillAnimO = frillAnim;
        if (flashFade > 0) flashFade--;
        if (landT > 0) landT--;
        biteTO = biteT;
        if (this.swinging && this.swingTime <= 0) { biteT = 0; biteTO = 0; }
        else if (biteT >= 0) biteT = biteT + 1 >= BITE_LEN ? -1 : biteT + 1;
        eatTO = eatT;
        if (eatT >= 0) eatT = eatT + 1 >= EAT_LEN ? -1 : eatT + 1;
        gaitO = gait; speedAnimO = speedAnim; runAnimO = runAnim; aggroAnimO = aggroAnim; turnAnimO = turnAnim;
        sitAnim += ((getOrder() == PetOrders.SIT ? 1.0F : 0.0F) - sitAnim) * 0.14F;
        sleepAnim += (((getOrder() == PetOrders.SLEEP || isFallen()) ? 1.0F : 0.0F) - sleepAnim) * 0.09F;
        frillAnim += ((isFrillOpen() ? 1.0F : 0.0F) - frillAnim) * 0.16F;

        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        float v = (float) Math.sqrt(dx * dx + dz * dz);
        if (v > 1.6F) v = 0.0F;
        speedAnim += (v - speedAnim) * 0.30F;
        boolean adult = isAdultStage();
        float sp01 = Mth.clamp(speedAnim / 0.9F, 0.0F, 1.0F);
        float legLen = adult ? 1.18F : 0.50F; // largo de la pierna en bloques (cadera -> suelo)
        float natural = speedAnim > 0.004F ? 1.5707964F * speedAnim / (strideAmp(speedAnim, adult) * legLen) : 0.0F;
        gait += Math.min(natural, adult ? 0.62F + 0.14F * sp01 : 0.90F);
        if (gait > 628.3F) { gait -= 628.31854F; gaitO -= 628.31854F; }

        runAnim += ((isPouncing() ? 1.0F : Mth.clamp((speedAnim - 0.30F) / 0.42F, 0.0F, 1.0F)) - runAnim) * 0.22F;
        aggroAnim += ((isAggressive() ? 1.0F : 0.0F) - aggroAnim) * 0.12F;
        turnAnim += (Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO) - turnAnim) * 0.25F;

        // aire, caida y agua
        boolean inWater = this.isInWater();
        airVel = (float) this.getDeltaMovement().y;
        if (!this.onGround() && !inWater && !this.isPassenger()) airTicks++;
        else { if (this.onGround() && airTicks > 6) landT = LAND_LEN; airTicks = 0; }
        airAnim += ((airTicks > 3 ? 1.0F : 0.0F) - airAnim) * 0.35F;
        swimAnim += ((inWater && !this.onGround() ? 1.0F : 0.0F) - swimAnim) * 0.2F;
    }

    /** Amplitud (radianes) del vaiven del muslo segun la velocidad (bloques/tick). La usan la entidad (cadencia) y el modelo (pose). */
    public static float strideAmp(float speed, boolean adult) {
        float t = Mth.clamp(speed / 0.9F, 0.0F, 1.0F);
        return adult ? Mth.lerp(t, 0.40F, 0.80F) : Mth.lerp(t, 0.42F, 0.64F);
    }

    /** Servidor: postura de caza (flag "agresivo"), giro mas rapido persiguiendo, velocidad extra al perseguir (M5) y sentidos agudos (S3). */
    private void huntTick() {
        LivingEntity t = this.getTarget();
        boolean hunting = t != null && t.isAlive();
        this.setAggressive(hunting);
        this.smoothMove.setMaxTurn(isPouncing() ? 3.0F : hunting ? 16.0F : 9.0F);
        int m5 = progress.rank("M5");
        setTransient(Attributes.MOVEMENT_SPEED, CHASE_SPD_ID, 0.04 * m5, AttributeModifier.Operation.MULTIPLY_TOTAL, hunting && m5 > 0 && isAdultStage());
        // S3 Sentidos agudos: marca con brillo a los monstruos hostiles cerca del dueno
        int s3 = progress.rank("S3");
        if (s3 > 0 && isAdultStage() && isTame() && !isFallen() && this.tickCount % 60 == 0) {
            LivingEntity owner = this.getOwner();
            if (owner != null) {
                double r = 10.0 + 6.0 * (s3 - 1);
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(r), x -> x instanceof Enemy && x.isAlive()))
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false, false));
            }
        }
        if (hunting && !isTame() && isAdultStage() && this.tickCount % 40 == 0 && getAbilityLeft() == 0 && this.random.nextInt(4) == 0)
            this.playSound(ModSounds.DILO_AMBIENT.get(), 1.0F, 1.3F + this.random.nextFloat() * 0.15F);
    }

    // ------------------------------------------------------------------ habilidades

    private void startAbility(int kind, int len) {
        this.entityData.set(ABILITY_KIND, kind);
        this.entityData.set(ABILITY_LEFT, len);
    }

    private void abilityTick() {
        int left = getAbilityLeft(), kind = getAbilityKind();
        if (left <= 0) {
            if (kind != 0) this.entityData.set(ABILITY_KIND, 0);
            return;
        }
        int len = abilityLen(kind), el = len - left;
        this.entityData.set(ABILITY_LEFT, left - 1);
        switch (kind) {
            case K_BITE -> { if (el == 6) biteImpact(); }
            case K_CLAW -> { if (el == 5) clawHit(0); else if (el == 11) clawHit(1); }
            case K_POUNCE -> pounceTick(el, left);
            case K_FRENZY -> frenzyActivationTick(el);
            case K_ROAR -> { if (el == 6) roarEffect(false); }
            case K_SPIT -> { if (el == 12) spitLaunch(); }
            default -> { }
        }
        if (left - 1 <= 0) {
            this.entityData.set(ABILITY_KIND, 0);
            pounceLeap = false; pounceHit.clear();
        }
    }

    private float cooldownFactor() { return 1.0F - 0.05F * progress.rank("M3"); }
    private float talentDamage(LivingEntity victim, float base) {
        float d = base;
        if (victim.getHealth() < victim.getMaxHealth() * 0.5F) d *= 1.0F + 0.10F * progress.rank("C5");
        if (victim instanceof Enemy) d *= 1.0F + 0.15F * progress.rank("P1");
        int c4 = progress.rank("C4");
        if (c4 > 0 && this.random.nextFloat() < 0.08F + 0.06F * (c4 - 1) + (c4 > 2 ? 0.0F : 0.0F)) {
            d *= 1.6F;
            if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
        }
        return d;
    }

    /** Golpe con todos los efectos de talentos (critico, ejecutor, hemorragia). Solo servidor. */
    private boolean strike(LivingEntity victim, float base, double kb, boolean bleed) {
        if (!canHit(victim)) return false;
        lastCombatTick = this.tickCount;
        float dmg = talentDamage(victim, base);
        boolean hit = victim.hurt(this.damageSources().mobAttack(this), dmg);
        if (hit) {
            Vec3 d = victim.position().subtract(this.position());
            if (kb > 0.0 && d.lengthSqr() > 1.0E-4) victim.knockback(kb, -d.x, -d.z);
            int c3 = progress.rank("C3");
            if (bleed && c3 > 0 && isAdultStage()) bleeding.put(victim.getId(), 4);
            if (!victim.isAlive()) onKill();
        }
        return hit;
    }

    private void onKill() {
        int c6 = progress.rank("C6");
        if (c6 > 0 && isAdultStage()) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80 + 40 * c6, 0, false, false, true));
            this.heal(this.getMaxHealth() * 0.03F * c6);
        }
    }

    private void bleedTick() {
        if (bleeding.isEmpty() || this.tickCount % 20 != 0 || !(this.level() instanceof ServerLevel sl)) return;
        float per = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.04F + 0.06F * progress.rank("C3"));
        Iterator<Map.Entry<Integer, Integer>> it = bleeding.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> en = it.next();
            Entity e = sl.getEntity(en.getKey());
            if (!(e instanceof LivingEntity le) || !le.isAlive() || en.getValue() <= 0) { it.remove(); continue; }
            le.hurt(this.damageSources().indirectMagic(this, this), per);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, le.getX(), le.getY() + le.getBbHeight() * 0.6, le.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            en.setValue(en.getValue() - 1);
            if (en.getValue() <= 0 || !le.isAlive()) it.remove();
        }
    }

    private Vec3 aimDir() {
        float yaw = this.getControllingPassenger() instanceof Player r ? r.getYRot() : this.getYRot();
        return Vec3.directionFromRotation(0.0F, yaw);
    }

    /** Presa mas cercana delante del dilofosaurio dentro del alcance (prefiere su objetivo actual). */
    @Nullable private LivingEntity findFront(double reach, double minDot) {
        Vec3 look = aimDir();
        LivingEntity best = null;
        double bd = reach * reach;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach, 1.2, reach), this::canHit)) {
            Vec3 to = e.position().subtract(this.position());
            if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) < minDot || !this.getSensing().hasLineOfSight(e)) continue;
            double d = this.distanceToSqr(e) - (e == this.getTarget() ? 1.5 : 0.0);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    private float attack() { return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE); }

    private void biteImpact() {
        LivingEntity v = findFront(3.0 + (isAdultStage() ? 0.0 : -0.5), 0.25);
        if (v == null) return;
        strike(v, attack() * (float) DiloConfig.get(DiloConfig.BITE_DAMAGE, 1.0) * (1.0F + 0.06F * progress.rank("C1")), 0.5, true);
        this.playSound(ModSounds.DILO_BITE.get(), isAdultStage() ? 1.2F : 0.8F, (0.95F + this.random.nextFloat() * 0.1F) * vp());
    }

    private void clawHit(int which) {
        double reach = 3.0 + 0.3 * progress.rank("C2");
        Vec3 look = aimDir();
        float dmg = attack() * (float) DiloConfig.get(DiloConfig.CLAW_DAMAGE, 0.75) * (1.0F + 0.10F * progress.rank("C2"));
        int hits = 0;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach, 1.0, reach), this::canHit)) {
            Vec3 to = e.position().subtract(this.position());
            if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) < 0.30 || to.length() > reach + e.getBbWidth() * 0.5 || !this.getSensing().hasLineOfSight(e)) continue;
            if (strike(e, dmg, 0.35, true) && this.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.55, e.getZ(), 1, 0, 0, 0, 0);
            hits++;
        }
        this.playSound(ModSounds.DILO_CLAW.get(), 1.1F, (which == 0 ? 1.0F : 1.15F) * vp());
        if (hits > 0 && this.level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.CRIT, getX() + look.x * 2.0, getY() + getBbHeight() * 0.5, getZ() + look.z * 2.0, 4, 0.3, 0.3, 0.3, 0.2);
    }

    private void pounceTick(int el, int left) {
        final int takeoff = 5;
        boolean ridden = this.getControllingPassenger() != null;
        if (el == takeoff && !ridden) {
            double sp = pounceSpeed();
            this.setDeltaMovement(pounceDir.x * sp, 0.50 + 0.02 * progress.rank("M2"), pounceDir.z * sp);
            this.hasImpulse = true;
        }
        if (el == takeoff) { pounceLeap = true; this.playSound(ModSounds.DILO_POUNCE.get(), 1.3F, vp()); }
        if (!pounceLeap || pounceLanded) return;
        // contacto en vuelo: golpea una vez a lo que roza
        float dmg = attack() * (float) DiloConfig.get(DiloConfig.POUNCE_DAMAGE, 1.1) * (1.0F + 0.30F * progress.rank("M6")) * 0.7F;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.5, 0.3, 0.5), this::canHit)) {
            if (!pounceHit.add(e.getId())) continue;
            strike(e, dmg, 0.8, false);
        }
        if (el > takeoff + 3 && this.onGround()) {
            pounceLanded = true;
            pounceImpact();
            this.entityData.set(ABILITY_LEFT, Math.min(Math.max(0, left - 1), 5));
        }
    }

    private double pounceSpeed() { return pounceSpeedHint * (1.0 + 0.12 * progress.rank("M2")); }

    private void pounceImpact() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        float dmg = attack() * (float) DiloConfig.get(DiloConfig.POUNCE_DAMAGE, 1.1) * (1.0F + 0.30F * progress.rank("M6")) * 0.6F;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.4, 1.0, 2.4), this::canHit)) {
            Vec3 d = e.position().subtract(this.position());
            if (!pounceHit.contains(e.getId())) strike(e, dmg, 0.0, false);
            if (d.lengthSqr() > 1.0E-4) e.knockback(0.7, -d.x, -d.z);
        }
        this.playSound(ModSounds.DILO_POUNCE.get(), 1.0F, 0.8F * vp());
        sl.sendParticles(variantParticle(), getX(), getY() + 0.2, getZ(), 18, 1.0, 0.1, 1.0, 0.08);
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.1, getZ(), 10, 0.8, 0.1, 0.8, 0.05);
    }

    private ParticleOptions variantParticle() {
        return switch (getVariant()) {
            case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
            case MAGMA -> ParticleTypes.FLAME;
            case DESERT -> ParticleTypes.CLOUD;
            case SPECTRAL -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.COMPOSTER;
        };
    }

    /** Activacion del Frenesi: se detiene, baja, levanta la cabeza, abre la membrana (animacion) y al llegar al chillido empieza el efecto. */
    private void frenzyActivationTick(int el) {
        this.getNavigation().stop();
        frillTicks = Math.max(frillTicks, 4);
        if (el == 9) this.playSound(ModSounds.DILO_FRENZY.get(), 2.6F, 1.0F);
        if (el == 12) startFrenzyEffect();
    }

    private void startFrenzyEffect() {
        int secs = DiloConfig.get(DiloConfig.FRENZY_SECONDS, 10) + 3 * progress.rank("P1");
        frenzyTotal = secs * 20;
        this.entityData.set(FRENZY, frenzyTotal);
        roarEffect(true);
        this.level().broadcastEntityEvent(this, (byte) 67);
    }

    /** Rugido: intimida (lentitud breve; con S4 tambien debilidad) a los no aliados cercanos; el del frenesi tiene onda de choque. */
    private void roarEffect(boolean shock) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        int s4 = progress.rank("S4");
        double r = (shock ? DiloConfig.get(DiloConfig.FRENZY_INTIMIDATE_RADIUS, 7) : 6.0) + 1.0 * s4 + (s4 > 0 ? 1.0 : 0.0);
        this.playSound(shock ? ModSounds.DILO_FRENZY.get() : ModSounds.DILO_ROAR.get(), shock ? 3.0F : 2.6F, vp());
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), x -> x != this && x.isAlive())) {
            if (isAlly(e)) continue;
            if (e instanceof Player && isTame()) continue;
            if (e instanceof TamableAnimal t && t.isTame()) continue;
            if (e instanceof AbstractVillager || e instanceof IronGolem) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, shock ? 60 + 20 * s4 : 40, 0));
            if (s4 >= 2 || shock && s4 >= 1) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        ParticleOptions fx = variantParticle();
        for (double rr = 1.5; rr <= Math.min(r, 8.0); rr += 1.5)
            for (int k = 0; k < 14; k++) {
                double a = k * Math.PI * 2.0 / 14.0;
                sl.sendParticles(fx, getX() + Math.cos(a) * rr, getY() + 0.3, getZ() + Math.sin(a) * rr, 1, 0.1, 0.15, 0.1, 0.03);
            }
        if (shock) sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + getBbHeight() * 0.7, getZ(), 1, 0, 0, 0, 0);
    }

    /** Efectos mientras dura el Frenesi: +30 % dano y +25 % velocidad (configurables), aura de su variante; al acabar quita los bonos. */
    private void frenzyTick() {
        int left = frenzyLeft();
        boolean on = left > 0 && isAdultStage() && !isFallen();
        setTransient(Attributes.ATTACK_DAMAGE, FRENZY_DMG_ID, DiloConfig.get(DiloConfig.FRENZY_DAMAGE_PERCENT, 30) / 100.0, AttributeModifier.Operation.MULTIPLY_BASE, on);
        setTransient(Attributes.MOVEMENT_SPEED, FRENZY_SPD_ID, DiloConfig.get(DiloConfig.FRENZY_SPEED_PERCENT, 25) / 100.0, AttributeModifier.Operation.MULTIPLY_BASE, on);
        if (left <= 0) return;
        if (!on) { this.entityData.set(FRENZY, 0); return; }
        this.entityData.set(FRENZY, left - 1);
        if (this.level() instanceof ServerLevel sl) {
            if (this.tickCount % 3 == 0) sl.sendParticles(variantParticle(), getX() + (random.nextDouble() - 0.5) * 1.4, getY() + random.nextDouble() * getBbHeight(), getZ() + (random.nextDouble() - 0.5) * 1.4, 1, 0.05, 0.05, 0.05, 0.02);
            if (progress.rank("P1") > 0 && this.tickCount % 4 == 0) { // aura del Cazador Primordial
                double a = this.tickCount * 0.35;
                sl.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 1.0, getY() + 0.2 + (this.tickCount % 24) / 24.0 * getBbHeight(), getZ() + Math.sin(a) * 1.0, 1, 0, 0, 0, 0);
            }
        }
        if (left - 1 == 0) { this.playSound(ModSounds.DILO_FRILL_CLOSE.get(), 1.4F, 1.0F); this.level().broadcastEntityEvent(this, (byte) 68); }
    }

    /** La membrana se despliega durante rugidos, frenesi (primera fase), aviso territorial y un instante al ser herido. */
    private void frillTick() {
        int k = getAbilityKind();
        boolean ab = getAbilityLeft() > 0 && (k == K_FRENZY || k == K_ROAR || k == K_SPIT) && abilityLen(k) - getAbilityLeft() >= 3 && getAbilityLeft() > 5;
        boolean firstPhase = frenzyLeft() > 0 && frenzyTotal - frenzyLeft() < 50;
        if (frillTicks > 0) frillTicks--;
        boolean want = isAdultStage() && (ab || firstPhase || frillTicks > 0) && !isFallen() && getMorph() <= 0 || (!isAdultStage() && ab);
        if (want != isFrillOpen()) {
            this.entityData.set(FRILL, want);
            this.playSound(want ? ModSounds.DILO_FRILL_OPEN.get() : ModSounds.DILO_FRILL_CLOSE.get(), 1.0F, vp());
        }
    }

    /**
     * Habilidades: 0 mordida rapida, 1 zarpazo doble, 2 salto depredador, 3 frenesi jurasico. Solo servidor; el jugador puede ser null (IA).
     * Los poderes del adulto; la cria solo muerde (debilmente).
     */
    public void useAbility(@Nullable Player user, int ability) {
        if (this.level().isClientSide || isFallen() || isOrderedToSit() || getMorph() > 0) return;
        if (!(this.level() instanceof ServerLevel)) return;
        boolean adult = isAdultStage();
        if (!adult && ability != 0) return;
        if (getAbilityLeft() > 0) return;
        lastCombatTick = this.tickCount;
        switch (ability) {
            case 0 -> {
                if (cdBite > 0) return;
                cdBite = Math.max(2, Math.round((adult ? DiloConfig.get(DiloConfig.BITE_COOLDOWN_TICKS, 14) : 22) * cooldownFactor()));
                markCd(CD_BITE, cdBite);
                if (this.getControllingPassenger() instanceof Player r) this.setYRot(r.getYRot());
                this.swing(InteractionHand.MAIN_HAND);
                startAbility(K_BITE, BITE_LEN);
            }
            case 1 -> {
                if (cdClaw > 0) return;
                cdClaw = Math.round(DiloConfig.get(DiloConfig.CLAW_COOLDOWN_SECONDS, 4) * 20 * cooldownFactor());
                markCd(CD_CLAW, cdClaw);
                if (this.getControllingPassenger() instanceof Player r) this.setYRot(r.getYRot());
                startAbility(K_CLAW, CLAW_LEN);
            }
            case 2 -> {
                if (cdPounce > 0 || !this.onGround()) return;
                cdPounce = Math.round(DiloConfig.get(DiloConfig.POUNCE_COOLDOWN_SECONDS, 9) * 20 * cooldownFactor() * (1.0F - 0.10F * progress.rank("M6")));
                markCd(CD_POUNCE, cdPounce);
                Vec3 dir = aimDir();
                LivingEntity t = this.getTarget();
                if (this.getControllingPassenger() == null && t != null) {
                    Vec3 to = t.position().subtract(this.position());
                    Vec3 flat = new Vec3(to.x, 0.0, to.z);
                    if (flat.lengthSqr() > 1.0E-4) dir = flat.normalize();
                    this.setYRot(yawTo(t));
                    pounceSpeedHint = Mth.clamp((float) (flat.length() / 7.6), 0.55F, 1.25F);
                } else pounceSpeedHint = 1.0F;
                pounceDir = dir;
                pounceLeap = false; pounceLanded = false; pounceHit.clear();
                this.getNavigation().stop();
                startAbility(K_POUNCE, POUNCE_LEN);
            }
            case 3 -> {
                if (cdFrenzy > 0) return;
                int secs = Math.max(5, DiloConfig.get(DiloConfig.FRENZY_COOLDOWN_SECONDS, 45));
                cdFrenzy = Math.round(secs * 20 * cooldownFactor() * (1.0F - 0.10F * progress.rank("P1")));
                markCd(CD_FRENZY, cdFrenzy);
                this.getNavigation().stop();
                startAbility(K_FRENZY, FRENZY_LEN);
                frillTicks = FRENZY_LEN;
            }
            case 4 -> {
                if (progress.rank("S7") <= 0) { if (user != null) user.displayClientMessage(Component.literal("Necesita el talento Glándula venenosa"), true); return; }
                if (cdSpit > 0) return;
                cdSpit = Math.round(DiloConfig.get(DiloConfig.SPIT_COOLDOWN_SECONDS, 8) * 20 * cooldownFactor());
                markCd(CD_SPIT, cdSpit);
                this.getNavigation().stop();
                LivingEntity st = this.getTarget();
                if (this.getControllingPassenger() instanceof Player r) this.setYRot(r.getYRot());
                else if (st != null) this.setYRot(yawTo(st));
                startAbility(K_SPIT, SPIT_LEN);
                frillTicks = SPIT_LEN + 14;
            }
            default -> { }
        }
    }
    /** Lanza el proyectil de veneno hacia donde mira el jinete (o hacia su objetivo). */
    private void spitLaunch() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        DiloSpitEntity s = ProdigyCreatures.DILO_SPIT.get().create(sl);
        if (s == null) return;
        int rank = Math.max(1, progress.rank("S7"));
        Vec3 dir;
        if (this.getControllingPassenger() instanceof Player r) dir = r.getViewVector(1.0F);
        else {
            LivingEntity t = this.getTarget();
            dir = t != null ? t.getEyePosition().subtract(this.getEyePosition()).normalize() : Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        }
        s.setOwner(this);
        s.setPos(this.getEyePosition().add(dir.scale(0.7)).add(0.0, -0.15, 0.0));
        s.damage = attack() * (float) DiloConfig.get(DiloConfig.SPIT_DAMAGE, 0.5) * (1.0F + 0.25F * (rank - 1));
        s.poisonTicks = 60 + 40 * (rank - 1);
        s.shoot(dir.x, dir.y + 0.08, dir.z, 1.4F, 1.0F);
        sl.addFreshEntity(s);
        this.playSound(net.minecraft.sounds.SoundEvents.LLAMA_SPIT, 1.2F, 1.2F);
        this.playSound(ModSounds.DILO_CLAW.get(), 0.8F, 0.7F);
        lastCombatTick = this.tickCount;
    }
    /** Escala de la velocidad del salto segun la distancia a la presa (la IA acierta mas cerca o mas lejos). */
    private float pounceSpeedHint = 1.0F;

    private void markCd(EntityDataAccessor<Long> acc, int ticks) {
        int t = Math.max(1, Math.min(4095, ticks));
        this.entityData.set(acc, ((this.level().getGameTime() + t) << 12) | t);
    }
    private static EntityDataAccessor<Long> cdAcc(int which) {
        return which == 0 ? CD_BITE : which == 1 ? CD_CLAW : which == 2 ? CD_POUNCE : which == 3 ? CD_FRENZY : CD_SPIT;
    }
    /** Fraccion de recarga que falta (1 = recien usada, 0 = lista). which: 0 mordida, 1 zarpazo, 2 salto, 3 frenesi. Sirve en el cliente. */
    public float cooldownLeft(int which) {
        long v = this.entityData.get(cdAcc(which));
        long dur = v & 4095L;
        if (dur == 0L) return 0.0F;
        long remaining = (v >> 12) - this.level().getGameTime();
        return remaining <= 0 ? 0.0F : Math.min(1.0F, remaining / (float) dur);
    }
    public int cooldownSeconds(int which) {
        long v = this.entityData.get(cdAcc(which));
        if ((v & 4095L) == 0L) return 0;
        long remaining = (v >> 12) - this.level().getGameTime();
        return remaining <= 0 ? 0 : (int) ((remaining + 19) / 20);
    }

    // ------------------------------------------------------------------ progresion (nivel, XP, talentos)

    @Override public ProdigyProgress getProgress() { return progress; }
    private void pushProgress() { this.entityData.set(PROGRESS, progress.save()); }
    private void applyProgress() {
        if (this.level().isClientSide) return;
        applyProgressStats(isAdultStage());
        if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
    }

    @Override public void gainXp(int amount) {
        if (this.level().isClientSide || amount <= 0) return;
        float oldMax = this.getMaxHealth();
        boolean leveled = progress.addXp(amount);
        if (leveled) {
            applyProgress();
            float diff = this.getMaxHealth() - oldMax;
            if (diff > 0) this.heal(diff);
            if (this.getOwner() instanceof Player p)
                p.displayClientMessage(Component.literal("Tu dilofosaurio sube a nivel " + progress.level() + (isAdultStage() ? " (" + progress.pointsAvailable() + " puntos disponibles)" : "")), true);
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0F, 0.9F);
        }
        pushProgress();
    }

    public boolean unlockTalent(int idx) {
        if (this.level().isClientSide || !isAdultStage()) return false;
        float oldMax = this.getMaxHealth();
        if (!progress.unlock(idx)) return false;
        applyProgress();
        float diff = this.getMaxHealth() - oldMax;
        if (diff > 0) this.heal(diff);
        pushProgress();
        return true;
    }

    public void resetTalents() {
        if (this.level().isClientSide) return;
        progress.reset();
        applyProgress();
        pushProgress();
    }

    public void toggleSit() {
        if (this.level().isClientSide) return;
        PetOrders.setOrder(this, this.isOrderedToSit() ? PetOrders.FOLLOW : PetOrders.SIT);
    }

    public void removeSaddle() {
        if (this.level().isClientSide || !isSaddled()) return;
        this.ejectPassengers();
        this.spawnAtLocation(Items.SADDLE);
        this.entityData.set(SADDLED, false);
    }

    public void removeArmor() {
        if (this.level().isClientSide || getArmorTier() == 0) return;
        this.entityData.set(ARMOR_TIER, 0);
        applyStageStats(true);
    }

    /** Vuelve a ser una cria (solo el adulto vivo): suelta la silla, baja al jinete y conserva dueno, nombre, color, nivel y orden. Solo servidor. */
    public void revertToChibi() {
        if (this.level().isClientSide || !isAlive() || isFallen() || !isAdultStage() || getMorph() > 0) return;
        this.ejectPassengers();
        removeSaddle();
        removeArmor();
        this.getNavigation().stop();
        this.growth = 0;
        this.entityData.set(MORPH_KIND, 2);
        this.entityData.set(MORPH, SHRINK_LEN);
        this.playSound(ModSounds.DILO_REVERT.get(), 2.0F, 1.0F);
    }

    public void rename(String name) {
        if (this.level().isClientSide) return;
        String n = name == null ? "" : name.trim();
        if (n.length() > 32) n = n.substring(0, 32);
        this.setCustomName(n.isEmpty() ? null : Component.literal(n));
        this.setCustomNameVisible(!n.isEmpty());
    }

    /** Servidor: regeneracion (S2), descanso y proteccion del dueno (S6). */
    private void serverProgressTick() {
        if (this.tickCount % 20 == 0 && this.isAlive() && this.getHealth() < this.getMaxHealth()) {
            float pct = 0.0F;
            int s2 = progress.rank("S2");
            if (s2 > 0) pct += new float[]{0F, 0.10F, 0.15F, 0.20F}[s2];
            if (this.tickCount - lastCombatTick > 200 && isTame()) pct += 0.15F; // en calma se recupera despacio
            if (pct > 0.0F) this.heal(this.getMaxHealth() * pct / 100.0F);
        }
        if (this.tickCount % 10 == 0 && progress.rank("S6") > 0 && isTame() && isAdultStage() && !isFallen() && this.tickCount >= aidReadyAt) {
            LivingEntity o = this.getOwner();
            if (o != null && o.isAlive() && this.distanceToSqr(o) < 24.0 * 24.0 && o.getHealth() < o.getMaxHealth() * 0.40F) {
                int rank = progress.rank("S6");
                aidReadyAt = this.tickCount + 1800;
                int dur = 100 + 40 * rank;
                o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 0));
                o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, 1));
                this.playSound(ModSounds.DILO_ROAR.get(), 2.0F, 1.2F);
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + o.getBbHeight(), o.getZ(), 8, 0.5, 0.4, 0.5, 0.02);
            }
        }
        // la confianza de un salvaje se pierde si pasa mucho tiempo sin recibir carne
        if (!isTame() && trust > 0 && this.tickCount - lastFeedTick > 1200) { trust--; lastFeedTick = this.tickCount; }
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.lastCombatTick = this.tickCount;
            int s5 = progress.rank("S5");
            if (s5 > 0 && isAdultStage()) amount *= 1.0F - 0.04F * s5;
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            if (!isTame() && isAdultStage() && this.random.nextInt(3) == 0) frillTicks = Math.max(frillTicks, 40); // defensivo: abre la membrana
            if (isTame() && source.getEntity() instanceof LivingEntity attacker && attacker != this && canHit(attacker) && this.getTarget() == null && !isOrderedToSit() && !isFallen()
                    && getOrder() != PetOrders.SIT && getOrder() != PetOrders.SLEEP && !(attacker instanceof Player))
                this.setTarget(attacker);
        }
        return result;
    }

    // ------------------------------------------------------------------ combate

    @Override public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !canHit(entity)) return false;
        lastCombatTick = this.tickCount;
        boolean hit = living.hurt(this.damageSources().mobAttack(this), attack());
        if (hit) {
            this.playSound(ModSounds.DILO_BITE.get(), isAdultStage() ? 1.2F : 0.8F, (0.95F + this.random.nextFloat() * 0.1F) * vp());
            living.knockback(0.4, this.getX() - entity.getX(), this.getZ() - entity.getZ());
        }
        return hit;
    }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        if ((isFallen() || getMorph() > 0) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return super.isInvulnerableTo(source);
    }

    /** Caida controlada (M4): reduce el dano por caida suyo y el de su jinete. */
    @Override public boolean causeFallDamage(float dist, float mult, DamageSource source) {
        int r = progress.rank("M4");
        float k = r <= 0 || !isAdultStage() ? 1.0F : Math.max(0.0F, 1.0F - new float[]{0F, 0.35F, 0.70F, 1.0F}[r]);
        return super.causeFallDamage(dist, mult * k, source);
    }
    /** Caida controlada (M4) para el jinete: reduccion 0..1 solo mientras lo lleva encima. */
    public float fallReduction() {
        int r = progress.rank("M4");
        return r <= 0 || !isAdultStage() || !(this.getControllingPassenger() instanceof Player) ? 0.0F : new float[]{0F, 0.35F, 0.70F, 1.0F}[r];
    }
    @Override protected float getJumpPower() { return super.getJumpPower() * (isAdultStage() ? 1.30F : 1.05F); }

    // ------------------------------------------------------------------ interaccion

    private static boolean isMeat(ItemStack s) {
        return s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON) || s.is(Items.CHICKEN) || s.is(Items.RABBIT)
            || s.is(Items.COOKED_BEEF) || s.is(Items.COOKED_PORKCHOP) || s.is(Items.COOKED_MUTTON) || s.is(Items.COOKED_CHICKEN) || s.is(Items.COOKED_RABBIT);
    }
    private static boolean isRawMeat(ItemStack s) {
        return s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON) || s.is(Items.CHICKEN) || s.is(Items.RABBIT);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = this.level().isClientSide;
        if (getMorph() > 0) return InteractionResult.sidedSuccess(client);
        if (isFallen()) {
            if (this.isTame() && this.isOwnedBy(player) && held.is(ProdigyCreatures.GOLDEN_MUTTON.get())) {
                if (!client) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    PetOrders.revive(this);
                    this.playSound(ModSounds.DILO_HAPPY.get(), 1.2F, vp());
                }
            } else if (!client && this.isOwnedBy(player)) {
                player.displayClientMessage(Component.literal("Esta fuera de combate: reanimalo con carne de cordero dorada"), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (!this.isTame()) {
            if (!isMeat(held)) return InteractionResult.PASS;
            if (!client) feedWild(player, held);
            return InteractionResult.sidedSuccess(client);
        }
        if (!this.isOwnedBy(player)) return super.mobInteract(player, hand);
        if (isMeat(held)) {
            boolean needsHeal = this.getHealth() < this.getMaxHealth();
            boolean grows = !isAdultStage() && isRawMeat(held);
            if (needsHeal || grows) {
                if (!client) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(isRawMeat(held) ? 6.0F : 10.0F);
                    this.playSound(ModSounds.DILO_EAT.get(), 1.0F, vp());
                    this.level().broadcastEntityEvent(this, (byte) 66);
                    if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                    int need = DiloConfig.get(DiloConfig.FEEDS_TO_GROW, 6);
                    if (grows && ++growth >= need) grow();
                    else if (grows) player.displayClientMessage(Component.literal("Crece: " + growth + "/" + need + " carnes crudas"), true);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS; // ya esta recuperado y no hay nada mas que hacer: no se gasta la carne
        }
        if (isAdultStage() && held.is(Items.SADDLE) && !isSaddled()) {
            if (!client) {
                this.entityData.set(SADDLED, true);
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.playSound(net.minecraft.sounds.SoundEvents.HORSE_SADDLE, 1.0F, 1.1F);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isShiftKeyDown() && held.is(Items.SADDLE) && isSaddled()) {
            if (!client) removeSaddle();
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isShiftKeyDown() && held.isEmpty()) { // Mayus + clic con la mano vacia: cambia la orden (Atacar objetivo solo con el menu)
            if (!client) {
                int next = (getOrder() + 1) % PetOrders.DILO_COUNT;
                if (next == PetOrders.ATTACK) next = PetOrders.FOLLOW;
                PetOrders.setOrder(this, next);
                player.displayClientMessage(Component.literal("Orden: " + PetOrders.DILO_NAMES[next]), true);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (isAdultStage() && isSaddled() && !player.isShiftKeyDown() && held.isEmpty() && !isOrderedToSit()) {
            if (!client) player.startRiding(this);
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    /** Alimentar a un salvaje: cada carne sube su confianza (con una pausa entre bocados); al llegar al total se doma. En Creativo, al instante. */
    private void feedWild(Player player, ItemStack held) {
        if (this.tickCount - lastFeedTick < 30 && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.literal("Aún está masticando... dale un momento"), true);
            return;
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        lastFeedTick = this.tickCount;
        this.playSound(ModSounds.DILO_EAT.get(), 1.0F, vp());
        this.level().broadcastEntityEvent(this, (byte) 66);
        this.setTarget(null);
        this.heal(4.0F);
        trust++;
        int need = trustNeeded();
        if (player.getAbilities().instabuild || trust >= need) {
            this.tame(player);
            this.setPersistenceRequired();
            PetOrders.setOrder(this, PetOrders.FOLLOW);
            this.setTarget(null);
            this.trust = 0;
            this.playSound(ModSounds.DILO_HAPPY.get(), 1.2F, vp());
            this.level().broadcastEntityEvent(this, (byte) 7);   // corazones
            player.displayClientMessage(Component.literal("¡Tu dilofosaurio confía en ti!"), true);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);   // humo: todavia desconfia
            player.displayClientMessage(Component.literal("Confianza: " + trust + "/" + need), true);
        }
    }

    /** Empieza la evolucion: silueta blanca que crece (cinematica); el cambio a adulto ocurre al final, en finishGrow(). */
    private void grow() {
        growth = 0;
        if (getMorph() > 0) return;
        this.ejectPassengers();
        this.getNavigation().stop();
        this.entityData.set(MORPH_KIND, 1);
        this.entityData.set(MORPH, GROW_LEN);
        this.playSound(ModSounds.DILO_EVOLUTION.get(), 2.5F, 1.0F);
    }

    private void finishGrow() {
        growth = 0;
        int guard = 0;
        while (progress.level() < ProdigyProgress.ADULT_START_LEVEL && guard++ < 100) progress.addXp(Math.max(1, progress.xpToNext() - progress.xp()));
        this.entityData.set(PROGRESS, progress.save());
        setAdultStage(true);
        this.level().broadcastEntityEvent(this, (byte) 64);
        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.0, getZ(), 36, 0.7, 0.9, 0.7, 0.3);
        if (this.getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Tu dilofosaurio es ahora un adulto"), true);
    }

    private void finishRevert() {
        this.growth = 0;
        setAdultStage(false);
        this.level().broadcastEntityEvent(this, (byte) 65);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.7, getZ(), 20, 0.5, 0.4, 0.5, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 12, 0.4, 0.4, 0.4, 0.06);
        }
        if (this.getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Tu dilofosaurio volvió a ser una cría"), true);
    }

    /** Cuenta atras de la transformacion (solo servidor): remolino de luz y chispas; al llegar a 0 cambia de etapa y destella. */
    private void morphTick() {
        int left = getMorph();
        if (left <= 0 || !(this.level() instanceof ServerLevel sl)) return;
        boolean up = getMorphKind() == 1;
        int len = up ? GROW_LEN : SHRINK_LEN, e = len - left;
        double p = e / (double) len, cx = getX(), cy = getY(), cz = getZ();
        this.getNavigation().stop();
        Vec3 mv = this.getDeltaMovement();
        this.setDeltaMovement(0.0, Math.min(0.0, mv.y), 0.0);
        if (e == 0) sl.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.8, cz, 26, 0.4, 0.4, 0.4, 0.15);
        if (e % 2 == 0) {
            for (int k = 0; k < 3; k++) {
                double a = e * 0.35 + k * 2.094, r = up ? 1.8 * (1.0 - p) + 0.5 : 0.5 + 1.3 * p;
                double h = up ? 0.2 + 2.2 * p : 1.7 - 1.4 * p;
                sl.sendParticles(ParticleTypes.END_ROD, cx + Math.cos(a) * r, cy + h, cz + Math.sin(a) * r, 1, 0, 0.03, 0, 0.0);
            }
        }
        if (up && e % 6 == 0) sl.sendParticles(variantParticle(), cx + (this.random.nextDouble() - 0.5) * 2.2, cy + this.random.nextDouble() * 2.6, cz + (this.random.nextDouble() - 0.5) * 2.2, 3, 0.1, 0.1, 0.1, 0.05);
        if (up && left == 5) sl.sendParticles(ParticleTypes.FIREWORK, cx, cy + 1.3, cz, 50, 1.0, 1.0, 1.0, 0.12);
        this.entityData.set(MORPH, left - 1);
        if (left == 1) { if (up) finishGrow(); else finishRevert(); }
    }

    // ------------------------------------------------------------------ montura

    @Override protected boolean canAddPassenger(Entity passenger) {
        return isSaddled() && isAdultStage() && this.getPassengers().isEmpty() && passenger instanceof Player p && this.isOwnedBy(p);
    }
    @Nullable @Override public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof Player p && this.isOwnedBy(p) ? p : null;
    }
    /** Altura de la silla ligera (~2,0 bloques sobre el suelo) menos el desfase del jugador sentado. */
    @Override public double getPassengersRidingOffset() { return 1.22; }
    @Override public void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        Vec3 off = new Vec3(0.0, 0.0, -0.1).yRot(-this.yBodyRot * ((float) Math.PI / 180.0F));
        callback.accept(passenger, this.getX() + off.x, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + off.z);
    }

    @Override public void travel(Vec3 travelVector) {
        if (isFallen()) { this.calculateEntityAnimation(false); return; }
        if (this.isAlive() && this.isVehicle() && this.getControllingPassenger() instanceof Player rider) {
            boolean pouncing = isPouncing();
            float turnCap = pouncing ? 2.5F : (rider.zza > 0.0F ? 7.0F : 11.0F);
            this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(rider.getYRot() - this.getYRot()), -turnCap, turnCap));
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.4F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;
            if (pouncing) {
                if (this.isControlledByLocalInstance()) {
                    int el = POUNCE_LEN - getAbilityLeft();
                    Vec3 d = Vec3.directionFromRotation(0.0F, this.getYRot()), mv = this.getDeltaMovement();
                    if (el >= 5 && !leapApplied) { leapApplied = true; double sp = pounceSpeed(); this.setDeltaMovement(d.x * sp, 0.50 + 0.02 * progress.rank("M2"), d.z * sp); this.hasImpulse = true; }
                    else if (el < 5) this.setDeltaMovement(0.0, mv.y, 0.0);
                    super.travel(Vec3.ZERO);
                } else this.setDeltaMovement(Vec3.ZERO);
                this.calculateEntityAnimation(false);
                return;
            }
            leapApplied = false;
            float strafe = rider.xxa * 0.45F, forward = rider.zza;
            if (forward <= 0.0F) forward *= 0.3F;
            if (this.isControlledByLocalInstance()) {
                this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.95F);
                if (clientJumpHeld && this.onGround()) this.jumpFromGround();   // salto del jinete (tecla de salto del cliente que controla)
                super.travel(new Vec3(strafe, travelVector.y, forward));
            } else this.setDeltaMovement(Vec3.ZERO);
            this.calculateEntityAnimation(false);
        } else if (isPouncing() && !this.isVehicle()) {
            // salto sin jinete: el impulso se aplica en abilityTick; durante el vuelo no hay direccion propia
            super.travel(Vec3.ZERO.add(0.0, travelVector.y, 0.0));
        } else super.travel(travelVector);
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // hembra salvaje adulta abatida: 50 % de dejar un huevo (la variante sale por rareza, con sesgo hacia la suya)
        if (!this.isTame() && isFemale() && isAdultStage() && recentlyHit && this.random.nextBoolean())
            this.spawnAtLocation(new ItemStack(ProdigyCreatures.DILO_EGG_ITEMS.get(com.prodigytown.creatures.egg.DilophosaurusEggs.rollVariant(this.random, getVariant())).get()));
        if (isSaddled()) this.spawnAtLocation(Items.SADDLE);
    }

    // ------------------------------------------------------------------ sonidos y varios

    @Override protected SoundEvent getHurtSound(DamageSource source) { return isAdultStage() ? ModSounds.DILO_HURT.get() : ModSounds.DILO_HURT_BABY.get(); }
    @Override protected SoundEvent getDeathSound() { return isAdultStage() ? ModSounds.DILO_DEATH.get() : ModSounds.DILO_HURT_BABY.get(); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.DILO_STEP.get(), isAdultStage() ? 0.5F : 0.25F, (0.95F + this.random.nextFloat() * 0.25F) * vp());
    }
    @Override protected float getSoundVolume() { return isAdultStage() ? 1.2F : 1.0F; }
    @Nullable @Override protected SoundEvent getAmbientSound() {
        if (isFallen() || getMorph() > 0 || getOrder() == PetOrders.SLEEP) return null;
        return isAdultStage() ? ModSounds.DILO_AMBIENT.get() : ModSounds.DILO_AMBIENT_BABY.get();
    }

    @Override public int getMaxSpawnClusterSize() { return 3; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean removeWhenFarAway(double distance) { return !isTame() && super.removeWhenFarAway(distance); }

    public static boolean roomFor(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
