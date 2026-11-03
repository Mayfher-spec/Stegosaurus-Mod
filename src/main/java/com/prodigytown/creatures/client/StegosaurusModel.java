package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Block-based articulated prototype inspired by the scale/detail of ROR, no copied geometry. */
public class StegosaurusModel extends HierarchicalModel<StegosaurusEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID,"stegosaurus"),"main");
    private final ModelPart root;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart tail;
    private final ModelPart head;
    public StegosaurusModel(ModelPart root) {
        this.root = root;
        legs[0] = root.getChild("front_left"); legs[1] = root.getChild("front_right");
        legs[2] = root.getChild("hind_left"); legs[3] = root.getChild("hind_right");
        tail = root.getChild("tail"); head = root.getChild("head");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        // 16 pixels = one block, dinosaur rests on Y=24. Long neck/body and thick tail.
        r.addOrReplaceChild("body", CubeListBuilder.create()
           .texOffs(0,0).addBox(-10,-18,-19,20,16,36)
           .texOffs(0,54).addBox(-9,-13,-22,18,10,8), PartPose.offset(0,24,0));
        r.addOrReplaceChild("head", CubeListBuilder.create()
           .texOffs(0,76).addBox(-5,-8,-11,10,9,13)
           .texOffs(46,80).addBox(-4,-3,-16,8,5,7), PartPose.offset(0,11,-23));
        PartDefinition tail = r.addOrReplaceChild("tail",CubeListBuilder.create()
           .texOffs(84,54).addBox(-6,-6,0,12,12,22)
           .texOffs(84,88).addBox(-4,-5,20,8,9,13),PartPose.offset(0,11,14));
        // Four defensive spikes, distinct from the dorsal plates.
        for (int i=0;i<4;i++) {
            float x = (i%2==0 ? -5.0f : 5.0f);
            float z = i<2 ? 25.0f : 34.0f;
            tail.addOrReplaceChild("thagomizer"+i,CubeListBuilder.create().texOffs(120,120)
                .addBox(-2,-2,-2,4,4,15),PartPose.offsetAndRotation(x,-2,z,0,(i%2==0?-0.75f:0.75f),0));
        }
        // Alternating dorsal plates; painted separately on the texture atlas.
        for (int i=0;i<10;i++) {
            float z = -16 + i*3.5f;
            float y = -15f + Math.abs(i-4.5f)*0.6f;
            r.addOrReplaceChild("plate"+i, CubeListBuilder.create().texOffs(160,0)
               .addBox(-1.5f,-15,-2.5f,3,16,5),PartPose.offset((i%2==0 ? -4.0f:4.0f),y,z));
        }
        r.addOrReplaceChild("front_left",CubeListBuilder.create().texOffs(0,112).addBox(-4,0,-4,8,16,8),PartPose.offset(10,8,-15));
        r.addOrReplaceChild("front_right",CubeListBuilder.create().texOffs(36,112).addBox(-4,0,-4,8,16,8),PartPose.offset(-10,8,-15));
        r.addOrReplaceChild("hind_left",CubeListBuilder.create().texOffs(72,112).addBox(-5,0,-5,10,17,10),PartPose.offset(10,7,11));
        r.addOrReplaceChild("hind_right",CubeListBuilder.create().texOffs(112,112).addBox(-5,0,-5,10,17,10),PartPose.offset(-10,7,11));
        return LayerDefinition.create(mesh,256,256);
    }
    @Override public ModelPart root() {return root;}
    @Override public void setupAnim(StegosaurusEntity stego,float limbSwing,float limbSwingAmount,float ageInTicks,float netHeadYaw,float headPitch) {
        for(int i=0;i<legs.length;i++) {
            legs[i].xRot = Mth.cos(limbSwing*0.55f + ((i==0||i==3)?0:Mth.PI))*1.0f*limbSwingAmount;
        }
        tail.yRot = Mth.sin(ageInTicks*0.075f)*0.12f;
        head.yRot = Mth.clamp(netHeadYaw*0.017453292f,-0.42f,0.42f);
        head.xRot = Mth.clamp(headPitch*0.017453292f,-0.25f,0.25f);
    }
}
