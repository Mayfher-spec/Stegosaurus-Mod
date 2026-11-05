package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

/** Huevo de estegosaurio hecho con 6 losas apiladas (silueta de huevo). Textura 64x64 generada por scratchpad/egg/genegg.py. */
public final class StegosaurusEggModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus_egg"), "main");
    public final ModelPart root;

    public StegosaurusEggModel(ModelPart root) { this.root = root; }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("egg", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 2.0F, 6.0F)
            .texOffs(24, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 2.0F, 8.0F)
            .texOffs(0, 10).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 4.0F, 10.0F)
            .texOffs(0, 24).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 2.0F, 8.0F)
            .texOffs(32, 24).addBox(-3.0F, -12.0F, -3.0F, 6.0F, 2.0F, 6.0F)
            .texOffs(0, 34).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 1.0F, 4.0F),
            PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
