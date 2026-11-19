package com.prodigytown.creatures.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERADO por tools/carnotaurus/armor2.py (armadura ligera del carnotauro: mismos huesos que el adulto, atlas propio). No editar a mano. */
final class CarnotaurusArmorLayers {
    private CarnotaurusArmorLayers() {}
    static final int TEX_W = 256;
    static final int TEX_H = 320;

    static LayerDefinition adult() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part_root = root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition part_body = part_root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 190).addBox(-20.592F, -32.696F, -25.52F, 41.184F, 2.928F, 10.44F).texOffs(107, 190).addBox(-20.592F, -33.062F, -17.864F, 41.184F, 2.928F, 10.44F).texOffs(0, 205).addBox(-20.592F, -33.428F, -10.208F, 41.184F, 2.928F, 10.44F).texOffs(103, 280).addBox(-25.872F, -26.962F, -31.784F, 51.744F, 1.83F, 3.944F).texOffs(0, 291).addBox(-25.872F, 27.45F, -31.784F, 51.744F, 1.83F, 3.944F).texOffs(112, 0).addBox(25.212F, -20.74F, -31.784F, 2.112F, 50.02F, 3.944F).texOffs(127, 0).addBox(-27.324F, -20.74F, -31.784F, 2.112F, 50.02F, 3.944F).texOffs(113, 291).addBox(-25.872F, -31.842F, -8.004F, 51.744F, 1.83F, 3.944F).texOffs(0, 298).addBox(-25.872F, 27.45F, -8.004F, 51.744F, 1.83F, 3.944F).texOffs(82, 0).addBox(25.212F, -25.62F, -8.004F, 2.112F, 54.9F, 3.944F).texOffs(97, 0).addBox(-27.324F, -25.62F, -8.004F, 2.112F, 54.9F, 3.944F).texOffs(95, 171).addBox(27.06F, 2.44F, -33.64F, 1.584F, 8.54F, 7.192F).texOffs(0, 0).addBox(26.004F, -14.64F, -25.52F, 1.188F, 41.48F, 17.4F).texOffs(45, 138).addBox(25.608F, -17.08F, -26.68F, 2.376F, 2.928F, 19.72F).texOffs(116, 171).addBox(-28.644F, 2.44F, -33.64F, 1.584F, 8.54F, 7.192F).texOffs(41, 0).addBox(-27.192F, -14.64F, -25.52F, 1.188F, 41.48F, 17.4F).texOffs(92, 138).addBox(-27.984F, -17.08F, -26.68F, 2.376F, 2.928F, 19.72F), PartPose.offsetAndRotation(0F, -91.877F, 0F, 0.13F, 0F, 0F));
        PartDefinition part_tail1 = part_body.addOrReplaceChild("tail1", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -4.88F, 58F, -0.06F, 0F, 0F));
        PartDefinition part_tail2 = part_tail1.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 1.28F, 31.36F, -0.05F, 0F, 0F));
        PartDefinition part_tail3 = part_tail2.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 1.28F, 29.12F, -0.06F, 0F, 0F));
        PartDefinition part_tail4 = part_tail3.addOrReplaceChild("tail4", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 26.88F, -0.05F, 0F, 0F));
        PartDefinition part_tail5 = part_tail4.addOrReplaceChild("tail5", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 24.64F, -0.04F, 0F, 0F));
        PartDefinition part_chest = part_body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(107, 205).addBox(-20.592F, -30.012F, -25.056F, 41.184F, 2.928F, 10.44F).texOffs(0, 220).addBox(-20.592F, -30.378F, -17.4F, 41.184F, 2.928F, 10.44F).texOffs(107, 220).addBox(-20.592F, -30.744F, -9.744F, 41.184F, 2.928F, 10.44F).texOffs(110, 100).addBox(25.212F, -21.96F, -16.82F, 3.432F, 24.4F, 10.44F).texOffs(0, 100).addBox(25.212F, -15.86F, -23.2F, 2.904F, 12.2F, 23.2F).texOffs(0, 61).addBox(25.212F, -18.91F, -20.88F, 2.376F, 18.3F, 18.56F).texOffs(141, 100).addBox(-28.644F, -21.96F, -16.82F, 3.432F, 24.4F, 10.44F).texOffs(55, 100).addBox(-28.116F, -15.86F, -23.2F, 2.904F, 12.2F, 23.2F).texOffs(45, 61).addBox(-27.588F, -18.91F, -20.88F, 2.376F, 18.3F, 18.56F), PartPose.offset(0F, -4.88F, -30.16F));
        PartDefinition part_neck1 = part_chest.addOrReplaceChild("neck1", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -14.64F, -20.88F, -1.12F, 0F, 0F));
        PartDefinition part_neck2 = part_neck1.addOrReplaceChild("neck2", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -2.56F, -23.76F, 0.95F, 0F, 0F));
        PartDefinition part_head = part_neck2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(90, 61).addBox(-20.72F, -21.384F, -22.692F, 41.44F, 3.432F, 33.184F).texOffs(139, 138).addBox(-14.7F, -23.76F, -18.3F, 29.4F, 2.376F, 18.3F).texOffs(0, 171).addBox(-15.96F, -16.368F, -43.92F, 31.92F, 2.904F, 14.64F).texOffs(137, 171).addBox(-14F, -13.728F, -55.388F, 28F, 2.904F, 11.468F).texOffs(0, 280).addBox(-21.56F, -12.936F, -13.42F, 43.12F, 2.904F, 6.1F).texOffs(142, 0).addBox(19.18F, -14.52F, -12.932F, 2.38F, 17.16F, 20.74F).texOffs(172, 100).addBox(15.4F, -11.22F, -48.8F, 2.1F, 12.54F, 18.3F).texOffs(191, 0).addBox(-21.56F, -14.52F, -12.932F, 2.38F, 17.16F, 20.74F).texOffs(0, 138).addBox(-17.5F, -11.22F, -48.8F, 2.1F, 12.54F, 18.3F), PartPose.offsetAndRotation(0F, 0F, -21.6F, 0.16F, 0F, 0F));
        PartDefinition part_jaw = part_head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0F, 11.88F, -2.44F));
        PartDefinition part_horn_l = part_head.addOrReplaceChild("horn_l", CubeListBuilder.create(), PartPose.offsetAndRotation(14F, -17.16F, -17.08F, -0.4F, 0F, 0.62F));
        PartDefinition part_horn2_l = part_horn_l.addOrReplaceChild("horn2_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.625F, 0F, -0.14F, 0F, 0.14F));
        PartDefinition part_horn3_l = part_horn2_l.addOrReplaceChild("horn3_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.625F, 0F, -0.12F, 0F, 0.12F));
        PartDefinition part_arm_l = part_chest.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(25.08F, 7.32F, -16.24F, 0.5F, 0F, -0.1F));
        PartDefinition part_arm2_l = part_arm_l.addOrReplaceChild("arm2_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 12.5F, 0F, 0.9F, 0F, 0F));
        PartDefinition part_thigh_l = part_body.addOrReplaceChild("thigh_l", CubeListBuilder.create(), PartPose.offsetAndRotation(25.08F, 2.44F, 16.24F, -0.4F, 0F, 0F));
        PartDefinition part_shin_l = part_thigh_l.addOrReplaceChild("shin_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 33.06F, -2.8F, 0.95F, 0F, 0F));
        PartDefinition part_meta_l = part_shin_l.addOrReplaceChild("meta_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 28.5F, 1.4F, -0.6F, 0F, 0F));
        PartDefinition part_foot_l = part_meta_l.addOrReplaceChild("foot_l", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 23.94F, 0F, 0.1F, 0F, 0F));
        PartDefinition part_toe0_l = part_foot_l.addOrReplaceChild("toe0_l", CubeListBuilder.create(), PartPose.offset(0F, 3.42F, -19.6F));
        PartDefinition part_toe1_l = part_foot_l.addOrReplaceChild("toe1_l", CubeListBuilder.create(), PartPose.offsetAndRotation(8.82F, 3.42F, -19.6F, 0F, -0.3F, 0F));
        PartDefinition part_toe2_l = part_foot_l.addOrReplaceChild("toe2_l", CubeListBuilder.create(), PartPose.offsetAndRotation(-8.82F, 3.42F, -19.6F, 0F, 0.3F, 0F));
        PartDefinition part_horn_r = part_head.addOrReplaceChild("horn_r", CubeListBuilder.create(), PartPose.offsetAndRotation(-14F, -17.16F, -17.08F, -0.4F, 0F, -0.62F));
        PartDefinition part_horn2_r = part_horn_r.addOrReplaceChild("horn2_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.625F, 0F, -0.14F, 0F, -0.14F));
        PartDefinition part_horn3_r = part_horn2_r.addOrReplaceChild("horn3_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -11.625F, 0F, -0.12F, 0F, -0.12F));
        PartDefinition part_arm_r = part_chest.addOrReplaceChild("arm_r", CubeListBuilder.create(), PartPose.offsetAndRotation(-25.08F, 7.32F, -16.24F, 0.5F, 0F, 0.1F));
        PartDefinition part_arm2_r = part_arm_r.addOrReplaceChild("arm2_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 12.5F, 0F, 0.9F, 0F, 0F));
        PartDefinition part_thigh_r = part_body.addOrReplaceChild("thigh_r", CubeListBuilder.create(), PartPose.offsetAndRotation(-25.08F, 2.44F, 16.24F, -0.4F, 0F, 0F));
        PartDefinition part_shin_r = part_thigh_r.addOrReplaceChild("shin_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 33.06F, -2.8F, 0.95F, 0F, 0F));
        PartDefinition part_meta_r = part_shin_r.addOrReplaceChild("meta_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 28.5F, 1.4F, -0.6F, 0F, 0F));
        PartDefinition part_foot_r = part_meta_r.addOrReplaceChild("foot_r", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 23.94F, 0F, 0.1F, 0F, 0F));
        PartDefinition part_toe0_r = part_foot_r.addOrReplaceChild("toe0_r", CubeListBuilder.create(), PartPose.offset(0F, 3.42F, -19.6F));
        PartDefinition part_toe1_r = part_foot_r.addOrReplaceChild("toe1_r", CubeListBuilder.create(), PartPose.offsetAndRotation(-8.82F, 3.42F, -19.6F, 0F, 0.3F, 0F));
        PartDefinition part_toe2_r = part_foot_r.addOrReplaceChild("toe2_r", CubeListBuilder.create(), PartPose.offsetAndRotation(8.82F, 3.42F, -19.6F, 0F, -0.3F, 0F));
        PartDefinition part_wing0_l = part_chest.addOrReplaceChild("wing0_l", CubeListBuilder.create().texOffs(0, 235).addBox(0F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(20.592F, -28.548F, -19.836F, 0F, 0F, 0.78F));
        PartDefinition part_wing0_r = part_chest.addOrReplaceChild("wing0_r", CubeListBuilder.create().texOffs(55, 235).addBox(-15.84F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(-20.592F, -28.548F, -19.836F, 0F, 0F, -0.78F));
        PartDefinition part_wing1_l = part_chest.addOrReplaceChild("wing1_l", CubeListBuilder.create().texOffs(110, 235).addBox(0F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(20.592F, -28.914F, -12.18F, 0F, 0F, 0.78F));
        PartDefinition part_wing1_r = part_chest.addOrReplaceChild("wing1_r", CubeListBuilder.create().texOffs(165, 235).addBox(-15.84F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(-20.592F, -28.914F, -12.18F, 0F, 0F, -0.78F));
        PartDefinition part_wing2_l = part_chest.addOrReplaceChild("wing2_l", CubeListBuilder.create().texOffs(0, 250).addBox(0F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(20.592F, -29.28F, -4.524F, 0F, 0F, 0.78F));
        PartDefinition part_wing2_r = part_chest.addOrReplaceChild("wing2_r", CubeListBuilder.create().texOffs(55, 250).addBox(-15.84F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(-20.592F, -29.28F, -4.524F, 0F, 0F, -0.78F));
        PartDefinition part_wing3_l = part_body.addOrReplaceChild("wing3_l", CubeListBuilder.create().texOffs(110, 250).addBox(0F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(20.592F, -31.232F, -20.3F, 0F, 0F, 0.78F));
        PartDefinition part_wing3_r = part_body.addOrReplaceChild("wing3_r", CubeListBuilder.create().texOffs(165, 250).addBox(-15.84F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(-20.592F, -31.232F, -20.3F, 0F, 0F, -0.78F));
        PartDefinition part_wing4_l = part_body.addOrReplaceChild("wing4_l", CubeListBuilder.create().texOffs(0, 265).addBox(0F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(20.592F, -31.598F, -12.644F, 0F, 0F, 0.78F));
        PartDefinition part_wing4_r = part_body.addOrReplaceChild("wing4_r", CubeListBuilder.create().texOffs(55, 265).addBox(-15.84F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(-20.592F, -31.598F, -12.644F, 0F, 0F, -0.78F));
        PartDefinition part_wing5_l = part_body.addOrReplaceChild("wing5_l", CubeListBuilder.create().texOffs(110, 265).addBox(0F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(20.592F, -31.964F, -4.988F, 0F, 0F, 0.78F));
        PartDefinition part_wing5_r = part_body.addOrReplaceChild("wing5_r", CubeListBuilder.create().texOffs(165, 265).addBox(-15.84F, 0F, -5.22F, 15.84F, 2.928F, 10.44F), PartPose.offsetAndRotation(-20.592F, -31.964F, -4.988F, 0F, 0F, -0.78F));
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }
}
