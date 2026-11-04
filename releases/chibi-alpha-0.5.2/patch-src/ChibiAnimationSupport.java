package com.prodigytown.chibi;

/**
 * Movement-safe animation selection for Prodigy Chibi Alpha 0.5.2.
 * GeckoLib controller IDs:
 * 0 idle, 1 walk, 2 run, 3 look around, 4 sniff, 5 startle,
 * 6 stomp, 7 yawn, 8 tail swipe, 9 sleep, 10 happy.
 */
public final class ChibiAnimationSupport {
    private static final double MIN_MOVING_SPEED_SQUARED = 0.000225D;
    private ChibiAnimationSupport() {}

    public static int choose(int ticks, int variant, double speedSquared, int hurtTime, boolean moving) {
        // Locomotion wins over expressions: WALK animates the four legs.
        if (moving || speedSquared > MIN_MOVING_SPEED_SQUARED) return 1;
        if (hurtTime > 0) return variant == 2 ? 8 : 5;

        int phase = ticks + Math.floorMod(variant * 67, 311);
        switch (variant) {
            case 0: // Natural
                if (Math.floorMod(phase, 500) < 33) return 10;
                if (Math.floorMod(phase, 390) < 60) return 3;
                if (Math.floorMod(phase, 740) < 40) return 4;
                break;
            case 1: // Cyber
                if (Math.floorMod(phase, 850) < 30) return 5;
                if (Math.floorMod(phase, 390) < 40) return 4;
                if (Math.floorMod(phase, 550) < 60) return 3;
                break;
            case 2: // Magma
                if (Math.floorMod(phase, 450) < 36) return 6;
                if (Math.floorMod(phase, 830) < 23) return 8;
                if (Math.floorMod(phase, 670) < 40) return 4;
                break;
            case 3: // Gold
                if (Math.floorMod(phase, 1120) < 130) return 9;
                if (Math.floorMod(phase, 550) < 60) return 7;
                if (Math.floorMod(phase, 680) < 60) return 3;
                break;
            default: break;
        }
        return 0;
    }
}
