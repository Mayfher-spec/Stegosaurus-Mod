package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Dilofosaurio: esqueleto propio (cuello en S de 2 tramos, cola de 5, patas de 4 tramos + 3 dedos, dos crestas oseas y membrana de 7 petalos articulados).
 * Dos mallas con los mismos huesos (adulto y cria). Huesos: root > body > (tail1..tail5, chest > (neck1 > neck2 > (head > (jaw, crest_*), fp0..6 > fq0..6), arm_*), thigh_* > shin_* > meta_* > foot_* > toe0..2_*).
 * Los huesos se animan SUMANDO a su pose de reposo (la de la malla). La membrana (fq*) esta plegada contra el cuello en reposo (-1.45) y se abre girando hasta -0.45.
 * La marcha usa la fase de zancada que mantiene la entidad (ligada a la distancia recorrida).
 */
public final class DilophosaurusModel<T extends DilophosaurusEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_ADULT = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "dilophosaurus"), "adult");
    public static final ModelLayerLocation LAYER_CHIBI = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "dilophosaurus"), "chibi");
    public static LayerDefinition createAdultLayer() { return DilophosaurusLayers.adult(); }
    public static LayerDefinition createChibiLayer() { return DilophosaurusLayers.chibi(); }

    private static final int PETALS = 7;

    /** Hueso con su pose de reposo guardada. */
    private static final class Bone {
        final ModelPart p; final float x, y, z, rx, ry, rz;
        Bone(ModelPart p) { this.p = p; x = p.x; y = p.y; z = p.z; rx = p.xRot; ry = p.yRot; rz = p.zRot; }
        void reset() { p.x = x; p.y = y; p.z = z; p.xRot = rx; p.yRot = ry; p.zRot = rz; }
        void rotX(float d) { p.xRot += d; }
        void rotY(float d) { p.yRot += d; }
        void rotZ(float d) { p.zRot += d; }
        void move(float dy) { p.y += dy; }
    }

    private final boolean adult;
    private final ModelPart root;
    private final Bone body, chest, neck1, neck2, head, jaw;
    private final Bone[] tail = new Bone[5];
    private final Bone[] arm = new Bone[2], arm2 = new Bone[2];
    private final Bone[] thigh = new Bone[2], shin = new Bone[2], meta = new Bone[2], foot = new Bone[2];
    private final Bone[][] toe = new Bone[2][3];
    private final Bone[] petal = new Bone[PETALS];
    private final Bone[] all;

    /** Descensos del cuerpo (unidades de modelo) medidos en la vista previa para que sentado y dormido apoye en el suelo. */
    private final float sitDrop, sleepDrop;
    private final float restDepth;

    public DilophosaurusModel(ModelPart bakedRoot, boolean adult) {
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
        add(list, h.getChild("crest_l")); add(list, h.getChild("crest_r"));
        for (int k = 0; k < PETALS; k++) {
            ModelPart fp = n2.getChild("fp" + k);
            add(list, fp);
            petal[k] = add(list, fp.getChild("fq" + k));
        }
        ModelPart t = b;
        for (int i = 0; i < 5; i++) { t = t.getChild("tail" + (i + 1)); tail[i] = add(list, t); }
        String[] sd = {"l", "r"};
        for (int s = 0; s < 2; s++) {
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
        if (adult) { sitDrop = 26.65F; sleepDrop = 39.04F; }
        else { sitDrop = 15.46F; sleepDrop = 15.12F; }
    }

    private static float rotY2(float y, float z, float a) { return y * Mth.cos(a) - z * Mth.sin(a); }
    private static float smax(float a, float b) { return 0.5F * (a + b + (float) Math.sqrt((a - b) * (a - b) + 4.0F)); }
    /** Cuanto cuelga el pie por debajo del pivote del cuerpo con los giros dados sumados al reposo (el pie va plano). Mantiene la planta pegada al suelo. */
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
    private static float sstep(float t) { t = Mth.clamp(t, 0.0F, 1.0F); return t * t * (3.0F - 2.0F * t); }
    /** Ventana suave: sube entre a y b, baja entre c y d. */
    private static float win(float t, float a, float b, float c, float d) { return sstep((t - a) / Math.max(1.0E-4F, b - a)) * (1.0F - sstep((t - c) / Math.max(1.0E-4F, d - c))); }

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
        float air = sstep(Mth.lerp(pt, e.airAnimO, e.airAnim)) * walk;
        float swim = sstep(Mth.lerp(pt, e.swimAnimO, e.swimAnim)) * walk;
        float frill = sstep(Mth.lerp(pt, e.frillAnimO, e.frillAnim));
        float mv = sstep(spd / 0.07F) * walk * (1.0F - air) * (1.0F - swim);
        float sp01 = Mth.clamp(spd / 0.9F, 0.0F, 1.0F);
        float stride = DilophosaurusEntity.strideAmp(spd, adult) * mv;
        float breathe = Mth.sin(age * 0.09F);
        float lean = (0.03F * mv + 0.15F * run + 0.05F * aggro) * walk;

        // ---- marcha bipeda: muslo -> tibia -> metatarso -> pie (el pie se mantiene plano aunque se incline el torso) ----
        float d0 = 0.0F, d1 = 0.0F;
        for (int s = 0; s < 2; s++) {
            float p = ph + (s == 0 ? 0.0F : Mth.PI);
            float sn = Mth.sin(p), cs = Mth.cos(p);
            float lift = Math.max(0.0F, -cs);
            float th = stride * sn;
            float sh = (0.55F + 0.40F * run) * lift * mv;
            float me = -(0.30F + 0.14F * run) * lift * mv;
            thigh[s].rotX(th - lean * 0.7F); shin[s].rotX(sh); meta[s].rotX(me); foot[s].rotX(-(lean * 0.3F + th + sh + me));
            float dd = legDepth(s, lean, th - lean * 0.7F, sh, me);
            if (s == 0) d0 = dd; else d1 = dd;
        }
        float ground = 1.0F - air - swim; if (ground < 0.0F) ground = 0.0F;
        body.move((restDepth - smax(d0, d1)) * (1.0F - air) * (1.0F - swim));
        body.rotX(lean);
        body.rotY(Mth.sin(ph) * (0.035F + 0.035F * run) * mv);
        chest.rotY(-Mth.sin(ph) * 0.035F * mv);
        body.p.zRot += Mth.sin(ph) * 0.025F * mv - turn * (0.05F + 0.10F * sp01) * mv + Mth.sin(age * 0.04F) * 0.012F * (1.0F - mv) * walk;

        // ---- cuello, cabeza y cola (la cola sube al correr para equilibrar) ----
        chest.rotX(breathe * (adult ? 0.022F : 0.014F) * (1.0F + 1.5F * aggro));
        neck1.rotX(Mth.sin(ph * 2.0F) * 0.035F * mv + breathe * 0.012F + 0.16F * run + 0.10F * aggro);
        neck1.rotY(-Mth.sin(ph) * 0.02F * mv);
        neck2.rotX(-Mth.sin(ph * 2.0F) * 0.025F * mv - 0.05F * aggro);
        head.rotX(-0.08F * run - lean * 0.6F);
        for (int i = 0; i < 5; i++) {
            float k = 1.0F + 0.30F * i;
            float tw = (Mth.sin(ph - i * 0.7F) * 0.12F * mv + Mth.sin(age * 0.05F - i * 0.5F) * 0.04F) * k;
            tail[i].rotY(tw - turn * 0.18F * (0.4F + 0.12F * (i + 1)) * (0.3F + 0.7F * sp01) * mv);
            tail[i].rotX((-0.07F * run - 0.04F * aggro) * (i == 0 ? 1.0F : 0.6F) + Mth.sin(ph * 2.0F - i * 0.6F) * 0.025F * mv);
        }
        // brazos: balanceo suave; al correr se pliegan contra el pecho; al acechar se adelantan
        for (int s = 0; s < 2; s++) {
            float p = ph + (s == 0 ? Mth.PI : 0.0F);
            arm[s].rotX(Mth.sin(p) * 0.16F * mv * (1.0F - run) + breathe * 0.04F * (s == 0 ? 1 : -1) - 0.18F * aggro + 0.45F * run);
            arm2[s].rotX(0.10F * Mth.sin(age * 0.09F + s) + 0.28F * aggro + 0.55F * run);
        }
        // ---- mirada (no al dormir) ----
        float look = 1.0F - sleep;
        float yawR = Mth.clamp(yaw, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        neck1.rotY(yawR * 0.22F * look); neck2.rotY(yawR * 0.30F * look); head.rotY(yawR * 0.30F * look);
        head.rotX(Mth.clamp(pitch, -45.0F, 45.0F) * Mth.DEG_TO_RAD * 0.6F * look);
        jaw.rotX((adult ? 0.10F : 0.04F) + Mth.sin(age * 0.09F) * 0.02F + aggro * (adult ? 0.22F : 0.10F) + 0.06F * run + Mth.sin(age * 2.2F) * 0.03F * aggro);
        head.rotY(Mth.sin(age * 1.1F) * 0.018F * aggro);

        // ---- reposo vivo: giro de cabeza curioso y (cria) cabeceo + saltitos ----
        float idle = (1.0F - mv) * walk * (1.0F - air) * (1.0F - swim);
        float curious = Mth.sin(age * 0.031F + e.getId()) * Mth.sin(age * 0.017F + 1.7F * e.getId());
        head.rotY(curious * 0.30F * idle * look); neck2.rotY(curious * 0.12F * idle * look);
        if (!adult) {
            float tilt = win(Mth.sin(age * 0.045F + e.getId() * 0.7F) * 0.5F + 0.5F, 0.80F, 0.88F, 0.92F, 1.0F);
            head.rotZ(tilt * 0.30F * idle * look);
            float hop = win(Mth.sin(age * 0.021F + e.getId() * 2.3F) * 0.5F + 0.5F, 0.93F, 0.96F, 0.97F, 1.0F);
            body.move(-hop * 3.0F * idle);
            for (int s = 0; s < 2; s++) { thigh[s].rotX(-0.25F * hop * idle); foot[s].rotX(0.2F * hop * idle); }
        }

        // ---- en el aire: piernas recogidas, cabeza al frente (salto y caida) ----
        if (air > 0.0F) {
            float fall = Mth.clamp(-e.airVel * 1.5F, 0.0F, 1.0F);       // 1 cayendo, 0 subiendo
            for (int s = 0; s < 2; s++) {
                thigh[s].rotX((-0.60F + 0.50F * fall) * air); shin[s].rotX((0.70F - 0.30F * fall) * air); meta[s].rotX(-0.45F * air); foot[s].rotX(0.30F * air);
                arm[s].rotX(-0.30F * air); arm2[s].rotX(0.25F * air);
            }
            body.rotX((-0.10F + 0.18F * fall) * air);
            neck1.rotX(-0.10F * air);
            for (int i = 0; i < 5; i++) tail[i].rotX(-0.06F * air);
            jaw.rotX(0.25F * air);
        }
        // ---- aterrizaje: se agacha un instante y recupera ----
        if (e.landT > 0) {
            float x = Mth.clamp((e.landT - pt) / DilophosaurusEntity.LAND_LEN, 0.0F, 1.0F);
            float k = Mth.sin(x * Mth.PI) * walk;
            body.move(5.0F * k * (adult ? 1.0F : 0.5F));
            for (int s = 0; s < 2; s++) { thigh[s].rotX(-0.35F * k); shin[s].rotX(0.45F * k); meta[s].rotX(-0.25F * k); foot[s].rotX(0.25F * k); }
            neck1.rotX(0.15F * k); tail[0].rotX(-0.08F * k);
        }
        // ---- nadando: cuerpo casi horizontal, patas pedaleando, cola ondulando ----
        if (swim > 0.0F) {
            float sw = age * 0.32F;
            body.rotX(-0.12F * swim); body.move(-4.0F * swim);
            for (int s = 0; s < 2; s++) {
                float p = sw + (s == 0 ? 0.0F : Mth.PI);
                thigh[s].rotX(-0.55F * swim + Mth.sin(p) * 0.45F * swim); shin[s].rotX(0.65F * swim + Math.max(0.0F, Mth.cos(p)) * 0.5F * swim); meta[s].rotX(-0.35F * swim); foot[s].rotX(0.25F * swim);
                arm[s].rotX(-0.35F * swim + Mth.sin(p + 1.0F) * 0.18F * swim);
            }
            neck1.rotX(0.30F * swim); neck2.rotX(-0.10F * swim); head.rotX(-0.15F * swim);
            for (int i = 0; i < 5; i++) tail[i].rotY(Mth.sin(sw * 0.6F - i * 0.8F) * 0.22F * swim);
        }

        // ---- mordida rapida: ladea la cabeza, abre y se lanza; el dano cae justo en el cierre ----
        if (e.biteT >= 0) {
            float atk = Mth.clamp((Mth.lerp(pt, e.biteTO, e.biteT) + 0.5F) / (float) DilophosaurusEntity.BITE_LEN, 0.0F, 1.0F);
            float wind = win(atk, 0.0F, 0.22F, 0.26F, 0.40F);
            float strike = win(atk, 0.18F, 0.36F, 0.52F, 1.0F);
            float open = win(atk, 0.0F, 0.20F, 0.50F, 0.70F);
            float snap = win(atk, 0.50F, 0.60F, 0.66F, 0.86F);
            neck1.rotX(-0.20F * wind + 0.40F * strike); neck2.rotX(0.08F * wind - 0.12F * strike); head.rotX(-0.25F * wind + 0.18F * strike);
            neck1.rotY(0.10F * wind); head.rotY(0.14F * wind - 0.10F * strike);
            body.rotX(-0.03F * wind + 0.10F * strike); body.move(1.5F * strike);
            jaw.rotX((adult ? 0.95F : 0.75F) * open - 0.20F * snap);
        }
        // ---- golpe recibido ----
        if (e.hurtTime > 0) {
            float x = Mth.clamp((e.hurtTime - pt) / Math.max(1, e.hurtDuration), 0.0F, 1.0F);
            float hurt = x * x;
            neck1.rotX(-0.22F * hurt); head.rotX(-0.16F * hurt); jaw.rotX(0.35F * hurt); body.rotX(-0.06F * hurt); tail[0].rotX(0.12F * hurt);
            head.rotY(Mth.sin(age * 2.0F) * 0.12F * hurt);
        }
        // ---- comer: baja la cabeza, tira hacia atras y mastica ----
        if (e.eatT >= 0) {
            float x = Mth.clamp((Mth.lerp(pt, e.eatTO, e.eatT) + 0.5F) / (float) DilophosaurusEntity.EAT_LEN, 0.0F, 1.0F);
            float dip = win(x, 0.0F, 0.25F, 0.60F, 0.85F);
            float chew = Mth.sin(x * Mth.PI * 8.0F) * win(x, 0.25F, 0.35F, 0.75F, 1.0F);
            neck1.rotX(0.30F * dip); neck2.rotX(-0.10F * dip); head.rotX(0.30F * dip - 0.10F * chew);
            jaw.rotX(0.45F * dip * (0.5F + 0.5F * Math.abs(chew)) + 0.20F * Math.abs(chew));
            body.rotX(0.05F * dip);
        }

        // ---- habilidades (tipo y progreso vienen del servidor) ----
        int kind = e.getAbilityKind();
        boolean ab = kind != 0 && e.getAbilityLeft() > 0;
        if (ab) {
            int len = DilophosaurusEntity.abilityLen(kind);
            float p = Mth.clamp(1.0F - Math.max(0.0F, e.getAbilityLeft() - pt) / len, 0.0F, 1.0F);
            if (kind == DilophosaurusEntity.K_CLAW) {         // zarpazo doble: se inclina, golpea con un brazo, sigue con el otro, se recupera
                float lean2 = win(p, 0.0F, 0.20F, 0.80F, 1.0F);
                float c1 = win(p, 0.12F, 0.26F, 0.34F, 0.50F), c2 = win(p, 0.46F, 0.60F, 0.68F, 0.88F);
                body.rotX(0.14F * lean2); body.rotY((c1 - c2) * 0.20F); chest.rotY(-(c1 - c2) * 0.28F);
                neck1.rotX(0.12F * lean2); head.rotX(0.10F * lean2); jaw.rotX(0.30F * lean2);
                arm[0].rotX(-1.55F * c1 + 0.20F * lean2); arm2[0].rotX(-0.70F * c1);
                arm[1].rotX(-1.55F * c2 + 0.20F * lean2); arm2[1].rotX(-0.70F * c2);
                for (int s = 0; s < 2; s++) { thigh[s].rotX(-0.15F * lean2); foot[s].rotX(0.15F * lean2); }
                tail[0].rotX(-0.10F * lean2);
            } else if (kind == DilophosaurusEntity.K_POUNCE) { // salto depredador: flexiona, baja la cabeza, se impulsa, vuela y cae con impacto
                float crouch = win(p, 0.0F, 0.22F, 0.22F, 0.34F);
                float fly = win(p, 0.20F, 0.34F, 0.70F, 0.84F);
                float hit = win(p, 0.76F, 0.84F, 0.86F, 1.0F);
                body.move(7.0F * crouch - 4.0F * hit);
                body.rotX(0.12F * crouch - 0.10F * fly + 0.16F * hit);
                for (int s = 0; s < 2; s++) {
                    thigh[s].rotX(-0.55F * crouch - 0.50F * fly - 0.30F * hit); shin[s].rotX(0.75F * crouch + 0.60F * fly + 0.40F * hit); meta[s].rotX(-0.40F * crouch - 0.30F * fly - 0.20F * hit);
                    foot[s].rotX(0.35F * crouch + 0.20F * fly + 0.2F * hit);
                    arm[s].rotX(-0.9F * fly); arm2[s].rotX(0.4F * fly);
                }
                neck1.rotX(0.35F * crouch - 0.30F * fly + 0.25F * hit); neck2.rotX(-0.12F * crouch + 0.10F * fly); head.rotX(0.22F * crouch - 0.10F * fly);
                jaw.rotX(0.95F * fly * (1.0F - hit) + 0.25F * crouch);
                for (int i = 0; i < 5; i++) tail[i].rotX(-0.10F * fly - 0.06F * crouch);
            } else if (kind == DilophosaurusEntity.K_SPIT) {   // escupitajo: echa la cabeza atras con la membrana abierta y la lanza hacia delante
                float wind = win(p, 0.0F, 0.30F, 0.44F, 0.52F), snap = win(p, 0.44F, 0.52F, 0.60F, 1.0F);
                body.move(3.0F * wind); body.rotX(-0.10F * wind + 0.05F * snap);
                for (int s = 0; s < 2; s++) { thigh[s].rotX(-0.18F * wind); shin[s].rotX(0.22F * wind); foot[s].rotX(0.10F * wind); arm[s].rotX(-0.35F * wind); }
                neck1.rotX(-0.30F * wind + 0.30F * snap); neck2.rotX(-0.26F * wind + 0.22F * snap); head.rotX(-0.45F * wind + 0.30F * snap);
                jaw.rotX(0.80F * wind + 0.55F * snap);
                tail[0].rotX(-0.10F * wind);
            } else if (kind == DilophosaurusEntity.K_FRENZY || kind == DilophosaurusEntity.K_ROAR) {
                // activacion del frenesi / rugido intimidante: se detiene, baja el cuerpo, levanta la cabeza, abre la membrana y chilla
                float coil = win(p, 0.0F, 0.18F, 0.18F, 0.30F);
                float hold = win(p, 0.20F, 0.32F, 0.80F, 1.0F);
                float tr = Mth.sin(age * 2.8F);
                body.move(5.0F * coil - 1.0F * hold);
                body.rotX(0.08F * coil - 0.14F * hold);
                for (int s = 0; s < 2; s++) { thigh[s].rotX(-0.25F * coil - 0.08F * hold); shin[s].rotX(0.30F * coil + 0.10F * hold); foot[s].rotX(0.15F * coil); arm[s].rotX(-0.45F * hold); arm2[s].rotX(0.3F * hold); }
                neck1.rotX(0.28F * coil - 0.40F * hold); neck2.rotX(0.10F * coil - 0.25F * hold); head.rotX(0.18F * coil - 0.35F * hold);
                jaw.rotX(0.12F * coil + (1.00F + 0.07F * tr) * hold);
                chest.rotX(-0.06F * coil + 0.05F * hold);
                body.p.zRot += tr * 0.025F * hold;
                head.rotY(Mth.sin(age * 1.9F) * 0.07F * hold);
                for (int i = 0; i < 5; i++) tail[i].rotY(Mth.sin(age * 0.5F - i * 0.6F) * 0.12F * hold * (1.0F + 0.3F * i));
                tail[0].rotX(-0.12F * hold);
            }
        }

        // ---- membrana: se abre petalo a petalo desde el centro y tiembla un poco abierta ----
        for (int k = 0; k < PETALS; k++) {
            float delay = Math.abs(k - 3) * 0.10F;
            float o = sstep((frill - delay) / (1.0F - 0.30F));
            petal[k].rotX(1.0F * o + Mth.sin(age * 0.8F + k * 1.3F) * 0.04F * o);
        }

        // ---- sentado / dormido ----
        if (both > 0.0F) {
            body.move(Mth.lerp(sleep, sitDrop, sleepDrop) * both);
            for (int s = 0; s < 2; s++) {
                float th = Mth.lerp(sleep, -1.17F, -1.35F), sh = Mth.lerp(sleep, 0.62F, 0.95F), me = Mth.lerp(sleep, -0.90F, -1.25F), fo = Mth.lerp(sleep, 1.45F, 1.70F);
                thigh[s].p.xRot = Mth.lerp(both, thigh[s].p.xRot, thigh[s].rx + th);
                shin[s].p.xRot = Mth.lerp(both, shin[s].p.xRot, shin[s].rx + sh);
                meta[s].p.xRot = Mth.lerp(both, meta[s].p.xRot, meta[s].rx + me);
                foot[s].p.xRot = Mth.lerp(both, foot[s].p.xRot, foot[s].rx + fo);
                arm[s].p.xRot = Mth.lerp(both, arm[s].p.xRot, arm[s].rx - Mth.lerp(sleep, 0.5F, 0.4F));
                arm2[s].p.xRot = Mth.lerp(both, arm2[s].p.xRot, arm2[s].rx + 0.6F * sleep);
            }
            body.p.xRot = Mth.lerp(both, body.p.xRot, body.rx + 0.12F * (1.0F - sleep));
            // sentado: cuello erguido y atento; dormido: cuello estirado, cabeza apoyada en el suelo y cola enroscada hacia un lado
            neck1.p.xRot = Mth.lerp(both, neck1.p.xRot, neck1.rx + Mth.lerp(sleep, -0.2F, 0.85F));
            neck2.p.xRot = Mth.lerp(both, neck2.p.xRot, neck2.rx + Mth.lerp(sleep, 0.3F, -0.35F));
            head.p.xRot = Mth.lerp(both, head.p.xRot, head.rx + Mth.lerp(sleep, 0.1F, 0.35F));
            neck1.p.yRot = Mth.lerp(both, neck1.p.yRot, neck1.ry - 0.55F * sleep);
            head.p.yRot = Mth.lerp(both, head.p.yRot, head.ry);
            for (int i = 0; i < 5; i++) {
                tail[i].p.xRot = Mth.lerp(both, tail[i].p.xRot, tail[i].rx + 0.05F * sleep);
                tail[i].p.yRot = Mth.lerp(both, tail[i].p.yRot, tail[i].ry + 0.35F * sleep);
            }
            jaw.p.xRot = Mth.lerp(sleep, jaw.p.xRot, jaw.rx);
            chest.rotX(Mth.sin(age * 0.07F) * 0.022F * sleep);
        }
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
