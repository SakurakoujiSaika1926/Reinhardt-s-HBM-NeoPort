package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.SpotlightBlock;
import com.reinhardt.hbm.blockentity.SpotlightBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

/** Renders each original 1.7.10 spotlight kind with its own verified world pose. */
public final class SpotlightBlockEntityRenderer implements LongRangeBlockEntityRenderer<SpotlightBlockEntity> {
    private static final ModelResourceLocation INCANDESCENT_ON = model("spotlight_incandescent_world");
    private static final ModelResourceLocation INCANDESCENT_OFF = model("spotlight_incandescent_off_world");
    private static final ModelResourceLocation HALOGEN_ON = model("spotlight_halogen_world");
    private static final ModelResourceLocation HALOGEN_OFF = model("spotlight_halogen_off_world");
    private static final ModelResourceLocation FLUORO_SINGLE_ON = model("spotlight_fluoro_single_world");
    private static final ModelResourceLocation FLUORO_CAP_ON = model("spotlight_fluoro_cap_world");
    private static final ModelResourceLocation FLUORO_MID_ON = model("spotlight_fluoro_mid_world");
    private static final ModelResourceLocation FLUORO_SINGLE_OFF = model("spotlight_fluoro_single_off_world");
    private static final ModelResourceLocation FLUORO_CAP_OFF = model("spotlight_fluoro_cap_off_world");
    private static final ModelResourceLocation FLUORO_MID_OFF = model("spotlight_fluoro_mid_off_world");

    private static final List<ModelResourceLocation> MODELS = List.of(
            INCANDESCENT_ON, INCANDESCENT_OFF, HALOGEN_ON, HALOGEN_OFF,
            FLUORO_SINGLE_ON, FLUORO_CAP_ON, FLUORO_MID_ON,
            FLUORO_SINGLE_OFF, FLUORO_CAP_OFF, FLUORO_MID_OFF
    );

    public SpotlightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        SpotlightBlockEntity.setClientRenderSnapshotUpdater(
                SpotlightBlockEntityRenderer::updateRenderSnapshot);
    }

    static void invalidateRenderSnapshotsAfterBake() {
        SpotlightBlockEntity.setClientRenderSnapshotUpdater(
                SpotlightBlockEntityRenderer::updateRenderSnapshot);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MODELS.forEach(event::register);
    }

    @Override
    public void render(SpotlightBlockEntity spotlight, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        SpotlightBlockEntity.RenderSnapshot snapshot = spotlight.renderSnapshot();
        BlockState state = snapshot.blockState();
        if (!(state.getBlock() instanceof SpotlightBlock block)) {
            return;
        }
        Direction facing = state.getValue(SpotlightBlock.FACING);
        switch (block.kind()) {
            case INCANDESCENT -> renderIncandescent(snapshot, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
            case FLUORESCENT -> renderFluorescent(snapshot, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
            case HALOGEN -> renderHalogen(snapshot, facing, poseStack, bufferSource,
                    packedLight, packedOverlay);
        }
    }

    private static void renderIncandescent(SpotlightBlockEntity.RenderSnapshot snapshot, Direction facing,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyIncandescentPose(poseStack, facing);
        BakedBlockGeometrySnapshot.renderCutoutNoCull(
                snapshot.modelToken(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderFluorescent(SpotlightBlockEntity.RenderSnapshot snapshot, Direction facing,
                                          PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyFluorescentPose(poseStack, facing, snapshot.connectionDirection());
        BakedBlockGeometrySnapshot.renderCutoutNoCull(
                snapshot.modelToken(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHalogen(SpotlightBlockEntity.RenderSnapshot snapshot, Direction facing,
                                      PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyHalogenPose(poseStack, facing);
        BakedBlockGeometrySnapshot.renderCutoutNoCull(
                snapshot.modelToken(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SpotlightBlockEntity spotlight) {
        return spotlight.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(SpotlightBlockEntity spotlight) {
        if (spotlight.getLevel() == null) {
            return;
        }
        BlockState state = spotlight.getBlockState();
        if (!(state.getBlock() instanceof SpotlightBlock block)) {
            return;
        }
        Direction facing = state.getValue(SpotlightBlock.FACING);
        Direction connectionDirection = null;
        int connectionCount = 0;
        if (block.kind() == SpotlightBlock.Kind.FLUORESCENT) {
            for (Direction direction : Direction.values()) {
                if (direction == facing || direction == facing.getOpposite()) {
                    continue;
                }
                if (block.canConnectTo(spotlight.getLevel().getBlockState(
                        spotlight.getBlockPos().relative(direction)))) {
                    connectionDirection = direction;
                    connectionCount++;
                    if (block.canConnectTo(spotlight.getLevel().getBlockState(
                            spotlight.getBlockPos().relative(direction.getOpposite())))) {
                        connectionCount++;
                    }
                    break;
                }
            }
        }

        long revision = spotlight.clientRenderSnapshotRevision();
        SpotlightBlockEntity.RenderSnapshot previous = spotlight.renderSnapshot();
        if (previous.revision() == revision
                && previous.blockState() == state
                && previous.connectionDirection() == connectionDirection
                && previous.connectionCount() == connectionCount) {
            return;
        }

        switch (block.kind()) {
            case INCANDESCENT -> updateIncandescentSnapshot(
                    spotlight, block, state, facing, revision);
            case FLUORESCENT -> updateFluorescentSnapshot(
                    spotlight, block, state, facing, connectionDirection, connectionCount, revision);
            case HALOGEN -> updateHalogenSnapshot(
                    spotlight, block, state, facing, revision);
        }
    }

    private static void updateIncandescentSnapshot(SpotlightBlockEntity spotlight, SpotlightBlock block,
                                                   BlockState state, Direction facing, long revision) {
        ModelResourceLocation model = block.isLit() ? INCANDESCENT_ON : INCANDESCENT_OFF;
        PoseStack geometryPose = new PoseStack();
        applyIncandescentPose(geometryPose, facing);
        BakedBlockGeometrySnapshot.Result geometry = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(model), state, List.of(geometryPose), spotlight.getBlockPos());
        spotlight.publishRenderSnapshot(new SpotlightBlockEntity.RenderSnapshot(
                state, null, 0, geometry.modelToken(), geometry.bounds(), revision));
    }

    private static void updateFluorescentSnapshot(SpotlightBlockEntity spotlight, SpotlightBlock block,
                                                  BlockState state, Direction facing,
                                                  Direction connectionDirection, int connectionCount,
                                                  long revision) {
        ModelResourceLocation model = fluorescentModel(connectionCount, block.isLit());
        PoseStack geometryPose = new PoseStack();
        applyFluorescentPose(geometryPose, facing, connectionDirection);
        BakedBlockGeometrySnapshot.Result geometry = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(model), state, List.of(geometryPose), spotlight.getBlockPos());
        spotlight.publishRenderSnapshot(new SpotlightBlockEntity.RenderSnapshot(
                state, connectionDirection, connectionCount,
                geometry.modelToken(), geometry.bounds(), revision));
    }

    private static void updateHalogenSnapshot(SpotlightBlockEntity spotlight, SpotlightBlock block,
                                              BlockState state, Direction facing, long revision) {
        ModelResourceLocation model = block.isLit() ? HALOGEN_ON : HALOGEN_OFF;
        PoseStack geometryPose = new PoseStack();
        applyHalogenPose(geometryPose, facing);
        BakedBlockGeometrySnapshot.Result geometry = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(model), state, List.of(geometryPose), spotlight.getBlockPos());
        spotlight.publishRenderSnapshot(new SpotlightBlockEntity.RenderSnapshot(
                state, null, 0, geometry.modelToken(), geometry.bounds(), revision));
    }

    private static ModelResourceLocation fluorescentModel(int connections, boolean lit) {
        if (connections == 0) {
            return lit ? FLUORO_SINGLE_ON : FLUORO_SINGLE_OFF;
        }
        if (connections == 1) {
            return lit ? FLUORO_CAP_ON : FLUORO_CAP_OFF;
        }
        return lit ? FLUORO_MID_ON : FLUORO_MID_OFF;
    }

    private static void applyIncandescentPose(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.5F, 1.0F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case UP -> {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            case NORTH -> {
                poseStack.translate(0.5F, 0.5F, 1.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case SOUTH -> {
                poseStack.translate(0.5F, 0.5F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.translate(1.0F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case EAST -> poseStack.translate(0.0F, 0.5F, 0.5F);
        }
    }

    private static void applyHalogenPose(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.5F, 1.0F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case UP -> {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            case NORTH -> {
                poseStack.translate(0.5F, 0.5F, 1.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case SOUTH -> {
                poseStack.translate(0.5F, 0.5F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.translate(1.0F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case EAST -> poseStack.translate(0.0F, 0.5F, 0.5F);
        }
    }

    private static void applyFluorescentPose(PoseStack poseStack, Direction facing,
                                             Direction connectionDirection) {
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.5F, 1.0F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                applyFluorescentDownRoll(poseStack, connectionDirection);
            }
            case UP -> {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                applyFluorescentUpRoll(poseStack, connectionDirection);
            }
            case NORTH -> {
                poseStack.translate(0.5F, 0.5F, 1.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                applyFluorescentNorthRoll(poseStack, connectionDirection);
            }
            case SOUTH -> {
                poseStack.translate(0.5F, 0.5F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                applyFluorescentSouthRoll(poseStack, connectionDirection);
            }
            case WEST -> {
                poseStack.translate(1.0F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                applyFluorescentWestRoll(poseStack, connectionDirection);
            }
            case EAST -> {
                poseStack.translate(0.0F, 0.5F, 0.5F);
                applyFluorescentEastRoll(poseStack, connectionDirection);
            }
        }
    }

    private static void applyFluorescentDownRoll(PoseStack poseStack, Direction connectionDirection) {
        if (connectionDirection == null) return;
        switch (connectionDirection) {
            case NORTH -> { }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
            case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
    }

    private static void applyFluorescentUpRoll(PoseStack poseStack, Direction connectionDirection) {
        if (connectionDirection == null) return;
        switch (connectionDirection) {
            case NORTH -> { }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
            case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
    }

    private static void applyFluorescentNorthRoll(PoseStack poseStack, Direction connectionDirection) {
        if (connectionDirection == null) return;
        switch (connectionDirection) {
            case NORTH -> { }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
            case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case WEST -> { }
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
    }

    private static void applyFluorescentSouthRoll(PoseStack poseStack, Direction connectionDirection) {
        if (connectionDirection == null) return;
        switch (connectionDirection) {
            case NORTH -> { }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
    }

    private static void applyFluorescentWestRoll(PoseStack poseStack, Direction connectionDirection) {
        if (connectionDirection == null) return;
        switch (connectionDirection) {
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
            case SOUTH -> { }
            case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
    }

    private static void applyFluorescentEastRoll(PoseStack poseStack, Direction connectionDirection) {
        if (connectionDirection == null) return;
        switch (connectionDirection) {
            case NORTH -> { }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-180.0F));
            case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }
}
