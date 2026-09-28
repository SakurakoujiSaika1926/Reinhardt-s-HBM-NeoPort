package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-thread sink which records the final float positions emitted by an
 * existing render path.  It deliberately captures after PoseStack transforms,
 * so model, item, font, and attachment geometry share one exact source of truth.
 */
final class GeometryCaptureBufferSource implements MultiBufferSource {
    private final CaptureConsumer consumer = new CaptureConsumer();

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return this.consumer;
    }

    AABB bounds(BlockPos pos) {
        return this.consumer.bounds(pos);
    }

    double maximumDistance(Vector3f pivot) {
        return this.consumer.maximumDistance(pivot);
    }

    private static final class CaptureConsumer implements VertexConsumer {
        private final List<Vector3f> vertices = new ArrayList<>();
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.vertices.add(new Vector3f(x, y, z));
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }

        private AABB bounds(BlockPos pos) {
            if (!Float.isFinite(this.minX)) {
                return new AABB(pos);
            }
            return new AABB(
                    pos.getX() + this.minX, pos.getY() + this.minY, pos.getZ() + this.minZ,
                    pos.getX() + this.maxX, pos.getY() + this.maxY, pos.getZ() + this.maxZ);
        }

        private double maximumDistance(Vector3f pivot) {
            double radiusSquared = 0.0D;
            for (Vector3f vertex : this.vertices) {
                double dx = vertex.x - pivot.x;
                double dy = vertex.y - pivot.y;
                double dz = vertex.z - pivot.z;
                radiusSquared = Math.max(radiusSquared, dx * dx + dy * dy + dz * dz);
            }
            return Math.sqrt(radiusSquared);
        }
    }
}
