package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.TurretChekhovBlock;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class TurretChekhovBlockEntityRenderer implements LongRangeBlockEntityRenderer<TurretChekhovBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/turret_chekhov_base");
    private static final ModelResourceLocation CARRIAGE = MachineModelRenderer.standalone("block/turret_chekhov_carriage");
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/turret_chekhov_body");
    private static final ModelResourceLocation BARRELS = MachineModelRenderer.standalone("block/turret_chekhov_barrels");
    private static final ModelResourceLocation CONNECTORS = MachineModelRenderer.standalone("block/turret_chekhov_connectors");
    /* Exact OBJ envelope across full yaw/spin and the machine's legal -30..45 degree pitch. */
    private static final double HORIZONTAL_RADIUS = 3.2655876504571117D;
    private static final double MIN_Y = -0.4009928026480037D;
    private static final double MAX_Y = 4.023444218579035D;
    // Keep this machine's exact connector render table local.  World-space
    // ConnectorSpec values remain owned by the block entity and are not needed
    // to draw the already-published connected bit mask.
    private static final RenderConnectorSpec[] RENDER_CONNECTORS = {
            new RenderConnectorSpec(Direction.WEST, 0.0F, 0.0F),
            new RenderConnectorSpec(Direction.WEST, 0.0F, -1.0F),
            new RenderConnectorSpec(Direction.SOUTH, 0.0F, -1.0F),
            new RenderConnectorSpec(Direction.SOUTH, 0.0F, 0.0F),
            new RenderConnectorSpec(Direction.EAST, 0.0F, -1.0F),
            new RenderConnectorSpec(Direction.EAST, 0.0F, 0.0F),
            new RenderConnectorSpec(Direction.NORTH, 0.0F, -1.0F),
            new RenderConnectorSpec(Direction.NORTH, 0.0F, 0.0F)
    };

    public TurretChekhovBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(CARRIAGE);
        event.register(BODY);
        event.register(BARRELS);
        event.register(CONNECTORS);
    }

    @Override
    public void render(TurretChekhovBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turret.getBlockState();
        Direction facing = chekhovFacing(state);
        float posX = chekhovOriginX(facing);
        float posZ = chekhovOriginZ(facing);

        poseStack.pushPose();
        poseStack.translate(posX, 0.0F, posZ);
        renderConnectors(turret, poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        double yaw = -Math.toDegrees(turret.renderYaw(partialTick)) - 90.0D;
        double pitch = Math.toDegrees(turret.renderPitch(partialTick));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CARRIAGE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) pitch));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(Axis.XN.rotationDegrees(turret.renderSpin(partialTick)));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BARRELS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TurretChekhovBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        Direction facing = chekhovFacing(blockEntity.getBlockState());
        double centerX = chekhovOriginX(facing);
        double centerZ = chekhovOriginZ(facing);
        return new AABB(
                pos.getX() + centerX - HORIZONTAL_RADIUS,
                pos.getY() + MIN_Y,
                pos.getZ() + centerZ - HORIZONTAL_RADIUS,
                pos.getX() + centerX + HORIZONTAL_RADIUS,
                pos.getY() + MAX_Y,
                pos.getZ() + centerZ + HORIZONTAL_RADIUS
        );
    }

    static void renderItem(PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0F, -3.0F, 0.0F);
        poseStack.scale(4.0F, 4.0F, 4.0F);
        poseStack.translate(-0.75F, 0.0F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CARRIAGE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BARRELS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderConnectors(
            TurretChekhovBlockEntity turret,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        TurretChekhovBlockEntity.ConnectorRenderSnapshot connectors = turret.connectorRenderSnapshot();
        for (int index = 0; index < RENDER_CONNECTORS.length; index++) {
            renderConnectorIfPresent(poseStack, bufferSource, state, packedLight, packedOverlay,
                    RENDER_CONNECTORS[index], connectors.connected(index));
        }
    }

    private static void renderConnectorIfPresent(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            RenderConnectorSpec spec,
            boolean connected
    ) {
        Direction side = spec.machineSide();
        if (!connected) {
            return;
        }
        poseStack.pushPose();
        applyChekhovConnectorOrientation(poseStack, side);
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CONNECTORS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static Direction chekhovFacing(BlockState state) {
        if (!state.hasProperty(TurretChekhovBlock.FACING)) {
            throw new IllegalStateException("Chekhov turret state is missing its owned facing property: " + state);
        }
        return state.getValue(TurretChekhovBlock.FACING);
    }

    private static float chekhovOriginX(Direction facing) {
        return switch (facing) {
            case NORTH, WEST -> 1.0F;
            case EAST, SOUTH -> 0.0F;
            case UP, DOWN -> throw new IllegalArgumentException("Chekhov turret facing must be horizontal: " + facing);
        };
    }

    private static float chekhovOriginZ(Direction facing) {
        return switch (facing) {
            case NORTH, EAST -> 1.0F;
            case WEST, SOUTH -> 0.0F;
            case UP, DOWN -> throw new IllegalArgumentException("Chekhov turret facing must be horizontal: " + facing);
        };
    }

    private static void applyChekhovConnectorOrientation(PoseStack poseStack, Direction side) {
        switch (side) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Chekhov connector side must be horizontal: " + side);
        }
    }

    private record RenderConnectorSpec(Direction machineSide, float renderOffsetX, float renderOffsetZ) {
    }
}
