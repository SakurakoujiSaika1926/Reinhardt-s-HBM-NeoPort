package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PileMk2DeviceBlock;
import com.reinhardt.hbm.blockentity.PileLoaderBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact current-upstream renderer for the Chicago Pile MK2 fuel loader. */
public final class PileLoaderBlockEntityRenderer implements LongRangeBlockEntityRenderer<PileLoaderBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/pile_loader_body");
    private static final ModelResourceLocation LEVER = MachineModelRenderer.standalone("block/pile_loader_lever");
    private static final ModelResourceLocation SLIDER = MachineModelRenderer.standalone("block/pile_loader_slider");
    private static final ModelResourceLocation ROD = MachineModelRenderer.standalone("block/pile_loader_rod");

    public PileLoaderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(LEVER);
        event.register(SLIDER);
        event.register(ROD);
    }

    @Override
    public void render(PileLoaderBlockEntity loader, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = loader.getBlockState();
        double position = loader.renderedLevel(partialTick);

        poseStack.pushPose();
        applyLoaderTransform(poseStack, loaderFacing(state));
        renderPart(BODY, poseStack, buffers, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(-0.1875F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (position * 90.0D)));
        poseStack.translate(0.1875F, -0.5F, 0.0F);
        renderPart(LEVER, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.translate((float) (position * -0.5D), 0.0F, 0.0F);
        renderPart(SLIDER, poseStack, buffers, state, packedLight, packedOverlay);
        if (!loader.loadingStack().isEmpty()) {
            renderPart(ROD, poseStack, buffers, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PileLoaderBlockEntity loader) {
        PoseStack poseStack = new PoseStack();
        applyLoaderTransform(poseStack, loaderFacing(loader.getBlockState()));
        // Union of the exact Loader mesh and the Lever's complete authored 0..90 degree travel.
        return RenderBounds.transformedBox(loader.getBlockPos(), poseStack,
                -0.5F, 0.125F, -0.40625F,
                0.53125F, 1.21875F, 0.375F);
    }

    private static void applyLoaderTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Pile loader facing must be horizontal: " + facing);
        }
    }

    private static Direction loaderFacing(BlockState state) {
        if (!state.hasProperty(PileMk2DeviceBlock.FACING)) {
            throw new IllegalStateException("Pile loader state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(PileMk2DeviceBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Pile loader state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource buffers, BlockState state,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model), poseStack, buffers, state, packedLight, packedOverlay);
    }
}
