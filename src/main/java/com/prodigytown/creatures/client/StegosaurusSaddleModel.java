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
 * Equipo de montura del estegosaurio: manta, silla con pomo y cantle, rieles, cinchas con hebillas, laminas laterales,
 * estandarte con la hoja, collar y gorguera, casco con cresta, protectores de patas y de cola.
 * GENERADO por scratchpad/saddle/gen_saddle2.py junto con textures/entity/stegosaurus_saddle.png; no editar a mano.
 * Tiene la misma jerarquia de huesos que StegosaurusModel y copia su pose cada cuadro, asi que sigue al cuello,
 * la cola y las patas.
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
        {"all", "body2", "arm_l"},
        {"all", "body2", "arm_r"},
        {"all", "leg_l"},
        {"all", "leg_r"},
        {"all", "leg_l", "leg_l2"},
        {"all", "leg_r", "leg_r2"}
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
        PartDefinition arm_l = body2.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offset(-25.76F, -11.5F, -6.9F));
        PartDefinition arm_r = body2.addOrReplaceChild("arm_r", CubeListBuilder.create(), PartPose.offset(25.76F, -11.5F, -6.9F));
        PartDefinition leg_l = all.addOrReplaceChild("leg_l", CubeListBuilder.create(), PartPose.offset(-25.99F, -57.5F, 28.75F));
        PartDefinition leg_r = all.addOrReplaceChild("leg_r", CubeListBuilder.create(), PartPose.offset(25.99F, -57.5F, 28.75F));
        PartDefinition leg_l2 = leg_l.addOrReplaceChild("leg_l2", CubeListBuilder.create(), PartPose.offset(0F, 27.14F, 2.3F));
        PartDefinition leg_r2 = leg_r.addOrReplaceChild("leg_r2", CubeListBuilder.create(), PartPose.offset(0F, 27.14F, 2.3F));
        body2.addOrReplaceChild("blanket", CubeListBuilder.create().texOffs(0, 0).addBox(-22F, -27F, 3.3F, 44F, 2F, 30F), PartPose.ZERO);
        body2.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(148, 0).addBox(-15F, -29F, 8.3F, 30F, 4F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("pommel", CubeListBuilder.create().texOffs(256, 0).addBox(-6F, -31F, 5.3F, 12F, 6F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("pommel_t", CubeListBuilder.create().texOffs(286, 0).addBox(-6F, -32F, 5.3F, 12F, 1F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("rail", CubeListBuilder.create().texOffs(316, 0).addBox(-17F, -31F, 8.3F, 2F, 6F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("rail_r", CubeListBuilder.create().mirror(true).texOffs(316, 0).addBox(15F, -31F, 8.3F, 2F, 6F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("rail_t", CubeListBuilder.create().texOffs(374, 0).addBox(-17F, -32F, 8.3F, 2F, 1F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("rail_t_r", CubeListBuilder.create().mirror(true).texOffs(374, 0).addBox(15F, -32F, 8.3F, 2F, 1F, 27F), PartPose.ZERO);
        body.addOrReplaceChild("cantle", CubeListBuilder.create().texOffs(432, 0).addBox(-15F, -33F, -9.1F, 30F, 8F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("cantle_t", CubeListBuilder.create().texOffs(0, 33).addBox(-15F, -34F, -9.1F, 30F, 1F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("band_f", CubeListBuilder.create().texOffs(66, 33).addBox(-27.5F, -26.25F, -0.7F, 55F, 1F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("strap_f", CubeListBuilder.create().texOffs(182, 33).addBox(-29.5F, -26.25F, -0.7F, 2F, 38F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("strap_f_r", CubeListBuilder.create().mirror(true).texOffs(182, 33).addBox(27.5F, -26.25F, -0.7F, 2F, 38F, 3F), PartPose.ZERO);
        body2.addOrReplaceChild("buckle_f", CubeListBuilder.create().texOffs(192, 33).addBox(-30.5F, -10F, -1.2F, 1F, 6F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("buckle_f_r", CubeListBuilder.create().mirror(true).texOffs(192, 33).addBox(29.5F, -10F, -1.2F, 1F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("band_b", CubeListBuilder.create().texOffs(66, 33).addBox(-27.5F, -26.25F, -9.1F, 55F, 1F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("strap_b", CubeListBuilder.create().texOffs(182, 33).addBox(-29.5F, -26.25F, -9.1F, 2F, 38F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("strap_b_r", CubeListBuilder.create().mirror(true).texOffs(182, 33).addBox(27.5F, -26.25F, -9.1F, 2F, 38F, 3F), PartPose.ZERO);
        body.addOrReplaceChild("buckle_b", CubeListBuilder.create().texOffs(192, 33).addBox(-30.5F, -10F, -9.6F, 1F, 6F, 4F), PartPose.ZERO);
        body.addOrReplaceChild("buckle_b_r", CubeListBuilder.create().mirror(true).texOffs(192, 33).addBox(29.5F, -10F, -9.6F, 1F, 6F, 4F), PartPose.ZERO);
        body2.addOrReplaceChild("lame_a", CubeListBuilder.create().texOffs(202, 33).addBox(-31.5F, -20F, 3.3F, 2F, 12F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("lame_a_r", CubeListBuilder.create().mirror(true).texOffs(202, 33).addBox(29.5F, -20F, 3.3F, 2F, 12F, 27F), PartPose.ZERO);
        body2.addOrReplaceChild("lame_b", CubeListBuilder.create().texOffs(260, 33).addBox(-31F, -10F, 4.3F, 2F, 12F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("lame_b_r", CubeListBuilder.create().mirror(true).texOffs(260, 33).addBox(29F, -10F, 4.3F, 2F, 12F, 24F), PartPose.ZERO);
        body2.addOrReplaceChild("lame_c", CubeListBuilder.create().texOffs(312, 33).addBox(-30.5F, 0F, 5.3F, 2F, 12F, 21F), PartPose.ZERO);
        body2.addOrReplaceChild("lame_c_r", CubeListBuilder.create().mirror(true).texOffs(312, 33).addBox(28.5F, 0F, 5.3F, 2F, 12F, 21F), PartPose.ZERO);
        body2.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(358, 33).addBox(-33.5F, -18F, 12.3F, 2F, 26F, 8F), PartPose.ZERO);
        body2.addOrReplaceChild("banner_r", CubeListBuilder.create().mirror(true).texOffs(358, 33).addBox(31.5F, -18F, 12.3F, 2F, 26F, 8F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_top", CubeListBuilder.create().texOffs(378, 33).addBox(-17F, -18.1F, -9F, 34F, 2F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_bot", CubeListBuilder.create().texOffs(378, 33).addBox(-17F, 13.9F, -9F, 34F, 2F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_s", CubeListBuilder.create().texOffs(456, 33).addBox(-18F, -18.1F, -9F, 2F, 34F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_s_r", CubeListBuilder.create().mirror(true).texOffs(456, 33).addBox(16F, -18.1F, -9F, 2F, 34F, 5F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_g", CubeListBuilder.create().texOffs(470, 33).addBox(-19F, -3F, -8F, 1F, 6F, 3F), PartPose.ZERO);
        neck2.addOrReplaceChild("collar_g_r", CubeListBuilder.create().mirror(true).texOffs(470, 33).addBox(18F, -3F, -8F, 1F, 6F, 3F), PartPose.ZERO);
        neck2.addOrReplaceChild("gorget", CubeListBuilder.create().texOffs(0, 74).addBox(-12F, -20F, -14F, 24F, 2F, 12F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_top", CubeListBuilder.create().texOffs(72, 74).addBox(-12.5F, -17F, -9F, 25F, 2F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_bot", CubeListBuilder.create().texOffs(72, 74).addBox(-12.5F, 9F, -9F, 25F, 2F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_s", CubeListBuilder.create().texOffs(130, 74).addBox(-13.5F, -17F, -9F, 2F, 28F, 4F), PartPose.ZERO);
        neck.addOrReplaceChild("nband_s_r", CubeListBuilder.create().mirror(true).texOffs(130, 74).addBox(11.5F, -17F, -9F, 2F, 28F, 4F), PartPose.ZERO);
        head.addOrReplaceChild("helm", CubeListBuilder.create().texOffs(142, 74).addBox(-8.5F, -8.25F, -12F, 17F, 2F, 14F), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(204, 74).addBox(-1F, -10.25F, -10F, 2F, 2F, 10F), PartPose.ZERO);
        head.addOrReplaceChild("cheek", CubeListBuilder.create().texOffs(228, 74).addBox(-10F, -3F, -11F, 2F, 9F, 12F), PartPose.ZERO);
        head.addOrReplaceChild("cheek_r", CubeListBuilder.create().mirror(true).texOffs(228, 74).addBox(8F, -3F, -11F, 2F, 9F, 12F), PartPose.ZERO);
        snout.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(256, 74).addBox(-6F, -2F, -18F, 12F, 2F, 16F), PartPose.ZERO);
        tail.addOrReplaceChild("tguard", CubeListBuilder.create().texOffs(312, 74).addBox(-12F, -19.5F, 3F, 24F, 2F, 16F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_top", CubeListBuilder.create().texOffs(392, 74).addBox(-15.5F, -18.5F, 18F, 31F, 2F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_bot", CubeListBuilder.create().texOffs(392, 74).addBox(-15.5F, 16.5F, 18F, 31F, 2F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_s", CubeListBuilder.create().texOffs(460, 74).addBox(-16.5F, -18.5F, 18F, 2F, 37F, 3F), PartPose.ZERO);
        tail.addOrReplaceChild("tband_s_r", CubeListBuilder.create().mirror(true).texOffs(460, 74).addBox(14.5F, -18.5F, 18F, 2F, 37F, 3F), PartPose.ZERO);
        arm_l.addOrReplaceChild("brace", CubeListBuilder.create().texOffs(0, 114).addBox(-9.4F, 26F, -11.3F, 22F, 12F, 23F), PartPose.ZERO);
        arm_r.addOrReplaceChild("brace_r", CubeListBuilder.create().mirror(true).texOffs(0, 114).addBox(-12.6F, 26F, -11.3F, 22F, 12F, 23F), PartPose.ZERO);
        leg_l2.addOrReplaceChild("greave", CubeListBuilder.create().texOffs(90, 114).addBox(-11.3F, 4F, -10.4F, 22F, 12F, 21F), PartPose.ZERO);
        leg_r2.addOrReplaceChild("greave_r", CubeListBuilder.create().mirror(true).texOffs(90, 114).addBox(-10.7F, 4F, -10.4F, 22F, 12F, 21F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }
}
