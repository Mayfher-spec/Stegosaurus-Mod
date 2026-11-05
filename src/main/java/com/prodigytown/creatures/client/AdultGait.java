package com.prodigytown.creatures.client;

/**
 * Marcha del adulto ligada a la distancia REAL recorrida (no a limbSwing, que se satura).
 * Con zancada fija por velocidad, el pie de apoyo retrocede respecto al cuerpo a la misma
 * velocidad a la que avanza el cuerpo: no patina. Clase sin dependencias de Minecraft (verificable aparte).
 *
 * Convenciones: theta &gt; 0 = el pie se va hacia atras; v en bloques/tick; 16 unidades de modelo = 1 bloque.
 */
public final class AdultGait {
    /** Cadera a suela, en bloques (57,5 unidades de modelo: igual en patas traseras y delanteras). */
    public static final float LEG = 57.5F / 16.0F;
    public static final float LEG_UNITS = 57.5F;
    public static final float MIN_V = 0.02F;
    private AdultGait() {}

    /** 0 = paso lento, 1 = trote rapido. */
    public static float blend(float v) { return clamp((v - 0.12F) / 0.30F, 0F, 1F); }
    public static float amp(float v) { return 0.30F + 0.16F * blend(v); }
    public static float duty(float v) { return 0.64F - 0.14F * blend(v); }

    /** Bloques que avanza el cuerpo en un ciclo completo para cero deslizamiento. */
    public static float strideLength(float v) {
        return 2.0F * LEG * (float) Math.sin(amp(v)) / duty(v);
    }
    /** Radianes de fase por tick. */
    public static float phaseRate(float v) {
        return (float) (2.0 * Math.PI) * v / strideLength(v);
    }
    /** Desfase de cada pata: lateral (paso) -> diagonal (trote) al aumentar la velocidad. */
    public static float offBackLeft(float v) { return 0.0F; }
    public static float offFrontLeft(float v) { return 0.25F + 0.25F * blend(v); }
    public static float offBackRight(float v) { return 0.5F; }
    public static float offFrontRight(float v) { return 0.75F + 0.25F * blend(v); }

    /** Resultado por pata: [0]=theta (rad), [1]=elevacion del pie 0..1 (solo en vuelo). */
    public static void pose(float phaseRad, float offset, float amp, float duty, float[] out) {
        float u = phaseRad / (float) (2.0 * Math.PI) + offset;
        u -= (float) Math.floor(u);
        if (u < duty) {
            float s = u / duty;
            out[0] = -amp + 2.0F * amp * s;      // apoyo: avance lineal del pie hacia atras
            out[1] = 0.0F;
        } else {
            float s = (u - duty) / (1.0F - duty);
            float e = s * s * (3.0F - 2.0F * s);
            out[0] = amp - 2.0F * amp * e;       // vuelo: el pie vuelve hacia delante
            out[1] = (float) Math.sin(Math.PI * s);
        }
    }
    private static float clamp(float x, float lo, float hi) { return x < lo ? lo : Math.min(x, hi); }
}
