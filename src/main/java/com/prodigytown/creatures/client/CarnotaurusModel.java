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
 * Carnotauro (modelo de Blockbench convertido a ModelPart; ver CarnotaurusLayers). Dos mallas con los mismos huesos:
 * adulto y cria (chibi). Huesos: root > body > (chest > neck > head > jaw / horns, arm_left, arm_right), tail_1 > tail_2 > tail_3, leg_left, leg_right.
 */
public final class CarnotaurusModel<T extends CarnotaurusEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_ADULT = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "carnotaurus"), "adult");
    public static final ModelLayerLocation LAYER_CHIBI = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "carnotaurus"), "chibi");
    public static LayerDefinition createAdultLayer() { return CarnotaurusLayers.adult(); }
    public static LayerDefinition createChibiLayer() { return CarnotaurusLayers.chibi(); }

    private final boolean adult;
    private final ModelPart root, body, chest, neck, head, jaw, armL, armR, tail1, tail2, tail3, legL, legR;
    private final float restBodyY;

    public CarnotaurusModel(ModelPart bakedRoot, boolean adult) {
        this.adult = adult;
        this.root = bakedRoot.getChild("root");
        this.body = root.getChild("body");
        this.chest = body.getChild("chest");
        this.neck = chest.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        this.armL = chest.getChild("arm_left");
        this.armR = chest.getChild("arm_right");
        this.tail1 = body.getChild("tail_1");
        this.tail2 = tail1.getChild("tail_2");
        this.tail3 = tail2.getChild("tail_3");
        this.legL = body.getChild("leg_left");
        this.legR = body.getChild("leg_right");
        this.restBodyY = body.y;
    }

    @Override public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        float amp = Math.min(1.0F, limbSwingAmount * 1.5F);
        float sw = limbSwing * (adult ? 0.55F : 0.85F);
        float stride = adult ? 0.55F : 0.8F;
        float sit = e.sitAnim, sleep = e.sleepAnim, both = Math.max(sit, sleep);
        float run = e.isCharging() ? 1.0F : 0.0F;
        // reposo
        body.y = restBodyY; body.xRot = 0.0F; body.zRot = 0.0F; body.yRot = 0.0F;
        chest.yScale = 1.0F;
        neck.xRot = 0.0F; neck.yRot = 0.0F;
        head.xRot = 0.0F; head.yRot = 0.0F; head.zRot = 0.0F;
        jaw.xRot = 0.0F;
        legL.yScale = 1.0F; legR.yScale = 1.0F;
        // marcha: patas alternadas, brazos y cola de contrapeso
        float tl = Mth.cos(sw) * stride * amp, tr = Mth.cos(sw + Mth.PI) * stride * amp;
        legL.xRot = tl; legR.xRot = tr;
        body.y -= Math.abs(Mth.cos(sw)) * (adult ? 1.1F : 0.7F) * amp;
        body.xRot += 0.10F * run + 0.03F * amp;
        float breathe = Mth.sin(age * 0.07F);
        armL.xRot = breathe * 0.05F + tl * 0.25F; armR.xRot = -breathe * 0.05F + tr * 0.25F;
        float idle = Mth.sin(age * 0.06F), step = Mth.sin(sw) * 0.16F * amp;
        tail1.xRot = 0.0F; tail2.xRot = 0.0F; tail3.xRot = 0.0F;
        tail1.yRot = idle * 0.05F + step; tail2.yRot = idle * 0.08F + step * 1.2F; tail3.yRot = idle * 0.10F + step * 1.4F;
        // mirada (no al dormir)
        float look = 1.0F - sleep;
        neck.yRot = yaw * Mth.DEG_TO_RAD * 0.35F * look;
        head.yRot = yaw * Mth.DEG_TO_RAD * 0.55F * look;
        head.xRot += pitch * Mth.DEG_TO_RAD * 0.6F * look;
        neck.xRot += Mth.sin(age * 0.05F) * 0.015F;
        jaw.xRot = 0.04F + Mth.sin(age * 0.09F) * 0.02F;
        // mordida del ataque basico
        float atk = e.attackAnim;
        if (atk > 0.0F) {
            float s = Mth.sin(atk * Mth.PI);
            jaw.xRot += s * 0.85F; neck.xRot += s * 0.35F; head.xRot += s * 0.15F; body.xRot += s * 0.06F;
        }
        // habilidades (el tipo y el progreso vienen del servidor)
        int kind = e.getAbilityKind();
        if (kind != 0) {
            float p = e.getAbilityProgress();
            float s = Mth.sin(p * Mth.PI);
            switch (kind) {
                case 1 -> { neck.xRot += 0.30F; head.xRot += 0.10F; body.xRot += 0.18F; jaw.xRot += 0.15F; } // embestida
                case 2 -> { neck.xRot += s * 1.05F; head.xRot += s * 0.40F; body.xRot += s * 0.28F; jaw.xRot += s * 0.2F; legL.xRot += s * 0.3F; legR.xRot += s * 0.3F; } // cabezazo
                case 3 -> { neck.xRot -= s * 0.55F; head.xRot -= s * 0.55F; jaw.xRot += s * 1.0F; body.xRot -= s * 0.12F; body.zRot += Mth.sin(age * 2.2F) * 0.025F * s; } // rugido
                default -> {}
            }
        }
        // sentado y dormido: el cuerpo baja hasta apoyarse, las patas se pliegan hacia delante
        if (both > 0.0F) {
            float drop = adult ? Mth.lerp(sleep, 14.0F, 15.5F) : Mth.lerp(sleep, 4.6F, 5.6F);
            body.y += drop * both;
            float fold = Mth.lerp(sleep, -1.2F, -0.9F);
            legL.xRot = Mth.lerp(both, legL.xRot, fold); legR.xRot = Mth.lerp(both, legR.xRot, fold);
            float ys = Mth.lerp(sleep, 0.75F, 0.5F);
            legL.yScale = Mth.lerp(both, 1.0F, ys); legR.yScale = legL.yScale;
            body.xRot += -0.10F * sit;
            tail1.xRot += -0.22F * both; tail2.xRot += -0.12F * both; tail1.yRot += 0.45F * sleep;
            neck.xRot += 0.75F * sleep; head.xRot += 0.45F * sleep; jaw.xRot = Mth.lerp(sleep, jaw.xRot, 0.0F);
            chest.yScale = 1.0F + Mth.sin(age * 0.07F) * 0.015F * sleep;
        }
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
