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
    private final int[] ranks = new int[Talents.ALL.length];

    public int level() { return level; }
    public int xp() { return xp; }
    public int xpToNext() { return level >= GrowthTable.MAX_LEVEL ? 0 : GrowthTable.XP_TO_NEXT[level]; }
    public int rank(int idx) { return ranks[idx]; }
    public int rank(String id) { int i = Talents.index(id); return i < 0 ? 0 : ranks[i]; }

    public int pointsEarned() { return GrowthTable.POINTS_TOTAL[level]; }
    public int pointsSpent() {
        int total = 0;
        for (int i = 0; i < ranks.length; i++)
            for (int r = 0; r < ranks[i]; r++) total += Talents.ALL[i].cost[r];
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
        Talent t = Talents.ALL[idx];
        int cur = ranks[idx];
        if (cur >= t.maxRank) return "Rango maximo alcanzado";
        if (level < t.minLevel[cur]) return "Requiere nivel " + t.minLevel[cur];
        for (String[] req : t.reqs) {
            int need = Integer.parseInt(req[1]);
            if (rank(req[0]) < need) return "Requiere " + Talents.ALL[Talents.index(req[0])].name + " rango " + need;
        }
        for (String ex : t.exclusive) {
            if (rank(ex) > 0) return "Excluyente con " + Talents.ALL[Talents.index(ex)].name;
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
        for (int i = 0; i < ranks.length; i++) if (ranks[i] > 0) tag.putInt(Talents.ALL[i].id, ranks[i]);
        return tag;
    }

    public static ProdigyProgress load(CompoundTag tag) {
        ProdigyProgress p = new ProdigyProgress();
        if (tag == null) return p;
        if (tag.contains("Level")) p.level = Math.max(1, Math.min(GrowthTable.MAX_LEVEL, tag.getInt("Level")));
        p.xp = Math.max(0, tag.getInt("Xp"));
        for (int i = 0; i < p.ranks.length; i++) {
            Talent t = Talents.ALL[i];
            p.ranks[i] = Math.max(0, Math.min(t.maxRank, tag.getInt(t.id)));
        }
        // Defensa ante datos corruptos o puntos negativos: si se gastaron mas puntos de los ganados, reiniciar.
        if (p.pointsAvailable() < 0) p.reset();
        return p;
    }
}
