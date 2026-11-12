package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Carnotauro: esqueleto articulado propio (cuello en S de 2 tramos, cola de 5, patas de 4 tramos + 3 dedos, cuernos de 3 tramos).
 * Dos mallas con los mismos huesos (adulto "Brutus": grande, robusto y agresivo, y cria). Huesos: root > body > (tail1..tail5, chest > (neck1 > neck2 > head > (jaw, horn_*), arm_*), thigh_* > shin_* > meta_* > foot_* > toe0..2_*).
 * Los huesos se animan SUMANDO a su pose de reposo (la de la malla), para no perder la postura de diseno.
 * La marcha usa la fase de zancada que mantiene la entidad (ligada a la distancia recorrida), no limbSwing de vanilla, que satura y hace que las patas resbalen.
 */
public final class CarnotaurusModel<T extends CarnotaurusEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_ADULT = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "carnotaurus"), "adult");
    public static final ModelLayerLocation LAYER_CHIBI = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "carnotaurus"), "chibi");
    public static LayerDefinition createAdultLayer() { return CarnotaurusLayers.adult(); }
    public static LayerDefinition createChibiLayer() { return CarnotaurusLayers.chibi(); }

    /** Hueso con su pose de reposo guardada. */
    private static final class Bone {
        final ModelPart p; final float x, y, z, rx, ry, rz;
        Bone(ModelPart p) { this.p = p; x = p.x; y = p.y; z = p.z; rx = p.xRot; ry = p.yRot; rz = p.zRot; }
        void reset() { p.x = x; p.y = y; p.z = z; p.xRot = rx; p.yRot = ry; p.zRot = rz; }
        void rotX(float dx) { p.xRot += dx; }
        void rotY(float dy) { p.yRot += dy; }
        void move(float dy) { p.y += dy; }
    }

    private final boolean adult;
    private final ModelPart root;
    private final Bone body, chest, neck1, neck2, head, jaw;
    private final Bone[] tail = new Bone[5];
    private final Bone[] horn = new Bone[6];                 // l0 l1 l2 r0 r1 r2 (solo se quedan en reposo)
    private final Bone[] arm = new Bone[2], arm2 = new Bone[2];
    private final Bone[] thigh = new Bone[2], shin = new Bone[2], meta = new Bone[2], foot = new Bone[2];
    private final Bone[][] toe = new Bone[2][3];
    private final Bone[] all;

    // posturas "sentado/dormido" (sumadas al reposo), medidas contra el suelo en la vista previa
    private final float sitDrop, sleepDrop, sleepNeck1, sleepNeck2, sleepHead;
    /** Profundidad de reposo del pie (suavizada) respecto al pivote del cuerpo: referencia para mantener las plantas pegadas al suelo. */
    private final float restDepth;

    public CarnotaurusModel(ModelPart bakedRoot, boolean adult) {
        this.adult = adult;
        this.root = bakedRoot.getChild("root");
        ModelPart b = root.getChild("body");
        java.util.ArrayList<Bone> list = new java.util.ArrayList<>();
        this.body = add(list, b);
        ModelPart c = b.getChild("chest");
        this.chest = add(list, c);
        ModelPart n1 = c.getChild("neck1"), n2 = n1.getChild("neck2"), h = n2.getChild("head");
        this.neck1 = add(list, n1); this.neck2 = add(list, n2); this.head = add(list, h);
        this.jaw = add(list, h.getChild("jaw"));
        ModelPart t = b;
        for (int i = 0; i < 5; i++) { t = t.getChild("tail" + (i + 1)); tail[i] = add(list, t); }
        String[] sd = {"l", "r"};
        for (int s = 0; s < 2; s++) {
            ModelPart hp = h.getChild("horn_" + sd[s]);
            ModelPart hp2 = hp.getChild("horn2_" + sd[s]);
            ModelPart hp3 = hp2.getChild("horn3_" + sd[s]);
            horn[s * 3] = add(list, hp); horn[s * 3 + 1] = add(list, hp2); horn[s * 3 + 2] = add(list, hp3);
            ModelPart a1 = c.getChild("arm_" + sd[s]);
            arm[s] = add(list, a1); arm2[s] = add(list, a1.getChild("arm2_" + sd[s]));
            ModelPart th = b.getChild("thigh_" + sd[s]);
            ModelPart sh = th.getChild("shin_" + sd[s]);
            ModelPart me = sh.getChild("meta_" + sd[s]);
            ModelPart fo = me.getChild("foot_" + sd[s]);
            thigh[s] = add(list, th); shin[s] = add(list, sh); meta[s] = add(list, me); foot[s] = add(list, fo);
            for (int k = 0; k < 3; k++) toe[s][k] = add(list, fo.getChild("toe" + k + "_" + sd[s]));
        }
        this.all = list.toArray(new Bone[0]);
        this.restDepth = smax(legDepth(0, 0, 0, 0, 0), legDepth(1, 0, 0, 0, 0));
        if (adult) { sitDrop = 41.87F; sleepDrop = 40.33F; sleepNeck1 = 1.37F; sleepNeck2 = -0.85F; sleepHead = 0.0F; }
        else { sitDrop = 20.36F; sleepDrop = 12.66F; sleepNeck1 = 1.37F; sleepNeck2 = -0.85F; sleepHead = 0.0F; }
    }

    private static float rotY2(float y, float z, float a) { return y * Mth.cos(a) - z * Mth.sin(a); }
    /** Maximo suave (sin esquina) de dos profundidades. */
    private static float smax(float a, float b) { return 0.5F * (a + b + (float) Math.sqrt((a - b) * (a - b) + 4.0F)); }
    /**
     * Cinematica plana de una pierna: cuanto cuelga el pie por debajo del pivote del cuerpo (unidades del modelo, hacia abajo) con los giros dados
     * sumados al reposo (cuerpo, muslo, tibia, metatarso). El pie va plano, asi que el resto es constante. Sirve para bajar o subir el cuerpo
     * de modo que el pie mas bajo siempre toque el suelo (sin hundirse ni flotar), en lugar de un balanceo vertical fijo.
     */
    private float legDepth(int s, float dBody, float dTh, float dSh, float dMe) {
        float a0 = body.rx + dBody;
        float d = rotY2(thigh[s].y, thigh[s].z, a0);
        float a1 = a0 + thigh[s].rx + dTh;
        d += rotY2(shin[s].y, shin[s].z, a1);
        float a2 = a1 + shin[s].rx + dSh;
        d += rotY2(meta[s].y, meta[s].z, a2);
        float a3 = a2 + meta[s].rx + dMe;
        return d + rotY2(foot[s].y, foot[s].z, a3);
    }

    private static Bone add(java.util.List<Bone> list, ModelPart p) { Bone bn = new Bone(p); list.add(bn); return bn; }

    /** Curva suave 0..1 (arranca y termina frenando): para que ninguna pose empiece o acabe de golpe. */
    private static float sstep(float t) { t = Mth.clamp(t, 0.0F, 1.0F); return t * t * (3.0F - 2.0F * t); }

    /**
     * Animacion fluida: todo se calcula con valores suavizados que mantiene la entidad en el cliente (fase de zancada ligada a la distancia
     * recorrida, velocidad, carrera, postura de caza, giro) y se interpola con el tiempo parcial del fotograma, asi no hay saltos a 20 Hz.
     * Las poses de mordida, cabezazo y rugido son curvas con anticipacion (preparar, golpear, recuperar) y no un seno simple.
     */
    @Override public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        for (Bone bn : all) bn.reset();
        float pt = Mth.clamp(age - e.tickCount, 0.0F, 1.0F);

        float sit = sstep(Mth.lerp(pt, e.sitAnimO, e.sitAnim)), sleep = sstep(Mth.lerp(pt, e.sleepAnimO, e.sleepAnim));
        float both = Math.max(sit, sleep);
        float walk = 1.0F - both;
        float ph = Mth.lerp(pt, e.gaitO, e.gait);
        float spd = Mth.lerp(pt, e.speedAnimO, e.speedAnim);
        float run = Mth.lerp(pt, e.runAnimO, e.runAnim) * walk;
        float aggro = Mth.lerp(pt, e.aggroAnimO, e.aggroAnim) * walk;
        float turn = Mth.clamp(Mth.lerp(pt, e.turnAnimO, e.turnAnim) / 7.0F, -1.0F, 1.0F);
        float mv = sstep(spd / 0.07F) * walk;                       // 0 parado .. 1 en marcha
        float sp01 = Mth.clamp(spd / 0.9F, 0.0F, 1.0F);
        float stride = CarnotaurusEntity.strideAmp(spd, adult) * mv;
        float breathe = Mth.sin(age * 0.07F);
        float lean = (0.02F * mv + 0.11F * run + 0.05F * aggro) * walk; // el torso se inclina hacia delante al correr y al acechar

        // ---- marcha bipeda: muslo -> tibia -> metatarso -> pie (el pie se mantiene plano aunque se incline el torso) ----
        float d0 = 0.0F, d1 = 0.0F;
        for (int s = 0; s < 2; s++) {
            float p = ph + (s == 0 ? 0.0F : Mth.PI);
            float sn = Mth.sin(p), cs = Mth.cos(p);
            float lift = Math.max(0.0F, -cs);                         // fase de vuelo de la pierna
            float th = stride * sn;
            float sh = (0.50F + 0.35F * run) * lift * mv;
            float me = -(0.28F + 0.12F * run) * lift * mv;
            thigh[s].rotX(th - lean * 0.7F); shin[s].rotX(sh); meta[s].rotX(me); foot[s].rotX(-(lean * 0.3F + th + sh + me));
            float dd = legDepth(s, lean, th - lean * 0.7F, sh, me);
            if (s == 0) d0 = dd; else d1 = dd;
        }
        body.move(restDepth - smax(d0, d1));                           // el cuerpo sube y baja lo justo para que el pie de apoyo pise el suelo
        body.rotX(lean);
        body.rotY(Mth.sin(ph) * (0.030F + 0.030F * run) * mv);           // giro de la cadera con cada paso
        chest.rotY(-Mth.sin(ph) * 0.030F * mv);                           // el pecho contrarresta
        body.p.zRot += Mth.sin(ph) * 0.020F * mv                           // el peso pasa de una pierna a otra
                     - turn * (0.04F + 0.08F * sp01) * mv                  // se inclina hacia dentro de la curva
                     + Mth.sin(age * 0.04F) * 0.012F * (1.0F - mv) * walk; // parado: cambia el peso de vez en cuando

        // ---- cuello, cabeza y cola ----
        chest.rotX(breathe * (adult ? 0.02F : 0.012F) * (1.0F + 1.5F * aggro));
        neck1.rotX(Mth.sin(ph * 2.0F) * 0.03F * mv + breathe * 0.01F + 0.26F * run + 0.12F * aggro);
        neck1.rotY(-Mth.sin(ph) * 0.015F * mv);
        neck2.rotX(-Mth.sin(ph * 2.0F) * 0.02F * mv - 0.04F * aggro);
        head.rotX(-0.10F * run - lean * 0.6F);                              // la cabeza se mantiene nivelada
        for (int i = 0; i < 5; i++) {
            float k = 1.0F + 0.28F * i;
            float tw = (Mth.sin(ph - i * 0.7F) * 0.11F * mv + Mth.sin(age * 0.05F - i * 0.5F) * 0.035F) * k;
            tail[i].rotY(tw - turn * 0.16F * (0.4F + 0.12F * (i + 1)) * (0.3F + 0.7F * sp01) * mv); // la cola se queda atras en las curvas
            tail[i].rotX((-0.05F * run - 0.04F * aggro) * (i == 0 ? 1.0F : 0.6F) + Mth.sin(ph * 2.0F - i * 0.6F) * 0.02F * mv);
        }
        // brazos pequenos: balanceo suave; al acechar se adelantan listos para agarrar
        for (int s = 0; s < 2; s++) {
            float p = ph + (s == 0 ? Mth.PI : 0.0F);
            arm[s].rotX(Mth.sin(p) * 0.18F * mv + breathe * 0.04F * (s == 0 ? 1 : -1) - 0.15F * aggro);
            arm2[s].rotX(0.10F * Mth.sin(age * 0.09F + s) + 0.25F * aggro);
        }

        // ---- mirada (no al dormir) ----
        float look = 1.0F - sleep;
        float yawR = Mth.clamp(yaw, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        neck1.rotY(yawR * 0.22F * look); neck2.rotY(yawR * 0.30F * look); head.rotY(yawR * 0.30F * look);
        head.rotX(Mth.clamp(pitch, -45.0F, 45.0F) * Mth.DEG_TO_RAD * 0.6F * look);
        // mandibula: reposo, jadeo, y entreabierta con gruñido al acechar o correr
        jaw.rotX((adult ? 0.16F : 0.04F) + Mth.sin(age * 0.09F) * 0.02F + aggro * (adult ? 0.20F : 0.10F) + 0.08F * run + Mth.sin(age * 1.9F) * 0.025F * aggro);
        head.rotY(Mth.sin(age * 0.9F) * 0.015F * aggro);

        // ---- mordida: preparar (echa la cabeza atras y abre), golpear (se lanza), cerrar y sacudir la presa ----
        if (e.biteT >= 0) {
            float atk = Mth.clamp((Mth.lerp(pt, e.biteTO, e.biteT) + 0.5F) / (float) CarnotaurusEntity.BITE_LEN, 0.0F, 1.0F);
            float wind = sstep(atk / 0.20F) * (1.0F - sstep((atk - 0.20F) / 0.14F));                  // echa la cabeza atras
            float strike = sstep((atk - 0.16F) / 0.16F) * (1.0F - sstep((atk - 0.55F) / 0.45F));      // se lanza hacia la presa
            float open = sstep(atk / 0.18F) * (1.0F - sstep((atk - 0.52F) / 0.20F));                  // boca muy abierta hasta cerrar de golpe
            float snap = sstep((atk - 0.52F) / 0.10F) * (1.0F - sstep((atk - 0.66F) / 0.20F));        // pequeno rebote al cerrar
            neck1.rotX(-0.26F * wind + 0.45F * strike); neck2.rotX(0.08F * wind - 0.12F * strike); head.rotX(-0.30F * wind + 0.20F * strike);
            body.rotX(-0.04F * wind + 0.10F * strike); body.move(2.0F * strike);
            jaw.rotX((adult ? 1.05F : 0.80F) * open - 0.22F * snap);
            head.rotY(Mth.sin(atk * Mth.PI * 3.0F) * 0.10F * strike * (1.0F - open * 0.5F));
        }
        // ---- golpe recibido: sacude la cabeza hacia atras y se recupera ----
        if (e.hurtTime > 0) {
            float x = Mth.clamp((e.hurtTime - pt) / Math.max(1, e.hurtDuration), 0.0F, 1.0F);
            float hurt = x * x;
            neck1.rotX(-0.20F * hurt); head.rotX(-0.15F * hurt); jaw.rotX(0.30F * hurt); body.rotX(-0.05F * hurt); tail[0].rotX(0.10F * hurt);
        }
        // ---- habilidades (tipo y progreso vienen del servidor; se interpola el progreso con el tiempo parcial) ----
        int kind = e.getAbilityKind();
        if (kind == 2 || kind == 3) {
            float len = kind == 2 ? CarnotaurusEntity.BUTT_LEN : CarnotaurusEntity.ROAR_LEN;
            float p = Mth.clamp(1.0F - Math.max(0.0F, e.getAbilityLeft() - pt) / len, 0.0F, 1.0F);
            if (kind == 2) { // cabezazo: echa la cabeza atras, clava los pies y embiste con los cuernos
                float wind = sstep(p / 0.28F) * (1.0F - sstep((p - 0.28F) / 0.10F));
                float strike = sstep((p - 0.26F) / 0.16F) * (1.0F - sstep((p - 0.55F) / 0.45F));
                neck1.rotX(-0.25F * wind + 0.60F * strike); neck2.rotX(0.08F * wind - 0.16F * strike); head.rotX(-0.18F * wind + 0.32F * strike);
                body.rotX(-0.05F * wind + 0.20F * strike); jaw.rotX(0.55F * strike);
                for (int i = 0; i < 2; i++) { thigh[i].rotX(-strike * 0.18F); foot[i].rotX(strike * 0.18F); }
            } else {         // rugido: se encoge e inspira, lanza la cabeza al cielo, sostiene temblando y suelta
                float coil = sstep(p / 0.20F) * (1.0F - sstep((p - 0.20F) / 0.10F));
                float hold = sstep((p - 0.20F) / 0.10F) * (1.0F - sstep((p - 0.82F) / 0.18F));
                float tr = Mth.sin(age * 2.4F);
                neck1.rotX(0.22F * coil - 0.34F * hold); neck2.rotX(0.10F * coil - 0.36F * hold); head.rotX(0.14F * coil - 0.45F * hold);
                jaw.rotX(0.12F * coil + (1.05F + 0.06F * tr) * hold);
                body.rotX(0.07F * coil - 0.11F * hold); body.move(3.5F * coil - 1.5F * hold);
                chest.rotX(-0.06F * coil + 0.05F * hold + Mth.sin(age * 0.9F) * 0.02F * hold);
                body.p.zRot += tr * 0.022F * hold;
                head.rotY(Mth.sin(age * 1.7F) * 0.05F * hold);
                tail[0].rotX(-0.15F * hold);
                for (int i = 0; i < 5; i++) tail[i].rotY(Mth.sin(age * 0.45F - i * 0.6F) * 0.10F * hold * (1.0F + 0.28F * i));
                for (int i = 0; i < 2; i++) { arm[i].rotX(-0.28F * hold); thigh[i].rotX(-0.10F * hold); foot[i].rotX(0.10F * hold); }
            }
        }

        // ---- sentado / dormido: el cuerpo baja, las patas se pliegan (muslo adelante, tibia atras, metatarso plano) ----
        if (both > 0.0F) {
            body.move((Mth.lerp(sleep, sitDrop, sleepDrop)) * both);
            for (int s = 0; s < 2; s++) {
                thigh[s].p.xRot = Mth.lerp(both, thigh[s].p.xRot, thigh[s].rx - 1.17F);
                shin[s].p.xRot = Mth.lerp(both, shin[s].p.xRot, shin[s].rx + 0.62F);
                meta[s].p.xRot = Mth.lerp(both, meta[s].p.xRot, meta[s].rx - 0.90F);
                foot[s].p.xRot = Mth.lerp(both, foot[s].p.xRot, foot[s].rx + 1.45F);
            }
            tail[0].rotX(-0.10F * both);
            // dormido: tumbado de costado y hecho una rueda (el cuerpo rueda sobre su eje largo, la cola y el cuello se enroscan hacia la barriga)
            body.p.zRot = body.rz + 1.50F * sleep;
            for (int i = 0; i < 5; i++) tail[i].rotX(-0.50F * sleep);
            neck1.rotX(sleepNeck1 * sleep); neck2.rotX(sleepNeck2 * sleep); head.rotX(sleepHead * sleep);
            neck1.rotY(-0.25F * sleep); // el cuello queda estirado al frente y la cabeza baja hasta apoyarse en el suelo (a la vista, no escondida bajo el cuerpo)
            jaw.p.xRot = Mth.lerp(sleep, jaw.p.xRot, jaw.rx);
            chest.rotX(Mth.sin(age * 0.07F) * 0.02F * sleep);
        }
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
