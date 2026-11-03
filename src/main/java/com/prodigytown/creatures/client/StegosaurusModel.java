package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
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

/**
 * Stegosaurus for ROR v2.0 — new articulated voxel anatomy.
 * All geometry is original. Reference illustrations are NOT included as textures.
 * Atlas: 512 x 512. Base/tinted armor are separate cuboids for readable variants.
 * Roots and pivots intentionally follow the vanilla Minecraft model coordinate convention
 * (ground at model Y=24); dorsal plates are children of the torso to avoid floating.
 */
public final class StegosaurusModel extends HierarchicalModel<StegosaurusEntity> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus"), "main");

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] legs = new ModelPart[4];

    public StegosaurusModel(ModelPart root) {
        this.root = root;
        torso = root.getChild("torso");
        neck = root.getChild("neck");
        head = neck.getChild("head");
        tail = root.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        legs[0] = root.getChild("leg_front_left");
        legs[1] = root.getChild("leg_front_right");
        legs[2] = root.getChild("leg_rear_left");
        legs[3] = root.getChild("leg_rear_right");
    }

    private static CubeListBuilder cube(int u, int v, float x, float y, float z,
                                        float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x,y,z,w,h,d);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        // MASSIVE BARREL BODY, SHOULDERS, HIPS, BELLY AND INTERLOCKING ARMOR.
        // All torso subparts use the same torso pivot (Y=11). The top is Y=1.
        PartDefinition torso = r.addOrReplaceChild("torso", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-12,-10,-21,24,18,45)
            .texOffs(0, 72).addBox(-11,-8,-29,22,16,16)         // shoulders
            .texOffs(0, 0).addBox(-14,-9,11,28,19,20)         // muscular hips
            .texOffs(0, 160).addBox(-10,3,-23,20,7,49)       // pale underbody
            .texOffs(192, 160).addBox(-13,-6,-13,2,12,23)    // left scale flank
            .texOffs(192, 160).addBox(11,-6,-13,2,12,23)     // right scale flank
            .texOffs(384, 160).addBox(-10,-11,-16,20,3,25),  // exposed armored ridge
            PartPose.offset(0,11,0));

        // Rhombus dorsal armor: short at the ends and tallest behind shoulders.
        // Each plate is a physical tapered silhouette rather than a floating single cube.
        int[] heights = {8,12,17,23,26,27,25,20,16,12,9};
        for (int i=0; i<heights.length; i++) {
            int h = heights[i];
            float z = -24 + i*4.8f;
            float x = ((i & 1)==0) ? -4.6f : 4.6f;
            float slope = ((i&1)==0) ? -0.10f : 0.10f;
            PartDefinition plate = torso.addOrReplaceChild("dorsal_plate_"+i,
                CubeListBuilder.create()
                    .texOffs(288,0).addBox(-2.0f,-4,-5.7f,4,5,11.4f)
                    .texOffs(288,48).addBox(-1.65f,-h*0.65f,-4.7f,3.3f,h*0.65f-3.0f,9.4f)
                    .texOffs(288,92).addBox(-1.2f,-h,-2.7f,2.4f,h*0.38f+1.0f,5.4f)
                    .texOffs(384,6).addBox(-0.7f,-h-2,-1.6f,1.4f,3.0f,3.2f),
                PartPose.offsetAndRotation(x,-10,z,-0.04f,slope,0));
            // Dark seam at the base so the glowing core is grounded to the back.
            plate.addOrReplaceChild("plate_base",cube(384,161,-2.7f,-1.0f,-6.1f,5.4f,2.5f,12.2f),PartPose.ZERO);
        }

        // NECK: slopes toward a small, elongated head; multiple joined segments.
        PartDefinition neck = r.addOrReplaceChild("neck",CubeListBuilder.create()
            .texOffs(192,0).addBox(-7,-7,-18,14,15,25)
            .texOffs(0,0).addBox(-6,-6,-25,12,12,15)
            .texOffs(0,160).addBox(-5,3,-23,10,5,18),
            PartPose.offset(0,11,-25));
        // Descending, ridged neck plates lead into the spine.
        for(int i=0; i<4; i++) {
            float z=-20 + i*5.4f;
            float h=4 + i*1.6f;
            neck.addOrReplaceChild("neck_scute_"+i,
                cube(288,48,-2f,-h-5,z,4,h+3,4.0f),PartPose.ZERO);
        }
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0,274).addBox(-5,-5,-18,10,9,20)
            .texOffs(0,274).addBox(-5,-3,-27,10,6,12)
            .texOffs(192,0).addBox(-5,-6,-6,10,8,9)
            .texOffs(0,160).addBox(-4.5f,2,-26,9,3,24)
            .texOffs(384,160).addBox(-5,-5,-8,10,2,9),
            PartPose.offset(0,-1,-18));
        // Squared snout, visibly inset nostrils, luminous pupils, cheek guards.
        head.addOrReplaceChild("eye_left", cube(428,288,4.95f,-4.2f,-10.5f,1.1f,2.3f,2.7f),PartPose.ZERO);
        head.addOrReplaceChild("eye_right",cube(428,288,-6.05f,-4.2f,-10.5f,1.1f,2.3f,2.7f),PartPose.ZERO);
        head.addOrReplaceChild("nostril_left",cube(384,270,4.65f,-2.7f,-24f,1,1.4f,2.2f),PartPose.ZERO);
        head.addOrReplaceChild("nostril_right",cube(384,270,-5.65f,-2.7f,-24f,1,1.4f,2.2f),PartPose.ZERO);
        head.addOrReplaceChild("beak",cube(288,281,-4.3f,-0.5f,-29.5f,8.6f,3.2f,6.0f),PartPose.ZERO);

        // LONG, FULLY CONNECTED TAIL. Both articulated segments are real parents.
        PartDefinition tail = r.addOrReplaceChild("tail",CubeListBuilder.create()
            .texOffs(0,0).addBox(-10,-8,-3,20,15,27)
            .texOffs(0,72).addBox(-7,-6,18,14,12,18)
            .texOffs(384,160).addBox(-8,-9,6,16,2,19),
            PartPose.offset(0,11,24));
        PartDefinition tailTip = tail.addOrReplaceChild("tail_tip",CubeListBuilder.create()
            .texOffs(0,0).addBox(-6,-5,-2,12,10,23)
            .texOffs(192,0).addBox(-4,-4,19,8,8,19)
            .texOffs(384,160).addBox(-5,-6,5,10,2,16),
            PartPose.offset(0,0,34));
        for(int i=0;i<4;i++) {
            float x=(i%2==0)?-3.5f:3.5f;
            float z=(i<2)?21:31;
            float yaw=(i%2==0)?0.57f:-0.57f;
            tailTip.addOrReplaceChild("tail_spike_"+i,
                CubeListBuilder.create()
                    .texOffs(288,160).addBox(-2.6f,-2.6f,0,5.2f,5.2f,16)
                    .texOffs(288,215).addBox(-1.8f,-1.8f,14,3.6f,3.6f,9)
                    .texOffs(288,272).addBox(-1.1f,-1.1f,21,2.2f,2.2f,8),
                PartPose.offsetAndRotation(x,-1.2f,z,0,yaw,(i%2==0?0.14f:-0.14f)));
        }
        for(int i=0;i<4;i++) {
            tail.addOrReplaceChild("tail_scute_"+i,
                cube(288,48,-2,-8.5f,i*7.5f,4,5.4f,4),PartPose.ZERO);
        }

        // FOUR INDEPENDENTLY ANIMATED HEAVY LEGS with wrists, ankle scales,
        // separated feet and three armored toes per foot.
        addLeg(r,"leg_front_left",+1,true);
        addLeg(r,"leg_front_right",-1,true);
        addLeg(r,"leg_rear_left",+1,false);
        addLeg(r,"leg_rear_right",-1,false);
        return LayerDefinition.create(mesh,512,512);
    }

    private static void addLeg(PartDefinition parent, String name,int side,boolean front) {
        float x=side*(front?11f:12f);
        float z=front?-17f:18f;
        float radius=front?5.0f:6.4f;
        float originY=front?10:9;
        float shinH=front?12:13;
        PartDefinition leg=parent.addOrReplaceChild(name,CubeListBuilder.create()
            .texOffs(192,160).addBox(-radius,-4,-radius,2*radius,13,2*radius)
            .texOffs(0,72).addBox(-radius-0.8f,-2,-radius-1f,2*radius+1.6f,9,2*radius+2f)
            .texOffs(192,190).addBox(-radius+1,6,-radius+1,2*radius-2,shinH,2*radius-2)
            .texOffs(288,281).addBox(-radius-0.8f,13,-radius-4,2*radius+1.6f,3.8f,2*radius+7),
            PartPose.offset(x,originY,z));
        for(int toe=-1;toe<=1;toe++) {
            leg.addOrReplaceChild("claw_"+toe,
                cube(288,272,toe*radius*0.62f-1.4f,14.5f,-radius-7,2.8f,2.5f,6),PartPose.ZERO);
        }
        for(int i=0;i<2;i++) {
            leg.addOrReplaceChild("leg_scale_"+i,
                cube(384,160,side>0?radius-0.2f:-radius-1,1+i*5,-radius+1.5f,1.3f,4,5),PartPose.ZERO);
        }
    }

    @Override public ModelPart root() {return root;}

    @Override public void setupAnim(StegosaurusEntity entity,float limbSwing,float limbSwingAmount,
                                    float ageInTicks,float netHeadYaw,float headPitch) {
        // Opposing pairs: slow, heavy steps (a defensive tank rather than a runner).
        float walk=Mth.clamp(limbSwingAmount,0,1);
        for(int i=0;i<legs.length;i++) {
            float phase=((i==0||i==3)?0:Mth.PI);
            legs[i].xRot=Mth.cos(limbSwing*0.47f+phase)*0.52f*walk;
            legs[i].zRot=0;
        }
        torso.y=11+Mth.cos(limbSwing*0.5f)*0.30f*walk;
        float defensive=entity.getTarget()!=null ? 0.5f : 0.0f;
        tail.yRot=Mth.sin(ageInTicks*(defensive>0?0.25f:0.07f))*(defensive+0.10f);
        tail.xRot=Mth.cos(ageInTicks*0.1f)*0.035f;
        tailTip.yRot=Mth.sin(ageInTicks*(defensive>0?0.31f:0.085f)+1.2f)*(defensive*0.7f+0.12f);
        head.yRot=Mth.clamp(netHeadYaw*0.017453292f,-0.50f,0.50f);
        head.xRot=Mth.clamp(headPitch*0.017453292f,-0.3f,0.3f);
        neck.xRot=Mth.sin(ageInTicks*0.045f)*0.02f;
    }
}
