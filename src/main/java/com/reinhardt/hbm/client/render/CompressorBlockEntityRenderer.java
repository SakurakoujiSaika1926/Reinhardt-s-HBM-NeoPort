package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.CompressorBlock;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class CompressorBlockEntityRenderer implements LongRangeBlockEntityRenderer<CompressorBlockEntity> {
    private static final ModelResourceLocation NORMAL_BODY = MachineModelRenderer.standalone("block/machine_compressor_world");
    private static final ModelResourceLocation NORMAL_PUMP = MachineModelRenderer.standalone("block/machine_compressor_pump");
    private static final ModelResourceLocation NORMAL_FAN = MachineModelRenderer.standalone("block/machine_compressor_fan");
    private static final ModelResourceLocation COMPACT_BODY = MachineModelRenderer.standalone("block/machine_compressor_compact_world");
    private static final ModelResourceLocation COMPACT_FAN1 = MachineModelRenderer.standalone("block/machine_compressor_compact_fan1");
    private static final ModelResourceLocation COMPACT_FAN2 = MachineModelRenderer.standalone("block/machine_compressor_compact_fan2");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB NORMAL_NORTH_RENDER_BOUNDS = createNormalLocalBounds(Direction.NORTH);
    private static final AABB NORMAL_SOUTH_RENDER_BOUNDS = createNormalLocalBounds(Direction.SOUTH);
    private static final AABB NORMAL_WEST_RENDER_BOUNDS = createNormalLocalBounds(Direction.WEST);
    private static final AABB NORMAL_EAST_RENDER_BOUNDS = createNormalLocalBounds(Direction.EAST);
    private static final AABB COMPACT_NORTH_RENDER_BOUNDS = createCompactLocalBounds(Direction.NORTH);
    private static final AABB COMPACT_SOUTH_RENDER_BOUNDS = createCompactLocalBounds(Direction.SOUTH);
    private static final AABB COMPACT_WEST_RENDER_BOUNDS = createCompactLocalBounds(Direction.WEST);
    private static final AABB COMPACT_EAST_RENDER_BOUNDS = createCompactLocalBounds(Direction.EAST);

    public CompressorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(NORMAL_BODY);
        event.register(NORMAL_PUMP);
        event.register(NORMAL_FAN);
        event.register(COMPACT_BODY);
        event.register(COMPACT_FAN1);
        event.register(COMPACT_FAN2);
    }

    @Override
    public void render(CompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = compressor.getBlockState();
        Direction facing = compressorFacing(state);

        poseStack.pushPose();
        if (compressor.kind() == CompressorBlockEntity.Kind.COMPACT) {
            applyCompactCompressorPose(poseStack, facing);
            renderCompact(compressor, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else {
            applyNormalCompressorPose(poseStack, facing);
            renderNormal(compressor, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CompressorBlockEntity blockEntity) {
        if (blockEntity.kind() == CompressorBlockEntity.Kind.COMPACT) {
            return compactRenderBounds(blockEntity.getBlockPos(), compressorFacing(blockEntity.getBlockState()));
        }
        return normalRenderBounds(blockEntity.getBlockPos(), compressorFacing(blockEntity.getBlockState()));
    }

    /** Exact RenderCompressor metadata mapping. */
    private static void applyNormalCompressorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Normal compressor facing must be horizontal: " + facing);
        }
    }

    /** Exact RenderCompressorCompact metadata mapping. */
    private static void applyCompactCompressorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Compact compressor facing must be horizontal: " + facing);
        }
    }

    private static Direction compressorFacing(BlockState state) {
        if (!state.hasProperty(CompressorBlock.FACING)) {
            throw new IllegalStateException("Compressor state is missing its owned facing property: " + state);
        }
        Direction facing = state.getValue(CompressorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Compressor state has an invalid vertical facing: " + state);
        }
        return facing;
    }

    static AABB normalRenderBounds(BlockPos pos, Direction facing) {
        AABB localBounds = switch (facing) {
            case NORTH -> NORMAL_NORTH_RENDER_BOUNDS;
            case SOUTH -> NORMAL_SOUTH_RENDER_BOUNDS;
            case WEST -> NORMAL_WEST_RENDER_BOUNDS;
            case EAST -> NORMAL_EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Normal compressor facing must be horizontal: " + facing);
        };
        return localBounds.move(pos);
    }

    static AABB compactRenderBounds(BlockPos pos, Direction facing) {
        AABB localBounds = switch (facing) {
            case NORTH -> COMPACT_NORTH_RENDER_BOUNDS;
            case SOUTH -> COMPACT_SOUTH_RENDER_BOUNDS;
            case WEST -> COMPACT_WEST_RENDER_BOUNDS;
            case EAST -> COMPACT_EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Compact compressor facing must be horizontal: " + facing);
        };
        return localBounds.move(pos);
    }

    private static AABB createNormalLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyNormalCompressorPose(poseStack, facing);
        // Compressor's 478 face-used vertices dominate the pump's full 0..1 stroke and fan sweep.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F,
                2.5F, 8.5F, 1.5F);
    }

    private static AABB createCompactLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyCompactCompressorPose(poseStack, facing);
        // Condenser's 1,242 face-used vertices dominate both fans' full sweeps.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.625F, -0.125F, -3.5F,
                1.625F, 3.125F, 3.5F);
    }

    private static void renderNormal(CompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(NORMAL_BODY, poseStack, state, packedLight, packedOverlay);

        float lift = compressor.piston(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, lift * 3.0D - 3.0D, 0.0D);
        STATIC_MESHES.render(NORMAL_PUMP, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        float fan = compressor.fanSpin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(fan), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(NORMAL_FAN, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCompact(CompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(COMPACT_BODY, poseStack, state, packedLight, packedOverlay);

        float fan = compressor.fanSpin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(fan), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(COMPACT_FAN1, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(fan), -1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(COMPACT_FAN2, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
