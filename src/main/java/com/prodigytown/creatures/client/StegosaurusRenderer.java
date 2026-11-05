package com.prodigytown.creatures.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Vanilla/Forge ModelPart renderer, same rendering family as ROR's T-Rex. */
public final class StegosaurusRenderer extends MobRenderer<StegosaurusEntity, StegosaurusModel<StegosaurusEntity>> {
    private static final ResourceLocation[] SKINS = {
        texture("cybernetic"), texture("golden"), texture("magma"), texture("glacial"), texture("natural")
    };
    /** Textura 512x512 toda blanca: da la silueta luminosa de la evolucion y el destello final. */
    public static final ResourceLocation WHITE = texture("white");
    private static ResourceLocation texture(String name) {
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/entity/stegosaurus_" + name + ".png");
    }
    public StegosaurusRenderer(EntityRendererProvider.Context context) {
        super(context, new StegosaurusModel<>(context.bakeLayer(StegosaurusModel.LAYER_LOCATION)), 3.0F);
        this.addLayer(new StegosaurusSaddleLayer(this, context.getModelSet()));
        this.addLayer(new StegosaurusArmorLayer(this, context.getModelSet()));
        this.addLayer(new StegosaurusGlowLayer(this));
        this.addLayer(new StegosaurusFlashLayer(this));
    }
    @Override public ResourceLocation getTextureLocation(StegosaurusEntity entity) {
        if (entity.getEvolveTicks() > 0) return WHITE;
        return SKINS[Math.max(0, Math.min(SKINS.length - 1, entity.getVariant()))];
    }

    /** Evolucion: silueta blanca, brillante, que crece de tamano de chibi (x0,25) al adulto con pulsos cada vez mas rapidos. */
    @Nullable @Override protected RenderType getRenderType(StegosaurusEntity entity, boolean visible, boolean translucent, boolean glowing) {
        if (entity.getEvolveTicks() > 0) return RenderType.eyes(WHITE);
        return super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override protected void scale(StegosaurusEntity entity, PoseStack stack, float partialTick) {
        int left = entity.getEvolveTicks();
        float s = 1.0F;
        if (left > 0) {
            float e = StegosaurusEntity.EVOLVE_LEN - left + partialTick;
            float t = Mth.clamp(e / 70.0F, 0.0F, 1.0F);
            float ease = t * t * (3.0F - 2.0F * t);
            s = 0.25F + 0.75F * ease;
            s *= 1.0F + 0.05F * (1.0F - 0.5F * t) * Mth.sin(e * (0.5F + 1.2F * t));
        }
        stack.scale(s, s, s);
        this.shadowRadius = 3.0F * s;
    }
}
