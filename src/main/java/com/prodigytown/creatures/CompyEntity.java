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
 * UNA sola malla y ocho variantes de textura (verde, rojo, azul, gris, amarillo, rosa, albino, melanico). Se doma con carne y obedece las
 * ordenes comunes (Seguir, Quieto, Deambular, Patrullar, Sentarse, Dormir). Habilidades, talentos y HUD llegan despues.
 */
public class CompyEntity extends TamableAnimal implements PetOrders.Holder {
    public static final String[] VARIANT_IDS = {"verde", "rojo", "azul", "gris", "amarillo", "rosa", "albino", "melanico"};
    public static final String[] VARIANT_NAMES = {"Verde", "Rojo", "Azul", "Gris", "Amarillo", "Rosa", "Albino", "Melánico"};
    public static final int VERDE = 0, ROJO = 1, AZUL = 2, GRIS = 3, AMARILLO = 4, ROSA = 5, ALBINO = 6, MELANICO = 7, VARIANTS = 8;
    public static final int BITE_LEN = 10, EAT_LEN = 36;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORDER = SynchedEntityData.defineId(CompyEntity.class, EntityDataSerializers.INT);

    // ---- solo cliente: estado de animacion suavizado (cada valor guarda su version del tick anterior ...O para interpolar) ----
    public float sitAnim, sitAnimO, sleepAnim, sleepAnimO;
    public float gait, gaitO, speedAnim, speedAnimO, runAnim, runAnimO, aggroAnim, aggroAnimO, turnAnim, turnAnimO, airAnim, airAnimO;
    public float airVel;
    /** Mordida visual: ticks desde que empezo (-1 = sin golpe). Comer visual: idem. */
    public int biteT = -1, biteTO = -1, eatT = -1, eatTO = -1;

    @Nullable private BlockPos orderHome;
    private int trust, lastFeedTick = -100000;
    private boolean variantFixed = false;

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
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Trust", trust);
        tag.putInt("PetOrder", getOrder());
        if (orderHome != null) tag.putLong("OrderHome", orderHome.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) { setVariant(tag.getInt("Variant")); variantFixed = true; }   // /summon ... {Variant:3} o huevo con variante concreta
        this.trust = tag.getInt("Trust");
        this.entityData.set(ORDER, PetOrders.clamp(tag.getInt("PetOrder")));
        this.orderHome = tag.contains("OrderHome") ? BlockPos.of(tag.getLong("OrderHome")) : null;
        if (isTame()) this.setPersistenceRequired();
    }

    public int getVariant() { return this.entityData.get(VARIANT); }
    public void setVariant(int v) { this.entityData.set(VARIANT, Math.max(0, Math.min(VARIANTS - 1, v))); }
    public int getTrust() { return trust; }

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
        final int base;
        PackData(int base) { this.base = base; }
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if (!variantFixed) {
            int base;
            if (spawnData instanceof PackData pd) base = pd.base;
            else { base = baseVariantFor(level.getBiome(this.blockPosition())); data = new PackData(base); }
            setVariant(rareRoll(level, base));
        }
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
            @Override public boolean canUse() { return getOrder() == PetOrders.FOLLOW && super.canUse(); }
            @Override public boolean canContinueToUse() { return getOrder() == PetOrders.FOLLOW && super.canContinueToUse(); }
        });
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new PetOrders.PatrolGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7D) {
            @Override public boolean canUse() { return (!isTame() || getOrder() == PetOrders.WANDER) && super.canUse(); }
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
        else PetOrders.serverTick(this);
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
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(16.0);
            this.setHealth(16.0F);
            PetOrders.setOrder(this, PetOrders.FOLLOW);
            this.trust = 0;
            this.playSound(ModSounds.COMPY_HAPPY.get(), 1.0F, 1.0F);
            this.level().broadcastEntityEvent(this, (byte) 7);   // corazones
            player.displayClientMessage(Component.literal("¡Tu compy confía en ti!"), true);
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
