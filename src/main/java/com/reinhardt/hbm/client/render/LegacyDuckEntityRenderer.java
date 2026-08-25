package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;

/** Vanilla chicken model with the legacy duck texture. */
public final class LegacyDuckEntityRenderer extends ChickenRenderer {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/duck.png");

    public LegacyDuckEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Chicken duck) {
        return TEXTURE;
    }
}
