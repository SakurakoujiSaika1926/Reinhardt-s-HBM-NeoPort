package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.entity.ConveyorMovingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the carried stack exactly as the 1.7.10 moving-item cache did. */
public final class ConveyorMovingItemRenderer extends EntityRenderer<ConveyorMovingItem> {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    public ConveyorMovingItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ConveyorMovingItem item, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (item.getItemStack().isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean threeDimensional = minecraft.getItemRenderer()
                .getModel(item.getItemStack(), item.level(), null, item.getId()).isGui3d();
        poseStack.pushPose();
        poseStack.translate(0.0D, deterministicHeight(item.getId()), 0.0D);
        if (!threeDimensional) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.0D, -0.1875D, 0.0D);
            if (minecraft.options.graphicsMode().get().getId() == 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
        }
        minecraft.getItemRenderer().renderStatic(item.getItemStack(),
                threeDimensional ? ItemDisplayContext.GROUND : ItemDisplayContext.FIXED,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, item.level(), item.getId());
        poseStack.popPose();
        super.render(item, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ConveyorMovingItem entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    /** Exact first nextDouble() from RandomSource.create(entityId), without a per-frame object. */
    private static double deterministicHeight(int entityId) {
        long state = (entityId ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        state = (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
        long high = state >>> 22;
        state = (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
        return ((high << 27) + (state >>> 21)) * 0x1.0p-53D * 0.0625D;
    }
}
