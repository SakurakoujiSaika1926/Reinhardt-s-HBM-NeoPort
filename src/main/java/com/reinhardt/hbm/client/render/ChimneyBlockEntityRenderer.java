package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.ChimneyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ChimneyBlockEntityRenderer implements LongRangeBlockEntityRenderer<ChimneyBlockEntity> {
    private static final ModelResourceLocation BRICK = MachineModelRenderer.standalone("block/chimney_brick");
    private static final ModelResourceLocation INDUSTRIAL = MachineModelRenderer.standalone("block/chimney_industrial");
    private static final AABB BRICK_LOCAL_BOUNDS = RenderBounds.localFloatVertices(
            BlockPos.ZERO, -1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F
    );
    private static final AABB INDUSTRIAL_LOCAL_BOUNDS = RenderBounds.localFloatVertices(
            BlockPos.ZERO, -1.0F, 0.0F, -1.0F, 2.0F, 23.0F, 2.0F
    );

    public ChimneyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BRICK);
        event.register(INDUSTRIAL);
    }

    @Override
    public void render(ChimneyBlockEntity chimney, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = chimney.getBlockState();
        if (state.is(HbmBlocks.CHIMNEY_INDUSTRIAL.get())) {
            renderIndustrialChimney(state, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderBrickChimney(state, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(ChimneyBlockEntity blockEntity) {
        // Exact face-used OBJ extrema; the renderer applies no local transform.
        if (blockEntity.getBlockState().is(HbmBlocks.CHIMNEY_INDUSTRIAL.get())) {
            return industrialChimneyRenderBounds(blockEntity.getBlockPos());
        }
        return brickChimneyRenderBounds(blockEntity.getBlockPos());
    }

    private static void renderBrickChimney(BlockState state, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(BRICK), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderIndustrialChimney(BlockState state, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(INDUSTRIAL), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static AABB brickChimneyRenderBounds(BlockPos pos) {
        return BRICK_LOCAL_BOUNDS.move(pos);
    }

    private static AABB industrialChimneyRenderBounds(BlockPos pos) {
        return INDUSTRIAL_LOCAL_BOUNDS.move(pos);
    }
}
