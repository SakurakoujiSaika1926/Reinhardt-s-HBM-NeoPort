package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.ParasiteMaggotEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 1.7.10 RenderMaggot used ModelSilverfish with a 0.3 shadow radius. */
public final class ParasiteMaggotEntityRenderer
        extends MobRenderer<ParasiteMaggotEntity, SilverfishModel<ParasiteMaggotEntity>> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/entity/parasite_maggot.png");

    public ParasiteMaggotEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteMaggotEntity entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(ParasiteMaggotEntity entity) {
        return 180.0F;
    }
}
