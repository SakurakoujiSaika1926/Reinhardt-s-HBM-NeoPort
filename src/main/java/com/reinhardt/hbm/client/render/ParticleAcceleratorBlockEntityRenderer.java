package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ParticleAcceleratorBlock;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ParticleAcceleratorBlockEntityRenderer implements LongRangeBlockEntityRenderer<ParticleAcceleratorBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation SOURCE = MachineModelRenderer.standalone("block/pa_source");
    private static final ModelResourceLocation BEAMLINE_BODY = MachineModelRenderer.standalone("block/pa_beamline_body");
    private static final ModelResourceLocation BEAMLINE_GLASS = MachineModelRenderer.standalone("block/pa_beamline_glass");
    private static final ModelResourceLocation BEAMLINE_WINDOW = MachineModelRenderer.standalone("block/pa_beamline_window");
    private static final ModelResourceLocation RFC = MachineModelRenderer.standalone("block/pa_rfc");
    private static final ModelResourceLocation QUADRUPOLE = MachineModelRenderer.standalone("block/pa_quadrupole");
    private static final ModelResourceLocation DIPOLE = MachineModelRenderer.standalone("block/pa_dipole");
    private static final ModelResourceLocation DETECTOR = MachineModelRenderer.standalone("block/pa_detector");
    private static final AABB SOURCE_NORTH_BOUNDS = createSourceLocalBounds(Direction.NORTH);
    private static final AABB SOURCE_SOUTH_BOUNDS = createSourceLocalBounds(Direction.SOUTH);
    private static final AABB SOURCE_WEST_BOUNDS = createSourceLocalBounds(Direction.WEST);
    private static final AABB SOURCE_EAST_BOUNDS = createSourceLocalBounds(Direction.EAST);
    private static final AABB BEAMLINE_NORTH_BOUNDS = createBeamlineLocalBounds(Direction.NORTH);
    private static final AABB BEAMLINE_SOUTH_BOUNDS = createBeamlineLocalBounds(Direction.SOUTH);
    private static final AABB BEAMLINE_WEST_BOUNDS = createBeamlineLocalBounds(Direction.WEST);
    private static final AABB BEAMLINE_EAST_BOUNDS = createBeamlineLocalBounds(Direction.EAST);
    private static final AABB RFC_NORTH_BOUNDS = createRfcLocalBounds(Direction.NORTH);
    private static final AABB RFC_SOUTH_BOUNDS = createRfcLocalBounds(Direction.SOUTH);
    private static final AABB RFC_WEST_BOUNDS = createRfcLocalBounds(Direction.WEST);
    private static final AABB RFC_EAST_BOUNDS = createRfcLocalBounds(Direction.EAST);
    private static final AABB QUADRUPOLE_NORTH_BOUNDS = createQuadrupoleLocalBounds(Direction.NORTH);
    private static final AABB QUADRUPOLE_SOUTH_BOUNDS = createQuadrupoleLocalBounds(Direction.SOUTH);
    private static final AABB QUADRUPOLE_WEST_BOUNDS = createQuadrupoleLocalBounds(Direction.WEST);
    private static final AABB QUADRUPOLE_EAST_BOUNDS = createQuadrupoleLocalBounds(Direction.EAST);
    private static final AABB DIPOLE_BOUNDS = createDipoleLocalBounds();
    private static final AABB DETECTOR_NORTH_BOUNDS = createDetectorLocalBounds(Direction.NORTH);
    private static final AABB DETECTOR_SOUTH_BOUNDS = createDetectorLocalBounds(Direction.SOUTH);
    private static final AABB DETECTOR_WEST_BOUNDS = createDetectorLocalBounds(Direction.WEST);
    private static final AABB DETECTOR_EAST_BOUNDS = createDetectorLocalBounds(Direction.EAST);

    public ParticleAcceleratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOURCE);
        event.register(BEAMLINE_BODY);
        event.register(BEAMLINE_GLASS);
        event.register(BEAMLINE_WINDOW);
        event.register(RFC);
        event.register(QUADRUPOLE);
        event.register(DIPOLE);
        event.register(DETECTOR);
    }

    @Override
    public void render(ParticleAcceleratorBlockEntity accelerator, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = accelerator.getBlockState();
        poseStack.pushPose();
        switch (accelerator.kind()) {
            case SOURCE -> {
                applySourcePose(poseStack, sourceFacing(state));
                STATIC_MESHES.render(SOURCE, poseStack, state, packedLight, packedOverlay);
            }
            case BEAMLINE -> {
                applyBeamlinePose(poseStack, beamlineFacing(state));
                if (accelerator.hasBeamlineWindow()) {
                    STATIC_MESHES.render(BEAMLINE_WINDOW, poseStack, state, packedLight, packedOverlay);
                    int alpha = Math.min(255, Math.max(48, Math.round(accelerator.beamlineLight(partialTick) * 120.0F)));
                    MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(BEAMLINE_GLASS), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, (alpha << 24) | 0xE6E6FF);
                } else {
                    STATIC_MESHES.render(BEAMLINE_BODY, poseStack, state, packedLight, packedOverlay);
                }
            }
            case RFC -> {
                applyRfcPose(poseStack, rfcFacing(state));
                STATIC_MESHES.render(RFC, poseStack, state, packedLight, packedOverlay);
            }
            case QUADRUPOLE -> {
                applyQuadrupolePose(poseStack, quadrupoleFacing(state));
                STATIC_MESHES.render(QUADRUPOLE, poseStack, state, packedLight, packedOverlay);
            }
            case DIPOLE -> {
                applyDipolePose(poseStack);
                STATIC_MESHES.render(DIPOLE, poseStack, state, packedLight, packedOverlay);
            }
            case DETECTOR -> {
                applyDetectorPose(poseStack, detectorFacing(state));
                STATIC_MESHES.render(DETECTOR, poseStack, state, packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ParticleAcceleratorBlockEntity accelerator) {
        BlockState state = accelerator.getBlockState();
        return switch (accelerator.kind()) {
            case SOURCE -> sourceRenderBounds(accelerator.getBlockPos(), sourceFacing(state));
            case BEAMLINE -> beamlineRenderBounds(accelerator.getBlockPos(), beamlineFacing(state));
            case RFC -> rfcRenderBounds(accelerator.getBlockPos(), rfcFacing(state));
            case QUADRUPOLE -> quadrupoleRenderBounds(accelerator.getBlockPos(), quadrupoleFacing(state));
            case DIPOLE -> DIPOLE_BOUNDS.move(accelerator.getBlockPos());
            case DETECTOR -> detectorRenderBounds(accelerator.getBlockPos(), detectorFacing(state));
        };
    }

    private static Direction sourceFacing(BlockState state) {
        if (!state.hasProperty(ParticleAcceleratorBlock.FACING)) {
            throw new IllegalStateException("PA source state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(ParticleAcceleratorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PA source state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction beamlineFacing(BlockState state) {
        if (!state.hasProperty(ParticleAcceleratorBlock.FACING)) {
            throw new IllegalStateException("PA beamline state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(ParticleAcceleratorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PA beamline state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rfcFacing(BlockState state) {
        if (!state.hasProperty(ParticleAcceleratorBlock.FACING)) {
            throw new IllegalStateException("PA RFC state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(ParticleAcceleratorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PA RFC state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction quadrupoleFacing(BlockState state) {
        if (!state.hasProperty(ParticleAcceleratorBlock.FACING)) {
            throw new IllegalStateException("PA quadrupole state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(ParticleAcceleratorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PA quadrupole state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction detectorFacing(BlockState state) {
        if (!state.hasProperty(ParticleAcceleratorBlock.FACING)) {
            throw new IllegalStateException("PA detector state is missing its dedicated facing property");
        }
        Direction facing = state.getValue(ParticleAcceleratorBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PA detector state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void applySourcePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, -1.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("PA source facing must be horizontal: " + facing);
        }
    }

    private static void applyBeamlinePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("PA beamline facing must be horizontal: " + facing);
        }
    }

    private static void applyRfcPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, -1.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("PA RFC facing must be horizontal: " + facing);
        }
    }

    private static void applyQuadrupolePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, -1.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("PA quadrupole facing must be horizontal: " + facing);
        }
    }

    private static void applyDipolePose(PoseStack poseStack) {
        // RenderPADipole has no metadata rotation.
        poseStack.translate(0.5F, -1.0F, 0.5F);
    }

    private static void applyDetectorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, -2.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("PA detector facing must be horizontal: " + facing);
        }
    }

    static AABB sourceRenderBounds(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> SOURCE_NORTH_BOUNDS.move(pos);
            case SOUTH -> SOURCE_SOUTH_BOUNDS.move(pos);
            case WEST -> SOURCE_WEST_BOUNDS.move(pos);
            case EAST -> SOURCE_EAST_BOUNDS.move(pos);
            case UP, DOWN -> throw new IllegalArgumentException("PA source facing must be horizontal: " + facing);
        };
    }

    static AABB beamlineRenderBounds(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> BEAMLINE_NORTH_BOUNDS.move(pos);
            case SOUTH -> BEAMLINE_SOUTH_BOUNDS.move(pos);
            case WEST -> BEAMLINE_WEST_BOUNDS.move(pos);
            case EAST -> BEAMLINE_EAST_BOUNDS.move(pos);
            case UP, DOWN -> throw new IllegalArgumentException("PA beamline facing must be horizontal: " + facing);
        };
    }

    static AABB rfcRenderBounds(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> RFC_NORTH_BOUNDS.move(pos);
            case SOUTH -> RFC_SOUTH_BOUNDS.move(pos);
            case WEST -> RFC_WEST_BOUNDS.move(pos);
            case EAST -> RFC_EAST_BOUNDS.move(pos);
            case UP, DOWN -> throw new IllegalArgumentException("PA RFC facing must be horizontal: " + facing);
        };
    }

    static AABB quadrupoleRenderBounds(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> QUADRUPOLE_NORTH_BOUNDS.move(pos);
            case SOUTH -> QUADRUPOLE_SOUTH_BOUNDS.move(pos);
            case WEST -> QUADRUPOLE_WEST_BOUNDS.move(pos);
            case EAST -> QUADRUPOLE_EAST_BOUNDS.move(pos);
            case UP, DOWN -> throw new IllegalArgumentException("PA quadrupole facing must be horizontal: " + facing);
        };
    }

    static AABB detectorRenderBounds(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> DETECTOR_NORTH_BOUNDS.move(pos);
            case SOUTH -> DETECTOR_SOUTH_BOUNDS.move(pos);
            case WEST -> DETECTOR_WEST_BOUNDS.move(pos);
            case EAST -> DETECTOR_EAST_BOUNDS.move(pos);
            case UP, DOWN -> throw new IllegalArgumentException("PA detector facing must be horizontal: " + facing);
        };
    }

    private static AABB createSourceLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySourcePose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -4.5F,
                1.5F, 3.0F, 4.5F);
    }

    private static AABB createBeamlineLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyBeamlinePose(poseStack, facing);
        // Body/window share this face-used extent; glass is strictly contained by it.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -0.5F, 0.0F, -1.5F,
                0.5F, 1.0F, 1.5F);
    }

    private static AABB createRfcLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyRfcPose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.0F, 0.0F, -4.5F,
                1.0F, 3.0F, 4.5F);
    }

    private static AABB createQuadrupoleLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyQuadrupolePose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F,
                1.5F, 3.0F, 1.5F);
    }

    private static AABB createDipoleLocalBounds() {
        PoseStack poseStack = new PoseStack();
        applyDipolePose(poseStack);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, -0.0F, -1.5F,
                1.5F, 3.0F, 1.5F);
    }

    private static AABB createDetectorLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyDetectorPose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -2.5F, 0.0F, -4.5F,
                2.5F, 5.0F, 4.5F);
    }
}
