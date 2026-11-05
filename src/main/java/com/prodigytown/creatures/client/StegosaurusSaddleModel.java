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
 * Silla de montar del estegosaurio: asiento con respaldo, rieles dorados, cinchas, laminas laterales y estandarte.
 * GENERADO por scratchpad/saddle/gen_saddle.py junto con textures/entity/stegosaurus_saddle.png; no editar a mano
 * (cambiar el generador). Las coordenadas estan en el mismo marco que StegosaurusModel (parte "all", suelo y=0).
 */
public final class StegosaurusSaddleModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus_saddle"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_saddle.png");
    private final ModelPart root;

    public StegosaurusSaddleModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public ModelPart all() { return root.getChild("all"); }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                         float red, float green, float blue, float alpha) {
        root.render(pose, consumer, light, overlay, red, green, blue, alpha);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition all = mesh.getRoot().addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
        all.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(0, 0).addBox(-15F, -76F, -17F, 30F, 5F, 24F), PartPose.ZERO);
        all.addOrReplaceChild("backrest", CubeListBuilder.create().texOffs(108, 0).addBox(-15F, -89F, 7F, 30F, 17F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("backtrim", CubeListBuilder.create().texOffs(174, 0).addBox(-15F, -91F, 7F, 30F, 2F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("pommel", CubeListBuilder.create().texOffs(0, 29).addBox(-6F, -80F, -19F, 12F, 4F, 2F), PartPose.ZERO);
        all.addOrReplaceChild("rail", CubeListBuilder.create().texOffs(28, 29).addBox(-17F, -81F, -17F, 2F, 5F, 27F), PartPose.ZERO);
        all.addOrReplaceChild("rail_r", CubeListBuilder.create().mirror(true).texOffs(28, 29).addBox(15F, -81F, -17F, 2F, 5F, 27F), PartPose.ZERO);
        all.addOrReplaceChild("railtrim", CubeListBuilder.create().texOffs(86, 29).addBox(-17F, -83F, -17F, 2F, 2F, 27F), PartPose.ZERO);
        all.addOrReplaceChild("railtrim_r", CubeListBuilder.create().mirror(true).texOffs(86, 29).addBox(15F, -83F, -17F, 2F, 2F, 27F), PartPose.ZERO);
        all.addOrReplaceChild("band_f", CubeListBuilder.create().texOffs(0, 61).addBox(-27.5F, -72.25F, -26F, 55F, 1F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("band_b", CubeListBuilder.create().texOffs(0, 61).addBox(-27.5F, -72.25F, 7F, 55F, 1F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("strap_f", CubeListBuilder.create().texOffs(116, 61).addBox(-29.5F, -72.25F, -26F, 2F, 38F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("strap_f_r", CubeListBuilder.create().mirror(true).texOffs(116, 61).addBox(27.5F, -72.25F, -26F, 2F, 38F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("strap_b", CubeListBuilder.create().texOffs(116, 61).addBox(-29.5F, -72.25F, 7F, 2F, 38F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("strap_b_r", CubeListBuilder.create().mirror(true).texOffs(116, 61).addBox(27.5F, -72.25F, 7F, 2F, 38F, 3F), PartPose.ZERO);
        all.addOrReplaceChild("buckle_f", CubeListBuilder.create().texOffs(126, 61).addBox(-30.5F, -56F, -26.5F, 1F, 6F, 4F), PartPose.ZERO);
        all.addOrReplaceChild("buckle_f_r", CubeListBuilder.create().mirror(true).texOffs(126, 61).addBox(29.5F, -56F, -26.5F, 1F, 6F, 4F), PartPose.ZERO);
        all.addOrReplaceChild("buckle_b", CubeListBuilder.create().texOffs(126, 61).addBox(-30.5F, -56F, 6.5F, 1F, 6F, 4F), PartPose.ZERO);
        all.addOrReplaceChild("buckle_b_r", CubeListBuilder.create().mirror(true).texOffs(126, 61).addBox(29.5F, -56F, 6.5F, 1F, 6F, 4F), PartPose.ZERO);
        all.addOrReplaceChild("lame_a", CubeListBuilder.create().texOffs(136, 61).addBox(-31.5F, -66F, -22F, 2F, 12F, 27F), PartPose.ZERO);
        all.addOrReplaceChild("lame_a_r", CubeListBuilder.create().mirror(true).texOffs(136, 61).addBox(29.5F, -66F, -22F, 2F, 12F, 27F), PartPose.ZERO);
        all.addOrReplaceChild("lame_b", CubeListBuilder.create().texOffs(194, 61).addBox(-31F, -56F, -21F, 2F, 12F, 24F), PartPose.ZERO);
        all.addOrReplaceChild("lame_b_r", CubeListBuilder.create().mirror(true).texOffs(194, 61).addBox(29F, -56F, -21F, 2F, 12F, 24F), PartPose.ZERO);
        all.addOrReplaceChild("lame_c", CubeListBuilder.create().texOffs(0, 102).addBox(-30.5F, -46F, -20F, 2F, 12F, 21F), PartPose.ZERO);
        all.addOrReplaceChild("lame_c_r", CubeListBuilder.create().mirror(true).texOffs(0, 102).addBox(28.5F, -46F, -20F, 2F, 12F, 21F), PartPose.ZERO);
        all.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(46, 102).addBox(-33.5F, -64F, -13F, 2F, 26F, 8F), PartPose.ZERO);
        all.addOrReplaceChild("banner_r", CubeListBuilder.create().mirror(true).texOffs(46, 102).addBox(31.5F, -64F, -13F, 2F, 26F, 8F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
}
