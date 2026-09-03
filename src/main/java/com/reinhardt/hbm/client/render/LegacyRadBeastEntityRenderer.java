package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyRadBeastEntity;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Radiation Beast uses the old Blaze body and its dedicated texture. */
public final class LegacyRadBeastEntityRenderer
        extends MobRenderer<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/radbeast.png");

    public LegacyRadBeastEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyRadBeastEntity entity) {
        return TEXTURE;
    }
}
