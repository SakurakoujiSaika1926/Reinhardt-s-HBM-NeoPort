package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Immutable post-bake geometry and one static VBO for legacy construction
 * previews.  Layout ownership remains in the individual structure renderers;
 * this class only freezes baked quads and submits the resulting mesh.
 */
final class ConstructionPreviewMesh {
    static final double SMALL_BLOCK_SIZE = 5.0D / 16.0D;
    static final double SMALL_BLOCK_OFFSET = (1.0D - SMALL_BLOCK_SIZE) * 0.5D;

    private static final int ALPHA_75 = 0xBFFFFFFF;
    private static final RenderType RENDER_TYPE =
            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    private VertexBuffer vertexBuffer;
    private Geometry builtGeometry;
    private int builtOverlay = Integer.MIN_VALUE;

    void render(Geometry geometry, PoseStack poseStack, int packedOverlay) {
        if (geometry == null || !geometry.hasGeometry()) {
            return;
        }
        ensureBuffer(geometry, packedOverlay);
        if (this.vertexBuffer == null || this.vertexBuffer.isInvalid()) {
            return;
        }

        RENDER_TYPE.setupRenderState();
        try {
            this.vertexBuffer.bind();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                this.vertexBuffer.drawWithShader(
                        MachineModelRenderer.composeModelView(poseStack),
                        RenderSystem.getProjectionMatrix(),
                        shader);
            }
        } finally {
            VertexBuffer.unbind();
            RENDER_TYPE.clearRenderState();
        }
    }

    void close() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
        this.builtGeometry = null;
        this.builtOverlay = Integer.MIN_VALUE;
    }

    private void ensureBuffer(Geometry geometry, int packedOverlay) {
        if (this.vertexBuffer != null
                && !this.vertexBuffer.isInvalid()
                && this.builtGeometry == geometry
                && this.builtOverlay == packedOverlay) {
            return;
        }

        close();
        BufferBuilder builder = Tesselator.getInstance().begin(
                RENDER_TYPE.mode(), RENDER_TYPE.format());
        PoseStack buildPose = new PoseStack();
        for (Placement placement : geometry.placements()) {
            buildPose.pushPose();
            applyComponentPose(buildPose, placement.x(), placement.y(), placement.z());
            for (List<BakedQuad> layer : placement.model().layers()) {
                MachineModelRenderer.emitCapturedQuadsTinted(
                        layer,
                        buildPose,
                        builder,
                        LightTexture.FULL_BRIGHT,
                        packedOverlay,
                        ALPHA_75);
            }
            buildPose.popPose();
        }

        MeshData mesh = builder.buildOrThrow();
        VertexBuffer uploaded = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            uploaded.bind();
            uploaded.upload(mesh);
        } catch (RuntimeException exception) {
            uploaded.close();
            throw exception;
        } finally {
            VertexBuffer.unbind();
        }
        this.vertexBuffer = uploaded;
        this.builtGeometry = geometry;
        this.builtOverlay = packedOverlay;
    }

    static Geometry capture(int generation, List<PlacementSpec> specifications) {
        Map<BlockState, CapturedModel> models = new HashMap<>();
        List<Placement> placements = new ArrayList<>(specifications.size());
        for (PlacementSpec specification : specifications) {
            CapturedModel model = models.computeIfAbsent(
                    specification.state(), ConstructionPreviewMesh::captureModel);
            placements.add(new Placement(
                    model, specification.x(), specification.y(), specification.z()));
        }
        Geometry geometry = new Geometry(generation, List.copyOf(placements), null, false);
        AABB localBounds = geometry.transformedLocalBounds(ignored -> { });
        boolean hasGeometry = placements.stream()
                .anyMatch(placement -> placement.model().vertices().length != 0);
        return new Geometry(generation, List.copyOf(placements), localBounds, hasGeometry);
    }

    static PlacementSpec placement(BlockState state, int x, int y, int z) {
        return new PlacementSpec(state, x, y, z);
    }

    static void applyComponentPose(PoseStack poseStack, int x, int y, int z) {
        poseStack.translate(
                x + SMALL_BLOCK_OFFSET,
                y + SMALL_BLOCK_OFFSET,
                z + SMALL_BLOCK_OFFSET);
        poseStack.scale(
                (float) SMALL_BLOCK_SIZE,
                (float) SMALL_BLOCK_SIZE,
                (float) SMALL_BLOCK_SIZE);
    }

    private static CapturedModel captureModel(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create(42L);
        List<List<BakedQuad>> layers = new ArrayList<>();
        List<Float> coordinates = new ArrayList<>();
        random.setSeed(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, coordinates, model, state, null, renderType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, coordinates, model, state, side, renderType, random);
            }
            if (!quads.isEmpty()) {
                layers.add(List.copyOf(quads));
            }
        }
        float[] vertices = new float[coordinates.size()];
        for (int index = 0; index < coordinates.size(); index++) {
            vertices[index] = coordinates.get(index);
        }
        return new CapturedModel(List.copyOf(layers), vertices);
    }

    private static void collectQuads(List<BakedQuad> quads, List<Float> coordinates,
                                     BakedModel model, BlockState state, Direction side,
                                     RenderType renderType, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(
                state, side, random, ModelData.EMPTY, renderType)) {
            quads.add(quad);
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                coordinates.add(Float.intBitsToFloat(data[offset]));
                coordinates.add(Float.intBitsToFloat(data[offset + 1]));
                coordinates.add(Float.intBitsToFloat(data[offset + 2]));
            }
        }
    }

    record PlacementSpec(BlockState state, int x, int y, int z) {
    }

    record Geometry(int generation, List<Placement> placements,
                    AABB localBounds, boolean hasGeometry) {
        AABB boundsAt(BlockPos pos) {
            return this.localBounds == null
                    ? new AABB(pos)
                    : this.localBounds.move(pos);
        }

        AABB transformedLocalBounds(Consumer<PoseStack> rootTransform) {
            RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(BlockPos.ZERO);
            PoseStack poseStack = new PoseStack();
            rootTransform.accept(poseStack);
            for (Placement placement : this.placements) {
                poseStack.pushPose();
                applyComponentPose(poseStack, placement.x(), placement.y(), placement.z());
                placement.model().includeTransformed(bounds, poseStack);
                poseStack.popPose();
            }
            return bounds.build();
        }
    }

    private record Placement(CapturedModel model, int x, int y, int z) {
    }

    private record CapturedModel(List<List<BakedQuad>> layers, float[] vertices) {
        private void includeTransformed(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
            for (int index = 0; index < this.vertices.length; index += 3) {
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(
                        this.vertices[index],
                        this.vertices[index + 1],
                        this.vertices[index + 2])));
            }
        }
    }
}
