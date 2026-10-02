package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ExposureChamberBlock;
import com.reinhardt.hbm.blockentity.ExposureChamberBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Quaternionf;

public class ExposureChamberBlockEntityRenderer implements LongRangeBlockEntityRenderer<ExposureChamberBlockEntity> {
    private static final ModelResourceLocation CHAMBER = MachineModelRenderer.standalone("block/machine_exposure_chamber_chamber");
    private static final ModelResourceLocation MAGNETS = MachineModelRenderer.standalone("block/machine_exposure_chamber_magnets");
    private static final ModelResourceLocation CORE = MachineModelRenderer.standalone("block/machine_exposure_chamber_core");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB NORTH_RENDER_BOUNDS = createExposureChamberLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createExposureChamberLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createExposureChamberLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createExposureChamberLocalBounds(Direction.EAST);
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;

    public ExposureChamberBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(CHAMBER);
        event.register(MAGNETS);
        event.register(CORE);
    }

    @Override
    public void render(ExposureChamberBlockEntity chamber, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = chamber.getBlockState();
        Direction facing = exposureChamberFacing(state);
        float rotation = chamber.rotation(partialTick);

        poseStack.pushPose();
        applyExposureChamberPose(poseStack, facing);
        STATIC_MESHES.render(CHAMBER, poseStack, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rotation)));
        STATIC_MESHES.render(MAGNETS, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (chamber.isOn()) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rotation / 2.0F)));
            double bob = Math.sin(((chamber.getLevel() == null ? 0 : chamber.getLevel().getGameTime()) + partialTick) * 0.125D) * 0.0625D;
            poseStack.translate(0.0D, bob, 0.0D);
            STATIC_MESHES.render(CORE, poseStack, state, LightTexture.FULL_BRIGHT, packedOverlay);
            poseStack.popPose();
            renderBeams(chamber, poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ExposureChamberBlockEntity blockEntity) {
        Direction facing = exposureChamberFacing(blockEntity.getBlockState());
        return exposureChamberRenderBounds(blockEntity.getBlockPos(), facing);
    }

    /** Exact RenderExposureChamber metadata mapping. */
    private static void applyExposureChamberPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Exposure chamber facing must be horizontal: " + facing);
        }
    }

    private static Direction exposureChamberFacing(BlockState state) {
        if (!state.hasProperty(ExposureChamberBlock.FACING)) {
            throw new IllegalStateException("Exposure chamber state is missing its owned facing property: " + state);
        }
        Direction facing = state.getValue(ExposureChamberBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Exposure chamber state has an invalid vertical facing: " + state);
        }
        return facing;
    }

    static AABB exposureChamberRenderBounds(BlockPos pos, Direction facing) {
        AABB localBounds = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Exposure chamber facing must be horizontal: " + facing);
        };
        return localBounds.move(pos);
    }

    private static AABB createExposureChamberLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyExposureChamberPose(poseStack, facing);
        /* The exact 788 face-used Chamber vertices contain every continuous magnet yaw,
         * the core's full +/-0.0625 bob and yaw, and every emitted beam vertex. */
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -2.5F, 0.0F, -8.5F,
                2.5F, 5.0F, 2.5F);
    }

    private static void renderBeams(ExposureChamberBlockEntity chamber, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!isNearEnoughForEffects(chamber, 96.0D)) {
            return;
        }
        long gameTime = chamber.getLevel() == null ? 0L : chamber.getLevel().getGameTime();
        int duration = 8;
        long randomState = javaRandomInitialState(gameTime / duration);
        int color = gameTime % duration >= duration / 2 ? 0x80D0FF : 0xFFFFFF;
        int phase = (int) (System.currentTimeMillis() % 1000L) / 50;
        randomState = javaRandomNextState(randomState); // Legacy code discarded its first nextInt(2).

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        randomState = javaRandomNextState(randomState);
        if (!javaRandomBoolean(randomState)) {
            renderRandomLineBeam(pose, consumer, 0.0D, 3.675D, -7.5D, 0.0D, 0.0D, 5.0D,
                    color, 0xFFFFFF, phase, 15, 0.125D);
        }
        randomState = javaRandomNextState(randomState);
        if (!javaRandomBoolean(randomState)) {
            renderRandomLineBeam(pose, consumer, 1.1875D, 2.5D, -7.5D, 0.0D, 0.0D, 5.0D,
                    color, 0xFFFFFF, phase, 15, 0.125D);
        }
        randomState = javaRandomNextState(randomState);
        if (!javaRandomBoolean(randomState)) {
            renderRandomLineBeam(pose, consumer, -1.1875D, 2.5D, -7.5D, 0.0D, 0.0D, 5.0D,
                    color, 0xFFFFFF, phase, 15, 0.125D);
        }

        renderRandomLineBeam(pose, consumer, 0.0D, 1.75D, 0.0D, 0.0D, 1.5D, 0.0D,
                0x80D0FF, 0xFFFFFF, phase, 10, 0.125D);
        renderRandomLineBeam(pose, consumer, 0.0D, 1.75D, 0.0D, 0.0D, 1.5D, 0.0D,
                0x8080FF, 0xFFFFFF, phase + 5, 10, 0.125D);
        int spiralPhase = (int) (System.currentTimeMillis() % 360L);
        renderSpiralLineBeam(pose, consumer, 0.0D, 2.5D, 0.0D, 0.0D, 0.0D, -1.0D,
                0xFFFF80, 0xFFFFFF, spiralPhase, 15, 0.125D);
        renderSpiralLineBeam(pose, consumer, 0.0D, 2.5D, 0.0D, 0.0D, 0.0D, -1.0D,
                0xFF8080, 0xFFFFFF, spiralPhase + 180, 15, 0.125D);
    }

    private static long javaRandomInitialState(long seed) {
        return (seed ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
    }

    private static long javaRandomNextState(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
    }

    private static boolean javaRandomBoolean(long state) {
        return (state >>> 47) != 0L;
    }

    private static float javaRandomFloat(long state) {
        return (float) (state >>> 24) / 16777216.0F;
    }

    private static void renderRandomLineBeam(PoseStack.Pose pose, VertexConsumer consumer,
                                             double startX, double startY, double startZ,
                                             double skeletonX, double skeletonY, double skeletonZ,
                                             int outerColor, int innerColor, int phase, int segments, double size) {
        renderLineBeam(pose, consumer, startX, startY, startZ, skeletonX, skeletonY, skeletonZ,
                outerColor, innerColor, phase, segments, size, true);
    }

    private static void renderSpiralLineBeam(PoseStack.Pose pose, VertexConsumer consumer,
                                             double startX, double startY, double startZ,
                                             double skeletonX, double skeletonY, double skeletonZ,
                                             int outerColor, int innerColor, int phase, int segments, double size) {
        renderLineBeam(pose, consumer, startX, startY, startZ, skeletonX, skeletonY, skeletonZ,
                outerColor, innerColor, phase, segments, size, false);
    }

    private static void renderLineBeam(PoseStack.Pose pose, VertexConsumer consumer,
                                       double startX, double startY, double startZ,
                                       double skeletonX, double skeletonY, double skeletonZ,
                                       int outerColor, int innerColor, int phase, int segments,
                                       double size, boolean randomWave) {
        double length = Math.sqrt(skeletonX * skeletonX + skeletonY * skeletonY + skeletonZ * skeletonZ);
        double axisYX = skeletonX / length;
        double axisYY = skeletonY / length;
        double axisYZ = skeletonZ / length;
        double axisXX = axisYZ;
        double axisXY = 0.0D;
        double axisXZ = -axisYX;
        double axisXLengthSqr = axisXX * axisXX + axisXZ * axisXZ;
        if (axisXLengthSqr < 1.0E-5D) {
            axisXX = 1.0D;
            axisXZ = 0.0D;
        } else {
            double inverseAxisXLength = 1.0D / Math.sqrt(axisXLengthSqr);
            axisXX *= inverseAxisXLength;
            axisXZ *= inverseAxisXLength;
        }
        double axisZX = axisXY * axisYZ - axisXZ * axisYY;
        double axisZY = axisXZ * axisYX - axisXX * axisYZ;
        double axisZZ = axisXX * axisYY - axisXY * axisYX;
        double inverseAxisZLength = 1.0D / Math.sqrt(axisZX * axisZX + axisZY * axisZY + axisZZ * axisZZ);
        axisZX *= inverseAxisZLength;
        axisZY *= inverseAxisZLength;
        axisZZ *= inverseAxisZLength;

        long randomState = javaRandomInitialState(phase);
        double segmentLength = length / segments;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= segments; index++) {
            double angle;
            if (randomWave) {
                randomState = javaRandomNextState(randomState);
                angle = Math.PI * 2.0D * javaRandomFloat(randomState);
                randomState = javaRandomNextState(randomState);
                angle += Math.PI * 2.0D * javaRandomFloat(randomState);
            } else {
                angle = Math.toRadians(phase + 45.0D * index);
            }
            double radialX = Math.cos(angle) * size;
            double radialZ = Math.sin(angle) * size;
            double distance = segmentLength * index;
            double pointX = startX + axisYX * distance + axisXX * radialX + axisZX * radialZ;
            double pointY = startY + axisYY * distance + axisXY * radialX + axisZY * radialZ;
            double pointZ = startZ + axisYZ * distance + axisXZ * radialX + axisZZ * radialZ;
            if (index > 0) {
                renderBeamSegment(pose, consumer,
                        previousX, previousY, previousZ, pointX, pointY, pointZ,
                        axisXX, axisXY, axisXZ, axisZX, axisZY, axisZZ,
                        0.025D, outerColor);
            }
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
        renderBeamSegment(pose, consumer,
                startX, startY, startZ,
                startX + skeletonX, startY + skeletonY, startZ + skeletonZ,
                axisXX, axisXY, axisXZ, axisZX, axisZY, axisZZ,
                0.0125D, innerColor);
    }

    private static void renderBeamSegment(PoseStack.Pose pose, VertexConsumer consumer,
                                          double previousX, double previousY, double previousZ,
                                          double pointX, double pointY, double pointZ,
                                          double axisXX, double axisXY, double axisXZ,
                                          double axisZX, double axisZY, double axisZZ,
                                          double thickness, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        double xX = axisXX * thickness;
        double xY = axisXY * thickness;
        double xZ = axisXZ * thickness;
        double zX = axisZX * thickness;
        double zY = axisZY * thickness;
        double zZ = axisZZ * thickness;
        beamQuad(consumer, pose,
                previousX + xX + zX, previousY + xY + zY, previousZ + xZ + zZ,
                previousX + xX - zX, previousY + xY - zY, previousZ + xZ - zZ,
                pointX + xX - zX, pointY + xY - zY, pointZ + xZ - zZ,
                pointX + xX + zX, pointY + xY + zY, pointZ + xZ + zZ,
                red, green, blue);
        beamQuad(consumer, pose,
                previousX - xX + zX, previousY - xY + zY, previousZ - xZ + zZ,
                previousX - xX - zX, previousY - xY - zY, previousZ - xZ - zZ,
                pointX - xX - zX, pointY - xY - zY, pointZ - xZ - zZ,
                pointX - xX + zX, pointY - xY + zY, pointZ - xZ + zZ,
                red, green, blue);
        beamQuad(consumer, pose,
                previousX + xX + zX, previousY + xY + zY, previousZ + xZ + zZ,
                previousX - xX + zX, previousY - xY + zY, previousZ - xZ + zZ,
                pointX - xX + zX, pointY - xY + zY, pointZ - xZ + zZ,
                pointX + xX + zX, pointY + xY + zY, pointZ + xZ + zZ,
                red, green, blue);
        beamQuad(consumer, pose,
                previousX + xX - zX, previousY + xY - zY, previousZ + xZ - zZ,
                previousX - xX - zX, previousY - xY - zY, previousZ - xZ - zZ,
                pointX - xX - zX, pointY - xY - zY, pointZ - xZ - zZ,
                pointX + xX - zX, pointY + xY - zY, pointZ + xZ - zZ,
                red, green, blue);
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double ax, double ay, double az,
                                 double bx, double by, double bz,
                                 double cx, double cy, double cz,
                                 double dx, double dy, double dz,
                                 int red, int green, int blue) {
        beamVertex(consumer, pose, ax, ay, az, red, green, blue);
        beamVertex(consumer, pose, bx, by, bz, red, green, blue);
        beamVertex(consumer, pose, cx, cy, cz, red, green, blue);
        beamVertex(consumer, pose, dx, dy, dz, red, green, blue);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   double x, double y, double z,
                                   int red, int green, int blue) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static boolean isNearEnoughForEffects(ExposureChamberBlockEntity chamber, double blocks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.distanceToSqr(
                chamber.getBlockPos().getX() + 0.5D,
                chamber.getBlockPos().getY() + 1.5D,
                chamber.getBlockPos().getZ() + 0.5D
        ) < Mth.square(blocks);
    }
}
