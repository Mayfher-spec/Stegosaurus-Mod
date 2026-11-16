package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.quests.CronistaEntity;
import net.minecraft.client.model.PlayerModel;
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

/** Humanoide con el esquema de una skin de jugador de brazos finos (64 de ancho; la mitad superior es la skin) y una cola de dinosaurio articulada de tres tramos (mitad inferior de la textura, 64x128). */
public final class CronistaModel extends PlayerModel<CronistaEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "cronista"), "main");
    private final ModelPart tail1, tail2, tail3;

    public CronistaModel(ModelPart root) {
        super(root, true);
        this.tail1 = root.getChild("body").getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, true);
        PartDefinition body = mesh.getRoot().getChild("body");
        // tramo 1 (nace en la pelvis, baja un poco) con una puas dorsal; ver tools/cronista/cronista.py para las zonas de la textura
        PartDefinition t1 = body.addOrReplaceChild("tail1", CubeListBuilder.create()
            .texOffs(0, 64).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 6.0F)
            .texOffs(24, 64).addBox(-0.5F, -3.5F, 1.0F, 1.0F, 1.5F, 2.0F)
            .texOffs(24, 64).addBox(-0.5F, -3.5F, 3.8F, 1.0F, 1.5F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, -0.12F, 0.0F, 0.0F));
        PartDefinition t2 = t1.addOrReplaceChild("tail2", CubeListBuilder.create()
            .texOffs(0, 76).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F)
            .texOffs(24, 72).addBox(-0.5F, -2.8F, 1.0F, 1.0F, 1.3F, 2.0F)
            .texOffs(24, 72).addBox(-0.5F, -2.8F, 3.8F, 1.0F, 1.3F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 5.8F, 0.08F, 0.0F, 0.0F));
        t2.addOrReplaceChild("tail3", CubeListBuilder.create()
            .texOffs(0, 88).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F)
            .texOffs(24, 80).addBox(-0.5F, -2.1F, 1.2F, 1.0F, 1.1F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 5.8F, 0.10F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(CronistaEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        float partial = age - e.tickCount;   // ageInTicks = tickCount + parcial
        float talk = e.talkBlend(partial), wave = e.waveBlend(partial);
        float walk = Mth.clamp(limbSwingAmount * 1.6F, 0.0F, 1.0F);   // 0 quieta .. 1 caminando
        float w = limbSwing * 0.6662F;

        // --- respiracion y reposo: brazos que cuelgan con un leve vaiven, cabeza con micro-movimientos
        float breath = Mth.sin(age * 0.09F), sway = Mth.sin(age * 0.05F);
        float rest = 1.0F - walk;
        this.rightArm.zRot += (0.05F + breath * 0.025F) * rest;
        this.leftArm.zRot -= (0.05F + breath * 0.025F) * rest;
        this.rightArm.xRot += Mth.sin(age * 0.07F + 0.6F) * 0.035F * rest;
        this.leftArm.xRot += Mth.sin(age * 0.07F + 2.2F) * 0.035F * rest;
        this.head.xRot += Mth.sin(age * 0.045F) * 0.025F * rest;
        this.head.zRot += sway * 0.02F * rest;

        // --- al caminar: la cadera gira con el paso y la cabeza compensa; la cola se contonea
        this.body.yRot += Mth.sin(w) * 0.12F * walk;
        this.head.yRot -= Mth.sin(w) * 0.06F * walk;

        // --- hablar: la mano derecha gesticula, la izquierda acompana y la cabeza asiente
        if (talk > 0.001F) {
            float t = age * 0.32F;
            this.rightArm.xRot = Mth.lerp(talk, this.rightArm.xRot, -1.05F + Mth.sin(t) * 0.30F);
            this.rightArm.zRot = Mth.lerp(talk, this.rightArm.zRot, 0.22F + Mth.cos(t * 0.8F) * 0.14F);
            this.leftArm.xRot = Mth.lerp(talk, this.leftArm.xRot, -0.55F + Mth.sin(t + 1.7F) * 0.24F);
            this.leftArm.zRot = Mth.lerp(talk, this.leftArm.zRot, -0.16F - Mth.cos(t * 0.9F) * 0.10F);
            this.head.xRot += Mth.sin(t * 1.1F) * 0.07F * talk;
        }
        // --- saludo: brazo derecho en alto moviendo la mano de lado a lado
        if (wave > 0.001F) {
            this.rightArm.xRot = Mth.lerp(wave, this.rightArm.xRot, -2.55F);
            this.rightArm.zRot = Mth.lerp(wave, this.rightArm.zRot, -0.15F + Mth.sin(age * 0.55F) * 0.38F);
            this.head.zRot += 0.10F * wave;
        }

        // --- cola: oleaje con retardo entre tramos; sube y baja un poco al andar y se agita mas al hablar
        float a = age * (0.09F + 0.05F * talk), amp = 1.0F + 0.8F * talk;
        this.tail1.yRot = (Mth.sin(a) * 0.12F * amp) + Mth.cos(w) * 0.42F * limbSwingAmount;
        this.tail2.yRot = (Mth.sin(a - 0.8F) * 0.16F * amp) + Mth.cos(w - 0.9F) * 0.52F * limbSwingAmount;
        this.tail3.yRot = (Mth.sin(a - 1.6F) * 0.22F * amp) + Mth.cos(w - 1.8F) * 0.64F * limbSwingAmount;
        this.tail1.xRot = -0.12F + Mth.sin(a * 0.7F) * 0.03F + Math.abs(Mth.sin(w)) * 0.10F * limbSwingAmount + (this.crouching ? 0.35F : 0.0F);
        this.tail2.xRot = 0.08F + Mth.sin(a * 0.7F - 0.6F) * 0.05F + Mth.sin(w - 0.7F) * 0.08F * limbSwingAmount;
        this.tail3.xRot = 0.10F + Mth.sin(a * 0.7F - 1.2F) * 0.07F + Mth.sin(w - 1.4F) * 0.10F * limbSwingAmount;

        // las capas exteriores de la skin copian el pose final
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.jacket.copyFrom(this.body);
        this.hat.copyFrom(this.head);
    }
}
