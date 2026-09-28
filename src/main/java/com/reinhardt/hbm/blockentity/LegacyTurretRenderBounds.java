package com.reinhardt.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Common-side render-envelope calculation for legacy turrets.
 *
 * <p>The static envelopes are extrema of the face-referenced OBJ vertices under
 * every legal pitch, full yaw, recoil and authored spin animation. Active beam
 * envelopes are evaluated over the actual previous-to-current tick angle path.
 * The latter uses outward-rounded interval arithmetic and branch-and-bound, so
 * no render-thread world lookup or endpoint-only/arc-length approximation is
 * involved.</p>
 */
final class LegacyTurretRenderBounds {
    private static final double TWO_PI = Math.PI * 2.0D;
    private static final double MAXWELL_SPIRAL_RADIUS = 0.375D;
    private static final double MAXWELL_SECTION_HALF_SIZE = 0.05D;
    private static final double TAUON_LINE_RADIUS = 0.1D;
    private static final int MAX_INTERVAL_SPLITS = 16_384;

    private LegacyTurretRenderBounds() {
    }

    static AABB calculate(BlockPos pos, LegacyTurretType type, Direction facing,
                          double previousYaw, double yaw, double previousPitch, double pitch,
                          int beamTicks, double beamDistance) {
        return switch (type) {
            case FRIENDLY -> friendlyBounds(pos, facing);
            case FRITZ -> fritzBounds(pos, facing);
            case HOWARD -> howardBounds(pos, facing);
            case HOWARD_DAMAGED -> howardDamagedBounds(pos, facing);
            case MAXWELL -> maxwellBounds(pos, facing, previousYaw, yaw, previousPitch, pitch, beamTicks, beamDistance);
            case RICHARD -> richardBounds(pos, facing);
            case TAUON -> tauonBounds(pos, facing, previousYaw, yaw, previousPitch, pitch, beamTicks, beamDistance);
            case ARTY -> artyBounds(pos, facing);
            case HIMARS -> himarsBounds(pos, facing);
            case SENTRY -> sentryBounds(pos, facing);
            case SENTRY_DAMAGED -> sentryDamagedBounds(pos, facing);
        };
    }

    private static AABB friendlyBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.friendlyWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 3.2655876504571117D, pos.getY() - 0.40099280264800369D,
                originZ - 3.2655876504571117D, originX + 3.2655876504571117D,
                pos.getY() + 4.0234442185790353D, originZ + 3.2655876504571117D);
    }

    private static AABB fritzBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.fritzWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 2.1895080069504198D, pos.getY() - 0.13518443149389131D,
                originZ - 2.1895080069504198D, originX + 2.1895080069504198D,
                pos.getY() + 3.1089818826102427D, originZ + 2.1895080069504198D);
    }

    private static AABB howardBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.howardWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 3.2076949418011944D, pos.getY() - 0.42276863766968875D,
                originZ - 3.2076949418011944D, originX + 3.2076949418011944D,
                pos.getY() + 5.4576949418011944D, originZ + 3.2076949418011944D);
    }

    private static AABB howardDamagedBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.howardDamagedWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 3.2103484555051027D, pos.getY() - 0.58080814427723049D,
                originZ - 3.2103484555051027D, originX + 3.2103484555051027D,
                pos.getY() + 5.4603484555051036D, originZ + 3.2103484555051027D);
    }

    private static AABB maxwellBounds(BlockPos pos, Direction facing,
                                      double previousYaw, double yaw,
                                      double previousPitch, double pitch,
                                      int beamTicks, double beamDistance) {
        Vec3 offset = LegacyTurretBlockEntity.maxwellWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        AABB bounds = new AABB(originX - 2.6983793284117783D, pos.getY() - 0.0625D,
                originZ - 2.6983793284117783D, originX + 2.6983793284117783D,
                pos.getY() + 3.800368568214036D, originZ + 2.6983793284117783D);
        if (beamTicks <= 0 || beamDistance <= 0.0D) {
            return bounds;
        }
        return bounds.minmax(maxwellBeamBounds(pos, facing, previousYaw, yaw,
                previousPitch, pitch, beamDistance));
    }

    private static AABB richardBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.richardWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 1.3437265479486353D, pos.getY() - 0.0625D,
                originZ - 1.3437265479486353D, originX + 1.3437265479486353D,
                pos.getY() + 2.5116573160251958D, originZ + 1.3437265479486353D);
    }

    private static AABB tauonBounds(BlockPos pos, Direction facing,
                                    double previousYaw, double yaw,
                                    double previousPitch, double pitch,
                                    int beamTicks, double beamDistance) {
        Vec3 offset = LegacyTurretBlockEntity.tauonWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        AABB bounds = new AABB(originX - 1.9385078139200267D, pos.getY() - 0.0625D,
                originZ - 1.9385078139200267D, originX + 1.9385078139200267D,
                pos.getY() + 2.7546453278464282D, originZ + 1.9385078139200267D);
        if (beamTicks <= 0 || beamDistance <= 0.0D) {
            return bounds;
        }
        return bounds.minmax(tauonBeamBounds(pos, facing, previousYaw, yaw,
                previousPitch, pitch, beamDistance));
    }

    private static AABB artyBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.artyWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 8.380829D, pos.getY() - 1.458134D,
                originZ - 8.380829D, originX + 8.380829D,
                pos.getY() + 11.380829D, originZ + 8.380829D);
    }

    private static AABB himarsBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.himarsWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 8.806958D, pos.getY(),
                originZ - 8.806958D, originX + 8.806958D,
                pos.getY() + 11.354001D, originZ + 8.806958D);
    }

    private static AABB sentryBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.sentryWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 1.2952516046205078D, pos.getY(),
                originZ - 1.2952516046205078D, originX + 1.2952516046205078D,
                pos.getY() + 2.0724467365275903D, originZ + 1.2952516046205078D);
    }

    private static AABB sentryDamagedBounds(BlockPos pos, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.sentryDamagedWorldOrigin(facing);
        double originX = pos.getX() + offset.x;
        double originZ = pos.getZ() + offset.z;
        return new AABB(originX - 1.2952516046205078D, pos.getY(),
                originZ - 1.2952516046205078D, originX + 1.2952516046205078D,
                pos.getY() + 2.0724467365275903D, originZ + 1.2952516046205078D);
    }

    private static AABB maxwellBeamBounds(BlockPos pos, Direction facing,
                                          double previousYaw, double yaw,
                                          double previousPitch, double pitch,
                                          double beamDistance) {
        Vec3 offset = LegacyTurretBlockEntity.maxwellWorldOrigin(facing);
        BeamPath path = new BeamPath(
                pos.getX() + offset.x,
                pos.getY(),
                pos.getZ() + offset.z,
                previousYaw,
                yaw,
                previousPitch,
                pitch,
                LegacyTurretType.MAXWELL.barrelLength(),
                beamDistance,
                2.0D,
                MAXWELL_SPIRAL_RADIUS,
                MAXWELL_SECTION_HALF_SIZE
        );
        return beamPathBounds(path);
    }

    private static AABB tauonBeamBounds(BlockPos pos, Direction facing,
                                        double previousYaw, double yaw,
                                        double previousPitch, double pitch,
                                        double beamDistance) {
        Vec3 offset = LegacyTurretBlockEntity.tauonWorldOrigin(facing);
        BeamPath path = new BeamPath(
                pos.getX() + offset.x,
                pos.getY(),
                pos.getZ() + offset.z,
                previousYaw,
                yaw,
                previousPitch,
                pitch,
                0.0D,
                beamDistance,
                1.5D,
                TAUON_LINE_RADIUS,
                0.0D
        );
        return beamPathBounds(path);
    }

    private static AABB beamPathBounds(BeamPath path) {
        return new AABB(
                extreme(path, Axis.X, false),
                extreme(path, Axis.Y, false),
                extreme(path, Axis.Z, false),
                extreme(path, Axis.X, true),
                extreme(path, Axis.Y, true),
                extreme(path, Axis.Z, true)
        );
    }

    /**
     * Certified interval branch-and-bound over the one-dimensional partial-tick
     * domain. Point evaluations only improve the incumbent; interval bounds are
     * what prove that every discarded interval cannot contain a tighter extreme.
     */
    private static double extreme(BeamPath path, Axis axis, boolean maximum) {
        Comparator<Node> order = maximum
                ? Comparator.comparingDouble((Node node) -> node.range.max).reversed()
                : Comparator.comparingDouble(node -> node.range.min);
        PriorityQueue<Node> pending = new PriorityQueue<>(order);
        Node root = node(path, axis, 0.0D, 1.0D);
        pending.add(root);
        double incumbent = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        incumbent = improve(incumbent, pointRange(path, axis, 0.0D), maximum);
        incumbent = improve(incumbent, pointRange(path, axis, 0.5D), maximum);
        incumbent = improve(incumbent, pointRange(path, axis, 1.0D), maximum);

        int splits = 0;
        while (!pending.isEmpty()) {
            Node best = pending.peek();
            double certified = maximum ? best.range.max : best.range.min;
            double tolerance = Math.max(1.0E-12D, Math.ulp(incumbent) * 32.0D);
            if (maximum ? certified <= incumbent + tolerance : certified >= incumbent - tolerance) {
                return certified;
            }
            if (splits++ >= MAX_INTERVAL_SPLITS) {
                // Still return the certified interval endpoint, never an
                // unproven point estimate, if a pathological path exhausts the cap.
                return certified;
            }
            pending.remove();
            double middle = best.lo + (best.hi - best.lo) * 0.5D;
            if (middle <= best.lo || middle >= best.hi) {
                return certified;
            }
            Range middlePoint = pointRange(path, axis, middle);
            incumbent = improve(incumbent, middlePoint, maximum);
            Node left = node(path, axis, best.lo, middle);
            Node right = node(path, axis, middle, best.hi);
            if (canImprove(left.range, incumbent, maximum)) {
                pending.add(left);
            }
            if (canImprove(right.range, incumbent, maximum)) {
                pending.add(right);
            }
        }
        return incumbent;
    }

    private static Node node(BeamPath path, Axis axis, double lo, double hi) {
        return new Node(lo, hi, intervalRange(path, axis, lo, hi));
    }

    private static boolean canImprove(Range range, double incumbent, boolean maximum) {
        return maximum ? range.max > incumbent : range.min < incumbent;
    }

    private static double improve(double incumbent, Range point, boolean maximum) {
        return maximum ? Math.max(incumbent, point.max) : Math.min(incumbent, point.min);
    }

    private static Range pointRange(BeamPath path, Axis axis, double partialTick) {
        return rendererFloatRange(path, axis, partialTick, partialTick);
    }

    private static Range intervalRange(BeamPath path, Axis axis, double lo, double hi) {
        return rendererFloatRange(path, axis, lo, hi);
    }

    /**
     * Encloses the actual renderer pipeline, not an ideal double rotation.
     * The renderer converts interpolated radians to float degrees and back to
     * float radians, constructs JOML 1.10.5 axis-angle quaternions, composes
     * float matrices, casts beam vertices to float, and finally transforms
     * them through that matrix.  Every operation below follows that ordering;
     * {@code f*} operations round their exact interval outward to the adjacent
     * IEEE-754 binary32 values.
     */
    private static Range rendererFloatRange(BeamPath path, Axis axis, double lo, double hi) {
        Range yawAngle = rendererAngle(path.previousYaw, path.yaw, lo, hi, true);
        Range pitchAngle = rendererAngle(path.previousPitch, path.pitch, lo, hi, false);

        FloatMatrix matrix = FloatMatrix.translation(
                path.originX - Math.floor(path.originX),
                0.0D,
                path.originZ - Math.floor(path.originZ)
        ).rotate(FloatRotation.axisY(yawAngle));
        matrix = matrix.translate(0.0D, 1.5D, 0.0D)
                .rotate(FloatRotation.axisZ(pitchAngle))
                .translate(0.0D, -1.5D, 0.0D)
                .translate(path.startX, path.localY, 0.0D);

        double localLength = path.endX - path.startX;
        double absLength = Math.abs(localLength);
        double xError = Math.nextUp(2.0D * Math.ulp(absLength)
                + Math.ulp((float) absLength));
        Range localX = new Range(Math.nextDown(Math.min(0.0D, localLength) - xError),
                Math.nextUp(Math.max(0.0D, localLength) + xError)).floatCast();

        Range mx = matrix.column(axis, 0);
        Range my = matrix.column(axis, 1);
        Range mz = matrix.column(axis, 2);
        Range translation = matrix.translation(axis);
        Range start = translation;
        Range end = FloatMatrix.ffma(mx, localX, translation);
        Range center = new Range(Math.min(start.min, end.min), Math.max(start.max, end.max));

        double localError = localBeamComponentError(path.circleRadius, path.squareHalfSize);
        Range radial = my.square().add(mz.square()).sqrt().scale(path.circleRadius);
        Range square = my.abs().add(mz.abs()).scale(path.squareHalfSize);
        Range castAndConstruction = my.abs().add(mz.abs()).scale(localError);
        double support = radial.max + square.max + castAndConstruction.max;

        double operationMagnitude = Math.max(Math.abs(center.min), Math.abs(center.max)) + support;
        // Vector3f.mulPosition uses three nested float FMAs.  Each FMA rounds
        // once; three full ulps at the proven intermediate magnitude strictly
        // dominate their three <= 0.5 ulp errors, including subnormal edges.
        double transformRounding = 3.0D * Math.ulp((float) operationMagnitude);
        return new Range(
                Math.nextDown(path.worldOffset(axis) + center.min - support - transformRounding),
                Math.nextUp(path.worldOffset(axis) + center.max + support + transformRounding)
        );
    }

    private static Range rendererAngle(double previous, double current, double lo, double hi, boolean yaw) {
        Range radians = Range.linear(previous, current, lo, hi);
        Range degrees = radians.scale(180.0D / Math.PI);
        if (yaw) {
            degrees = degrees.negate().add(Range.point(-90.0D));
        }
        // Mirrors `(float)Math.toDegrees(...)` followed by
        // `(float)Math.toRadians(degrees)` in the renderer helper.
        return degrees.floatCast().scale(Math.PI / 180.0D).floatCast();
    }

    private static double localBeamComponentError(double radius, double squareHalfSize) {
        double maximum = radius + squareHalfSize;
        // StrictMath/Math trigonometric results are within one double ulp.
        // Beam angle construction contributes at most four ulps over its
        // bounded [0, 4*pi] phase; the following double multiply/add and final
        // float cast each contribute at most the full ulp used below.
        double angleError = 4.0D * Math.ulp(4.0D * Math.PI);
        return Math.nextUp(radius * (Math.scalb(1.0D, -52) + angleError)
                + Math.ulp(radius) + Math.ulp(maximum) + Math.ulp((float) maximum));
    }

    private static double lerp(double start, double end, double value) {
        return start + (end - start) * value;
    }

    private enum Axis { X, Y, Z }

    private record BeamPath(double originX, double originY, double originZ,
                            double previousYaw, double yaw,
                            double previousPitch, double pitch,
                            double startX, double endX, double localY,
                            double circleRadius, double squareHalfSize) {
        double worldOffset(Axis axis) {
            return switch (axis) {
                case X -> Math.floor(originX);
                case Y -> originY;
                case Z -> Math.floor(originZ);
            };
        }
    }

    private record Node(double lo, double hi, Range range) {
    }

    /** Exact JOML axis-angle quaternion-to-matrix operation ordering. */
    private record FloatRotation(Range m00, Range m01, Range m02,
                                 Range m10, Range m11, Range m12,
                                 Range m20, Range m21, Range m22) {
        private static final float PI_F = (float) Math.PI;
        private static final float PI2_F = PI_F * 2.0F;
        private static final float PI_HALF_F = (float) (Math.PI * 0.5D);

        static FloatRotation axisY(Range angle) {
            Range[] quaternion = quaternion(angle, 1);
            return fromQuaternion(quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
        }

        static FloatRotation axisZ(Range angle) {
            Range[] quaternion = quaternion(angle, 2);
            return fromQuaternion(quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
        }

        private static Range[] quaternion(Range angle, int axis) {
            Range half = angle.fmul(Range.point(0.5F));
            Range sin = Range.sin(half).floatCast();
            Range cosMagnitude = Range.point(1.0F).fsub(sin.fmul(sin))
                    .clampMinimum(0.0D).sqrt().floatCast();
            Range cos = signedCosFromSinMagnitude(half, cosMagnitude);
            Range zero = Range.point(0.0F);
            return switch (axis) {
                case 1 -> new Range[]{zero, sin, zero, cos};
                case 2 -> new Range[]{zero, zero, sin, cos};
                default -> throw new IllegalArgumentException("Unsupported quaternion axis " + axis);
            };
        }

        /** Mirrors JOML Math.cosFromSinInternal's float phase/sign branch. */
        private static Range signedCosFromSinMagnitude(Range half, Range magnitude) {
            Range a = half.fadd(Range.point(PI_HALF_F));
            Range quotient = a.fdiv(Range.point(PI2_F));
            int minQ = truncateToInt(quotient.min);
            int maxQ = truncateToInt(quotient.max);
            if (minQ > maxQ) {
                int swap = minQ;
                minQ = maxQ;
                maxQ = swap;
            }
            // Legal turret angles span only a few turns.  Keep this bounded in
            // corrupt-data cases without claiming a sign that was not proved.
            if ((long) maxQ - minQ > 32L) {
                return new Range(-magnitude.max, magnitude.max);
            }
            boolean positive = false;
            boolean negative = false;
            for (int q = minQ; q <= maxQ; q++) {
                Range product = Range.point((float) q).fmul(Range.point(PI2_F));
                Range b = a.fsub(product);
                if (b.min < 0.0D) {
                    Range adjusted = b.intersect(Double.NEGATIVE_INFINITY, Math.nextDown(0.0D))
                            .fadd(Range.point(PI2_F));
                    if (adjusted.min < PI_F) positive = true;
                    if (adjusted.max >= PI_F) negative = true;
                }
                if (b.max >= 0.0D) {
                    Range nonNegative = b.intersect(0.0D, Double.POSITIVE_INFINITY);
                    if (nonNegative.min < PI_F) positive = true;
                    if (nonNegative.max >= PI_F) negative = true;
                }
            }
            if (positive && negative) return new Range(-magnitude.max, magnitude.max);
            if (negative) return magnitude.negate();
            return magnitude;
        }

        private static int truncateToInt(double value) {
            if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return (int) value;
        }

        private static FloatRotation fromQuaternion(Range x, Range y, Range z, Range w) {
            Range w2 = w.fmul(w), x2 = x.fmul(x), y2 = y.fmul(y), z2 = z.fmul(z);
            Range zw = z.fmul(w), dzw = zw.fadd(zw);
            Range xy = x.fmul(y), dxy = xy.fadd(xy);
            Range xz = x.fmul(z), dxz = xz.fadd(xz);
            Range yw = y.fmul(w), dyw = yw.fadd(yw);
            Range yz = y.fmul(z), dyz = yz.fadd(yz);
            Range xw = x.fmul(w), dxw = xw.fadd(xw);
            return new FloatRotation(
                    w2.fadd(x2).fsub(z2).fsub(y2),
                    dxy.fadd(dzw),
                    dxz.fsub(dyw),
                    dxy.fsub(dzw),
                    y2.fsub(z2).fadd(w2).fsub(x2),
                    dyz.fadd(dxw),
                    dyw.fadd(dxz),
                    dyz.fsub(dxw),
                    z2.fsub(y2).fsub(x2).fadd(w2)
            );
        }
    }

    /** Affine float matrix in JOML's column-major field naming. */
    private record FloatMatrix(Range m00, Range m01, Range m02,
                               Range m10, Range m11, Range m12,
                               Range m20, Range m21, Range m22,
                               Range m30, Range m31, Range m32) {
        static FloatMatrix translation(double x, double y, double z) {
            Range zero = Range.point(0.0F);
            Range one = Range.point(1.0F);
            return new FloatMatrix(one, zero, zero, zero, one, zero, zero, zero, one,
                    Range.point(x).floatCast(), Range.point(y).floatCast(), Range.point(z).floatCast());
        }

        FloatMatrix rotate(FloatRotation r) {
            return new FloatMatrix(
                    dot(m00, r.m00, m10, r.m01, m20, r.m02),
                    dot(m01, r.m00, m11, r.m01, m21, r.m02),
                    dot(m02, r.m00, m12, r.m01, m22, r.m02),
                    dot(m00, r.m10, m10, r.m11, m20, r.m12),
                    dot(m01, r.m10, m11, r.m11, m21, r.m12),
                    dot(m02, r.m10, m12, r.m11, m22, r.m12),
                    dot(m00, r.m20, m10, r.m21, m20, r.m22),
                    dot(m01, r.m20, m11, r.m21, m21, r.m22),
                    dot(m02, r.m20, m12, r.m21, m22, r.m22),
                    m30, m31, m32
            );
        }

        FloatMatrix translate(double x, double y, double z) {
            Range rx = Range.point(x).floatCast();
            Range ry = Range.point(y).floatCast();
            Range rz = Range.point(z).floatCast();
            return new FloatMatrix(m00, m01, m02, m10, m11, m12, m20, m21, m22,
                    ffma(m00, rx, ffma(m10, ry, ffma(m20, rz, m30))),
                    ffma(m01, rx, ffma(m11, ry, ffma(m21, rz, m31))),
                    ffma(m02, rx, ffma(m12, ry, ffma(m22, rz, m32))));
        }

        Range column(Axis axis, int column) {
            return switch (axis) {
                case X -> switch (column) { case 0 -> m00; case 1 -> m10; default -> m20; };
                case Y -> switch (column) { case 0 -> m01; case 1 -> m11; default -> m21; };
                case Z -> switch (column) { case 0 -> m02; case 1 -> m12; default -> m22; };
            };
        }

        Range translation(Axis axis) {
            return switch (axis) { case X -> m30; case Y -> m31; case Z -> m32; };
        }

        private static Range dot(Range a0, Range b0, Range a1, Range b1, Range a2, Range b2) {
            return a0.fmul(b0).fadd(a1.fmul(b1)).fadd(a2.fmul(b2));
        }

        static Range ffma(Range a, Range b, Range c) {
            // A fused multiply-add has one final binary32 rounding.  The exact
            // real product-plus-sum interval therefore encloses it after one
            // outward float cast.
            return a.multiply(b).add(c).floatCast();
        }
    }

    /** Minimal outward-rounded interval arithmetic used by the beam solver. */
    private record Range(double min, double max) {
        private Range {
            if (min > max) {
                throw new IllegalArgumentException("Invalid interval: " + min + " > " + max);
            }
        }

        static Range point(double value) {
            return new Range(value, value);
        }

        Range floatCast() {
            return new Range(floatDown(min), floatUp(max));
        }

        Range fadd(Range other) {
            return add(other).floatCast();
        }

        Range fsub(Range other) {
            return subtract(other).floatCast();
        }

        Range fmul(Range other) {
            return multiply(other).floatCast();
        }

        Range fdiv(Range other) {
            if (other.min <= 0.0D && other.max >= 0.0D) {
                return new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
            double a = min / other.min;
            double b = min / other.max;
            double c = max / other.min;
            double d = max / other.max;
            return new Range(Math.nextDown(Math.min(Math.min(a, b), Math.min(c, d))),
                    Math.nextUp(Math.max(Math.max(a, b), Math.max(c, d)))).floatCast();
        }

        Range clampMinimum(double lower) {
            return new Range(Math.max(lower, min), Math.max(lower, max));
        }

        Range intersect(double lower, double upper) {
            double clippedMin = Math.max(min, lower);
            double clippedMax = Math.min(max, upper);
            if (clippedMin > clippedMax) {
                return Range.point(clippedMin);
            }
            return new Range(clippedMin, clippedMax);
        }

        static Range linear(double start, double end, double lo, double hi) {
            double a = lerp(start, end, lo);
            double b = lerp(start, end, hi);
            return new Range(Math.nextDown(Math.min(a, b)), Math.nextUp(Math.max(a, b)));
        }

        Range add(Range other) {
            return new Range(Math.nextDown(this.min + other.min), Math.nextUp(this.max + other.max));
        }

        Range subtract(Range other) {
            return new Range(Math.nextDown(this.min - other.max), Math.nextUp(this.max - other.min));
        }

        Range negate() {
            return new Range(-this.max, -this.min);
        }

        Range multiply(Range other) {
            double a = this.min * other.min;
            double b = this.min * other.max;
            double c = this.max * other.min;
            double d = this.max * other.max;
            return new Range(Math.nextDown(Math.min(Math.min(a, b), Math.min(c, d))),
                    Math.nextUp(Math.max(Math.max(a, b), Math.max(c, d))));
        }

        Range scale(double scalar) {
            if (scalar >= 0.0D) {
                return new Range(Math.nextDown(this.min * scalar), Math.nextUp(this.max * scalar));
            }
            return new Range(Math.nextDown(this.max * scalar), Math.nextUp(this.min * scalar));
        }

        Range square() {
            if (this.min <= 0.0D && this.max >= 0.0D) {
                return new Range(0.0D, Math.nextUp(Math.max(this.min * this.min, this.max * this.max)));
            }
            double a = this.min * this.min;
            double b = this.max * this.max;
            return new Range(Math.nextDown(Math.min(a, b)), Math.nextUp(Math.max(a, b)));
        }

        Range abs() {
            if (this.min <= 0.0D && this.max >= 0.0D) {
                return new Range(0.0D, Math.nextUp(Math.max(-this.min, this.max)));
            }
            return new Range(Math.nextDown(Math.min(Math.abs(this.min), Math.abs(this.max))),
                    Math.nextUp(Math.max(Math.abs(this.min), Math.abs(this.max))));
        }

        Range sqrt() {
            return new Range(Math.max(0.0D, Math.nextDown(Math.sqrt(Math.max(0.0D, this.min)))),
                    Math.nextUp(Math.sqrt(Math.max(0.0D, this.max))));
        }

        static Range sin(Range angle) {
            return trig(angle, true);
        }

        static Range cos(Range angle) {
            return trig(angle, false);
        }

        private static Range trig(Range angle, boolean sine) {
            if (angle.max - angle.min >= TWO_PI) {
                return new Range(-1.0D, 1.0D);
            }
            double a = sine ? Math.sin(angle.min) : Math.cos(angle.min);
            double b = sine ? Math.sin(angle.max) : Math.cos(angle.max);
            double min = Math.min(a, b);
            double max = Math.max(a, b);
            double maximumPhase = sine ? Math.PI * 0.5D : 0.0D;
            double minimumPhase = sine ? -Math.PI * 0.5D : Math.PI;
            if (containsPhase(angle, maximumPhase)) {
                max = 1.0D;
            }
            if (containsPhase(angle, minimumPhase)) {
                min = -1.0D;
            }
            return new Range(Math.nextDown(min), Math.nextUp(max));
        }

        private static boolean containsPhase(Range interval, double phase) {
            double first = Math.ceil((interval.min - phase) / TWO_PI);
            double value = phase + first * TWO_PI;
            return value >= interval.min && value <= interval.max;
        }

        private static double floatDown(double value) {
            if (Double.isNaN(value)) return Double.NEGATIVE_INFINITY;
            float rounded = (float) value;
            if ((double) rounded > value) rounded = Math.nextDown(rounded);
            return rounded;
        }

        private static double floatUp(double value) {
            if (Double.isNaN(value)) return Double.POSITIVE_INFINITY;
            float rounded = (float) value;
            if ((double) rounded < value) rounded = Math.nextUp(rounded);
            return rounded;
        }
    }
}
