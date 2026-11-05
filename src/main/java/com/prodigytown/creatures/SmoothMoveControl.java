package com.prodigytown.creatures;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Giro amplio: el MoveControl de vanilla gira hasta 90 grados por tick (el animal pivota sobre su eje).
 * Aqui el rumbo cambia como mucho {@code maxTurn} grados por tick y, mientras queda mucho angulo por girar,
 * avanza mas despacio: el resultado es un arco, como un animal grande que gira caminando.
 */
public class SmoothMoveControl extends MoveControl {
    private final float maxTurn;
    public SmoothMoveControl(Mob mob, float maxTurn) {
        super(mob);
        this.maxTurn = maxTurn;
    }
    @Override public void tick() {
        float before = this.mob.getYRot();
        super.tick();
        float wanted = this.mob.getYRot();
        float limited = this.rotlerp(before, wanted, this.maxTurn);
        this.mob.setYRot(limited);
        float left = Math.abs(Mth.wrapDegrees(wanted - limited));
        if (left > 12.0F) {
            // cuanto mas lejos esta el rumbo deseado, mas despacio avanza (minimo 30 %)
            this.mob.setSpeed(this.mob.getSpeed() * Mth.clamp(1.0F - (left - 12.0F) / 90.0F, 0.3F, 1.0F));
        }
    }
}
