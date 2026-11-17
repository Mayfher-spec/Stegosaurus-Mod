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
        new Talent("C3", 0, "Embestida básica", 3, new int[]{2,2,3}, new int[]{30,30,30}, new String[][]{{"C1","2"}}, "Carrera veloz de 1,5 s (~22 bloques): 50 / 75 / 100 % del daño y aturde 2 s a quien embiste; recarga 15 s", new String[]{}),
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
        new Talent("S7", 2, "Maestría de soporte", 2, new int[]{5,5}, new int[]{90,90}, new String[][]{{"S6","1"}}, "+20 / +40 % a toda la regeneración y a los efectos de protección del dueño; −10 / −20 % de recarga de las habilidades de soporte", new String[]{}),
        new Talent("U1", 0, "Definitiva: potencia", 3, new int[]{4,5,6}, new int[]{50,75,100}, new String[][]{}, "Mejora de la definitiva: +15 / +30 / +50 % de daño (el rango pide nivel 50 / 75 / 100)", new String[]{}),
        new Talent("U2", 0, "Definitiva: alcance", 3, new int[]{3,4,5}, new int[]{50,75,100}, new String[][]{}, "Mejora de la definitiva: +1,5 / +3 / +4,5 bloques de alcance (el rango pide nivel 50 / 75 / 100)", new String[]{}),
        new Talent("U3", 2, "Definitiva: recarga", 3, new int[]{4,5,6}, new int[]{50,75,100}, new String[][]{}, "Mejora de la definitiva: recarga de 52 / 46 / 40 s en vez de 60 s (el rango pide nivel 50 / 75 / 100)", new String[]{})
    };
    /** Arbol del carnotauro (Brutus): mismas ramas, costes, niveles y requisitos que el del estegosaurio, con el combate adaptado a mordida, embestida y cabezazo. */
    public static final Talent[] CARNO = buildCarno();

    private static Talent retext(Talent o, String name, String effect) {
        return new Talent(o.id, o.branch, name, o.maxRank, o.cost, o.minLevel, o.reqs, effect, o.exclusive);
    }

    private static Talent[] buildCarno() {
        Talent[] t = ALL.clone();
        for (int i = 0; i < t.length; i++) {
            Talent o = t[i];
            switch (o.id) {
                case "D1" -> t[i] = retext(o, "Piel endurecida", "+1 de armadura por rango (hasta +5)");
                case "D2" -> t[i] = retext(o, "Corazón de bestia", "+4 % de vida máxima por rango (hasta +20 %)");
                case "D3" -> t[i] = retext(o, "Pisada firme", "+0,05 de resistencia al retroceso por rango (el total nunca pasa de 1,0)");
                case "D4" -> t[i] = retext(o, "Instinto de supervivencia", "Al bajar del 50 % de vida: −20 / −30 / −40 % de daño recibido durante 5 s (recarga 60 / 50 / 40 s)");
                case "D5" -> t[i] = retext(o, "Guardia salvaje I–III", "Montado: redirige 50 / 75 / 100 % del daño dirigido al jinete hacia el carnotauro (pasa por su armadura)");
                case "C1" -> t[i] = retext(o, "Mordida feroz", "+5 % de daño por rango (hasta +25 %)");
                case "C2" -> t[i] = retext(o, "Mandíbula desgarradora", "+0,5 bloques de alcance de la mordida montado por rango");
                case "C3" -> t[i] = retext(o, "Embestida de cacería", "La embestida hace +20 / +40 / +60 % más de daño; desde el rango 2 deja lento 2 s a quien embiste");
                case "C4" -> t[i] = retext(o, "Cuernos afilados", "Devuelve 15 / 25 / 35 % del daño cuerpo a cuerpo recibido (máx. 15 PV por golpe)");
                case "C5" -> t[i] = retext(o, "Cabezazo demoledor", "El cabezazo hace +50 / +100 / +150 % de daño y su recarga baja un 10 % por rango");
                case "C6" -> t[i] = retext(o, "Furia carmesí", "+10 / +20 / +30 % de daño mientras el dueño esté por debajo del 50 % de vida o haya sido herido hace menos de 5 s");
                case "C7" -> t[i] = retext(o, "Rey de la cacería", "+10 / +20 % de daño y −10 / −20 % de recarga de todas sus habilidades");
                case "S1" -> t[i] = retext(o, "Carrera brutal", "+2 % de velocidad de movimiento por rango (hasta +10 %)");
                case "S2" -> t[i] = retext(o, "Recuperación rápida", "Regenera 0,10 / 0,15 / 0,20 % de la vida máxima por segundo, también en combate");
                case "S3" -> t[i] = retext(o, "Cazador leal", "Se teletransporta a tu lado desde 88 / 80 / 64 bloques (sin el talento, 96)");
                case "S4" -> t[i] = retext(o, "Vínculo de manada", "Amortigua las caídas del dueño: −30 / −60 / −90 % de daño por caída si el carnotauro está a 8 bloques o menos");
                case "S5" -> t[i] = retext(o, "Descanso del cazador", "Tras 10 s sin combate regenera 0,3 / 0,5 / 0,7 % de la vida máxima por segundo");
                case "S6" -> t[i] = retext(o, "Rugido de aliento", "Si el dueño baja del 30 % de vida, ruge y le da Resistencia II y Regeneración durante 8 s (recarga 90 s)");
                case "S7" -> t[i] = retext(o, "Maestría de manada", "+20 / +40 % a toda la regeneración y a los efectos de protección del dueño; −10 / −20 % de recarga de las habilidades de soporte");
                case "U1" -> t[i] = retext(o, "Definitiva: potencia", "Rugido ancestral: +15 / +30 / +50 % de daño (el rango pide nivel 50 / 75 / 100)");
                case "U2" -> t[i] = retext(o, "Definitiva: alcance", "Rugido ancestral: +1,5 / +3 / +4,5 bloques de alcance (el rango pide nivel 50 / 75 / 100)");
                case "U3" -> t[i] = retext(o, "Definitiva: recarga", "Rugido ancestral: recarga de 52 / 46 / 40 s en vez de 60 s (el rango pide nivel 50 / 75 / 100)");
                default -> { }
            }
        }
        return t;
    }


    /**
     * Arbol del dilofosaurio: tres ramas (0 Caza, 1 Movilidad, 2 Supervivencia) de seis talentos y la mejora final "Cazador Primordial" (rama 3).
     * Mismos puntos por nivel (GrowthTable); cada talento tiene un efecto real en DilophosaurusEntity.
     */
    public static final Talent[] DILO = buildDilo();
    /** Compy (solo el lider de la manada): ramas Acecho (A, roja), Astucia (C, azul), Bandada (B, verde) y la mejora final Rey de la bandada (P1). Mismos puntos por nivel. */
    public static final Talent[] COMPY = buildCompy();

    private static Talent t(String id, int branch, String name, int maxRank, int[] cost, int[] minLevel, String[][] reqs, String effect) {
        return new Talent(id, branch, name, maxRank, cost, minLevel, reqs, effect, new String[]{});
    }

    private static Talent[] buildDilo() {
        return new Talent[]{
            // --- Rama roja: CAZA ---
            t("C1", 0, "Mandíbula fortalecida", 5, new int[]{1,1,2,2,3}, new int[]{10,10,10,10,10}, new String[][]{}, "+6 % de daño de la mordida por rango (hasta +30 %)"),
            t("C2", 0, "Zarpazos afilados", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"C1","2"}}, "+10 % de daño del zarpazo doble por rango y +0,3 bloques de alcance"),
            t("C3", 0, "Hemorragia", 3, new int[]{2,2,3}, new int[]{28,28,28}, new String[][]{{"C1","2"}}, "Mordida y zarpazos provocan sangrado: 10 / 16 / 22 % del ataque por segundo durante 4 s"),
            t("C4", 0, "Golpe crítico", 3, new int[]{2,3,3}, new int[]{36,36,36}, new String[][]{{"C2","1"},{"C3","1"}}, "8 / 14 / 20 % de probabilidad de golpe crítico (+60 % de daño)"),
            t("C5", 0, "Instinto cazador", 3, new int[]{2,3,3}, new int[]{46,46,46}, new String[][]{{"C4","1"}}, "+10 / +20 / +30 % de daño contra presas por debajo del 50 % de vida"),
            t("C6", 0, "Depredador implacable", 3, new int[]{3,3,4}, new int[]{58,58,58}, new String[][]{{"C5","1"}}, "Al abatir una presa: Velocidad I durante 4 / 6 / 8 s y recupera 3 / 6 / 9 % de su vida"),
            // --- Rama azul: MOVILIDAD ---
            t("M1", 1, "Patas veloces", 5, new int[]{1,1,2,2,3}, new int[]{10,10,10,10,10}, new String[][]{}, "+2 % de velocidad de movimiento por rango (hasta +10 %)"),
            t("M2", 1, "Salto mejorado", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"M1","2"}}, "El Salto depredador llega +12 / +24 / +36 % más lejos"),
            t("M3", 1, "Recuperación de energía", 3, new int[]{2,2,3}, new int[]{28,28,28}, new String[][]{{"M1","2"}}, "−5 / −10 / −15 % de recarga en todas sus habilidades"),
            t("M4", 1, "Caída controlada", 3, new int[]{2,3,3}, new int[]{36,36,36}, new String[][]{{"M2","1"}}, "−35 / −70 / −100 % de daño por caída para él y su jinete"),
            t("M5", 1, "Persecución persistente", 3, new int[]{2,3,3}, new int[]{46,46,46}, new String[][]{{"M3","1"}}, "+4 / +8 / +12 % de velocidad mientras persigue a una presa"),
            t("M6", 1, "Maestro del acecho", 2, new int[]{4,5}, new int[]{58,58}, new String[][]{{"M4","1"},{"M5","1"}}, "El Salto depredador hace +30 / +60 % de daño y su recarga baja otro 10 / 20 %"),
            // --- Rama verde: SUPERVIVENCIA ---
            t("S1", 2, "Piel resistente", 5, new int[]{1,1,2,2,3}, new int[]{10,10,10,10,10}, new String[][]{}, "+1 de armadura por rango (hasta +5)"),
            t("S2", 2, "Recuperación rápida", 3, new int[]{1,2,2}, new int[]{15,15,15}, new String[][]{{"S1","1"}}, "Regenera 0,10 / 0,15 / 0,20 % de la vida máxima por segundo, también en combate"),
            t("S3", 2, "Sentidos agudos", 3, new int[]{2,2,3}, new int[]{24,24,24}, new String[][]{{"S1","2"}}, "Marca con brillo a los monstruos hostiles a 10 / 16 / 22 bloques de su dueño"),
            t("S4", 2, "Intimidación mejorada", 3, new int[]{2,3,3}, new int[]{36,36,36}, new String[][]{{"S2","1"},{"S3","1"}}, "El rugido y el Frenesí intimidan en +2 / +3 / +4 bloques más de radio; desde el rango 2 también debilitan"),
            t("S5", 2, "Resistencia al daño", 3, new int[]{2,3,3}, new int[]{46,46,46}, new String[][]{{"S4","1"}}, "−4 / −8 / −12 % de daño recibido y +0,05 de resistencia al retroceso por rango"),
            t("S6", 2, "Protector jurásico", 3, new int[]{3,3,4}, new int[]{58,58,58}, new String[][]{{"S5","1"}}, "Si el dueño baja del 40 % de vida: Resistencia y Regeneración durante 5 / 7 / 9 s (recarga 90 s)"),
            t("S7", 2, "Glándula venenosa", 3, new int[]{2,3,3}, new int[]{30,30,30}, new String[][]{{"S3","1"}}, "Desbloquea el Escupitajo venenoso (tecla Y): despliega la membrana y escupe veneno a distancia. Rangos 2 y 3: veneno de 5 / 7 s y más daño"),
            // --- Mejora final ---
            t("P1", 3, "CAZADOR PRIMORDIAL", 1, new int[]{6}, new int[]{70}, new String[][]{{"C6","1"},{"M6","1"},{"S6","1"}},
              "+10 % de daño, +5 % de velocidad, +15 % de daño a monstruos hostiles, el Frenesí dura 3 s más y recarga −10 %; aura visual en el Frenesí")
        };
    }


    private static Talent[] buildCompy() {
        return new Talent[]{
            // --- Rama roja: ACECHO (dano y caza) ---
            t("A1", 0, "Garras afiladas", 3, new int[]{1,1,2}, new int[]{10,10,10}, new String[][]{}, "+8 % de daño de mordida por rango (hasta +24 %)"),
            t("A2", 0, "Ojo de cazador", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"A1","1"}}, "+6 / +12 / +18 % de daño contra presas por debajo del 50 % de vida"),
            t("A3", 0, "Zarpazo veloz", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"A1","1"}}, "−6 / −12 / −18 % de recarga de la Mordida fugaz"),
            t("A4", 0, "Colmillos de sangre", 3, new int[]{2,3,3}, new int[]{32,32,32}, new String[][]{{"A2","1"}}, "Las mordidas provocan sangrado: 10 / 16 / 22 % del ataque por segundo durante 3 s"),
            t("A5", 0, "Punto débil", 3, new int[]{2,3,3}, new int[]{32,32,32}, new String[][]{{"A3","1"}}, "8 / 14 / 20 % de probabilidad de golpe crítico (+60 % de daño)"),
            t("A6", 0, "Racha de caza", 3, new int[]{3,3,4}, new int[]{46,46,46}, new String[][]{{"A4","1"},{"A5","1"}}, "Al abatir una presa: recupera 3 / 6 / 9 % de su vida y gana Velocidad I durante 3 / 4 / 5 s"),
            t("A7", 0, "Depredador alfa", 1, new int[]{5}, new int[]{60}, new String[][]{{"A6","1"}}, "+12 % de daño y la Mordida fugaz golpea dos veces"),
            // --- Rama azul: ASTUCIA (movilidad y evasion) ---
            t("C1", 1, "Pasos silenciosos", 3, new int[]{1,1,2}, new int[]{10,10,10}, new String[][]{}, "Reduce el ruido que generas al moverte (−20 / −35 / −50 %) y mejora tu sigilo, lo que además aumenta el daño del primer ataque al emboscar (+10 / +20 / +30 %)"),
            t("C2", 1, "Carrera relámpago", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"C1","1"}}, "+4 % de velocidad de movimiento por rango (hasta +12 %)"),
            t("C3", 1, "Salto ágil", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"C1","1"}}, "El Salto evasivo llega +15 / +30 / +45 % más lejos"),
            t("C4", 1, "Plumaje ligero", 3, new int[]{2,3,3}, new int[]{32,32,32}, new String[][]{{"C2","1"}}, "−35 / −70 / −100 % de daño por caída para el líder y su dueño"),
            t("C5", 1, "Esquiva instintiva", 3, new int[]{2,3,3}, new int[]{32,32,32}, new String[][]{{"C3","1"}}, "6 / 12 / 18 % de probabilidad de esquivar un golpe"),
            t("C6", 1, "Huida veloz", 3, new int[]{3,3,4}, new int[]{46,46,46}, new String[][]{{"C4","1"},{"C5","1"}}, "Al recibir daño gana Velocidad I durante 3 / 4 / 5 s (recarga 10 s)"),
            t("C7", 1, "Maestro del sigilo", 1, new int[]{5}, new int[]{60}, new String[][]{{"C6","1"}}, "Tras el Salto evasivo se vuelve invisible 3 s y su siguiente mordida hace +50 % de daño"),
            // --- Rama verde: BANDADA (apoyo y sinergia) ---
            t("B1", 2, "Soldados fuertes", 3, new int[]{1,1,2}, new int[]{10,10,10}, new String[][]{}, "Los soldados de la manada ganan +10 / +20 / +30 % de vida y de daño"),
            t("B2", 2, "Festín compartido", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"B1","1"}}, "Cuando el líder o un soldado abate una presa, los soldados cercanos recuperan 2 / 4 / 6 de vida"),
            t("B3", 2, "Camaradería", 3, new int[]{2,2,3}, new int[]{20,20,20}, new String[][]{{"B1","1"}}, "Con 3 o más soldados a 8 bloques, el líder recibe −4 / −8 / −12 % de daño"),
            t("B4", 2, "Eco del chillido", 3, new int[]{2,3,3}, new int[]{32,32,32}, new String[][]{{"B2","1"}}, "El Chillido de alarma llega 2 / 3 / 4 bloques más lejos y dura 1 / 2 / 3 s más"),
            t("B5", 2, "Descanso en nidada", 3, new int[]{2,3,3}, new int[]{32,32,32}, new String[][]{{"B3","1"}}, "Sentados o dormidos, el líder y sus soldados cercanos recuperan 0,8 / 1,4 / 2,0 % de su vida por segundo"),
            t("B6", 2, "Voz de mando", 3, new int[]{3,3,4}, new int[]{46,46,46}, new String[][]{{"B4","1"},{"B5","1"}}, "El Ataque en bandada hace +15 / +30 / +45 % de daño y su recarga baja −10 / −20 / −30 %"),
            // --- Mejora final ---
            t("P1", 3, "REY DE LA BANDADA", 1, new int[]{6}, new int[]{70}, new String[][]{{"A7","1"},{"B6","1"},{"C7","1"}},
              "+10 % de daño y +5 % de velocidad al líder, +15 % de daño a los soldados, el Ataque en bandada dura 2 s más y todas las recargas bajan −10 %")
        };
    }

    private Talents() {}
    public static int index(String id) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].id.equals(id)) return i;
        return -1;
    }
}
