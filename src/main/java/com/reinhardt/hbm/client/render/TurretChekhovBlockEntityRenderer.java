package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.TurretChekhovBlock;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class TurretChekhovBlockEntityRenderer implements BlockEntityRenderer<TurretChekhovBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/turret_chekhov_base");
    private static final ModelResourceLocation CARRIAGE = MachineModelRenderer.standalone("block/turret_chekhov_carriage");
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/turret_chekhov_body");
    private static final ModelResourceLocation BARRELS = MachineModelRenderer.standalone("block/turret_chekhov_barrels");
    private static final ModelResourceLocation CONNECTORS = MachineModelRenderer.standalone("block/turret_chekhov_connectors");

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
        Direction facing = state.hasProperty(TurretChekhovBlock.FACING) ? state.getValue(TurretChekhovBlock.FACING) : Direction.NORTH;
        float posX = switch (facing) {
            case NORTH -> 1.0F;
            case WEST -> 1.0F;
            case EAST, SOUTH -> 0.0F;
            default -> 0.0F;
        };
        float posZ = switch (facing) {
            case NORTH -> 1.0F;
            case WEST -> 0.0F;
            case EAST -> 1.0F;
            case SOUTH -> 0.0F;
            default -> 0.0F;
        };

        poseStack.pushPose();
        poseStack.translate(posX, 0.0F, posZ);
        renderConnectors(turret, poseStack, bufferSource, state, packedLight, packedOverlay, posX, posZ);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        double yaw = -Math.toDegrees(turret.renderYaw(partialTick)) - 90.0D;
        double pitch = Math.toDegrees(turret.renderPitch(partialTick));
        poseStack.mulPose(yaw((float) yaw));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CARRIAGE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(pitch), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(turret.renderSpin(partialTick)), -1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BARRELS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TurretChekhovBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    @Override
    public boolean shouldRenderOffScreen(TurretChekhovBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
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
            int packedOverlay,
            float posX,
            float posZ
    ) {
        if (turret.getLevel() == null) {
            return;
        }
        Direction facing = state.hasProperty(TurretChekhovBlock.FACING) ? state.getValue(TurretChekhovBlock.FACING) : Direction.NORTH;
        for (TurretChekhovBlockEntity.ConnectorSpec spec : TurretChekhovBlockEntity.connectorSpecs(turret.getBlockPos(), facing)) {
            renderConnectorIfPresent(turret, poseStack, bufferSource, state, packedLight, packedOverlay, spec);
        }
    }

    private static void renderConnectorIfPresent(
            TurretChekhovBlockEntity turret,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            TurretChekhovBlockEntity.ConnectorSpec spec
    ) {
        Direction side = spec.machineSide();
        if (!PowerNetworkManager.canCableConnectTo(turret.getLevel(), spec.pos(), side.getOpposite())) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(yaw(sideToDegrees(side)));
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CONNECTORS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static float sideToDegrees(Direction side) {
        return switch (side) {
            case WEST -> 0.0F;
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> 270.0F;
            default -> 0.0F;
        };
    }
}
