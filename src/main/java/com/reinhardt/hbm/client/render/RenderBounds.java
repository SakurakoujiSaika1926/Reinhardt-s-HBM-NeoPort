package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/** Exact helpers for block-relative OBJ envelopes used by block-entity renderers. */
final class RenderBounds {
    private RenderBounds() {
    }

    /** Adds a world position to final coordinates emitted through a float vertex path. */
    static AABB localFloatVertices(BlockPos pos,
                                   double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        return new AABB(
                pos.getX() + (float) minX, pos.getY() + (float) minY, pos.getZ() + (float) minZ,
                pos.getX() + (float) maxX, pos.getY() + (float) maxY, pos.getZ() + (float) maxZ
        );
    }

    /** Adds a world position to a proved continuous/procedural double envelope. */
    static AABB localAnalytic(BlockPos pos,
                              double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ) {
        return new AABB(
                pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ
        );
    }

    /** Applies an arbitrary completed float PoseStack to a local baked-float box. */
    static AABB transformedBox(BlockPos pos, PoseStack poseStack,
                               float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ) {
        FloatBuilder builder = new FloatBuilder(pos);
        builder.includeBox(poseStack, minX, minY, minZ, maxX, maxY, maxZ);
        return builder.build();
    }

    /**
     * Accumulates the same float positions emitted by ModelPart/BakedQuad
     * rendering.  Callers supply each face-used primitive separately so a
     * rotated sparse model is not widened to the transformed corners of a
     * merely enclosing box.
     */
    static final class FloatBuilder {
        private final BlockPos pos;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        FloatBuilder(BlockPos pos) {
            this.pos = pos;
        }

        void includeBox(PoseStack poseStack,
                        float minX, float minY, float minZ,
                        float maxX, float maxY, float maxZ) {
            for (float x : new float[] {minX, maxX}) {
                for (float y : new float[] {minY, maxY}) {
                    for (float z : new float[] {minZ, maxZ}) {
                        include(poseStack.last().pose().transformPosition(new Vector3f(x, y, z)));
                    }
                }
            }
        }

        void include(Vector3f point) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        AABB build() {
            if (!Float.isFinite(minX)) {
                return zeroVolume(pos);
            }
            return new AABB(
                    pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ
            );
        }

        private static AABB zeroVolume(BlockPos pos) {
            return new AABB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX(), pos.getY(), pos.getZ()
            );
        }
    }
}
