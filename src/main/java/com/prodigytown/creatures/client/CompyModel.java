package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CompyEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Compy: esqueleto propio y diminuto (cuello en S de 2 tramos, cola de 6 tramos, patas de 4 tramos + 3 dedos, brazos de 2 tramos con garras, mandibula articulada).
 * Huesos: root > body > (tail1..tail6, chest > (neck1 > neck2 > head > jaw, arm_* > arm2_*), thigh_* > shin_* > meta_* > foot_* > toe0..2_*).
 * Los huesos se animan SUMANDO a su pose de reposo (la de la malla). La marcha usa la fase de zancada que mantiene la entidad (ligada a la distancia recorrida).
 * Animaciones: idle (respiracion, parpadeo por textura, giros de cabeza), caminar, correr, mordida, golpe, muerte, sentado, dormido, mirar alrededor y comer.
 */
public final class CompyModel<T extends CompyEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "compy"), "main");
    public static LayerDefinition createLayer() { return CompyLayers.create(); }

    private static final int TAILS = 6;
    /** Descensos del cuerpo (unidades de modelo) medidos con tools/compy/mpose.py para que sentado y dormido apoye en el suelo. */
    private static final float SIT_DROP = 10.21F, SLEEP_DROP = 13.28F;

    /** Hueso con su pose de reposo guardada. */
    private static final class Bone {
        final ModelPart p; final float x, y, z, rx, ry, rz;
        Bone(ModelPart p) { this.p = p; x = p.x; y = p.y; z = p.z; rx = p.xRot; ry = p.yRot; rz = p.zRot; }
        void reset() { p.x = x; p.y = y; p.z = z; p.xRot = rx; p.yRot = ry; p.zRot = rz; }
        void rotX(float d) { p.xRot += d; }
        void rotY(float d) { p.yRot += d; }
        void rotZ(float d) { p.zRot += d; }
        void move(float dy) { p.y += dy; }
        /** hacia la pose de reposo + (dx, dy, dz) con peso k */
        void blend(float k, float dx, float dy, float dz) { p.xRot = Mth.lerp(k, p.xRot, rx + dx); p.yRot = Mth.lerp(k, p.yRot, ry + dy); p.zRot = Mth.lerp(k, p.zRot, rz + dz); }
    }

    private final ModelPart root;
    private final Bone body, chest, neck1, neck2, head, jaw;
    private final Bone[] tail = new Bone[TAILS];
    private final Bone[] arm = new Bone[2], arm2 = new Bone[2];
    private final Bone[] thigh = new Bone[2], shin = new Bone[2], meta = new Bone[2], foot = new Bone[2];
    private final Bone[][] toe = new Bone[2][3];
    private final Bone[] all;
    private final float restDepth;

    public CompyModel(ModelPart bakedRoot) {
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
        for (int i = 0; i < TAILS; i++) { t = t.getChild("tail" + (i + 1)); tail[i] = add(list, t); }
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
    }

    private static float rotY2(float y, float z, float a) { return y * Mth.cos(a) - z * Mth.sin(a); }
    private static float smax(float a, float b) { return 0.5F * (a + b + (float) Math.sqrt((a - b) * (a - b) + 1.0F)); }
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
        float death = Mth.clamp((e.deathTime + pt) / 20.0F, 0.0F, 1.0F);
        float walk = (1.0F - both) * (1.0F - death);
        float ph = Mth.lerp(pt, e.gaitO, e.gait);
        float spd = Mth.lerp(pt, e.speedAnimO, e.speedAnim);
        float run = Mth.lerp(pt, e.runAnimO, e.runAnim) * walk;
        float aggro = Mth.lerp(pt, e.aggroAnimO, e.aggroAnim) * walk;
        float turn = Mth.clamp(Mth.lerp(pt, e.turnAnimO, e.turnAnim) / 7.0F, -1.0F, 1.0F);
        float air = sstep(Mth.lerp(pt, e.airAnimO, e.airAnim)) * walk;
        float mv = sstep(spd / 0.03F) * walk * (1.0F - air);
        float breathe = Mth.sin(age * 0.16F);
        float lean = (0.05F * mv + 0.20F * run + 0.08F * aggro) * walk;

        // ---- marcha bipeda: muslo -> tibia -> metatarso -> pie (el pie se mantiene plano aunque se incline el torso) ----
        float d0 = 0.0F, d1 = 0.0F;
        for (int s = 0; s < 2; s++) {
            float p = ph + (s == 0 ? 0.0F : Mth.PI);
            float sn = Mth.sin(p), cs = Mth.cos(p);
            float lift = Math.max(0.0F, -cs);
            float th = (0.50F + 0.22F * run) * sn * mv;
            float sh = (0.70F + 0.35F * run) * lift * mv;
            float me = -(0.40F + 0.20F * run) * lift * mv;
            thigh[s].rotX(th - lean * 0.7F); shin[s].rotX(sh); meta[s].rotX(me); foot[s].rotX(-(lean * 0.3F + th + sh + me));
            float dd = legDepth(s, lean, th - lean * 0.7F, sh, me);
            if (s == 0) d0 = dd; else d1 = dd;
        }
        body.move((restDepth - smax(d0, d1)) * (1.0F - air));
        body.rotX(lean);
        body.rotY(Mth.sin(ph) * (0.05F + 0.05F * run) * mv);
        chest.rotY(-Mth.sin(ph) * 0.05F * mv);
        body.p.zRot += Mth.sin(ph) * 0.03F * mv - turn * 0.10F * mv;
        chest.rotX(breathe * 0.025F * (1.0F + aggro));

        // ---- cuello, cabeza y cola (la cola sube al correr para equilibrar el cuerpo) ----
        neck1.rotX(Mth.sin(ph * 2.0F) * 0.05F * mv + breathe * 0.015F + 0.14F * run + 0.08F * aggro);
        neck2.rotX(-Mth.sin(ph * 2.0F) * 0.07F * mv - 0.05F * aggro);   // cabeceo de ave al caminar
        head.rotX(-0.10F * run - lean * 0.6F + Mth.sin(ph * 2.0F + 0.6F) * 0.05F * mv);
        for (int i = 0; i < TAILS; i++) {
            float k = 1.0F + 0.25F * i;
            float tw = (Mth.sin(ph - i * 0.6F) * 0.12F * mv + Mth.sin(age * 0.06F - i * 0.5F) * 0.05F) * k;
            tail[i].rotY(tw - turn * 0.16F * (0.4F + 0.10F * (i + 1)) * (0.3F + 0.7F * Mth.clamp(spd / 0.25F, 0.0F, 1.0F)) * mv);
            tail[i].rotX((-0.07F * run - 0.04F * aggro) * (i == 0 ? 1.0F : 0.6F) - lean * (i == 0 ? 0.7F : 0.0F) + Mth.sin(ph * 2.0F - i * 0.6F) * 0.025F * mv);
        }
        // brazos: balanceo suave; al correr se pliegan contra el pecho; al acechar se adelantan
        for (int s = 0; s < 2; s++) {
            float p = ph + (s == 0 ? Mth.PI : 0.0F);
            arm[s].rotX(Mth.sin(p) * 0.20F * mv * (1.0F - run) + breathe * 0.05F * (s == 0 ? 1 : -1) - 0.20F * aggro + 0.50F * run);
            arm2[s].rotX(0.12F * Mth.sin(age * 0.11F + s) + 0.30F * aggro + 0.55F * run);
        }

        // ---- mirada (no al dormir) ----
        float look = 1.0F - sleep;
        float yawR = Mth.clamp(yaw, -70.0F, 70.0F) * Mth.DEG_TO_RAD;
        neck1.rotY(yawR * 0.22F * look); neck2.rotY(yawR * 0.30F * look); head.rotY(yawR * 0.35F * look);
        head.rotX(Mth.clamp(pitch, -45.0F, 45.0F) * Mth.DEG_TO_RAD * 0.6F * look);
        jaw.rotX(0.05F + Mth.sin(age * 0.11F) * 0.015F + aggro * 0.20F + 0.05F * run + Mth.sin(age * 2.4F) * 0.03F * aggro);

        // ---- reposo vivo: giros de cabeza curiosos (mirar alrededor), ladeos de ave, respingos de cola ----
        float idle = (1.0F - mv) * walk * (1.0F - air);
        float curious = Mth.sin(age * 0.045F + e.getId()) * Mth.sin(age * 0.023F + 1.7F * e.getId());
        head.rotY(curious * 0.50F * idle * look); neck2.rotY(curious * 0.20F * idle * look); neck1.rotY(curious * 0.10F * idle * look);
        float tilt = win(Mth.sin(age * 0.05F + e.getId() * 0.7F) * 0.5F + 0.5F, 0.80F, 0.88F, 0.92F, 1.0F);
        head.rotZ(tilt * 0.35F * idle * look);
        float twitch = win(Mth.sin(age * 0.031F + e.getId() * 2.3F) * 0.5F + 0.5F, 0.90F, 0.94F, 0.96F, 1.0F);
        for (int i = 0; i < TAILS; i++) tail[i].rotY(twitch * 0.18F * idle * (i + 1) * 0.35F * Mth.sin(age * 1.5F));

        // ---- en el aire: piernas recogidas, cabeza al frente ----
        if (air > 0.0F) {
            float fall = Mth.clamp(-e.airVel * 1.5F, 0.0F, 1.0F);
            for (int s = 0; s < 2; s++) {
                thigh[s].rotX((-0.60F + 0.50F * fall) * air); shin[s].rotX((0.75F - 0.30F * fall) * air); meta[s].rotX(-0.45F * air); foot[s].rotX(0.30F * air);
                arm[s].rotX(-0.30F * air); arm2[s].rotX(0.25F * air);
            }
            body.rotX((-0.10F + 0.18F * fall) * air);
            for (int i = 0; i < TAILS; i++) tail[i].rotX(-0.05F * air);
            jaw.rotX(0.22F * air);
        }

        // ---- mordida rapida: ladea la cabeza, abre y se lanza; el dano cae en el cierre ----
        if (e.biteT >= 0) {
            float atk = Mth.clamp((Mth.lerp(pt, e.biteTO, e.biteT) + 0.5F) / (float) CompyEntity.BITE_LEN, 0.0F, 1.0F);
            float wind = win(atk, 0.0F, 0.22F, 0.26F, 0.40F);
            float strike = win(atk, 0.18F, 0.36F, 0.52F, 1.0F);
            float open = win(atk, 0.0F, 0.20F, 0.50F, 0.70F);
            float snap = win(atk, 0.50F, 0.60F, 0.66F, 0.86F);
            neck1.rotX(-0.25F * wind + 0.45F * strike); neck2.rotX(0.10F * wind - 0.14F * strike); head.rotX(-0.28F * wind + 0.20F * strike);
            neck1.rotY(0.12F * wind); head.rotY(0.16F * wind - 0.12F * strike);
            body.rotX(-0.04F * wind + 0.12F * strike); body.move(1.0F * strike);
            jaw.rotX(0.95F * open - 0.20F * snap);
        }
        // ---- golpe recibido ----
        if (e.hurtTime > 0) {
            float x = Mth.clamp((e.hurtTime - pt) / Math.max(1, e.hurtDuration), 0.0F, 1.0F);
            float hurt = x * x;
            neck1.rotX(-0.25F * hurt); head.rotX(-0.20F * hurt); jaw.rotX(0.40F * hurt); body.rotX(-0.08F * hurt); tail[0].rotX(0.14F * hurt);
            head.rotY(Mth.sin(age * 2.0F) * 0.14F * hurt);
            for (int s = 0; s < 2; s++) arm[s].rotX(-0.35F * hurt);
        }
        // ---- comer: picotazos rapidos hacia el suelo ----
        if (e.eatT >= 0) {
            float x = Mth.clamp((Mth.lerp(pt, e.eatTO, e.eatT) + 0.5F) / (float) CompyEntity.EAT_LEN, 0.0F, 1.0F);
            float dip = win(x, 0.0F, 0.15F, 0.80F, 1.0F);
            float peck = (0.5F + 0.5F * Mth.sin(x * Mth.PI * 10.0F)) * dip;
            neck1.rotX(0.55F * dip); neck2.rotX(-0.15F * dip + 0.20F * peck); head.rotX(0.45F * dip + 0.25F * peck);
            jaw.rotX(0.45F * Math.abs(Mth.sin(x * Mth.PI * 5.0F)) * dip);
            body.rotX(0.06F * dip);
        }

        // ---- sentado / dormido ----
        if (both > 0.0F) {
            body.move(Mth.lerp(sleep, SIT_DROP, SLEEP_DROP) * both);
            for (int s = 0; s < 2; s++) {
                thigh[s].blend(both, Mth.lerp(sleep, -1.17F, -1.35F), 0, 0);
                shin[s].blend(both, Mth.lerp(sleep, 0.62F, 0.95F), 0, 0);
                meta[s].blend(both, Mth.lerp(sleep, -0.90F, -1.25F), 0, 0);
                foot[s].blend(both, Mth.lerp(sleep, 1.45F, 1.70F), 0, 0);
                arm[s].blend(both, Mth.lerp(sleep, -0.5F, -0.4F), 0, 0);
                arm2[s].blend(both, 0.6F * sleep, 0, 0);
            }
            body.blend(both, 0.12F * (1.0F - sleep), 0, 0);
            // sentado: cuello erguido y atento; dormido: cuello estirado, cabeza apoyada en el suelo y cola enroscada hacia un lado
            neck1.blend(both, Mth.lerp(sleep, -0.2F, 0.85F), -0.55F * sleep, 0);
            neck2.blend(both, Mth.lerp(sleep, 0.3F, -0.35F), 0, 0);
            head.blend(both, Mth.lerp(sleep, 0.1F, 0.35F), 0, 0);
            for (int i = 0; i < TAILS; i++) tail[i].blend(both, 0.05F * sleep, 0.35F * sleep, 0);
            jaw.p.xRot = Mth.lerp(sleep, jaw.p.xRot, jaw.rx);
            chest.rotX(Mth.sin(age * 0.09F) * 0.03F * sleep);
        }

        // ---- muerte: se desploma de lado (el giro lo pone el render), con las patas encogidas, el cuello doblado y la cola enroscada ----
        if (death > 0.0F) {
            float k = sstep(death);
            body.move(SIT_DROP * 0.55F * k);
            for (int s = 0; s < 2; s++) {
                thigh[s].blend(k, -0.9F, 0, 0); shin[s].blend(k, 0.80F, 0, 0); meta[s].blend(k, -0.9F, 0, 0); foot[s].blend(k, 1.2F, 0, 0);
                arm[s].blend(k, -0.7F, 0, 0); arm2[s].blend(k, 0.8F, 0, 0);
            }
            neck1.blend(k, 0.5F, -0.35F, 0); neck2.blend(k, -0.1F, 0, 0); head.blend(k, 0.3F, 0, 0.25F);
            jaw.p.xRot = Mth.lerp(k, jaw.p.xRot, jaw.rx + 0.35F);
            for (int i = 0; i < TAILS; i++) tail[i].blend(k, 0, 0.22F, 0);
        }
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
