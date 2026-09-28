package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.block.FilingCabinetBlock;
import com.reinhardt.hbm.blockentity.FilingCabinetBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class FilingCabinetBlockEntityRenderer implements LongRangeBlockEntityRenderer<FilingCabinetBlockEntity> {
    private static final ModelResourceLocation GREEN_BASE = MachineModelRenderer.standalone("block/filing_cabinet_green_base");
    private static final ModelResourceLocation GREEN_LOWER = MachineModelRenderer.standalone("block/filing_cabinet_green_lower");
    private static final ModelResourceLocation GREEN_UPPER = MachineModelRenderer.standalone("block/filing_cabinet_green_upper");
    private static final ModelResourceLocation STEEL_BASE = MachineModelRenderer.standalone("block/filing_cabinet_steel_base");
    private static final ModelResourceLocation STEEL_LOWER = MachineModelRenderer.standalone("block/filing_cabinet_steel_lower");
    private static final ModelResourceLocation STEEL_UPPER = MachineModelRenderer.standalone("block/filing_cabinet_steel_upper");
    private static final AABB[] LOCAL_BOUNDS = buildFilingCabinetBounds();

    public FilingCabinetBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GREEN_BASE);
        event.register(GREEN_LOWER);
        event.register(GREEN_UPPER);
        event.register(STEEL_BASE);
        event.register(STEEL_LOWER);
        event.register(STEEL_UPPER);
    }

    @Override
    public void render(FilingCabinetBlockEntity cabinet, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = cabinet.getBlockState();
        boolean steel = state.getValue(FilingCabinetBlock.MATERIAL) == 1;
        ModelResourceLocation base = steel ? STEEL_BASE : GREEN_BASE;
        ModelResourceLocation lower = steel ? STEEL_LOWER : GREEN_LOWER;
        ModelResourceLocation upper = steel ? STEEL_UPPER : GREEN_UPPER;
        poseStack.pushPose();
        applyFilingCabinetTransform(poseStack, filingCabinetFacing(state));
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(base), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, .6875F * cabinet.lower(partialTick));
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(lower), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, .6875F * cabinet.upper(partialTick));
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(upper), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FilingCabinetBlockEntity cabinet) {
        Direction facing = filingCabinetFacing(cabinet.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(cabinet.getBlockPos());
    }

    private static AABB[] buildFilingCabinetBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyFilingCabinetTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.3125F, 0.0F, -0.5F, 0.3125F, 1.0F, 0.83125F);
        }
        return bounds;
    }

    private static void applyFilingCabinetTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderFileCabinet rotation indices 0/1/2/3 mapped by FilingCabinetBlock.
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Filing cabinet facing must be horizontal: " + facing);
        }
    }

    private static Direction filingCabinetFacing(BlockState state) {
        if (!state.hasProperty(FilingCabinetBlock.FACING)) {
            throw new IllegalStateException("Filing cabinet block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(FilingCabinetBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Filing cabinet block state has a vertical facing: " + facing);
        }
        return facing;
    }

}
