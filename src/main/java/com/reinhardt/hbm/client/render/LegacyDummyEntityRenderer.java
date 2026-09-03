package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyDummyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dedicated target-dummy renderer using the old unarmored biped model. */
public final class LegacyDummyEntityRenderer
        extends MobRenderer<LegacyDummyEntity, HumanoidModel<LegacyDummyEntity>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/dummy.png");

    public LegacyDummyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDummyEntity entity) {
        return TEXTURE;
    }
}
