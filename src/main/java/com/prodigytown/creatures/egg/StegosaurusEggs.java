package com.prodigytown.creatures.egg;

import com.prodigytown.chibi.ChibiStego;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.util.RandomSource;

/** Reglas de los huevos: nombres, probabilidad de raza (con sesgo de la madre) y raza del chibi que nace. */
public final class StegosaurusEggs {
    private StegosaurusEggs() {}

    /** Indice = variante adulta (0 cibernetico, 1 dorado, 2 magma, 3 glacial, 4 natural). */
    public static final String[] NAMES = {"cybernetic", "golden", "magma", "glacial", "natural"};
    /** Mezcla base (%) por variante adulta: cibernetico 18, dorado 15, magma 12, esmeralda 5, natural 50. */
    private static final int[] BASE = {18, 15, 12, 5, 50};

    /** Raza del huevo que suelta una errante: mezcla fija 50/18/15/12/5, sin herencia. */
    public static int rollVariant(RandomSource rnd) {
        int r = rnd.nextInt(100), acc = 0;
        for (int i = 0; i < 5; i++) { acc += BASE[i]; if (r < acc) return i; }
        return 4;
    }

    /** Huevo de un nido silvestre: casi siempre el del tipo de nido; ~1 % sale otra raza (la rareza es encontrarlo). */
    public static int nestEgg(RandomSource rnd, int nestVariant) {
        return rnd.nextInt(100) == 0 ? rollVariant(rnd) : nestVariant;
    }

    /** Raza de un adulto silvestre: bonus = raza propia del bioma (20 %), -1 si el bioma solo da natural; sin esmeralda salvo bioma esmeralda. */
    public static int rollWild(RandomSource rnd, int biomeVariant, boolean naturalOnly) {
        if (naturalOnly) return 4;
        if (biomeVariant >= 0 && rnd.nextInt(100) < 20) return biomeVariant;
        int r = rnd.nextInt(95); // 18+15+12+50, sin esmeralda
        if (r < 18) return 0;
        if (r < 33) return 1;
        if (r < 45) return 2;
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
