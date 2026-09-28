package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.blockentity.FusionTorusStructBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Post-bake, immutable torus preview generation shared by VBO emission and culling. */
public class FusionTorusStructBlockEntityRenderer implements LongRangeBlockEntityRenderer<FusionTorusStructBlockEntity> {
    private static final double SMALL_BLOCK_SIZE = 5.0D / 16.0D;
    private static final double SMALL_BLOCK_OFFSET = (1.0D - SMALL_BLOCK_SIZE) * 0.5D;
    private static final int ALPHA_75 = 0xBFFFFFFF;
    private static final RenderType STRUCTURE_RENDER_TYPE =
            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final Set<FusionTorusStructBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeneration;

    private VertexBuffer structureBuffer;
    private GeometryGeneration builtGeneration;
    private int builtOverlay;

    public FusionTorusStructBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        FusionTorusStructBlockEntity.setClientRenderSnapshotUpdater(
                FusionTorusStructBlockEntityRenderer::updateRenderSnapshot);
    }

    @Override
    public void render(FusionTorusStructBlockEntity struct, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Object token = struct.renderSnapshot().geometryToken();
        if (!(token instanceof GeometryGeneration generation)) {
            return;
        }
        if (!generation.hasGeometry()) {
            return;
        }
        ensureStructureBuffer(generation, packedOverlay);
        if (this.structureBuffer == null || this.structureBuffer.isInvalid()) {
            return;
        }

        STRUCTURE_RENDER_TYPE.setupRenderState();
        try {
            this.structureBuffer.bind();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                this.structureBuffer.drawWithShader(
                        MachineModelRenderer.composeModelView(poseStack),
                        RenderSystem.getProjectionMatrix(), shader);
            }
        } finally {
            VertexBuffer.unbind();
            STRUCTURE_RENDER_TYPE.clearRenderState();
        }
    }

    @Override
    public AABB getRenderBoundingBox(FusionTorusStructBlockEntity struct) {
        return struct.renderSnapshot().bounds();
    }

    /** Captures actual baked layers/quads/vertices after every model bake. */
    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(FusionTorusStructBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        GeometryGeneration next = captureGeometryGeneration(++nextGeneration);
        geometryGeneration = next;
        closeAllBuffers();
        FusionTorusStructBlockEntity.setClientRenderSnapshotUpdater(
                FusionTorusStructBlockEntityRenderer::updateRenderSnapshot);
        FusionTorusStructBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Releases GPU buffers while retaining immutable CPU geometry for reconnects. */
    static void invalidateModelCache() {
        closeAllBuffers();
    }

    private static void closeAllBuffers() {
        List<FusionTorusStructBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (FusionTorusStructBlockEntityRenderer renderer : renderers) {
            renderer.closeStructureBuffer();
        }
    }

    private static void updateRenderSnapshot(FusionTorusStructBlockEntity struct) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        FusionTorusStructBlockEntity.RenderSnapshot previous = struct.renderSnapshot();
        long revision = FusionTorusStructBlockEntity.clientRenderSnapshotRevision();
        if (previous.geometryToken() == generation && previous.revision() == revision) {
            return;
        }
        struct.publishClientRenderSnapshot(new FusionTorusStructBlockEntity.RenderSnapshot(
                generation, generation.localBounds().move(struct.getBlockPos()), revision));
    }

    private void ensureStructureBuffer(GeometryGeneration generation, int packedOverlay) {
        if (this.structureBuffer != null
                && !this.structureBuffer.isInvalid()
                && this.builtGeneration == generation
                && this.builtOverlay == packedOverlay) {
            return;
        }

        closeStructureBuffer();
        BufferBuilder builder = Tesselator.getInstance().begin(
                STRUCTURE_RENDER_TYPE.mode(), STRUCTURE_RENDER_TYPE.format());
        PoseStack buildPose = new PoseStack();
        for (Placement placement : generation.placements()) {
            buildPose.pushPose();
            applyComponentPose(buildPose, placement.x(), placement.y(), placement.z());
            for (List<BakedQuad> layer : placement.model().layers()) {
                MachineModelRenderer.emitCapturedQuadsTinted(
                        layer, buildPose, builder, LightTexture.FULL_BRIGHT,
                        packedOverlay, ALPHA_75);
            }
            buildPose.popPose();
        }

        MeshData mesh = builder.buildOrThrow();
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            buffer.bind();
            buffer.upload(mesh);
        } catch (RuntimeException exception) {
            buffer.close();
            throw exception;
        } finally {
            VertexBuffer.unbind();
        }
        this.structureBuffer = buffer;
        this.builtGeneration = generation;
        this.builtOverlay = packedOverlay;
    }

    private void closeStructureBuffer() {
        if (this.structureBuffer != null) {
            this.structureBuffer.close();
            this.structureBuffer = null;
        }
        this.builtGeneration = null;
    }

    private static GeometryGeneration captureGeometryGeneration(int generation) {
        Map<Integer, CapturedModel> models = new HashMap<>();
        List<Placement> placements = new ArrayList<>();
        for (int y = 0; y < 5; y++) {
            int layerIndex = y > 2 ? 4 - y : y;
            int[][] layer = FusionMachineBlock.TORUS_LAYOUT[layerIndex];
            for (int x = 0; x < layer.length; x++) {
                for (int z = 0; z < layer[x].length; z++) {
                    int variant = layer[x][z];
                    if (variant == 0) {
                        continue;
                    }
                    CapturedModel model = models.computeIfAbsent(variant,
                            FusionTorusStructBlockEntityRenderer::captureVariant);
                    placements.add(new Placement(model, x - 7, y, z - 7));
                }
            }
        }

        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(BlockPos.ZERO);
        PoseStack poseStack = new PoseStack();
        for (Placement placement : placements) {
            poseStack.pushPose();
            applyComponentPose(poseStack, placement.x(), placement.y(), placement.z());
            placement.model().includeTransformed(bounds, poseStack);
            poseStack.popPose();
        }
        boolean hasGeometry = placements.stream()
                .anyMatch(placement -> placement.model().vertices().length != 0);
        return new GeometryGeneration(
                generation, List.copyOf(placements), bounds.build(), hasGeometry);
    }

    private static CapturedModel captureVariant(int variant) {
        BlockState state = HbmBlocks.FUSION_COMPONENT.get().defaultBlockState()
                .setValue(LegacyVariantBlock.VARIANT, variant);
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
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
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

    private static void applyComponentPose(PoseStack poseStack, int x, int y, int z) {
        poseStack.translate(x + SMALL_BLOCK_OFFSET, y + SMALL_BLOCK_OFFSET, z + SMALL_BLOCK_OFFSET);
        poseStack.scale((float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE);
    }

    private record GeometryGeneration(int generation, List<Placement> placements,
                                      AABB localBounds, boolean hasGeometry) {
    }

    private record Placement(CapturedModel model, int x, int y, int z) {
    }

    private record CapturedModel(List<List<BakedQuad>> layers, float[] vertices) {
        private void includeTransformed(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
            for (int index = 0; index < vertices.length; index += 3) {
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(
                        vertices[index], vertices[index + 1], vertices[index + 2])));
            }
        }
    }
}
