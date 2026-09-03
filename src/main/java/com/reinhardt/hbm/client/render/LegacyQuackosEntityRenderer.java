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
