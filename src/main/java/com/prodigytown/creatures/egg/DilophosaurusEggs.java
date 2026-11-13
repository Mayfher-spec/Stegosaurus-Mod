package com.prodigytown.creatures.egg;

import net.minecraft.util.RandomSource;

/** Reglas de los huevos de dilofosaurio: nombres, rareza de cada variante y estado que ocupan en el nido (10..14). */
public final class DilophosaurusEggs {
    private DilophosaurusEggs() {}

    /** Indice = variante (DilophosaurusEntity.NATURAL, ELECTRIC, MAGMA, DESERT, SPECTRAL). */
    public static final String[] NAMES = {"natural", "electric", "magma", "desert", "spectral"};
    /** Estados del nido: 1..5 estegosaurio, 6..9 carnotauro, 10..14 dilofosaurio (10 + variante). */
    public static final int NEST_BASE = 10;
    private static final int[] BASE = {50, 18, 12, 15, 5};
    /** Puntos de bonus que suma la variante de la madre (la herencia). */
    private static final int HERITAGE = 20;

    /** Variante del huevo que deja una hembra: mezcla por rareza con sesgo hacia la suya (asi las raras son raras de verdad). */
    public static int rollVariant(RandomSource rnd, int mother) {
        double[] w = new double[BASE.length];
        double total = 0.0;
        for (int i = 0; i < w.length; i++) w[i] = BASE[i];
        if (mother >= 0 && mother < w.length) w[mother] += HERITAGE;
        for (double d : w) total += d;
        double r = rnd.nextDouble() * total, acc = 0.0;
        for (int i = 0; i < w.length; i++) { acc += w[i]; if (r < acc) return i; }
        return 0;
    }
}
