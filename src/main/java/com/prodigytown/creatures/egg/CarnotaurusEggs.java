package com.prodigytown.creatures.egg;

import net.minecraft.util.RandomSource;

/** Reglas de los huevos de carnotauro: nombres, rareza de cada color y estado que ocupan en el nido. */
public final class CarnotaurusEggs {
    private CarnotaurusEggs() {}

    /** Indice = color (CarnotaurusEntity.SCARLET, GREEN, STEEL, ARENA). */
    public static final String[] NAMES = {"scarlet", "green", "steel", "arena"};
    /** Estados del nido: 1..5 huevos de estegosaurio (variante + 1); 6..9 huevos de carnotauro (6 + color). */
    public static final int NEST_BASE = 6;
    /** Rareza (%) de cada color: rojo 50, acero 22, verde 18, arena 10 (el mas raro). */
    private static final int[] BASE = {50, 18, 22, 10};
    /** Puntos de bonus que suma el color de la madre. */
    private static final int HERITAGE = 15;

    /** Color del huevo que deja una hembra silvestre: mezcla por rareza con sesgo hacia su propio color. */
    public static int rollVariant(RandomSource rnd, int mother) {
        double total = 0.0;
        double[] w = new double[BASE.length];
        for (int i = 0; i < w.length; i++) { w[i] = BASE[i]; }
        if (mother >= 0 && mother < w.length) w[mother] += HERITAGE;
        for (double d : w) total += d;
        double r = rnd.nextDouble() * total, acc = 0.0;
        for (int i = 0; i < w.length; i++) { acc += w[i]; if (r < acc) return i; }
        return 0;
    }
}
