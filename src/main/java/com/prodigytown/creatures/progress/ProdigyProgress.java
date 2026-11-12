package com.prodigytown.creatures.progress;

import net.minecraft.nbt.CompoundTag;

/**
 * Nivel, XP y rangos de talento de un estegosaurio adulto.
 * Los estegosaurios guardados antes de existir este sistema cargan con nivel 30 (etapa adulta,
 * estadisticas base 270 PV / 27 dano / 14 armadura = fila 30 de la tabla), sin XP y sin talentos.
 */
public final class ProdigyProgress {
    public static final int ADULT_START_LEVEL = 30;

    private int level = ADULT_START_LEVEL;
    private int xp = 0;
    /** Tabla de talentos de la especie (estegosaurio: Talents.ALL, carnotauro: Talents.CARNO). */
    private final Talent[] table;
    private final int[] ranks;

    public ProdigyProgress() { this(Talents.ALL); }
    public ProdigyProgress(Talent[] table) { this.table = table; this.ranks = new int[table.length]; }
    public Talent[] table() { return table; }
    private int indexOf(String id) {
        for (int i = 0; i < table.length; i++) if (table[i].id.equals(id)) return i;
        return -1;
    }

    /** Progreso nuevo empezando en el nivel dado (el bebe empieza en 1; el adulto en 30). */
    public static ProdigyProgress atLevel(int lv) { return atLevel(lv, Talents.ALL); }
    public static ProdigyProgress atLevel(int lv, Talent[] table) {
        ProdigyProgress p = new ProdigyProgress(table);
        p.level = Math.max(1, Math.min(GrowthTable.MAX_LEVEL, lv));
        return p;
    }
    public ProdigyProgress copy() {
        ProdigyProgress c = new ProdigyProgress(table);
        c.level = level; c.xp = xp;
        System.arraycopy(ranks, 0, c.ranks, 0, ranks.length);
        return c;
    }
    public int level() { return level; }
    public int xp() { return xp; }
    public int xpToNext() { return level >= GrowthTable.MAX_LEVEL ? 0 : GrowthTable.XP_TO_NEXT[level]; }
    public int rank(int idx) { return ranks[idx]; }
    public int rank(String id) { int i = indexOf(id); return i < 0 ? 0 : ranks[i]; }

    public int pointsEarned() { return GrowthTable.POINTS_TOTAL[level]; }
    public int pointsSpent() {
        int total = 0;
        for (int i = 0; i < ranks.length; i++)
            for (int r = 0; r < ranks[i]; r++) total += table[i].cost[r];
        return total;
    }
    public int pointsAvailable() { return pointsEarned() - pointsSpent(); }
    public int talentsUnlocked() { int n = 0; for (int r : ranks) if (r > 0) n++; return n; }

    /** @return true si sube de nivel al menos una vez. */
    public boolean addXp(int amount) {
        if (amount <= 0 || level >= GrowthTable.MAX_LEVEL) return false;
        boolean leveled = false;
        xp += amount;
        while (level < GrowthTable.MAX_LEVEL && xp >= GrowthTable.XP_TO_NEXT[level]) {
            xp -= GrowthTable.XP_TO_NEXT[level];
            level++;
            leveled = true;
        }
        if (level >= GrowthTable.MAX_LEVEL) xp = 0;
        return leveled;
    }

    /** @return null si se puede subir un rango, o el motivo (en espanol) si no. */
    public String whyNotUnlock(int idx) {
        Talent t = table[idx];
        int cur = ranks[idx];
        if (cur >= t.maxRank) return "Rango maximo alcanzado";
        if (level < t.minLevel[cur]) return "Requiere nivel " + t.minLevel[cur];
        for (String[] req : t.reqs) {
            int need = Integer.parseInt(req[1]);
            if (rank(req[0]) < need) return "Requiere " + table[indexOf(req[0])].name + " rango " + need;
        }
        for (String ex : t.exclusive) {
            if (rank(ex) > 0) return "Excluyente con " + table[indexOf(ex)].name;
        }
        int cost = t.cost[cur];
        if (pointsAvailable() < cost) return "Faltan puntos (coste " + cost + ")";
        return null;
    }

    public boolean unlock(int idx) {
        if (idx < 0 || idx >= ranks.length || whyNotUnlock(idx) != null) return false;
        ranks[idx]++;
        return true;
    }

    public void reset() { java.util.Arrays.fill(ranks, 0); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level);
        tag.putInt("Xp", xp);
        for (int i = 0; i < ranks.length; i++) if (ranks[i] > 0) tag.putInt(table[i].id, ranks[i]);
        return tag;
    }

    public static ProdigyProgress load(CompoundTag tag) { return load(tag, ADULT_START_LEVEL); }

    public static ProdigyProgress load(CompoundTag tag, int defaultLevel) { return load(tag, defaultLevel, Talents.ALL); }

    public static ProdigyProgress load(CompoundTag tag, int defaultLevel, Talent[] table) {
        ProdigyProgress p = atLevel(defaultLevel, table);
        if (tag == null) return p;
        if (tag.contains("Level")) p.level = Math.max(1, Math.min(GrowthTable.MAX_LEVEL, tag.getInt("Level")));
        p.xp = Math.max(0, tag.getInt("Xp"));
        for (int i = 0; i < p.ranks.length; i++) {
            Talent t = table[i];
            p.ranks[i] = Math.max(0, Math.min(t.maxRank, tag.getInt(t.id)));
        }
        // Defensa ante datos corruptos o puntos negativos: si se gastaron mas puntos de los ganados, reiniciar.
        if (p.pointsAvailable() < 0) p.reset();
        return p;
    }
}
