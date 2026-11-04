package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Auto-generated from the user supplied Blockbench Modded Entity model.
 * Same structure / UVs across cybernetic, magma, golden. No GeckoLib.
 * Flat dorsal plates are double-sided via entityCutoutNoCull rendering.
 */
public final class StegosaurusModel<T extends StegosaurusEntity> extends HierarchicalModel<T> {
  public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus"), "main");
  private final ModelPart root;
  public StegosaurusModel(ModelPart root) { this.root = root; }
  @Override public ModelPart root() { return root; }
  @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    root().getAllParts().forEach(ModelPart::resetPose);
    this.animate(entity.idleAnimationState, StegosaurusAnimations.IDLE, ageInTicks);
    if (!entity.isOrderedToSit()) {
      this.animateWalk(StegosaurusAnimations.WALK, limbSwing, limbSwingAmount, 1.0F, 1.0F);
    }
    this.animate(entity.tailWhipAnimationState, StegosaurusAnimations.TAIL_WHIP, ageInTicks);
    ModelPart head = this.root.getChild("all").getChild("body2").getChild("neck2").getChild("neck").getChild("head");
    head.yRot += netHeadYaw * ((float)Math.PI / 180.0F) * 0.35F;
    head.xRot += headPitch * ((float)Math.PI / 180.0F) * 0.35F;
  }
  @Override public void renderToBuffer(PoseStack stack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
    root.render(stack, buffer, light, overlay, red, green, blue, alpha);
  }
  public static LayerDefinition createBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition all = root.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
    PartDefinition body2 = all.addOrReplaceChild("body2", CubeListBuilder.create()
      .mirror(false).texOffs(305, 119).addBox(-27.5F, -21.375F, -25.325F, 55F, 46F, 23F)
      .mirror(false).texOffs(0, 119).addBox(-27.5F, -25.25F, -3.325F, 55F, 50F, 25F), PartPose.offset(0F, -46F, -25.3F));
    PartDefinition neck2 = body2.addOrReplaceChild("neck2", CubeListBuilder.create()
      .mirror(false).texOffs(0, 262).addBox(-16F, -16.125F, -14.8375F, 32F, 30F, 17F), PartPose.offset(0F, 0F, -19.55F));
    PartDefinition neck = neck2.addOrReplaceChild("neck", CubeListBuilder.create()
      .mirror(false).texOffs(186, 262).addBox(-11.5F, -15.975F, -14.85F, 23F, 25F, 17F), PartPose.offset(0F, 4.6F, -13.8F));
    PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
      .mirror(false).texOffs(156, 310).addBox(-9.5F, -9.1875F, -17.4875F, 19F, 17F, 20F)
      .mirror(false).texOffs(63, 351).addBox(-6F, -2F, -30.9875F, 12F, 10F, 15F), PartPose.offset(0F, 1.15F, -13.8F));
    PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
      .mirror(false).texOffs(418, 310).addBox(-7.5F, -1.005F, -26.9375F, 15F, 5F, 27F), PartPose.offset(0F, 7.13F, -2.3F));
    PartDefinition plate_c_l = neck.addOrReplaceChild("plate_c_l", CubeListBuilder.create()
      .mirror(false).texOffs(0, 195).addBox(0F, -31F, -17.4875F, 0F, 31F, 35F), PartPose.offsetAndRotation(0F, 1.15F, -13.8F, 0F, -0.5236F, 0F));
    PartDefinition plate_c_r = neck.addOrReplaceChild("plate_c_r", CubeListBuilder.create()
      .mirror(true).texOffs(0, 195).addBox(0F, -31F, -17.4875F, 0F, 31F, 35F), PartPose.offsetAndRotation(0F, 1.15F, -13.8F, 0F, 0.5236F, 0F));
    PartDefinition plate_p4_l = body2.addOrReplaceChild("plate_p4_l", CubeListBuilder.create()
      .mirror(false).texOffs(235, 310).addBox(0F, -25.8875F, -5.4875F, 0F, 26F, 11F), PartPose.offsetAndRotation(-11.5F, -13.8F, -27.025F, 0F, -0.43633F, -0.19739F));
    PartDefinition plate_p4_r = body2.addOrReplaceChild("plate_p4_r", CubeListBuilder.create()
      .mirror(true).texOffs(235, 310).addBox(0F, -25.8875F, -5.4875F, 0F, 26F, 11F), PartPose.offsetAndRotation(11.5F, -13.8F, -27.025F, 0F, 0.43633F, 0.19739F));
    PartDefinition plate_p56_l = body2.addOrReplaceChild("plate_p56_l", CubeListBuilder.create()
      .mirror(false).texOffs(0, 0).addBox(0F, -56.1625F, -30.9875F, 0F, 56F, 62F), PartPose.offsetAndRotation(0F, -18.4F, 7.475F, 0F, -0.34907F, 0F));
    PartDefinition plate_p56_r = body2.addOrReplaceChild("plate_p56_r", CubeListBuilder.create()
      .mirror(true).texOffs(0, 0).addBox(0F, -56.1625F, -30.9875F, 0F, 56F, 62F), PartPose.offsetAndRotation(0F, -18.4F, 7.475F, 0F, 0.34907F, 0F));
    PartDefinition arm_l = body2.addOrReplaceChild("arm_l", CubeListBuilder.create()
      .mirror(false).texOffs(222, 119).addBox(-8.365F, -2.1875F, -10.3F, 20F, 49F, 21F), PartPose.offset(-25.76F, -11.5F, -6.9F));
    PartDefinition hand_l = arm_l.addOrReplaceChild("hand_l", CubeListBuilder.create()
      .mirror(false).texOffs(329, 310).addBox(-10.99F, 0.04F, -13.05F, 22F, 11F, 22F), PartPose.offset(0F, 46.46F, 0F));
    PartDefinition finger_l = hand_l.addOrReplaceChild("finger_l", CubeListBuilder.create()
      .mirror(false).texOffs(193, 351).addBox(-3.4925F, -3.7175F, -7.92F, 7F, 7F, 9F), PartPose.offset(6.44F, 7.82F, -12.88F));
    PartDefinition finger_l2 = hand_l.addOrReplaceChild("finger_l2", CubeListBuilder.create()
      .mirror(false).texOffs(226, 351).addBox(-3.49F, -3.7175F, -7.92F, 7F, 7F, 9F), PartPose.offset(0F, 7.82F, -12.88F));
    PartDefinition finger_l3 = hand_l.addOrReplaceChild("finger_l3", CubeListBuilder.create()
      .mirror(false).texOffs(259, 351).addBox(-3.4875F, -3.7175F, -7.92F, 7F, 7F, 9F), PartPose.offset(-6.44F, 7.82F, -12.88F));
    PartDefinition arm_r = body2.addOrReplaceChild("arm_r", CubeListBuilder.create()
      .mirror(true).texOffs(222, 119).addBox(-11.635F, -2.1875F, -10.3F, 20F, 49F, 21F), PartPose.offset(25.76F, -11.5F, -6.9F));
    PartDefinition hand_r = arm_r.addOrReplaceChild("hand_r", CubeListBuilder.create()
      .mirror(true).texOffs(329, 310).addBox(-11.01F, 0.04F, -13.05F, 22F, 11F, 22F), PartPose.offset(0F, 46.46F, 0F));
    PartDefinition finger_r = hand_r.addOrReplaceChild("finger_r", CubeListBuilder.create()
      .mirror(true).texOffs(193, 351).addBox(-3.5075F, -3.7175F, -7.92F, 7F, 7F, 9F), PartPose.offset(-6.44F, 7.82F, -12.88F));
    PartDefinition finger_r2 = hand_r.addOrReplaceChild("finger_r2", CubeListBuilder.create()
      .mirror(true).texOffs(226, 351).addBox(-3.51F, -3.7175F, -7.92F, 7F, 7F, 9F), PartPose.offset(0F, 7.82F, -12.88F));
    PartDefinition finger_r3 = hand_r.addOrReplaceChild("finger_r3", CubeListBuilder.create()
      .mirror(true).texOffs(259, 351).addBox(-3.5125F, -3.7175F, -7.92F, 7F, 7F, 9F), PartPose.offset(6.44F, 7.82F, -12.88F));
    PartDefinition body = all.addOrReplaceChild("body", CubeListBuilder.create()
      .mirror(false).texOffs(210, 0).addBox(-27.5F, -25.25F, -24.1F, 55F, 50F, 31F)
      .mirror(false).texOffs(174, 195).addBox(-27.5F, -22.25F, 5.525F, 55F, 42F, 20F), PartPose.offset(0F, -46F, 16.1F));
    PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
      .mirror(false).texOffs(325, 195).addBox(-14.5F, -17.475F, -0.125F, 29F, 34F, 24F), PartPose.offsetAndRotation(0F, -1.15F, 24.15F, -0.10472F, 0F, 0F));
    PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create()
      .mirror(false).texOffs(99, 262).addBox(-10F, -10.7408F, -0.014F, 20F, 21F, 23F), PartPose.offsetAndRotation(0F, 4.5783F, 22.4515F, 0.10472F, 0F, 0F));
    PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
      .mirror(false).texOffs(431, 262).addBox(-7.5F, -8.2658F, 0.011F, 15F, 17F, 23F), PartPose.offset(0F, 1.15F, 21.85F));
    PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create()
      .mirror(false).texOffs(0, 310).addBox(-5F, -6.3658F, -0.214F, 10F, 12F, 28F), PartPose.offset(0F, -1.15F, 21.85F));
    PartDefinition spike_D = tail4.addOrReplaceChild("spike_D", CubeListBuilder.create()
      .mirror(false).texOffs(77, 310).addBox(-1.5F, -1.4783F, 0.136F, 3F, 3F, 36F), PartPose.offsetAndRotation(0F, -3.45F, 1.15F, 0.54585F, 0F, 0F));
    PartDefinition spike_C_l = tail3.addOrReplaceChild("spike_C_l", CubeListBuilder.create()
      .mirror(false).texOffs(267, 262).addBox(-2.485F, -2.4733F, 0.0235F, 5F, 5F, 37F), PartPose.offsetAndRotation(-4.14F, -5.98F, 13.8F, 0.71833F, -0.37132F, 0F));
    PartDefinition spike_C_r = tail3.addOrReplaceChild("spike_C_r", CubeListBuilder.create()
      .mirror(true).texOffs(267, 262).addBox(-2.515F, -2.4733F, 0.0235F, 5F, 5F, 37F), PartPose.offsetAndRotation(4.14F, -5.98F, 13.8F, 0.71833F, 0.37132F, 0F));
    PartDefinition spike_A_l = tail2.addOrReplaceChild("spike_A_l", CubeListBuilder.create()
      .mirror(false).texOffs(0, 351).addBox(-2.0125F, -1.9958F, -0.2515F, 4F, 4F, 27F), PartPose.offsetAndRotation(-2.3F, -4.37F, 19.55F, 1.0153F, -0.16515F, 0F));
    PartDefinition spike_A_r = tail2.addOrReplaceChild("spike_A_r", CubeListBuilder.create()
      .mirror(true).texOffs(0, 351).addBox(-1.9875F, -1.9958F, -0.2515F, 4F, 4F, 27F), PartPose.offsetAndRotation(2.3F, -4.37F, 19.55F, 1.0153F, 0.16515F, 0F));
    PartDefinition spike_B_l = tail2.addOrReplaceChild("spike_B_l", CubeListBuilder.create()
      .mirror(false).texOffs(258, 310).addBox(-2.03F, -1.9958F, -0.0415F, 4F, 4F, 31F), PartPose.offsetAndRotation(-3.22F, -4.37F, 24.84F, 0.93276F, -0.2783F, 0F));
    PartDefinition spike_B_r = tail2.addOrReplaceChild("spike_B_r", CubeListBuilder.create()
      .mirror(true).texOffs(258, 310).addBox(-1.97F, -1.9958F, -0.0415F, 4F, 4F, 31F), PartPose.offsetAndRotation(3.22F, -4.37F, 24.84F, 0.93276F, 0.2783F, 0F));
    PartDefinition plate_p810_l = tail.addOrReplaceChild("plate_p810_l", CubeListBuilder.create()
      .mirror(false).texOffs(125, 0).addBox(0F, -47.2392F, -20.9867F, 0F, 47F, 42F), PartPose.offsetAndRotation(-5.75F, -1.9233F, 18.2992F, 0.11152F, -0.36935F, -0.16049F));
    PartDefinition plate_p810_r = tail.addOrReplaceChild("plate_p810_r", CubeListBuilder.create()
      .mirror(true).texOffs(125, 0).addBox(0F, -47.2392F, -20.9867F, 0F, 47F, 42F), PartPose.offsetAndRotation(5.75F, -1.9233F, 18.2992F, 0.11152F, 0.36935F, 0.16049F));
    PartDefinition plate_p7_l = body.addOrReplaceChild("plate_p7_l", CubeListBuilder.create()
      .mirror(false).texOffs(161, 119).addBox(-0.025F, -45.025F, -14.9875F, 0F, 45F, 30F), PartPose.offsetAndRotation(-4.6F, -21.85F, 9.2F, 0F, -0.43633F, -0.46365F));
    PartDefinition plate_p7_r = body.addOrReplaceChild("plate_p7_r", CubeListBuilder.create()
      .mirror(true).texOffs(161, 119).addBox(0.025F, -45.025F, -14.9875F, 0F, 45F, 30F), PartPose.offsetAndRotation(4.6F, -21.85F, 9.2F, 0F, 0.43633F, 0.46365F));
    PartDefinition leg_l = all.addOrReplaceChild("leg_l", CubeListBuilder.create()
      .mirror(false).texOffs(71, 195).addBox(-8.9475F, -3.25F, -19.5F, 20F, 33F, 31F), PartPose.offset(-25.99F, -57.5F, 28.75F));
    PartDefinition leg_l2 = leg_l.addOrReplaceChild("leg_l2", CubeListBuilder.create()
      .mirror(false).texOffs(352, 262).addBox(-10.26F, -0.8275F, -9.3625F, 20F, 23F, 19F), PartPose.offset(0F, 27.14F, 2.3F));
    PartDefinition foot_l = leg_l2.addOrReplaceChild("foot_l", CubeListBuilder.create()
      .mirror(false).texOffs(118, 351).addBox(-11.135F, -0.1525F, -4.525F, 22F, 10F, 15F), PartPose.offset(0F, 20.7F, -1.15F));
    PartDefinition toe_l = foot_l.addOrReplaceChild("toe_l", CubeListBuilder.create()
      .mirror(false).texOffs(292, 351).addBox(-3.4925F, -3.7175F, -7.2375F, 7F, 7F, 8F), PartPose.offset(6.67F, 6.44F, -4.6F));
    PartDefinition toe_l2 = foot_l.addOrReplaceChild("toe_l2", CubeListBuilder.create()
      .mirror(false).texOffs(323, 351).addBox(-3.51F, -3.7175F, -7.2375F, 7F, 7F, 8F), PartPose.offset(0F, 6.44F, -4.6F));
    PartDefinition toe_l3 = foot_l.addOrReplaceChild("toe_l3", CubeListBuilder.create()
      .mirror(false).texOffs(354, 351).addBox(-3.5275F, -3.7175F, -7.2375F, 7F, 7F, 8F), PartPose.offset(-6.67F, 6.44F, -4.6F));
    PartDefinition leg_r = all.addOrReplaceChild("leg_r", CubeListBuilder.create()
      .mirror(true).texOffs(71, 195).addBox(-11.0525F, -3.25F, -19.5F, 20F, 33F, 31F), PartPose.offset(25.99F, -57.5F, 28.75F));
    PartDefinition leg_r2 = leg_r.addOrReplaceChild("leg_r2", CubeListBuilder.create()
      .mirror(true).texOffs(352, 262).addBox(-9.74F, -0.8275F, -9.3625F, 20F, 23F, 19F), PartPose.offset(0F, 27.14F, 2.3F));
    PartDefinition foot_r = leg_r2.addOrReplaceChild("foot_r", CubeListBuilder.create()
      .mirror(true).texOffs(118, 351).addBox(-10.865F, -0.1525F, -4.525F, 22F, 10F, 15F), PartPose.offset(0F, 20.7F, -1.15F));
    PartDefinition toe_r = foot_r.addOrReplaceChild("toe_r", CubeListBuilder.create()
      .mirror(true).texOffs(292, 351).addBox(-3.5075F, -3.7175F, -7.2375F, 7F, 7F, 8F), PartPose.offset(-6.67F, 6.44F, -4.6F));
    PartDefinition toe_r2 = foot_r.addOrReplaceChild("toe_r2", CubeListBuilder.create()
      .mirror(true).texOffs(323, 351).addBox(-3.49F, -3.7175F, -7.2375F, 7F, 7F, 8F), PartPose.offset(0F, 6.44F, -4.6F));
    PartDefinition toe_r3 = foot_r.addOrReplaceChild("toe_r3", CubeListBuilder.create()
      .mirror(true).texOffs(354, 351).addBox(-3.4725F, -3.7175F, -7.2375F, 7F, 7F, 8F), PartPose.offset(6.67F, 6.44F, -4.6F));
    return LayerDefinition.create(mesh, 512, 512);
  }
}
