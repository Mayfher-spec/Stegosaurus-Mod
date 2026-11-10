package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Carnotauro: modelo de ModelPart (mismo tipo de render que el adulto del estegosaurio). Cuerpo, cola de 4 tramos, cuello, cabeza con cuernos, mandibula, brazos diminutos y patas de 3 huesos. */
public final class CarnotaurusModel<T extends CarnotaurusEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "carnotaurus"), "main");
    private static final float BODY_Y = -11.2F;
    private static final float R_TAIL1 = -0.04F, R_TAIL2 = 0.03F, R_TAIL3 = 0.04F, R_TAIL4 = 0.05F;
    private static final float R_NECK = -0.45F, R_HEAD = 0.55F, R_ARM = -0.6F, R_THIGH = -0.3F, R_SHIN = 0.75F, R_FOOT = -0.45F;
    private final ModelPart root, body, tail1, tail2, tail3, tail4, neck, head, jaw, armL, armR, thighL, shinL, footL, thighR, shinR, footR;

    public CarnotaurusModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail1 = body.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
        this.tail4 = tail3.getChild("tail4");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        this.armL = body.getChild("arm_l");
        this.armR = body.getChild("arm_r");
        this.thighL = body.getChild("thigh_l");
        this.shinL = thighL.getChild("shin_l");
        this.footL = shinL.getChild("foot_l");
        this.thighR = body.getChild("thigh_r");
        this.shinR = thighR.getChild("shin_r");
        this.footR = shinR.getChild("foot_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9F, -12F, -14F, 18F, 22F, 34F), PartPose.offset(0F, -11.2F, 0F));
        PartDefinition part_tail1 = part_body.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(104, 0).addBox(-6.5F, -6F, 0F, 13F, 12F, 20F), PartPose.offsetAndRotation(0F, -3F, 19F, -0.04F, 0F, 0F));
        PartDefinition part_tail2 = part_tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(170, 0).addBox(-5F, -5F, 0F, 10F, 10F, 20F), PartPose.offsetAndRotation(0F, 0F, 20F, 0.03F, 0F, 0F));
        PartDefinition part_tail3 = part_tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(190, 56).addBox(-3.5F, -3.5F, 0F, 7F, 7F, 18F), PartPose.offsetAndRotation(0F, 0F, 20F, 0.04F, 0F, 0F));
        PartDefinition part_tail4 = part_tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(110, 85).addBox(-2F, -2.5F, 0F, 4F, 5F, 14F), PartPose.offsetAndRotation(0F, 0F, 18F, 0.05F, 0F, 0F));
        PartDefinition part_neck = part_body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 56).addBox(-5.5F, -7F, -15F, 11F, 13F, 16F), PartPose.offsetAndRotation(0F, -5F, -13F, -0.45F, 0F, 0F));
        PartDefinition part_head = part_neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(138, 56).addBox(-6.5F, -6.5F, -13F, 13F, 13F, 13F).texOffs(146, 85).addBox(-5F, -3F, -23F, 10F, 8F, 10F).texOffs(66, 108).addBox(-6.7F, -8.3F, -12F, 13.4F, 2F, 9F), PartPose.offsetAndRotation(0F, -1F, -14F, 0.55F, 0F, 0F));
        PartDefinition part_jaw = part_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 85).addBox(-4.5F, 0.5F, -21F, 9F, 3F, 20F), PartPose.offset(0F, 3.5F, -1F));
        PartDefinition part_horn_l = part_head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(111, 108).addBox(-2F, -6F, -2F, 4F, 6F, 4F), PartPose.offsetAndRotation(4.5F, -7F, -9F, 0F, 0F, -0.35F));
        PartDefinition part_horn_l2 = part_horn_l.addOrReplaceChild("horn_l2", CubeListBuilder.create().texOffs(143, 108).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offset(0F, -6F, 0F));
        PartDefinition part_horn_r = part_head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(127, 108).addBox(-2F, -6F, -2F, 4F, 6F, 4F), PartPose.offsetAndRotation(-4.5F, -7F, -9F, 0F, 0F, 0.35F));
        PartDefinition part_horn_r2 = part_horn_r.addOrReplaceChild("horn_r2", CubeListBuilder.create().texOffs(151, 108).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offset(0F, -6F, 0F));
        PartDefinition part_arm_l = part_body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(42, 108).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F), PartPose.offsetAndRotation(9F, 4F, -10F, -0.6F, 0F, 0F));
        PartDefinition part_arm_r = part_body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(54, 108).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F), PartPose.offsetAndRotation(-9F, 4F, -10F, -0.6F, 0F, 0F));
        PartDefinition part_thigh_l = part_body.addOrReplaceChild("thigh_l", CubeListBuilder.create().texOffs(54, 56).addBox(-4.5F, -2F, -6F, 9F, 17F, 12F), PartPose.offsetAndRotation(8F, 6F, 6F, -0.3F, 0F, 0F));
        PartDefinition part_shin_l = part_thigh_l.addOrReplaceChild("shin_l", CubeListBuilder.create().texOffs(58, 85).addBox(-3F, 0F, -3F, 6F, 15F, 7F), PartPose.offsetAndRotation(0F, 13F, 2F, 0.75F, 0F, 0F));
        PartDefinition part_foot_l = part_shin_l.addOrReplaceChild("foot_l", CubeListBuilder.create().texOffs(186, 85).addBox(-3.5F, 0F, -10F, 7F, 4F, 14F), PartPose.offsetAndRotation(0F, 14F, 1F, -0.45F, 0F, 0F));
        PartDefinition part_thigh_r = part_body.addOrReplaceChild("thigh_r", CubeListBuilder.create().texOffs(96, 56).addBox(-4.5F, -2F, -6F, 9F, 17F, 12F), PartPose.offsetAndRotation(-8F, 6F, 6F, -0.3F, 0F, 0F));
        PartDefinition part_shin_r = part_thigh_r.addOrReplaceChild("shin_r", CubeListBuilder.create().texOffs(84, 85).addBox(-3F, 0F, -3F, 6F, 15F, 7F), PartPose.offsetAndRotation(0F, 13F, 2F, 0.75F, 0F, 0F));
        PartDefinition part_foot_r = part_shin_r.addOrReplaceChild("foot_r", CubeListBuilder.create().texOffs(0, 108).addBox(-3.5F, 0F, -10F, 7F, 4F, 14F), PartPose.offsetAndRotation(0F, 14F, 1F, -0.45F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override public void setupAnim(T e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        float amp = Math.min(1.0F, limbSwingAmount * 1.5F);
        float sw = limbSwing * 0.55F;
        float sit = e.sitAnim, sleep = e.sleepAnim, both = Math.max(sit, sleep);
        float run = e.isCharging() ? 1.0F : 0.0F;
        // reposo
        body.y = BODY_Y; body.xRot = 0.0F; body.zRot = 0.0F; body.yRot = 0.0F;
        neck.xRot = R_NECK; neck.yRot = 0.0F;
        head.xRot = R_HEAD; head.yRot = 0.0F; head.zRot = 0.0F;
        jaw.xRot = 0.0F;
        // marcha: muslos y espinillas alternados, pie compensa
        float tl = Mth.cos(sw) * 0.85F * amp, tr = Mth.cos(sw + Mth.PI) * 0.85F * amp;
        float sl = Math.max(0.0F, Mth.sin(sw)) * 0.95F * amp, sr = Math.max(0.0F, Mth.sin(sw + Mth.PI)) * 0.95F * amp;
        thighL.xRot = R_THIGH + tl; thighR.xRot = R_THIGH + tr;
        shinL.xRot = R_SHIN + sl; shinR.xRot = R_SHIN + sr;
        footL.xRot = R_FOOT - tl * 0.55F - sl * 0.45F; footR.xRot = R_FOOT - tr * 0.55F - sr * 0.45F;
        body.y += -Math.abs(Mth.cos(sw)) * 1.3F * amp;
        body.xRot += 0.10F * run + 0.04F * amp;
        armL.xRot = R_ARM + Mth.sin(age * 0.07F) * 0.05F + tl * 0.25F; armR.xRot = R_ARM - Mth.sin(age * 0.07F) * 0.05F + tr * 0.25F;
        // cola: balanceo lento + contrapeso al andar
        float idle = Mth.sin(age * 0.06F), step = Mth.sin(sw) * 0.18F * amp;
        tail1.xRot = R_TAIL1; tail2.xRot = R_TAIL2; tail3.xRot = R_TAIL3; tail4.xRot = R_TAIL4;
        tail1.yRot = idle * 0.05F + step; tail2.yRot = idle * 0.08F + step * 1.2F; tail3.yRot = idle * 0.10F + step * 1.4F; tail4.yRot = idle * 0.12F + step * 1.6F;
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
                case 2 -> { neck.xRot += s * 1.05F; head.xRot += s * 0.40F; body.xRot += s * 0.28F; jaw.xRot += s * 0.2F; thighL.xRot += s * 0.3F; thighR.xRot += s * 0.3F; } // cabezazo
                case 3 -> { neck.xRot -= s * 0.55F; head.xRot -= s * 0.55F; jaw.xRot += s * 1.0F; body.xRot -= s * 0.12F; body.zRot += Mth.sin(age * 2.2F) * 0.025F * s; } // rugido
                default -> {}
            }
        }
        // sentado y dormido
        if (both > 0.0F) {
            float b = sit, z = sleep;
            body.y += 10.5F * b + 13.5F * z;
            body.xRot += -0.12F * b;
            thighL.xRot = Mth.lerp(both, thighL.xRot, Mth.lerp(z, -1.35F, -1.5F)); thighR.xRot = Mth.lerp(both, thighR.xRot, Mth.lerp(z, -1.35F, -1.5F));
            shinL.xRot = Mth.lerp(both, shinL.xRot, Mth.lerp(z, 1.9F, 2.2F)); shinR.xRot = Mth.lerp(both, shinR.xRot, Mth.lerp(z, 1.9F, 2.2F));
            footL.xRot = Mth.lerp(both, footL.xRot, -0.55F); footR.xRot = Mth.lerp(both, footR.xRot, -0.55F);
            tail1.xRot += -0.22F * b; tail1.yRot += 0.45F * z;
            neck.xRot += 0.75F * z; head.xRot += 0.45F * z; jaw.xRot = Mth.lerp(z, jaw.xRot, 0.0F);
            body.yScale = 1.0F + Mth.sin(age * 0.07F) * 0.015F * z;
        } else {
            body.yScale = 1.0F;
        }
        // cria: cabeza grande
        float hs = e.isAdultStage() ? 1.0F : 1.45F;
        head.xScale = hs; head.yScale = hs; head.zScale = hs;
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
