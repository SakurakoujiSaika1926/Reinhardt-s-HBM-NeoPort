package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyDummyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dedicated target-dummy renderer using the old unarmored biped model. */
public final class LegacyDummyEntityRenderer
        extends MobRenderer<LegacyDummyEntity, LegacyDummyModel> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/dummy.png");

    public LegacyDummyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyDummyModel(context.bakeLayer(LegacyDummyModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDummyEntity entity) {
        return TEXTURE;
    }
}
