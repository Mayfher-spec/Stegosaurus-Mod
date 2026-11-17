package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO por tools/compy/mexport.py (esqueleto propio del Compy: cuello en S, cola de 6 tramos, patas de 4 tramos + 3 dedos). No editar a mano. */
final class CompyLayers {
    private CompyLayers() {}
    static final int TEX_W = 256;
    static final int TEX_H = 128;
    /** Altura (unidades de modelo) del pivote del cuerpo sobre el suelo en reposo. */
    static final float BODY_Y = 23.617F;

    static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part_root = root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition part_body = part_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -5.5F, -10F, 10F, 11F, 20F).texOffs(141, 0).addBox(-3.8F, 5.1F, -7F, 7.6F, 1.6F, 15F).texOffs(187, 61).addBox(-0.6F, -6.4F, 4.6F, 1.2F, 1.6F, 2.4F).texOffs(198, 61).addBox(-0.6F, -6.6F, -0.4F, 1.2F, 1.8F, 2.4F).texOffs(209, 61).addBox(-0.6F, -6.4F, -5.4F, 1.2F, 1.6F, 2.4F), PartPose.offset(0F, -23.617F, 0F));
        PartDefinition part_tail1 = part_body.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(33, 32).addBox(-3F, -3F, 0F, 6F, 6F, 9F).texOffs(220, 61).addBox(-0.5F, -4.2F, 3.38F, 1F, 1.4F, 2.2F), PartPose.offset(0F, -1.5F, 9.5F));
        PartDefinition part_tail2 = part_tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(95, 32).addBox(-2.5F, -2.5F, 0F, 5F, 5F, 8F).texOffs(229, 61).addBox(-0.5F, -3.7F, 2.76F, 1F, 1.4F, 2.2F), PartPose.offset(0F, 0F, 8F));
        PartDefinition part_tail3 = part_tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(122, 32).addBox(-2F, -2.5F, 0F, 4F, 5F, 8F).texOffs(238, 61).addBox(-0.5F, -3.7F, 2.76F, 1F, 1.4F, 2.2F), PartPose.offset(0F, 0F, 7F));
        PartDefinition part_tail4 = part_tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(210, 32).addBox(-2F, -2F, 0F, 4F, 4F, 8F).texOffs(247, 61).addBox(-0.5F, -3.2F, 2.76F, 1F, 1.4F, 2.2F), PartPose.offset(0F, 0F, 7F));
        PartDefinition part_tail5 = part_tail4.addOrReplaceChild("tail5", CubeListBuilder.create().texOffs(235, 32).addBox(-1.5F, -2F, 0F, 3F, 4F, 7F), PartPose.offset(0F, 0F, 7F));
        PartDefinition part_tail6 = part_tail5.addOrReplaceChild("tail6", CubeListBuilder.create().texOffs(26, 49).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 7F), PartPose.offset(0F, 0F, 6F));
        PartDefinition part_chest = part_body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(61, 0).addBox(-5F, -5.5F, -8F, 10F, 10.8F, 10F).texOffs(124, 49).addBox(-3.6F, 5.1F, -7F, 7.2F, 1.4F, 7F).texOffs(0, 71).addBox(-0.6F, -6.4F, -3.4F, 1.2F, 1.6F, 2.4F).texOffs(11, 71).addBox(-0.6F, -6.2F, -8.2F, 1.2F, 1.4F, 2.2F), PartPose.offset(0F, -0.8F, -8F));
        PartDefinition part_neck1 = part_chest.addOrReplaceChild("neck1", CubeListBuilder.create().texOffs(0, 32).addBox(-3F, -2.9F, -9.5F, 6F, 5.6F, 9.5F).texOffs(47, 49).addBox(-2.3F, 1.5F, -8.5F, 4.6F, 1.6F, 7.5F), PartPose.offsetAndRotation(0F, -3.8F, -7.5F, -0.9F, 0F, 0F));
        PartDefinition part_neck2 = part_neck1.addOrReplaceChild("neck2", CubeListBuilder.create().texOffs(64, 32).addBox(-2.6F, -2.5F, -8.5F, 5.2F, 5F, 8.5F).texOffs(155, 49).addBox(-2F, 1.3F, -7.8F, 4F, 1.6F, 7F), PartPose.offsetAndRotation(0F, -0.4F, -9F, 0.62F, 0F, 0F));
        PartDefinition part_head = part_neck2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(147, 32).addBox(-3F, -2.6F, -6F, 6F, 5F, 8F).texOffs(178, 49).addBox(-2.1F, -1.8F, -11F, 4.2F, 3.6F, 5F).texOffs(94, 61).addBox(-1.6F, -1.4F, -14F, 3.2F, 2.8F, 3F).texOffs(23, 61).addBox(-3.3F, -3.5F, -5.5F, 6.6F, 1.3F, 5.5F).texOffs(74, 49).addBox(-1.95F, 2.4F, -14.5F, 3.9F, 1.1F, 8F).texOffs(130, 71).addBox(-1.2F, -2.3F, -14.2F, 2.4F, 1F, 2.2F), PartPose.offsetAndRotation(0F, 0F, -8F, 0.42F, 0F, 0F));
        PartDefinition part_jaw = part_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(102, 0).addBox(-1.5F, 0F, -13.4F, 3F, 1.9F, 15.4F).texOffs(99, 49).addBox(-1.3F, -0.9F, -14F, 2.6F, 0.9F, 9F).texOffs(199, 49).addBox(-0.8F, -0.7F, -11F, 1.6F, 0.7F, 8F), PartPose.offset(0F, 2.4F, -0.4F));
        PartDefinition part_arm_l = part_chest.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(50, 61).addBox(-1.2F, -0.4F, -1.2F, 2.4F, 5F, 2.4F), PartPose.offsetAndRotation(4.8F, 2.6F, -6.5F, 0.45F, 0F, -0.08F));
        PartDefinition part_arm2_l = part_arm_l.addOrReplaceChild("arm2_l", CubeListBuilder.create().texOffs(76, 61).addBox(-1F, -0.4F, -1F, 2F, 4.6F, 2F).texOffs(22, 71).addBox(-1.15F, 4F, -0.6F, 0.7F, 2.6F, 1.2F).texOffs(29, 71).addBox(-0.35F, 4F, -0.6F, 0.7F, 3F, 1.2F).texOffs(36, 71).addBox(0.45F, 4F, -0.6F, 0.7F, 2.6F, 1.2F), PartPose.offsetAndRotation(0F, 4F, 0F, 0.8F, 0F, 0F));
        PartDefinition part_thigh_l = part_body.addOrReplaceChild("thigh_l", CubeListBuilder.create().texOffs(188, 0).addBox(-2.7F, -1.4F, -3.5F, 5.4F, 9.2F, 7F), PartPose.offsetAndRotation(4.6F, 2.8F, 5F, -0.4F, 0F, 0F));
        PartDefinition part_shin_l = part_thigh_l.addOrReplaceChild("shin_l", CubeListBuilder.create().texOffs(176, 32).addBox(-1.9F, -0.6F, -1.9F, 3.8F, 8.8F, 3.8F), PartPose.offsetAndRotation(0F, 7.2F, -1F, 0.95F, 0F, 0F));
        PartDefinition part_meta_l = part_shin_l.addOrReplaceChild("meta_l", CubeListBuilder.create().texOffs(0, 49).addBox(-1.5F, -0.8F, -1.5F, 3F, 7.8F, 3F), PartPose.offsetAndRotation(0F, 7.6F, 0.8F, -0.65F, 0F, 0F));
        PartDefinition part_foot_l = part_meta_l.addOrReplaceChild("foot_l", CubeListBuilder.create().texOffs(220, 49).addBox(-2F, 0F, -3.2F, 4F, 2F, 6.4F), PartPose.offsetAndRotation(0F, 6.4F, 0F, 0.12F, 0F, 0F));
        PartDefinition part_toe0_l = part_foot_l.addOrReplaceChild("toe0_l", CubeListBuilder.create().texOffs(109, 61).addBox(-0.9F, -1F, -4F, 1.8F, 2F, 4F).texOffs(43, 71).addBox(-0.6F, -0.8F, -6.2F, 1.2F, 1.5F, 2.2F), PartPose.offset(0F, 1F, -3.2F));
        PartDefinition part_toe1_l = part_foot_l.addOrReplaceChild("toe1_l", CubeListBuilder.create().texOffs(122, 61).addBox(-0.9F, -1F, -4F, 1.8F, 2F, 4F).texOffs(54, 71).addBox(-0.6F, -0.8F, -6.2F, 1.2F, 1.5F, 2.2F), PartPose.offsetAndRotation(1.5F, 1F, -3.2F, 0F, -0.24F, 0F));
        PartDefinition part_toe2_l = part_foot_l.addOrReplaceChild("toe2_l", CubeListBuilder.create().texOffs(135, 61).addBox(-0.9F, -1F, -4F, 1.8F, 2F, 4F).texOffs(65, 71).addBox(-0.6F, -0.8F, -6.2F, 1.2F, 1.5F, 2.2F), PartPose.offsetAndRotation(-1.5F, 1F, -3.2F, 0F, 0.24F, 0F));
        PartDefinition part_arm_r = part_chest.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(63, 61).addBox(-1.2F, -0.4F, -1.2F, 2.4F, 5F, 2.4F), PartPose.offsetAndRotation(-4.8F, 2.6F, -6.5F, 0.45F, 0F, 0.08F));
        PartDefinition part_arm2_r = part_arm_r.addOrReplaceChild("arm2_r", CubeListBuilder.create().texOffs(85, 61).addBox(-1F, -0.4F, -1F, 2F, 4.6F, 2F).texOffs(76, 71).addBox(-1.15F, 4F, -0.6F, 0.7F, 2.6F, 1.2F).texOffs(83, 71).addBox(-0.35F, 4F, -0.6F, 0.7F, 3F, 1.2F).texOffs(90, 71).addBox(0.45F, 4F, -0.6F, 0.7F, 2.6F, 1.2F), PartPose.offsetAndRotation(0F, 4F, 0F, 0.8F, 0F, 0F));
        PartDefinition part_thigh_r = part_body.addOrReplaceChild("thigh_r", CubeListBuilder.create().texOffs(215, 0).addBox(-2.7F, -1.4F, -3.5F, 5.4F, 9.2F, 7F), PartPose.offsetAndRotation(-4.6F, 2.8F, 5F, -0.4F, 0F, 0F));
        PartDefinition part_shin_r = part_thigh_r.addOrReplaceChild("shin_r", CubeListBuilder.create().texOffs(193, 32).addBox(-1.9F, -0.6F, -1.9F, 3.8F, 8.8F, 3.8F), PartPose.offsetAndRotation(0F, 7.2F, -1F, 0.95F, 0F, 0F));
        PartDefinition part_meta_r = part_shin_r.addOrReplaceChild("meta_r", CubeListBuilder.create().texOffs(13, 49).addBox(-1.5F, -0.8F, -1.5F, 3F, 7.8F, 3F), PartPose.offsetAndRotation(0F, 7.6F, 0.8F, -0.65F, 0F, 0F));
        PartDefinition part_foot_r = part_meta_r.addOrReplaceChild("foot_r", CubeListBuilder.create().texOffs(0, 61).addBox(-2F, 0F, -3.2F, 4F, 2F, 6.4F), PartPose.offsetAndRotation(0F, 6.4F, 0F, 0.12F, 0F, 0F));
        PartDefinition part_toe0_r = part_foot_r.addOrReplaceChild("toe0_r", CubeListBuilder.create().texOffs(148, 61).addBox(-0.9F, -1F, -4F, 1.8F, 2F, 4F).texOffs(97, 71).addBox(-0.6F, -0.8F, -6.2F, 1.2F, 1.5F, 2.2F), PartPose.offset(0F, 1F, -3.2F));
        PartDefinition part_toe1_r = part_foot_r.addOrReplaceChild("toe1_r", CubeListBuilder.create().texOffs(161, 61).addBox(-0.9F, -1F, -4F, 1.8F, 2F, 4F).texOffs(108, 71).addBox(-0.6F, -0.8F, -6.2F, 1.2F, 1.5F, 2.2F), PartPose.offsetAndRotation(-1.5F, 1F, -3.2F, 0F, 0.24F, 0F));
        PartDefinition part_toe2_r = part_foot_r.addOrReplaceChild("toe2_r", CubeListBuilder.create().texOffs(174, 61).addBox(-0.9F, -1F, -4F, 1.8F, 2F, 4F).texOffs(119, 71).addBox(-0.6F, -0.8F, -6.2F, 1.2F, 1.5F, 2.2F), PartPose.offsetAndRotation(1.5F, 1F, -3.2F, 0F, -0.24F, 0F));
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }
}
