package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item renderer for the old BAT models.
 *
 * Each tank retains its exact 1.7.10 item transform and never delegates its
 * pose to another machine.
 */
public final class LegacyTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public enum Kind {
        BAT9000,
        BIG_ASS_TANK
    }

    private static final ModelResourceLocation BAT9000_MODEL =
            MachineModelRenderer.standalone("block/machine_bat9000");
    private static final ModelResourceLocation BIG_ASS_TANK_MODEL =
            MachineModelRenderer.standalone("block/machine_bigasstank");

    private final Kind kind;

    public LegacyTankItemRenderer(Kind kind) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.kind = kind;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (this.kind) {
            case BAT9000 -> renderBat9000Item(context, poseStack, bufferSource, packedLight, packedOverlay);
            case BIG_ASS_TANK -> renderBigAssTankItem(context, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderBat9000Item(ItemDisplayContext context, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_BAT9000.get().defaultBlockState();
        poseStack.pushPose();
        applyBat9000ItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BAT9000_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyBat9000ItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.0F, 2.0F, 2.0F);
            return;
        }
        poseStack.translate(0.5F, 0.25F, 0.0F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderBigAssTankItem(ItemDisplayContext context, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_BIGASSTANK.get().defaultBlockState();
        poseStack.pushPose();
        applyBigAssTankItemPose(context, poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BIG_ASS_TANK_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyBigAssTankItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            return;
        }
        poseStack.translate(0.5F, 0.25F, 0.0F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }
}
