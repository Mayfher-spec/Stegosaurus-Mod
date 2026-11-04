package com.prodigytown.creatures.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Authored in Blockbench; converted to vanilla 1.20.1 keyframe animations. */
public final class StegosaurusAnimations {
  private StegosaurusAnimations() {}
  public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(4F).looping()
    .addAnimation("body2", new AnimationChannel(AnimationChannel.Targets.POSITION,
      new Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(2F, KeyframeAnimations.posVec(0F, -0.5F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
      new Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(2.1F, KeyframeAnimations.posVec(0F, -0.3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("neck2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(2F, KeyframeAnimations.degreeVec(-1.5F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("neck", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(2F, KeyframeAnimations.degreeVec(1F, 2F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1F, KeyframeAnimations.degreeVec(-1F, 3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(3F, KeyframeAnimations.degreeVec(-1F, -3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("jaw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(2F, KeyframeAnimations.degreeVec(2.5F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1F, KeyframeAnimations.degreeVec(0F, 3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(3F, KeyframeAnimations.degreeVec(0F, -3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.3F, KeyframeAnimations.degreeVec(0F, 4F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(3.3F, KeyframeAnimations.degreeVec(0F, -4F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 5F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(3.6F, KeyframeAnimations.degreeVec(0F, -5F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.9F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(3.9F, KeyframeAnimations.degreeVec(0F, -6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .build();
  public static final AnimationDefinition WALK = AnimationDefinition.Builder.withLength(1.2F).looping()
    .addAnimation("leg_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(-20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(-20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("leg_r2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(35F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("foot_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(-20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(-35F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("leg_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(-20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("leg_l2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(35F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("foot_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(-20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(-35F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(-20F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("arm_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(-16F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(16F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(-16F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("hand_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(13F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(-13F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(13F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("arm_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(16F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(-16F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(16F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("hand_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(-13F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(13F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(-13F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("body2", new AnimationChannel(AnimationChannel.Targets.POSITION,
      new Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.posVec(0F, -0.9F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.posVec(0F, -0.9F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
      new Keyframe(0F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.posVec(0F, -0.9F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.posVec(0F, -0.9F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.posVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("neck2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(-2F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.3F, KeyframeAnimations.degreeVec(2F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(-2F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(2F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(-2F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 4F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(0F, -4F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 4F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.6F, KeyframeAnimations.degreeVec(0F, -6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.7F, KeyframeAnimations.degreeVec(0F, -6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.8F, KeyframeAnimations.degreeVec(0F, -6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, -6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.2F, KeyframeAnimations.degreeVec(0F, 6F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .build();
  public static final AnimationDefinition TAIL_WHIP = AnimationDefinition.Builder.withLength(1.6F)
    .addAnimation("body2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 5F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, -4F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, 3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, -3F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, -8F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.9F, KeyframeAnimations.degreeVec(0F, 5F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.5F, KeyframeAnimations.degreeVec(0F, -8F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.8F, KeyframeAnimations.degreeVec(0F, 16F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.1F, KeyframeAnimations.degreeVec(0F, -4.8F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.59F, KeyframeAnimations.degreeVec(0F, -10F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.89F, KeyframeAnimations.degreeVec(0F, 20F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.19F, KeyframeAnimations.degreeVec(0F, -6F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.68F, KeyframeAnimations.degreeVec(0F, -12F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.98F, KeyframeAnimations.degreeVec(0F, 24F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.28F, KeyframeAnimations.degreeVec(0F, -7.2F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .addAnimation("tail4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
      new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(0.77F, KeyframeAnimations.degreeVec(0F, -14F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.07F, KeyframeAnimations.degreeVec(0F, 28F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.37F, KeyframeAnimations.degreeVec(0F, -8.4F, 0F), AnimationChannel.Interpolations.CATMULLROM),
      new Keyframe(1.6F, KeyframeAnimations.degreeVec(0F, 0F, 0F), AnimationChannel.Interpolations.CATMULLROM)))
    .build();
}
