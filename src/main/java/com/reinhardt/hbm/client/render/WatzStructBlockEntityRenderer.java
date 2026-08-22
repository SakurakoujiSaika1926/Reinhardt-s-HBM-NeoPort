package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.WatzStructBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class WatzStructBlockEntityRenderer implements BlockEntityRenderer<WatzStructBlockEntity> {
    private static final double SMALL_BLOCK_SIZE = 5.0D / 16.0D;
    private static final double SMALL_BLOCK_OFFSET = (1.0D - SMALL_BLOCK_SIZE) * 0.5D;
    private static final int ALPHA_75 = 0xBFFFFFFF;

    public WatzStructBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WatzStructBlockEntity struct, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        draw(HbmBlocks.WATZ_COOLER.get(), 0, 1, 0, poseStack, bufferSource, packedOverlay);
        draw(HbmBlocks.WATZ_COOLER.get(), 0, 2, 0, poseStack, bufferSource, packedOverlay);

        for (int y = 0; y < 3; y++) {
            draw(HbmBlocks.WATZ_ELEMENT.get(), 1, y, 0, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 2, y, 0, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 0, y, 1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 0, y, 2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), -1, y, 0, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), -2, y, 0, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 0, y, -1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 0, y, -2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 1, y, 1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), 1, y, -1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), -1, y, 1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_ELEMENT.get(), -1, y, -1, poseStack, bufferSource, packedOverlay);

            draw(HbmBlocks.WATZ_COOLER.get(), 2, y, 1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), 2, y, -1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), 1, y, 2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), -1, y, 2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), -2, y, 1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), -2, y, -1, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), 1, y, -2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_COOLER.get(), -1, y, -2, poseStack, bufferSource, packedOverlay);

            for (int j = -1; j < 2; j++) {
                draw(HbmBlocks.WATZ_CASING.get(), 3, y, j, poseStack, bufferSource, packedOverlay);
                draw(HbmBlocks.WATZ_CASING.get(), j, y, 3, poseStack, bufferSource, packedOverlay);
                draw(HbmBlocks.WATZ_CASING.get(), -3, y, j, poseStack, bufferSource, packedOverlay);
                draw(HbmBlocks.WATZ_CASING.get(), j, y, -3, poseStack, bufferSource, packedOverlay);
            }
            draw(HbmBlocks.WATZ_CASING.get(), 2, y, 2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_CASING.get(), 2, y, -2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_CASING.get(), -2, y, 2, poseStack, bufferSource, packedOverlay);
            draw(HbmBlocks.WATZ_CASING.get(), -2, y, -2, poseStack, bufferSource, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(WatzStructBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX() - 3.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 3.0D,
                blockEntity.getBlockPos().getX() + 4.0D,
                blockEntity.getBlockPos().getY() + 3.0D,
                blockEntity.getBlockPos().getZ() + 4.0D
        );
    }

    private static void draw(Block block, int x, int y, int z, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        BlockState state = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        poseStack.pushPose();
        poseStack.translate(x + SMALL_BLOCK_OFFSET, y + SMALL_BLOCK_OFFSET, z + SMALL_BLOCK_OFFSET);
        poseStack.scale((float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE);
        MachineModelRenderer.renderUnculledTintedTranslucent(model, poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, ALPHA_75);
        poseStack.popPose();
    }
}
