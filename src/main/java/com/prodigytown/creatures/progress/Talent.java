package com.prodigytown.creatures.progress;

/** Definicion inmutable de un nodo del arbol. Branch: 0 combate, 1 defensa, 2 soporte. */
public final class Talent {
    public final String id;
    public final int branch;
    public final String name;
    public final int maxRank;
    public final int[] cost;
    public final int[] minLevel;
    public final String[][] reqs;
    public final String effect;
    public final String[] exclusive;

    public Talent(String id, int branch, String name, int maxRank, int[] cost, int[] minLevel,
                  String[][] reqs, String effect, String[] exclusive) {
        this.id = id; this.branch = branch; this.name = name; this.maxRank = maxRank;
        this.cost = cost; this.minLevel = minLevel; this.reqs = reqs; this.effect = effect; this.exclusive = exclusive;
    }
}
