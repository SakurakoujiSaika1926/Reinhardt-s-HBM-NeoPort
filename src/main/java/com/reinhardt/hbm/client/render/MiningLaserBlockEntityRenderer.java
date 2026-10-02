package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.MiningLaserBlock;
import com.reinhardt.hbm.blockentity.MiningLaserBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Quaternionf;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public class MiningLaserBlockEntityRenderer implements LongRangeBlockEntityRenderer<MiningLaserBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final double LASER_SWEEP_RADIUS = 1.505199322349037D;
    private static final float LASER_SWEEP_EXTENT = floatPoseSweepExtent(LASER_SWEEP_RADIUS);
    private static final double BEAM_SPIRAL_RADIUS = 0.075D;
    private static final double BEAM_HALF_THICKNESS = 0.025D;
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_mining_laser_base");
    static final ModelResourceLocation PIVOT = MachineModelRenderer.standalone("block/machine_mining_laser_pivot");
    static final ModelResourceLocation LASER = MachineModelRenderer.standalone("block/machine_mining_laser_laser");
    /** CPU-only entries are owned weakly by their block entity and never cross machines or dimensions. */
    private final Map<MiningLaserBlockEntity, CachedBeamGeometry> beamGeometry = new WeakHashMap<>();

    public MiningLaserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(PIVOT);
        event.register(LASER);
    }

    @Override
    public void render(MiningLaserBlockEntity laser, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = laser.getBlockState();
        Direction facing = miningLaserFacing(state);
        MiningLaserBlockEntity.RenderSnapshot snapshot = laser.renderSnapshot();
        int phase = (int) ((laser.getLevel() == null ? 0L : laser.getLevel().getGameTime()) * -25L % 360L);
        CachedBeamGeometry geometry = geometryFor(laser, snapshot.target(), phase, snapshot.beamActive());

        poseStack.pushPose();
        poseStack.translate(0.5D, -1.0D, 0.5D);
        applyMiningLaserRootPose(poseStack, facing);
        renderParts(state, geometry, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (snapshot.beamActive()) {
            poseStack.pushPose();
            poseStack.translate(0.5D, -1.0D, 0.5D);
            renderBeam(geometry, poseStack, bufferSource);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(MiningLaserBlockEntity blockEntity) {
        /* Base plus every legal target orientation. The farthest face-used
         * Laser vertex is LASER_SWEEP_RADIUS from its (0,-1,0) pitch pivot;
         * LASER_SWEEP_EXTENT adds the proved float PoseStack error for the
         * complete yaw/pitch/cardinal hierarchy.  It also exceeds the Base's
         * +/-1.5 footprint. */
        AABB bounds = RenderBounds.localAnalytic(blockEntity.getBlockPos(),
                0.5D - LASER_SWEEP_EXTENT, -2.0D - LASER_SWEEP_EXTENT, 0.5D - LASER_SWEEP_EXTENT,
                0.5D + LASER_SWEEP_EXTENT, 2.0D, 0.5D + LASER_SWEEP_EXTENT);
        MiningLaserBlockEntity.RenderSnapshot snapshot = blockEntity.renderSnapshot();
        if (!snapshot.beamActive()) {
            return bounds;
        }
        CachedBeamGeometry geometry = geometryFor(blockEntity, snapshot.target(), Integer.MIN_VALUE, false);
        return bounds.minmax(geometry.beamBounds());
    }

    private static AABB beamBounds(MiningLaserBlockEntity laser, RenderAngles angles) {
        Vec3 beam = angles.beam();
        if (beam.lengthSqr() < 1.0E-5D) {
            return RenderBounds.localAnalytic(laser.getBlockPos(), 0.5D, -1.0D, 0.5D, 0.5D, -1.0D, 0.5D);
        }

        Vec3 axisY = beam.normalize();
        Vec3 axisX = new Vec3(axisY.z, 0.0D, -axisY.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            axisX = axisX.normalize();
        }
        Vec3 axisZ = axisX.cross(axisY).normalize();
        Vec3 normal = angles.normal();
        int range = Math.max(1, (int) Math.ceil(beam.length() * 0.5D));
        int segments = range * 2;
        double segmentLength = beam.length() / segments;
        Vec3 end = axisY.scale(segmentLength * segments);
        double marginX = transverseMargin(axisX.x, axisZ.x);
        double marginY = transverseMargin(axisX.y, axisZ.y);
        double marginZ = transverseMargin(axisX.z, axisZ.z);
        double errorX = beamArithmeticError(end.x, axisX.x, axisZ.x);
        double errorY = beamArithmeticError(end.y, axisX.y, axisZ.y);
        double errorZ = beamArithmeticError(end.z, axisX.z, axisZ.z);

        /* Match renderBeam's two float PoseStack translations exactly.  The
         * local limits are rounded outwards before the same JOML matrix is
         * applied, so the returned corners contain every emitted float. */
        PoseStack beamPose = new PoseStack();
        beamPose.translate(0.5D, -1.0D, 0.5D);
        beamPose.translate(normal.x, normal.y - 1.0D, normal.z);
        return RenderBounds.transformedBox(laser.getBlockPos(), beamPose,
                outwardFloatMin(Math.min(0.0D, end.x) - marginX - errorX),
                outwardFloatMin(Math.min(0.0D, end.y) - marginY - errorY),
                outwardFloatMin(Math.min(0.0D, end.z) - marginZ - errorZ),
                outwardFloatMax(Math.max(0.0D, end.x) + marginX + errorX),
                outwardFloatMax(Math.max(0.0D, end.y) + marginY + errorY),
                outwardFloatMax(Math.max(0.0D, end.z) + marginZ + errorZ));
    }

    private static double transverseMargin(double axisX, double axisZ) {
        // Exact full-phase projection of the radius-.075 spiral plus each
        // sign combination of the .025 x .025 beam-segment corner offsets.
        return BEAM_SPIRAL_RADIUS * Math.hypot(axisX, axisZ)
                + BEAM_HALF_THICKNESS * (Math.abs(axisX) + Math.abs(axisZ));
    }

    private static double beamArithmeticError(double longitudinal, double axisX, double axisZ) {
        /* A generated coordinate has at most sixteen dependent double
         * operations after the already-computed orthonormal axes: the line
         * product, two spiral products and sum, two thickness products, and
         * the vertex sums. Higham's gamma(16) bounds their accumulated
         * roundoff using the sum of absolute term magnitudes. */
        double unitRoundoff = 0x1.0p-53;
        double gamma16 = 16.0D * unitRoundoff / (1.0D - 16.0D * unitRoundoff);
        double magnitude = Math.abs(longitudinal)
                + (BEAM_SPIRAL_RADIUS + BEAM_HALF_THICKNESS)
                * (Math.abs(axisX) + Math.abs(axisZ));
        return Math.nextUp(magnitude * gamma16);
    }

    private static float outwardFloatMin(double value) {
        return Math.nextDown((float) value);
    }

    private static float outwardFloatMax(double value) {
        return Math.nextUp((float) value);
    }

    private static float floatPoseSweepExtent(double exactFaceRadius) {
        /* A vertex coordinate passes through fewer than 128 dependent float
         * operations across the three AxisAngle/quaternion constructions,
         * PoseStack matrix compositions, pivot translations, and final JOML
         * transformPosition. gamma(128), applied to the maximum radius plus
         * the two units of pivot/outer translation magnitude, therefore
         * bounds all float roundoff, including the one-ulp trig results. */
        double unitRoundoff = 0x1.0p-24;
        double gamma128 = 128.0D * unitRoundoff / (1.0D - 128.0D * unitRoundoff);
        double radius = Math.nextUp(exactFaceRadius);
        return Math.nextUp((float) (radius + (radius + 2.0D) * gamma128));
    }

    private static void renderParts(BlockState state, CachedBeamGeometry geometry, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(geometry.yawRotation());
        STATIC_MESHES.render(PIVOT, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(geometry.yawRotation());
        poseStack.translate(0.0D, -1.0D, 0.0D);
        poseStack.mulPose(geometry.pitchRotation());
        poseStack.translate(0.0D, 1.0D, 0.0D);
        STATIC_MESHES.render(LASER, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static RenderAngles angles(net.minecraft.core.BlockPos corePos, Vec3 target) {
        double vx = target.x - corePos.getX();
        double vy = target.y - corePos.getY() + 3.0D;
        double vz = target.z - corePos.getZ();
        Vec3 normal = new Vec3(vx, vy, vz).normalize().scale(1.5D);
        Vec3 beam = new Vec3(vx - normal.x, vy - normal.y, vz - normal.z);
        double yaw = Math.toDegrees(Math.atan2(beam.x, beam.z));
        double horizontal = Math.sqrt(beam.x * beam.x + beam.z * beam.z);
        double pitch = Math.toDegrees(Math.atan2(beam.y, horizontal));
        return new RenderAngles(yaw, pitch, beam, normal);
    }

    /** RenderLaserMiner applies no metadata rotation; aiming stays in world coordinates. */
    private static void applyMiningLaserRootPose(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case NORTH -> { }
            case EAST -> { }
            case SOUTH -> { }
            case WEST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Mining laser facing must be horizontal: " + facing);
        }
    }

    private static Direction miningLaserFacing(BlockState state) {
        if (!state.hasProperty(MiningLaserBlock.FACING)) {
            throw new IllegalStateException("Mining laser state is missing its owned facing property: " + state);
        }
        return state.getValue(MiningLaserBlock.FACING);
    }

    private static void renderBeam(CachedBeamGeometry geometry, PoseStack poseStack, MultiBufferSource bufferSource) {
        RenderAngles angles = geometry.angles();
        Vec3 beam = angles.beam();
        Vec3 normal = angles.normal();
        if (beam.lengthSqr() < 1.0E-5D) {
            return;
        }

        poseStack.translate(normal.x, normal.y - 1.0D, normal.z);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        float[] vertices = geometry.vertices();
        for (int offset = 0; offset < vertices.length; offset += 3) {
            consumer.addVertex(pose, vertices[offset], vertices[offset + 1], vertices[offset + 2])
                    .setColor(0xA0, 0x00, 0x00, 255)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }

    private static float[] buildBeamVertices(RenderAngles angles, int phase) {
        Vec3 beam = angles.beam();
        if (beam.lengthSqr() < 1.0E-5D) {
            return new float[0];
        }
        int range = Math.max(1, (int) Math.ceil(beam.length() * 0.5D));
        int segments = range * 2;
        FloatVertexBuilder output = new FloatVertexBuilder(3 * segments * 16 * 3);
        appendSpiralBeam(output, beam.x, beam.y, beam.z, phase, segments, 0.075D, 3, 0.025D);
        appendSpiralBeam(output, beam.x, beam.y, beam.z, phase + 120, segments, 0.075D, 3, 0.025D);
        appendSpiralBeam(output, beam.x, beam.y, beam.z, phase + 240, segments, 0.075D, 3, 0.025D);
        return output.toArray();
    }

    private static void appendSpiralBeam(FloatVertexBuilder output,
                                         double skeletonX, double skeletonY, double skeletonZ,
                                         int phase, int segments, double size, int layers, double thickness) {
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
        double segmentLength = length / Math.max(1, segments);
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        long randomState = (phase ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(phase + 360.0D * layers * index / Math.max(1, segments));
            randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
            long high = randomState >>> 22;
            randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
            angle += ((high << 27) + (randomState >>> 21)) * 0x1.0p-53 * 0.035D;
            double radialX = Math.cos(angle) * size;
            double radialZ = Math.sin(angle) * size;
            double distance = segmentLength * index;
            double pointX = axisYX * distance + axisXX * radialX + axisZX * radialZ;
            double pointY = axisYY * distance + axisXY * radialX + axisZY * radialZ;
            double pointZ = axisYZ * distance + axisXZ * radialX + axisZZ * radialZ;
            if (index > 0) {
                appendBeamSegment(output,
                        previousX, previousY, previousZ,
                        pointX, pointY, pointZ,
                        axisXX, axisXY, axisXZ,
                        axisZX, axisZY, axisZZ,
                        thickness);
            }
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
    }

    private static void appendBeamSegment(FloatVertexBuilder output,
                                          double previousX, double previousY, double previousZ,
                                          double pointX, double pointY, double pointZ,
                                          double axisXX, double axisXY, double axisXZ,
                                          double axisZX, double axisZY, double axisZZ,
                                          double thickness) {
        double xX = axisXX * thickness;
        double xY = axisXY * thickness;
        double xZ = axisXZ * thickness;
        double zX = axisZX * thickness;
        double zY = axisZY * thickness;
        double zZ = axisZZ * thickness;
        appendBeamQuad(output,
                previousX + xX + zX, previousY + xY + zY, previousZ + xZ + zZ,
                previousX + xX - zX, previousY + xY - zY, previousZ + xZ - zZ,
                pointX + xX - zX, pointY + xY - zY, pointZ + xZ - zZ,
                pointX + xX + zX, pointY + xY + zY, pointZ + xZ + zZ);
        appendBeamQuad(output,
                previousX - xX + zX, previousY - xY + zY, previousZ - xZ + zZ,
                previousX - xX - zX, previousY - xY - zY, previousZ - xZ - zZ,
                pointX - xX - zX, pointY - xY - zY, pointZ - xZ - zZ,
                pointX - xX + zX, pointY - xY + zY, pointZ - xZ + zZ);
        appendBeamQuad(output,
                previousX + xX + zX, previousY + xY + zY, previousZ + xZ + zZ,
                previousX - xX + zX, previousY - xY + zY, previousZ - xZ + zZ,
                pointX - xX + zX, pointY - xY + zY, pointZ - xZ + zZ,
                pointX + xX + zX, pointY + xY + zY, pointZ + xZ + zZ);
        appendBeamQuad(output,
                previousX + xX - zX, previousY + xY - zY, previousZ + xZ - zZ,
                previousX - xX - zX, previousY - xY - zY, previousZ - xZ - zZ,
                pointX - xX - zX, pointY - xY - zY, pointZ - xZ - zZ,
                pointX + xX - zX, pointY + xY - zY, pointZ + xZ - zZ);
    }

    private static void appendBeamQuad(FloatVertexBuilder output,
                                       double ax, double ay, double az,
                                       double bx, double by, double bz,
                                       double cx, double cy, double cz,
                                       double dx, double dy, double dz) {
        output.add(ax, ay, az);
        output.add(bx, by, bz);
        output.add(cx, cy, cz);
        output.add(dx, dy, dz);
    }

    private synchronized CachedBeamGeometry geometryFor(
            MiningLaserBlockEntity laser, net.minecraft.core.BlockPos target,
            int phase, boolean requireVertices) {
        CachedBeamGeometry previous = this.beamGeometry.get(laser);
        if (previous == null || !previous.target().equals(target)) {
            RenderAngles angles = angles(laser.getBlockPos(), Vec3.atLowerCornerOf(target));
            previous = new CachedBeamGeometry(
                    target.immutable(), angles, beamBounds(laser, angles),
                    miningLaserAimYaw((float) angles.yaw()),
                    Axis.XN.rotationDegrees((float) (angles.pitch() + 90.0D)),
                    Integer.MIN_VALUE, new float[0]);
        }
        if (requireVertices && previous.phase() != phase) {
            previous = new CachedBeamGeometry(
                    previous.target(), previous.angles(), previous.beamBounds(),
                    previous.yawRotation(), previous.pitchRotation(),
                    phase, buildBeamVertices(previous.angles(), phase));
        }
        this.beamGeometry.put(laser, previous);
        return previous;
    }

    private static Quaternionf miningLaserAimYaw(float degrees) {
        return Axis.YP.rotationDegrees(Mth.wrapDegrees(degrees));
    }

    record RenderAngles(double yaw, double pitch, Vec3 beam, Vec3 normal) {
    }

    private record CachedBeamGeometry(net.minecraft.core.BlockPos target, RenderAngles angles,
                                      AABB beamBounds, Quaternionf yawRotation, Quaternionf pitchRotation,
                                      int phase, float[] vertices) {
    }

    private static final class FloatVertexBuilder {
        private float[] values;
        private int size;

        private FloatVertexBuilder(int expectedFloats) {
            this.values = new float[Math.max(3, expectedFloats)];
        }

        private void add(double x, double y, double z) {
            ensureCapacity(this.size + 3);
            this.values[this.size++] = (float) x;
            this.values[this.size++] = (float) y;
            this.values[this.size++] = (float) z;
        }

        private void ensureCapacity(int required) {
            if (required > this.values.length) {
                this.values = Arrays.copyOf(this.values, Math.max(required, this.values.length * 2));
            }
        }

        private float[] toArray() {
            return Arrays.copyOf(this.values, this.size);
        }
    }
}
