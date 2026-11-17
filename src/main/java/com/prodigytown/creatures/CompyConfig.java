package com.prodigytown.creatures;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion del Compy (config/prodigycreatures-compy.toml): domesticacion y reglas de aparicion de las variantes raras.
 *  Donde y en que grupos aparece (3-8) se ajusta con los biome modifiers de data/prodigycreatures/forge/biome_modifier/compy_*.json. */
public final class CompyConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue TAME_FEEDS, SPAWN_PERCENT, ALBINO_PERCENT, ALBINO_STORM_PERCENT, MELANIC_NIGHT_PERCENT;
    public static final ForgeConfigSpec.DoubleValue BITE_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue HUNT_SMALL_PREY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("taming");
        TAME_FEEDS = b.comment("Carnes seguidas que necesita un compy salvaje para confiar en ti").defineInRange("feeds", 3, 1, 20);
        b.pop();
        b.push("spawning");
        SPAWN_PERCENT = b.comment("Porcentaje de intentos de aparicion natural que prosperan (100 = todos)").defineInRange("spawnAttemptPercent", 60, 1, 100);
        ALBINO_PERCENT = b.comment("Probabilidad (%) de que un compy que aparece sea albino").defineInRange("albinoPercent", 2, 0, 100);
        ALBINO_STORM_PERCENT = b.comment("Idem durante una tormenta con rayos (evento especial)").defineInRange("albinoStormPercent", 15, 0, 100);
        MELANIC_NIGHT_PERCENT = b.comment("Probabilidad (%) de que un compy que aparece de noche sea melanico").defineInRange("melanicNightPercent", 4, 0, 100);
        b.pop();
        b.push("behaviour");
        BITE_DAMAGE = b.comment("Dano de la mordida (puntos de vida)").defineInRange("biteDamage", 2.0, 0.5, 20.0);
        HUNT_SMALL_PREY = b.comment("Los compys salvajes cazan gallinas y conejos").define("huntSmallPrey", true);
        b.pop();
        SPEC = b.build();
    }

    private CompyConfig() {}

    public static int get(ForgeConfigSpec.IntValue v, int fallback) {
        try { return v.get(); } catch (RuntimeException e) { return fallback; }
    }
    public static double get(ForgeConfigSpec.DoubleValue v, double fallback) {
        try { return v.get(); } catch (RuntimeException e) { return fallback; }
    }
    public static boolean get(ForgeConfigSpec.BooleanValue v, boolean fallback) {
        try { return v.get(); } catch (RuntimeException e) { return fallback; }
    }
}
