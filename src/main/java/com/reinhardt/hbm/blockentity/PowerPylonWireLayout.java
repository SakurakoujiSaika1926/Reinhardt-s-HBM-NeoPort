package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerPylonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Kind-owned deterministic mount pairing plus common cable-mesh generation. */
public final class PowerPylonWireLayout {
    private PowerPylonWireLayout() {
    }

    public static int[] mountMapping(PowerPylonBlockEntity first,
                                     PowerPylonBlockEntity.ConnectionRenderInfo second,
                                     int lineCount) {
        return switch (first.kind()) {
            case RED_CONNECTOR -> redConnectorMountMapping(first, second, lineCount);
            case CONNECTOR_RED_SUPER -> redSuperConnectorMountMapping(first, second, lineCount);
            case RED_PYLON -> redPylonMountMapping(first, second, lineCount);
            case RED_PYLON_STEEL -> redPylonSteelMountMapping(first, second, lineCount);
            case RED_PYLON_MEDIUM_WOOD -> mediumWoodPylonMountMapping(first, second, lineCount);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> mediumWoodTransformerMountMapping(first, second, lineCount);
            case RED_PYLON_MEDIUM_STEEL -> mediumSteelPylonMountMapping(first, second, lineCount);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> mediumSteelTransformerMountMapping(first, second, lineCount);
            case RED_PYLON_LARGE -> largePylonMountMapping(first, second, lineCount);
            case SUBSTATION -> substationMountMapping(first, second, lineCount);
        };
    }

    private static int[] redConnectorMountMapping(PowerPylonBlockEntity first,
                                                  PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                  int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapRedConnectorMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapRedConnectorIdentity(second.mountPositions(), lineCount);
    }

    private static int[] redSuperConnectorMountMapping(PowerPylonBlockEntity first,
                                                       PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                       int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapRedSuperConnectorMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapRedSuperConnectorIdentity(second.mountPositions(), lineCount);
    }

    private static int[] redPylonMountMapping(PowerPylonBlockEntity first,
                                              PowerPylonBlockEntity.ConnectionRenderInfo second,
                                              int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapRedPylonMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapRedPylonIdentity(second.mountPositions(), lineCount);
    }

    private static int[] redPylonSteelMountMapping(PowerPylonBlockEntity first,
                                                   PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                   int lineCount) {
        int[] mapping = new int[lineCount];
        Vec3[] secondMounts = second.mountPositions();
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mediumWoodPylonMountMapping(PowerPylonBlockEntity first,
                                                     PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                     int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapMediumWoodPylonMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapMediumWoodPylonIdentity(second.mountPositions(), lineCount);
    }

    private static int[] mediumWoodTransformerMountMapping(PowerPylonBlockEntity first,
                                                           PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                           int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapMediumWoodTransformerMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapMediumWoodTransformerIdentity(second.mountPositions(), lineCount);
    }

    private static int[] mediumSteelPylonMountMapping(PowerPylonBlockEntity first,
                                                      PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                      int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapMediumSteelPylonMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapMediumSteelPylonIdentity(second.mountPositions(), lineCount);
    }

    private static int[] mediumSteelTransformerMountMapping(PowerPylonBlockEntity first,
                                                            PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                            int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapMediumSteelTransformerMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapMediumSteelTransformerIdentity(second.mountPositions(), lineCount);
    }

    private static int[] largePylonMountMapping(PowerPylonBlockEntity first,
                                                PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                int lineCount) {
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.SUBSTATION) {
            return mapLargePylonMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        int[] mapping = mapLargePylonIdentity(second.mountPositions(), lineCount);
        if (lineCount == 4 && second.kind() == PowerPylonBlock.Kind.RED_PYLON_LARGE) {
            Direction a = first.facing();
            Direction b = second.facing();
            if ((a == Direction.EAST && b == Direction.NORTH)
                    || (a == Direction.NORTH && b == Direction.EAST)) {
                Vec3[] secondMounts = second.mountPositions();
                for (int i = 0; i < lineCount; i++) {
                    mapping[i] = (mapping[i] + 2) % secondMounts.length;
                }
            }
        }
        return mapping;
    }

    private static int[] substationMountMapping(PowerPylonBlockEntity first,
                                                PowerPylonBlockEntity.ConnectionRenderInfo second,
                                                int lineCount) {
        if (lineCount == 4) {
            return mapSubstationMatched(first.getBlockPos(), first.mountPositions(), second, lineCount);
        }
        return mapSubstationIdentity(second.mountPositions(), lineCount);
    }

    /**
     * Generates the exact ten-segment, two-quad cable mesh used by the renderer.
     * Keeping this geometry in common code lets the renderer and its culling
     * envelope consume the same vertices instead of maintaining approximations.
     */
    public static AABB appendSaggingWire(List<WireQuad> quads, Vec3 start, Vec3 end, boolean fullSpan) {
        int firstQuad = quads.size();
        Vec3 delta = end.subtract(start);
        double hang = Math.min(delta.length() / (fullSpan ? 30.0D : 15.0D), 2.5D);
        int segments = 10;

        for (int i = 0; i < segments; i++) {
            double t0 = (double) i / (double) segments;
            double t1 = (double) (i + 1) / (double) segments;
            double sagArc = fullSpan ? Math.PI : Math.PI * 0.5D;
            double sag0 = Math.sin(t0 * sagArc) * hang;
            double sag1 = Math.sin(t1 * sagArc) * hang;
            Vec3 p0 = start.add(delta.scale(t0)).subtract(0.0D, sag0, 0.0D);
            Vec3 p1 = start.add(delta.scale(t1)).subtract(0.0D, sag1, 0.0D);
            appendWireSegment(quads, p0, p1);
        }
        return emittedBounds(quads, firstQuad);
    }

    /** Exact AABB of every vertex emitted by {@link #appendSaggingWire}. */
    public static AABB saggingWireBounds(Vec3 start, Vec3 end, boolean fullSpan) {
        List<WireQuad> quads = new ArrayList<>(20);
        return appendSaggingWire(quads, start, end, fullSpan);
    }

    private static AABB emittedBounds(List<WireQuad> quads, int firstQuad) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int index = firstQuad; index < quads.size(); index++) {
            WireQuad quad = quads.get(index);
            for (Vec3 vertex : quad.vertices()) {
                minX = Math.min(minX, vertex.x);
                minY = Math.min(minY, vertex.y);
                minZ = Math.min(minZ, vertex.z);
                maxX = Math.max(maxX, vertex.x);
                maxY = Math.max(maxY, vertex.y);
                maxZ = Math.max(maxZ, vertex.z);
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void appendWireSegment(List<WireQuad> quads, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double length = Math.max(delta.length(), 0.001D);
        Vec3 horizontalNormal = new Vec3(-delta.z, 0.0D, delta.x);
        if (horizontalNormal.lengthSqr() < 0.0001D) {
            horizontalNormal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        horizontalNormal = horizontalNormal.normalize().scale(0.03125D);
        Vec3 verticalNormal = delta.cross(horizontalNormal).normalize().scale(0.03125D);
        int wrap = Math.max(1, Mth.ceil(length * 8.0D));

        quads.add(new WireQuad(emitted(start.add(horizontalNormal)), emitted(start.subtract(horizontalNormal)),
                emitted(end.subtract(horizontalNormal)), emitted(end.add(horizontalNormal)), wrap));
        quads.add(new WireQuad(emitted(start.add(verticalNormal)), emitted(start.subtract(verticalNormal)),
                emitted(end.subtract(verticalNormal)), emitted(end.add(verticalNormal)), wrap));
    }

    /** Matches VertexConsumer's float position payload before the block origin is applied. */
    private static Vec3 emitted(Vec3 point) {
        return new Vec3((float) point.x, (float) point.y, (float) point.z);
    }

    // Intentionally owned by the red connector Kind. Do not fold this path into another pylon Kind.
    private static int[] mapRedConnectorIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapRedConnectorMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareRedConnectorBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteRedConnectorMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteRedConnectorMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectRedConnectorBest(firstMounts, secondMounts, lineCount)
                : selectRedConnectorBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectRedConnectorBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchRedConnectorPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchRedConnectorPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreRedConnectorPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchRedConnectorPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreRedConnectorPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countRedConnectorProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countRedConnectorProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageRedConnectorMounts(firstMounts);
        Vec3 secondCenter = averageRedConnectorMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doRedConnectorProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doRedConnectorProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaRedConnector(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaRedConnector(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaRedConnector(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaRedConnector(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaRedConnector(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageRedConnectorMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteRedConnectorMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareRedConnectorBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the red super connector Kind. Do not fold this path into another pylon Kind.
    private static int[] mapRedSuperConnectorIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapRedSuperConnectorMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareRedSuperConnectorBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteRedSuperConnectorMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteRedSuperConnectorMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectRedSuperConnectorBest(firstMounts, secondMounts, lineCount)
                : selectRedSuperConnectorBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectRedSuperConnectorBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchRedSuperConnectorPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchRedSuperConnectorPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreRedSuperConnectorPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchRedSuperConnectorPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreRedSuperConnectorPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countRedSuperConnectorProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countRedSuperConnectorProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageRedSuperConnectorMounts(firstMounts);
        Vec3 secondCenter = averageRedSuperConnectorMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doRedSuperConnectorProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doRedSuperConnectorProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaRedSuperConnector(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaRedSuperConnector(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaRedSuperConnector(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaRedSuperConnector(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaRedSuperConnector(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageRedSuperConnectorMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteRedSuperConnectorMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareRedSuperConnectorBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the red pylon Kind. Do not fold this path into another pylon Kind.
    private static int[] mapRedPylonIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapRedPylonMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareRedPylonBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteRedPylonMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteRedPylonMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectRedPylonBest(firstMounts, secondMounts, lineCount)
                : selectRedPylonBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectRedPylonBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchRedPylonPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchRedPylonPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreRedPylonPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchRedPylonPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreRedPylonPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countRedPylonProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countRedPylonProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageRedPylonMounts(firstMounts);
        Vec3 secondCenter = averageRedPylonMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doRedPylonProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doRedPylonProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaRedPylon(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaRedPylon(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaRedPylon(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaRedPylon(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaRedPylon(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageRedPylonMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteRedPylonMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareRedPylonBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the medium wood pylon Kind. Do not fold this path into another pylon Kind.
    private static int[] mapMediumWoodPylonIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapMediumWoodPylonMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareMediumWoodPylonBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteMediumWoodPylonMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteMediumWoodPylonMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectMediumWoodPylonBest(firstMounts, secondMounts, lineCount)
                : selectMediumWoodPylonBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectMediumWoodPylonBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchMediumWoodPylonPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchMediumWoodPylonPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreMediumWoodPylonPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchMediumWoodPylonPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreMediumWoodPylonPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countMediumWoodPylonProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countMediumWoodPylonProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageMediumWoodPylonMounts(firstMounts);
        Vec3 secondCenter = averageMediumWoodPylonMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doMediumWoodPylonProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doMediumWoodPylonProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaMediumWoodPylon(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaMediumWoodPylon(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaMediumWoodPylon(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaMediumWoodPylon(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaMediumWoodPylon(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageMediumWoodPylonMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteMediumWoodPylonMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareMediumWoodPylonBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the medium wood transformer Kind. Do not fold this path into another pylon Kind.
    private static int[] mapMediumWoodTransformerIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapMediumWoodTransformerMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareMediumWoodTransformerBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteMediumWoodTransformerMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteMediumWoodTransformerMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectMediumWoodTransformerBest(firstMounts, secondMounts, lineCount)
                : selectMediumWoodTransformerBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectMediumWoodTransformerBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchMediumWoodTransformerPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchMediumWoodTransformerPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreMediumWoodTransformerPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchMediumWoodTransformerPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreMediumWoodTransformerPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countMediumWoodTransformerProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countMediumWoodTransformerProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageMediumWoodTransformerMounts(firstMounts);
        Vec3 secondCenter = averageMediumWoodTransformerMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doMediumWoodTransformerProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doMediumWoodTransformerProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaMediumWoodTransformer(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaMediumWoodTransformer(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaMediumWoodTransformer(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaMediumWoodTransformer(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaMediumWoodTransformer(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageMediumWoodTransformerMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteMediumWoodTransformerMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareMediumWoodTransformerBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the medium steel pylon Kind. Do not fold this path into another pylon Kind.
    private static int[] mapMediumSteelPylonIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapMediumSteelPylonMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareMediumSteelPylonBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteMediumSteelPylonMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteMediumSteelPylonMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectMediumSteelPylonBest(firstMounts, secondMounts, lineCount)
                : selectMediumSteelPylonBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectMediumSteelPylonBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchMediumSteelPylonPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchMediumSteelPylonPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreMediumSteelPylonPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchMediumSteelPylonPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreMediumSteelPylonPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countMediumSteelPylonProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countMediumSteelPylonProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageMediumSteelPylonMounts(firstMounts);
        Vec3 secondCenter = averageMediumSteelPylonMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doMediumSteelPylonProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doMediumSteelPylonProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaMediumSteelPylon(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaMediumSteelPylon(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaMediumSteelPylon(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaMediumSteelPylon(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaMediumSteelPylon(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageMediumSteelPylonMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteMediumSteelPylonMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareMediumSteelPylonBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the medium steel transformer Kind. Do not fold this path into another pylon Kind.
    private static int[] mapMediumSteelTransformerIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapMediumSteelTransformerMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareMediumSteelTransformerBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteMediumSteelTransformerMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteMediumSteelTransformerMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectMediumSteelTransformerBest(firstMounts, secondMounts, lineCount)
                : selectMediumSteelTransformerBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectMediumSteelTransformerBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchMediumSteelTransformerPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchMediumSteelTransformerPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreMediumSteelTransformerPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchMediumSteelTransformerPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreMediumSteelTransformerPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countMediumSteelTransformerProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countMediumSteelTransformerProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageMediumSteelTransformerMounts(firstMounts);
        Vec3 secondCenter = averageMediumSteelTransformerMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doMediumSteelTransformerProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doMediumSteelTransformerProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaMediumSteelTransformer(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaMediumSteelTransformer(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaMediumSteelTransformer(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaMediumSteelTransformer(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaMediumSteelTransformer(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageMediumSteelTransformerMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteMediumSteelTransformerMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareMediumSteelTransformerBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the large pylon Kind. Do not fold this path into another pylon Kind.
    private static int[] mapLargePylonIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapLargePylonMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareLargePylonBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteLargePylonMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteLargePylonMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectLargePylonBest(firstMounts, secondMounts, lineCount)
                : selectLargePylonBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectLargePylonBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchLargePylonPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchLargePylonPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreLargePylonPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchLargePylonPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreLargePylonPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countLargePylonProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countLargePylonProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageLargePylonMounts(firstMounts);
        Vec3 secondCenter = averageLargePylonMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doLargePylonProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doLargePylonProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaLargePylon(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaLargePylon(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaLargePylon(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaLargePylon(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaLargePylon(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageLargePylonMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteLargePylonMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareLargePylonBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    // Intentionally owned by the substation Kind. Do not fold this path into another pylon Kind.
    private static int[] mapSubstationIdentity(Vec3[] secondMounts, int lineCount) {
        int[] mapping = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            mapping[i] = i % secondMounts.length;
        }
        return mapping;
    }

    private static int[] mapSubstationMatched(BlockPos firstPos,
                                        Vec3[] firstLocalMounts,
                                        PowerPylonBlockEntity.ConnectionRenderInfo second,
                                        int lineCount) {
        boolean firstIsCanonical = compareSubstationBlockPos(firstPos, second.pos()) <= 0;
        Vec3[] firstMounts = absoluteSubstationMounts(firstLocalMounts, firstPos, lineCount);
        Vec3[] secondMounts = absoluteSubstationMounts(second.mountPositions(), second.pos(), lineCount);
        int[] canonicalMapping = firstIsCanonical
                ? selectSubstationBest(firstMounts, secondMounts, lineCount)
                : selectSubstationBest(secondMounts, firstMounts, lineCount);
        if (firstIsCanonical) {
            return canonicalMapping;
        }

        int[] inverse = new int[lineCount];
        for (int i = 0; i < canonicalMapping.length; i++) {
            inverse[canonicalMapping[i]] = i;
        }
        return inverse;
    }

    private static int[] selectSubstationBest(Vec3[] firstMounts, Vec3[] secondMounts, int lineCount) {
        int[] current = new int[lineCount];
        int[] best = new int[lineCount];
        boolean[] used = new boolean[lineCount];
        double[] bestScore = {Double.POSITIVE_INFINITY};
        searchSubstationPermutation(0, current, best, used, firstMounts, secondMounts, bestScore);
        return best;
    }

    private static void searchSubstationPermutation(int depth, int[] current, int[] best, boolean[] used,
                                              Vec3[] firstMounts, Vec3[] secondMounts,
                                              double[] bestScore) {
        if (depth == current.length) {
            double score = scoreSubstationPermutation(firstMounts, secondMounts, current);
            if (score + 1.0E-6D < bestScore[0]) {
                bestScore[0] = score;
                System.arraycopy(current, 0, best, 0, current.length);
            }
            return;
        }

        for (int i = 0; i < current.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current[depth] = i;
                searchSubstationPermutation(depth + 1, current, best, used, firstMounts, secondMounts, bestScore);
                used[i] = false;
            }
        }
    }

    private static double scoreSubstationPermutation(Vec3[] firstMounts, Vec3[] secondMounts, int[] mapping) {
        double score = 0.0D;
        for (int i = 0; i < mapping.length; i++) {
            score += firstMounts[i].distanceToSqr(secondMounts[mapping[i]]);
        }
        return score + countSubstationProjectedCrossings(firstMounts, secondMounts, mapping) * 1_000_000.0D;
    }

    private static int countSubstationProjectedCrossings(Vec3[] firstMounts,
                                                   Vec3[] secondMounts,
                                                   int[] mapping) {
        Vec3 firstCenter = averageSubstationMounts(firstMounts);
        Vec3 secondCenter = averageSubstationMounts(secondMounts);
        Vec3 span = secondCenter.subtract(firstCenter);
        Vec3 horizontal = new Vec3(span.x, 0.0D, span.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 sideAxis = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();

        int crossings = 0;
        for (int i = 0; i < mapping.length; i++) {
            for (int j = i + 1; j < mapping.length; j++) {
                Vec3 a = firstMounts[i];
                Vec3 b = secondMounts[mapping[i]];
                Vec3 c = firstMounts[j];
                Vec3 d = secondMounts[mapping[j]];
                if (doSubstationProjectedSegmentsCross(
                        a.dot(sideAxis), a.y, b.dot(sideAxis), b.y,
                        c.dot(sideAxis), c.y, d.dot(sideAxis), d.y)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private static boolean doSubstationProjectedSegmentsCross(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double o1 = signedAreaSubstation(ax, ay, bx, by, cx, cy);
        double o2 = signedAreaSubstation(ax, ay, bx, by, dx, dy);
        double o3 = signedAreaSubstation(cx, cy, dx, dy, ax, ay);
        double o4 = signedAreaSubstation(cx, cy, dx, dy, bx, by);
        return o1 * o2 < -1.0E-7D && o3 * o4 < -1.0E-7D;
    }

    private static double signedAreaSubstation(double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static Vec3 averageSubstationMounts(Vec3[] points) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        double count = Math.max(1, points.length);
        return new Vec3(x / count, y / count, z / count);
    }

    private static Vec3[] absoluteSubstationMounts(Vec3[] mounts, BlockPos pos, int lineCount) {
        Vec3[] result = new Vec3[lineCount];
        for (int i = 0; i < lineCount; i++) {
            result[i] = mounts[i % mounts.length].add(pos.getX(), pos.getY(), pos.getZ());
        }
        return result;
    }

    private static int compareSubstationBlockPos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) {
            return y;
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    public record WireQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int wrap) {
        private List<Vec3> vertices() {
            return List.of(this.a, this.b, this.c, this.d);
        }
    }
}
