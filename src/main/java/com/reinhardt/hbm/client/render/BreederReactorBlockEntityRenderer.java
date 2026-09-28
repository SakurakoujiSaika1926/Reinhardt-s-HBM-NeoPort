package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.BreederReactorBlock;
import com.reinhardt.hbm.blockentity.BreederReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class BreederReactorBlockEntityRenderer implements LongRangeBlockEntityRenderer<BreederReactorBlockEntity> {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_reactor_breeding");
    private static final AABB[] LOCAL_BOUNDS = buildBreederBounds();

    public BreederReactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(BreederReactorBlockEntity breeder, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = breeder.getBlockState();
        Direction facing = breederReactorFacing(state);
        poseStack.pushPose();
        applyBreederTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (breeder.progress() > 0.0F) {
            renderWorkingSparks(breeder, poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BreederReactorBlockEntity blockEntity) {
        Direction facing = breederReactorFacing(blockEntity.getBlockState());
        /* Exhausting all 100 clock phases and three seeds gives the extrema
         * below; the OBJ still supplies the Y extremes 0..3.25. */
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildBreederBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyBreederTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.53584254F, 0.0F, -0.52308357F,
                    0.53584254F, 3.25F, 0.52308357F);
        }
        return bounds;
    }

    private static void applyBreederTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderBreeder's fixed 90 degrees plus its metadata table.
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Breeder reactor facing must be horizontal: " + facing);
        }
    }

    private static Direction breederReactorFacing(BlockState state) {
        if (!state.hasProperty(BreederReactorBlock.FACING)) {
            throw new IllegalStateException("Breeder reactor state is missing its owned facing property: " + state);
        }
        return state.getValue(BreederReactorBlock.FACING);
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
        long randomState = (seed ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        randomState = advanceJavaRandom(randomState);
        long high = randomState >>> 22;
        randomState = advanceJavaRandom(randomState);
        double vectorX = ((high << 27) + (randomState >>> 21)) * 0x1.0p-53D - 0.5D;
        randomState = advanceJavaRandom(randomState);
        high = randomState >>> 22;
        randomState = advanceJavaRandom(randomState);
        double vectorY = ((high << 27) + (randomState >>> 21)) * 0x1.0p-53D - 0.5D;
        randomState = advanceJavaRandom(randomState);
        high = randomState >>> 22;
        randomState = advanceJavaRandom(randomState);
        double vectorZ = ((high << 27) + (randomState >>> 21)) * 0x1.0p-53D - 0.5D;
        double magnitude = Math.sqrt(vectorX * vectorX + vectorY * vectorY + vectorZ * vectorZ);
        if (magnitude < 1.0E-6D) return;
        vectorX /= magnitude;
        vectorY /= magnitude;
        vectorZ /= magnitude;

        PoseStack.Pose pose = poseStack.last();
        randomState = advanceJavaRandom(randomState);
        int count = 3 + (int) ((4L * (randomState >>> 17)) >> 31);
        for (int segment = 0; segment < count; segment++) {
            double previousX = x;
            double previousY = y;
            double previousZ = z;
            randomState = advanceJavaRandom(randomState);
            x += vectorX * 0.15D * ((float) (randomState >>> 24) * 0x1.0p-24F);
            randomState = advanceJavaRandom(randomState);
            y += vectorY * 0.15D * ((float) (randomState >>> 24) * 0x1.0p-24F);
            randomState = advanceJavaRandom(randomState);
            z += vectorZ * 0.15D * ((float) (randomState >>> 24) * 0x1.0p-24F);
            lineVertex(consumer, pose, previousX, previousY, previousZ, 0, 255, 0);
            lineVertex(consumer, pose, x, y, z, 0, 255, 0);
            lineVertex(consumer, pose, previousX, previousY, previousZ, 255, 255, 255);
            lineVertex(consumer, pose, x, y, z, 255, 255, 255);
        }
    }

    private static long advanceJavaRandom(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
    }

    private static void lineVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
                                   int red, int green, int blue) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
