package com.prodigytown.creatures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Ordenes y estado "caido" compartidos por el adulto y el chibi.
 * Ordenes: 0 seguir, 1 quieto, 2 deambular (arena de 3 chunks), 3 patrullar, 4 sentarse, 5 dormir (cura mas rapido, con Zzz).
 * Caido: en vez de morir, la mascota domada queda dormida en el suelo, invulnerable, hasta que su dueno la reanima con la ensalada de domesticacion.
 */
public final class PetOrders {
    private PetOrders() {}
    public static final int FOLLOW = 0, STAY = 1, WANDER = 2, PATROL = 3, SIT = 4, SLEEP = 5, COUNT = 6;
    public static final String[] NAMES = {"Seguir", "Quieto", "Deambular", "Patrullar", "Sentarse", "Dormir"};
    public static final String[] HELP = {
        "Tu dinosaurio te seguira a donde vayas.",
        "Tu dinosaurio permanecera en el lugar actual hasta que le des otra orden.",
        "Paseara libremente por la zona, sin alejarse mas de 3 chunks de donde se lo ordenaste.",
        "Recorrera la zona en ronda y atacara a los monstruos que se acerquen.",
        "Se sentara y esperara.",
        "Dormira profundamente: recupera vida mucho mas rapido."};
    /** Radio (bloques) de la arena de deambular y patrullar: 3 chunks a cada lado. */
    public static final int WANDER_RADIUS = 48;
    /** Por debajo de esta fraccion de vida el chibi se asusta: deja de atacar, huye de monstruos y te sigue muy cerca. */
    public static final float WEAK_FRACTION = 0.30F;
    public static final float REVIVE_HEALTH = 20.0F;

    /** Lo implementan StegosaurusEntity y ChibiStego (datos sincronizados + guardado). */
    public interface Holder {
        int getOrder();
        void setOrderRaw(int order);
        boolean isFallen();
        void setFallenRaw(boolean fallen);
        @Nullable BlockPos orderHome();
        void setOrderHome(@Nullable BlockPos pos);
    }

    public static int clamp(int order) { return Math.max(0, Math.min(COUNT - 1, order)); }
    public static boolean isSittingOrder(int order) { return order == SIT || order == SLEEP; }

    /** Solo servidor. */
    public static void setOrder(TamableAnimal mob, int order) {
        if (mob.level().isClientSide || !(mob instanceof Holder h) || h.isFallen()) return;
        order = clamp(order);
        h.setOrderRaw(order);
        boolean sit = isSittingOrder(order);
        mob.setOrderedToSit(sit);
        if (sit) mob.ejectPassengers();
        mob.getNavigation().stop();
        mob.setTarget(null);
        if (order == WANDER || order == PATROL) {
            h.setOrderHome(mob.blockPosition());
            mob.restrictTo(h.orderHome(), WANDER_RADIUS);
        } else {
            h.setOrderHome(null);
            mob.clearRestriction();
        }
    }

    /** Al transformarse (adulto <-> chibi) conserva la orden y la zona. */
    public static void copy(TamableAnimal from, TamableAnimal to) {
        if (!(from instanceof Holder a) || !(to instanceof Holder b)) return;
        b.setOrderRaw(a.getOrder());
        b.setOrderHome(a.orderHome());
        to.setOrderedToSit(isSittingOrder(a.getOrder()));
        if ((a.getOrder() == WANDER || a.getOrder() == PATROL) && a.orderHome() != null) to.restrictTo(a.orderHome(), WANDER_RADIUS);
    }

    public static boolean weak(TamableAnimal mob) {
        return mob.isTame() && mob.getHealth() < mob.getMaxHealth() * WEAK_FRACTION;
    }

    /** Solo servidor, una vez por tick. */
    public static void serverTick(TamableAnimal mob) {
        if (!(mob instanceof Holder h) || !(mob.level() instanceof ServerLevel level)) return;
        int order = h.getOrder();
        if ((order == WANDER || order == PATROL) && mob.tickCount % 40 == 0) {
            if (h.orderHome() == null) h.setOrderHome(mob.blockPosition());
            if (!mob.hasRestriction()) mob.restrictTo(h.orderHome(), WANDER_RADIUS);
        }
        double yaw = Math.toRadians(mob.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        if (h.isFallen()) {
            if (!mob.isOrderedToSit()) mob.setOrderedToSit(true);
            if (mob.getTarget() != null) mob.setTarget(null);
            if (mob.getHealth() > 1.0F) mob.setHealth(1.0F);
            if (mob.tickCount % 50 == 0) {
                level.sendParticles(ModParticles.ZZZ.get(), mob.getX() + fx * mob.getBbWidth() * 0.55, mob.getY() + mob.getBbHeight() * 0.7,
                    mob.getZ() + fz * mob.getBbWidth() * 0.55, 1, 0.1, 0.05, 0.1, 0.0);
            }
            return;
        }
        if (order == SLEEP) {
            if (mob.tickCount % 20 == 0 && mob.getHealth() < mob.getMaxHealth()) mob.heal(Math.max(1.0F, mob.getMaxHealth() * 0.02F));
            if (mob.tickCount % 30 == 0) {
                level.sendParticles(ModParticles.ZZZ.get(), mob.getX() + fx * mob.getBbWidth() * 0.55, mob.getY() + mob.getBbHeight() * 0.7,
                    mob.getZ() + fz * mob.getBbWidth() * 0.55, 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
    }

    /** En vez de morir: queda inconsciente con 1 de vida hasta que lo reanimen. Solo servidor. */
    public static void enterFallen(TamableAnimal mob) {
        if (!(mob instanceof Holder h) || mob.level().isClientSide) return;
        h.setFallenRaw(true);
        h.setOrderRaw(SLEEP);
        h.setOrderHome(null);
        mob.clearRestriction();
        mob.setHealth(1.0F);
        mob.ejectPassengers();
        mob.clearFire();
        mob.removeAllEffects();
        mob.setOrderedToSit(true);
        mob.getNavigation().stop();
        mob.setTarget(null);
        if (mob.getOwner() instanceof Player p) {
            p.displayClientMessage(Component.literal("Tu dinosaurio quedo fuera de combate: reanimalo con la ensalada de domesticacion"), false);
        }
    }

    /** Solo servidor. */
    public static void revive(TamableAnimal mob) {
        if (!(mob instanceof Holder h) || mob.level().isClientSide || !h.isFallen()) return;
        h.setFallenRaw(false);
        h.setOrderRaw(FOLLOW);
        mob.setOrderedToSit(false);
        mob.setHealth(Math.min(REVIVE_HEALTH, mob.getMaxHealth()));
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() * 0.8, mob.getZ(), 8, mob.getBbWidth() * 0.4, 0.4, mob.getBbWidth() * 0.4, 0.02);
        }
    }

    // ------------------------------------------------------------------ metas de IA

    /** Ronda alrededor del punto donde se dio la orden. */
    public static final class PatrolGoal extends Goal {
        private final PathfinderMob mob;
        private final Holder holder;
        private double angle;
        public PatrolGoal(PathfinderMob mob) {
            this.mob = mob; this.holder = (Holder) mob;
            this.angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }
        @Override public boolean canUse() {
            return holder.getOrder() == PATROL && !holder.isFallen() && holder.orderHome() != null
                && !((TamableAnimal) mob).isOrderedToSit() && mob.getTarget() == null && mob.getRandom().nextInt(20) == 0;
        }
        @Override public boolean canContinueToUse() {
            return holder.getOrder() == PATROL && !holder.isFallen() && mob.getTarget() == null && !mob.getNavigation().isDone();
        }
        @Override public void start() {
            BlockPos home = holder.orderHome();
            angle += 1.0 + mob.getRandom().nextDouble() * 0.6;
            double r = 8.0 + mob.getRandom().nextDouble() * 10.0;
            int x = (int) Math.floor(home.getX() + 0.5 + Math.cos(angle) * r);
            int z = (int) Math.floor(home.getZ() + 0.5 + Math.sin(angle) * r);
            int y = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).getY();
            mob.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.0);
        }
        @Override public void stop() { mob.getNavigation().stop(); }
    }

    /** Durante la ronda ataca a los monstruos cercanos. */
    public static Goal patrolTargeting(PathfinderMob mob) {
        return new NearestAttackableTargetGoal<Monster>(mob, Monster.class, 10, true, false, e -> true) {
            @Override public boolean canUse() {
                return ((Holder) mob).getOrder() == PATROL && !((Holder) mob).isFallen() && super.canUse();
            }
        };
    }

    /** Chibi asustado: pegado al dueno. */
    public static final class CloseFollowGoal extends Goal {
        private final TamableAnimal mob; private int recalc;
        public CloseFollowGoal(TamableAnimal mob) { this.mob = mob; this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); }
        private boolean active(double min) {
            LivingEntity o = mob.getOwner();
            return o != null && !o.isSpectator() && weak(mob) && ((Holder) mob).getOrder() == FOLLOW && !((Holder) mob).isFallen()
                && !mob.isOrderedToSit() && mob.distanceToSqr(o) > min * min;
        }
        @Override public boolean canUse() { return active(2.5); }
        @Override public boolean canContinueToUse() { return active(1.8); }
        @Override public void start() { recalc = 0; }
        @Override public void stop() { mob.getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity o = mob.getOwner();
            if (o == null) return;
            mob.getLookControl().setLookAt(o, 10.0F, mob.getMaxHeadXRot());
            if (--recalc > 0) return;
            recalc = 6;
            if (mob.distanceToSqr(o) > 400.0 && mob.level() instanceof ServerLevel) mob.teleportTo(o.getX(), o.getY(), o.getZ());
            else mob.getNavigation().moveTo(o, 1.35);
        }
    }

    /** Chibi asustado: huye de los monstruos. */
    public static final class FleeGoal extends AvoidEntityGoal<Monster> {
        private final TamableAnimal pet;
        public FleeGoal(TamableAnimal mob) { super(mob, Monster.class, 10.0F, 1.2D, 1.5D); this.pet = mob; }
        @Override public boolean canUse() { return weak(pet) && !((Holder) pet).isFallen() && super.canUse(); }
    }
}
