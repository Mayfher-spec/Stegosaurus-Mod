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
        float a = age * 0.07F, w = limbSwing * 0.6662F;
        // oleaje de la cola: cada tramo va un poco retrasado respecto al anterior; al caminar se contonea con las piernas
        this.tail1.yRot = Mth.sin(a) * 0.10F + Mth.cos(w) * 0.40F * limbSwingAmount;
        this.tail2.yRot = Mth.sin(a - 0.8F) * 0.14F + Mth.cos(w - 0.9F) * 0.50F * limbSwingAmount;
        this.tail3.yRot = Mth.sin(a - 1.6F) * 0.18F + Mth.cos(w - 1.8F) * 0.60F * limbSwingAmount;
        this.tail1.xRot = -0.12F + (this.crouching ? 0.35F : 0.0F);
    }
}
