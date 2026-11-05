package com.prodigytown.creatures.progress;

/** GENERADO desde plan_sistema/tabla_talentos.csv (fuente de verdad). No editar a mano. */
public final class Talents {
    public static final Talent[] ALL = {
        new Talent("D1", 1, "Escamas resistentes", 5, new int[]{1,1,2,2,3}, new int[]{10,10,10,10,10}, new String[][]{}, "+1 de armadura por rango (hasta +5)", new String[]{}),
        new Talent("D2", 1, "Corazón robusto", 5, new int[]{1,1,2,2,3}, new int[]{10,10,10,10,10}, new String[][]{}, "+4 % de vida máxima por rango (hasta +20 %)", new String[]{}),
        new Talent("D3", 1, "Firmeza ancestral", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"D1","2"}}, "+0,05 de resistencia al retroceso por rango (el total nunca pasa de 1,0)", new String[]{}),
        new Talent("D4", 1, "Instinto defensivo", 3, new int[]{2,3,3}, new int[]{30,30,30}, new String[][]{{"D2","2"}}, "Al bajar del 50 % de vida: −20 / −30 / −40 % de daño recibido durante 5 s (recarga 60 / 50 / 40 s)", new String[]{}),
        new Talent("D5", 1, "Guardia del Estegosaurio I–III", 3, new int[]{3,4,5}, new int[]{60,70,85}, new String[][]{{"D3","1"},{"D4","1"}}, "Montado: redirige 50 / 75 / 100 % del daño dirigido al jinete; el daño pasa por la armadura y la vida del estegosaurio y se procesa una sola vez", new String[]{}),
        new Talent("C1", 0, "Cola entrenada", 5, new int[]{1,1,2,2,3}, new int[]{20,20,20,20,20}, new String[][]{}, "+5 % de daño de cola por rango (hasta +25 %)", new String[]{}),
        new Talent("C2", 0, "Golpe barrido", 3, new int[]{2,2,3}, new int[]{25,25,25}, new String[][]{{"C1","2"}}, "El coletazo golpea en arco: +0,5 bloques de alcance por rango", new String[]{}),
        new Talent("C3", 0, "Embestida básica", 3, new int[]{2,2,3}, new int[]{30,30,30}, new String[][]{{"C1","2"}}, "Carga corta hacia delante: 4 / 6 / 8 bloques, 50 / 75 / 100 % del daño de ataque, recarga 12 s", new String[]{}),
        new Talent("C4", 0, "Púas afiladas", 3, new int[]{2,2,3}, new int[]{35,35,35}, new String[][]{{"C2","1"}}, "Devuelve 15 / 25 / 35 % del daño cuerpo a cuerpo recibido (máx. 15 PV por golpe; no vuelve a devolverse ni daña al dueño ni a aliados)", new String[]{}),
        new Talent("C5", 0, "Coletazo devastador", 3, new int[]{3,3,4}, new int[]{45,45,45}, new String[][]{{"C2","2"},{"C3","1"}}, "Ataque cargado (mantener 1 s): +100 / +150 / +200 % de daño y retroceso fuerte; recarga 10 / 9 / 8 s", new String[]{}),
        new Talent("C6", 0, "Furia protectora", 3, new int[]{2,3,3}, new int[]{50,50,50}, new String[][]{{"C4","1"},{"C5","1"}}, "+10 / +20 / +30 % de daño mientras el dueño esté por debajo del 50 % de vida o haya sido herido en los últimos 5 s", new String[]{}),
        new Talent("C7", 0, "Dominio de combate", 2, new int[]{5,5}, new int[]{90,90}, new String[][]{{"C6","2"}}, "+10 / +20 % de daño y −10 / −20 % de recarga de todas las habilidades", new String[]{}),
        new Talent("S1", 2, "Paso ligero", 5, new int[]{1,1,2,2,3}, new int[]{10,10,10,10,10}, new String[][]{}, "+2 % de velocidad de movimiento por rango (hasta +10 %)", new String[]{}),
        new Talent("S2", 2, "Recuperación natural", 3, new int[]{1,2,2}, new int[]{15,15,15}, new String[][]{{"S1","1"}}, "Regenera 0,10 / 0,15 / 0,20 % de la vida máxima por segundo, también en combate", new String[]{}),
        new Talent("S3", 2, "Compañero fiel", 3, new int[]{1,1,2}, new int[]{15,15,15}, new String[][]{}, "Se reúne contigo desde 24 / 18 / 12 bloques de distancia (sin el talento, 64); el rango 3 también cruza portales contigo", new String[]{}),
        new Talent("S4", 2, "Vínculo reforzado", 3, new int[]{2,2,3}, new int[]{55,55,55}, new String[][]{{"S3","1"}}, "Amortigua las caídas del dueño: −30 / −60 / −90 % de daño por caída si el compañero está a 8 bloques o menos", new String[]{}),
        new Talent("S5", 2, "Descanso reparador", 3, new int[]{2,2,3}, new int[]{35,35,35}, new String[][]{{"S2","1"}}, "Tras 10 s sin combate regenera 0,3 / 0,5 / 0,7 % de la vida máxima por segundo", new String[]{}),
        new Talent("S6", 2, "Protección de emergencia", 1, new int[]{4}, new int[]{75}, new String[][]{{"S4","1"},{"S5","1"}}, "Si el dueño baja del 30 % de vida y el compañero está en forma chibi, se transforma solo y combate; recarga 5 min", new String[]{}),
        new Talent("S7", 2, "Maestría de soporte", 2, new int[]{5,5}, new int[]{90,90}, new String[][]{{"S6","1"}}, "+20 / +40 % a toda la regeneración y a los efectos de protección del dueño; −10 / −20 % de recarga de las habilidades de soporte", new String[]{})
    };
    private Talents() {}
    public static int index(String id) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].id.equals(id)) return i;
        return -1;
    }
}
