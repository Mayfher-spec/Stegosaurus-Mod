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
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.IronGolem;
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
import com.prodigytown.creatures.progress.GrowthTable;
import com.prodigytown.creatures.progress.ProdigyPet;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talents;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Carnotauro: segundo dinosaurio del mod. Carnivoro de ataque (mas rapido y mas agil que el estegosaurio, con menos vida).
 * Una sola entidad con dos etapas: cria (chibi) y adulto. Se doma con carne cruda; la cria crece comiendo carne.
 * Adulto: montable con silla, embestida (V), cabezazo con cuernos (B / clic derecho), mordida (clic izquierdo) y rugido (R).
 * Comparte con el estegosaurio las ordenes y el estado "caido" (ver PetOrders y PetEvents).
 */
public class CarnotaurusEntity extends TamableAnimal implements PetOrders.Holder, ProdigyPet {
    /** Colores: Rojo, Verde, Acero y Arena. El adulto es siempre el "Brutus": grande, robusto y agresivo. */
    public static final int SCARLET = 0, GREEN = 1, STEEL = 2, ARENA = 3, VARIANTS = 4;
    public static final String[] VARIANT_NAMES = {"Rojo", "Verde", "Acero", "Arena"};
    public static final int CHARGE_LEN = 24, BUTT_LEN = 14, ROAR_LEN = 40;
    /** Furia: por debajo de este porcentaje de vida el adulto corre y muerde mas fuerte. */
    private static final float ENRAGE_BELOW = 0.40F;
    private static final UUID ENRAGE_SPEED_ID = UUID.fromString("5c1d0f5e-3d7a-4b8e-9a52-7c1e2b0a9d11");
    private static final UUID M_HP_ADD = UUID.fromString("c4a7b0de-0001-4d1e-9a10-5e90a5a10001");
    private static final UUID M_HP_MUL = UUID.fromString("c4a7b0de-0002-4d1e-9a10-5e90a5a10002");
    private static final UUID M_DMG_ADD = UUID.fromString("c4a7b0de-0003-4d1e-9a10-5e90a5a10003");
    private static final UUID M_DMG_MUL = UUID.fromString("c4a7b0de-0004-4d1e-9a10-5e90a5a10004");
    private static final UUID M_ARM_ADD = UUID.fromString("c4a7b0de-0005-4d1e-9a10-5e90a5a10005");
    private static final UUID M_KB_ADD = UUID.fromString("c4a7b0de-0006-4d1e-9a10-5e90a5a10006");
    private static final UUID M_SPD_MUL = UUID.fromString("c4a7b0de-0007-4d1e-9a10-5e90a5a10007");
    private static final UUID M_ARM_GEAR = UUID.fromString("c4a7b0de-0008-4d1e-9a10-5e90a5a10008");
    private static final UUID M_TGH_GEAR = UUID.fromString("c4a7b0de-0009-4d1e-9a10-5e90a5a10009");
    /** Estadisticas del adulto en el nivel 30 (220 PV, 26 de dano, 10 de armadura) frente a la fila 30 de la tabla compartida (270 / 27 / 14). */
    /** Cifras del adulto a nivel 30 (la tabla de crecimiento se escala con K_*): por debajo del T-Rex (250 de vida), tambien en estado salvaje. */
    private static final double ADULT_HP = 140.0, ADULT_DMG = 18.0, ADULT_ARM = 5.0;
    private static final double K_HP = ADULT_HP / 270.0, K_DMG = ADULT_DMG / 27.0, K_ARM = ADULT_ARM / 14.0;
    private static final UUID ENRAGE_DAMAGE_ID = UUID.fromString("a8e3c6b1-52f4-4d0b-8c6e-1f9b7d2e4a22");
    /** Carnes crudas que necesita la cria para convertirse en adulto. */
    public static final int FEEDS_TO_GROW = 6;
    /** El adulto mide lo mismo que el tipo registrado (ProdigyCreatures.CARNOTAURUS: sized 2.7 x 3.8), asi la comprobacion de espacio al aparecer ya lo cuenta. */
    private static final EntityDimensions ADULT_DIM = EntityDimensions.fixed(2.7F, 3.8F);
    private static final EntityDimensions CHIBI_DIM = EntityDimensions.fixed(1.1F, 1.8F);

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    /** Sexo: 50 % machos, 50 % hembras. Solo las hembras silvestres pueden dejar un huevo al caer. */
    private static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean sexAssigned = false;
    private static final EntityDataAccessor<Boolean> ADULT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    /** Armadura equipada: 0 = ninguna, 1 hierro, 2 oro, 3 diamante, 4 netherita (solo el adulto la lleva). */
    private static final EntityDataAccessor<Integer> ARMOR_TIER = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FALLEN = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ABILITY_KIND = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ABILITY_LEFT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> PROGRESS = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** Recargas para el HUD: (tick de fin << 12) | duracion. 0 embestida, 1 cabezazo, 2 mordida, 3 rugido. */
    /** Transformacion (cinematica): ticks que faltan y tipo (1 = crece a adulto, 2 = vuelve a cria). */
    private static final EntityDataAccessor<Integer> MORPH = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MORPH_KIND = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.INT);
    public static final int GROW_LEN = 56, SHRINK_LEN = 36;
    private static final EntityDataAccessor<Long> CD_CHARGE = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_BUTT = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_BITE = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_ROAR = SynchedEntityData.defineId(CarnotaurusEntity.class, EntityDataSerializers.LONG);

    /**
     * Solo cliente: estado de animacion suavizado. Cada valor guarda su version del tick anterior (...O) para interpolar con el tiempo parcial.
     * gait = fase de la zancada (avanza con la distancia recorrida, asi los pies no resbalan); speedAnim = velocidad horizontal suavizada (bloques/tick);
     * runAnim = 0 andando .. 1 corriendo/embistiendo; aggroAnim = postura de caza; turnAnim = giro (grados/tick).
     */
    public float sitAnim, sleepAnim, sitAnimO, sleepAnimO;
    /** Solo cliente: ticks restantes del destello tras transformarse (0 = sin destello) y su tipo (0 = crecer, 1 = encoger). */
    public int flashFade, flashKind;
    /** Mordida visual (cliente): ticks desde que empezo el golpe (-1 = sin golpe). Dura BITE_LEN ticks, mucho mas que el vaiven de 6 ticks de vanilla, para abrir bien la boca. */
    public static final int BITE_LEN = 16;
    public int biteT = -1, biteTO = -1;
    public float gait, gaitO, speedAnim, speedAnimO, runAnim, runAnimO, aggroAnim, aggroAnimO, turnAnim, turnAnimO;
    private int growth;
    @Nullable private BlockPos orderHome;
    private int cdCharge, cdButt, cdRoar, cdBite;
    private ProdigyProgress progress = ProdigyProgress.atLevel(ProdigyProgress.ADULT_START_LEVEL, Talents.CARNO);
    private int lastCombatTick = -100000, guardUntil = -1, guardPct = 0, guardReadyAt = 0, aidReadyAt = 0;
    /** IA: pausa entre rugidos de guerra y entre embestidas, objetivo del ultimo rugido, furia y objetivo inalcanzable (se ignora un rato). */
    private int cdAiRoar, cdAiCharge, growlTimer, unreachableId = -1, unreachableUntil;
    private int lastRoarTargetId = -1;
    private boolean enraged, enrageRoarDone;
    private final SmoothMoveControl smoothMove;
    private final Set<Integer> chargeHit = new HashSet<>();

    public CarnotaurusEntity(EntityType<? extends CarnotaurusEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.smoothMove = new SmoothMoveControl(this, 6.0F);
        this.moveControl = this.smoothMove;
        this.lookControl = new PetOrders.PetLookControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 110.0)
            .add(Attributes.MOVEMENT_SPEED, 0.34)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    // ------------------------------------------------------------------ datos

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, SCARLET);
        this.entityData.define(FEMALE, Boolean.FALSE);
        this.entityData.define(ADULT, Boolean.TRUE);
        this.entityData.define(SADDLED, Boolean.FALSE);
        this.entityData.define(ARMOR_TIER, 0);
        this.entityData.define(ORDER, PetOrders.FOLLOW);
        this.entityData.define(FALLEN, Boolean.FALSE);
        this.entityData.define(ABILITY_KIND, 0);
        this.entityData.define(ABILITY_LEFT, 0);
        this.entityData.define(PROGRESS, ProdigyProgress.atLevel(ProdigyProgress.ADULT_START_LEVEL, Talents.CARNO).save());
        this.entityData.define(MORPH, 0);
        this.entityData.define(MORPH_KIND, 0);
        this.entityData.define(CD_CHARGE, 0L);
        this.entityData.define(CD_BUTT, 0L);
        this.entityData.define(CD_BITE, 0L);
        this.entityData.define(CD_ROAR, 0L);
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
        this.entityData.set(MORPH, tag.getInt("Morph"));
        this.entityData.set(MORPH_KIND, tag.getInt("MorphKind"));
        // sin progreso guardado: los adultos de antes cargan en el nivel 30 y las crias en el 1
        this.progress = ProdigyProgress.load(tag.getCompound("Progress"), isAdultStage() ? ProdigyProgress.ADULT_START_LEVEL : 1, Talents.CARNO);
        this.entityData.set(PROGRESS, progress.save());
        this.entityData.set(SADDLED, tag.getBoolean("Saddled"));
        this.entityData.set(ARMOR_TIER, isAdultStage() ? Math.max(0, Math.min(4, tag.getInt("ArmorTier"))) : 0);
        this.entityData.set(ORDER, PetOrders.clamp(tag.getInt("PetOrder")));
        this.entityData.set(FALLEN, tag.getBoolean("PetFallen"));
        this.orderHome = tag.contains("OrderHome") ? BlockPos.of(tag.getLong("OrderHome")) : null;
        applyStageStats(false);
        if (isTame()) this.setPersistenceRequired();
    }

    public boolean isFemale() { return this.entityData.get(FEMALE); }
    private void assignSex() {
        this.entityData.set(FEMALE, this.random.nextBoolean());
        this.sexAssigned = true;
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int v) { this.entityData.set(VARIANT, Math.max(0, Math.min(VARIANTS - 1, v))); }
    public boolean isAdultStage() { return this.entityData.get(ADULT); }
    public int getMorph() { return this.entityData.get(MORPH); }
    public int getMorphKind() { return this.entityData.get(MORPH_KIND); }
    @Override protected boolean isImmobile() { return super.isImmobile() || getMorph() > 0; }
    @Override public void handleEntityEvent(byte id) {
        if (id == 64 || id == 65) { this.flashFade = 24; this.flashKind = id == 64 ? 0 : 1; }
        else super.handleEntityEvent(id);
    }
    /** Multiplicador de tono: el Brutus suena mas grave. */
    private static float vp() { return 1.0F; }
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
        if (PROGRESS.equals(key)) this.progress = ProdigyProgress.load(this.entityData.get(PROGRESS), ProdigyProgress.ADULT_START_LEVEL, Talents.CARNO);
    }

    @Override public EntityDimensions getDimensions(Pose pose) {
        return isAdultStage() ? ADULT_DIM : CHIBI_DIM;
    }
    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) { return dims.height * 0.82F; }

    /** Ajusta vida, ataque y velocidad a la etapa; conserva la fraccion de vida. */
    private void applyStageStats(boolean keepFraction) {
        boolean adult = isAdultStage();
        float frac = this.getMaxHealth() <= 0 ? 1.0F : this.getHealth() / this.getMaxHealth();
        // el Brutus: mucha vida, daño y armadura, y muy veloz (cazador de persecucion)
        setBase(Attributes.MAX_HEALTH, adult ? ADULT_HP : 50.0);
        setBase(Attributes.ATTACK_DAMAGE, adult ? ADULT_DMG : 7.0);
        setBase(Attributes.MOVEMENT_SPEED, adult ? 0.34 : 0.30);
        setBase(Attributes.ARMOR, adult ? ADULT_ARM : 2.0);
        setBase(Attributes.KNOCKBACK_RESISTANCE, 0.85);
        applyProgressStats(adult);
        if (keepFraction) this.setHealth(Math.max(1.0F, this.getMaxHealth() * frac));
        else if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
        this.refreshDimensions();
    }
    /** Nivel y talentos -> modificadores de atributos (solo el adulto; la cria no usa la tabla). */
    private void applyProgressStats(boolean adult) {
        int lv = progress.level();
        setMod(Attributes.MAX_HEALTH, M_HP_ADD, adult ? GrowthTable.HEALTH[lv] * K_HP - ADULT_HP : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.MAX_HEALTH, M_HP_MUL, adult ? 0.04 * progress.rank("D2") : 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG_ADD, adult ? GrowthTable.DAMAGE[lv] * K_DMG - ADULT_DMG : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG_MUL, adult ? 0.05 * progress.rank("C1") + 0.10 * progress.rank("C7") : 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        setMod(Attributes.ARMOR, M_ARM_ADD, adult ? GrowthTable.ARMOR[lv] * K_ARM - ADULT_ARM + progress.rank("D1") : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.KNOCKBACK_RESISTANCE, M_KB_ADD, adult ? 0.05 * progress.rank("D3") : 0.0, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.MOVEMENT_SPEED, M_SPD_MUL, adult ? 0.02 * progress.rank("S1") : 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        int gear = adult ? getArmorTier() : 0;   // mismos valores que la armadura del estegosaurio
        setMod(Attributes.ARMOR, M_ARM_GEAR, StegosaurusEntity.ARMOR_VALUE[gear], AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ARMOR_TOUGHNESS, M_TGH_GEAR, StegosaurusEntity.TOUGHNESS_VALUE[gear], AttributeModifier.Operation.ADDITION);
    }
    private void setMod(Attribute attr, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, "prodigy_progress", amount, op));
    }
    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute attr, double v) {
        var inst = this.getAttribute(attr);
        if (inst != null) inst.setBaseValue(v);
    }

    /** Recien nacido de un huevo: chibi del color del huevo, domado y a tu lado si hay dueno. */
    public void initFromEgg(int color, @Nullable java.util.UUID owner) {
        setVariant(color);
        this.progress = ProdigyProgress.atLevel(1, Talents.CARNO);
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
        int arenaChance = 8, greenChance = 15, steelChance = 15;
        if (biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS)) { arenaChance = 18; greenChance = 25; }
        if (biome.is(net.minecraft.tags.BiomeTags.IS_FOREST) || biome.is(net.minecraft.tags.BiomeTags.IS_JUNGLE)) steelChance = 35;
        int v = roll < arenaChance ? ARENA
            : roll < arenaChance + greenChance ? GREEN
            : roll < arenaChance + greenChance + steelChance ? STEEL : SCARLET;
        setVariant(v);
        if (!sexAssigned) assignSex(); // 50 % machos, 50 % hembras (los carnotauros viejos sin sexo lo reciben al aparecer o al cargarse)
        // en estado salvaje solo aparecen adultos: las crias vienen de los huevos que dejan las hembras
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.STRUCTURE) {
            this.entityData.set(ADULT, true);
        }
        applyStageStats(false);
        this.setHealth(this.getMaxHealth());
        return result;
    }

    // ------------------------------------------------------------------ IA

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(1, new BattleRoarGoal());   // ruge al ver una presa lejana y al entrar en furia
        this.goalSelector.addGoal(1, new BrutalChargeGoal()); // embestida a la carrera contra la presa
        this.goalSelector.addGoal(2, new BrutusMeleeGoal());  // persecucion y mordidas rapidas
        this.goalSelector.addGoal(3, new FollowGoal());
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.9));
        this.goalSelector.addGoal(5, new PetOrders.PatrolGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.5, 60) { // ronda inquieta pero sin trotar: el galope se reserva para la caza
            @Override public boolean canUse() { return !isTame() || (getOrder() != PetOrders.STAY && getOrder() != PetOrders.PATROL && getOrder() != PetOrders.FOLLOW && !isFallen() && super.canUse()); }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this, CarnotaurusEntity.class).setAlertOthers());
        this.targetSelector.addGoal(4, new PreyGoal(this));
    }

    /**
     * Presas del adulto. Silvestre: ataca TODO lo que ve (jugadores, aldeanos, golems, monstruos, ganado...) salvo los de su especie, las mascotas domadas,
     * los bichos que no puede alcanzar (agua, aire) y los soportes de armadura. Domado: caza monstruos (y animales si su orden es Deambular),
     * pero nunca a jugadores, aldeanos, golems ni mascotas.
     */
    private static boolean isPrey(CarnotaurusEntity self, LivingEntity e) {
        if (e == self || e instanceof CarnotaurusEntity || !e.isAlive()) return false;
        if (e instanceof ArmorStand || e instanceof Bat || e instanceof WaterAnimal || e instanceof FlyingMob || e instanceof FlyingAnimal) return false;
        if (e.getY() - self.getY() > 5.0) return false;
        if (e.getId() == self.unreachableId && self.tickCount < self.unreachableUntil) return false;
        if (self.isAlly(e)) return false;
        if (e instanceof TamableAnimal t && t.isTame()) return false;
        if (self.isTame()) {
            if (e instanceof Player || e instanceof AbstractVillager || e instanceof IronGolem) return false;
            LivingEntity owner = self.getOwner();
            if (owner != null && e.distanceToSqr(owner) > 28.0 * 28.0) return false; // no se aleja a perseguir lo que esta lejos de su dueno
            return e instanceof Enemy || (self.getOrder() == PetOrders.WANDER && e instanceof Animal);
        }
        return true;
    }

    /** Busca presa cada pocos ticks en todo su rango de vision (64 bloques); solo el adulto, y no sentado, dormido ni caido. */
    private static final class PreyGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private final CarnotaurusEntity self;
        PreyGoal(CarnotaurusEntity self) {
            super(self, LivingEntity.class, 4, true, false, e -> isPrey(self, e));
            this.self = self;
        }
        private boolean active() {
            return self.isAdultStage() && !self.isFallen() && !(self.isTame() && PetOrders.isSittingOrder(self.getOrder())) && self.stayOk();
        }
        @Override public boolean canUse() { return active() && super.canUse(); }
        @Override public boolean canContinueToUse() { return active() && super.canContinueToUse(); }
    }

    private boolean stayOk() {
        if (!isTame() || getOrder() != PetOrders.STAY) return true;
        LivingEntity t = getTarget();
        return t != null && this.distanceToSqr(t) < 36.0;
    }

    /** Giro suave de un angulo hacia otro, como mucho step grados. */
    private static float turnToward(float cur, float want, float step) {
        return cur + Mth.clamp(Mth.wrapDegrees(want - cur), -step, step);
    }

    /** Rumbo (grados de entidad) desde la posicion de la bestia hacia un objetivo. */
    private float yawTo(LivingEntity t) {
        return (float) (Mth.atan2(t.getZ() - this.getZ(), t.getX() - this.getX()) * 57.29577951308232) - 90.0F;
    }

    /** Persecucion: corre hacia la presa y muerde sin parar (el adulto cada 0,6 s). Si no hay camino hasta ella, la olvida un rato. */
    private final class BrutusMeleeGoal extends Goal {
        private int repath, cooldown, noPath;
        BrutusMeleeGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        private boolean valid(@Nullable LivingEntity t) {
            return t != null && t.isAlive() && !isAlly(t) && !isFallen() && stayOk() && !isOrderedToSit()
                && !(t instanceof Player p && (p.isCreative() || p.isSpectator()));
        }
        @Override public boolean canUse() { return valid(CarnotaurusEntity.this.getTarget()); }
        @Override public boolean canContinueToUse() { return valid(CarnotaurusEntity.this.getTarget()); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void start() { repath = 0; noPath = 0; cooldown = 6; }
        @Override public void stop() { CarnotaurusEntity.this.getNavigation().stop(); }
        @Override public void tick() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            LivingEntity t = me.getTarget();
            if (t == null) return;
            me.getLookControl().setLookAt(t, 40.0F, 40.0F);
            boolean adult = isAdultStage();
            double d2 = me.distanceToSqr(t.getX(), t.getY(), t.getZ());
            double reach2 = (double) (me.getBbWidth() * 2.0F * me.getBbWidth() * 2.0F) + t.getBbWidth();
            if (--repath <= 0) {
                repath = 3 + me.random.nextInt(4);
                if (d2 > reach2 * 0.6) {
                    double sp = adult ? (d2 > 100.0 ? 1.35 : 1.2) : 1.15;
                    if (me.getNavigation().moveTo(t, sp)) noPath = 0;
                    else if (++noPath >= 6 && d2 > reach2) { // sin camino: la olvida unos segundos
                        me.unreachableId = t.getId();
                        me.unreachableUntil = me.tickCount + 200;
                        me.setTarget(null);
                        return;
                    }
                } else me.getNavigation().stop();
            }
            if (cooldown > 0) cooldown--;
            if (cooldown <= 0 && d2 <= reach2 && me.getSensing().hasLineOfSight(t)) {
                cooldown = adult ? (enraged ? 9 : 12) : 20;
                me.swing(InteractionHand.MAIN_HAND);
                me.doHurtTarget(t);
            }
        }
    }

    /** Rugido de guerra: al ver una presa lejana (una vez por presa) y al entrar en furia. Se queda quieto y ruge; en furia es casi invulnerable mientras ruge. */
    private final class BattleRoarGoal extends Goal {
        BattleRoarGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        @Override public boolean canUse() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            LivingEntity t = me.getTarget();
            if (t == null || !t.isAlive() || !isAdultStage() || isFallen() || me.isVehicle() || isOrderedToSit() || !stayOk()) return false;
            if (getAbilityLeft() > 0 || isAlly(t)) return false;
            if (enraged && !enrageRoarDone) return true; // la furia ruge aunque acabe de rugir
            if (cdAiRoar > 0) return false;
            double d2 = me.distanceToSqr(t);
            return t.getId() != lastRoarTargetId && d2 > 49.0 && d2 < 48.0 * 48.0 && me.getSensing().hasLineOfSight(t);
        }
        @Override public boolean canContinueToUse() { return getAbilityKind() == 3 && getAbilityLeft() > 0 && CarnotaurusEntity.this.getTarget() != null; }
        @Override public void start() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            LivingEntity t = me.getTarget();
            me.getNavigation().stop();
            if (t != null) lastRoarTargetId = t.getId();
            if (enraged && !enrageRoarDone) {
                enrageRoarDone = true;
                me.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ROAR_LEN + 10, 1));
            }
            cdAiRoar = 400;
            startAbility(3, ROAR_LEN);
            if (me.level() instanceof ServerLevel server) roar(server, !me.isTame());
        }
        @Override public void tick() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            me.getNavigation().stop();
            LivingEntity t = me.getTarget();
            if (t != null) {
                me.getLookControl().setLookAt(t, 40.0F, 40.0F);
                me.setYRot(turnToward(me.getYRot(), me.yawTo(t), 6.0F));
            }
        }
    }

    /** Embestida a la carrera: si la presa esta de frente entre 10 y 26 bloques, baja la cabeza y arrolla en linea recta (golpea mas fuerte que una mordida). */
    private final class BrutalChargeGoal extends Goal {
        BrutalChargeGoal() { this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        @Override public boolean canUse() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            LivingEntity t = me.getTarget();
            if (t == null || !t.isAlive() || !isAdultStage() || isFallen() || me.isVehicle() || isOrderedToSit() || !stayOk() || isAlly(t)) return false;
            if (cdAiCharge > 0 || cdCharge > 0 || getAbilityLeft() > 0 || !me.onGround()) return false;
            double d2 = me.distanceToSqr(t);
            if (d2 < 100.0 || d2 > 26.0 * 26.0 || Math.abs(t.getY() - me.getY()) > 3.0) return false;
            if (Math.abs(Mth.wrapDegrees(me.yawTo(t) - me.getYRot())) > 40.0F || !me.getSensing().hasLineOfSight(t)) return false;
            return me.random.nextInt(5) == 0;
        }
        @Override public boolean canContinueToUse() {
            LivingEntity t = CarnotaurusEntity.this.getTarget();
            return isCharging() && t != null && t.isAlive() && !CarnotaurusEntity.this.isVehicle();
        }
        @Override public void start() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            me.getNavigation().stop();
            chargeHit.clear();
            cdAiCharge = 120 + me.random.nextInt(80);
            cdCharge = 100;
            startAbility(1, CHARGE_LEN);
            me.playSound(ModSounds.CARNO_ROAR.get(), 2.2F, 1.25F * vp());
        }
        @Override public void tick() {
            CarnotaurusEntity me = CarnotaurusEntity.this;
            LivingEntity t = me.getTarget();
            if (t == null) return;
            me.getLookControl().setLookAt(t, 60.0F, 60.0F);
            me.setYRot(turnToward(me.getYRot(), me.yawTo(t), 5.0F)); // la embestida se corrige un poco hacia la presa
            me.yRotO = me.getYRot();
            me.yBodyRot = me.getYRot();
        }
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
            if (dist >= rejoinDistance() && CarnotaurusEntity.this.level() instanceof ServerLevel) {
                CarnotaurusEntity.this.teleportTo(o.getX(), o.getY(), o.getZ());
                CarnotaurusEntity.this.getNavigation().stop();
                return;
            }
            double sp = dist > 24.0 ? 1.4 : 0.85;
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
            clientAnimTick();
        } else {
            if (!sexAssigned) assignSex();
            if (cdCharge > 0) cdCharge--;
            if (cdButt > 0) cdButt--;
            if (cdRoar > 0) cdRoar--;
            if (cdBite > 0) cdBite--;
            if (cdAiRoar > 0) cdAiRoar--;
            if (cdAiCharge > 0) cdAiCharge--;
            morphTick();
            abilityTick();
            huntTick();
            serverProgressTick();
            PetOrders.serverTick(this);
        }
    }

    /**
     * Cliente: actualiza el estado de animacion suavizado. La fase de la zancada avanza con la distancia realmente recorrida
     * (con la longitud de zancada que crece con la velocidad), asi las patas siguen al suelo y no patinan ni van a otro ritmo que el cuerpo.
     */
    private void clientAnimTick() {
        sitAnimO = sitAnim; sleepAnimO = sleepAnim;
        if (flashFade > 0) flashFade--;
        biteTO = biteT;
        if (this.swinging && this.swingTime <= 0) { biteT = 0; biteTO = 0; }
        else if (biteT >= 0) biteT = biteT + 1 >= BITE_LEN ? -1 : biteT + 1;
        gaitO = gait; speedAnimO = speedAnim; runAnimO = runAnim; aggroAnimO = aggroAnim; turnAnimO = turnAnim;
        sitAnim += ((getOrder() == PetOrders.SIT ? 1.0F : 0.0F) - sitAnim) * 0.12F;
        sleepAnim += (((getOrder() == PetOrders.SLEEP || isFallen()) ? 1.0F : 0.0F) - sleepAnim) * 0.08F;

        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        float v = (float) Math.sqrt(dx * dx + dz * dz);
        if (v > 1.6F) v = 0.0F; // salto de posicion (teletransporte): no cuenta como carrera
        speedAnim += (v - speedAnim) * 0.30F;
        boolean adult = isAdultStage();
        // cadencia que hace que el pie apoyado vaya a la velocidad del suelo: avance medio de la fase de apoyo = (2/pi) * amplitud * largo de pierna * cadencia.
        // Al correr muy rapido la cadencia se limita (no puede girar mas deprisa que un trote vivo) y las patas "ganan" terreno con zancadas mas amplias.
        float sp01 = Mth.clamp(speedAnim / 0.9F, 0.0F, 1.0F);
        float legLen = adult ? 1.8F : 0.95F; // largo de la pierna en bloques (cadera -> suelo)
        float natural = speedAnim > 0.004F ? 1.5707964F * speedAnim / (strideAmp(speedAnim, adult) * legLen) : 0.0F;
        gait += Math.min(natural, adult ? 0.58F + 0.12F * sp01 : 0.90F);
        if (gait > 628.3F) { gait -= 628.31854F; gaitO -= 628.31854F; }

        float runTarget = isCharging() ? 1.0F : Mth.clamp((speedAnim - 0.32F) / 0.45F, 0.0F, 1.0F);
        runAnim += (runTarget - runAnim) * 0.22F;
        aggroAnim += ((isAggressive() ? 1.0F : 0.0F) - aggroAnim) * 0.12F;
        turnAnim += (Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO) - turnAnim) * 0.25F;
    }

    /** Amplitud (radianes) del vaiven del muslo segun la velocidad (bloques/tick): paso corto al pasear, zancada larga al correr. La usan la entidad (cadencia) y el modelo (pose). */
    public static float strideAmp(float speed, boolean adult) {
        float t = Mth.clamp(speed / 0.9F, 0.0F, 1.0F);
        return adult ? Mth.lerp(t, 0.38F, 0.75F) : Mth.lerp(t, 0.40F, 0.62F);
    }

    /** Servidor: postura de caza sincronizada (flag "agresivo" de Mob), giro mas rapido persiguiendo, furia con poca vida y gruñidos. */
    private void huntTick() {
        LivingEntity t = this.getTarget();
        boolean hunting = t != null && t.isAlive();
        this.setAggressive(hunting);
        this.smoothMove.setMaxTurn(isCharging() ? 3.0F : hunting && isAdultStage() ? 13.0F : 6.0F);
        if (this.tickCount % 10 == 0) updateEnrage(hunting);
        if (hunting && isAdultStage() && !isFallen() && getAbilityLeft() == 0 && this.distanceToSqr(t) < 28.0 * 28.0 && --growlTimer <= 0) {
            growlTimer = 70 + this.random.nextInt(70);
            this.playSound(ModSounds.CARNO_ROAR.get(), 1.1F, 1.55F * vp() + this.random.nextFloat() * 0.12F);
        }
        if (enraged && this.tickCount % 20 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getBbHeight() * 0.9, getZ(), 2, 0.5, 0.2, 0.5, 0.0);
        }
    }

    /** Furia: por debajo del 40 % de vida y con presa, +15 % de velocidad y +25 % de dano hasta que se calme o se cure. */
    private void updateEnrage(boolean hunting) {
        boolean should = hunting && isAdultStage() && !isFallen() && this.getHealth() < this.getMaxHealth() * ENRAGE_BELOW;
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (should && !enraged) {
            enraged = true;
            if (speed != null && speed.getModifier(ENRAGE_SPEED_ID) == null)
                speed.addTransientModifier(new AttributeModifier(ENRAGE_SPEED_ID, "Furia", 0.15, AttributeModifier.Operation.MULTIPLY_BASE));
            if (damage != null && damage.getModifier(ENRAGE_DAMAGE_ID) == null)
                damage.addTransientModifier(new AttributeModifier(ENRAGE_DAMAGE_ID, "Furia", 0.25, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!should && enraged) {
            enraged = false;
            if (speed != null) speed.removeModifier(ENRAGE_SPEED_ID);
            if (damage != null) damage.removeModifier(ENRAGE_DAMAGE_ID);
        }
        if (!enraged && this.getHealth() > this.getMaxHealth() * 0.7F) enrageRoarDone = false; // podra volver a rugir de furia
    }

    private void abilityTick() {
        int left = getAbilityLeft();
        if (left <= 0) {
            if (getAbilityKind() != 0) this.entityData.set(ABILITY_KIND, 0);
            return;
        }
        this.entityData.set(ABILITY_LEFT, left - 1);
        if (getAbilityKind() == 1) {
            chargeHits();
            // embestida de la IA (sin jinete): termina enseguida al chocar con algo o contra un muro
            if (this.getControllingPassenger() == null && left > 3 && (!chargeHit.isEmpty() || this.horizontalCollision)) this.entityData.set(ABILITY_LEFT, 2);
        }
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
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.4F + 0.2F * progress.rank("C3")) * furyMultiplier();
        boolean ridden = this.getControllingPassenger() != null;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, x -> !isAlly(x) && (ridden || !isTame() || x instanceof Enemy || x == getTarget()))) {
            if (!chargeHit.add(e.getId())) continue;
            e.hurt(this.damageSources().mobAttack(this), dmg);
            e.knockback(1.6, -dir.x, -dir.z);
            if (progress.rank("C3") >= 2) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)); // Embestida de caceria: lo deja lento 2 s
            this.playSound(ModSounds.CARNO_CHARGE_HIT.get(), 2.4F, 0.85F + this.random.nextFloat() * 0.2F);
            if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 12, 0.4, 0.4, 0.4, 0.3);
        }
    }

    /** Habilidades del adulto: 0 embestida, 1 cabezazo, 2 mordida, 3 rugido. Solo servidor. */
    public void useAbility(Player user, int ability) {
        if (this.level().isClientSide || !isAdultStage() || isFallen() || isOrderedToSit()) return;
        if (!(this.level() instanceof ServerLevel server)) return;
        Vec3 look = Vec3.directionFromRotation(0.0F, this.getControllingPassenger() instanceof Player r ? r.getYRot() : this.getYRot());
        float atk = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * furyMultiplier();
        lastCombatTick = this.tickCount;
        switch (ability) {
            case 0 -> {
                if (cdCharge > 0 || getAbilityLeft() > 0) return;
                cdCharge = Math.round(160 * cooldownFactor());
                markCd(CD_CHARGE, cdCharge);
                chargeHit.clear();
                if (this.getControllingPassenger() instanceof Player r) this.setYRot(r.getYRot());
                startAbility(1, CHARGE_LEN);
                this.playSound(ModSounds.CARNO_CHARGE_HIT.get(), 2.0F, 0.6F);
            }
            case 1 -> {
                if (cdButt > 0 || getAbilityLeft() > 0) return;
                cdButt = Math.round(70 * (1.0F - 0.10F * progress.rank("C5")) * cooldownFactor());
                markCd(CD_BUTT, cdButt);
                startAbility(2, BUTT_LEN);
                this.playSound(ModSounds.CARNO_ROAR.get(), 1.0F, 1.5F * vp());
                Vec3 c = this.position().add(look.scale(2.6)).add(0, 1.2, 0);
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(2.4, 1.6, 2.4), x -> !isAlly(x))) {
                    e.hurt(this.damageSources().mobAttack(this), atk * (1.3F + 0.5F * progress.rank("C5")));
                    e.knockback(1.3, -look.x, -look.z);
                    e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.5, 0.0));
                    e.hurtMarked = true;
                    server.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 14, 0.4, 0.4, 0.4, 0.35);
                }
                this.playSound(ModSounds.CARNO_CHARGE_HIT.get(), 2.2F, 1.15F);
            }
            case 2 -> {
                if (cdBite > 0) return;
                cdBite = Math.max(2, Math.round(12 * cooldownFactor()));
                markCd(CD_BITE, cdBite);
                LivingEntity best = null;
                double reach = 4.2 + 0.5 * progress.rank("C2");
                double bd = reach * reach;
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach - 0.2, 1.5, reach - 0.2), x -> !isAlly(x))) {
                    Vec3 to = e.position().subtract(this.position());
                    if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) < 0.35) continue;
                    double d = this.distanceToSqr(e);
                    if (d < bd) { bd = d; best = e; }
                }
                this.swing(InteractionHand.MAIN_HAND);
                this.playSound(ModSounds.CARNO_BITE.get(), 1.4F, (0.95F + this.random.nextFloat() * 0.1F) * vp());
                if (best != null) {
                    best.hurt(this.damageSources().mobAttack(this), atk);
                    best.knockback(0.5, -look.x, -look.z);
                }
            }
            case 3 -> {
                if (cdRoar > 0 || getAbilityLeft() > 0) return;
                int ul = ultimateLevel(); // desde el nivel 50 el rugido es el "Rugido ancestral" de su color
                cdRoar = Math.round((ul > 0 ? 1200 - 20 * new int[]{0, 8, 14, 20}[progress.rank("U3")] : 500) * cooldownFactor());
                markCd(CD_ROAR, cdRoar);
                startAbility(3, ROAR_LEN);
                roar(server, false);
                if (ul > 0) ancestralRoar(server, ul, atk);
            }
            default -> {}
        }
    }

    /**
     * Rugido: aliados cerca reciben Fuerza; los demas, Lentitud (y Debilidad si el rugido es completo). El rugido "suave" lo usa el silvestre
     * al ver una presa: asusta (lentitud breve) pero no deja a nadie indefenso.
     */
    private void roar(ServerLevel server, boolean mild) {
        this.playSound(ModSounds.CARNO_ROAR.get(), 4.0F, vp());
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0), x -> x != this)) {
            if (isAlly(e)) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
            else if (e.isAlive() && !(e instanceof CarnotaurusEntity && !this.isTame())) {
                if (!mild) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, mild ? 50 : 100, 0));
            }
        }
        for (int k = 0; k < 40; k++) {
            double a = k * Math.PI * 2.0 / 40.0;
            server.sendParticles(ParticleTypes.SMOKE, getX() + Math.cos(a) * 3.0, getY() + 0.3, getZ() + Math.sin(a) * 3.0, 3, 0.2, 0.1, 0.2, 0.05);
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX() + Math.cos(a) * 1.5, getY() + 2.8, getZ() + Math.sin(a) * 1.5, 0, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ progresion (nivel, XP, talentos)

    @Override public ProdigyProgress getProgress() { return progress; }
    private float cooldownFactor() { return 1.0F - 0.10F * progress.rank("C7"); }
    private float supportBoost() { return 1.0F + 0.20F * progress.rank("S7"); }

    /** C6 Furia protectora: mas dano mientras el dueno esta por debajo del 50 % de vida o fue herido hace poco. */
    private float furyMultiplier() {
        float mult = 1.0F;
        int fury = progress.rank("C6");
        LivingEntity owner = this.getOwner();
        if (fury > 0 && owner != null) {
            boolean low = owner.getHealth() < owner.getMaxHealth() * 0.5F;
            boolean recentlyHurt = owner.tickCount - owner.getLastHurtByMobTimestamp() < 100 && owner.getLastHurtByMobTimestamp() != 0;
            if (low || recentlyHurt) mult += 0.10F * fury;
        }
        return mult;
    }

    /** Nivel de la definitiva (rugido ancestral): 1 desde el nivel 50, 2 desde el 75 y 3 en el 100. Solo el adulto. */
    public int ultimateLevel() {
        if (!isAdultStage()) return 0;
        int lv = progress.level();
        return lv >= 100 ? 3 : lv >= 75 ? 2 : lv >= 50 ? 1 : 0;
    }

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
                p.displayClientMessage(Component.literal("Tu carnotauro sube a nivel " + progress.level() + (isAdultStage() ? " (" + progress.pointsAvailable() + " puntos disponibles)" : "")), true);
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0F, 0.8F);
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

    // ------------------------------------------------------------------ armadura (hierro / oro / diamante / netherita)
    public int getArmorTier() { return this.entityData.get(ARMOR_TIER); }

    /** Equipa una armadura (la anterior cae al suelo). Solo servidor y solo el adulto. */
    public void equipArmor(int tier) {
        if (this.level().isClientSide || !isAdultStage()) return;
        int old = getArmorTier();
        this.entityData.set(ARMOR_TIER, Math.max(0, Math.min(4, tier)));
        if (old > 0 && old != tier) this.spawnAtLocation(new net.minecraft.world.item.ItemStack(ProdigyCreatures.CARNO_ARMOR_ITEMS.get(old - 1).get()));
        applyStageStats(true);
    }

    public void removeArmor() {
        if (this.level().isClientSide || getArmorTier() == 0) return;
        this.spawnAtLocation(new net.minecraft.world.item.ItemStack(ProdigyCreatures.CARNO_ARMOR_ITEMS.get(getArmorTier() - 1).get()));
        this.entityData.set(ARMOR_TIER, 0);
        applyStageStats(true);
    }

    /**
     * Vuelve a ser una cria (solo el adulto vivo). Suelta la silla y la armadura, baja al jinete y conserva dueno,
     * nombre, color, nivel y orden. Para volver a crecer hacen falta de nuevo las carnes crudas. Solo servidor.
     */
    public void revertToChibi() {
        if (this.level().isClientSide || !isAlive() || isFallen() || !isAdultStage() || getMorph() > 0) return;
        this.ejectPassengers();
        removeSaddle();
        removeArmor();
        this.getNavigation().stop();
        this.growth = 0;
        this.entityData.set(MORPH_KIND, 2);
        this.entityData.set(MORPH, SHRINK_LEN);
        this.playSound(ModSounds.CARNO_REVERT.get(), 2.5F, 1.0F);
    }

    public void rename(String name) {
        if (this.level().isClientSide) return;
        String n = name == null ? "" : name.trim();
        if (n.length() > 32) n = n.substring(0, 32);
        this.setCustomName(n.isEmpty() ? null : Component.literal(n));
        this.setCustomNameVisible(!n.isEmpty());
    }

    /** Distancia a partir de la cual se teletransporta junto al dueno (S3). */
    public double rejoinDistance() { return new double[]{96.0, 88.0, 80.0, 64.0}[progress.rank("S3")]; }

    /** Reduccion de dano por caida del dueno (S4) que aporta este companero, 0..1. */
    public float fallReduction() {
        int r = progress.rank("S4");
        return r <= 0 ? 0.0F : Math.min(1.0F, 0.30F * r * supportBoost());
    }

    /** D5 Guardia: porcentaje del dano al jinete que absorbe el carnotauro (0 si no aplica). */
    public int guardPercent() {
        return this.getControllingPassenger() == null ? 0 : new int[]{0, 50, 75, 100}[progress.rank("D5")];
    }

    public void takeRedirected(DamageSource source, float amount) {
        if (!this.level().isClientSide && amount > 0.0F) this.hurt(source, amount);
    }

    private void markCd(EntityDataAccessor<Long> acc, int ticks) {
        int t = Math.max(1, Math.min(4095, ticks));
        this.entityData.set(acc, ((this.level().getGameTime() + t) << 12) | t);
    }
    private static EntityDataAccessor<Long> cdAcc(int which) {
        return which == 0 ? CD_CHARGE : which == 1 ? CD_BUTT : which == 3 ? CD_ROAR : CD_BITE;
    }
    /** Fraccion de recarga que falta (1 = recien usada, 0 = lista). which: 0 embestida, 1 cabezazo, 2 mordida, 3 rugido. Sirve en el cliente. */
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

    /** Servidor: regeneracion (S2, S5) y Rugido de aliento (S6). */
    private void serverProgressTick() {
        if (this.tickCount % 20 == 0 && this.isAlive() && this.getHealth() < this.getMaxHealth()) {
            float pct = 0.0F;
            int s2 = progress.rank("S2");
            if (s2 > 0) pct += new float[]{0F, 0.10F, 0.15F, 0.20F}[s2];
            int s5 = progress.rank("S5");
            if (s5 > 0 && this.tickCount - lastCombatTick > 200) pct += new float[]{0F, 0.3F, 0.5F, 0.7F}[s5];
            if (pct > 0.0F) this.heal(this.getMaxHealth() * pct / 100.0F * supportBoost());
        }
        if (this.tickCount % 10 == 0 && progress.rank("S6") > 0 && isTame() && isAdultStage() && !isFallen() && this.tickCount >= aidReadyAt) {
            LivingEntity o = this.getOwner();
            if (o != null && o.isAlive() && this.distanceToSqr(o) < 24.0 * 24.0 && o.getHealth() < o.getMaxHealth() * 0.30F) {
                aidReadyAt = this.tickCount + Math.round(1800 * cooldownFactor());
                o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.round(160 * supportBoost()), 1));
                o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round(160 * supportBoost()), 1));
                this.playSound(ModSounds.CARNO_ROAR.get(), 3.0F, 1.1F * vp());
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + o.getBbHeight(), o.getZ(), 8, 0.5, 0.4, 0.5, 0.02);
            }
        }
    }

    private boolean isUltFoe(LivingEntity e) {
        if (isAlly(e) || !e.isAlive() || e instanceof Player || e instanceof AbstractVillager || e instanceof IronGolem || e instanceof ArmorStand) return false;
        return !(e instanceof TamableAnimal t && t.isTame());
    }

    /**
     * Rugido ancestral (definitiva desde el nivel 50): onda de choque con dano y un efecto segun el color.
     * Rojo: arde. Verde: veneno (y cura a los aliados). Acero: aturde (y protege a los aliados). Arena: ciega (y acelera a los aliados).
     */
    private void ancestralRoar(ServerLevel server, int ul, float atk) {
        float dmg = atk * new float[]{1.0F, 1.5F, 2.0F, 2.5F}[ul] * (1.0F + new float[]{0F, 0.15F, 0.30F, 0.50F}[progress.rank("U1")]);
        double reach = 8.0 + 2.0 * ul + 1.5 * progress.rank("U2");
        this.playSound(ModSounds.CARNO_ULT_ROAR.get(), 5.0F, 1.0F);
        this.playSound(ModSounds.CARNO_CHARGE_HIT.get(), 3.0F, 0.6F);
        int v = getVariant();
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach), x -> x != this)) {
            if (isAlly(e)) {
                switch (v) {
                    case GREEN -> e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                    case STEEL -> e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
                    case ARENA -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
                    default -> e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1));
                }
                continue;
            }
            if (!isUltFoe(e)) continue;
            e.hurt(this.damageSources().mobAttack(this), dmg);
            e.knockback(v == ARENA ? 1.8 : 1.1, this.getX() - e.getX(), this.getZ() - e.getZ());
            switch (v) {
                case GREEN -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
                case STEEL -> { e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)); }
                case ARENA -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                default -> e.setSecondsOnFire(6);
            }
        }
        net.minecraft.core.particles.ParticleOptions fx = switch (v) {
            case GREEN -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case STEEL -> ParticleTypes.ELECTRIC_SPARK;
            case ARENA -> ParticleTypes.CLOUD;
            default -> ParticleTypes.FLAME;
        };
        for (double r = 2.0; r <= reach; r += 2.0)
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI * 2.0 / 16.0;
                server.sendParticles(fx, getX() + Math.cos(a) * r, getY() + 0.4, getZ() + Math.sin(a) * r, 2, 0.15, 0.2, 0.15, 0.04);
            }
        lastCombatTick = this.tickCount;
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.lastCombatTick = this.tickCount;
            if (this.tickCount < guardUntil) amount *= (1.0F - guardPct / 100.0F);
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            // D4 Instinto defensivo: al bajar del 50 % de vida
            int d4 = progress.rank("D4");
            if (d4 > 0 && this.tickCount >= guardReadyAt && this.tickCount >= guardUntil && this.getHealth() < this.getMaxHealth() * 0.5F) {
                guardPct = new int[]{0, 20, 30, 40}[d4];
                guardUntil = this.tickCount + 100;
                guardReadyAt = this.tickCount + Math.round(20 * new int[]{0, 60, 50, 40}[d4] * cooldownFactor());
            }
            // C4 Cuernos afilados: devuelve parte del dano cuerpo a cuerpo
            int c4 = progress.rank("C4");
            if (c4 > 0 && !source.is(DamageTypes.THORNS) && source.getEntity() instanceof LivingEntity attacker
                    && source.getDirectEntity() == source.getEntity() && !isAlly(attacker)) {
                float back = Math.min(15.0F, amount * new float[]{0F, 0.15F, 0.25F, 0.35F}[c4]);
                if (back > 0.0F) attacker.hurt(this.damageSources().thorns(this), back);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ combate

    @Override public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || isAlly(entity)) return false;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * furyMultiplier();
        lastCombatTick = this.tickCount;
        boolean hit = entity.hurt(this.damageSources().mobAttack(this), damage);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            this.playSound(ModSounds.CARNO_BITE.get(), isAdultStage() ? 1.3F : 0.8F, ((isAdultStage() ? 0.95F : 1.5F) + this.random.nextFloat() * 0.1F) * vp());
            living.knockback(0.5, this.getX() - entity.getX(), this.getZ() - entity.getZ());
        }
        return hit;
    }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        if ((isFallen() || getMorph() > 0) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
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
    /** Para curar y hacer crecer a un carnotauro domado: solo carne de vaca, cerdo o cordero (cruda o cocinada). */
    private static boolean isHealMeat(ItemStack s) {
        return s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON)
            || s.is(Items.COOKED_BEEF) || s.is(Items.COOKED_PORKCHOP) || s.is(Items.COOKED_MUTTON);
    }
    private static boolean isRawHealMeat(ItemStack s) {
        return s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON);
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
                    this.playSound(ModSounds.CARNO_HAPPY.get(), 1.4F, 1.0F);
                }
            } else if (!client && this.isOwnedBy(player)) {
                player.displayClientMessage(Component.literal("Esta fuera de combate: reanimalo con carne de cordero dorada"), true);
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
        if (isHealMeat(held)) {
            boolean needsHeal = this.getHealth() < this.getMaxHealth();
            boolean grows = !isAdultStage() && isRawHealMeat(held);
            if (needsHeal || grows) {
                if (!client) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(isRawHealMeat(held) ? 8.0F : 14.0F);
                    this.playSound(ModSounds.CARNO_EAT.get(), 1.0F, isAdultStage() ? 1.0F : 1.45F);
                    if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 4, 0.4, 0.3, 0.4, 0.02);
                    if (grows && ++growth >= FEEDS_TO_GROW) grow();
                    else if (grows) player.displayClientMessage(Component.literal("Crece: " + growth + "/" + FEEDS_TO_GROW + " carnes crudas"), true);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof CarnotaurusArmorItem armor) {
            if (!isAdultStage()) {
                if (!client) player.displayClientMessage(Component.literal("Solo un carnotauro adulto puede llevar armadura"), true);
            } else if (!client && armor.tier != getArmorTier()) {
                equipArmor(armor.tier);
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.7F);
            }
            return InteractionResult.sidedSuccess(client);
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

    /** Empieza la evolucion: silueta blanca que crece (cinematica); el cambio a adulto ocurre al final, en finishGrow(). */
    private void grow() {
        growth = 0;
        if (getMorph() > 0) return;
        this.ejectPassengers();
        this.getNavigation().stop();
        this.entityData.set(MORPH_KIND, 1);
        this.entityData.set(MORPH, GROW_LEN);
        this.playSound(ModSounds.CARNO_EVOLUTION.get(), 3.0F, 1.0F);
    }

    private void finishGrow() {
        growth = 0;
        int guard = 0;
        while (progress.level() < ProdigyProgress.ADULT_START_LEVEL && guard++ < 100) progress.addXp(Math.max(1, progress.xpToNext() - progress.xp())); // el adulto empieza como minimo en el nivel 30
        this.entityData.set(PROGRESS, progress.save());
        setAdultStage(true);
        this.level().broadcastEntityEvent(this, (byte) 64); // destello sobre la textura final
        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.2, getZ(), 40, 0.8, 1.0, 0.8, 0.3);
        if (this.getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Tu carnotauro es ahora un adulto"), true);
    }

    private void finishRevert() {
        this.growth = 0;
        setAdultStage(false);
        this.level().broadcastEntityEvent(this, (byte) 65);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 24, 0.6, 0.5, 0.6, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 14, 0.5, 0.5, 0.5, 0.06);
        }
        if (this.getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Tu carnotauro volvio a ser una cria"), true);
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
        if (e == 0) sl.sendParticles(ParticleTypes.END_ROD, cx, cy + 1.0, cz, 30, 0.5, 0.5, 0.5, 0.15);
        if (e % 2 == 0) {
            for (int k = 0; k < 3; k++) {
                double a = e * 0.35 + k * 2.094, r = up ? 2.6 * (1.0 - p) + 0.7 : 0.7 + 1.9 * p;
                double h = up ? 0.3 + 3.2 * p : 2.4 - 2.0 * p;
                sl.sendParticles(ParticleTypes.END_ROD, cx + Math.cos(a) * r, cy + h, cz + Math.sin(a) * r, 1, 0, 0.03, 0, 0.0);
            }
        }
        if (up && e % 6 == 0) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx + (this.random.nextDouble() - 0.5) * 3.0, cy + this.random.nextDouble() * 3.4, cz + (this.random.nextDouble() - 0.5) * 3.0, 3, 0.1, 0.1, 0.1, 0.05);
        if (up && left == 5) sl.sendParticles(ParticleTypes.FIREWORK, cx, cy + 1.8, cz, 60, 1.4, 1.4, 1.4, 0.12);
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
    /** Altura de la silla 3D del Brutus (cojin a ~2.9 bloques) menos el desfase del jugador sentado: los muslos quedan sobre el asiento. */
    @Override public double getPassengersRidingOffset() { return 2.60; }
    @Override public void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        Vec3 off = new Vec3(0.0, 0.0, -0.2).yRot(-this.yBodyRot * ((float) Math.PI / 180.0F));
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
            double sp = enraged ? 1.15 : 1.0; // ~20 bloques/s: mas que un caballo
            this.setDeltaMovement(d.x * sp, mv.y, d.z * sp);
            super.travel(Vec3.ZERO.add(0.0, travelVector.y, 0.0));
        } else super.travel(travelVector);
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // hembra silvestre adulta derrotada: 50 % de dejar un huevo; su color sale por rareza (con sesgo hacia el suyo)
        if (!this.isTame() && isFemale() && isAdultStage() && recentlyHit && this.random.nextBoolean()) {
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(ProdigyCreatures.CARNO_EGG_ITEMS.get(
                com.prodigytown.creatures.egg.CarnotaurusEggs.rollVariant(this.random, getVariant())).get()));
        }
        if (isSaddled()) this.spawnAtLocation(Items.SADDLE);
        if (getArmorTier() > 0) this.spawnAtLocation(new net.minecraft.world.item.ItemStack(ProdigyCreatures.CARNO_ARMOR_ITEMS.get(getArmorTier() - 1).get()));
    }

    // ------------------------------------------------------------------ sonidos y varios

    @Override protected SoundEvent getHurtSound(DamageSource source) { return isAdultStage() ? ModSounds.CARNO_HURT.get() : ModSounds.CARNO_HURT_BABY.get(); }
    @Override protected SoundEvent getDeathSound() { return isAdultStage() ? ModSounds.CARNO_DEATH.get() : ModSounds.CARNO_HURT_BABY.get(); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.CARNO_STEP.get(), isAdultStage() ? 0.8F : 0.35F, (0.9F + this.random.nextFloat() * 0.2F) * vp());
    }
    @Override protected float getSoundVolume() { return isAdultStage() ? 1.4F : 1.0F; }
    @Nullable @Override protected SoundEvent getAmbientSound() {
        if (isFallen() || getMorph() > 0 || getOrder() == PetOrders.SLEEP) return null;
        return isAdultStage() ? ModSounds.CARNO_AMBIENT.get() : ModSounds.CARNO_AMBIENT_BABY.get();
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean removeWhenFarAway(double distance) { return !isTame() && super.removeWhenFarAway(distance); }

    public static boolean roomFor(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
