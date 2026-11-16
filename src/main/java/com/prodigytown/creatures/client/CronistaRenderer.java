package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.quests.CronistaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class CronistaRenderer extends MobRenderer<CronistaEntity, CronistaModel> {
    private static final ResourceLocation TEX = new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/cronista.png");

    public CronistaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CronistaModel(ctx.bakeLayer(CronistaModel.LAYER)), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(CronistaEntity e) { return TEX; }

}
