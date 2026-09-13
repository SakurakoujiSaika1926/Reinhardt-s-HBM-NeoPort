package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyQuackosEntity;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The old Quackos renderer is a chicken model scaled by exactly 25. */
public final class LegacyQuackosEntityRenderer extends ChickenRenderer {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/duck.png");

    public LegacyQuackosEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // EntityQuackos#getShadowSize returned 7.5F in 1.7.10; the giant
        // chicken must retain that exact footprint instead of ChickenRenderer's
        // normal 0.3F shadow.
        shadowRadius = 7.5F;
    }

    @Override
    protected void scale(net.minecraft.world.entity.animal.Chicken entity,
                         PoseStack poseStack, float partialTick) {
        poseStack.scale(25.0F, 25.0F, 25.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Chicken entity) {
        return TEXTURE;
    }
}
