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
import com.prodigytown.creatures.progress.GrowthTable;
import com.prodigytown.creatures.progress.ProdigyPet;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compy (Compsognathus): el cuarto dinosaurio. Diminuto (~1,1 bloques de largo, tamano de pollo), vive en manadas de 3 a 8 y no se puede montar.
 * UNA sola malla y ocho variantes de textura (verde, rojo, azul, gris, amarillo, rosa, albino, melanico).
 * MANADAS: cada grupo tiene UN lider (mas grande, con dos marcas rojas en la cola cerca de la punta). Solo el lider se puede domar; al hacerlo, el resto de la
 * manada se une como sus SOLDADOS: no tienen interfaz propia, su orden queda bloqueada en "seguidor" (siguen al lider y copian Quieto/Sentarse/Dormir),
 * atacan lo que ataca el lider y, si el lider cae, el soldado mas cercano asciende a lider. El lider tiene nivel, XP, 21 talentos (Acecho, Bandada, Astucia, Rey de la bandada), 4 habilidades y su pantalla (K).
 */
public class CompyEntity extends TamableAnimal implements PetOrders.Holder, ProdigyPet {
    public static final String[] VARIANT_IDS = {"verde", "rojo", "azul", "gris", "amarillo", "rosa", "albino", "melanico"};
    public static final String[] VARIANT_NAMES = {"Verde", "Rojo", "Azul", "Gris", "Amarillo", "Rosa", "Albino", "Melánico"};
    public static final int VERDE = 0, ROJO = 1, AZUL = 2, GRIS = 3, AMARILLO = 4, ROSA = 5, ALBINO = 6, MELANICO = 7, VARIANTS = 8;
    public static final int BITE_LEN = 10, EAT_LEN = 36;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.INT);
    /** Lider de su manada (solo uno por manada): mas grande, con distintivos, domable y con interfaz propia. */
    private static final EntityDataAccessor<Boolean> LEADER = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.BOOLEAN);
    /** Nivel, XP y talentos del lider (sincronizado para la pantalla del cliente). */
    private static final EntityDataAccessor<CompoundTag> PROGRESS = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** Estado del grupo de caza que ve la pantalla del lider (ajustes y miembros); lo rellena el servidor con refreshGroup(). */
    private static final EntityDataAccessor<CompoundTag> GROUP = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.COMPOUND_TAG);
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
    private ProdigyProgress progress = ProdigyProgress.atLevel(1, Talents.COMPY);
    /** Recargas (ticks restantes), solo servidor: 0 mordida fugaz, 1 salto evasivo, 2 chillido, 3 ataque en bandada. */
    private final int[] cd = new int[4];
    private int lungeId = -1, lungeEnd, evadeUntil, cdFlee, swarmUntil, bonusB1, bonusP1;
    private float lungeMul = 1.0F, swarmMul = 1.0F;
    private boolean nextBiteBonus;
    private final Map<Integer, float[]> bleeding = new HashMap<>();
    private int trust, lastFeedTick = -100000;
    private boolean variantFixed = false, syncingOrder = false;
    /** Manada a la que pertenece (los miembros comparten id). Solo servidor. */
    private java.util.UUID packId = java.util.UUID.randomUUID();
    @Nullable private CompyEntity leaderRef;
    private int leaderCheckAt = 0;
    // ---- grupo de caza (los ajustes los manda el lider; el rol es del soldado)
    public static final String[] G_MODES = {"Coordinado", "Agresivo", "Defensivo", "Pasivo"};
    public static final String[] G_FORMS = {"Abanico", "Línea", "Círculo", "Libre"};
    public static final int[] G_RADII = {8, 12, 16, 24, 32};
    public static final String[] G_PRIOS = {"Presas pequeñas", "Hostiles", "Más cercano", "Más débil"};
    public static final String[] ROLE_NAMES = {"Explorador", "Rastreador", "Apoyo"};
    public static final String[] GREEK = {"Alfa", "Beta", "Gamma", "Delta", "Épsilon"};
    public static final int G_MIN = 2, G_MAX = 5;
    /** Casillas: 0 seguir al lider, 1 mismo objetivo, 2 cazar en grupo, 3 volver al silbido, 4 proteger al lider, 5 reagruparse si hay peligro. */
    private int gFlags = 0b111111, gMode = 0, gForm = 0, gRad = 3, gMax = G_MAX, gPrio = 0;
    private int role = -1, recallUntil = 0, memCacheAt = -100;
    private List<CompyEntity> memCache = java.util.Collections.emptyList();

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
        this.entityData.define(PROGRESS, ProdigyProgress.atLevel(1, Talents.COMPY).save());
        this.entityData.define(GROUP, new CompoundTag());
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Trust", trust);
        tag.putInt("PetOrder", getOrder());
        tag.putBoolean("Leader", isLeader());
        tag.putUUID("PackId", packId);
        tag.putIntArray("Group", new int[]{gFlags, gMode, gForm, gRad, gMax, gPrio, role});
        tag.put("Progress", progress.save());
        if (orderHome != null) tag.putLong("OrderHome", orderHome.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) { setVariant(tag.getInt("Variant")); variantFixed = true; }   // /summon ... {Variant:3} o huevo con variante concreta
        this.trust = tag.getInt("Trust");
        this.entityData.set(ORDER, PetOrders.clamp(tag.getInt("PetOrder")));
        if (tag.contains("Leader")) this.entityData.set(LEADER, tag.getBoolean("Leader"));
        if (tag.hasUUID("PackId")) this.packId = tag.getUUID("PackId");
        int[] gs = tag.getIntArray("Group");
        if (gs.length >= 7) {
            gFlags = gs[0] & 0b111111; gMode = Mth.clamp(gs[1], 0, 3); gForm = Mth.clamp(gs[2], 0, 3); gRad = Mth.clamp(gs[3], 0, G_RADII.length - 1);
            gMax = Mth.clamp(gs[4], G_MIN, G_MAX); gPrio = Mth.clamp(gs[5], 0, 3); role = Mth.clamp(gs[6], -1, 2);
        }
        this.progress = ProdigyProgress.load(tag.getCompound("Progress"), 1, Talents.COMPY);
        this.entityData.set(PROGRESS, progress.save());
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
        if (PROGRESS.equals(key)) this.progress = ProdigyProgress.load(this.entityData.get(PROGRESS), 1, Talents.COMPY);
    }
    @Override public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) { return isLeader() ? LEADER_DIM : super.getDimensions(pose); }

    /** Rango de un talento del lider (los soldados no tienen talentos propios: sus mejoras vienen del lider, ver bonusB1 / bonusP1). */
    private int rk(String id) { return isLeader() ? progress.rank(id) : 0; }

    /**
     * Vida, ataque, armadura y velocidad segun el papel y el nivel. Lider salvaje 14 PV; lider domado 20 + nivel; soldado 10 (12 domado) mas las
     * mejoras Soldados fuertes / Rey de la bandada de su lider. Conserva la fraccion de vida cuando cambia el maximo.
     */
    private void applyRoleStats(boolean keepFraction) {
        if (this.level().isClientSide) return;
        boolean tame = isTame(), lead = isLeader();
        int lv = progress.level();
        double bite = CompyConfig.get(CompyConfig.BITE_DAMAGE, 2.0);
        double hp, atk, armor, speed = 0.30;
        if (lead) {
            hp = tame ? 20.0 + (GrowthTable.HEALTH[lv] - 24) * 0.10 : 14.0;
            atk = tame ? bite * 1.5 + GrowthTable.DAMAGE[lv] * 0.18 : bite;
            armor = tame ? GrowthTable.ARMOR[lv] * 0.30 : 0.0;
            atk *= 1.0 + 0.10 * progress.rank("P1") + 0.12 * progress.rank("A7");
            speed *= 1.0 + 0.04 * progress.rank("C2") + 0.05 * progress.rank("P1");
        } else {
            hp = (tame ? 12.0 : 10.0) * (1.0 + 0.10 * bonusB1);
            atk = (tame ? bite * 1.5 : bite) * (1.0 + 0.10 * bonusB1 + 0.15 * bonusP1);
            armor = 0.0;
            if (tame && role == 0) speed *= 1.15;     // Explorador: mas veloz
            if (tame && role == 2) atk *= 0.85;       // Apoyo: pega menos, cura
        }
        var attr = this.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != hp) {
            float frac = keepFraction ? this.getHealth() / Math.max(1.0F, this.getMaxHealth()) : 1.0F;
            attr.setBaseValue(hp);
            this.setHealth(Math.max(1.0F, (float) hp * Math.min(1.0F, frac)));
        }
        setBase(Attributes.ATTACK_DAMAGE, atk);
        setBase(Attributes.ARMOR, armor);
        setBase(Attributes.MOVEMENT_SPEED, speed);
    }
    private void setBase(net.minecraft.world.entity.ai.attributes.Attribute a, double v) {
        var inst = this.getAttribute(a);
        if (inst != null && inst.getBaseValue() != v) inst.setBaseValue(v);
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

    /** Al domar al lider, la manada cercana se une a su dueno como soldados (hasta el tope del grupo); los que no caben forman otra manada salvaje con su propio lider. */
    private void recruitPack(Player owner) {
        List<CompyEntity> wild = new java.util.ArrayList<>(this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(64.0), e -> e != this && !e.isTame() && e.isAlive() && e.packId.equals(packId)));
        wild.sort(java.util.Comparator.comparingDouble((CompyEntity e) -> this.distanceToSqr(e)));
        int slots = gMax - 1;
        for (int i = 0; i < Math.min(slots, wild.size()); i++) {
            CompyEntity m = wild.get(i);
            m.tame(owner);
            m.setPersistenceRequired();
            m.setTarget(null);
            m.leaderRef = this;
            m.role = i % 3;
            m.applyRoleStats(false);
            m.setPackOrder(PetOrders.FOLLOW);
            m.level().broadcastEntityEvent(m, (byte) 7);
        }
        if (wild.size() > slots) {                       // el resto: otra manada con un lider nuevo
            java.util.UUID np = java.util.UUID.randomUUID();
            for (int i = slots; i < wild.size(); i++) { wild.get(i).packId = np; wild.get(i).leaderRef = null; }
            wild.get(slots).setLeader(true);
        }
    }

    /**
     * Regalo de la Cronista: este lider (si aun es salvaje, lo doma) y {@code extras} compys mas pasan a ser del jugador, los extras como soldados de su manada. Solo servidor.
     */
    public void giftTo(Player owner, int extras) {
        if (this.level().isClientSide || !isLeader() || !(this.level() instanceof ServerLevel sl)) return;
        if (!isTame()) {
            this.tame(owner);
            if (owner instanceof net.minecraft.server.level.ServerPlayer qsp) com.prodigytown.creatures.quests.QuestData.onTame(qsp, this);
            this.setPersistenceRequired();
            applyRoleStats(false);
            PetOrders.setOrder(this, PetOrders.FOLLOW);
        } else if (!isOwnedBy(owner)) return;
        for (int i = 0; i < extras; i++) {
            CompyEntity c = ProdigyCreatures.COMPY.get().create(sl);
            if (c == null) break;
            c.moveTo(getX() + (this.random.nextDouble() - 0.5) * 2.0, getY(), getZ() + (this.random.nextDouble() - 0.5) * 2.0, this.random.nextFloat() * 360.0F, 0.0F);
            c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(c.blockPosition()), MobSpawnType.EVENT, null, null);
            c.tame(owner);
            c.setPersistenceRequired();
            c.variantFixed = true;
            c.setVariant(getVariant());
            sl.addFreshEntity(c);
            adopt(c);
        }
        refreshGroup();
        this.playSound(ModSounds.COMPY_HAPPY.get(), 1.0F, 1.0F);
        this.level().broadcastEntityEvent(this, (byte) 7);
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
        best.progress = this.progress.copy();   // hereda nivel y XP (los talentos se reparten de nuevo)
        best.progress.reset();
        best.pushProgress();
        best.bonusB1 = 0; best.bonusP1 = 0;
        best.gFlags = gFlags; best.gMode = gMode; best.gForm = gForm; best.gRad = gRad; best.gMax = gMax; best.gPrio = gPrio; best.role = -1;
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
        // AgeableMob castea spawnData a AgeableMobGroupData: nunca se le pasa nuestro PackData (crasheaba al generar el segundo miembro de una manada)
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData instanceof PackData ? null : spawnData, dataTag);
        PackData pd;
        if (spawnData instanceof PackData existing) { pd = existing; data = existing; this.packId = pd.id; this.entityData.set(LEADER, false); }   // el resto de la manada: soldados
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
            private boolean mine() { if (!isFollower()) return true; CompyEntity l = findLeader(); return l == null || !l.gOpt(0) && tickCount >= recallUntil; }
            @Override public boolean canUse() { return getOrder() == PetOrders.FOLLOW && mine() && super.canUse(); }
            @Override public boolean canContinueToUse() { return getOrder() == PetOrders.FOLLOW && mine() && super.canContinueToUse(); }
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
        this.targetSelector.addGoal(4, new GroupCombatGoal());
        this.targetSelector.addGoal(4, PetOrders.patrolTargeting(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Animal.class, 30, true, false, e -> e instanceof Chicken || e instanceof Rabbit) {
            @Override public boolean canUse() { return !isTame() && CompyConfig.get(CompyConfig.HUNT_SMALL_PREY, true) && super.canUse(); }
        });
    }

    /** Puesto de este soldado alrededor de su lider segun la formacion del grupo (Libre: junto al lider). */
    private net.minecraft.world.phys.Vec3 slotPos(CompyEntity l) {
        net.minecraft.world.phys.Vec3 base = l.position();
        if (l.gForm == 3) return base;
        List<CompyEntity> mem = l.membersCached();
        int n = Math.max(1, mem.size()), i = Math.max(0, mem.indexOf(this));
        double yaw = Math.toRadians(l.yBodyRot);
        double bx = Math.sin(yaw), bz = -Math.cos(yaw);             // hacia atras del lider
        double ox, oz;
        if (l.gForm == 0) {                                          // abanico detras
            double t = n == 1 ? 0.0 : (double) i / (n - 1) - 0.5, ang = t * Math.toRadians(130.0), c = Math.cos(ang), sn = Math.sin(ang);
            ox = (bx * c - bz * sn) * 2.4; oz = (bx * sn + bz * c) * 2.4;
        } else if (l.gForm == 1) { ox = bx * 1.5 * (i + 1); oz = bz * 1.5 * (i + 1); }   // fila india
        else { double ang = Math.PI * 2.0 * i / n; ox = Math.cos(ang) * 2.2; oz = Math.sin(ang) * 2.2; }   // circulo
        return new net.minecraft.world.phys.Vec3(base.x + ox, base.y, base.z + oz);
    }

    /** La manada se mantiene unida: los soldados (salvajes o domados) siguen a su lider cuando se alejan (o ocupan su puesto en la formacion). */
    private final class FollowLeaderGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private int recalc;
        FollowLeaderGoal() { this.setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        private boolean tight(CompyEntity l) { return isTame() && (tickCount < recallUntil || l.gOpt(5) && l.getHealth() < l.getMaxHealth() * 0.5F); }
        private net.minecraft.world.phys.Vec3 dest(CompyEntity l) { return isTame() && !tight(l) ? slotPos(l) : l.position(); }
        private boolean away(boolean start) {
            if (isLeader() || isOrderedToSit() || isPassenger()) return false;
            if (isTame() && getOrder() != PetOrders.FOLLOW) return false;
            CompyEntity l = findLeader();
            if (l == null) return false;
            if (isTame() && tickCount >= recallUntil && !l.gOpt(0)) return false;   // "Seguir al lider" desactivado: sigue al dueno
            double min = !isTame() ? (start ? 10.0 : 5.0) : tight(l) ? (start ? 2.5 : 1.8) : l.gForm != 3 ? (start ? 1.8 : 0.9) : (start ? 4.5 : 2.8);
            net.minecraft.world.phys.Vec3 d = dest(l);
            return distanceToSqr(d.x, d.y, d.z) > min * min;
        }
        @Override public boolean canUse() { return away(true); }
        @Override public boolean canContinueToUse() { return away(false) && getTarget() == null; }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { getNavigation().stop(); }
        @Override public void tick() {
            CompyEntity l = findLeader();
            if (l == null) return;
            getLookControl().setLookAt(l, 10.0F, getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 8;
            if (isTame() && distanceToSqr(l) > 24.0 * 24.0) { CompyEntity.this.teleportTo(l.getX(), l.getY(), l.getZ()); return; }
            net.minecraft.world.phys.Vec3 d = dest(l);
            getNavigation().moveTo(d.x, d.y, d.z, isTame() ? 1.25D : 1.0D);
        }
    }

    /** Un soldado ajeno al grupo? Presa valida para la caza en grupo: viva, sin nombre ni correa, ni jugador ni mascota ni compy. */
    private boolean fair(LivingEntity e) {
        return e != null && e.isAlive() && e != this && !(e instanceof CompyEntity) && !(e instanceof Player) && !(e instanceof TamableAnimal ta && ta.isTame())
            && !e.isInvulnerable() && !(e instanceof net.minecraft.world.entity.monster.Creeper) && !(e instanceof Mob m && (m.isLeashed() || m.hasCustomName()));
    }

    /** Objetivo de caza segun la prioridad del grupo (presas pequenas / hostiles / mas cercano / mas debil) dentro de r bloques del lider. */
    @Nullable private LivingEntity huntPick(CompyEntity l, double r) {
        boolean small = CompyConfig.get(CompyConfig.HUNT_SMALL_PREY, true);
        List<LivingEntity> prey = new java.util.ArrayList<>(), foes = new java.util.ArrayList<>();
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, l.getBoundingBox().inflate(r), this::fair)) {
            if (e.getMaxHealth() > 60.0F) continue;
            if (small && (e instanceof Chicken || e instanceof Rabbit)) prey.add(e);
            else if (e instanceof Enemy) foes.add(e);
        }
        List<LivingEntity> pool;
        if (l.gPrio == 0) pool = prey.isEmpty() ? foes : prey;
        else if (l.gPrio == 1) pool = foes.isEmpty() ? prey : foes;
        else { pool = new java.util.ArrayList<>(prey); pool.addAll(foes); }
        if (pool.isEmpty()) return null;
        java.util.Comparator<LivingEntity> cmp = l.gPrio == 3 ? java.util.Comparator.comparingDouble((LivingEntity e) -> e.getHealth()) : java.util.Comparator.comparingDouble((LivingEntity e) -> this.distanceToSqr(e));
        return java.util.Collections.min(pool, cmp);
    }

    /** Objetivos del soldado segun los ajustes del grupo: proteger al lider y cazar en grupo (el "mismo objetivo" lo hace packTick). */
    private final class GroupCombatGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private LivingEntity pick;
        private int next;
        GroupCombatGoal() { this.setFlags(java.util.EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            if (!isTame() || isLeader() || isOrderedToSit() || getOrder() == PetOrders.SLEEP || tickCount < recallUntil || tickCount < next) return false;
            next = tickCount + 10 + random.nextInt(6);
            CompyEntity l = findLeader();
            if (l == null || l.gMode == 3 || l.gOpt(5) && l.getHealth() < l.getMaxHealth() * 0.5F) return false;
            double rad = G_RADII[l.gRad];
            LivingEntity t = null;
            if (l.gOpt(4)) {
                LivingEntity h = l.getLastHurtByMob();
                if (h != null && tickCount - l.getLastHurtByMobTimestamp() < 100 && fair(h) && h.distanceToSqr(l) < rad * rad) t = h;
            }
            if (t == null && l.gMode != 2 && l.gOpt(2)) t = huntPick(l, rad * (l.gMode == 0 ? 0.6 : 1.0));
            if (t == null) return false;
            pick = t;
            return true;
        }
        @Override public boolean canContinueToUse() {
            CompyEntity l = findLeader();
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || l == null || tickCount < recallUntil || l.gMode == 3) return false;
            double rad = G_RADII[l.gRad] + 8.0;
            if (t.distanceToSqr(l) > rad * rad) return false;
            return !(l.gOpt(5) && l.getHealth() < l.getMaxHealth() * 0.5F && distanceToSqr(l) > 36.0);
        }
        @Override public void start() { setTarget(pick); }
        @Override public void stop() { if (getTarget() == pick) setTarget(null); pick = null; }
    }

    /** Pasivo: los soldados de un grupo en modo Pasivo no aceptan objetivos. */
    @Override public void setTarget(@Nullable LivingEntity t) {
        if (t != null && isTame() && !isLeader() && !this.level().isClientSide) { CompyEntity l = findLeader(); if (l != null && l.gMode == 3) return; }
        super.setTarget(t);
    }

    @Override public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity le)) return false;
        this.level().broadcastEntityEvent(this, (byte) 67);
        this.playSound(ModSounds.COMPY_BITE.get(), 0.9F, 0.95F + this.random.nextFloat() * 0.2F);
        return strike(le, 1.0F);
    }

    /**
     * Una mordida con todos los efectos: dano base (atributo) x multiplicador, mejoras de talento del lider (Garras afiladas, Ojo de cazador,
     * Pasos silenciosos, Punto debil, Maestro del sigilo), la pasiva Cazador diminuto (+25 % contra criaturas pequenas) y sangrado.
     */
    private boolean strike(LivingEntity target, float mult) {
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * mult;
        dmg *= 1.0F + 0.08F * rk("A1");
        if (target.getHealth() < target.getMaxHealth() * 0.5F) dmg *= 1.0F + 0.06F * rk("A2");
        if (target instanceof Mob m && m.getTarget() == null) dmg *= 1.0F + 0.10F * rk("C1");   // emboscada
        if (target.getBbHeight() < 1.0F) dmg *= 1.25F;                                          // Cazador diminuto (pasiva)
        if (nextBiteBonus) { dmg *= 1.5F; nextBiteBonus = false; }
        if (swarmUntil > this.tickCount) dmg *= swarmMul;
        if (!isLeader() && role == 1 && (target.hasEffect(MobEffects.GLOWING) || target.getHealth() < target.getMaxHealth() * 0.6F)) dmg *= 1.2F;   // Rastreador: remata presas marcadas o heridas
        int a5 = rk("A5");
        if (a5 > 0 && this.random.nextFloat() < new float[]{0F, 0.08F, 0.14F, 0.20F}[a5]) {
            dmg *= 1.6F;
            if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.2, 0.2, 0.2, 0.2);
        }
        float cfg = (float) CompyConfig.get(CompyConfig.BITE_DAMAGE, 2.0);
        if (dmg < cfg * 0.5F) dmg = cfg * 0.5F;
        boolean ok = target.hurt(this.damageSources().mobAttack(this), dmg);
        if (ok) {
            int a4 = rk("A4");
            if (a4 > 0 && target.isAlive()) bleeding.put(target.getId(), new float[]{(float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * new float[]{0F, 0.10F, 0.16F, 0.22F}[a4], 3});
            if (!target.isAlive()) onKill(target);
        }
        return ok;
    }

    /** Al abatir una presa: el lider se cura y se acelera (Racha de caza); los soldados cercanos se curan (Festin compartido); los soldados dan XP al lider. */
    private void onKill(LivingEntity victim) {
        this.level().broadcastEntityEvent(this, (byte) 66);
        this.heal(2.0F);
        CompyEntity l = isLeader() ? this : findLeader();
        if (!isLeader() && l != null) l.gainXp(Math.max(1, (int) (victim.getMaxHealth() / 4.0F)));
        if (l == null) return;
        if (isLeader()) {
            int a6 = rk("A6");
            if (a6 > 0) {
                this.heal(this.getMaxHealth() * 0.03F * a6);
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (2 + a6) * 20, 0, false, false));
            }
        }
        int b2 = l.rk("B2");
        if (b2 > 0) for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, l.getBoundingBox().inflate(16.0), e -> e != l && e.isTame() && e.isAlive() && e.packId.equals(packId)))
            o.heal(2.0F * b2);
    }

    @Override public void handleEntityEvent(byte id) {
        if (id == 67) { this.biteT = 0; this.biteTO = 0; }
        else if (id == 66) { this.eatT = 0; this.eatTO = 0; }
        else super.handleEntityEvent(id);
    }

    @Override public void tick() {
        super.tick();
        if (this.level().isClientSide) animTick();
        else { PetOrders.serverTick(this); packTick(); abilityTick(); }
    }

    /** Solo servidor: los soldados copian la orden del lider y atacan lo que el ataca; un solo lider por manada. */
    private void packTick() {
        if (this.tickCount % 10 != 3) return;
        if (isFollower()) {
            CompyEntity l = findLeader();
            if (l == null) return;
            int nb1 = l.rk("B1"), np1 = l.rk("P1");
            if (nb1 != bonusB1 || np1 != bonusP1) { bonusB1 = nb1; bonusP1 = np1; applyRoleStats(true); }
            int want = l.getOrder();
            if (want == PetOrders.WANDER || want == PetOrders.PATROL) want = PetOrders.FOLLOW;   // el lider pasea; los soldados lo acompanan
            if (getOrder() != want) setPackOrder(want);
            LivingEntity t = l.getTarget();
            if (getTarget() == null && t != null && t.isAlive() && t != this && !(t instanceof Player) && !(t instanceof TamableAnimal ta && ta.isTame()) && getOrder() != PetOrders.SLEEP
                    && l.gOpt(1) && l.gMode < 2 && this.tickCount >= recallUntil) setTarget(t);
            roleTick(l);
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
                if (!client) player.displayClientMessage(Component.literal("Solo el líder de la manada (marca roja en la cola) puede confiar en ti"), true);
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
        if (!player.isShiftKeyDown() && held.isEmpty() && hand == InteractionHand.MAIN_HAND) return huntGroupInteract(player);   // clic con la mano vacia: grupo de caza
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

    // ------------------------------------------------------------------ grupo de caza (solo del Compy): ajustes, roles y operaciones de la pestana Manada

    /** Seleccion pendiente de cada jugador para el clic con la mano vacia: el lider elegido y cuando (para que caduque). Solo servidor. */
    private static final Map<java.util.UUID, Object[]> GROUP_PICK = new HashMap<>();
    private static final int GROUP_PICK_TICKS = 600, GROUP_RANGE = 24;

    @Nullable private CompyEntity chief() { return isLeader() ? this : findLeader(); }
    /** Casilla de ajustes del grupo (la del lider; sin lider cerca: activada). */
    private boolean gOpt(int bit) { CompyEntity l = chief(); return l == null || ((l.gFlags >> bit) & 1) != 0; }
    public CompoundTag getGroupTag() { return this.entityData.get(GROUP); }

    /** Soldados domados de mi manada cerca (orden estable por UUID). Solo servidor. */
    private List<CompyEntity> members() {
        List<CompyEntity> l = new java.util.ArrayList<>(this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(64.0),
            e -> e != this && e.isTame() && !e.isLeader() && e.isAlive() && e.packId.equals(packId)));
        l.sort(java.util.Comparator.comparing(Entity::getUUID));
        return l;
    }
    private List<CompyEntity> membersCached() {
        if (this.tickCount - memCacheAt >= 10) { memCache = members(); memCacheAt = this.tickCount; }
        return memCache;
    }
    private int groupSize() { return 1 + members().size(); }
    private int capLeft() { return gMax - groupSize(); }

    private static CompoundTag memberTag(CompyEntity c, int slot) {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", c.getUUID());
        t.putString("n", c.hasCustomName() ? c.getCustomName().getString() : "Compy " + GREEK[Math.min(slot, GREEK.length - 1)]);
        t.putInt("v", c.getVariant());
        t.putInt("hp", Math.round(c.getHealth()));
        t.putInt("mh", Math.round(c.getMaxHealth()));
        t.putInt("lv", c.isLeader() ? c.progress.level() : -1);
        t.putInt("r", c.role);
        return t;
    }

    /** Publica el estado del grupo (ajustes y miembros) para la pantalla del lider. Solo servidor. */
    public void refreshGroup() {
        if (this.level().isClientSide || !isLeader()) return;
        CompoundTag t = new CompoundTag();
        t.putIntArray("s", new int[]{gFlags, gMode, gForm, gRad, gMax, gPrio});
        net.minecraft.nbt.ListTag m = new net.minecraft.nbt.ListTag();
        m.add(memberTag(this, 0));
        List<CompyEntity> mem = members();
        for (int i = 0; i < mem.size(); i++) {
            CompyEntity c = mem.get(i);
            if (c.role < 0) { c.role = i % 3; c.applyRoleStats(false); }
            m.add(memberTag(c, i + 1));
        }
        t.put("m", m);
        this.entityData.set(GROUP, t);
    }

    private static void msg(Player p, String text) { p.displayClientMessage(Component.literal(text), true); }

    /** Efectos de rol de un soldado (cada 10 ticks): Explorador marca con brillo a los hostiles cercanos; Apoyo cura al mas herido cerca. */
    private void roleTick(CompyEntity l) {
        if (!isTame() || isOrderedToSit() || getOrder() == PetOrders.SLEEP) return;
        if (role == 0 && this.tickCount % 40 == 3) {
            double r = Math.min(20.0, G_RADII[l.gRad]);
            LivingEntity foe = null; double bd = Double.MAX_VALUE;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), e -> e instanceof Enemy && e.isAlive() && !e.hasEffect(MobEffects.GLOWING))) {
                double d = this.distanceToSqr(e);
                if (d < bd) { bd = d; foe = e; }
            }
            if (foe != null) foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
        } else if (role == 2 && this.tickCount % 60 == 3) {
            LivingEntity best = null; float bf = 1.0F;
            List<LivingEntity> near = new java.util.ArrayList<>(this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(8.0), e -> e.isTame() && e.isAlive() && e.packId.equals(packId)));
            for (LivingEntity e : near) {
                float f = e.getHealth() / Math.max(1.0F, e.getMaxHealth());
                if (f < bf) { bf = f; best = e; }
            }
            if (best != null && bf < 0.999F) {
                best.heal(1.0F);
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HEART, best.getX(), best.getY() + best.getBbHeight() + 0.1, best.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    /** Silbido del lider: la manada deja lo que hace y vuelve (si "Volver al silbido" esta activo). */
    public void whistle(Player p) {
        if (this.level().isClientSide || !isLeader() || !isTame()) return;
        if ((gFlags & 8) == 0) { msg(p, "El silbido está desactivado en Ajustes de caza"); return; }
        int n = 0;
        for (CompyEntity m : members()) { m.recallUntil = m.tickCount + 160; m.setTarget(null); m.getNavigation().stop(); n++; }
        this.setTarget(null);
        this.playSound(ModSounds.COMPY_HAPPY.get(), 1.0F, 1.5F);
        msg(p, n == 0 ? "¡Silbido! (no tienes soldados cerca)" : "¡Silbido! La manada vuelve contigo (" + n + ")");
    }

    /** Pasa un compy al grupo de este lider (sin liderazgo propio y con la orden de seguir). */
    private void adopt(CompyEntity m) {
        m.packId = this.packId;
        m.leaderRef = this;
        if (m.isLeader()) m.setLeader(false);
        m.bonusB1 = 0; m.bonusP1 = 0;
        m.role = -1;
        m.setTarget(null);
        m.setPackOrder(PetOrders.FOLLOW);
        m.applyRoleStats(false);
        m.level().broadcastEntityEvent(m, (byte) 7);
    }

    /** Deja un soldado fuera del grupo: pasa a ser lider de una manada propia (conserva el nivel que tuviera). */
    private void detach(CompyEntity m) {
        m.packId = java.util.UUID.randomUUID();
        m.leaderRef = null;
        m.role = -1;
        m.setLeader(true);
        m.setPackOrder(PetOrders.FOLLOW);
        m.level().broadcastEntityEvent(m, (byte) 7);
    }

    /** Manadas de compys de este dueno cerca que no son la mia (cada una con su lider delante y sus soldados), la mas cercana primero. */
    private List<List<CompyEntity>> nearbyUnits(Player owner, double range) {
        List<CompyEntity> all = new java.util.ArrayList<>(this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(range),
            e -> e != this && e.isTame() && e.isAlive() && e.isOwnedBy(owner) && !e.packId.equals(packId)));
        all.sort(java.util.Comparator.comparingDouble((CompyEntity e) -> this.distanceToSqr(e)));
        List<List<CompyEntity>> out = new java.util.ArrayList<>();
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (CompyEntity c : all) {
            if (!seen.add(c.packId)) continue;
            List<CompyEntity> unit = new java.util.ArrayList<>();
            CompyEntity lead = c.isLeader() ? c : null;
            if (lead == null) for (CompyEntity o : all) if (o.packId.equals(c.packId) && o.isLeader()) { lead = o; break; }
            if (lead != null) { unit.add(lead); unit.addAll(lead.members()); }
            else unit.add(c);                          // soldado suelto de otra manada
            out.add(unit);
        }
        return out;
    }

    private int joinUnit(List<CompyEntity> unit, Player owner) {
        if (unit.size() > capLeft()) return -1;
        for (CompyEntity c : unit) adopt(c);
        if (!unit.isEmpty() && owner instanceof net.minecraft.server.level.ServerPlayer sp) { com.prodigytown.creatures.quests.QuestData.onEvent(sp, "compy_pack"); com.prodigytown.creatures.ProdigyAdvancements.grant(sp, "pack_formed"); if (groupSize() >= 5) com.prodigytown.creatures.ProdigyAdvancements.grant(sp, "full_pack"); }
        return unit.size();
    }

    /** Acciones de la pestana Manada (el lider debe ser del jugador): 0 actualizar, 1 crear (reunir cercanos), 2 invitar cercano, 3 expulsar, 4 nombrar lider, 5 disolver, 6 casilla, 7 selector, 8 cambiar rol. */
    public void groupAction(Player p, int action, int a, int b, java.util.UUID target) {
        if (this.level().isClientSide || !isLeader() || !isTame() || !isOwnedBy(p)) return;
        switch (action) {
            case 1 -> {
                int added = 0, skipped = 0;
                for (List<CompyEntity> u : nearbyUnits(p, GROUP_RANGE)) { int n = joinUnit(u, p); if (n < 0) skipped++; else added += n; }
                msg(p, added == 0 ? (skipped > 0 ? "No caben más compys en el grupo (máx. " + gMax + ")" : "No hay compys tuyos cerca (" + GROUP_RANGE + " bloques)")
                    : "Se unen " + added + " compys al grupo (" + groupSize() + " / " + gMax + ")");
            }
            case 2 -> {
                List<List<CompyEntity>> us = nearbyUnits(p, GROUP_RANGE);
                if (us.isEmpty()) { msg(p, "No hay compys tuyos cerca (" + GROUP_RANGE + " bloques)"); break; }
                List<CompyEntity> u = us.get(0).size() > 1 && !us.get(0).get(0).isLeader() ? us.get(0).subList(0, 1) : us.get(0);
                int n = joinUnit(u, p);
                msg(p, n < 0 ? "No cabe en el grupo (máx. " + gMax + ")" : "Se une " + (n == 1 ? "1 compy" : n + " compys") + " al grupo (" + groupSize() + " / " + gMax + ")");
            }
            case 3 -> {
                for (CompyEntity m : members()) if (m.getUUID().equals(target)) { detach(m); msg(p, "Un compy deja el grupo y forma el suyo"); break; }
            }
            case 4 -> {
                for (CompyEntity m : members()) if (m.getUUID().equals(target)) { promote(m); msg(p, m.getDisplayName().getString() + " es el nuevo líder del grupo"); break; }
            }
            case 5 -> {
                List<CompyEntity> mem = members();
                for (CompyEntity m : mem) detach(m);
                msg(p, mem.isEmpty() ? "No hay grupo que disolver" : "Grupo disuelto: " + mem.size() + " compys van por su cuenta");
            }
            case 6 -> { if (a >= 0 && a < 6) gFlags = b != 0 ? gFlags | (1 << a) : gFlags & ~(1 << a); }
            case 7 -> {
                int d = b >= 0 ? 1 : -1;
                switch (a) {
                    case 0 -> gMode = Math.floorMod(gMode + d, G_MODES.length);
                    case 1 -> gForm = Math.floorMod(gForm + d, G_FORMS.length);
                    case 2 -> gRad = Math.floorMod(gRad + d, G_RADII.length);
                    case 3 -> gMax = Mth.clamp(gMax + d, G_MIN, G_MAX);
                    case 4 -> gPrio = Math.floorMod(gPrio + d, G_PRIOS.length);
                    default -> { }
                }
            }
            case 8 -> { for (CompyEntity m : members()) if (m.getUUID().equals(target)) { m.role = Math.floorMod(m.role + 1, 3); m.applyRoleStats(false); break; } }
            default -> { }
        }
        refreshGroup();
    }

    /** El soldado m pasa a ser el lider: hereda nivel, XP y ajustes; yo paso a soldado. */
    private void promote(CompyEntity m) {
        if (this.getOwner() instanceof Player po) com.prodigytown.creatures.ProdigyAdvancements.grant(po, "new_leader");
        ProdigyProgress mine = this.progress;
        m.progress = mine.copy(); m.pushProgress();
        this.progress = ProdigyProgress.atLevel(1, Talents.COMPY); this.pushProgress();
        m.gFlags = gFlags; m.gMode = gMode; m.gForm = gForm; m.gRad = gRad; m.gMax = gMax; m.gPrio = gPrio;
        m.role = -1; m.bonusB1 = 0; m.bonusP1 = 0;
        int ord = this.getOrder();
        m.setLeader(true);
        m.leaderRef = null;
        this.leaderRef = m;
        this.role = -1;
        this.setLeader(false);
        this.setPackOrder(PetOrders.FOLLOW);
        m.setPackOrder(ord);
        this.level().broadcastEntityEvent(m, (byte) 7);
        m.refreshGroup();
    }

    /**
     * Clic derecho con la mano vacia sobre un Compy tuyo (atajo sin abrir la pantalla). 1) Sobre un lider: lo elige como lider del grupo. 2) Luego, sobre
     * otro lider tuyo: ese lider y su manada pasan a ser sus subditos (si caben). Sobre un soldado de otra manada lo traspasa; sobre un soldado del
     * propio grupo lo separa. Sobre el elegido otra vez cancela. Sin eleccion, un soldado dice a que grupo pertenece.
     */
    private InteractionResult huntGroupInteract(Player player) {
        boolean client = this.level().isClientSide;
        if (client) return InteractionResult.sidedSuccess(true);
        long now = this.level().getGameTime();
        CompyEntity chosen = null;
        Object[] sel = GROUP_PICK.get(player.getUUID());
        if (sel != null) {
            if (now - (Long) sel[1] <= GROUP_PICK_TICKS && this.level() instanceof ServerLevel sl && sl.getEntity((java.util.UUID) sel[0]) instanceof CompyEntity c
                    && c.isAlive() && c.isTame() && c.isLeader() && c.isOwnedBy(player)) chosen = c;
            else GROUP_PICK.remove(player.getUUID());
        }
        if (chosen == null) {
            if (isLeader()) {
                GROUP_PICK.put(player.getUUID(), new Object[]{this.getUUID(), now});
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + getBbHeight() + 0.2, getZ(), 6, 0.2, 0.1, 0.2, 0.02);
                msg(player, this.getDisplayName().getString() + " lidera el grupo de caza (" + groupSize() + " / " + gMax + "). Clic con la mano vacía en otro líder tuyo para unirlo");
            } else {
                CompyEntity l = findLeader();
                msg(player, l == null ? "Soldado sin líder cerca" : "Soldado del grupo de caza de " + l.getDisplayName().getString() + " (" + l.groupSize() + " / " + l.gMax + ")");
            }
            return InteractionResult.SUCCESS;
        }
        if (chosen == this) {
            GROUP_PICK.remove(player.getUUID());
            msg(player, "Grupo de caza de " + chosen.getDisplayName().getString() + ": " + chosen.groupSize() + " / " + chosen.gMax + ". Selección terminada");
            return InteractionResult.SUCCESS;
        }
        if (this.distanceTo(chosen) > GROUP_RANGE + 8) { msg(player, "Están muy lejos: acércalos a menos de " + (GROUP_RANGE + 8) + " bloques"); return InteractionResult.SUCCESS; }
        GROUP_PICK.put(player.getUUID(), new Object[]{chosen.getUUID(), now});
        if (isLeader()) {                                   // otro lider: toda su manada pasa al grupo del elegido
            List<CompyEntity> unit = new java.util.ArrayList<>();
            unit.add(this); unit.addAll(members());
            int n = chosen.joinUnit(unit, player);
            msg(player, n < 0 ? "No caben en el grupo (máx. " + chosen.gMax + ")" : this.getDisplayName().getString() + " y su manada siguen ahora a " + chosen.getDisplayName().getString() + " (" + chosen.groupSize() + " / " + chosen.gMax + ")");
        } else if (this.packId.equals(chosen.packId)) {     // soldado del propio grupo: se separa y forma el suyo
            chosen.detach(this);
            msg(player, "Un soldado se separa y forma su propio grupo de caza");
        } else {                                            // soldado de otra manada: pasa al grupo del elegido
            int n = chosen.joinUnit(java.util.Collections.singletonList(this), player);
            msg(player, n < 0 ? "No cabe en el grupo (máx. " + chosen.gMax + ")" : "Un soldado se une al grupo de caza de " + chosen.getDisplayName().getString() + " (" + chosen.groupSize() + " / " + chosen.gMax + ")");
        }
        chosen.refreshGroup();
        return InteractionResult.SUCCESS;
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
            if (this.isLeashed() && this.getLeashHolder() instanceof com.prodigytown.creatures.quests.CronistaEntity) this.dropLeash(true, false);   // el compy de la Cronista ya es tuyo: sin correa
            applyRoleStats(false);
            PetOrders.setOrder(this, PetOrders.FOLLOW);
            recruitPack(player);
            this.trust = 0;
            this.playSound(ModSounds.COMPY_HAPPY.get(), 1.0F, 1.0F);
            this.level().broadcastEntityEvent(this, (byte) 7);   // corazones
            boolean other = !this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(64.0), e -> e != this && e.isTame() && e.isLeader() && e.isOwnedBy(player)).isEmpty();
            player.displayClientMessage(Component.literal(other ? "¡Nueva manada! Clic con la mano vacía en un líder y luego en el otro para formar un grupo de caza"
                    : "¡El líder confía en ti: su manada te sigue como soldados!"), true);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);   // humo: todavia desconfia
            player.displayClientMessage(Component.literal("Confianza: " + trust + "/" + need), true);
        }
    }

    // ------------------------------------------------------------------ progresion (nivel, XP, talentos) - solo el lider

    @Override public ProdigyProgress getProgress() { return progress; }
    private void pushProgress() { this.entityData.set(PROGRESS, progress.save()); }

    @Override public void gainXp(int amount) {
        if (this.level().isClientSide || amount <= 0 || !isLeader() || !isTame()) return;
        float oldMax = this.getMaxHealth();
        boolean leveled = progress.addXp(amount);
        if (leveled) {
            applyRoleStats(true);
            float diff = this.getMaxHealth() - oldMax;
            if (diff > 0) this.heal(diff);
            if (this.getOwner() instanceof Player p)
                p.displayClientMessage(Component.literal("Tu Compy sube a nivel " + progress.level() + " (" + progress.pointsAvailable() + " puntos disponibles)"), true);
            this.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.8F, 1.3F);
        }
        pushProgress();
    }

    public boolean unlockTalent(int idx) {
        if (this.level().isClientSide || !isLeader() || !isTame()) return false;
        float oldMax = this.getMaxHealth();
        if (!progress.unlock(idx)) return false;
        applyRoleStats(true);
        float diff = this.getMaxHealth() - oldMax;
        if (diff > 0) this.heal(diff);
        pushProgress();
        return true;
    }

    public void resetTalents() {
        if (this.level().isClientSide) return;
        progress.reset();
        applyRoleStats(true);
        pushProgress();
    }

    public void toggleSit() {
        if (this.level().isClientSide) return;
        PetOrders.setOrder(this, this.isOrderedToSit() ? PetOrders.FOLLOW : PetOrders.SIT);
    }

    /** Plumaje ligero: fraccion de dano de caida que se evita (0 a 1). */
    public float fallReduction() { return isLeader() ? new float[]{0F, 0.35F, 0.70F, 1.0F}[progress.rank("C4")] : 0.0F; }

    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return super.causeFallDamage(dist, mult * (1.0F - fallReduction()), src);
    }

    // ------------------------------------------------------------------ habilidades del lider

    public static final int AB_BITE = 0, AB_LEAP = 1, AB_SCREECH = 2, AB_PACK = 3;
    public static final String[] AB_NAMES = {"Mordida fugaz", "Salto evasivo", "Chillido de alarma", "Ataque en bandada"};

    /** Recarga base (ticks) de cada habilidad segun los talentos del lider; la usa tambien la pantalla. */
    public static int cooldownTicks(ProdigyProgress p, int ab) {
        float f = 1.0F - 0.10F * p.rank("P1");
        return Math.max(10, Math.round(switch (ab) {
            case AB_BITE -> 60 * (1.0F - 0.06F * p.rank("A3")) * f;
            case AB_LEAP -> 160 * f;
            case AB_SCREECH -> 400 * f;
            default -> 200 * (1.0F - 0.10F * p.rank("B6")) * f;
        }));
    }
    public static float screechRadius(ProdigyProgress p) { return 8.0F + (p.rank("B4") > 0 ? p.rank("B4") + 1 : 0); }
    public static float leapDistance(ProdigyProgress p) { return 5.0F * (1.0F + 0.15F * p.rank("C3")); }
    public static float packDamageMul(ProdigyProgress p) { return 1.5F * (1.0F + 0.15F * p.rank("B6")); }

    /** Segundos de recarga restantes (solo servidor); la pantalla muestra la recarga base. */
    public int cooldownLeft(int ab) { return cd[ab]; }

    private boolean isAlly(LivingEntity e) {
        if (e instanceof Player) return true;
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return true;
        if (e instanceof TamableAnimal t && t.isTame()) return true;
        return e instanceof CompyEntity c && c.packId.equals(packId);
    }

    /** Objetivo de una habilidad ofensiva: el del lider, el ultimo golpeado por el dueno o la amenaza mas cercana delante (monstruos o quien ataque). */
    @Nullable private LivingEntity pickTarget(double range) {
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && distanceToSqr(t) < (range + 8) * (range + 8)) return t;
        if (getOwner() instanceof Player o) {
            LivingEntity h = o.getLastHurtMob();
            if (h != null && h.isAlive() && !isAlly(h) && o.tickCount - o.getLastHurtMobTimestamp() < 200 && distanceToSqr(h) < (range + 8) * (range + 8)) return h;
        }
        LivingEntity best = null; double bd = Double.MAX_VALUE;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), x -> x != this && x.isAlive() && !isAlly(x))) {
            boolean threat = e instanceof Enemy || (e instanceof Mob m && (m.getTarget() == this || m.getTarget() == getOwner()));
            if (!threat || !this.hasLineOfSight(e)) continue;
            double d = distanceToSqr(e);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    private void say(@Nullable Player user, String msg) { if (user != null) user.displayClientMessage(Component.literal(msg), true); }

    /** 0 mordida fugaz, 1 salto evasivo, 2 chillido de alarma, 3 ataque en bandada. Solo servidor; la IA lo llama con user = null. */
    public void useAbility(@Nullable Player user, int ab) {
        if (this.level().isClientSide || !isLeader() || !isTame() || !isAlive() || ab < 0 || ab > 3) return;
        if (getOrder() == PetOrders.SLEEP) { say(user, "Está dormido"); return; }
        if (cd[ab] > 0) { say(user, AB_NAMES[ab] + ": recarga " + ((cd[ab] + 19) / 20) + " s"); return; }
        ServerLevel sl = (ServerLevel) this.level();
        switch (ab) {
            case AB_BITE -> {
                LivingEntity t = pickTarget(7.0);
                if (t == null) { say(user, "Sin objetivo cercano"); return; }
                setTarget(t);
                lungeId = t.getId(); lungeEnd = this.tickCount + 12; lungeMul = 1.4F;
                this.setOrderedToSit(false);
                this.playSound(ModSounds.COMPY_BITE.get(), 1.0F, 1.5F);
            }
            case AB_LEAP -> {
                Vec3 away = null;
                LivingEntity threat = pickTarget(9.0);
                if (threat != null) away = this.position().subtract(threat.position()).multiply(1, 0, 1);
                if (away == null || away.lengthSqr() < 1.0E-4) away = this.getLookAngle().multiply(-1, 0, -1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                away = away.normalize();
                double sp = 0.30 * leapDistance(progress) / 5.0 * 1.9;
                this.setDeltaMovement(away.x * sp, 0.42, away.z * sp);
                this.hasImpulse = true;
                this.getNavigation().stop();
                evadeUntil = this.tickCount + 12;
                this.playSound(ModSounds.COMPY_STEP.get(), 0.8F, 1.6F);
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 8, 0.2, 0.05, 0.2, 0.02);
                if (progress.rank("C7") > 0) { this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false)); nextBiteBonus = true; }
            }
            case AB_SCREECH -> {
                float r = screechRadius(progress);
                int secs = 3 + progress.rank("B4");
                this.playSound(ModSounds.COMPY_HURT.get(), 1.8F, 1.9F);
                this.playSound(ModSounds.COMPY_AMBIENT.get(), 1.8F, 1.7F);
                for (int i = 0; i < 24; i++) {
                    double ang = i * Math.PI / 12.0;
                    sl.sendParticles(ParticleTypes.NOTE, getX() + Math.cos(ang) * 1.2, getY() + 0.6, getZ() + Math.sin(ang) * 1.2, 1, 0, 0, 0, 1.0);
                }
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), x -> x != this && x.isAlive() && !isAlly(x) && x instanceof Enemy)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, secs * 20, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, secs * 20, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (secs + 2) * 20, 0, false, false));
                    if (e instanceof Mob m && m.getTarget() != null) m.setTarget(null);
                }
                for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(24.0), e -> e != this && e.isTame() && e.isAlive() && e.packId.equals(packId))) {
                    o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, false, false));
                    if (o.getOrder() != PetOrders.SLEEP && o.distanceToSqr(this) > 100.0) o.getNavigation().moveTo(this, 1.4D);
                }
            }
            default -> {
                LivingEntity t = pickTarget(16.0);
                if (t == null) { say(user, "Sin objetivo cercano"); return; }
                int n = 0;
                float mul = packDamageMul(progress);
                int dur = 80 + 40 * progress.rank("P1");
                for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(32.0), e -> e != this && e.isTame() && e.isAlive() && e.packId.equals(packId))) {
                    o.setOrderedToSit(false);
                    o.setTarget(t);
                    o.swarmUntil = this.tickCount + dur; o.swarmMul = mul;
                    o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1, false, false));
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, o.getX(), o.getY() + o.getBbHeight() + 0.2, o.getZ(), 1, 0.1, 0.1, 0.1, 0);
                    n++;
                }
                setTarget(t);
                lungeId = t.getId(); lungeEnd = this.tickCount + 14; lungeMul = 1.0F;
                this.swarmUntil = this.tickCount + dur; this.swarmMul = mul;
                this.playSound(ModSounds.COMPY_AMBIENT.get(), 1.4F, 1.3F);
                say(user, "¡Ataque en bandada! (" + n + " soldados)");
            }
        }
        cd[ab] = cooldownTicks(progress, ab);
    }

    /** Solo servidor: recargas, embestida de la mordida fugaz, sangrado y descanso en nidada. */
    private void abilityTick() {
        for (int i = 0; i < 4; i++) if (cd[i] > 0) cd[i]--;
        if (lungeId != -1) {
            Entity t = this.level().getEntity(lungeId);
            if (!(t instanceof LivingEntity le) || !le.isAlive() || this.tickCount > lungeEnd) lungeId = -1;
            else {
                double reach = (getBbWidth() + le.getBbWidth()) / 2.0 + 0.9;
                if (distanceToSqr(le) <= reach * reach) {
                    lungeId = -1;
                    this.level().broadcastEntityEvent(this, (byte) 67);
                    this.playSound(ModSounds.COMPY_BITE.get(), 1.0F, 1.1F);
                    strike(le, lungeMul);
                    if (rk("A7") > 0 && le.isAlive()) { le.invulnerableTime = 0; strike(le, lungeMul * 0.8F); }
                } else {
                    Vec3 dir = le.position().subtract(position()).multiply(1, 0, 1);
                    if (dir.lengthSqr() > 1.0E-4) {
                        dir = dir.normalize();
                        Vec3 mv = getDeltaMovement();
                        this.setDeltaMovement(dir.x * 0.5, mv.y, dir.z * 0.5);
                        this.getLookControl().setLookAt(le, 60.0F, 60.0F);
                    }
                }
            }
        }
        if (this.tickCount % 20 == 7) {
            var it = bleeding.entrySet().iterator();
            while (it.hasNext()) {
                var en = it.next();
                Entity e = this.level().getEntity(en.getKey());
                if (!(e instanceof LivingEntity le) || !le.isAlive() || en.getValue()[1] <= 0) { it.remove(); continue; }
                le.invulnerableTime = 0;
                le.hurt(this.damageSources().mobAttack(this), en.getValue()[0]);
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 2, 0.15, 0.1, 0.15, 0.1);
                en.getValue()[1]--;
                if (!le.isAlive()) { onKill(le); it.remove(); }
            }
            int b5 = rk("B5");
            if (b5 > 0 && isTame() && (getOrder() == PetOrders.SIT || getOrder() == PetOrders.SLEEP)) {
                float pct = new float[]{0F, 0.008F, 0.014F, 0.020F}[b5];
                this.heal(this.getMaxHealth() * pct);
                for (CompyEntity o : this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(12.0), e -> e != this && e.isTame() && e.isAlive() && e.packId.equals(packId)))
                    if (o.getOrder() == PetOrders.SIT || o.getOrder() == PetOrders.SLEEP) o.heal(o.getMaxHealth() * pct);
            }
        }
    }

    private int soldiersNear(double r) {
        return this.level().getEntitiesOfClass(CompyEntity.class, this.getBoundingBox().inflate(r), e -> e != this && !e.isLeader() && e.isTame() && e.isAlive() && e.packId.equals(packId)).size();
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && isAlive() && source.getEntity() != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.tickCount < evadeUntil) {   // salto evasivo: en el aire no te alcanzan
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 3, 0.15, 0.1, 0.15, 0.02);
                return false;
            }
            if (isLeader()) {
                int c5 = rk("C5");
                if (c5 > 0 && this.random.nextFloat() < 0.06F * c5) {   // esquiva instintiva
                    if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 3, 0.15, 0.1, 0.15, 0.02);
                    return false;
                }
                int b3 = rk("B3");
                if (b3 > 0 && soldiersNear(8.0) >= 3) amount *= 1.0F - 0.04F * b3;
            }
        }
        boolean r = super.hurt(source, amount);
        if (r && !this.level().isClientSide && isLeader()) {
            int c6 = rk("C6");
            if (c6 > 0 && this.tickCount >= cdFlee) { cdFlee = this.tickCount + 200; this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (2 + c6) * 20, 0, false, false)); }
        }
        return r;
    }

    // ------------------------------------------------------------------ sonidos y varios

    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.COMPY_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.COMPY_DEATH.get(); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.COMPY_STEP.get(), 0.25F * (1.0F - new float[]{0F, 0.20F, 0.35F, 0.50F}[progress.rank("C1")] * (isLeader() ? 1.0F : 0.0F)), 0.9F + this.random.nextFloat() * 0.3F);
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
