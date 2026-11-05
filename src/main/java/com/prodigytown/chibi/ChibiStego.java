package com.prodigytown.chibi;

import com.prodigytown.creatures.ModSounds;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.progress.GrowthTable;
import com.prodigytown.creatures.progress.ProdigyPet;
import com.prodigytown.creatures.progress.ProdigyProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Chibi domable: niveles 1-29 (bebe y juvenil). Al llegar al nivel 30 evoluciona a estegosaurio adulto
 * conservando dueño, variante, nombre, XP y puntos. Mantiene el NBT "ChibiVariant" y las variantes 0..3.
 */
public class ChibiStego extends TamableAnimal implements GeoEntity, ProdigyPet {
    private static final EntityDataAccessor<Integer> VARIANT =
        SynchedEntityData.defineId(ChibiStego.class, EntityDataSerializers.INT);
    public static final int NATURAL = 0, CYBER = 1, MAGMA = 2, GOLD = 3;
    private static final ResourceLocation SALAD_ID = new ResourceLocation("ror", "redwood_salad");
    private static final int SALAD_XP = 25;
    private static final int EVOLVE_LEVEL = 30;
    private static final UUID M_HP = UUID.fromString("c41b1000-0001-4d1e-9a10-5e90a5a10001");
    private static final UUID M_DMG = UUID.fromString("c41b1000-0002-4d1e-9a10-5e90a5a10002");
    private static final UUID M_ARM = UUID.fromString("c41b1000-0003-4d1e-9a10-5e90a5a10003");

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop("animation.chibi_stegosaurus." + name);
    }
    private static RawAnimation play(String name) {
        return RawAnimation.begin().thenPlay("animation.chibi_stegosaurus." + name);
    }

    private static final RawAnimation IDLE = loop("idle");
    private static final RawAnimation WALK = loop("walk");
    private static final RawAnimation RUN = loop("run");
    private static final RawAnimation LOOK = play("look_around");
    private static final RawAnimation SNIFF = play("sniff");
    private static final RawAnimation STARTLE = play("startle");
    private static final RawAnimation STOMP = play("stomp");
    private static final RawAnimation YAWN = play("yawn");
    private static final RawAnimation TAIL_SWIPE = play("tail_swipe");
    private static final RawAnimation SLEEP = loop("sleep");
    private static final RawAnimation HAPPY = play("happy");

    /** Velocidad del suelo (bloques/tick) para la que estan escritos walk y run: 11,4 u/s y 21,3 u/s a 16 u/bloque. */
    private static final float WALK_SPEED = 0.0357F, RUN_SPEED = 0.0666F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ProdigyProgress progress = ProdigyProgress.atLevel(1);
    // Solo cliente: velocidad medida por desplazamiento real (la de GeckoLib se confunde a velocidades bajas)
    private float movedPerTick = 0.0F;
    private boolean walking = false;
    private int evolveNagTick = -1000;

    public ChibiStego(EntityType<? extends ChibiStego> type, Level world) {
        super(type, world);
        setMaxUpStep(0.65F);
    }

    public static AttributeSupplier.Builder attributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.18D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
            .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, NATURAL);
    }

    public int variant() { return entityData.get(VARIANT); }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.max(NATURAL, Math.min(GOLD, v)));
    }

    @Override public ProdigyProgress getProgress() { return progress; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChibiVariant", variant());
        tag.put("ProdigyProgress", progress.save());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChibiVariant")) setVariant(tag.getInt("ChibiVariant"));
        // Chibis guardados antes de este sistema: nivel 1.
        progress = ProdigyProgress.load(tag.contains("ProdigyProgress") ? tag.getCompound("ProdigyProgress") : null, 1);
        applyStats();
    }

    @Nullable @Override public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason,
            @Nullable SpawnGroupData old, @Nullable CompoundTag nbt) {
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, old, nbt);
        int chance = random.nextInt(100);
        setVariant(chance < 72 ? NATURAL : chance < 85 ? CYBER : chance < 95 ? MAGMA : GOLD);
        applyStats();
        return result;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true) {
            @Override public boolean canUse() { return progress.level() >= 10 && !isOrderedToSit() && super.canUse(); }
        });
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1D, 6.0F, 2.0F, false));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.75));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    private boolean isSalad(ItemStack stack) {
        Item salad = BuiltInRegistries.ITEM.get(SALAD_ID);
        return salad != Items.AIR && stack.is(salad);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSalad(stack)) {
            if (!level().isClientSide) {
                if (!isTame()) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    tame(player);
                    setOrderedToSit(false);
                    level().broadcastEntityEvent(this, (byte) 7);
                    heal(getMaxHealth());
                } else if (isOwnedBy(player)) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    heal(10.0F);
                    gainXp(SALAD_XP);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else return InteractionResult.PASS;
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.WHEAT) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                heal(5.0F);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (isTame() && isOwnedBy(player) && stack.isEmpty()) {
            if (!level().isClientSide) {
                if (player.isShiftKeyDown()) {
                    int need = progress.xpToNext();
                    player.displayClientMessage(Component.literal("Chibi nivel " + progress.level() + "  XP " + progress.xp() + "/" + need
                        + "  (evoluciona a adulto en el nivel " + EVOLVE_LEVEL + ")"), true);
                } else {
                    setOrderedToSit(!isOrderedToSit());
                    getNavigation().stop();
                    setTarget(null);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.SADDLE)) return InteractionResult.PASS;
        return super.mobInteract(player, hand);
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override protected boolean canAddPassenger(Entity passenger) { return false; }

    // ------------------------------------------------------------ progresion y evolucion

    private void setMod(Attribute attr, UUID id, double amount) {
        AttributeInstance inst = getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, "prodigy_progress", amount, AttributeModifier.Operation.ADDITION));
    }

    public void applyStats() {
        if (level().isClientSide) return;
        int lv = progress.level();
        setMod(Attributes.MAX_HEALTH, M_HP, GrowthTable.HEALTH[lv] - 24.0);
        setMod(Attributes.ATTACK_DAMAGE, M_DMG, GrowthTable.DAMAGE[lv]);
        setMod(Attributes.ARMOR, M_ARM, GrowthTable.ARMOR[lv]);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    @Override public void gainXp(int amount) {
        if (level().isClientSide || amount <= 0) return;
        float oldMax = getMaxHealth();
        if (progress.addXp(amount)) {
            applyStats();
            float diff = getMaxHealth() - oldMax;
            if (diff > 0) heal(diff);
            if (getOwner() instanceof Player p) {
                p.displayClientMessage(Component.literal("Tu chibi sube a nivel " + progress.level()), true);
            }
            playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);
        }
    }

    private int adultVariant() {
        return switch (variant()) {
            case CYBER -> StegosaurusEntity.CYBERNETIC;
            case MAGMA -> StegosaurusEntity.MAGMA;
            case GOLD -> StegosaurusEntity.GOLDEN;
            default -> StegosaurusEntity.NATURAL;
        };
    }

    /** Nivel 30: se convierte en adulto en el mismo sitio si hay espacio. */
    private void tryEvolve() {
        if (!(level() instanceof ServerLevel server) || !isAlive() || !isTame() || progress.level() < EVOLVE_LEVEL) return;
        StegosaurusEntity adult = ProdigyCreatures.STEGOSAURUS.get().create(server);
        if (adult == null) return;
        adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        if (!server.noCollision(adult, adult.getBoundingBox())) {
            if (tickCount - evolveNagTick > 200 && getOwner() instanceof Player p) {
                evolveNagTick = tickCount;
                p.displayClientMessage(Component.literal("Tu chibi esta listo para evolucionar: necesita mas espacio libre"), true);
            }
            adult.discard();
            return;
        }
        adult.setVariant(adultVariant());
        adult.setOwnerUUID(getOwnerUUID());
        adult.setTame(true);
        adult.setOrderedToSit(isOrderedToSit());
        if (hasCustomName()) adult.setCustomName(getCustomName());
        adult.adoptProgress(progress);
        adult.setHealth(adult.getMaxHealth());
        server.addFreshEntity(adult);
        adult.startEvolution(); // silueta blanca que crece, destello y rugido (ver StegosaurusEntity.evolveTick)
        if (getOwner() instanceof Player p) p.displayClientMessage(Component.literal("Tu chibi evoluciono a Estegosaurio adulto"), false);
        discard();
    }

    @Nullable @Override protected SoundEvent getAmbientSound() { return ModSounds.CHIBI_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return ModSounds.CHIBI_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.CHIBI_DEATH.get(); }
    @Override public int getAmbientSoundInterval() { return 160; }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            double dx = getX() - xo, dz = getZ() - zo;
            float v = (float) Math.sqrt(dx * dx + dz * dz);
            movedPerTick += (v - movedPerTick) * 0.3F;
            walking = walking ? movedPerTick > 0.006F : movedPerTick > 0.012F; // histeresis
        } else if (tickCount % 20 == 0) {
            tryEvolve();
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            // Se camina segun el desplazamiento REAL medido, no state.isMoving(): a velocidad baja
            // limbSwingAmount queda bajo el umbral y se reproducia idle mientras la entidad avanzaba (deslizamiento).
            if (walking && !isOrderedToSit()) {
                boolean run = movedPerTick > 0.052F;
                double ref = run ? RUN_SPEED : WALK_SPEED;
                state.getController().setAnimationSpeed(Mth.clamp(movedPerTick / ref, 0.6D, 1.8D));
                return state.setAndContinue(run ? RUN : WALK);
            }
            state.getController().setAnimationSpeed(1.0D);
            if (isOrderedToSit()) return state.setAndContinue(SLEEP);
            if (hurtTime > 0) return state.setAndContinue(STARTLE);

            int phase = Math.floorMod(tickCount + getId() * 37, 600);
            if (variant() == GOLD) {
                if (phase >= 190 && phase < 325) return state.setAndContinue(SLEEP);
                if (phase >= 175 && phase < 190) return state.setAndContinue(YAWN);
            }
            if (variant() == MAGMA) {
                if (phase >= 120 && phase < 145) return state.setAndContinue(STOMP);
                if (phase >= 360 && phase < 386) return state.setAndContinue(TAIL_SWIPE);
            }
            if (variant() == CYBER) {
                if (phase >= 100 && phase < 118) return state.setAndContinue(STARTLE);
                if (phase >= 315 && phase < 360) return state.setAndContinue(LOOK);
                if (phase >= 440 && phase < 480) return state.setAndContinue(SNIFF);
            }
            if (variant() == NATURAL) {
                if (phase >= 100 && phase < 150) return state.setAndContinue(LOOK);
                if (phase >= 290 && phase < 325) return state.setAndContinue(SNIFF);
                if (phase >= 475 && phase < 510) return state.setAndContinue(HAPPY);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
