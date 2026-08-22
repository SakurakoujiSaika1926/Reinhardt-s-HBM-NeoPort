package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DfcComponentBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final ModelResourceLocation EMITTER = MachineModelRenderer.standalone("block/dfc_emitter");
    private static final ModelResourceLocation RECEIVER = MachineModelRenderer.standalone("block/dfc_receiver");
    private static final ModelResourceLocation INJECTOR = MachineModelRenderer.standalone("block/dfc_injector");
    private static final ModelResourceLocation STABILIZER = MachineModelRenderer.standalone("block/dfc_stabilizer");

    public DfcComponentBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(EMITTER);
        event.register(RECEIVER);
        event.register(INJECTOR);
        event.register(STABILIZER);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation model = model(blockEntity);
        if (model == null) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(DfcComponentBlock.FACING) ? state.getValue(DfcComponentBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        applyLegacyTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (blockEntity instanceof DfcEmitterBlockEntity emitter && emitter.beam() > 0) {
            renderBeam(poseStack, bufferSource, facing, emitter.beam(), 0xE0FF6600, 0.07D);
        } else if (blockEntity instanceof DfcInjectorBlockEntity injector && injector.beam() > 0) {
            int color = injector.tank(0).amount() > 0 ? injector.tank(0).type().color() : injector.tank(1).type().color();
            renderBeam(poseStack, bufferSource, facing, injector.beam(), 0xC0000000 | color, 0.035D);
        } else if (blockEntity instanceof DfcStabilizerBlockEntity stabilizer && stabilizer.beam() > 0) {
            renderBeam(poseStack, bufferSource, facing, stabilizer.beam(), 0xE0FFA200, 0.04D);
        }
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(52.0D);
    }

    private static ModelResourceLocation model(BlockEntity blockEntity) {
        if (blockEntity instanceof DfcEmitterBlockEntity) {
            return EMITTER;
        }
        if (blockEntity instanceof DfcReceiverBlockEntity) {
            return RECEIVER;
        }
        if (blockEntity instanceof DfcInjectorBlockEntity) {
            return INJECTOR;
        }
        if (blockEntity instanceof DfcStabilizerBlockEntity) {
            return STABILIZER;
        }
        return null;
    }

    private static void applyLegacyTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.0D, 0.5D, -0.5D);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                poseStack.translate(0.0D, 0.5D, 0.5D);
                poseStack.mulPose(com.mojang.math.Axis.XN.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> {
            }
        }
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, Direction direction, int length, int argb, double thickness) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        Vec3 start = new Vec3(0.5D, 0.5D, 0.5D);
        Vec3 end = start.add(direction.getStepX() * length, direction.getStepY() * length, direction.getStepZ() * length);
        beamQuad(consumer, pose, start, end, thickness, argb);
        beamQuad(consumer, pose, start.add(0.0D, thickness, 0.0D), end.add(0.0D, thickness, 0.0D), thickness * 0.5D, argb);
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end, double thickness, int argb) {
        Vec3 direction = end.subtract(start);
        Vec3 axisX = new Vec3(direction.z, 0.0D, -direction.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisX = axisX.normalize().scale(thickness);
        Vec3 axisY = new Vec3(0.0D, thickness, 0.0D);
        beamVertex(consumer, pose, start.add(axisX).add(axisY), argb);
        beamVertex(consumer, pose, start.subtract(axisX).add(axisY), argb);
        beamVertex(consumer, pose, end.subtract(axisX).subtract(axisY), argb);
        beamVertex(consumer, pose, end.add(axisX).subtract(axisY), argb);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int argb) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
