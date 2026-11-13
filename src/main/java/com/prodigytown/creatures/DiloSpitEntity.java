package com.prodigytown.creatures;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Escupitajo venenoso del dilofosaurio: proyectil propio (basado en el de la llama) que hace dano y envenena; no da a su duenio ni a sus aliados. */
public class DiloSpitEntity extends LlamaSpit {
    public float damage = 2.0F;
    public int poisonTicks = 60;

    public DiloSpitEntity(EntityType<? extends DiloSpitEntity> type, Level level) { super(type, level); }

    @Override public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.ITEM_SLIME, this.getX(), this.getY() + 0.05, this.getZ(), 0.0, 0.0, 0.0);
            if (this.tickCount % 2 == 0) this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.tickCount > 80) this.discard();
    }

    @Override protected boolean canHitEntity(net.minecraft.world.entity.Entity e) {
        return super.canHitEntity(e) && this.getOwner() instanceof DilophosaurusEntity d && e instanceof LivingEntity le && d.canHitPublic(le);
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (this.level().isClientSide) return;
        if (this.getOwner() instanceof DilophosaurusEntity owner && hit.getEntity() instanceof LivingEntity victim) {
            victim.hurt(this.damageSources().indirectMagic(this, owner), damage);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
        }
        this.discard();
    }
}
