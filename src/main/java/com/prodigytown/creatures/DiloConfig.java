package com.prodigytown.creatures;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion del dilofosaurio (config/prodigycreatures-dilophosaurus.toml): equilibrio de poderes y domesticacion. */
public final class DiloConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FRENZY_SECONDS, FRENZY_COOLDOWN_SECONDS, FRENZY_DAMAGE_PERCENT, FRENZY_SPEED_PERCENT, FRENZY_INTIMIDATE_RADIUS;
    public static final ForgeConfigSpec.IntValue BITE_COOLDOWN_TICKS, CLAW_COOLDOWN_SECONDS, POUNCE_COOLDOWN_SECONDS, SPIT_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BITE_DAMAGE, CLAW_DAMAGE, POUNCE_DAMAGE, SPIT_DAMAGE;
    public static final ForgeConfigSpec.IntValue TAME_FEEDS_ADULT, TAME_FEEDS_CHIBI, FEEDS_TO_GROW;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("frenzy");
        FRENZY_SECONDS = b.comment("Duracion del Frenesi Jurasico (segundos)").defineInRange("durationSeconds", 10, 1, 120);
        FRENZY_COOLDOWN_SECONDS = b.comment("Recarga del Frenesi Jurasico (segundos)").defineInRange("cooldownSeconds", 45, 5, 600);
        FRENZY_DAMAGE_PERCENT = b.comment("Bonus de dano durante el Frenesi (%)").defineInRange("damageBonusPercent", 30, 0, 300);
        FRENZY_SPEED_PERCENT = b.comment("Bonus de velocidad durante el Frenesi (%)").defineInRange("speedBonusPercent", 25, 0, 200);
        FRENZY_INTIMIDATE_RADIUS = b.comment("Radio de la intimidacion al activarlo (bloques)").defineInRange("intimidateRadius", 7, 0, 32);
        b.pop();
        b.push("abilities");
        BITE_DAMAGE = b.comment("Dano de la Mordida rapida, como multiplo del ataque").defineInRange("biteDamage", 1.0, 0.1, 10.0);
        CLAW_DAMAGE = b.comment("Dano de cada zarpazo del Zarpazo doble, como multiplo del ataque").defineInRange("clawDamage", 0.75, 0.1, 10.0);
        POUNCE_DAMAGE = b.comment("Dano del Salto depredador (contacto e impacto), como multiplo del ataque").defineInRange("pounceDamage", 1.1, 0.1, 10.0);
        BITE_COOLDOWN_TICKS = b.comment("Recarga de la Mordida rapida (ticks)").defineInRange("biteCooldownTicks", 14, 2, 200);
        CLAW_COOLDOWN_SECONDS = b.comment("Recarga del Zarpazo doble (segundos)").defineInRange("clawCooldownSeconds", 4, 1, 120);
        POUNCE_COOLDOWN_SECONDS = b.comment("Recarga del Salto depredador (segundos)").defineInRange("pounceCooldownSeconds", 9, 1, 120);
        SPIT_COOLDOWN_SECONDS = b.comment("Recarga del Escupitajo venenoso (segundos)").defineInRange("spitCooldownSeconds", 8, 1, 120);
        SPIT_DAMAGE = b.comment("Dano directo del Escupitajo venenoso, como multiplo del ataque (ademas aplica veneno)").defineInRange("spitDamage", 0.5, 0.0, 10.0);
        b.pop();
        b.push("taming");
        TAME_FEEDS_ADULT = b.comment("Carnes seguidas que necesita un dilofosaurio adulto salvaje para confiar en ti").defineInRange("feedsAdult", 4, 1, 20);
        TAME_FEEDS_CHIBI = b.comment("Idem para una cria salvaje").defineInRange("feedsChibi", 2, 1, 20);
        FEEDS_TO_GROW = b.comment("Carnes crudas que necesita una cria domada para evolucionar a adulto").defineInRange("feedsToGrow", 6, 1, 64);
        b.pop();
        SPEC = b.build();
    }

    private DiloConfig() {}

    /** Lee un valor entero con respaldo (por si se consulta antes de cargar la configuracion). */
    public static int get(ForgeConfigSpec.IntValue v, int fallback) {
        try { return v.get(); } catch (RuntimeException e) { return fallback; }
    }
    public static double get(ForgeConfigSpec.DoubleValue v, double fallback) {
        try { return v.get(); } catch (RuntimeException e) { return fallback; }
    }
}
