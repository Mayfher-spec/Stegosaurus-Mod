package com.prodigytown.creatures.client;

import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.DiloConfig;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.progress.ProdigyProgress;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Datos de especie que usan la pantalla unificada (PetScreen) y el HUD unificado (PetHud): nombre, habilidades, ramas del arbol,
 * iconos de talento y acceso comun al estado de la criatura. Asi las tres criaturas se ven y se comportan igual en la interfaz.
 */
final class PetSpec {
    /** Habilidad: cd = indice de recarga de la entidad (-1 = pasiva). hudKey es la etiqueta corta para el HUD. */
    record Ability(String name, String desc, Item icon, Supplier<String> key, int cd, BooleanSupplier locked, String lockedText, Supplier<String[]> stats) {
        Ability(String name, String desc, Item icon, Supplier<String> key, int cd, BooleanSupplier locked, String lockedText) {
            this(name, desc, icon, key, cd, locked, lockedText, null);
        }
        String hudKey() {
            String k = key.get();
            if (k.startsWith("Clic i")) return "Izq";
            if (k.startsWith("Clic d")) return "Der";
            return k.length() > 3 ? k.substring(0, 3) : k;
        }
    }

    final TamableAnimal pet;
    final String species;
    final String subtitle;
    final String[] branchNames;
    final String finalName;
    final int accent;
    final String reviveHint;
    final List<Ability> abilities = new ArrayList<>();

    private static PetSpec cached;

    private PetSpec(TamableAnimal pet, String species, String[] branchNames, String finalName, int accent, String reviveHint) {
        this.pet = pet; this.species = species; this.branchNames = branchNames; this.finalName = finalName; this.accent = accent;
        this.reviveHint = reviveHint;
        this.subtitle = "Sistema de compañero domesticable";
    }

    /** null si la entidad no es una de las tres criaturas con pantalla propia (el chibi estegosaurio usa OrdersScreen). */
    static PetSpec of(TamableAnimal t) {
        if (t == null) return null;
        if (cached != null && cached.pet == t) return cached;
        PetSpec s;
        if (t instanceof StegosaurusEntity st) s = stego(st);
        else if (t instanceof CarnotaurusEntity c) s = carno(c);
        else if (t instanceof DilophosaurusEntity d) s = dilo(d);
        else return null;
        cached = s;
        return s;
    }

    private static String k(net.minecraft.client.KeyMapping m) { return m.getTranslatedKeyMessage().getString(); }

    private static PetSpec stego(StegosaurusEntity e) {
        PetSpec s = new PetSpec(e, "Estegosaurio", new String[]{"Combate", "Defensa", "Soporte"}, "Definitiva", 0xFFE8A030,
            "ensalada de domesticación");
        s.abilities.add(new Ability("Embestida", "Carrera veloz hacia delante: arrolla y aturde a quien alcanza.", Items.IRON_BOOTS,
            () -> k(ClientKeys.CHARGE), 0, () -> e.getProgress().rank("C3") <= 0, "Talento"));
        s.abilities.add(new Ability("Mordida", "Montado, clic izquierdo: un mordisco rápido al enemigo que tienes delante.", Items.IRON_SWORD,
            () -> "Clic izq.", 2, () -> false, ""));
        s.abilities.add(new Ability("Coletazo", "Golpe de la cola espinada que empuja a los enemigos. Con el talento devastador se puede cargar.", Items.BONE,
            () -> "Clic der. / " + k(ClientKeys.TAIL), 1, () -> false, ""));
        s.abilities.add(new Ability("Rugido ancestral", "Definitiva: se obtiene por nivel (50, 75 y 100). Las mejoras de potencia, alcance y recarga la fortalecen.", Items.NETHER_STAR,
            () -> k(ClientKeys.ULTIMATE), 3, () -> e.ultimateLevel() <= 0, "Nv. 50"));
        return s;
    }

    private static PetSpec carno(CarnotaurusEntity e) {
        PetSpec s = new PetSpec(e, "Carnotauro", new String[]{"Combate", "Defensa", "Soporte"}, "Definitiva", 0xFFE0453A,
            "ensalada de domesticación");
        s.abilities.add(new Ability("Mordida feroz", "Cierra sus mandíbulas sobre el enemigo que tiene delante. Es su arma más rápida.", Items.IRON_SWORD,
            () -> "Clic izq.", 2, () -> false, "", () -> carnoStats(e, 0)));
        s.abilities.add(new Ability("Embestida brutal", "Se lanza hacia delante, golpea a todo el que toca y lo empuja con fuerza.", Items.IRON_BOOTS,
            () -> k(ClientKeys.CHARGE), 0, () -> false, "", () -> carnoStats(e, 1)));
        s.abilities.add(new Ability("Rugido aterrador", "Definitiva: aliados cercanos ganan Fuerza; los enemigos quedan lentos y débiles. Desde el nivel 50.", Items.NETHER_STAR,
            () -> k(ClientKeys.ULTIMATE), 3, () -> e.ultimateLevel() <= 0, "Nv. 50", () -> carnoStats(e, 2)));
        s.abilities.add(new Ability("Cabezazo demoledor", "Echa la cabeza atrás y embiste con los cuernos: daño fuerte y los enemigos salen disparados hacia arriba.", Items.ANVIL,
            () -> k(ClientKeys.TAIL), 1, () -> false, "", () -> carnoStats(e, 3)));
        s.abilities.add(new Ability("Instinto depredador", "Pasiva: ataca todo lo que ve, corre más rápido tras una presa y se enfurece con poca vida.", Items.ENDER_EYE,
            () -> "Pasiva", -1, () -> false, ""));
        return s;
    }

    private static String sec(double ticks) {
        double s = ticks / 20.0;
        return (s == Math.rint(s) ? String.valueOf((int) s) : String.format("%.1f", s).replace('.', ',')) + " s";
    }

    /** Estadisticas de cada ataque del carnotauro: 0 mordida, 1 embestida, 2 rugido, 3 cabezazo. */
    private static String[] carnoStats(CarnotaurusEntity e, int a) {
        ProdigyProgress p = e.getProgress();
        float dmg = (float) e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float f = 1.0F - 0.10F * p.rank("C7");
        switch (a) {
            case 0 -> { return new String[]{"Daño: " + Math.round(dmg), "Recarga: " + sec(Math.max(2, Math.round(12 * f))), "Tecla: clic izq."}; }
            case 1 -> { return new String[]{"Daño: " + Math.round(dmg * (1.4F + 0.2F * p.rank("C3"))), "Recarga: " + sec(Math.round(160 * f)), "Tecla: " + k(ClientKeys.CHARGE)}; }
            case 2 -> {
                int ul = e.ultimateLevel();
                int cd = ul > 0 ? 1200 - 20 * new int[]{0, 8, 14, 20}[Math.min(3, p.rank("U3"))] : 500;
                return new String[]{"Alcance: 12 m", "Recarga: " + sec(Math.round(cd * f)), "Tecla: " + k(ClientKeys.ULTIMATE)};
            }
            default -> { return new String[]{"Daño: " + Math.round(dmg * (1.3F + 0.5F * p.rank("C5"))), "Recarga: " + sec(Math.round(70 * (1.0F - 0.10F * p.rank("C5")) * f)), "Tecla: " + k(ClientKeys.TAIL)}; }
        }
    }

    // ---- definitiva / mejora final
    String ultName() { return pet instanceof DilophosaurusEntity ? "Cazador primordial" : "Rugido ancestral"; }
    String ultHeader() { return pet instanceof DilophosaurusEntity ? "Mejora final" : "Mejoras de la definitiva"; }
    String ultStatus() {
        if (pet instanceof StegosaurusEntity s) { int u = s.ultimateLevel(); return u > 0 ? "Definitiva nivel " + u + " / 3" : "Se obtiene al nivel 50"; }
        if (pet instanceof CarnotaurusEntity c) { int u = c.ultimateLevel(); return u > 0 ? "Definitiva nivel " + u + " / 3" : "Se obtiene al nivel 50"; }
        return progress().rank("P1") > 0 ? "Desbloqueado" : "Se obtiene al nivel 70";
    }
    boolean hasUltimateCard() { return !(pet instanceof DilophosaurusEntity); }

    private static final String[] DILO_DESC = {
        "Un mordisco rápido y preciso. El daño llega justo en el momento del impacto.",
        "Dos zarpazos seguidos en un arco corto frente a ti. No atraviesan paredes.",
        "Un gran salto hacia delante: empuja a los que alcanza y deja un impacto al aterrizar.",
        "Más daño y velocidad unos segundos; la membrana se despliega y un rugido intimida a los enemigos cercanos.",
        "Despliega la membrana y escupe veneno a distancia. Se desbloquea con el talento Glándula venenosa."};
    private static final Item[] DILO_ICON = {Items.IRON_SWORD, Items.FLINT, Items.RABBIT_FOOT, Items.BLAZE_POWDER, Items.SPIDER_EYE};

    private static PetSpec dilo(DilophosaurusEntity e) {
        PetSpec s = new PetSpec(e, "Dilofosaurio", new String[]{"Caza", "Movilidad", "Supervivencia"}, "Cazador primordial", 0xFF6FA84A,
            "carne de cordero dorada");
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            s.abilities.add(new Ability(DilophosaurusEntity.ABILITY_NAMES[i], DILO_DESC[i], DILO_ICON[i],
                () -> idx < 2 ? (idx == 0 ? "Clic izq. / " + k(ClientKeys.DILO_BITE) : "Clic der. / " + k(ClientKeys.DILO_CLAW))
                    : k(idx == 2 ? ClientKeys.DILO_POUNCE : idx == 3 ? ClientKeys.DILO_FRENZY : ClientKeys.DILO_SPIT),
                i, () -> idx == 4 ? e.getProgress().rank("S7") <= 0 : (!e.isAdultStage() && idx != 0), idx == 4 ? "Talento" : "Adulto"));
        }
        return s;
    }

    // ------------------------------------------------------------------ acceso comun al estado

    ProdigyProgress progress() {
        if (pet instanceof StegosaurusEntity s) return s.getProgress();
        if (pet instanceof CarnotaurusEntity c) return c.getProgress();
        return ((DilophosaurusEntity) pet).getProgress();
    }
    boolean adult() {
        if (pet instanceof CarnotaurusEntity c) return c.isAdultStage();
        if (pet instanceof DilophosaurusEntity d) return d.isAdultStage();
        return true;
    }
    boolean saddled() {
        if (pet instanceof StegosaurusEntity s) return s.isSaddled();
        if (pet instanceof CarnotaurusEntity c) return c.isSaddled();
        return ((DilophosaurusEntity) pet).isSaddled();
    }
    int armorTier() {
        if (pet instanceof StegosaurusEntity s) return s.getArmorTier();
        if (pet instanceof CarnotaurusEntity c) return c.getArmorTier();
        return ((DilophosaurusEntity) pet).getArmorTier();
    }
    int variant() {
        if (pet instanceof StegosaurusEntity s) return s.getVariant();
        if (pet instanceof CarnotaurusEntity c) return c.getVariant();
        return ((DilophosaurusEntity) pet).getVariant();
    }
    boolean female() {
        if (pet instanceof StegosaurusEntity s) return s.isFemale();
        if (pet instanceof CarnotaurusEntity c) return c.isFemale();
        return ((DilophosaurusEntity) pet).isFemale();
    }
    String variantName() {
        int v = Math.max(0, variant());
        if (pet instanceof StegosaurusEntity) return new String[]{"Cibernético", "Dorado", "Magma", "Esmeralda", "Natural"}[Math.min(4, v)];
        if (pet instanceof CarnotaurusEntity) return new String[]{"Rojo", "Verde", "Acero", "Arena"}[Math.min(3, v)];
        return DilophosaurusEntity.VARIANT_NAMES[Math.min(DilophosaurusEntity.VARIANT_NAMES.length - 1, v)];
    }
    boolean fallen() { return ((PetOrders.Holder) pet).isFallen(); }
    int order() { return ((PetOrders.Holder) pet).getOrder(); }
    float cdLeft(int idx) {
        if (idx < 0) return 0.0F;
        if (pet instanceof StegosaurusEntity s) return s.cooldownLeft(idx);
        if (pet instanceof CarnotaurusEntity c) return c.cooldownLeft(idx);
        return ((DilophosaurusEntity) pet).cooldownLeft(idx);
    }
    int cdSeconds(int idx) {
        if (idx < 0) return 0;
        if (pet instanceof StegosaurusEntity s) return s.cooldownSeconds(idx);
        if (pet instanceof CarnotaurusEntity c) return c.cooldownSeconds(idx);
        return ((DilophosaurusEntity) pet).cooldownSeconds(idx);
    }
    /** Base de recarga en segundos solo para el dilofosaurio (para mostrarla en la lista); -1 si no se conoce. */
    double baseCooldown(int idx) {
        if (!(pet instanceof DilophosaurusEntity)) return -1;
        return switch (idx) {
            case 0 -> DiloConfig.get(DiloConfig.BITE_COOLDOWN_TICKS, 14) / 20.0;
            case 1 -> DiloConfig.get(DiloConfig.CLAW_COOLDOWN_SECONDS, 4);
            case 2 -> DiloConfig.get(DiloConfig.POUNCE_COOLDOWN_SECONDS, 9);
            case 3 -> DiloConfig.get(DiloConfig.FRENZY_COOLDOWN_SECONDS, 45);
            default -> DiloConfig.get(DiloConfig.SPIT_COOLDOWN_SECONDS, 8);
        };
    }

    /** Icono de la armadura puesta (o una armadura atenuada si no lleva). */
    Item armorItem() {
        int t = armorTier();
        if (pet instanceof DilophosaurusEntity) {
            if (t >= 1 && t <= 4) return ProdigyCreatures.DILO_ARMOR_ITEMS.get(t - 1).get();
            return Items.IRON_CHESTPLATE;
        }
        return new Item[]{Items.LEATHER_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE}[Math.max(0, Math.min(4, t))];
    }

    Item talentIcon(String id) {
        if (pet instanceof DilophosaurusEntity) {
            return switch (id) {
                case "C1" -> Items.BONE; case "C2" -> Items.FLINT; case "C3" -> Items.REDSTONE; case "C4" -> Items.DIAMOND_SWORD;
                case "C5" -> Items.ARROW; case "C6" -> Items.BLAZE_POWDER;
                case "M1" -> Items.SUGAR; case "M2" -> Items.RABBIT_FOOT; case "M3" -> Items.CLOCK; case "M4" -> Items.FEATHER;
                case "M5" -> Items.COMPASS; case "M6" -> Items.ENDER_EYE;
                case "S1" -> Items.IRON_CHESTPLATE; case "S2" -> Items.GLISTERING_MELON_SLICE; case "S3" -> Items.SPYGLASS;
                case "S4" -> Items.GOAT_HORN; case "S5" -> Items.SHIELD; case "S6" -> Items.TOTEM_OF_UNDYING; case "S7" -> Items.SPIDER_EYE;
                case "P1" -> Items.NETHER_STAR;
                default -> Items.PAPER;
            };
        }
        boolean carno = pet instanceof CarnotaurusEntity;
        return switch (id) {
            case "D1" -> Items.IRON_CHESTPLATE; case "D2" -> Items.GOLDEN_APPLE; case "D3" -> Items.ANVIL;
            case "D4" -> Items.TOTEM_OF_UNDYING; case "D5" -> Items.SHIELD;
            case "C1" -> Items.BONE; case "C2" -> Items.DIAMOND_AXE; case "C3" -> Items.FIRE_CHARGE;
            case "C4" -> Items.CACTUS; case "C5" -> Items.TNT; case "C6" -> Items.BLAZE_POWDER; case "C7" -> Items.NETHER_STAR;
            case "S1" -> Items.SUGAR; case "S2" -> Items.GLISTERING_MELON_SLICE; case "S3" -> Items.LEAD;
            case "S4" -> Items.SLIME_BALL; case "S5" -> Items.RED_BED; case "S6" -> carno ? Items.GOAT_HORN : Items.ENDER_PEARL; case "S7" -> Items.BEACON;
            case "U1" -> Items.NETHER_STAR; case "U2" -> Items.ENDER_EYE; case "U3" -> Items.CLOCK;
            default -> Items.PAPER;
        };
    }

    static boolean isFinalTalent(String id) { return id.startsWith("U") || id.startsWith("P"); }
}
