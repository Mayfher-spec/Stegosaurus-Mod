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

/** From user-authored Blockbench geometry. Four original skins share identical native ModelParts. */
public final class StegosaurusModel<T extends StegosaurusEntity> extends HierarchicalModel<T> {
  public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ProdigyCreatures.MODID, "stegosaurus"), "main");
  private final ModelPart root;
  public StegosaurusModel(ModelPart root) { this.root = root; }
  @Override public ModelPart root() { return root; }
  @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    root.getAllParts().forEach(ModelPart::resetPose);
    final boolean sitting = entity.isOrderedToSit();
    final double hSpeed2 = entity.getDeltaMovement().horizontalDistanceSqr();
    final boolean moving = !sitting && (limbSwingAmount > 0.055F || hSpeed2 > 0.0005D);
    // Never overlay the full idle animation with a gait: shared bones cause stiffness
    // and dorsal-plate intersections. The user-authored WALK and RUN clips remain intact.
    if (!moving) this.animate(entity.idleAnimationState, StegosaurusAnimations.IDLE, ageInTicks);
    else {
      boolean sprint = hSpeed2 > 0.024D;
      // The authored 2-second walk used to feel glacial. Faster playback retains
      // weight in the gait without slowing physical movement.
      this.animateWalk(sprint ? StegosaurusAnimations.RUN : StegosaurusAnimations.WALK,
        limbSwing, limbSwingAmount, sprint ? 1.8F : 2.25F, 1.0F);
    }
    this.animate(entity.tailWhipAnimationState, StegosaurusAnimations.TAIL_WHIP, ageInTicks);
    this.animate(entity.biteAnimationState, StegosaurusAnimations.BITE, ageInTicks);
    this.animate(entity.roarAnimationState, StegosaurusAnimations.ROAR, ageInTicks);

    // Adapt the 11 chibi-style behaviors to this adult's native skeleton.
    // No GeckoLib, shared chibi bones, or independent dorsal rotations are introduced.
    ModelPart base = root.getChild("all");
    ModelPart foreBody = base.getChild("body2");
    ModelPart hindBody = base.getChild("body");
    ModelPart neckBase = foreBody.getChild("neck2");
    ModelPart neck = neckBase.getChild("neck");
    ModelPart head = neck.getChild("head");
    ModelPart jaw = head.getChild("jaw");
    ModelPart tail = hindBody.getChild("tail");
    ModelPart tail2 = tail.getChild("tail2");
    float motion = ageInTicks * 0.16F;
    if (moving) {
      // Walk/run: counterbalance the stride without sliding the root position.
      foreBody.xRot += 0.014F * net.minecraft.util.Mth.sin(motion * 1.8F);
      head.xRot += 0.022F * net.minecraft.util.Mth.sin(motion * 2.4F);
      tail.yRot += 0.065F * net.minecraft.util.Mth.sin(motion * 1.5F);
      tail2.yRot += 0.04F * net.minecraft.util.Mth.sin(motion * 1.5F + 0.4F);
    } else if (sitting) {
      // Sleep/rest: tuck the head, relax jaw and sway tail gently.
      neckBase.xRot += 0.10F;
      head.xRot += 0.14F;
      jaw.xRot += 0.025F;
      tail.yRot += 0.020F * net.minecraft.util.Mth.sin(ageInTicks * 0.045F);
    } else {
      float phase = ageInTicks % 640.0F;
      // Look around - a slow scan with a short pause.
      if (phase > 65.0F && phase < 135.0F) {
        head.yRot += 0.18F * net.minecraft.util.Mth.sin((phase - 65.0F) * 0.045F);
      }
      // Sniff: small synchronized head nod and jaw movements.
      if (phase > 195.0F && phase < 240.0F) {
        float v = net.minecraft.util.Mth.sin((phase - 195.0F) * 0.18F);
        head.xRot += 0.055F * v;
        jaw.xRot += 0.025F * Math.abs(v);
      }
      // Yawn: only jaw/neck move, not the dorsal plates.
      if (phase > 340.0F && phase < 391.0F) {
        float v = Math.max(0F, net.minecraft.util.Mth.sin((phase - 340.0F) * (float)Math.PI / 51.0F));
        jaw.xRot += 0.25F * v;
        head.xRot -= 0.045F * v;
      }
      // Happy: tamed stego greets with a subtle tail wag.
      if (entity.isTame() && phase > 485.0F && phase < 541.0F) {
        float v = net.minecraft.util.Mth.sin((phase - 485.0F) * 0.31F);
        tail.yRot += 0.12F * v;
        tail2.yRot += 0.08F * v;
      }
    }
    // Startle (on damage) and stomp (when facing a target) are short overlays.
    if (entity.hurtTime > 0) {
      head.xRot -= 0.09F;
      neck.xRot -= 0.03F;
      tail.yRot += 0.05F;
    }
    if (entity.getTarget() != null && !sitting && !moving) {
      foreBody.xRot += 0.015F * net.minecraft.util.Mth.sin(ageInTicks * 0.38F);
      foreBody.getChild("arm_r").xRot += 0.07F * net.minecraft.util.Mth.sin(ageInTicks * 0.48F);
    }
    // Existing TAIL_WHIP clip provides the chibi-style tail swipe in combat.
    head.yRot += netHeadYaw * ((float)Math.PI / 180.0F) * 0.35F;
    head.xRot += headPitch * ((float)Math.PI / 180.0F) * 0.35F;
  }
  @Override public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay,
    float red, float green, float blue, float alpha) {
    root.render(pose, consumer, light, overlay, red, green, blue, alpha);
  }
  public static LayerDefinition createBodyLayer() {
    MeshDefinition mesh = new MeshDefinition();
    PartDefinition root = mesh.getRoot();
    PartDefinition part_0 = root.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
    PartDefinition part_1 = part_0.addOrReplaceChild("body2", CubeListBuilder.create()
      .mirror(false).texOffs(305, 119).addBox(-27.5F, -21.375F, -25.325F, 55F, 46F, 23F)
      .mirror(false).texOffs(0, 119).addBox(-27.5F, -25.25F, -3.325F, 55F, 50F, 25F), PartPose.offset(0F, -46F, -25.3F));
    PartDefinition part_2 = part_1.addOrReplaceChild("neck2", CubeListBuilder.create()
      .mirror(false).texOffs(0, 262).addBox(-16F, -16.125F, -14.8375F, 32F, 30F, 17F), PartPose.offset(0F, 0F, -19.55F));
    PartDefinition part_3 = part_2.addOrReplaceChild("neck", CubeListBuilder.create()
      .mirror(false).texOffs(186, 262).addBox(-11.5F, -15.975F, -14.85F, 23F, 25F, 17F), PartPose.offset(0F, 4.6F, -13.8F));
    PartDefinition part_4 = part_3.addOrReplaceChild("head", CubeListBuilder.create()
      .mirror(false).texOffs(4, 396).addBox(-8F, -6.25F, -13.55F, 16F, 13F, 16F), PartPose.offsetAndRotation(0F, 1.15F, -13.8F, 0.17453F, 0F, 0F));
    PartDefinition part_5 = part_4.addOrReplaceChild("snout", CubeListBuilder.create()
      .mirror(false).texOffs(72, 396).addBox(-5.5F, 0F, -19F, 11F, 9F, 19F), PartPose.offsetAndRotation(0F, -6.25F, -12.55F, 0.08727F, 0F, 0F));
    PartDefinition part_6 = part_4.addOrReplaceChild("jaw", CubeListBuilder.create()
      .mirror(false).texOffs(132, 396).addBox(-6F, -2.5F, -29F, 12F, 6F, 29F), PartPose.offset(0F, 5.25F, -2.55F));
    PartDefinition part_7 = part_3.addOrReplaceChild("plate_c_l", CubeListBuilder.create()
      .mirror(false).texOffs(0, 195).addBox(0F, -12F, -7F, 0F, 12F, 14F), PartPose.offsetAndRotation(-3F, -15F, -5.85F, 0F, -0.17453F, -0.2618F));
    PartDefinition part_8 = part_3.addOrReplaceChild("plate_c_r", CubeListBuilder.create()
      .mirror(false).texOffs(28, 195).addBox(0F, -12F, -7F, 0F, 12F, 14F), PartPose.offsetAndRotation(3F, -15F, -5.85F, 0F, 0.17453F, 0.2618F));
    PartDefinition part_9 = part_1.addOrReplaceChild("plate_p4_l", CubeListBuilder.create()
      .mirror(false).texOffs(156, 310).addBox(0F, -25.8875F, -5.4875F, 0F, 26F, 11F), PartPose.offsetAndRotation(-11.5F, -13.8F, -27.025F, 0F, -0.43633F, -0.19739F));
    PartDefinition part_10 = part_1.addOrReplaceChild("plate_p4_r", CubeListBuilder.create()
      .mirror(true).texOffs(156, 310).addBox(0F, -25.8875F, -5.4875F, 0F, 26F, 11F), PartPose.offsetAndRotation(11.5F, -13.8F, -27.025F, 0F, 0.43633F, 0.19739F));
    PartDefinition part_11 = part_1.addOrReplaceChild("plate_p56_l", CubeListBuilder.create()
      .mirror(false).texOffs(0, 0).addBox(0F, -56.1625F, -30.9875F, 0F, 56F, 62F), PartPose.offsetAndRotation(0F, -18.4F, 7.475F, 0F, -0.34907F, 0F));
    PartDefinition part_12 = part_1.addOrReplaceChild("plate_p56_r", CubeListBuilder.create()
      .mirror(true).texOffs(0, 0).addBox(0F, -56.1625F, -30.9875F, 0F, 56F, 62F), PartPose.offsetAndRotation(0F, -18.4F, 7.475F, 0F, 0.34907F, 0F));
    PartDefinition part_13 = part_1.addOrReplaceChild("arm_l", CubeListBuilder.create()
      .mirror(false).texOffs(222, 119).addBox(-8.365F, -2.1875F, -10.3F, 20F, 49F, 21F), PartPose.offset(-25.76F, -11.5F, -6.9F));
    PartDefinition part_14 = part_13.addOrReplaceChild("hand_l", CubeListBuilder.create()
      .mirror(false).texOffs(250, 310).addBox(-10.99F, 0.04F, -13.05F, 22F, 11F, 22F), PartPose.offset(0F, 46.46F, 0F));
    PartDefinition part_15 = part_14.addOrReplaceChild("finger_l", CubeListBuilder.create()
      .mirror(false).texOffs(446, 351).addBox(-2.9925F, -3.01F, -5.9825F, 6F, 6F, 6F), PartPose.offsetAndRotation(6.44F, 8.05F, -12.88F, 0F, -0.17453F, 0F));
    PartDefinition part_16 = part_15.addOrReplaceChild("fclaw_l", CubeListBuilder.create()
      .mirror(false).texOffs(94, 377).addBox(-2.4925F, -0.61F, -5.9825F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_17 = part_14.addOrReplaceChild("finger_l2", CubeListBuilder.create()
      .mirror(false).texOffs(471, 351).addBox(-2.99F, -3.01F, -5.9825F, 6F, 6F, 6F), PartPose.offset(0F, 8.05F, -12.88F));
    PartDefinition part_18 = part_17.addOrReplaceChild("fclaw_l2", CubeListBuilder.create()
      .mirror(false).texOffs(117, 377).addBox(-2.49F, -0.61F, -5.9825F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_19 = part_14.addOrReplaceChild("finger_l3", CubeListBuilder.create()
      .mirror(false).texOffs(0, 377).addBox(-2.9875F, -3.01F, -5.9825F, 6F, 6F, 6F), PartPose.offsetAndRotation(-6.44F, 8.05F, -12.88F, 0F, 0.17453F, 0F));
    PartDefinition part_20 = part_19.addOrReplaceChild("fclaw_l3", CubeListBuilder.create()
      .mirror(false).texOffs(140, 377).addBox(-2.4875F, -0.61F, -5.9825F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_21 = part_1.addOrReplaceChild("arm_r", CubeListBuilder.create()
      .mirror(true).texOffs(222, 119).addBox(-11.635F, -2.1875F, -10.3F, 20F, 49F, 21F), PartPose.offset(25.76F, -11.5F, -6.9F));
    PartDefinition part_22 = part_21.addOrReplaceChild("hand_r", CubeListBuilder.create()
      .mirror(true).texOffs(250, 310).addBox(-11.01F, 0.04F, -13.05F, 22F, 11F, 22F), PartPose.offset(0F, 46.46F, 0F));
    PartDefinition part_23 = part_22.addOrReplaceChild("finger_r", CubeListBuilder.create()
      .mirror(true).texOffs(446, 351).addBox(-3.0075F, -3.01F, -5.9825F, 6F, 6F, 6F), PartPose.offsetAndRotation(-6.44F, 8.05F, -12.88F, 0F, 0.17453F, 0F));
    PartDefinition part_24 = part_23.addOrReplaceChild("fclaw_r", CubeListBuilder.create()
      .mirror(true).texOffs(94, 377).addBox(-2.5075F, -0.61F, -5.9825F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_25 = part_22.addOrReplaceChild("finger_r2", CubeListBuilder.create()
      .mirror(true).texOffs(471, 351).addBox(-3.01F, -3.01F, -5.9825F, 6F, 6F, 6F), PartPose.offset(0F, 8.05F, -12.88F));
    PartDefinition part_26 = part_25.addOrReplaceChild("fclaw_r2", CubeListBuilder.create()
      .mirror(true).texOffs(117, 377).addBox(-2.51F, -0.61F, -5.9825F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_27 = part_22.addOrReplaceChild("finger_r3", CubeListBuilder.create()
      .mirror(true).texOffs(0, 377).addBox(-3.0125F, -3.01F, -5.9825F, 6F, 6F, 6F), PartPose.offsetAndRotation(6.44F, 8.05F, -12.88F, 0F, -0.17453F, 0F));
    PartDefinition part_28 = part_27.addOrReplaceChild("fclaw_r3", CubeListBuilder.create()
      .mirror(true).texOffs(140, 377).addBox(-2.5125F, -0.61F, -5.9825F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_29 = part_0.addOrReplaceChild("body", CubeListBuilder.create()
      .mirror(false).texOffs(210, 0).addBox(-27.5F, -25.25F, -24.1F, 55F, 50F, 31F)
      .mirror(false).texOffs(174, 195).addBox(-27.5F, -22.25F, 5.525F, 55F, 42F, 20F), PartPose.offset(0F, -46F, 16.1F));
    PartDefinition part_30 = part_29.addOrReplaceChild("tail", CubeListBuilder.create()
      .mirror(false).texOffs(325, 195).addBox(-14.5F, -17.475F, -0.125F, 29F, 34F, 24F), PartPose.offsetAndRotation(0F, -1.15F, 24.15F, -0.10472F, 0F, 0F));
    PartDefinition part_31 = part_30.addOrReplaceChild("tail2", CubeListBuilder.create()
      .mirror(false).texOffs(99, 262).addBox(-10F, -10.7408F, -0.014F, 20F, 21F, 23F), PartPose.offsetAndRotation(0F, 4.5783F, 22.4515F, 0.10472F, 0F, 0F));
    PartDefinition part_32 = part_31.addOrReplaceChild("tail3", CubeListBuilder.create()
      .mirror(false).texOffs(431, 262).addBox(-7.5F, -8.2658F, 0.011F, 15F, 17F, 23F), PartPose.offset(0F, 1.15F, 21.85F));
    PartDefinition part_33 = part_32.addOrReplaceChild("tail4", CubeListBuilder.create()
      .mirror(false).texOffs(0, 310).addBox(-5F, -6.3658F, -0.214F, 10F, 12F, 28F), PartPose.offset(0F, -1.15F, 21.85F));
    PartDefinition part_34 = part_33.addOrReplaceChild("spike_D", CubeListBuilder.create()
      .mirror(false).texOffs(77, 310).addBox(-1.5F, -1.4783F, 0.136F, 3F, 3F, 36F), PartPose.offsetAndRotation(0F, -3.45F, 1.15F, 0.54585F, 0F, 0F));
    PartDefinition part_35 = part_32.addOrReplaceChild("spike_C_l", CubeListBuilder.create()
      .mirror(false).texOffs(267, 262).addBox(-2.485F, -2.4733F, 0.0235F, 5F, 5F, 37F), PartPose.offsetAndRotation(-4.14F, -5.98F, 13.8F, 0.71833F, -0.37132F, 0F));
    PartDefinition part_36 = part_32.addOrReplaceChild("spike_C_r", CubeListBuilder.create()
      .mirror(true).texOffs(267, 262).addBox(-2.515F, -2.4733F, 0.0235F, 5F, 5F, 37F), PartPose.offsetAndRotation(4.14F, -5.98F, 13.8F, 0.71833F, 0.37132F, 0F));
    PartDefinition part_37 = part_31.addOrReplaceChild("spike_A_l", CubeListBuilder.create()
      .mirror(false).texOffs(410, 310).addBox(-2.0125F, -1.9958F, -0.2515F, 4F, 4F, 27F), PartPose.offsetAndRotation(-2.3F, -4.37F, 19.55F, 1.0153F, -0.16515F, 0F));
    PartDefinition part_38 = part_31.addOrReplaceChild("spike_A_r", CubeListBuilder.create()
      .mirror(true).texOffs(410, 310).addBox(-1.9875F, -1.9958F, -0.2515F, 4F, 4F, 27F), PartPose.offsetAndRotation(2.3F, -4.37F, 19.55F, 1.0153F, 0.16515F, 0F));
    PartDefinition part_39 = part_31.addOrReplaceChild("spike_B_l", CubeListBuilder.create()
      .mirror(false).texOffs(179, 310).addBox(-2.03F, -1.9958F, -0.0415F, 4F, 4F, 31F), PartPose.offsetAndRotation(-3.22F, -4.37F, 24.84F, 0.93276F, -0.2783F, 0F));
    PartDefinition part_40 = part_31.addOrReplaceChild("spike_B_r", CubeListBuilder.create()
      .mirror(true).texOffs(179, 310).addBox(-1.97F, -1.9958F, -0.0415F, 4F, 4F, 31F), PartPose.offsetAndRotation(3.22F, -4.37F, 24.84F, 0.93276F, 0.2783F, 0F));
    PartDefinition part_41 = part_30.addOrReplaceChild("plate_p810_l", CubeListBuilder.create()
      .mirror(false).texOffs(125, 0).addBox(0F, -47.2392F, -20.9867F, 0F, 47F, 42F), PartPose.offsetAndRotation(-5.75F, -1.9233F, 18.2992F, 0.11152F, -0.36935F, -0.16049F));
    PartDefinition part_42 = part_30.addOrReplaceChild("plate_p810_r", CubeListBuilder.create()
      .mirror(true).texOffs(125, 0).addBox(0F, -47.2392F, -20.9867F, 0F, 47F, 42F), PartPose.offsetAndRotation(5.75F, -1.9233F, 18.2992F, 0.11152F, 0.36935F, 0.16049F));
    PartDefinition part_43 = part_29.addOrReplaceChild("plate_p7_l", CubeListBuilder.create()
      .mirror(false).texOffs(161, 119).addBox(-0.025F, -45.025F, -14.9875F, 0F, 45F, 30F), PartPose.offsetAndRotation(-4.6F, -21.85F, 9.2F, 0F, -0.43633F, -0.46365F));
    PartDefinition part_44 = part_29.addOrReplaceChild("plate_p7_r", CubeListBuilder.create()
      .mirror(true).texOffs(161, 119).addBox(0.025F, -45.025F, -14.9875F, 0F, 45F, 30F), PartPose.offsetAndRotation(4.6F, -21.85F, 9.2F, 0F, 0.43633F, 0.46365F));
    PartDefinition part_45 = part_0.addOrReplaceChild("leg_l", CubeListBuilder.create()
      .mirror(false).texOffs(71, 195).addBox(-8.9475F, -3.25F, -19.5F, 20F, 33F, 31F), PartPose.offset(-25.99F, -57.5F, 28.75F));
    PartDefinition part_46 = part_45.addOrReplaceChild("leg_l2", CubeListBuilder.create()
      .mirror(false).texOffs(352, 262).addBox(-10.26F, -0.8275F, -9.3625F, 20F, 23F, 19F), PartPose.offset(0F, 27.14F, 2.3F));
    PartDefinition part_47 = part_46.addOrReplaceChild("foot_l", CubeListBuilder.create()
      .mirror(false).texOffs(0, 351).addBox(-11.135F, -0.1525F, -4.525F, 22F, 10F, 15F), PartPose.offset(0F, 20.7F, -1.15F));
    PartDefinition part_48 = part_47.addOrReplaceChild("toe_l", CubeListBuilder.create()
      .mirror(false).texOffs(371, 351).addBox(-2.9925F, -3.01F, -5.9875F, 6F, 6F, 6F), PartPose.offsetAndRotation(6.67F, 6.67F, -4.6F, 0F, -0.17453F, 0F));
    PartDefinition part_49 = part_48.addOrReplaceChild("claw_l", CubeListBuilder.create()
      .mirror(false).texOffs(25, 377).addBox(-2.4925F, -0.61F, -5.9875F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_50 = part_47.addOrReplaceChild("toe_l2", CubeListBuilder.create()
      .mirror(false).texOffs(396, 351).addBox(-3.01F, -3.01F, -5.9875F, 6F, 6F, 6F), PartPose.offset(0F, 6.67F, -4.6F));
    PartDefinition part_51 = part_50.addOrReplaceChild("claw_l2", CubeListBuilder.create()
      .mirror(false).texOffs(48, 377).addBox(-2.51F, -0.61F, -5.9875F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_52 = part_47.addOrReplaceChild("toe_l3", CubeListBuilder.create()
      .mirror(false).texOffs(421, 351).addBox(-3.0275F, -3.01F, -5.9875F, 6F, 6F, 6F), PartPose.offsetAndRotation(-6.67F, 6.67F, -4.6F, 0F, 0.17453F, 0F));
    PartDefinition part_53 = part_52.addOrReplaceChild("claw_l3", CubeListBuilder.create()
      .mirror(false).texOffs(71, 377).addBox(-2.5275F, -0.61F, -5.9875F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_54 = part_0.addOrReplaceChild("leg_r", CubeListBuilder.create()
      .mirror(true).texOffs(71, 195).addBox(-11.0525F, -3.25F, -19.5F, 20F, 33F, 31F), PartPose.offset(25.99F, -57.5F, 28.75F));
    PartDefinition part_55 = part_54.addOrReplaceChild("leg_r2", CubeListBuilder.create()
      .mirror(true).texOffs(352, 262).addBox(-9.74F, -0.8275F, -9.3625F, 20F, 23F, 19F), PartPose.offset(0F, 27.14F, 2.3F));
    PartDefinition part_56 = part_55.addOrReplaceChild("foot_r", CubeListBuilder.create()
      .mirror(true).texOffs(0, 351).addBox(-10.865F, -0.1525F, -4.525F, 22F, 10F, 15F), PartPose.offset(0F, 20.7F, -1.15F));
    PartDefinition part_57 = part_56.addOrReplaceChild("toe_r", CubeListBuilder.create()
      .mirror(true).texOffs(371, 351).addBox(-3.0075F, -3.01F, -5.9875F, 6F, 6F, 6F), PartPose.offsetAndRotation(-6.67F, 6.67F, -4.6F, 0F, 0.17453F, 0F));
    PartDefinition part_58 = part_57.addOrReplaceChild("claw_r", CubeListBuilder.create()
      .mirror(true).texOffs(25, 377).addBox(-2.5075F, -0.61F, -5.9875F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_59 = part_56.addOrReplaceChild("toe_r2", CubeListBuilder.create()
      .mirror(true).texOffs(396, 351).addBox(-2.99F, -3.01F, -5.9875F, 6F, 6F, 6F), PartPose.offset(0F, 6.67F, -4.6F));
    PartDefinition part_60 = part_59.addOrReplaceChild("claw_r2", CubeListBuilder.create()
      .mirror(true).texOffs(48, 377).addBox(-2.49F, -0.61F, -5.9875F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    PartDefinition part_61 = part_56.addOrReplaceChild("toe_r3", CubeListBuilder.create()
      .mirror(true).texOffs(421, 351).addBox(-2.9725F, -3.01F, -5.9875F, 6F, 6F, 6F), PartPose.offsetAndRotation(6.67F, 6.67F, -4.6F, 0F, -0.17453F, 0F));
    PartDefinition part_62 = part_61.addOrReplaceChild("claw_r3", CubeListBuilder.create()
      .mirror(true).texOffs(71, 377).addBox(-2.4725F, -0.61F, -5.9875F, 5F, 4F, 6F), PartPose.offsetAndRotation(0F, -1.4F, -6F, 0.38397F, 0F, 0F));
    return LayerDefinition.create(mesh, 512, 512);
  }
}
