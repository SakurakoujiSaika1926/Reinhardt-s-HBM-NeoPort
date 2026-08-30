package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.entity.GlyphidAcidSprayEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The old liquid chemical projectile was particle-only; no solid model is rendered. */
public final class GlyphidAcidSprayEntityRenderer extends EntityRenderer<GlyphidAcidSprayEntity> {
    public GlyphidAcidSprayEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphidAcidSprayEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
