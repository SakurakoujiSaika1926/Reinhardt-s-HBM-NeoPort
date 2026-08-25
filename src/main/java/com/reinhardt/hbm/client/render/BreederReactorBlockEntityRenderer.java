package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.BreederReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Random;

public class BreederReactorBlockEntityRenderer implements BlockEntityRenderer<BreederReactorBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_reactor_breeding");

    public BreederReactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(BreederReactorBlockEntity breeder, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = breeder.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (breeder.progress() > 0.0F) {
            renderWorkingSparks(breeder, poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BreederReactorBlockEntity blockEntity) {
        // TileEntityMachineReactorBreeding#getRenderBoundingBox: core through y + 3.
        return new AABB(blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ(),
                blockEntity.getBlockPos().getX() + 1.0D, blockEntity.getBlockPos().getY() + 3.0D, blockEntity.getBlockPos().getZ() + 1.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            // RenderBreeder applies a 90 degree base yaw, then this
            // metadata table. These are the resulting 1.7.10 rotations.
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            default -> 90.0F;
        };
    }

    /** Exact RenderSparks seed, position, segment count, and colors from RenderBreeder. */
    private static void renderWorkingSparks(BreederReactorBlockEntity breeder, PoseStack poseStack,
                                            MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        long phase = System.currentTimeMillis() % 10_000L / 100L;
        for (int index = 0; index < 3; index++) {
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F * index));
            renderSpark(consumer, poseStack, (int) phase + index, 0.0D, 1.5625D, 0.0D);
            poseStack.popPose();
        }
    }

    private static void renderSpark(VertexConsumer consumer, PoseStack poseStack, int seed,
                                    double x, double y, double z) {
        Random random = new Random(seed);
        double vectorX = random.nextDouble() - 0.5D;
        double vectorY = random.nextDouble() - 0.5D;
        double vectorZ = random.nextDouble() - 0.5D;
        double magnitude = Math.sqrt(vectorX * vectorX + vectorY * vectorY + vectorZ * vectorZ);
        if (magnitude < 1.0E-6D) return;
        vectorX /= magnitude;
        vectorY /= magnitude;
        vectorZ /= magnitude;

        PoseStack.Pose pose = poseStack.last();
        for (int segment = 0, count = 3 + random.nextInt(4); segment < count; segment++) {
            double previousX = x;
            double previousY = y;
            double previousZ = z;
            x += vectorX * 0.15D * random.nextFloat();
            y += vectorY * 0.15D * random.nextFloat();
            z += vectorZ * 0.15D * random.nextFloat();
            lineVertex(consumer, pose, previousX, previousY, previousZ, 0, 255, 0);
            lineVertex(consumer, pose, x, y, z, 0, 255, 0);
            lineVertex(consumer, pose, previousX, previousY, previousZ, 255, 255, 255);
            lineVertex(consumer, pose, x, y, z, 255, 255, 255);
        }
    }

    private static void lineVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
                                   int red, int green, int blue) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
