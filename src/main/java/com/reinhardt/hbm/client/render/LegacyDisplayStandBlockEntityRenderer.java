package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LegacyDisplayStandBlock;
import com.reinhardt.hbm.blockentity.LegacyDisplayStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact world transforms from RenderPedestalTile and RenderSkeletonHolder. */
public final class LegacyDisplayStandBlockEntityRenderer
        implements BlockEntityRenderer<LegacyDisplayStandBlockEntity> {
    private static final ModelResourceLocation SKELETON_HOLDER =
            MachineModelRenderer.standalone("block/skeleton_holder_world");

    public LegacyDisplayStandBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SKELETON_HOLDER);
    }

    @Override
    public void render(LegacyDisplayStandBlockEntity stand, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = stand.getBlockState();
        if (!(state.getBlock() instanceof LegacyDisplayStandBlock block)) {
            return;
        }

        if (block.kind() == LegacyDisplayStandBlock.Kind.SKELETON_HOLDER) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(skeletonRotation(state.getValue(LegacyDisplayStandBlock.FACING))));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SKELETON_HOLDER), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            renderSkeletonItem(stand.displayedItem(), stand, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        } else {
            renderPedestalItem(stand.displayedItem(), stand, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
        }
    }

    private static void renderPedestalItem(ItemStack stack, LegacyDisplayStandBlockEntity stand, float partialTick,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean threeDimensional = minecraft.getItemRenderer()
                .getModel(stack, stand.getLevel(), null, 0).isGui3d();
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        float bob = (float) Math.sin((stand.getLevel().getGameTime() + partialTick) * 0.1F) * 0.0625F;
        poseStack.translate(0.0F, bob + (threeDimensional ? 0.0625F : 0.125F), 0.0F);
        if (!threeDimensional && minecraft.player != null) {
            poseStack.mulPose(Axis.YN.rotationDegrees(minecraft.player.getYRot() + 180.0F));
        }
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, stand.getLevel(), 0);
        poseStack.popPose();
    }

    private static void renderSkeletonItem(ItemStack stack, LegacyDisplayStandBlockEntity stand,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        if (stack.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean threeDimensional = minecraft.getItemRenderer()
                .getModel(stack, stand.getLevel(), null, 0).isGui3d();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        if (!threeDimensional) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        }
        poseStack.translate(0.0F, 0.125F, 0.0F);
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, stand.getLevel(), 0);
        poseStack.popPose();
    }

    private static float skeletonRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            default -> 180.0F;
        };
    }
}
