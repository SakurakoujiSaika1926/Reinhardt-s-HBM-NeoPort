package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.blockentity.FusionTorusStructBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FusionTorusStructBlockEntityRenderer implements BlockEntityRenderer<FusionTorusStructBlockEntity> {
    private static final double SMALL_BLOCK_SIZE = 5.0D / 16.0D;
    private static final double SMALL_BLOCK_OFFSET = (1.0D - SMALL_BLOCK_SIZE) * 0.5D;
    private static final int ALPHA_75 = 0xBFFFFFFF;

    public FusionTorusStructBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FusionTorusStructBlockEntity struct, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int y = 0; y < 5; y++) {
            int layerIndex = y > 2 ? 4 - y : y;
            int[][] layer = FusionMachineBlock.TORUS_LAYOUT[layerIndex];
            for (int x = 0; x < layer.length; x++) {
                for (int z = 0; z < layer[x].length; z++) {
                    int variant = layer[x][z];
                    if (variant == 0) {
                        continue;
                    }
                    drawFusionComponent(variant, x - 7, y, z - 7, poseStack, bufferSource, packedOverlay);
                }
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(FusionTorusStructBlockEntity struct) {
        BlockPos pos = struct.getBlockPos();
        return new AABB(
                pos.getX() - 7.0D,
                pos.getY(),
                pos.getZ() - 7.0D,
                pos.getX() + 8.0D,
                pos.getY() + 5.0D,
                pos.getZ() + 8.0D
        );
    }

    @Override
    public boolean shouldRenderOffScreen(FusionTorusStructBlockEntity struct) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    private static void drawFusionComponent(int variant, int x, int y, int z, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        BlockState state = HbmBlocks.FUSION_COMPONENT.get()
                .defaultBlockState()
                .setValue(LegacyVariantBlock.VARIANT, variant);
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        poseStack.pushPose();
        poseStack.translate(x + SMALL_BLOCK_OFFSET, y + SMALL_BLOCK_OFFSET, z + SMALL_BLOCK_OFFSET);
        poseStack.scale((float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE);
        MachineModelRenderer.renderUnculledTintedTranslucent(model, poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, ALPHA_75);
        poseStack.popPose();
    }
}
