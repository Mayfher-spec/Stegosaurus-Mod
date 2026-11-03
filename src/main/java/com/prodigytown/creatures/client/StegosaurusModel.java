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
 * Stegosaurus for ROR — original voxel sculpt, 2.1.0.
 *
 * Face-oriented material atlas: 4 x 4 256px swatches in a 1024 atlas. Cuboids
 * use the same material across all six faces; no UV island accidentally samples
 * a neighboring block's colour. Transparent emissive texture is a second pass.
 *
 * The animated hierarchy is: root -> torso / neck -> head / tail -> mid -> tip /
 * four legs. All dorsal plates and all flank armor are torso children; the
 * thagomizer spikes are tail-tip children. Ground is model Y=24.
 *
 * Sculpt and all pattern work are original, not extracted from ROR.
 */
public final class StegosaurusModel extends HierarchicalModel<StegosaurusEntity> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus"), "main");

    private static final int SKIN = 0, ARMOR = 1, SHADOW = 2, BELLY = 3,
        RIDGE = 4, PLATE_RIM = 5, PLATE_CORE = 6, PLATE_GLOW = 7,
        ACCENT = 8, LIGHT = 9, BONE = 10, EYES = 11,
        SCARS = 12, VEINS = 13, MUZZLE = 14, JOINTS = 15;

    private final ModelPart root, torso, neck, head, tail, tailMid, tailTip;
    private final ModelPart[] legs = new ModelPart[4];

    public StegosaurusModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.tail = root.getChild("tail");
        this.tailMid = tail.getChild("mid");
        this.tailTip = tailMid.getChild("tip");
        for (int i = 0; i < 4; i++) this.legs[i] = root.getChild("leg_" + i);
    }

    private static CubeListBuilder box(int material, float x,float y,float z,float w,float h,float d) {
        return CubeListBuilder.create().texOffs((material % 4) * 256 + 12,
                                               (material / 4) * 256 + 12)
            .addBox(x,y,z,w,h,d);
    }
    private static PartDefinition add(PartDefinition p, String n, int m,
                                      float x,float y,float z,float w,float h,float d) {
        return p.addOrReplaceChild(n,box(m,x,y,z,w,h,d),PartPose.ZERO);
    }
    private static PartDefinition joint(PartDefinition p, String n,float x,float y,float z) {
        return p.addOrReplaceChild(n,CubeListBuilder.create(),PartPose.offset(x,y,z));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Eight layers of torso geometry create a barreled animal instead of
        // an extremely long flat rectangular box. All armor is attached here.
        PartDefinition body = joint(root,"torso",0,11,0);
        add(body,"body_core",SKIN,-13.5f,-9,-25,27,18,50);
        add(body,"back_curved",SKIN,-12,-11,-20,24,4,41);
        add(body,"lower_rib",BELLY,-11,3,-25,22,6,49);
        add(body,"belly_step",BELLY,-9,6,-20,18,4,43);
        add(body,"chest",SKIN,-14,-9,-29,28,18,20);
        add(body,"shoulder_high",ARMOR,-13,-11,-26,26,5,15);
        add(body,"hip",SKIN,-15,-10,12,30,20,22);
        add(body,"hip_cap",ARMOR,-13.5f,-12,16,27,6,17);
        add(body,"back_armor",RIDGE,-11,-12,-15,22,2.8f,36);
        add(body,"tail_root_join",SKIN,-11,-6,26,22,13,13);
        add(body,"shoulder_transition",SKIN,-12,-6,-35,24,13,14);
        // Distinct layered side armor, plus stylized whorls and darker scale seams.
        for (int side : new int[]{-1,1}) {
            float sx = side * 13.7f;
            for(int i=0;i<6;i++) {
                float z = -23 + i*8.6f;
                float y = -5 + (i%2)*1.1f;
                add(body,"flank_"+side+"_"+i,ARMOR,sx - (side<0?1.3f:0),y,z,1.3f,7.8f,7.2f);
                add(body,"flank_line_"+side+"_"+i,ACCENT,sx - (side<0?1.52f:-.25f),y+1.0f,z+1.5f,.5f,1.5f,4.7f);
            }
            add(body,"armor_spiral_"+side+"_a",ACCENT,
                side>0?15.85f:-16.50f,-4,11,.65f,2.8f,9);
            add(body,"armor_spiral_"+side+"_b",ACCENT,
                side>0?15.85f:-16.50f,-4,11,.65f,9,2.8f);
            add(body,"armor_spiral_"+side+"_c",ACCENT,
                side>0?15.85f:-16.50f,2,15,.65f,2.5f,6);
            // Shoulder and hip bevels give a thick anatomical silhouette.
            add(body,"shoulder_shield_"+side,RIDGE,
                side>0?13.2f:-15,-9,-24,1.8f,12,12);
            add(body,"hip_shield_"+side,ARMOR,
                side>0?14f:-16,-6,17,2,12,13);
        }

        // Twelve pairs of asymmetrically staggered broad tapered dorsal plates.
        // Each is sculpted from horizontal steps of decreasing width, rimmed
        // on both sides and filled with patterned, brightly coloured cores.
        final int[] heights = {7,10,14,18,23,28,30,29,25,20,15,10};
        for(int i=0;i<heights.length;i++) {
            float z=-34 + i*6.5f;
            float halfSpan = (i<3||i>9) ? 4.4f : 5.6f;
            for(int side : new int[]{-1,1}) {
                float x = side * halfSpan;
                float stagger = side*1.45f;
                addPlate(body,"dorsal_"+i+"_"+(side>0?"r":"l"),x,-11.2f,
                         z+stagger,heights[i],(i>2&&i<10)?13.6f:9.0f,side);
            }
        }
        // Irregular raised scar and energy/ember seams: sculpted geometry in
        // addition to the atlas, readable even when the camera is far away.
        // Magma interprets these as orange fractures, gold as ornamental veins.
        for (int side : new int[]{-1,1}) {
            float x= side>0 ? 15.1f : -15.45f;
            for(int k=0;k<4;k++) {
                float z=17+k*3.3f;
                add(body,"hip_fissure_"+side+"_"+k,VEINS,x,-6+k*1.65f,z,
                    .35f,.9f,5.7f);
            }
            add(body,"hip_wound_"+side,SCARS,x,-1,22,.45f,1.4f,7.5f);
            add(body,"shoulder_vein_"+side,VEINS,
                side>0?14.9f:-15.2f,-3,-22,.40f,1.15f,11);
        }

        // Small defensive ridge along neck and tail, continuous with big plates.
        for(int i=0;i<5;i++) {
            int h=4 + i*2;
            addPlate(body,"pre_dorsal_"+i,0,-10,-42 + i*3.2f,h,4.5f,1);
        }

        PartDefinition neck=joint(root,"neck",0,11,-30);
        add(neck,"neck_base",SKIN,-9,-6,-13,18,13,21);
        add(neck,"neck_taper",SKIN,-7,-6,-25,14,11,20);
        add(neck,"neck_under",BELLY,-6,2,-26,12,5,26);
        add(neck,"neck_lined",ARMOR,-7.2f,-6,-20,14.4f,2.5f,19);
        for(int i=0;i<4;i++) {
            add(neck,"neck_scale_"+i,RIDGE,-5,-7.2f,-25+i*5,10,2,4);
        }
        PartDefinition head=joint(neck,"head",0,2,-24);
        add(head,"skull",MUZZLE,-5.8f,-6,-14,11.6f,10.5f,18);
        add(head,"skull_brow",ARMOR,-6,-7,-12,12,3.4f,11);
        add(head,"jaw_lower",BELLY,-5.5f,2.5f,-22,11,3.8f,24);
        add(head,"cheek_back",SKIN,-6.1f,-3,-4,12.2f,8,8);
        add(head,"beak_nose",MUZZLE,-4.3f,-2.9f,-24.5f,8.6f,5,11);
        add(head,"beak_tip",BONE,-4.1f,0,-25.8f,8.2f,2,3);
        add(head,"lower_lip",ARMOR,-4.8f,2.5f,-22,9.6f,1.3f,17);
        add(head,"forehead",ARMOR,-5.7f,-7.3f,-16.5f,11.4f,2,6);
        for (int side : new int[]{-1,1}) {
            float x=side>0 ? 5.55f : -6.45f;
            add(head,"eye_socket_"+side,SHADOW,x,-4.9f,-12,0.9f,3.4f,5);
            add(head,"eye_luminous_"+side,EYES,x+(side>0?.35f:-.35f),-4.15f,-10.7f,0.45f,1.8f,2.6f);
            add(head,"cheek_armor_"+side,ARMOR,x,-1.4f,-8,0.9f,3.5f,8);
            add(head,"nostril_"+side,SHADOW,side>0?4.2f:-5,-2.3f,-22,0.8f,1.3f,2.1f);
            add(head,"jaw_trim_"+side,ACCENT,x,2.3f,-15,0.6f,0.9f,8.5f);
        }

        // Long tapering, fully attached articulated tail with a four-spike
        // thagomizer. The spikes articulate together, not separately hovering.
        PartDefinition tail=joint(root,"tail",0,11,31);
        add(tail,"base",SKIN,-10,-7,-7,20,14,20);
        add(tail,"base_slope",ARMOR,-8,-8,6,16,4.5f,18);
        add(tail,"base_lower",BELLY,-8,3,-5,16,5,18);
        add(tail,"base_taper",SKIN,-8,-6,11,16,12,20);
        PartDefinition mid=joint(tail,"mid",0,0,27);
        add(mid,"mid_core",SKIN,-6,-5,-3,12,10,25);
        add(mid,"mid_ridge",ARMOR,-5,-6,2,10,2.8f,23);
        add(mid,"mid_taper",SKIN,-4.4f,-4.2f,20,8.8f,8,15);
        PartDefinition tip=joint(mid,"tip",0,0,31);
        add(tip,"tip_main",SKIN,-3.7f,-3.7f,-4,7.4f,7.4f,24);
        add(tip,"tip_tip",ARMOR,-2.8f,-2.8f,15,5.6f,5.6f,18);
        for(int i=0;i<6;i++) {
            float z=2+i*5f;
            float radius=4.9f-i*.42f;
            add(mid,"scale_"+i,RIDGE,-radius,-6+(i*.28f),z,radius*2,2.1f,3.1f);
        }
        // Four tapered dorsal spikes - separate base / middle / sharpened tip.
        for(int i=0;i<4;i++) {
            int side=(i&1)==0? -1:1;
            float z=i<2?15:25;
            PartDefinition spike=tip.addOrReplaceChild("thagomizer_"+i,
                CubeListBuilder.create(),PartPose.offsetAndRotation(side*2.0f,-1,z,
                    -0.13f,side*0.96f,side*0.18f));
            add(spike,"spike_socket",JOINTS,-2.3f,-2.3f,-1,4.6f,4.6f,5);
            add(spike,"spike_root",BONE,-2,-2,2,4,4,11);
            add(spike,"spike_mid",BONE,-1.6f,-1.6f,12,3.2f,3.2f,9);
            add(spike,"spike_tip",BONE,-1,-1,21,2,2,8);
            add(spike,"spike_point",LIGHT,-.5f,-.5f,29,1,1,4);
        }
        for(int i=0;i<4;i++) {
            float z=7+i*5.7f;
            addPlate(tail,"tail_plate_"+i,0,-8.5f,z,7-i,5.8f,1);
        }
        addLeg(root,0,-1,-20,true);
        addLeg(root,1,1,-20,true);
        addLeg(root,2,-1,22,false);
        addLeg(root,3,1,22,false);
        return LayerDefinition.create(mesh,1024,1024);
    }

    private static void addPlate(PartDefinition body, String name,
                                 float x,float y,float z,int height,float depth,int side) {
        PartDefinition plate = body.addOrReplaceChild(name,CubeListBuilder.create(),
            PartPose.offsetAndRotation(x,y,z,side*.08f,side*.08f,side*.075f));
        // Per-level profile creates a true *stepped leaf* outline from the side:
        // broad lower-middle and a narrow, distinct pointed top. Side-facing
        // panels ensure that glow is visible from the ground beside the beast.
        float[] profile={0.51f,0.78f,1.00f,0.98f,0.86f,0.68f,0.45f,0.24f};
        float tier = height/8.0f;
        for(int n=0;n<8;n++) {
            float d=depth*profile[n];
            float yy=-(n+1)*tier;
            float thick=2.65f - .09f*n;
            add(plate,"rim_"+n,PLATE_RIM,-thick/2,yy,-d/2,thick,tier+.10f,d);
            if (n < 7 && d>2.1f) {
                float visible= d*0.77f;
                float insetZ=(depth-d)*0.02f;
                int material = n>4?PLATE_GLOW:PLATE_CORE;
                add(plate,"face_r_"+n,material,thick/2-.04f,yy+.22f,-visible/2+insetZ,
                    .34f,Math.max(.28f,tier-.44f),visible);
                add(plate,"face_l_"+n,material,-thick/2-.3f,yy+.22f,-visible/2+insetZ,
                    .34f,Math.max(.28f,tier-.44f),visible);
            }
            if (n==2 || n==4 || n==6) {
                float w=Math.max(.9f,d*.36f);
                add(plate,"crystal_line_r_"+n,LIGHT,thick/2+.29f,yy+.38f,-w/2,
                    .17f,Math.max(.38f,tier*.3f),w);
                add(plate,"crystal_line_l_"+n,LIGHT,-thick/2-.49f,yy+.38f,-w/2,
                    .17f,Math.max(.38f,tier*.3f),w);
            }
        }
        add(plate,"plate_socket",RIDGE,-1.9f,-.7f,-depth*.33f,3.8f,2.4f,depth*.66f);
    }

    private static void addLeg(PartDefinition root,int id,int side,float z,boolean front) {
        float x=side*(front?12f:13.5f);
        PartDefinition leg=joint(root,"leg_"+id,x,7.5f,z);
        float t=front?5.9f:7f;
        add(leg,"hip_socket",RIDGE,-t-1,-4,-t-1,2*t+2,8,2*t+2);
        add(leg,"thigh_bulk",SKIN,-t,-1,-t,2*t,10,2*t);
        add(leg,"thigh_bevel",ARMOR,-t+1,-3,-t+1,2*t-2,4,2*t-2);
        add(leg,"calf",SKIN,-t+1,7,-t+1,2*t-2,8.4f,2*t-2);
        add(leg,"ankle",ARMOR,-t,12,-t,2*t,3.8f,2*t);
        add(leg,"foot",SKIN,-t-1,15,-t-3,2*t+2,2.0f,2*t+5);
        // Armored knee plates and an exposed highlight on the outside.
        add(leg,"knee_pad",RIDGE,side>0?t-.25f:-t-1.4f,2.0f,-t+1,1.6f,8.4f,2*t-2);
        add(leg,"knee_glow",ACCENT,side>0?t+1.05f:-t-1.65f,3.5f,-t+2,.65f,2.1f,2*t-5);
        for(int k=0;k<3;k++) {
            float footX=(k-1)*(t*.59f)-1.6f;
            add(leg,"toe_"+k,BONE,footX,15.0f,-t-6.4f,3.2f,2.4f,6.3f);
            add(leg,"claw_"+k,BONE,footX+.48f,15.3f,-t-8.4f,2.15f,1.9f,3.2f);
        }
    }

    @Override public ModelPart root(){return root;}

    @Override public void setupAnim(StegosaurusEntity entity,float limbSwing,float limbAmount,
                                    float ageTicks,float headYaw,float headPitch) {
        float walk=Mth.clamp(limbAmount,0,1);
        for(int i=0;i<4;i++) {
            float phase=(i==0||i==3)?0:Mth.PI;
            legs[i].xRot=Mth.cos(limbSwing*.57f+phase)*.43f*walk;
            legs[i].zRot=Mth.sin(limbSwing*.57f+phase)*.035f*walk;
        }
        torso.y=11 + Mth.cos(limbSwing*.55f)*.27f*walk
                   +Mth.sin(ageTicks*.07f)*.12f;
        neck.xRot=Mth.sin(ageTicks*.05f)*.025f;
        head.yRot=Mth.clamp(headYaw*0.017453292f,-.42f,.42f);
        head.xRot=Mth.clamp(headPitch*0.017453292f,-.25f,.26f);
        float aggression=entity.getTarget()!=null?.56f:.10f;
        tail.yRot=Mth.sin(ageTicks*(aggression>.3f?.21f:.07f))*aggression;
        tail.xRot=Mth.sin(ageTicks*.10f)*.045f;
        tailMid.yRot=Mth.sin(ageTicks*(aggression>.3f?.23f:.07f)-.8f)*aggression*.60f;
        tailTip.yRot=Mth.sin(ageTicks*(aggression>.3f?.25f:.08f)-1.4f)*aggression*.42f;
    }
}
