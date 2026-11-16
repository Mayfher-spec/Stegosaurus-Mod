package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.quests.CronistaEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class CronistaRenderer extends MobRenderer<CronistaEntity, CronistaModel> {
    private static final ResourceLocation TEX = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/cronista.png");

    public CronistaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CronistaModel(ctx.bakeLayer(CronistaModel.LAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override public ResourceLocation getTextureLocation(CronistaEntity e) { return TEX; }

    @Override
    public void render(CronistaEntity e, float yaw, float partial, PoseStack stack, MultiBufferSource buffers, int light) {
        this.model.rightArmPose = HumanoidModel.ArmPose.ITEM;   // sostiene el libro de registro
        super.render(e, yaw, partial, stack, buffers, light);
    }
}
