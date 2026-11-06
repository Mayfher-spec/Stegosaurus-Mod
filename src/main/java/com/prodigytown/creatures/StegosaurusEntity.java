package com.prodigytown.creatures;

import com.prodigytown.creatures.progress.GrowthTable;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.AnimationState;
import org.jetbrains.annotations.Nullable;

/** Original Stegosaurus; does not import, modify or redistribute ROR code or artwork. */
public class StegosaurusEntity extends TamableAnimal implements com.prodigytown.creatures.progress.ProdigyPet, PetOrders.Holder {
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState tailWhipAnimationState = new AnimationState();
    public final AnimationState biteAnimationState = new AnimationState();
    public final AnimationState roarAnimationState = new AnimationState();
    public final AnimationState ultAnimationState = new AnimationState();
    @Override public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.idleAnimationState.startIfStopped(this.tickCount);
            updateGait();
            sitAnimO = sitAnim; sleepAnimO = sleepAnim;
            sitAnim += ((getOrder() == PetOrders.SIT ? 1.0F : 0.0F) - sitAnim) * 0.12F;
            sleepAnim += (((getOrder() == PetOrders.SLEEP || isFallen()) ? 1.0F : 0.0F) - sleepAnim) * 0.08F;
            if (flashFade > 0) flashFade--;
            if (this.tailWhipAnimationState.isStarted() && this.tailWhipAnimationState.getAccumulatedTime() > 1800L) this.tailWhipAnimationState.stop();
            if (this.biteAnimationState.isStarted() && this.biteAnimationState.getAccumulatedTime() > 1200L) this.biteAnimationState.stop();
            if (this.roarAnimationState.isStarted() && this.roarAnimationState.getAccumulatedTime() > 3000L) this.roarAnimationState.stop();
            if (this.ultAnimationState.isStarted() && this.ultAnimationState.getAccumulatedTime() > 2600L) this.ultAnimationState.stop();
        } else {
            if (!sexAssigned) assignSex();
            serverProgressTick();
            guardTick();
            evolveTick();
            revertTick();
            ultTick();
            if (this.tickCount % 20 == 7) unstick();
            PetOrders.serverTick(this);
        }
        if (!this.level().isClientSide && this.getTarget() != null && !this.isOrderedToSit() && this.tickCount % 240 == 0 && this.random.nextInt(4) == 0) {
            // Periodic roar while in combat, synced to nearby clients. Not every tick.
            this.level().broadcastEntityEvent(this, (byte) 63);
        }
    }
    @Override public void handleEntityEvent(byte id) {
        if (id == 61) this.tailWhipAnimationState.start(this.tickCount);
        else if (id == 62) this.biteAnimationState.start(this.tickCount);
        else if (id == 64) this.flashFade = 24;
        else if (id == 66) this.ultAnimationState.start(this.tickCount);
        else if (id == 63) {
            this.roarAnimationState.start(this.tickCount);
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.STEGO_ROAR.get(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 2.5F, 0.95F + this.random.nextFloat() * 0.1F, false);
        }
        else super.handleEntityEvent(id);
    }

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    /** Ticks restantes de la embestida (para animar en el cliente). */
    private static final EntityDataAccessor<Integer> CHARGE_LEFT = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    /** Sexo (solo hembras silvestres sueltan huevo). */
    private static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean sexAssigned = false;
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FALLEN = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    @Nullable private BlockPos orderHome;
    private boolean revertWeak = false;
    @Override public int getOrder() { return this.entityData.get(ORDER); }
    @Override public void setOrderRaw(int order) { this.entityData.set(ORDER, order); }
    @Override public boolean isFallen() { return this.entityData.get(FALLEN); }
    @Override public void setFallenRaw(boolean fallen) { this.entityData.set(FALLEN, fallen); }
    @Override @Nullable public BlockPos orderHome() { return orderHome; }
    @Override public void setOrderHome(@Nullable BlockPos pos) { this.orderHome = pos; }
    /** Solo cliente (animacion): 0..1 de lo sentado / dormido que esta, suavizado. */
    public float sitAnim, sitAnimO, sleepAnim, sleepAnimO;
    private static final EntityDataAccessor<Integer> ARMOR_TIER = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    /** Recargas visibles en el HUD: (tick de juego en que terminan << 12) | duracion en ticks. */
    private static final EntityDataAccessor<Long> CD_CHARGE = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_TAIL = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_ULT = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CD_BITE = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> EVOLVE_QUICK = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REVERT = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EVOLVE = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> PROGRESS = SynchedEntityData.defineId(StegosaurusEntity.class, EntityDataSerializers.COMPOUND_TAG);

    // ---- Progresion y talentos (ver progress/*) ----
    private static final double BASE_HEALTH = 270.0, BASE_DAMAGE = 27.0, BASE_ARMOR = 14.0;
    private static final UUID M_HP_ADD = UUID.fromString("a7d1c0de-0001-4d1e-9a10-5e90a5a00001");
    private static final UUID M_HP_MUL = UUID.fromString("a7d1c0de-0002-4d1e-9a10-5e90a5a00002");
    private static final UUID M_DMG_ADD = UUID.fromString("a7d1c0de-0003-4d1e-9a10-5e90a5a00003");
    private static final UUID M_DMG_MUL = UUID.fromString("a7d1c0de-0004-4d1e-9a10-5e90a5a00004");
    private static final UUID M_ARM_ADD = UUID.fromString("a7d1c0de-0005-4d1e-9a10-5e90a5a00005");
    private static final UUID M_KB_ADD = UUID.fromString("a7d1c0de-0006-4d1e-9a10-5e90a5a00006");
    private static final UUID M_ARM_GEAR = UUID.fromString("a7d1c0de-0008-4d1e-9a10-5e90a5a00008");
    private static final UUID M_TGH_GEAR = UUID.fromString("a7d1c0de-0009-4d1e-9a10-5e90a5a00009");
    private static final UUID M_SPD_MUL = UUID.fromString("a7d1c0de-0007-4d1e-9a10-5e90a5a00007");

    private ProdigyProgress progress = new ProdigyProgress();
    private int lastCombatTick = -100000;
    private int guardUntil = -1, guardPct = 0, guardReadyAt = 0;
    private int chargeTicks = 0, tailWindup = 0;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> chargeHit = new HashSet<>();
    private int chargeReadyAt = 0, tailReadyAt = 0;
    private int biteWindup = 0, biteReadyAt = 0;

    public ProdigyProgress getProgress() { return progress; }

    // ---- Marcha (solo cliente): fase ligada a la distancia real recorrida, ver client/AdultGait ----
    public float gaitV = 0.0F, gaitPhase = 0.0F;
    /** 0..1: cuanto se pliegan las placas dorsales centrales mientras alguien va montado (solo visual). */
    public float plateFold = 0.0F;
    /** 0..1 suavizado: pose de embestida (solo visual, cliente). */
    public float chargeAnim = 0.0F, chargeAnimO = 0.0F;
    public boolean isCharging() { return this.entityData.get(CHARGE_LEFT) > 0; }
    private void updateGait() {
        plateFold += ((this.isVehicle() ? 1.0F : 0.0F) - plateFold) * 0.12F;
        chargeAnimO = chargeAnim;
        chargeAnim += ((isCharging() ? 1.0F : 0.0F) - chargeAnim) * 0.35F;
        if (chargeAnim > 0.4F && this.onGround()) { // polvo y chispas bajo las patas
            for (int i = 0; i < 3; i++) {
                double ox = (this.random.nextDouble() - 0.5) * 3.0, oz = (this.random.nextDouble() - 0.5) * 3.0;
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX() + ox, this.getY() + 0.1, this.getZ() + oz,
                    -chargeDir.x * 0.05, 0.02, -chargeDir.z * 0.05);
            }
            if (this.tickCount % 2 == 0) this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CRIT,
                this.getX() + (this.random.nextDouble() - 0.5) * 3.5, this.getY() + 0.3 + this.random.nextDouble(), this.getZ() + (this.random.nextDouble() - 0.5) * 3.5, 0.0, 0.05, 0.0);
        }
        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        float v = (float) Math.sqrt(dx * dx + dz * dz);
        gaitV += (v - gaitV) * 0.35F;
        if (gaitV > com.prodigytown.creatures.client.AdultGait.MIN_V) {
            gaitPhase += com.prodigytown.creatures.client.AdultGait.phaseRate(gaitV);
            float wrap = (float) (Math.PI * 2.0 * 64.0);
            if (gaitPhase > wrap) gaitPhase -= wrap;
        }
    }
    // Preserve previously saved NBT variant numbers: 0 -> cyber, 1 -> golden, 2 -> magma.
    public static final int CYBERNETIC = 0, GOLDEN = 1, MAGMA = 2, GLACIAL = 3, NATURAL = 4;
    public static final int NORMAL = CYBERNETIC, ALBINO = GOLDEN; // deprecated aliases

    public StegosaurusEntity(EntityType<? extends StegosaurusEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.moveControl = new SmoothMoveControl(this, 4.0F);
        this.lookControl = new PetOrders.PetLookControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 270.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 27.0)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, CYBERNETIC);
        this.entityData.define(PROGRESS, new ProdigyProgress().save());
        this.entityData.define(SADDLED, Boolean.FALSE);
        this.entityData.define(EVOLVE, 0);
        this.entityData.define(REVERT, 0);
        this.entityData.define(EVOLVE_QUICK, false);
        this.entityData.define(ARMOR_TIER, 0);
        this.entityData.define(FEMALE, false);
        this.entityData.define(CHARGE_LEFT, 0);
        this.entityData.define(CD_CHARGE, 0L);
        this.entityData.define(CD_TAIL, 0L);
        this.entityData.define(CD_BITE, 0L);
        this.entityData.define(CD_ULT, 0L);
        this.entityData.define(ORDER, 0);
        this.entityData.define(FALLEN, false);
    }

    public boolean isFemale() { return this.entityData.get(FEMALE); }
    /** Fija el sexo (p. ej. al crecer desde un chibi, que conserva el suyo). */
    public void setFemale(boolean f) { this.entityData.set(FEMALE, f); this.sexAssigned = true; }
    private void assignSex() {
        this.entityData.set(FEMALE, this.random.nextBoolean());
        this.sexAssigned = true;
    }

    /** Nido silvestre que vigila este adulto (null si no es guardian). */
    @Nullable private BlockPos guardPos;
    public void setGuard(BlockPos nest) {
        this.guardPos = nest;
        this.restrictTo(nest, 9);
        this.setPersistenceRequired();
    }
    @Nullable public BlockPos guardPos() { return guardPos; }

    /** Los guardianes son muy agresivos: atacan a todo lo que se acerque al nido (jugadores a 16 bloques, cualquier otro ser a 12). */
    private void guardTick() {
        if (guardPos == null || isTame() || this.tickCount % 10 != 0) return;
        double cx = guardPos.getX() + 0.5, cy = guardPos.getY(), cz = guardPos.getZ() + 0.5;
        LivingEntity cur = this.getTarget();
        if (cur != null && (!cur.isAlive() || cur.distanceToSqr(cx, cy, cz) > 30.0 * 30.0)) { this.setTarget(null); cur = null; } // no los persigue sin fin
        if (cur != null) return;
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(cx - 16, cy - 8, cz - 16, cx + 16, cy + 8, cz + 16),
                o -> o != this && o.isAlive() && !(o instanceof StegosaurusEntity) && !(o instanceof com.prodigytown.chibi.ChibiStego)
                    && !(o instanceof net.minecraft.world.entity.decoration.ArmorStand)
                    && !(o instanceof Player pl && (pl.isCreative() || pl.isSpectator())))) {
            double d = e.distanceToSqr(cx, cy, cz);
            if (!(e instanceof Player) && d > 12.0 * 12.0) continue;
            if (d < bd) { bd = d; best = e; }
        }
        if (best != null) {
            this.setTarget(best);
            for (StegosaurusEntity o : this.level().getEntitiesOfClass(StegosaurusEntity.class, this.getBoundingBox().inflate(20.0),
                    o -> o != this && o.isAlive() && !o.isTame() && o.guardPos() != null)) o.setTarget(best);
        }
    }

    @Override public boolean canAttack(LivingEntity target) {
        if (isTame() && target == this.getOwner()) return false;
        return super.canAttack(target);
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int variant) { this.entityData.set(VARIANT, Math.max(0, Math.min(NATURAL, variant))); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkinVariant", getVariant());
        tag.put("ProdigyProgress", progress.save());
        tag.putBoolean("Saddled", isSaddled());
        tag.putInt("ArmorTier", getArmorTier());
        tag.putBoolean("Female", isFemale());
        tag.putBoolean("SexAssigned", sexAssigned);
        if (guardPos != null) tag.putLong("GuardPos", guardPos.asLong());
        tag.putInt("PetOrder", getOrder());
        tag.putBoolean("PetFallen", isFallen());
        if (orderHome != null) tag.putLong("OrderHome", orderHome.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("SkinVariant"));
        // Mundos antiguos no tienen este tag: se carga con nivel 30 y sin talentos.
        progress = ProdigyProgress.load(tag.contains("ProdigyProgress") ? tag.getCompound("ProdigyProgress") : null);
        this.entityData.set(PROGRESS, progress.save());
        this.entityData.set(SADDLED, tag.getBoolean("Saddled"));
        this.entityData.set(ARMOR_TIER, Math.max(0, Math.min(4, tag.getInt("ArmorTier"))));
        this.entityData.set(FEMALE, tag.getBoolean("Female"));
        this.sexAssigned = tag.getBoolean("SexAssigned");
        if (tag.contains("GuardPos")) setGuard(BlockPos.of(tag.getLong("GuardPos")));
        if (tag.contains("PetOrder")) this.entityData.set(ORDER, PetOrders.clamp(tag.getInt("PetOrder")));
        else if (isOrderedToSit()) this.entityData.set(ORDER, PetOrders.SIT); // mundos anteriores: sentado
        this.entityData.set(FALLEN, tag.getBoolean("PetFallen"));
        if (tag.contains("OrderHome")) orderHome = BlockPos.of(tag.getLong("OrderHome"));
        applyStats();
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PROGRESS.equals(key)) this.progress = ProdigyProgress.load(this.entityData.get(PROGRESS));
    }

    @Nullable
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType reason, @Nullable SpawnGroupData previous, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, previous, tag);
        if (reason != net.minecraft.world.entity.MobSpawnType.STRUCTURE) {
            var biome = level.getBiome(this.blockPosition());
            boolean deep = biome.is(net.minecraft.world.level.biome.Biomes.DEEP_DARK);
            int bv = -1;
            boolean natural = biome.is(net.minecraft.world.level.biome.Biomes.PLAINS) || biome.is(net.minecraft.tags.BiomeTags.IS_FOREST);
            if (deep) bv = GLACIAL; // GLACIAL = esmeralda
            else if (biome.is(net.minecraft.tags.BiomeTags.IS_SAVANNA) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS)) bv = MAGMA;
            else if (biome.is(net.minecraft.world.level.biome.Biomes.DESERT)) bv = GOLDEN;
            else if (biome.is(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS) || biome.is(net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA)
                || biome.is(net.minecraft.world.level.biome.Biomes.ICE_SPIKES) || biome.is(net.minecraft.world.level.biome.Biomes.GROVE)
                || biome.is(net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES) || biome.is(net.minecraft.world.level.biome.Biomes.FROZEN_PEAKS)) bv = CYBERNETIC;
            setVariant(com.prodigytown.creatures.egg.StegosaurusEggs.rollWild(this.random, bv, natural));
        }
        if (reason == net.minecraft.world.entity.MobSpawnType.STRUCTURE) setFemale(false); // guardianes de nido: siempre machos
        else if (!this.isTame() && reason != net.minecraft.world.entity.MobSpawnType.SPAWN_EGG && reason != net.minecraft.world.entity.MobSpawnType.COMMAND) setFemale(true); // errantes: siempre hembras
        else assignSex();
        return result;
    }

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.22, true) {
            @Override public boolean canUse() { return !isFallen() && stayOk() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isFallen() && stayOk() && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(3, new RejoinFollowGoal(1.15, 16.0f, 9.0f));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(5, new PetOrders.PatrolGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.85) {
            @Override public boolean canUse() { return !isTame() || (getOrder() != PetOrders.STAY && getOrder() != PetOrders.PATROL && !isFallen() && super.canUse()); }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, PetOrders.patrolTargeting(this));
    }

    /** En "quieto" solo se defiende de lo que tiene al lado (no sale a perseguir). */
    private boolean stayOk() {
        if (!isTame() || getOrder() != PetOrders.STAY) return true;
        LivingEntity t = getTarget();
        return t != null && this.distanceToSqr(t) < 36.0;
    }

    /** Pienso para estegosaurio: cura y da experiencia. */
    private boolean isSalad(ItemStack stack) { return stack.is(ProdigyCreatures.FEED.get()); }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isFallen()) {
            // Fuera de combate: solo la ensalada de domesticacion del dueno lo reanima.
            if (this.isTame() && this.isOwnedBy(player) && held.is(ProdigyCreatures.TAMING_SALAD.get())) {
                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    PetOrders.revive(this);
                }
            } else if (!this.level().isClientSide && this.isOwnedBy(player)) {
                player.displayClientMessage(Component.literal("Esta fuera de combate: reanimalo con la ensalada de domesticacion"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (held.is(ProdigyCreatures.TAMING_SALAD.get()) && !this.isTame()) {
            // Los adultos silvestres no se domestican: solo se consiguen de un huevo (creativo puede, para pruebas).
            if (!player.getAbilities().instabuild) return InteractionResult.PASS;
            if (!this.level().isClientSide) {
                this.tame(player);
                PetOrders.setOrder(this, PetOrders.FOLLOW);
                this.level().broadcastEntityEvent(this, (byte)7);
                this.heal(this.getMaxHealth());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (isSalad(held)) {
            if (!this.level().isClientSide) {
                if (!this.isTame()) {
                    return InteractionResult.PASS;
                } else if (this.isOwnedBy(player)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(40.0f);
                    this.gainXp(25);
                } else return InteractionResult.PASS;
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && held.getItem() instanceof StegosaurusArmorItem armor) {
            if (!this.level().isClientSide) {
                if (armor.tier != getArmorTier()) {
                    equipArmor(armor.tier);
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && held.is(Items.SADDLE) && !isSaddled()) {
            if (!this.level().isClientSide) {
                setSaddled(true);
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown() && held.isEmpty()) {
            if (this.level().isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.prodigytown.creatures.client.ClientHooks.openTalents(this));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && !player.isShiftKeyDown() && isSaddled()) {
            // Con silla: clic = montar. Sentar/levantar se hace desde la pantalla de habilidades.
            if (!this.level().isClientSide && !this.isVehicle()) {
                PetOrders.setOrder(this, PetOrders.FOLLOW);
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isTame() && this.isOwnedBy(player) && !player.isShiftKeyDown()) {
            if (!this.level().isClientSide) PetOrders.setOrder(this, this.isOrderedToSit() ? PetOrders.FOLLOW : PetOrders.SIT);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override public boolean isFood(ItemStack stack) { return isSalad(stack); }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == this.getOwner()) return false;
        if (living instanceof StegosaurusEntity other && other.getOwnerUUID() != null && other.getOwnerUUID().equals(this.getOwnerUUID())) return false;
        this.lastCombatTick = this.tickCount;
        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * furyMultiplier();
        // Defensive tail strike: slower than a rex, but heavy impact.
        boolean hit = entity.hurt(this.damageSources().mobAttack(this), damage);
        if (hit) {
            if (!this.level().isClientSide) {
                boolean bite = this.random.nextInt(5) == 0;
                this.level().broadcastEntityEvent(this, (byte)(bite ? 62 : 61));
                this.playSound((bite ? ModSounds.STEGO_BITE : ModSounds.STEGO_TAIL).get(), 1.3F, 0.9F + this.random.nextFloat() * 0.2F);
            }
            living.knockback(1.9, this.getX()-entity.getX(), this.getZ()-entity.getZ());
            int sweep = progress.rank("C2");
            if (sweep > 0 && !this.level().isClientSide) {
                AABB area = living.getBoundingBox().inflate(1.0 + 0.5 * sweep);
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, area, x -> x != this && x != living && x instanceof Enemy)) {
                    e.hurt(this.damageSources().mobAttack(this), damage * 0.5F);
                }
            }
        }
        return hit;
    }

    // ------------------------------------------------------------------ armadura, recargas del HUD, quitar equipo

    /** Armadura por nivel (0 = sin armadura, 1 hierro, 2 oro, 3 diamante, 4 netherite). */
    public static final int[] ARMOR_VALUE = {0, 6, 7, 12, 16};
    public static final int[] TOUGHNESS_VALUE = {0, 0, 0, 2, 3};

    public int getArmorTier() { return this.entityData.get(ARMOR_TIER); }

    /** Equipa una armadura (devuelve la anterior al suelo). Solo servidor. */
    public void equipArmor(int tier) {
        if (this.level().isClientSide) return;
        int old = getArmorTier();
        this.entityData.set(ARMOR_TIER, Math.max(0, Math.min(4, tier)));
        if (old > 0 && old != tier) this.spawnAtLocation(new ItemStack(ProdigyCreatures.ARMOR_ITEMS.get(old - 1).get()));
        applyStats();
    }

    public void removeArmor() {
        if (this.level().isClientSide || getArmorTier() == 0) return;
        this.spawnAtLocation(new ItemStack(ProdigyCreatures.ARMOR_ITEMS.get(getArmorTier() - 1).get()));
        this.entityData.set(ARMOR_TIER, 0);
        applyStats();
    }

    /**
     * Vuelve a chibi: el adulto se convierte en un chibi de su misma raza (conserva dueno, nombre, nivel y progreso),
     * suelta la silla y la armadura y desmonta al jinete. Solo servidor, solo el dueno (lo comprueba la red).
     */
    public void revertToChibi() { revertToChibi(false); }

    /** weak = se encoge herido (10 % de vida) en vez de por voluntad del dueno. */
    public void revertToChibi(boolean weak) {
        if (!(this.level() instanceof ServerLevel server) || !isAlive() || isFallen() || getEvolveTicks() > 0 || getRevertTicks() > 0) return;
        this.revertWeak = weak;
        com.prodigytown.chibi.ChibiStego probe = com.prodigytown.chibi.ChibiAlpha.STEGO.get().create(server);
        if (probe == null) return;
        probe.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        if (!server.noCollision(probe, probe.getBoundingBox())) return;
        probe.discard();
        this.ejectPassengers();
        this.getNavigation().stop();
        this.entityData.set(REVERT, REVERT_LEN);
        this.playSound(ModSounds.CHIBI_EVOLVE.get(), 2.0F, 1.6F);
    }

    /** Cuenta atras corta: el adulto se encoge como silueta blanca y al final aparece el chibi. */
    private void revertTick() {
        int left = getRevertTicks();
        if (left <= 0 || !(this.level() instanceof ServerLevel server)) return;
        if (left % 4 == 0) server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 6, 1.5, 1.0, 1.5, 0.03);
        this.entityData.set(REVERT, left - 1);
        if (left == 1) finishRevert(server);
    }

    private void finishRevert(ServerLevel server) {
        com.prodigytown.chibi.ChibiStego chibi = com.prodigytown.chibi.ChibiAlpha.STEGO.get().create(server);
        if (chibi == null) { this.entityData.set(REVERT, 0); return; }
        chibi.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        chibi.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
        chibi.initFromAdult(getVariant());
        chibi.setFemale(isFemale());
        chibi.setOwnerUUID(getOwnerUUID());
        chibi.setTame(true);
        PetOrders.copy(this, chibi);
        if (hasCustomName()) chibi.setCustomName(getCustomName());
        chibi.adoptProgress(progress);
        chibi.setGear(getArmorTier(), isSaddled());
        chibi.setHealth(revertWeak ? Math.max(1.0F, chibi.getMaxHealth() * 0.10F) : chibi.getMaxHealth());
        server.addFreshEntity(chibi);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 18, 0.4, 0.3, 0.4, 0.04);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 12, 0.3, 0.3, 0.3, 0.06);
        if (getOwner() instanceof net.minecraft.world.entity.player.Player p) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Tu estegosaurio volvio a ser un chibi"), false);
        discard();
    }

    public void removeSaddle() {
        if (this.level().isClientSide || !isSaddled()) return;
        this.ejectPassengers();
        this.spawnAtLocation(Items.SADDLE);
        setSaddled(false);
    }

    private static EntityDataAccessor<Long> cdAcc(int which) {
        return which == 0 ? CD_CHARGE : which == 1 ? CD_TAIL : which == 3 ? CD_ULT : CD_BITE;
    }

    private void markCooldown(EntityDataAccessor<Long> acc, int ticks) {
        int t = Math.max(1, Math.min(4095, ticks));
        this.entityData.set(acc, ((this.level().getGameTime() + t) << 12) | t);
    }

    /** Fraccion de recarga que falta (1 = recien usada, 0 = lista). which: 0 embestida, 1 coletazo, 2 mordida. Sirve en el cliente. */
    public float cooldownLeft(int which) {
        long v = this.entityData.get(cdAcc(which));
        long dur = v & 4095L;
        if (dur == 0L) return 0.0F;
        long remaining = (v >> 12) - this.level().getGameTime();
        return remaining <= 0 ? 0.0F : Math.min(1.0F, remaining / (float) dur);
    }

    /** Segundos que faltan (redondeado hacia arriba), 0 si esta lista. */
    public int cooldownSeconds(int which) {
        long v = this.entityData.get(cdAcc(which));
        if ((v & 4095L) == 0L) return 0;
        long remaining = (v >> 12) - this.level().getGameTime();
        return remaining <= 0 ? 0 : (int) ((remaining + 19) / 20);
    }

    /** Renombrar desde la pantalla (solo servidor). Vacio = quitar el nombre. */
    public void rename(String name) {
        if (this.level().isClientSide) return;
        String n = name == null ? "" : name.trim();
        if (n.length() > 32) n = n.substring(0, 32);
        this.setCustomName(n.isEmpty() ? null : Component.literal(n));
        this.setCustomNameVisible(!n.isEmpty());
    }

    // ------------------------------------------------------------------ transicion de evolucion (chibi -> adulto)

    /** Duracion de la transicion en ticks (4 s). El adulto aparece como silueta blanca chica, crece y estalla en un destello. */
    public static final int EVOLVE_LEN = 80;
    /** Solo cliente: ticks restantes del destello blanco con el que aparece la textura final. */
    public int flashFade = 0;

    /** Ticks que le faltan a la transicion (0 = no esta evolucionando). Se sincroniza al cliente; no se guarda en el mundo. */
    /** Duracion corta (1,25 s) de la transformacion adulto -> chibi. */
    public static final int REVERT_LEN = 25;
    public int getRevertTicks() { return this.entityData.get(REVERT); }
    public int getEvolveTicks() { return this.entityData.get(EVOLVE); }

    @Override protected boolean isImmobile() { return super.isImmobile() || getEvolveTicks() > 0 || getRevertTicks() > 0 || isFallen(); }

    /** La llama el chibi al convertirse en adulto (solo servidor). */
    public static final int EVOLVE_LEN_QUICK = 30;
    public int evolveLen() { return this.entityData.get(EVOLVE_QUICK) ? EVOLVE_LEN_QUICK : EVOLVE_LEN; }

    public void startEvolution(boolean quick) {
        if (this.level().isClientSide) return;
        this.entityData.set(EVOLVE_QUICK, quick);
        this.entityData.set(EVOLVE, quick ? EVOLVE_LEN_QUICK : EVOLVE_LEN);
        this.playSound(ModSounds.STEGO_EVOLVE.get(), quick ? 1.5F : 3.0F, quick ? 1.5F : 1.0F);
    }

    private void evolveTick() {
        int left = getEvolveTicks();
        if (left <= 0 || !(this.level() instanceof ServerLevel server)) return;
        int len = evolveLen();
        int e = len - left;
        double p = e / (double) len;
        this.getNavigation().stop();
        Vec3 mv = this.getDeltaMovement();
        this.setDeltaMovement(0.0, Math.min(0.0, mv.y), 0.0);
        double cx = this.getX(), cz = this.getZ(), cy = this.getY();
        if (e == 0) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, cx, cy + 1.2, cz, 30, 0.5, 0.5, 0.5, 0.15);
        }
        if (e % 2 == 0) { // remolino de luz que se cierra sobre el estegosaurio y sube
            for (int k = 0; k < 3; k++) {
                double a = e * 0.35 + k * 2.094;
                double r = 2.6 * (1.0 - p) + 0.7;
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    cx + Math.cos(a) * r, cy + 0.3 + 3.8 * p, cz + Math.sin(a) * r, 1, 0, 0.03, 0, 0.0);
            }
        }
        if (e % 6 == 0) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                cx + (this.random.nextDouble() - 0.5) * 3.0, cy + this.random.nextDouble() * 4.0, cz + (this.random.nextDouble() - 0.5) * 3.0, 3, 0.1, 0.1, 0.1, 0.05);
        }
        if (left == 5) { // estallido final
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK, cx, cy + 2.0, cz, 70, 1.6, 1.6, 1.6, 0.12);
        }
        this.entityData.set(EVOLVE, left - 1);
        if (left == 1) {
            this.level().broadcastEntityEvent(this, (byte) 64); // destello blanco sobre la textura final
            this.level().broadcastEntityEvent(this, (byte) 63); // rugido
        }
    }

    // ------------------------------------------------------------------ montura (silla)

    public boolean isSaddled() { return this.entityData.get(SADDLED); }
    public void setSaddled(boolean v) { this.entityData.set(SADDLED, v); }

    @Override protected boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        return isSaddled() && this.getPassengers().isEmpty() && passenger instanceof Player p && this.isOwnedBy(p);
    }

    @Nullable @Override public LivingEntity getControllingPassenger() {
        net.minecraft.world.entity.Entity first = this.getFirstPassenger();
        return first instanceof Player p && this.isOwnedBy(p) ? p : null;
    }

    @Override public double getPassengersRidingOffset() { return this.getBbHeight() * 0.98; }

    /** El jinete se sienta un poco hacia atras, detras del cruce de las placas centrales (0,35 bloques). */
    private static final double SEAT_BACK = 0.35;
    @Override public void positionRider(net.minecraft.world.entity.Entity passenger, net.minecraft.world.entity.Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        Vec3 off = new Vec3(0.0, 0.0, -SEAT_BACK).yRot(-this.yBodyRot * ((float) Math.PI / 180.0F));
        callback.accept(passenger, this.getX() + off.x,
            this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + off.z);
    }

    @Override public void travel(Vec3 travelVector) {
        if (ultWindup > 0) { this.calculateEntityAnimation(false); return; } // concentrando la definitiva: no se mueve
        if (this.isAlive() && this.isVehicle() && this.getControllingPassenger() instanceof Player rider) {
            // giro amplio: sigue la mirada del jinete con un limite por tick (en marcha, mas lento)
            float turnCap = rider.zza > 0.0F ? 3.5F : 7.0F;
            this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(rider.getYRot() - this.getYRot()), -turnCap, turnCap));
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.yBodyRot;
            if (isCharging()) { // carrera: avanza recto con ligero giro hacia donde mira el jinete
                this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(rider.getYRot() - this.getYRot()), -2.5F, 2.5F));
                this.yRotO = this.getYRot(); this.yBodyRot = this.getYRot(); this.yHeadRot = this.getYRot();
                if (this.isControlledByLocalInstance()) {
                    Vec3 d = Vec3.directionFromRotation(0.0F, this.getYRot()), mv = this.getDeltaMovement();
                    this.setDeltaMovement(d.x * 0.75, mv.y, d.z * 0.75);
                    this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                    this.setDeltaMovement(d.x * 0.75, (mv.y - 0.08) * 0.98, d.z * 0.75);
                }
                this.calculateEntityAnimation(false);
                return;
            }
            float strafe = rider.xxa * 0.4F;
            float forward = rider.zza;
            if (forward <= 0.0F) forward *= 0.3F;
            if (this.isControlledByLocalInstance()) {
                this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.9F);
                super.travel(new Vec3(strafe, travelVector.y, forward));
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
            this.calculateEntityAnimation(false);
        } else {
            super.travel(travelVector);
        }
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!this.isTame() && isFemale() && guardPos == null && recentlyHit && this.random.nextBoolean()) { // 50 %: errante hembra // la hembra silvestre derrotada deja un huevo; su raza influye en la del huevo
            this.spawnAtLocation(new ItemStack(ProdigyCreatures.EGG_ITEMS.get(
                com.prodigytown.creatures.egg.StegosaurusEggs.rollVariant(this.random, getVariant())).get()));
        }
        if (isSaddled()) this.spawnAtLocation(Items.SADDLE);
        if (getArmorTier() > 0) this.spawnAtLocation(new ItemStack(ProdigyCreatures.ARMOR_ITEMS.get(getArmorTier() - 1).get()));
    }

    public void toggleSit() {
        if (this.level().isClientSide) return;
        PetOrders.setOrder(this, this.isOrderedToSit() ? PetOrders.FOLLOW : PetOrders.SIT);
    }

    /** D5 Guardia del Estegosaurio: porcentaje del dano al jinete que absorbe el estegosaurio (0 si no aplica). */
    public int guardPercent() {
        if (this.getControllingPassenger() == null) return 0;
        return new int[]{0, 50, 75, 100}[progress.rank("D5")];
    }

    /** Dano redirigido desde el jinete: pasa por la armadura y vida del estegosaurio, una sola vez. */
    public void takeRedirected(DamageSource source, float amount) {
        if (!this.level().isClientSide && amount > 0.0F) this.hurt(source, amount);
    }

    /** La evolucion chibi -> adulto entrega aqui nivel, XP y puntos. */
    public void adoptProgress(ProdigyProgress incoming) {
        if (this.level().isClientSide) return;
        this.progress = incoming.copy();
        applyStats();
        pushProgress();
    }

    // ------------------------------------------------------------------ progresion

    private float cooldownFactor() { return 1.0F - 0.10F * progress.rank("C7"); }
    private float supportBoost() { return 1.0F + 0.20F * progress.rank("S7"); }

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

    private boolean isFoe(LivingEntity e) {
        if (e == this || e == this.getOwner() || e instanceof Player) return false;
        if (e instanceof TamableAnimal t && t.isTame()) return false;
        if (this.isTame()) { // domesticado: golpea a cualquier mob, menos mascotas (fuego amigo no), jugadores ni soportes
            if (e instanceof net.minecraft.world.entity.OwnableEntity oe && oe.getOwnerUUID() != null) return false;
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand || e.isPassengerOfSameVehicle(this) || this.hasPassenger(e)) return false;
            return e.isAlive();
        }
        return e instanceof Enemy || e == this.getTarget();
    }

    /** Recalcula los modificadores de atributos a partir de nivel y talentos (solo servidor). */
    public void applyStats() {
        if (this.level().isClientSide) return;
        int lv = progress.level();
        setMod(Attributes.MAX_HEALTH, M_HP_ADD, GrowthTable.HEALTH[lv] - BASE_HEALTH, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.MAX_HEALTH, M_HP_MUL, 0.04 * progress.rank("D2"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG_ADD, GrowthTable.DAMAGE[lv] - BASE_DAMAGE, AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG_MUL, 0.05 * progress.rank("C1") + 0.10 * progress.rank("C7"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        setMod(Attributes.ARMOR, M_ARM_ADD, GrowthTable.ARMOR[lv] - BASE_ARMOR + progress.rank("D1"), AttributeModifier.Operation.ADDITION);
        setMod(Attributes.KNOCKBACK_RESISTANCE, M_KB_ADD, 0.05 * progress.rank("D3"), AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ARMOR, M_ARM_GEAR, ARMOR_VALUE[getArmorTier()], AttributeModifier.Operation.ADDITION);
        setMod(Attributes.ARMOR_TOUGHNESS, M_TGH_GEAR, TOUGHNESS_VALUE[getArmorTier()], AttributeModifier.Operation.ADDITION);
        setMod(Attributes.MOVEMENT_SPEED, M_SPD_MUL, 0.02 * progress.rank("S1"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
    }

    private void setMod(Attribute attr, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, "prodigy_progress", amount, op));
    }

    private void pushProgress() {
        this.entityData.set(PROGRESS, progress.save());
    }

    @Override public void gainXp(int amount) {
        if (this.level().isClientSide || amount <= 0) return;
        float oldMax = this.getMaxHealth();
        boolean leveled = progress.addXp(amount);
        if (leveled) {
            applyStats();
            float diff = this.getMaxHealth() - oldMax;
            if (diff > 0) this.heal(diff);
            if (this.getOwner() instanceof Player p) {
                p.displayClientMessage(Component.literal("Tu estegosaurio sube a nivel " + progress.level()
                    + " (" + progress.pointsAvailable() + " puntos disponibles)"), true);
            }
            this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.8F);
        }
        pushProgress();
    }

    public boolean unlockTalent(int idx) {
        if (this.level().isClientSide) return false;
        float oldMax = this.getMaxHealth();
        if (!progress.unlock(idx)) return false;
        applyStats();
        float diff = this.getMaxHealth() - oldMax;
        if (diff > 0) this.heal(diff);
        pushProgress();
        return true;
    }

    public void resetTalents() {
        if (this.level().isClientSide) return;
        progress.reset();
        applyStats();
        pushProgress();
    }

    private void serverProgressTick() {
        // Regeneracion (S2 siempre; S5 tras 10 s sin combate), una vez por segundo.
        if (this.tickCount % 20 == 0 && this.isAlive() && this.getHealth() < this.getMaxHealth()) {
            float pct = 0.0F;
            int s2 = progress.rank("S2");
            if (s2 > 0) pct += new float[]{0F, 0.10F, 0.15F, 0.20F}[s2];
            int s5 = progress.rank("S5");
            if (s5 > 0 && this.tickCount - lastCombatTick > 200) pct += new float[]{0F, 0.3F, 0.5F, 0.7F}[s5];
            if (pct > 0.0F) this.heal(this.getMaxHealth() * pct / 100.0F * supportBoost());
        }
        goldenAura();
        spectralTick();
        // Embestida (C3)
        if (chargeTicks > 0) {
            chargeTicks--;
            this.entityData.set(CHARGE_LEFT, chargeTicks);
            Vec3 mv = this.getDeltaMovement();
            Vec3 cdir = this.getControllingPassenger() instanceof Player ? Vec3.directionFromRotation(0.0F, this.getYRot()) : chargeDir;
            this.setDeltaMovement(cdir.x * 0.75, mv.y, cdir.z * 0.75);
            if (chargeTicks % 2 == 0) powerFx(this.getX() - cdir.x * 1.5, this.getY() + 0.3, this.getZ() - cdir.z * 1.5, 8, 0.8);
            if (this.horizontalCollision && this.tickCount % 2 == 0 && chargeTicks > 4) { // choca con un muro: termina la carrera
                chargeTicks = 3;
                this.playSound(ModSounds.STEGO_CHARGE_HIT.get(), 2.8F, 0.75F); // choque contra el muro
            }
            float dmg = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * new float[]{0F, 0.5F, 0.75F, 1.0F}[Math.max(1, progress.rank("C3"))];
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.0), this::isFoe)) {
                if (chargeHit.add(e.getId())) {
                    e.hurt(this.damageSources().mobAttack(this), dmg);
                    this.playSound(ModSounds.STEGO_CHARGE_HIT.get(), 2.6F, 0.9F + this.random.nextFloat() * 0.2F);
                    e.knockback(0.6, -cdir.x, -cdir.z);
                    stun(e, 40); // aturdido 2 s
                }
            }
            if (chargeTicks == 0) chargeHit.clear();
        }
        if (biteWindup > 0 && --biteWindup == 0) doBite();
        // Coletazo (basico, o devastador con C5): carga y golpe en area
        if (tailWindup > 0) {
            tailWindup--;
            if (tailWindup == 0) {
                int r = progress.rank("C5");
                float bonus = r > 0 ? new float[]{0F, 1.0F, 1.5F, 2.0F}[r] : 0.5F;
                float dmg = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.0F + bonus);
                this.playSound(ModSounds.STEGO_TAIL.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
                this.playSound(ModSounds.STEGO_TAIL_TREE.get(), 3.0F, 0.95F + this.random.nextFloat() * 0.1F);
                powerFx(this.getX(), this.getY() + 0.9, this.getZ(), 40, 2.4);
                LivingEntity firstFoe = null;
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.5), this::isFoe)) {
                    e.hurt(this.damageSources().mobAttack(this), dmg);
                    e.knockback(2.6, this.getX() - e.getX(), this.getZ() - e.getZ());
                    tailEffect(e);
                    if (firstFoe == null) firstFoe = e;
                }
                if (firstFoe != null && this.getVariant() == CYBERNETIC) chainLightning(firstFoe, dmg * 0.6F, 4);
                this.lastCombatTick = this.tickCount;
            }
        }
    }

    /** @param id 0 = embestida (C3), 1 = coletazo (basico; devastador con C5), 2 = mordida (clic izquierdo montado) */
    public boolean useAbility(Player player, int id) {
        if (this.level().isClientSide || this.isOrderedToSit() || !this.isAlive()) return false;
        if (ultWindup > 0) return false;
        if (id == 0) {
            int r = progress.rank("C3");
            if (r <= 0) { player.displayClientMessage(Component.literal("Embestida no desbloqueada"), true); return false; }
            if (this.tickCount < chargeReadyAt) { cooldownMsg(player, chargeReadyAt); return false; }
            this.chargeDir = Vec3.directionFromRotation(0.0F, player.getYRot());
            this.setYRot(player.getYRot()); this.yBodyRot = player.getYRot(); this.setYHeadRot(player.getYRot());
            this.chargeHit.clear();
            this.chargeTicks = 30; // carrera de 1,5 s
            this.entityData.set(CHARGE_LEFT, chargeTicks);
            this.playSound(ModSounds.STEGO_CHARGE_HIT.get(), 2.0F, 0.6F); // pisoton pesado al arrancar
            this.chargeReadyAt = this.tickCount + Math.round(300 * cooldownFactor());
            markCooldown(CD_CHARGE, Math.round(300 * cooldownFactor()));
            this.lastCombatTick = this.tickCount;
            return true;
        }
        if (id == 1) {
            // Coletazo basico siempre disponible (carga 0,4 s, +50 % de dano, recarga 5 s); el talento C5 lo vuelve devastador.
            int r = progress.rank("C5");
            if (this.tickCount < tailReadyAt) { cooldownMsg(player, tailReadyAt); return false; }
            this.tailWindup = 18; // 0,9 s: el latigazo de la animacion golpea a ese tiempo
            this.level().broadcastEntityEvent(this, (byte) 61);
            this.tailReadyAt = this.tickCount + Math.round(20 * (r > 0 ? new int[]{0, 10, 9, 8}[r] : 5) * cooldownFactor());
            markCooldown(CD_TAIL, Math.round(20 * (r > 0 ? new int[]{0, 10, 9, 8}[r] : 5) * cooldownFactor()));
            return true;
        }
        if (id == 2) { // mordida con el clic izquierdo montado: dano pequeno, recarga corta
            if (this.tickCount < biteReadyAt || this.biteWindup > 0) return false;
            this.biteWindup = 9; // la mandibula se cierra a ~0,45 s en la animacion
            this.biteReadyAt = this.tickCount + 16;
            markCooldown(CD_BITE, 16);
            this.level().broadcastEntityEvent(this, (byte) 62);
            return true;
        }
        if (id == 3) { // definitiva de la raza, solo montado
            if (player.getVehicle() != this) { player.displayClientMessage(Component.literal("La definitiva solo se usa montado"), true); return false; }
            if (ultimateLevel() <= 0) { player.displayClientMessage(Component.literal("La definitiva se obtiene al nivel 50"), true); return false; }
            if (this.tickCount < ultReadyAt) { cooldownMsg(player, ultReadyAt); return false; }
            int ucd = Math.round((1200 - 20 * new int[]{0, 8, 14, 20}[progress.rank("U3")]) * cooldownFactor()); // 60 s, menos con la mejora de recarga y Dominio de combate
            this.ultReadyAt = this.tickCount + ucd;
            markCooldown(CD_ULT, ucd);
            this.lastCombatTick = this.tickCount;
            startUltimate(player);
            return true;
        }
        return false;
    }


    /** Particulas de los poderes segun la raza: 0 cibernetico, 1 dorado, 2 magma, 3 esmeralda, 4 natural. */
    private void powerFx(double x, double y, double z, int count, double spread) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        net.minecraft.core.particles.SimpleParticleType main, extra;
        switch (this.getVariant()) {
            case CYBERNETIC -> { main = net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK; extra = net.minecraft.core.particles.ParticleTypes.END_ROD; }
            case GOLDEN -> { main = net.minecraft.core.particles.ParticleTypes.WAX_ON; extra = net.minecraft.core.particles.ParticleTypes.END_ROD; }
            case MAGMA -> { main = net.minecraft.core.particles.ParticleTypes.FLAME; extra = net.minecraft.core.particles.ParticleTypes.LAVA; }
            case GLACIAL -> { main = net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER; extra = net.minecraft.core.particles.ParticleTypes.SCULK_SOUL; }
            default -> { main = net.minecraft.core.particles.ParticleTypes.COMPOSTER; extra = net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR; }
        }
        server.sendParticles(main, x, y, z, count, spread, spread * 0.6, spread, 0.04);
        server.sendParticles(extra, x, y, z, Math.max(1, count / 3), spread, spread * 0.6, spread, 0.02);
    }

    // ---------------------------------------------------------------- pasivas y definitiva por raza
    private final java.util.Map<Integer, Integer> spectral = new java.util.HashMap<>();
    private int ultReadyAt = 0;

    private static net.minecraft.core.particles.ParticleOptions emeraldDust() {
        return new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.2F, 1.0F, 0.45F), 1.3F);
    }

    /** Magma y esmeralda son inmunes al fuego (y a la lava). */
    @Override public boolean fireImmune() {
        return this.getVariant() == MAGMA || this.getVariant() == GLACIAL || super.fireImmune();
    }

    /** Efecto del coletazo sobre cada enemigo alcanzado, segun la raza. */
    private void tailEffect(LivingEntity e) {
        switch (this.getVariant()) {
            case MAGMA -> e.setSecondsOnFire(6);
            case GOLDEN -> e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0));
            case GLACIAL -> spectralBurn(e, 100);
            case NATURAL -> e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            default -> { } // cibernetico: rayo encadenado, ver chainLightning
        }
    }

    /** Fuego espectral verde: 2 de dano por segundo (el doble que el fuego normal). */
    private void spectralBurn(LivingEntity e, int ticks) {
        if (e.fireImmune()) return;
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.SPECTRAL_BURN.get(), ticks, 0, false, false, false), this);
    }

    private void spectralTick() { }

    private void zap(Vec3 a, Vec3 b) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        int n = Math.max(2, (int) (a.distanceTo(b) * 3));
        for (int k = 0; k <= n; k++) {
            Vec3 pnt = a.lerp(b, k / (double) n);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pnt.x, pnt.y, pnt.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Rayo que rebota entre enemigos: el primero ya recibio el golpe de cola; los siguientes reciben dmg. */
    private void chainLightning(LivingEntity start, float dmg, int bounces) {
        java.util.Set<Integer> hit = new java.util.HashSet<>();
        LivingEntity cur = start;
        Vec3 from = this.position().add(0.0, 1.8, 0.0);
        for (int i = 0; i <= bounces && cur != null; i++) {
            hit.add(cur.getId());
            Vec3 to = cur.position().add(0.0, cur.getBbHeight() * 0.5, 0.0);
            zap(from, to);
            if (i > 0) cur.hurt(this.damageSources().mobAttack(this), dmg);
            from = to;
            LivingEntity next = null;
            double best = 49.0;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, cur.getBoundingBox().inflate(7.0), this::isFoe)) {
                if (hit.contains(e.getId())) continue;
                double d = e.distanceToSqr(cur);
                if (d < best) { best = d; next = e; }
            }
            cur = next;
        }
        this.playSound(net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F, 1.6F);
    }

    /** Aura dorada: cada 2 s regenera un poco al dino, a su dueno y a sus aliados cercanos. */
    private void goldenAura() {
        if (this.getVariant() != GOLDEN || !this.isAlive() || this.tickCount % 40 != 0) return;
        if (this.getHealth() < this.getMaxHealth()) this.heal(1.0F);
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7.0),
                x -> x != this && (x == this.getOwner() || (x instanceof TamableAnimal t && t.isTame() && t.getOwner() == this.getOwner())))) {
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 60, 0, true, false));
        }
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON, this.getX(), this.getY() + 1.5, this.getZ(), 8, 3.0, 1.0, 3.0, 0.0);
    }

    private void ring(net.minecraft.core.particles.ParticleOptions type, double radius, int count) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        for (int k = 0; k < count; k++) {
            double a = k * Math.PI * 2.0 / count;
            server.sendParticles(type, this.getX() + Math.cos(a) * radius, this.getY() + 0.4, this.getZ() + Math.sin(a) * radius, 1, 0.1, 0.3, 0.1, 0.02);
        }
    }

    /** Nivel de la definitiva: 1 desde el nivel 50, 2 desde el 75 y 3 en el 100. */
    public int ultimateLevel() {
        int lv = progress.level();
        return lv >= 100 ? 3 : lv >= 75 ? 2 : lv >= 50 ? 1 : 0;
    }

    /** Habilidad definitiva de cada raza (solo montado). */

    // ---------------------------------------------------------------- animacion de la definitiva
    private int ultWindup = 0, ultWave = 0, ultRootTicks = 0;
    private final java.util.List<double[]> ultRoots = new java.util.ArrayList<>();
    private Player ultCaster;
    private static final int ULT_WINDUP = 28; // 1,4 s: el golpe cae en el cuadro del pisoton de la animacion

    /** Polvo de color vivo de la raza (nada de blanco): cian, oro, naranja, esmeralda o verde hoja. */
    private net.minecraft.core.particles.ParticleOptions ultDust(float size) {
        org.joml.Vector3f c = switch (this.getVariant()) {
            case CYBERNETIC -> new org.joml.Vector3f(0.10F, 0.80F, 1.00F);
            case GOLDEN -> new org.joml.Vector3f(1.00F, 0.72F, 0.05F);
            case MAGMA -> new org.joml.Vector3f(1.00F, 0.30F, 0.02F);
            case GLACIAL -> new org.joml.Vector3f(0.20F, 1.00F, 0.45F);
            default -> new org.joml.Vector3f(0.45F, 0.90F, 0.15F);
        };
        return new net.minecraft.core.particles.DustParticleOptions(c, size);
    }

    private net.minecraft.core.particles.ParticleOptions ultMain() {
        return switch (this.getVariant()) {
            case MAGMA -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case GLACIAL -> ModParticles.SPECTRAL_FLAME.get();
            case GOLDEN -> net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
            default -> ultDust(1.4F);
        };
    }

    /** Llama grande para anillos e impacto de la definitiva (magma y esmeralda). */
    private net.minecraft.core.particles.ParticleOptions ultBig() {
        return switch (this.getVariant()) {
            case MAGMA -> ModParticles.BIG_FLAME.get();
            case GLACIAL -> ModParticles.SPECTRAL_FLAME_BIG.get();
            case GOLDEN -> net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE;
            default -> ultDust(2.4F);
        };
    }

    /** Aturdimiento: no se mueve, no pega y pierde su objetivo durante el tiempo indicado. */
    private void stun(LivingEntity e, int ticks) {
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false, true), this);
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, ticks, 9, false, false, false), this);
        if (e instanceof net.minecraft.world.entity.Mob m) m.setTarget(null);
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 16, 0.4, 0.2, 0.4, 0.2);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), 12, 0.4, 0.3, 0.4, 0.5);
        }
        e.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.8F, 1.4F);
    }

    private void startUltimate(Player player) {
        this.ultCaster = player;
        this.ultWindup = ULT_WINDUP;
        this.getNavigation().stop();
        this.level().broadcastEntityEvent(this, (byte) 66);
        if ((this.getVariant() == CYBERNETIC || this.getVariant() == MAGMA || this.getVariant() == GLACIAL) && this.level() instanceof net.minecraft.server.level.ServerLevel sl) startNight(sl); // anochece mientras carga (azul, magma y esmeralda)
        this.playSound(net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, 2.5F, 0.6F);
    }

    private void ultTick() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        nightStep(server);
        double x = this.getX(), y = this.getY(), z = this.getZ();
        if (ultWave > 0 && (this.getVariant() == MAGMA || this.getVariant() == GLACIAL)) { // llamas crecientes que avanzan en pared, sin estrella
            int step = 23 - ultWave;
            double R = step * 0.85;
            int n = (int) (16 + R * 4);
            for (int k = 0; k < n; k++) {
                double a = k * Math.PI * 2.0 / n;
                server.sendParticles(ultBig(), x + Math.cos(a) * R, y + 0.2, z + Math.sin(a) * R, 2, 0.1, 0.4 + step * 0.06, 0.1, 0.04);
            }
            ultWave--;
        }
        if (ultWave > 0) { // onda expansiva en forma de estrella de puntas (como las placas dorsales), tres capas que giran
            int step = 23 - ultWave;
            double R = step * 0.85;
            var type = ultBig();
            int per = 3 + step / 2; // puntos por arista
            for (int layer = 0; layer < 3; layer++) {
                double rot = step * 0.12 * (layer % 2 == 0 ? 1 : -1) + layer * 0.314, sc = 1.0 - layer * 0.2, yy = layer == 0 ? 0.25 : layer == 1 ? 1.4 + step * 0.05 : 3.0 + step * 0.12;
                for (int v = 0; v < 10; v++) { // 5 puntas largas y 5 hendiduras
                    double a0 = rot + v * Math.PI / 5.0, a1 = rot + (v + 1) * Math.PI / 5.0;
                    double r0 = (v % 2 == 0 ? 1.0 : 0.42) * R * sc, r1 = (v % 2 == 0 ? 0.42 : 1.0) * R * sc;
                    double x0 = Math.cos(a0) * r0, z0 = Math.sin(a0) * r0, x1 = Math.cos(a1) * r1, z1 = Math.sin(a1) * r1;
                    for (int k = 0; k < per; k++) {
                        double t = k / (double) per;
                        server.sendParticles(layer == 0 ? type : ultMain(), x + x0 + (x1 - x0) * t, y + yy, z + z0 + (z1 - z0) * t, 1, 0.04, 0.12, 0.04, 0.01);
                    }
                }
            }
            ultWave--;
        }
        if (ultRootTicks > 0) { // raices que brotan del suelo, se curvan y se quedan un momento
            double g = Math.min(1.0, (37 - ultRootTicks) / 12.0);
            var root = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS.defaultBlockState());
            var wood = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DARK_OAK_LOG.defaultBlockState());
            for (double[] rt : ultRoots) {
                double top = rt[3] * g;
                for (double h = 0; h <= top; h += 0.3) {
                    double bend = h * h * 0.12;
                    server.sendParticles(h > top - 0.7 ? wood : root, rt[0] + Math.cos(rt[2]) * bend, y + h, rt[1] + Math.sin(rt[2]) * bend, 1, 0.05, 0.0, 0.05, 0.0);
                }
            }
            ultRootTicks--;
        }
        if (ultWindup <= 0) return;
        int left = ultWindup--;
        if (!this.isAlive()) { ultWindup = 0; return; }
        double prog = (ULT_WINDUP - left) / (double) ULT_WINDUP;
        for (int arm = 0; arm < 4; arm++) { // doble helice que se cierra sobre el cuerpo y sube
            double a = this.tickCount * 0.6 + arm * 1.571, r = 4.2 * (1.0 - prog) + 0.8;
            server.sendParticles(ultMain(), x + Math.cos(a) * r, y + 0.2 + prog * 3.5, z + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
            server.sendParticles(ultBig(), x + Math.cos(a + 0.8) * r * 1.2, y + 0.3 + prog * 1.5, z + Math.sin(a + 0.8) * r * 1.2, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (left % 4 == 0) { // anillo en el suelo que se contrae: se ve de lejos
            double rr = 7.0 * (1.0 - prog) + 1.5;
            for (int k = 0; k < 36; k++) {
                double a = k * Math.PI * 2.0 / 36.0;
                server.sendParticles(ultBig(), x + Math.cos(a) * rr, y + 0.15, z + Math.sin(a) * rr, 1, 0.0, 0.05, 0.0, 0.0);
            }
        }
        if (left == 24) this.playSound(net.minecraft.sounds.SoundEvents.BEACON_AMBIENT, 3.0F, 1.5F);
        if (left == 18) this.playSound(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 1.0F);
        if (left == 10) this.playSound(ModSounds.STEGO_ULT_ROAR.get(), 4.0F, 1.0F); // el rugido coincide con la boca abierta
        if (left == 6 && isNaturalUlt()) ultImpactFx(server); // la tierra salta antes del pisoton
        if (left == 1) {
            Player who = this.getControllingPassenger() instanceof Player p ? p : ultCaster;
            if (who != null) { castUltimate(who); if (!isNaturalUlt()) ultImpactFx(server); }
            ultWave = 22;
            this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 3.0F, 0.5F);
            this.playSound(net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE, 3.0F, 0.6F);
            server.sendParticles(ultBig(), x, y + 1.0, z, 120, 3.0, 0.8, 3.0, 0.3);
        }
    }

    // --- cielo nocturno temporal (cibernetico): el reloj del mundo se desliza hacia la noche y vuelve
    private int nightTick = 0;
    private long nightBase = 0L;
    private static final int NIGHT_TICKS = 100;

    private boolean isNaturalUlt() {
        int v = this.getVariant();
        return v != CYBERNETIC && v != GOLDEN && v != MAGMA && v != GLACIAL;
    }

    private void startNight(net.minecraft.server.level.ServerLevel server) {
        if (server.dimension() != net.minecraft.world.level.Level.OVERWORLD || nightTick > 0) return;
        nightBase = server.getDayTime();
        nightTick = NIGHT_TICKS;
    }

    private void nightStep(net.minecraft.server.level.ServerLevel server) {
        if (nightTick <= 0) return;
        int t = NIGHT_TICKS - nightTick;
        boolean cycle = server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        long natural = nightBase + (cycle ? t + 1 : 0);
        nightTick--;
        long tod = natural % 24000L;
        double u = t < 22 ? (t + 1) / 22.0 : t < 62 ? 1.0 : Math.max(0.0, (NIGHT_TICKS - t) / (double) (NIGHT_TICKS - 62)); // sube rapido, aguanta y vuelve
        double f = u * u * (3.0 - 2.0 * u);
        long goal = 15000L; // anochecer oscuro, no negro total
        long target = tod >= goal && tod < 23000L ? tod : (long) (tod + ((tod < goal ? goal : goal + 24000L) - tod) * f);
        long out = nightTick == 0 ? natural : natural - tod + target;
        server.setDayTime(out);
        var pk = new net.minecraft.network.protocol.game.ClientboundSetTimePacket(server.getGameTime(), cycle ? out : -out, cycle);
        for (net.minecraft.server.level.ServerPlayer sp : server.players()) sp.connection.send(pk); // cada tick, para que el cambio sea fluido y rapido
    }

    /** Esfera de humo oscuro: simula nubes que ciegan. */
    private void smokeBall(net.minecraft.server.level.ServerLevel server, double cx, double cy, double cz, double r) {
        for (int k = 0; k < 46; k++) {
            double u = this.random.nextDouble() * 2.0 - 1.0, a = this.random.nextDouble() * Math.PI * 2.0, q = Math.sqrt(1.0 - u * u);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, cx + q * Math.cos(a) * r, cy + u * r, cz + q * Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.01);
            if (k % 3 == 0) server.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, cx + q * Math.cos(a) * r * 0.6, cy + u * r * 0.6, cz + q * Math.sin(a) * r * 0.6, 0, 0.0, 0.04, 0.0, 1.0);
        }
    }

    /** Efecto de impacto propio de cada raza (ademas de los anillos del poder). */
    private void ultImpactFx(net.minecraft.server.level.ServerLevel server) {
        double x = this.getX(), y = this.getY(), z = this.getZ();
        switch (this.getVariant()) {
            case CYBERNETIC -> {
                int ul = Math.max(1, ultimateLevel()), n = 0;
                for (int i = 0; i < 5; i++) { // 5 rayos reales alrededor del dino
                    double a = i * Math.PI * 2.0 / 5.0 + 0.3, rr = 6.0 + this.random.nextDouble() * 2.0;
                    net.minecraft.world.entity.LightningBolt b2 = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(server);
                    if (b2 == null) continue;
                    b2.moveTo(x + Math.cos(a) * rr, y, z + Math.sin(a) * rr);
                    b2.setVisualOnly(true);
                    server.addFreshEntity(b2);
                }
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(8.0 + 2.0 * ul), this::isFoe)) {
                    if (n++ >= 3) break;
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(server);
                    if (bolt == null) continue;
                    bolt.moveTo(e.getX(), e.getY(), e.getZ());
                    bolt.setVisualOnly(true);
                    server.addFreshEntity(bolt);
                }
                server.sendParticles(ultDust(1.9F), x, y + 1.5, z, 240, 5.0, 3.0, 5.0, 0.0);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y + 1.5, z, 60, 4.0, 2.0, 4.0, 0.5);
            }
            case GOLDEN -> { // bolas de humo alrededor (la ceguera): una esfera sobre cada enemigo y varias en torno al dino
                for (int i = 0; i < 8; i++) smokeBall(server, x + Math.cos(i * 0.785) * 6.0, y + 1.8, z + Math.sin(i * 0.785) * 6.0, 2.2);
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(14.0), this::isFoe))
                    smokeBall(server, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 1.6);
            }
            case MAGMA -> {
                server.sendParticles(ModParticles.BIG_FLAME.get(), x, y + 0.5, z, 60, 2.5, 0.6, 2.5, 0.3);
                for (int i = 0; i < 40; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2.0, r = 1.5 + this.random.nextDouble() * 12.0;
                    double px = x + Math.cos(a) * r, pz = z + Math.sin(a) * r;
                    server.sendParticles(ModParticles.BIG_FLAME.get(), px, y + 0.3, pz, 14, 0.3, 2.5, 0.3, 0.2);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, px, y + 0.5, pz, 4, 0.2, 0.3, 0.2, 0.0);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, px, y + 1.0, pz, 3, 0.2, 0.6, 0.2, 0.03);
                }
            }
            case GLACIAL -> {
                for (int arm = 0; arm < 5; arm++) { // cinco espirales de llama espectral que se abren
                    for (int k = 0; k < 45; k++) {
                        double a = arm * 1.2566 + k * 0.22, r = 0.6 + k * 0.33;
                        server.sendParticles(ModParticles.SPECTRAL_FLAME_BIG.get(), x + Math.cos(a) * r, y + 0.3 + k * 0.05, z + Math.sin(a) * r, 1, 0.2, 0.3, 0.2, 0.01);
                    }
                }
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.5, z, 140, 7.0, 1.0, 7.0, 0.15);
            }
            default -> { // golpe fisico: la tierra salta en trozos y brotan raices
                var block = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, this.level().getBlockState(this.blockPosition().below()));
                for (int ring = 1; ring <= 4; ring++) {
                    for (int k = 0; k < 28; k++) {
                        double a = k * Math.PI * 2.0 / 28.0, r = ring * 2.5;
                        server.sendParticles(block, x + Math.cos(a) * r, y + 0.2, z + Math.sin(a) * r, 8, 0.3, 0.6, 0.3, 0.4);
                    }
                }
                for (int i = 0; i < 40; i++) { // trozos de tierra que saltan: solo particulas, no se mueve ningun bloque real
                    double a = this.random.nextDouble() * Math.PI * 2.0, r = 2.0 + this.random.nextDouble() * 9.0;
                    net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(x + Math.cos(a) * r, y - 0.5, z + Math.sin(a) * r);
                    net.minecraft.world.level.block.state.BlockState st = server.getBlockState(bp);
                    if (st.isAir()) continue;
                    var chunk = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, st);
                    for (int k = 0; k < 5; k++)
                        server.sendParticles(chunk, bp.getX() + 0.5, y + 0.3 + k * 0.1, bp.getZ() + 0.5, 0, (this.random.nextDouble() - 0.5) * 0.3, 0.8 + this.random.nextDouble() * 0.6, (this.random.nextDouble() - 0.5) * 0.3, 0.9);
                }
                ultRoots.clear();
                for (int i = 0; i < 22; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2.0, r = 2.5 + this.random.nextDouble() * 9.0;
                    ultRoots.add(new double[]{x + Math.cos(a) * r, z + Math.sin(a) * r, this.random.nextDouble() * Math.PI * 2.0, 2.0 + this.random.nextDouble() * 2.5});
                }
                ultRootTicks = 36;
            }
        }
    }

    private void castUltimate(Player player) {
        int ul = Math.max(1, ultimateLevel());
        float atk = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * new float[]{1.0F, 1.0F, 1.5F, 2.0F}[ul] // nivel 1/2/3: x1 / x1,5 / x2
            * (1.0F + new float[]{0F, 0.15F, 0.30F, 0.50F}[progress.rank("U1")]); // mejora de potencia
        double reach = 8.0 + 2.0 * ul + 1.5 * progress.rank("U2"); // alcance 10 / 12 / 14, mas la mejora
        java.util.List<LivingEntity> foes = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach), this::isFoe);
        switch (this.getVariant()) {
            case CYBERNETIC -> { // Tormenta cibernetica: rayos sobre hasta 8 enemigos
                this.playSound(net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, 2.5F, 1.0F);
                int n = 0;
                for (LivingEntity e : foes) {
                    if (n++ >= 4 + 2 * ul + progress.rank("U2")) break;
                    zap(this.position().add(0.0, 4.0, 0.0), e.position().add(0.0, e.getBbHeight() * 0.5, 0.0));
                    e.hurt(this.damageSources().mobAttack(this), atk * 3.0F);
                }
                ring(ultDust(1.9F), 6.0, 32);
            }
            case GOLDEN -> { // Resplandor dorado: cura a los aliados y ciega a los enemigos
                this.playSound(net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 2.5F, 1.2F);
                this.heal(this.getMaxHealth() * 0.4F * supportBoost());
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(reach),
                        x -> x != this && (x == this.getOwner() || (x instanceof TamableAnimal t && t.isTame() && t.getOwner() == this.getOwner())))) {
                    e.heal(e.getMaxHealth() * 0.4F * supportBoost());
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 160, 1));
                }
                for (LivingEntity e : foes) {
                    e.hurt(this.damageSources().mobAttack(this), atk);
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 0));
                }
                ring(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 5.0, 40);
                ring(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 8.0, 40);
            }
            case MAGMA -> { // Erupcion: onda de fuego
                this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
                for (LivingEntity e : foes) {
                    e.hurt(this.damageSources().mobAttack(this), atk * 2.5F);
                    e.setSecondsOnFire(8);
                    e.knockback(1.6, this.getX() - e.getX(), this.getZ() - e.getZ());
                }
                ring(net.minecraft.core.particles.ParticleTypes.FLAME, 4.0, 40);
                ring(net.minecraft.core.particles.ParticleTypes.LAVA, 7.0, 40);
            }
            case GLACIAL -> { // Llamarada espectral verde
                this.playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.9F);
                for (LivingEntity e : foes) {
                    e.hurt(this.damageSources().mobAttack(this), atk * 2.0F);
                    spectralBurn(e, 160);
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 0));
                }
                ring(emeraldDust(), 3.0, 40);
                ring(emeraldDust(), 6.0, 60);
                ring(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, 8.0, 30);
            }
            default -> { // Estampida natural: pisotón que frena y empuja
                this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 1.8F, 0.5F);
                for (LivingEntity e : foes) {
                    e.hurt(this.damageSources().mobAttack(this), atk * 1.5F);
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    e.knockback(2.0, this.getX() - e.getX(), this.getZ() - e.getZ());
                }
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
                ring(ultDust(1.9F), 4.0, 28);
                ring(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR, 7.0, 40);
            }
        }
    }

    /** Mordida: golpea al enemigo mas cercano delante de la boca (daño = 40 % del ataque). */
    private void doBite() {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 mouth = this.position().add(fwd.x * 5.0, 2.0, fwd.z * 5.0);
        LivingEntity best = null;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(mouth, mouth).inflate(2.6, 2.4, 2.6), this::isFoe)) {
            if (best == null || e.distanceToSqr(mouth) < best.distanceToSqr(mouth)) best = e;
        }
        this.playSound(ModSounds.STEGO_BITE.get(), 1.8F, 0.9F + this.random.nextFloat() * 0.2F);
        powerFx(mouth.x, mouth.y, mouth.z, 10, 0.5);
        if (best == null) return;
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F * furyMultiplier();
        if (best.hurt(this.damageSources().mobAttack(this), dmg)) {
            best.knockback(0.5, this.getX() - best.getX(), this.getZ() - best.getZ());
            this.lastCombatTick = this.tickCount;
        }
    }

    private void cooldownMsg(Player p, int readyAt) {
        p.displayClientMessage(Component.literal("Recarga: " + Math.max(1, (readyAt - this.tickCount + 19) / 20) + " s"), true);
    }

    /** Distancia a partir de la cual el compañero se teletransporta junto al dueño (talento S3). */
    public double rejoinDistance() {
        return new double[]{64.0, 24.0, 18.0, 12.0}[progress.rank("S3")];
    }

    /** Reduccion de dano por caida del dueño (S4) que aporta este compañero, 0..1. */
    public float fallReduction() {
        int r = progress.rank("S4");
        if (r <= 0) return 0.0F;
        return Math.min(1.0F, 0.30F * r * supportBoost());
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.lastCombatTick = this.tickCount;
            if (this.tickCount < guardUntil) amount *= (1.0F - guardPct / 100.0F);
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && !this.isTame() && source.getEntity() instanceof LivingEntity culprit
                && !(culprit instanceof StegosaurusEntity) && culprit.isAlive()
                && !(culprit instanceof Player pl && (pl.isCreative() || pl.isSpectator()))) {
            // Manada: quien ataque a uno, es atacado por todos los silvestres cercanos
            this.setTarget(culprit);
            for (StegosaurusEntity other : this.level().getEntitiesOfClass(StegosaurusEntity.class, this.getBoundingBox().inflate(40.0),
                    o -> o != this && o.isAlive() && !o.isTame())) {
                other.setTarget(culprit);
            }
        }
        if (result && !this.level().isClientSide && isTame() && isAlive() && !isFallen() && getRevertTicks() == 0 && getEvolveTicks() == 0
                && getHealth() <= getMaxHealth() * 0.10F) {
            // A punto de caer: se encoge a chibi (con 10 % de vida) para protegerse; sin espacio libre sigue como adulto.
            revertToChibi(true);
        }
        if (result && !this.level().isClientSide) {
            // D4 Instinto defensivo: al bajar del 50 % de vida
            int d4 = progress.rank("D4");
            if (d4 > 0 && this.tickCount >= guardReadyAt && this.tickCount >= guardUntil
                    && this.getHealth() < this.getMaxHealth() * 0.5F) {
                guardPct = new int[]{0, 20, 30, 40}[d4];
                guardUntil = this.tickCount + 100;
                guardReadyAt = this.tickCount + Math.round(20 * new int[]{0, 60, 50, 40}[d4] * cooldownFactor());
            }
            // C4 Puas afiladas: devuelve parte del dano cuerpo a cuerpo
            int c4 = progress.rank("C4");
            if (c4 > 0 && !source.is(DamageTypes.THORNS) && source.getEntity() instanceof LivingEntity attacker
                    && source.getDirectEntity() == source.getEntity() && isFoeOrPlayerNotAlly(attacker)) {
                float back = Math.min(15.0F, amount * new float[]{0F, 0.15F, 0.25F, 0.35F}[c4]);
                if (back > 0.0F) attacker.hurt(this.damageSources().thorns(this), back);
            }
        }
        return result;
    }

    private boolean isFoeOrPlayerNotAlly(LivingEntity e) {
        if (e == this || e == this.getOwner()) return false;
        if (e instanceof TamableAnimal t && t.isTame()) return false;
        return true;
    }

    /** Teletransporte a un sitio libre junto al dueño. */
    private void rejoinOwner(LivingEntity owner) {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 40; i++) {
            int dx = this.random.nextInt(11) - 5, dz = this.random.nextInt(11) - 5, dy = this.random.nextInt(3) - 1;
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;
            BlockPos p = base.offset(dx, dy, dz);
            BlockPos below = p.below();
            if (!this.level().getBlockState(below).isFaceSturdy(this.level(), below, Direction.UP)) continue;
            AABB box = this.getBoundingBox().move(p.getX() + 0.5 - this.getX(), p.getY() - this.getY(), p.getZ() + 0.5 - this.getZ());
            if (!this.level().noCollision(this, box)) continue;
            this.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, this.getYRot(), this.getXRot());
            this.getNavigation().stop();
            return;
        }
    }

    /** Seguir al dueño caminando; teletransportarse solo a partir de rejoinDistance() (talento S3). */
    private final class RejoinFollowGoal extends Goal {
        private final double speed; private final float startDist, stopDist; private int recalc;
        RejoinFollowGoal(double speed, float startDist, float stopDist) {
            this.speed = speed; this.startDist = startDist; this.stopDist = stopDist;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        private LivingEntity owner() { return StegosaurusEntity.this.getOwner(); }
        @Override public boolean canUse() {
            LivingEntity o = owner();
            if (o == null || o.isSpectator() || StegosaurusEntity.this.isOrderedToSit() || StegosaurusEntity.this.isLeashed() || getOrder() != PetOrders.FOLLOW) return false;
            return StegosaurusEntity.this.distanceToSqr(o) >= (double)(startDist * startDist);
        }
        @Override public boolean canContinueToUse() {
            LivingEntity o = owner();
            if (o == null || StegosaurusEntity.this.isOrderedToSit() || getOrder() != PetOrders.FOLLOW) return false;
            return StegosaurusEntity.this.distanceToSqr(o) > (double)(stopDist * stopDist);
        }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { StegosaurusEntity.this.getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity o = owner();
            if (o == null) return;
            StegosaurusEntity.this.getLookControl().setLookAt(o, 10.0F, (float)StegosaurusEntity.this.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 10;
            double d = rejoinDistance();
            if (StegosaurusEntity.this.distanceToSqr(o) >= d * d) rejoinOwner(o);
            else StegosaurusEntity.this.getNavigation().moveTo(o, speed);
        }
    }

    /** Hay sitio libre (3x3, 4 de alto) para un adulto en este punto? Evita nacer dentro de arboles o muros. */
    public static boolean roomFor(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 3; dy++) {
            net.minecraft.core.BlockPos p = pos.offset(dx, dy, dz);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getFluidState(p).isEmpty()) return false;
        }
        return true;
    }

    /** Si queda atrapado en un arbol o muro, sube hasta el primer hueco libre (y nunca muere asfixiado). */
    private void unstick() {
        if (this.level().noCollision(this, this.getBoundingBox())) return;
        for (int dy = 1; dy <= 12; dy++) {
            if (this.level().noCollision(this, this.getBoundingBox().move(0.0, dy, 0.0))) {
                this.setPos(this.getX(), this.getY() + dy, this.getZ());
                return;
            }
        }
    }

    @Override public boolean isInvulnerableTo(DamageSource damage) {
        if (damage.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return true; // los arboles/muros no lo asfixian
        if ((getEvolveTicks() > 0 || getRevertTicks() > 0 || isFallen()) && !damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return (this.getVariant() == MAGMA && damage.is(DamageTypeTags.IS_FIRE)) || super.isInvulnerableTo(damage);
    }

    @Nullable @Override protected SoundEvent getAmbientSound() { return null; } // sin gruñido ambiental ("mm, mm")
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.STEGO_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.STEGO_DEATH.get(); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.STEGO_STEP.get(), 0.9F, 0.9F + this.random.nextFloat() * 0.2F);
    }
    @Override protected float getSoundVolume() { return 1.5F; }
    @Override public int getAmbientSoundInterval() { return 220; }

    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // Incubation/breeding intentionally planned for v0.2.
    }
}
