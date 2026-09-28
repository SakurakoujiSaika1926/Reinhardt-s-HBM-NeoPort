package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.RenderTypeHelper;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Main-thread capture of a block model and every float vertex emitted for it. */
final class BakedBlockGeometrySnapshot {
    private BakedBlockGeometrySnapshot() {
    }

    static Result build(BlockState state, Iterable<PoseStack> poses, BlockPos worldPos) {
        try {
            if (state.getRenderShape() != RenderShape.MODEL) {
                double x = worldPos.getX();
                double y = worldPos.getY();
                double z = worldPos.getZ();
                return new Result(state, new CapturedModel(null, List.of()),
                        new AABB(x, y, z, x, y, z), 0.0D);
            }
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            return build(model, state, poses, worldPos);
        } catch (RuntimeException malformedModel) {
            return empty(state, worldPos);
        }
    }

    static Result build(BakedModel model, BlockState state, Iterable<PoseStack> poses, BlockPos worldPos) {
        try {
            Bounds bounds = new Bounds(worldPos);
            RandomSource random = RandomSource.create(42L);
            random.setSeed(42L);
            List<Layer> layers = new ArrayList<>();
            for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                List<BakedQuad> capturedQuads = new ArrayList<>();
                random.setSeed(42L);
                capturedQuads.addAll(model.getQuads(state, null, random, ModelData.EMPTY, renderType));
                for (Direction side : Direction.values()) {
                    random.setSeed(42L);
                    capturedQuads.addAll(model.getQuads(state, side, random, ModelData.EMPTY, renderType));
                }
                List<BakedQuad> immutableQuads = List.copyOf(capturedQuads);
                include(bounds, immutableQuads, poses);
                layers.add(new Layer(renderType, immutableQuads));
            }
            return new Result(state, new CapturedModel(model, List.copyOf(layers)),
                    bounds.build(), bounds.maximumRadius());
        } catch (RuntimeException malformedModel) {
            return empty(state, worldPos);
        }
    }

    private static Result empty(BlockState state, BlockPos worldPos) {
        double x = worldPos.getX();
        double y = worldPos.getY();
        double z = worldPos.getZ();
        return new Result(state, null, new AABB(x, y, z, x, y, z), 0.0D);
    }

    private static void include(Bounds bounds, Iterable<BakedQuad> quads, Iterable<PoseStack> poses) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);
                for (PoseStack pose : poses) {
                    bounds.include(pose.last().pose().transformPosition(new Vector3f(x, y, z)));
                }
            }
        }
    }

    static void render(Object token, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!(token instanceof CapturedModel captured)) {
            return;
        }
        for (Layer layer : captured.layers()) {
            VertexConsumer consumer = buffers.getBuffer(RenderTypeHelper.getEntityRenderType(layer.renderType(), false));
            MachineModelRenderer.renderCapturedQuads(
                    layer.quads(), poseStack, consumer, packedLight, packedOverlay);
        }
    }

    static void renderCutoutNoCull(Object token, PoseStack poseStack, MultiBufferSource buffers,
                                   int packedLight, int packedOverlay) {
        if (!(token instanceof CapturedModel captured)) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (Layer layer : captured.layers()) {
            MachineModelRenderer.renderCapturedQuads(
                    layer.quads(), poseStack, consumer, packedLight, packedOverlay);
        }
    }

    record Result(BlockState state, Object modelToken, AABB bounds, double maximumRadius) {
    }

    private record CapturedModel(BakedModel sourceModel, List<Layer> layers) {
    }

    private record Layer(RenderType renderType, List<BakedQuad> quads) {
    }

    private static final class Bounds {
        private final BlockPos pos;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private double maximumRadiusSquared;

        private Bounds(BlockPos pos) {
            this.pos = pos;
        }

        private void include(Vector3f point) {
            this.minX = Math.min(this.minX, point.x);
            this.minY = Math.min(this.minY, point.y);
            this.minZ = Math.min(this.minZ, point.z);
            this.maxX = Math.max(this.maxX, point.x);
            this.maxY = Math.max(this.maxY, point.y);
            this.maxZ = Math.max(this.maxZ, point.z);
            this.maximumRadiusSquared = Math.max(this.maximumRadiusSquared,
                    (double) point.x * point.x
                            + (double) point.y * point.y
                            + (double) point.z * point.z);
        }

        private double maximumRadius() {
            return Math.nextUp(Math.sqrt(this.maximumRadiusSquared));
        }

        private AABB build() {
            if (!Float.isFinite(this.minX)) {
                double x = this.pos.getX();
                double y = this.pos.getY();
                double z = this.pos.getZ();
                return new AABB(x, y, z, x, y, z);
            }
            return new AABB(
                    this.pos.getX() + this.minX,
                    this.pos.getY() + this.minY,
                    this.pos.getZ() + this.minZ,
                    this.pos.getX() + this.maxX,
                    this.pos.getY() + this.maxY,
                    this.pos.getZ() + this.maxZ
            );
        }
    }
}
