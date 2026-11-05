package com.prodigytown.creatures.egg;

import com.prodigytown.chibi.ChibiStego;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.util.RandomSource;

/** Reglas de los huevos: nombres, probabilidad de raza (con sesgo de la madre) y raza del chibi que nace. */
public final class StegosaurusEggs {
    private StegosaurusEggs() {}

    /** Indice = variante adulta (0 cibernetico, 1 dorado, 2 magma, 3 glacial, 4 natural). */
    public static final String[] NAMES = {"cybernetic", "golden", "magma", "glacial", "natural"};
    /** Probabilidad base (%) de cada raza en un huevo sin madre conocida: natural 50, cibernetico 20, magma 20, albino dorado 10. */
    private static final int[] BASE = {20, 10, 20, 0, 50};
    /** Parte de la probabilidad que se reserva a la raza de la madre; el resto se reparte segun BASE. */
    private static final double MOTHER_SHARE = 0.5;

    /** Raza del huevo que suelta una hembra de la raza dada. */
    public static int rollVariant(RandomSource rnd, int motherVariant) {
        int m = Math.max(0, Math.min(4, motherVariant));
        double[] w = new double[5];
        for (int i = 0; i < 5; i++) w[i] = BASE[i] / 100.0;
        // la madre se queda con MOTHER_SHARE y el resto se reparte entre todas segun BASE (la madre tambien participa)
        for (int i = 0; i < 5; i++) w[i] *= (1.0 - MOTHER_SHARE);
        w[m] += MOTHER_SHARE;
        double r = rnd.nextDouble(), acc = 0.0;
        for (int i = 0; i < 5; i++) { acc += w[i]; if (r < acc) return i; }
        return 4;
    }

    /** Configura un chibi recien nacido segun la variante adulta del huevo. */
    public static void applyToChibi(ChibiStego chibi, int adultVariant) {
        switch (adultVariant) {
            case StegosaurusEntity.CYBERNETIC -> chibi.initFromEgg(ChibiStego.CYBER, false);
            case StegosaurusEntity.GOLDEN -> chibi.initFromEgg(ChibiStego.GOLD, false);
            case StegosaurusEntity.MAGMA -> chibi.initFromEgg(ChibiStego.MAGMA, false);
            case StegosaurusEntity.GLACIAL -> chibi.initFromEgg(ChibiStego.NATURAL, true); // no hay chibi glacial: crece como adulto glacial
            default -> chibi.initFromEgg(ChibiStego.NATURAL, false);
        }
    }
}
