package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.FelBlock;
import com.reinhardt.hbm.blockentity.FelBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class FelBlockEntityRenderer implements LongRangeBlockEntityRenderer<FelBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_fel");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB[] LOCAL_BOUNDS = buildFelBounds();
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;

    public FelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(FelBlockEntity fel, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = fel.getBlockState();
        Direction facing = felFacing(state);

        poseStack.pushPose();
        applyFelTransform(poseStack, facing);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);

        if (fel.beamVisible()) {
            int color = fel.mode().renderedBeamColor() == 0
                    ? fel.mode().guiColor(fel.getLevel() == null ? 0L : fel.getLevel().getGameTime())
                    : fel.mode().renderedBeamColor();
            poseStack.translate(0.0D, 1.5D, -1.5D);
            renderBeam(poseStack, bufferSource, Math.max(1, fel.distance() - 3), color, fel.getLevel() == null ? 0L : fel.getLevel().getGameTime());
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FelBlockEntity blockEntity) {
        Direction facing = felFacing(blockEntity.getBlockState());
        /* Full distance 1..24 and every discrete spiral/random phase. The beam's
         * transverse extrema remain inside the OBJ; the exact local -Z extreme
         * comes from the far segment corner. */
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, int length, int color, long gameTime) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        double beamLength = length + 1.0D;
        renderSpiralBeam(pose, consumer, beamLength, (int) (gameTime % 1000L), 2, 0.0625D, color);
        renderRandomBeam(pose, consumer, beamLength, (int) (gameTime % 500L), Math.max(2, length / 2 + 1), 0.0625D, color);
    }

    private static void renderSpiralBeam(PoseStack.Pose pose, VertexConsumer consumer, double beamLength,
                                         int phase, int turns, double thickness, int color) {
        int segments = Math.max(8, (int) Math.ceil(beamLength * 2.0D));
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= segments; index++) {
            double progress = index / (double) segments;
            double angle = Math.toRadians(phase + 360.0D * turns * progress);
            // The FEL owns a fixed local -Z beam. These are the exact axes
            // produced by its former Vec3 basis: axisX=-X, axisZ=-Y.
            double pointX = -Math.cos(angle) * thickness;
            double pointY = -Math.sin(angle) * thickness;
            double pointZ = -beamLength * progress;
            if (index > 0) {
                beamQuad(consumer, pose, previousX, previousY, previousZ,
                        pointX, pointY, pointZ, thickness * 0.35D, color);
            }
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
    }

    private static void renderRandomBeam(PoseStack.Pose pose, VertexConsumer consumer, double beamLength,
                                         int seed, int segments, double thickness, int color) {
        long randomState = (seed ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 1; index <= segments; index++) {
            double progress = index / (double) segments;
            double pointX = 0.0D;
            double pointY = 0.0D;
            double pointZ = -beamLength * progress;
            if (index < segments) {
                randomState = javaRandomNextState(randomState);
                long high = randomState >>> 22;
                randomState = javaRandomNextState(randomState);
                pointX = (((high << 27) + (randomState >>> 21)) * 0x1.0p-53 - 0.5D) * thickness * 4.0D;
                randomState = javaRandomNextState(randomState);
                high = randomState >>> 22;
                randomState = javaRandomNextState(randomState);
                pointY = (((high << 27) + (randomState >>> 21)) * 0x1.0p-53 - 0.5D) * thickness * 4.0D;
                randomState = javaRandomNextState(randomState);
                high = randomState >>> 22;
                randomState = javaRandomNextState(randomState);
                pointZ += (((high << 27) + (randomState >>> 21)) * 0x1.0p-53 - 0.5D) * thickness * 2.0D;
            }
            beamQuad(consumer, pose, previousX, previousY, previousZ,
                    pointX, pointY, pointZ, thickness * 0.5D, color);
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
    }

    private static long javaRandomNextState(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double startX, double startY, double startZ,
                                 double endX, double endY, double endZ,
                                 double thickness, int color) {
        double directionX = endX - startX;
        double directionZ = endZ - startZ;
        double axisX = directionZ;
        double axisZ = -directionX;
        double axisLengthSqr = axisX * axisX + axisZ * axisZ;
        if (axisLengthSqr < 1.0E-5D) {
            axisX = thickness;
            axisZ = 0.0D;
        } else {
            double scale = thickness / Math.sqrt(axisLengthSqr);
            axisX *= scale;
            axisZ *= scale;
        }
        beamVertex(consumer, pose, startX + axisX, startY + thickness, startZ + axisZ, color);
        beamVertex(consumer, pose, startX - axisX, startY + thickness, startZ - axisZ, color);
        beamVertex(consumer, pose, endX - axisX, endY - thickness, endZ - axisZ, color);
        beamVertex(consumer, pose, endX + axisX, endY - thickness, endZ + axisZ, color);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   double x, double y, double z, int color) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static AABB[] buildFelBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyFelTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -23.501984F, 1.5F, 3.0F, 4.5F);
        }
        return bounds;
    }

    private static void applyFelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderFEL metadata 2/3/4/5: 0/180/90/270 degrees.
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case NORTH -> { }
            case UP, DOWN -> throw new IllegalArgumentException("FEL facing must be horizontal: " + facing);
        }
    }

    private static Direction felFacing(BlockState state) {
        if (!state.hasProperty(FelBlock.FACING)) {
            throw new IllegalStateException("FEL state is missing its owned facing property: " + state);
        }
        return state.getValue(FelBlock.FACING);
    }
}
