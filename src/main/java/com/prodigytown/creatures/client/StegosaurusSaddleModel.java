package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Montura del estegosaurio: manta doble, silla con pomo, agarradera y respaldo, cinchas con hebillas, faldones con tachuelas, estandarte,
 * alforjas, collar con medallon, cabezada y bozal de cuero.
 * GENERADO por scratchpad/gear/gen3.py junto con sus texturas; no editar a mano.
 * Misma jerarquia de huesos que StegosaurusModel; copia su pose cada cuadro.
 */
public final class StegosaurusSaddleModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus_saddle"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_saddle.png");
    /** Ruta de cada hueso rastreado, desde la raiz. */
    public static final String[][] TRACK = {
        {"all"},
        {"all", "body2"},
        {"all", "body2", "neck2"},
        {"all", "body2", "neck2", "neck"},
        {"all", "body2", "neck2", "neck", "head"},
        {"all", "body2", "neck2", "neck", "head", "snout"},
        {"all", "body"},
        {"all", "body", "tail"},
        {"all", "body", "tail", "tail2"},
        {"all", "body", "tail", "tail2", "tail3"},
        {"all", "body", "tail", "tail2", "tail3", "tail4"},
        {"all", "body2", "arm_l"},
        {"all", "body2", "arm_r"},
        {"all", "body2", "arm_l", "hand_l"},
        {"all", "body2", "arm_r", "hand_r"},
        {"all", "leg_l"},
        {"all", "leg_r"},
        {"all", "leg_l", "leg_l2"},
        {"all", "leg_r", "leg_r2"},
        {"all", "leg_l", "leg_l2", "foot_l"},
        {"all", "leg_r", "leg_r2", "foot_r"}
    };
    private final ModelPart root;
    private final ModelPart[] mine = new ModelPart[TRACK.length];

    public StegosaurusSaddleModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        for (int i = 0; i < TRACK.length; i++) mine[i] = walk(root, TRACK[i]);
    }

    public static ModelPart walk(ModelPart from, String[] path) {
        ModelPart p = from;
        for (String n : path) p = p.getChild(n);
        return p;
    }

    /** Copia la pose de los huesos del modelo del estegosaurio (theirs[i] corresponde a TRACK[i]). */
    public void sync(ModelPart[] theirs) {
        for (int i = 0; i < mine.length; i++) {
            ModelPart d = mine[i], s = theirs[i];
            d.x = s.x; d.y = s.y; d.z = s.z;
            d.xRot = s.xRot; d.yRot = s.yRot; d.zRot = s.zRot;
            d.xScale = s.xScale; d.yScale = s.yScale; d.zScale = s.zScale;
        }
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                         float red, float green, float blue, float alpha) {
        root.render(pose, consumer, light, overlay, red, green, blue, alpha);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition all = mesh.getRoot().addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        PartDefinition body2 = all.addOrReplaceChild("body2", CubeListBuilder.create(), PartPose.offset(0F, -46F, -25.3F));
        PartDefinition neck2 = body2.addOrReplaceChild("neck2", CubeListBuilder.create(), PartPose.offset(0F, 0F, -19.55F));
        PartDefinition neck = neck2.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0F, 4.6F, -13.8F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 1.15F, -13.8F, 0.1745F, 0F, 0F));
        PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -6.25F, -12.55F, 0.0873F, 0F, 0F));
        PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, -46F, 16.1F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -1.15F, 24.15F, -0.1047F, 0F, 0F));
        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 4.5783F, 22.4515F, 0.1047F, 0F, 0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offset(0F, 1.15F, 21.85F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create(), PartPose.offset(0F, -1.15F, 21.85F));
        PartDefinition arm_l = body2.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offset(-25.76F, -11.5F, -6.9F));
        PartDefinition arm_r = body2.addOrReplaceChild("arm_r", CubeListBuilder.create(), PartPose.offset(25.76F, -11.5F, -6.9F));
        PartDefinition hand_l = arm_l.addOrReplaceChild("hand_l", CubeListBuilder.create(), PartPose.offset(0F, 46.46F, 0F));
        PartDefinition hand_r = arm_r.addOrReplaceChild("hand_r", CubeListBuilder.create(), PartPose.offset(0F, 46.46F, 0F));
        PartDefinition leg_l = all.addOrReplaceChild("leg_l", CubeListBuilder.create(), PartPose.offset(-25.99F, -57.5F, 28.75F));
        PartDefinition leg_r = all.addOrReplaceChild("leg_r", CubeListBuilder.create(), PartPose.offset(25.99F, -57.5F, 28.75F));
        PartDefinition leg_l2 = leg_l.addOrReplaceChild("leg_l2", CubeListBuilder.create(), PartPose.offset(0F, 27.14F, 2.3F));
        PartDefinition leg_r2 = leg_r.addOrReplaceChild("leg_r2", CubeListBuilder.create(), PartPose.offset(0F, 27.14F, 2.3F));
        PartDefinition foot_l = leg_l2.addOrReplaceChild("foot_l", CubeListBuilder.create(), PartPose.offset(0F, 20.7F, -1.15F));
        PartDefinition foot_r = leg_r2.addOrReplaceChild("foot_r", CubeListBuilder.create(), PartPose.offset(0F, 20.7F, -1.15F));
        body2.addOrReplaceChild("blanket", CubeListBuilder.create().texOffs(0, 0).addBox(-22F, -27F, 3.3F, 44F, 2F, 30F), PartPose.ZERO);
        body.addOrReplaceChild("blanket_b", CubeListBuilder.create().texOffs(149, 0).addBox(-23F, -27F, -22.1F, 46F, 2F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(290, 0).addBox(-15F, -29F, 8.3F, 30F, 4F, 24F), PartPose.ZERO);
        body.addOrReplaceChild("pad_b", CubeListBuilder.create().texOffs(399, 0).addBox(-14F, -29F, -12.1F, 28F, 4F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("pommel", CubeListBuilder.create().texOffs(472, 0).addBox(-6F, -31F, 5.3F, 12F, 6F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("pommel_t", CubeListBuilder.create().texOffs(0, 33).addBox(-6F, -32F, 5.3F, 12F, 1F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("stud_a", CubeListBuilder.create().texOffs(31, 33).addBox(-4F, -29.5F, 4.5F, 2F, 2F, 1F), PartPose.ZERO);
        body2.addOrReplaceChild("stud_b", CubeListBuilder.create().texOffs(31, 33).addBox(2F, -29.5F, 4.5F, 2F, 2F, 1F), PartPose.ZERO);
        body2.addOrReplaceChild("grip_post", CubeListBuilder.create().texOffs(38, 33).addBox(-5F, -38F, 5.8F, 2F, 6F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("grip_post_r", CubeListBuilder.create().mirror(true).texOffs(38, 33).addBox(3F, -38F, 5.8F, 2F, 6F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("grip_bar", CubeListBuilder.create().texOffs(47, 33).addBox(-5F, -39F, 5.8F, 10F, 2F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("rail", CubeListBuilder.create().texOffs(72, 33).addBox(-17F, -31F, 8.3F, 2F, 6F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("rail_r", CubeListBuilder.create().mirror(true).texOffs(72, 33).addBox(15F, -31F, 8.3F, 2F, 6F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("rail_t", CubeListBuilder.create().texOffs(131, 33).addBox(-17F, -32F, 8.3F, 2F, 1F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("rail_t_r", CubeListBuilder.create().mirror(true).texOffs(131, 33).addBox(15F, -32F, 8.3F, 2F, 1F, 27F), PartPose.ZERO);
        body.addOrReplaceChild("cantle", CubeListBuilder.create().texOffs(190, 33).addBox(-15F, -33F, -9.1F, 30F, 8F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("cantle_t", CubeListBuilder.create().texOffs(257, 33).addBox(-15F, -34F, -9.1F, 30F, 1F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("backrest", CubeListBuilder.create().texOffs(324, 33).addBox(-13F, -50F, -9.1F, 26F, 17F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("backrest_t", CubeListBuilder.create().texOffs(383, 33).addBox(-14F, -52F, -9.6F, 28F, 2F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("back_postl", CubeListBuilder.create().texOffs(448, 33).addBox(-15F, -52F, -9.6F, 3F, 20F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("back_postr", CubeListBuilder.create().texOffs(448, 33).addBox(12F, -52F, -9.6F, 3F, 20F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("back_crys_l", CubeListBuilder.create().texOffs(463, 33).addBox(-15.5F, -58F, -9.6F, 4F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("back_crys_r", CubeListBuilder.create().texOffs(463, 33).addBox(11.5F, -58F, -9.6F, 4F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("back_emb", CubeListBuilder.create().texOffs(480, 33).addBox(-6F, -46F, -10.6F, 12F, 8F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("seat_front", CubeListBuilder.create().texOffs(0, 67).addBox(-14F, -31F, 2.3F, 28F, 3F, 6F), PartPose.ZERO);
        body2.addOrReplaceChild("post_fl", CubeListBuilder.create().texOffs(69, 67).addBox(-17F, -40F, 4.3F, 3F, 12F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("post_fr", CubeListBuilder.create().texOffs(69, 67).addBox(14F, -40F, 4.3F, 3F, 12F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("post_crys_l", CubeListBuilder.create().texOffs(463, 33).addBox(-17.5F, -46F, 3.8F, 4F, 6F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("post_crys_r", CubeListBuilder.create().texOffs(463, 33).addBox(13.5F, -46F, 3.8F, 4F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("roll", CubeListBuilder.create().texOffs(82, 67).addBox(-14F, -44F, -5.1F, 28F, 9F, 9F), PartPose.ZERO);
        body.addOrReplaceChild("roll_band_a", CubeListBuilder.create().texOffs(157, 67).addBox(-9F, -44.5F, -5.6F, 3F, 10F, 10F), PartPose.ZERO);
        body.addOrReplaceChild("roll_band_b", CubeListBuilder.create().texOffs(157, 67).addBox(6F, -44.5F, -5.6F, 3F, 10F, 10F), PartPose.ZERO);
        body.addOrReplaceChild("roll_buckle", CubeListBuilder.create().texOffs(184, 67).addBox(-1.5F, -42F, -6.6F, 3F, 4F, 1F), PartPose.ZERO);
        body2.addOrReplaceChild("band_f", CubeListBuilder.create().texOffs(193, 67).addBox(-27.5F, -26.25F, -0.7F, 55F, 1F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("strap_f", CubeListBuilder.create().texOffs(310, 67).addBox(-29.5F, -26.25F, -0.7F, 2F, 38F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("strap_f_r", CubeListBuilder.create().mirror(true).texOffs(310, 67).addBox(27.5F, -26.25F, -0.7F, 2F, 38F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("buckle_f", CubeListBuilder.create().texOffs(321, 67).addBox(-30.5F, -10F, -1.2F, 1F, 6F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("buckle_f_r", CubeListBuilder.create().mirror(true).texOffs(321, 67).addBox(29.5F, -10F, -1.2F, 1F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("band_b", CubeListBuilder.create().texOffs(193, 67).addBox(-27.5F, -26.25F, -9.1F, 55F, 1F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("strap_b", CubeListBuilder.create().texOffs(310, 67).addBox(-29.5F, -26.25F, -9.1F, 2F, 38F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("strap_b_r", CubeListBuilder.create().mirror(true).texOffs(310, 67).addBox(27.5F, -26.25F, -9.1F, 2F, 38F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("buckle_b", CubeListBuilder.create().texOffs(321, 67).addBox(-30.5F, -10F, -9.6F, 1F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("buckle_b_r", CubeListBuilder.create().mirror(true).texOffs(321, 67).addBox(29.5F, -10F, -9.6F, 1F, 6F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_a", CubeListBuilder.create().texOffs(332, 67).addBox(-31.5F, -20F, 3.3F, 2F, 12F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_a_r", CubeListBuilder.create().mirror(true).texOffs(332, 67).addBox(29.5F, -20F, 3.3F, 2F, 12F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_b", CubeListBuilder.create().texOffs(391, 67).addBox(-31F, -10F, 4.3F, 2F, 12F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_b_r", CubeListBuilder.create().mirror(true).texOffs(391, 67).addBox(29F, -10F, 4.3F, 2F, 12F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_c", CubeListBuilder.create().texOffs(444, 67).addBox(-30.5F, 0F, 5.3F, 2F, 12F, 21F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_c_r", CubeListBuilder.create().mirror(true).texOffs(444, 67).addBox(28.5F, 0F, 5.3F, 2F, 12F, 21F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_stud", CubeListBuilder.create().texOffs(491, 67).addBox(-32.5F, -16F, 15.3F, 1F, 2F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fend_stud_r", CubeListBuilder.create().mirror(true).texOffs(491, 67).addBox(31.5F, -16F, 15.3F, 1F, 2F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 109).addBox(-33.5F, -20F, 10.3F, 2F, 28F, 12F), PartPose.ZERO);
        body2.addOrReplaceChild("banner_r", CubeListBuilder.create().mirror(true).texOffs(0, 109).addBox(31.5F, -20F, 10.3F, 2F, 28F, 12F), PartPose.ZERO);
        body2.addOrReplaceChild("banner_pt", CubeListBuilder.create().texOffs(29, 109).addBox(-33.5F, 8F, 12.3F, 2F, 4F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("banner_pt_r", CubeListBuilder.create().mirror(true).texOffs(29, 109).addBox(31.5F, 8F, 12.3F, 2F, 4F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("banner_t", CubeListBuilder.create().texOffs(50, 109).addBox(-33.5F, -21F, 10.3F, 2F, 1F, 12F), PartPose.ZERO);
        body2.addOrReplaceChild("banner_t_r", CubeListBuilder.create().mirror(true).texOffs(50, 109).addBox(31.5F, -21F, 10.3F, 2F, 1F, 12F), PartPose.ZERO);
        body.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(79, 109).addBox(-31.5F, -18F, -5.1F, 5F, 16F, 13F), PartPose.ZERO);
        body.addOrReplaceChild("bag_r", CubeListBuilder.create().mirror(true).texOffs(79, 109).addBox(26.5F, -18F, -5.1F, 5F, 16F, 13F), PartPose.ZERO);
        body.addOrReplaceChild("bag_flap", CubeListBuilder.create().texOffs(116, 109).addBox(-32.5F, -19F, -5.1F, 1F, 6F, 13F), PartPose.ZERO);
        body.addOrReplaceChild("bag_flap_r", CubeListBuilder.create().mirror(true).texOffs(116, 109).addBox(31.5F, -19F, -5.1F, 1F, 6F, 13F), PartPose.ZERO);
        body.addOrReplaceChild("bag_clasp", CubeListBuilder.create().texOffs(145, 109).addBox(-33F, -15F, -0.1F, 1F, 3F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("bag_clasp_r", CubeListBuilder.create().mirror(true).texOffs(145, 109).addBox(32F, -15F, -0.1F, 1F, 3F, 3F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_top", CubeListBuilder.create().texOffs(154, 109).addBox(-17F, -18.1F, -9F, 34F, 2F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_bot", CubeListBuilder.create().texOffs(154, 109).addBox(-17F, 13.9F, -9F, 34F, 2F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_s", CubeListBuilder.create().texOffs(233, 109).addBox(-18F, -18.1F, -9F, 2F, 34F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_s_r", CubeListBuilder.create().mirror(true).texOffs(233, 109).addBox(16F, -18.1F, -9F, 2F, 34F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_g", CubeListBuilder.create().texOffs(248, 109).addBox(-19F, -3F, -8F, 1F, 6F, 3F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_g_r", CubeListBuilder.create().mirror(true).texOffs(248, 109).addBox(18F, -3F, -8F, 1F, 6F, 3F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_med", CubeListBuilder.create().texOffs(257, 109).addBox(-2.5F, 12F, -11F, 5F, 5F, 3F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_top", CubeListBuilder.create().texOffs(274, 109).addBox(-12.5F, -17F, -9F, 25F, 2F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_bot", CubeListBuilder.create().texOffs(274, 109).addBox(-12.5F, 9F, -9F, 25F, 2F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_s", CubeListBuilder.create().texOffs(333, 109).addBox(-13.5F, -17F, -9F, 2F, 28F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_s_r", CubeListBuilder.create().mirror(true).texOffs(333, 109).addBox(11.5F, -17F, -9F, 2F, 28F, 4F), PartPose.ZERO);
        head.addOrReplaceChild("hs_a", CubeListBuilder.create().texOffs(346, 109).addBox(-8.5F, -1F, -12F, 17F, 2F, 3F), PartPose.ZERO);
        head.addOrReplaceChild("hs_b", CubeListBuilder.create().texOffs(346, 109).addBox(-8.5F, -1F, -5F, 17F, 2F, 3F), PartPose.ZERO);
        head.addOrReplaceChild("hs_side", CubeListBuilder.create().texOffs(387, 109).addBox(-9.5F, -1F, -12F, 2F, 2F, 10F), PartPose.ZERO);
        head.addOrReplaceChild("hs_side_r", CubeListBuilder.create().mirror(true).texOffs(387, 109).addBox(7.5F, -1F, -12F, 2F, 2F, 10F), PartPose.ZERO);
        head.addOrReplaceChild("hs_ring", CubeListBuilder.create().texOffs(412, 109).addBox(-10.5F, 0F, -8F, 2F, 3F, 3F), PartPose.ZERO);
        head.addOrReplaceChild("hs_ring_r", CubeListBuilder.create().mirror(true).texOffs(412, 109).addBox(8.5F, 0F, -8F, 2F, 3F, 3F), PartPose.ZERO);
        snout.addOrReplaceChild("sn_band", CubeListBuilder.create().texOffs(423, 109).addBox(-6F, -0.5F, -13F, 12F, 10F, 3F), PartPose.ZERO);
        snout.addOrReplaceChild("sn_buckle", CubeListBuilder.create().texOffs(454, 109).addBox(-6.8F, 3F, -12.5F, 1F, 3F, 2F), PartPose.ZERO);
        snout.addOrReplaceChild("sn_buckle_r", CubeListBuilder.create().mirror(true).texOffs(454, 109).addBox(5.8F, 3F, -12.5F, 1F, 3F, 2F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_top", CubeListBuilder.create().texOffs(0, 150).addBox(-15.5F, -18.5F, 18F, 31F, 2F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_bot", CubeListBuilder.create().texOffs(0, 150).addBox(-15.5F, 16.5F, 18F, 31F, 2F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_s", CubeListBuilder.create().texOffs(69, 150).addBox(-16.5F, -18.5F, 18F, 2F, 37F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_s_r", CubeListBuilder.create().mirror(true).texOffs(69, 150).addBox(14.5F, -18.5F, 18F, 2F, 37F, 3F), PartPose.ZERO);
        arm_l.addOrReplaceChild("lband", CubeListBuilder.create().texOffs(80, 150).addBox(-9.4F, 30F, -11.3F, 22F, 3F, 23F), PartPose.ZERO);
        arm_r.addOrReplaceChild("lband_r", CubeListBuilder.create().mirror(true).texOffs(80, 150).addBox(-12.6F, 30F, -11.3F, 22F, 3F, 23F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }
}
