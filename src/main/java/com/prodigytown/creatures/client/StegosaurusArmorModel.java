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
 * Armadura del estegosaurio (hierro, oro, diamante, netherita; solo cambia la textura): casco con cresta y pomulos, gorguera,
 * peto con gema, hombreras con pinchos, protectores de vientre, muslos, rodillas y patas, y placas articuladas en la cola.
 * GENERADO por scratchpad/gear/gen3.py junto con sus texturas; no editar a mano.
 * Misma jerarquia de huesos que StegosaurusModel; copia su pose cada cuadro.
 */
public final class StegosaurusArmorModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus_armor"), "main");
    private static final String[] TIER_NAMES = {"iron", "gold", "diamond", "netherite"};
    /** Textura por nivel de armadura (1 = hierro .. 4 = netherite). */
    public static ResourceLocation texture(int tier) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_armor_" + TIER_NAMES[Math.max(1, Math.min(4, tier)) - 1] + ".png");
    }
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
    private final ModelPart[] bagParts;

    public StegosaurusArmorModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        for (int i = 0; i < TRACK.length; i++) mine[i] = walk(root, TRACK[i]);
        this.bagParts = new ModelPart[] {};
        setBags(false);
    }

    /** Muestra u oculta las maletas de almacenamiento. */
    public void setBags(boolean v) { for (ModelPart p : bagParts) p.visible = v; }

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
        head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(0, 0).addBox(-9.5F, -8.5F, -14.5F, 19F, 3F, 18F), PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(75, 0).addBox(-9.5F, -5.5F, -15.5F, 19F, 2F, 2F), PartPose.ZERO);
        head.addOrReplaceChild("crest_a", CubeListBuilder.create().texOffs(118, 0).addBox(-1.5F, -12.5F, -11F, 3F, 4F, 4F), PartPose.ZERO);
        head.addOrReplaceChild("crest_b", CubeListBuilder.create().texOffs(133, 0).addBox(-1.5F, -13.5F, -5F, 3F, 5F, 4F), PartPose.ZERO);
        head.addOrReplaceChild("crest_c", CubeListBuilder.create().texOffs(148, 0).addBox(-1.5F, -12.5F, 0F, 3F, 4F, 3F), PartPose.ZERO);
        head.addOrReplaceChild("helm_gem", CubeListBuilder.create().texOffs(161, 0).addBox(-2F, -8F, -16.3F, 4F, 3F, 1F), PartPose.ZERO);
        head.addOrReplaceChild("cheek", CubeListBuilder.create().texOffs(172, 0).addBox(-11F, 3F, -12F, 2F, 6F, 14F), PartPose.ZERO);
        head.addOrReplaceChild("cheek_r", CubeListBuilder.create().mirror(true).texOffs(172, 0).addBox(9F, 3F, -12F, 2F, 6F, 14F), PartPose.ZERO);
        head.addOrReplaceChild("cheek_sp", CubeListBuilder.create().texOffs(205, 0).addBox(-14F, 5F, -6F, 3F, 3F, 7F), PartPose.ZERO);
        head.addOrReplaceChild("cheek_sp_r", CubeListBuilder.create().mirror(true).texOffs(205, 0).addBox(11F, 5F, -6F, 3F, 3F, 7F), PartPose.ZERO);
        snout.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(226, 0).addBox(-6.5F, -2.5F, -18.5F, 13F, 2F, 17F), PartPose.ZERO);
        snout.addOrReplaceChild("muz_tip", CubeListBuilder.create().texOffs(287, 0).addBox(-6.5F, -2.5F, -19.5F, 13F, 6F, 1F), PartPose.ZERO);
        snout.addOrReplaceChild("muz_gem", CubeListBuilder.create().texOffs(316, 0).addBox(-2F, -4.5F, -14F, 4F, 3F, 4F), PartPose.ZERO);
        neck2.addOrReplaceChild("gorget_a", CubeListBuilder.create().texOffs(333, 0).addBox(-17.5F, -18.5F, -10F, 35F, 3F, 13F), PartPose.ZERO);
        neck.addOrReplaceChild("gorget_b", CubeListBuilder.create().texOffs(430, 0).addBox(-13F, -18.5F, -10F, 26F, 3F, 11F), PartPose.ZERO);
        neck2.addOrReplaceChild("gorget_tr", CubeListBuilder.create().texOffs(0, 22).addBox(-17.5F, -16F, -10F, 35F, 1F, 13F), PartPose.ZERO);
        neck2.addOrReplaceChild("neck_sp_a", CubeListBuilder.create().texOffs(97, 22).addBox(-2F, -23F, -6F, 4F, 5F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("neck_sp_b", CubeListBuilder.create().texOffs(97, 22).addBox(-2F, -22.5F, -6F, 4F, 5F, 4F), PartPose.ZERO);
        arm_l.addOrReplaceChild("paul", CubeListBuilder.create().texOffs(114, 22).addBox(-11F, -6.5F, -12.5F, 25F, 9F, 25F), PartPose.ZERO);
        arm_r.addOrReplaceChild("paul_r", CubeListBuilder.create().mirror(true).texOffs(114, 22).addBox(-14F, -6.5F, -12.5F, 25F, 9F, 25F), PartPose.ZERO);
        arm_l.addOrReplaceChild("paul_tr", CubeListBuilder.create().texOffs(215, 22).addBox(-11.5F, -0.5F, -13F, 26F, 2F, 26F), PartPose.ZERO);
        arm_r.addOrReplaceChild("paul_tr_r", CubeListBuilder.create().mirror(true).texOffs(215, 22).addBox(-14.5F, -0.5F, -13F, 26F, 2F, 26F), PartPose.ZERO);
        arm_l.addOrReplaceChild("paul_sp", CubeListBuilder.create().texOffs(320, 22).addBox(-0.4F, -12.5F, -3F, 4F, 6F, 4F), PartPose.ZERO);
        arm_r.addOrReplaceChild("paul_sp_r", CubeListBuilder.create().mirror(true).texOffs(320, 22).addBox(-3.6F, -12.5F, -3F, 4F, 6F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("belly_f", CubeListBuilder.create().texOffs(337, 22).addBox(-29.5F, 13F, -22.7F, 2F, 12F, 22F), PartPose.ZERO);
        body2.addOrReplaceChild("belly_f_r", CubeListBuilder.create().mirror(true).texOffs(337, 22).addBox(27.5F, 13F, -22.7F, 2F, 12F, 22F), PartPose.ZERO);
        body.addOrReplaceChild("belly_b", CubeListBuilder.create().texOffs(386, 22).addBox(-29.5F, 13F, -14.1F, 2F, 12F, 26F), PartPose.ZERO);
        body.addOrReplaceChild("belly_b_r", CubeListBuilder.create().mirror(true).texOffs(386, 22).addBox(27.5F, 13F, -14.1F, 2F, 12F, 26F), PartPose.ZERO);
        body2.addOrReplaceChild("belly_ft", CubeListBuilder.create().texOffs(443, 22).addBox(-30F, 13F, -22.7F, 1F, 2F, 22F), PartPose.ZERO);
        body2.addOrReplaceChild("belly_ft_r", CubeListBuilder.create().mirror(true).texOffs(443, 22).addBox(29F, 13F, -22.7F, 1F, 2F, 22F), PartPose.ZERO);
        body.addOrReplaceChild("belly_bt", CubeListBuilder.create().texOffs(0, 61).addBox(-30F, 13F, -14.1F, 1F, 2F, 26F), PartPose.ZERO);
        body.addOrReplaceChild("belly_bt_r", CubeListBuilder.create().mirror(true).texOffs(0, 61).addBox(29F, 13F, -14.1F, 1F, 2F, 26F), PartPose.ZERO);
        leg_l.addOrReplaceChild("thigh", CubeListBuilder.create().texOffs(55, 61).addBox(-10.5F, 2F, -14F, 2F, 22F, 22F), PartPose.ZERO);
        leg_r.addOrReplaceChild("thigh_r", CubeListBuilder.create().mirror(true).texOffs(55, 61).addBox(8.5F, 2F, -14F, 2F, 22F, 22F), PartPose.ZERO);
        leg_l.addOrReplaceChild("thigh_top", CubeListBuilder.create().texOffs(104, 61).addBox(-10F, -5F, -18F, 21F, 2F, 29F), PartPose.ZERO);
        leg_r.addOrReplaceChild("thigh_top_r", CubeListBuilder.create().mirror(true).texOffs(104, 61).addBox(-11F, -5F, -18F, 21F, 2F, 29F), PartPose.ZERO);
        leg_l.addOrReplaceChild("thigh_tr", CubeListBuilder.create().texOffs(205, 61).addBox(-10.5F, 12F, -14F, 2F, 2F, 22F), PartPose.ZERO);
        leg_r.addOrReplaceChild("thigh_tr_r", CubeListBuilder.create().mirror(true).texOffs(205, 61).addBox(8.5F, 12F, -14F, 2F, 2F, 22F), PartPose.ZERO);
        leg_l2.addOrReplaceChild("knee", CubeListBuilder.create().texOffs(254, 61).addBox(-11.3F, -3F, -10.4F, 22F, 5F, 21F), PartPose.ZERO);
        leg_r2.addOrReplaceChild("knee_r", CubeListBuilder.create().mirror(true).texOffs(254, 61).addBox(-10.7F, -3F, -10.4F, 22F, 5F, 21F), PartPose.ZERO);
        leg_l2.addOrReplaceChild("greave", CubeListBuilder.create().texOffs(341, 61).addBox(-11.3F, 4F, -10.4F, 22F, 12F, 21F), PartPose.ZERO);
        leg_r2.addOrReplaceChild("greave_r", CubeListBuilder.create().mirror(true).texOffs(341, 61).addBox(-10.7F, 4F, -10.4F, 22F, 12F, 21F), PartPose.ZERO);
        arm_l.addOrReplaceChild("brace", CubeListBuilder.create().texOffs(0, 106).addBox(-9.4F, 26F, -11.3F, 22F, 12F, 23F), PartPose.ZERO);
        arm_r.addOrReplaceChild("brace_r", CubeListBuilder.create().mirror(true).texOffs(0, 106).addBox(-12.6F, 26F, -11.3F, 22F, 12F, 23F), PartPose.ZERO);
        hand_l.addOrReplaceChild("hand_cuff", CubeListBuilder.create().texOffs(91, 106).addBox(-11.5F, -0.5F, -13.5F, 23F, 4F, 23F), PartPose.ZERO);
        hand_r.addOrReplaceChild("hand_cuff_r", CubeListBuilder.create().mirror(true).texOffs(91, 106).addBox(-11.5F, -0.5F, -13.5F, 23F, 4F, 23F), PartPose.ZERO);
        leg_l2.addOrReplaceChild("greave_gem", CubeListBuilder.create().texOffs(184, 106).addBox(-12.4F, 6F, -3F, 1F, 6F, 6F), PartPose.ZERO);
        leg_r2.addOrReplaceChild("greave_gem_r", CubeListBuilder.create().mirror(true).texOffs(184, 106).addBox(11.4F, 6F, -3F, 1F, 6F, 6F), PartPose.ZERO);
        leg_l2.addOrReplaceChild("greave_tr", CubeListBuilder.create().texOffs(199, 106).addBox(-11.8F, 3F, -10.9F, 23F, 2F, 22F), PartPose.ZERO);
        leg_r2.addOrReplaceChild("greave_tr_r", CubeListBuilder.create().mirror(true).texOffs(199, 106).addBox(-11.2F, 3F, -10.9F, 23F, 2F, 22F), PartPose.ZERO);
        arm_l.addOrReplaceChild("brace_gem", CubeListBuilder.create().texOffs(184, 106).addBox(-10.4F, 29F, -3F, 1F, 6F, 6F), PartPose.ZERO);
        arm_r.addOrReplaceChild("brace_gem_r", CubeListBuilder.create().mirror(true).texOffs(184, 106).addBox(9.4F, 29F, -3F, 1F, 6F, 6F), PartPose.ZERO);
        foot_l.addOrReplaceChild("toe_cap", CubeListBuilder.create().texOffs(290, 106).addBox(-11.6F, 2F, -5.5F, 23F, 6F, 3F), PartPose.ZERO);
        foot_r.addOrReplaceChild("toe_cap_r", CubeListBuilder.create().mirror(true).texOffs(290, 106).addBox(-11.4F, 2F, -5.5F, 23F, 6F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("t_plate1", CubeListBuilder.create().texOffs(343, 106).addBox(-15.5F, -20F, 2F, 31F, 3F, 20F), PartPose.ZERO);
        tail2.addOrReplaceChild("t_plate2", CubeListBuilder.create().texOffs(0, 142).addBox(-11F, -13F, 2F, 22F, 3F, 18F), PartPose.ZERO);
        tail3.addOrReplaceChild("t_plate3", CubeListBuilder.create().texOffs(81, 142).addBox(-8.5F, -10.5F, 2F, 17F, 3F, 18F), PartPose.ZERO);
        tail4.addOrReplaceChild("t_plate4", CubeListBuilder.create().texOffs(152, 142).addBox(-6F, -8.5F, 3F, 12F, 3F, 22F), PartPose.ZERO);
        tail.addOrReplaceChild("t_side", CubeListBuilder.create().texOffs(221, 142).addBox(-16.5F, -12F, 3F, 2F, 22F, 18F), PartPose.ZERO);
        tail.addOrReplaceChild("t_side_r", CubeListBuilder.create().mirror(true).texOffs(221, 142).addBox(14.5F, -12F, 3F, 2F, 22F, 18F), PartPose.ZERO);
        tail2.addOrReplaceChild("t_side2", CubeListBuilder.create().texOffs(262, 142).addBox(-11.5F, -6F, 3F, 2F, 13F, 16F), PartPose.ZERO);
        tail2.addOrReplaceChild("t_side2_r", CubeListBuilder.create().mirror(true).texOffs(262, 142).addBox(9.5F, -6F, 3F, 2F, 13F, 16F), PartPose.ZERO);
        tail4.addOrReplaceChild("t_cap", CubeListBuilder.create().texOffs(299, 142).addBox(-5.5F, -7F, 23F, 11F, 13F, 6F), PartPose.ZERO);
        tail.addOrReplaceChild("t_gem", CubeListBuilder.create().texOffs(334, 142).addBox(-2F, -21.2F, 10F, 4F, 2F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(351, 142).addBox(-26F, -21F, -28.2F, 52F, 6F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_b", CubeListBuilder.create().texOffs(0, 183).addBox(-23F, -15F, -28.2F, 46F, 5F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_c", CubeListBuilder.create().texOffs(99, 183).addBox(-19F, -10F, -28.2F, 38F, 5F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_d", CubeListBuilder.create().texOffs(182, 183).addBox(-15F, -5F, -28.2F, 30F, 5F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_e", CubeListBuilder.create().texOffs(249, 183).addBox(-11F, 0F, -28.2F, 22F, 5F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_f", CubeListBuilder.create().texOffs(300, 183).addBox(-7F, 5F, -28.2F, 14F, 5F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_g", CubeListBuilder.create().texOffs(335, 183).addBox(-3F, 10F, -28.2F, 6F, 5F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_tr", CubeListBuilder.create().texOffs(354, 183).addBox(-27F, -22F, -29.2F, 54F, 2F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_el", CubeListBuilder.create().texOffs(466, 183).addBox(-27F, -20F, -29.2F, 3F, 6F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_er", CubeListBuilder.create().texOffs(466, 183).addBox(24F, -20F, -29.2F, 3F, 6F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_bl", CubeListBuilder.create().texOffs(476, 183).addBox(-24F, -14F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_br", CubeListBuilder.create().texOffs(476, 183).addBox(21F, -14F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_cl", CubeListBuilder.create().texOffs(476, 183).addBox(-20F, -9F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_cr", CubeListBuilder.create().texOffs(476, 183).addBox(17F, -9F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_dl", CubeListBuilder.create().texOffs(476, 183).addBox(-16F, -4F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_dr", CubeListBuilder.create().texOffs(476, 183).addBox(13F, -4F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_fl", CubeListBuilder.create().texOffs(476, 183).addBox(-12F, 1F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_fr", CubeListBuilder.create().texOffs(476, 183).addBox(9F, 1F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_gl", CubeListBuilder.create().texOffs(476, 183).addBox(-8F, 6F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_gr", CubeListBuilder.create().texOffs(476, 183).addBox(5F, 6F, -29.2F, 3F, 5F, 1.5F), PartPose.ZERO);
        body2.addOrReplaceChild("chest_gem", CubeListBuilder.create().texOffs(486, 183).addBox(-4F, -16F, -30.7F, 8F, 8F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_a", CubeListBuilder.create().texOffs(0, 194).addBox(-32.5F, -20F, -26.7F, 2F, 5F, 32F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_a_r", CubeListBuilder.create().mirror(true).texOffs(0, 194).addBox(30.5F, -20F, -26.7F, 2F, 5F, 32F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_b", CubeListBuilder.create().texOffs(69, 194).addBox(-32.5F, -15F, -24.7F, 2F, 5F, 28F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_b_r", CubeListBuilder.create().mirror(true).texOffs(69, 194).addBox(30.5F, -15F, -24.7F, 2F, 5F, 28F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_c", CubeListBuilder.create().texOffs(130, 194).addBox(-32.5F, -10F, -22.7F, 2F, 5F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_c_r", CubeListBuilder.create().mirror(true).texOffs(130, 194).addBox(30.5F, -10F, -22.7F, 2F, 5F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_d", CubeListBuilder.create().texOffs(183, 194).addBox(-32.5F, -5F, -20.7F, 2F, 5F, 20F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_d_r", CubeListBuilder.create().mirror(true).texOffs(183, 194).addBox(30.5F, -5F, -20.7F, 2F, 5F, 20F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e", CubeListBuilder.create().texOffs(228, 194).addBox(-32.5F, 0F, -18.7F, 2F, 5F, 16F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e_r", CubeListBuilder.create().mirror(true).texOffs(228, 194).addBox(30.5F, 0F, -18.7F, 2F, 5F, 16F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_f", CubeListBuilder.create().texOffs(265, 194).addBox(-32.5F, 5F, -16.7F, 2F, 5F, 12F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_f_r", CubeListBuilder.create().mirror(true).texOffs(265, 194).addBox(30.5F, 5F, -16.7F, 2F, 5F, 12F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_g", CubeListBuilder.create().texOffs(294, 194).addBox(-32.5F, 10F, -14.7F, 2F, 4F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_g_r", CubeListBuilder.create().mirror(true).texOffs(294, 194).addBox(30.5F, 10F, -14.7F, 2F, 4F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_top", CubeListBuilder.create().texOffs(315, 194).addBox(-33.7F, -21F, -26.7F, 1.2F, 2F, 32F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_top_r", CubeListBuilder.create().mirror(true).texOffs(315, 194).addBox(32.5F, -21F, -26.7F, 1.2F, 2F, 32F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e1", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -19F, -26.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e1_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -19F, -26.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e2", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -19F, 3.3F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e2_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -19F, 3.3F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e3", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -14F, -24.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e3_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -14F, -24.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e4", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -14F, 1.3F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e4_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -14F, 1.3F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e5", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -9F, -22.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e5_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -9F, -22.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e6", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -9F, -0.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e6_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -9F, -0.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e7", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -4F, -20.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e7_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -4F, -20.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e8", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, -4F, -2.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e8_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, -4F, -2.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e9", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, 1F, -18.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e9_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, 1F, -18.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e10", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, 1F, -4.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e10_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, 1F, -4.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e11", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, 6F, -16.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e11_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, 6F, -16.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e12", CubeListBuilder.create().texOffs(382, 194).addBox(-33.7F, 6F, -6.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_e12_r", CubeListBuilder.create().mirror(true).texOffs(382, 194).addBox(32.5F, 6F, -6.7F, 1.2F, 5F, 2F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_tip", CubeListBuilder.create().texOffs(389, 194).addBox(-33.7F, 10F, -14.7F, 1.2F, 3F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("fl_tip_r", CubeListBuilder.create().mirror(true).texOffs(389, 194).addBox(32.5F, 10F, -14.7F, 1.2F, 3F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("flank_gem", CubeListBuilder.create().texOffs(408, 194).addBox(-34.4F, -12F, -15.7F, 1.5F, 8F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("flank_gem_r", CubeListBuilder.create().mirror(true).texOffs(408, 194).addBox(32.9F, -12F, -15.7F, 1.5F, 8F, 8F), PartPose.ZERO);
        neck2.addOrReplaceChild("neck_gem", CubeListBuilder.create().texOffs(428, 194).addBox(-2.5F, -19.5F, -9F, 5F, 3F, 5F), PartPose.ZERO);
        leg_l2.addOrReplaceChild("knee_gem", CubeListBuilder.create().texOffs(449, 194).addBox(-12.4F, -4F, -4F, 1F, 4F, 4F), PartPose.ZERO);
        leg_r2.addOrReplaceChild("knee_gem_r", CubeListBuilder.create().mirror(true).texOffs(449, 194).addBox(11.4F, -4F, -4F, 1F, 4F, 4F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }
}
