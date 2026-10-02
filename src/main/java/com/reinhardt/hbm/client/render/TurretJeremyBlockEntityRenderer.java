package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.TurretJeremyBlock;
import com.reinhardt.hbm.blockentity.TurretJeremyBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class TurretJeremyBlockEntityRenderer implements LongRangeBlockEntityRenderer<TurretJeremyBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/turret_jeremy_base");
    private static final ModelResourceLocation CARRIAGE = MachineModelRenderer.standalone("block/turret_jeremy_carriage");
    private static final ModelResourceLocation GUN = MachineModelRenderer.standalone("block/turret_jeremy_gun");
    private static final ModelResourceLocation CONNECTORS = MachineModelRenderer.standalone("block/turret_jeremy_connectors");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    /* Exact OBJ envelope across full yaw and the machine's legal -45..30 degree pitch. */
    private static final double HORIZONTAL_RADIUS = 4.628799115321381D;
    private static final double MIN_Y = -1.9029513844602601D;
    private static final double MAX_Y = 4.858757210636101D;
    // The block entity owns world-space connector positions.  Rendering uses
    // only these invariant side/offset fields, so keep an immutable renderer-
    // local table instead of rebuilding eight BlockPos/record objects per frame.
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

    public TurretJeremyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(CARRIAGE);
        event.register(GUN);
        event.register(CONNECTORS);
    }

    @Override
    public void render(TurretJeremyBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turret.getBlockState();
        Direction facing = jeremyFacing(state);
        float posX = jeremyOriginX(facing);
        float posZ = jeremyOriginZ(facing);

        poseStack.pushPose();
        poseStack.translate(posX, 0.0F, posZ);
        renderConnectors(turret, poseStack, bufferSource, state, packedLight, packedOverlay);
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);

        double yaw = -Math.toDegrees(turret.renderYaw(partialTick)) - 90.0D;
        double pitch = Math.toDegrees(turret.renderPitch(partialTick));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
        STATIC_MESHES.render(CARRIAGE, poseStack, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) pitch));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        STATIC_MESHES.render(GUN, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TurretJeremyBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        Direction facing = jeremyFacing(blockEntity.getBlockState());
        double centerX = jeremyOriginX(facing);
        double centerZ = jeremyOriginZ(facing);
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
        poseStack.translate(0.0F, -2.0F, 0.0F);
        poseStack.scale(2.5F, 2.5F, 2.5F);
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);
        STATIC_MESHES.render(CARRIAGE, poseStack, state, packedLight, packedOverlay);
        STATIC_MESHES.render(GUN, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderConnectors(
            TurretJeremyBlockEntity turret,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        TurretJeremyBlockEntity.ConnectorRenderSnapshot connectors = turret.connectorRenderSnapshot();
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
        applyJeremyConnectorOrientation(poseStack, side);
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
        STATIC_MESHES.render(CONNECTORS, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static Direction jeremyFacing(BlockState state) {
        if (!state.hasProperty(TurretJeremyBlock.FACING)) {
            throw new IllegalStateException("Jeremy turret state is missing its owned facing property: " + state);
        }
        return state.getValue(TurretJeremyBlock.FACING);
    }

    private static float jeremyOriginX(Direction facing) {
        return switch (facing) {
            case NORTH, WEST -> 1.0F;
            case EAST, SOUTH -> 0.0F;
            case UP, DOWN -> throw new IllegalArgumentException("Jeremy turret facing must be horizontal: " + facing);
        };
    }

    private static float jeremyOriginZ(Direction facing) {
        return switch (facing) {
            case NORTH, EAST -> 1.0F;
            case WEST, SOUTH -> 0.0F;
            case UP, DOWN -> throw new IllegalArgumentException("Jeremy turret facing must be horizontal: " + facing);
        };
    }

    private static void applyJeremyConnectorOrientation(PoseStack poseStack, Direction side) {
        switch (side) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Jeremy connector side must be horizontal: " + side);
        }
    }

    private record RenderConnectorSpec(Direction machineSide, float renderOffsetX, float renderOffsetZ) {
    }
}
