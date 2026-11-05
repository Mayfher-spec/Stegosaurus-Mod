package com.prodigytown.creatures.progress;

/** Compañero con nivel y XP (chibi y adulto). */
public interface ProdigyPet {
    ProdigyProgress getProgress();
    void gainXp(int amount);
}
