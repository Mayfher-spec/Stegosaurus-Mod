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
 * Dos mallas con los mismos huesos (adulto y cria). Huesos: root > body > (tail1..tail5, chest > (neck1 > neck2 > head > (jaw, horn_*), arm_*), thigh_* > shin_* > meta_* > foot_* > toe0..2_*).
 * Los huesos se animan SUMANDO a su pose de reposo (la de la malla), para no perder la postura de diseno.
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
        if (adult) { sitDrop = 39.16F; sleepDrop = 36.38F; sleepNeck1 = 0.95F; sleepNeck2 = -0.25F; sleepHead = 0.0F; }
        else { sitDrop = 20.36F; sleepDrop = 20.36F; sleepNeck1 = 0.55F; sleepNeck2 = -0.10F; sleepHead = -0.10F; }
    }

    private static Bone add(java.util.List<Bone> list, ModelPart p) { Bone bn = new Bone(p); list.add(bn); return bn; }

    @Override public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        for (Bone bn : all) bn.reset();
        float amp = Math.min(1.0F, limbSwingAmount * 1.5F);
        float sit = e.sitAnim, sleep = e.sleepAnim, both = Math.max(sit, sleep);
        float walk = (1.0F - both);
        float run = e.isCharging() ? 1.0F : 0.0F;
        float sw = limbSwing * (adult ? 0.62F : 0.9F);
        float stride = (0.45F + 0.15F * run) * amp * walk;
        float bob = adult ? 7.0F : 3.6F;

        // ---- marcha bipeda: muslo -> tibia -> metatarso -> pie (el pie se mantiene plano) ----
        for (int s = 0; s < 2; s++) {
            float ph = sw + (s == 0 ? 0.0F : Mth.PI);
            float sn = Mth.sin(ph), cs = Mth.cos(ph);
            float lift = Math.max(0.0F, -cs);
            float th = stride * sn;
            float sh = 0.55F * lift * amp * walk;
            float me = -0.30F * lift * amp * walk;
            thigh[s].rotX(th); shin[s].rotX(sh); meta[s].rotX(me); foot[s].rotX(-(th + sh + me));
        }
        body.move((0.5F - 0.5F * Mth.cos(sw * 2.0F)) * bob * amp * walk);
        body.rotX(0.02F * amp * walk + 0.13F * run);
        body.rotY(Mth.sin(sw) * 0.035F * amp * walk);

        // ---- cuello, cabeza y cola ----
        float breathe = Mth.sin(age * 0.07F);
        chest.rotX(breathe * 0.012F);
        neck1.rotX(Mth.sin(sw * 2.0F) * 0.03F * amp * walk + breathe * 0.01F + 0.22F * run);
        neck2.rotX(-Mth.sin(sw * 2.0F) * 0.02F * amp * walk);
        head.rotX(-0.10F * run);
        for (int i = 0; i < 5; i++) {
            float k = 1.0F + 0.28F * i;
            tail[i].rotY((Mth.sin(sw - i * 0.7F) * 0.11F * amp * walk + Mth.sin(age * 0.05F - i * 0.5F) * 0.035F) * k);
            tail[i].rotX(-0.04F * run * (i == 0 ? 1.0F : 0.5F));
        }
        // brazos pequenos: balanceo suave
        for (int s = 0; s < 2; s++) {
            float ph = sw + (s == 0 ? Mth.PI : 0.0F);
            arm[s].rotX(Mth.sin(ph) * 0.18F * amp * walk + breathe * 0.04F * (s == 0 ? 1 : -1));
            arm2[s].rotX(0.10F * Mth.sin(age * 0.09F + s));
        }

        // ---- mirada (no al dormir) ----
        float look = 1.0F - sleep;
        float yawR = Mth.clamp(yaw, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        neck1.rotY(yawR * 0.22F * look); neck2.rotY(yawR * 0.30F * look); head.rotY(yawR * 0.30F * look);
        head.rotX(Mth.clamp(pitch, -45.0F, 45.0F) * Mth.DEG_TO_RAD * 0.6F * look);
        jaw.rotX(0.04F + Mth.sin(age * 0.09F) * 0.02F);

        // ---- mordida del ataque basico ----
        float atk = e.attackAnim;
        if (atk > 0.0F) {
            float s = Mth.sin(atk * Mth.PI);
            jaw.rotX(s * 0.62F); neck1.rotX(s * 0.32F); neck2.rotX(-s * 0.10F); head.rotX(s * 0.16F); body.rotX(s * 0.05F);
        }
        // ---- habilidades (tipo y progreso vienen del servidor) ----
        int kind = e.getAbilityKind();
        if (kind != 0) {
            float p = e.getAbilityProgress();
            float s = Mth.sin(p * Mth.PI);
            switch (kind) {
                case 1 -> { neck1.rotX(0.12F); head.rotX(0.06F); body.rotX(0.04F); jaw.rotX(0.18F); tail[0].rotX(-0.08F); }              // embestida
                case 2 -> { neck1.rotX(s * 0.55F); neck2.rotX(-s * 0.15F); head.rotX(s * 0.30F); body.rotX(s * 0.20F); jaw.rotX(s * 0.20F); // cabezazo
                            for (int i = 0; i < 2; i++) { thigh[i].rotX(-s * 0.18F); foot[i].rotX(s * 0.18F); } }
                case 3 -> { neck1.rotX(-s * 0.30F); neck2.rotX(-s * 0.35F); head.rotX(-s * 0.45F); jaw.rotX(s * 0.85F); body.rotX(-s * 0.10F); // rugido
                            body.p.zRot += Mth.sin(age * 2.2F) * 0.025F * s; tail[0].rotX(-0.15F * s); }
                default -> {}
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
            for (int i = 0; i < 5; i++) tail[i].rotY(Math.min(0.70F, 0.45F + 0.10F * i) * sleep);
            neck1.rotX(sleepNeck1 * sleep); neck2.rotX(sleepNeck2 * sleep); head.rotX(sleepHead * sleep);
            neck1.rotY(0.30F * sleep); jaw.p.xRot = Mth.lerp(sleep, jaw.p.xRot, jaw.rx);
            chest.rotX(Mth.sin(age * 0.07F) * 0.02F * sleep);
        }
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
