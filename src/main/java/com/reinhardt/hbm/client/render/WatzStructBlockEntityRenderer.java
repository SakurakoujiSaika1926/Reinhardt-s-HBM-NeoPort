package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.blockentity.WatzStructBlockEntity;
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
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Static 110-component Watz construction preview, baked once per model generation. */
public class WatzStructBlockEntityRenderer implements LongRangeBlockEntityRenderer<WatzStructBlockEntity> {
    private static final double SMALL_BLOCK_SIZE = 5.0D / 16.0D;
    private static final double SMALL_BLOCK_OFFSET = (1.0D - SMALL_BLOCK_SIZE) * 0.5D;
    private static final int ALPHA_75 = 0xBFFFFFFF;
    private static final int COMPONENT_COUNT = 110;
    private static final RenderType STRUCTURE_RENDER_TYPE =
            RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final Set<WatzStructBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeneration;

    private VertexBuffer structureBuffer;
    private GeometryGeneration builtGeneration;
    private int builtOverlay;

    public WatzStructBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        WatzStructBlockEntity.setClientRenderSnapshotUpdater(
                WatzStructBlockEntityRenderer::updateRenderSnapshot);
    }

    @Override
    public void render(WatzStructBlockEntity struct, float partialTick, PoseStack poseStack,
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
                        RenderSystem.getProjectionMatrix(),
                        shader);
            }
        } finally {
            VertexBuffer.unbind();
            STRUCTURE_RENDER_TYPE.clearRenderState();
        }
    }

    @Override
    public AABB getRenderBoundingBox(WatzStructBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    /** Captures a complete immutable CPU generation after model baking. */
    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(WatzStructBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        GeometryGeneration next = captureGeometryGeneration(++nextGeneration);
        geometryGeneration = next;
        closeAllBuffers();
        WatzStructBlockEntity.setClientRenderSnapshotUpdater(
                WatzStructBlockEntityRenderer::updateRenderSnapshot);
        WatzStructBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Releases GPU state on resource invalidation or client logout. */
    static void invalidateModelCache() {
        closeAllBuffers();
    }

    private static void closeAllBuffers() {
        List<WatzStructBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (WatzStructBlockEntityRenderer renderer : renderers) {
            renderer.closeStructureBuffer();
        }
    }

    private static void updateRenderSnapshot(WatzStructBlockEntity struct) {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            return;
        }
        WatzStructBlockEntity.RenderSnapshot previous = struct.renderSnapshot();
        long revision = WatzStructBlockEntity.clientRenderSnapshotRevision();
        if (previous.geometryToken() == generation && previous.revision() == revision) {
            return;
        }
        struct.publishClientRenderSnapshot(new WatzStructBlockEntity.RenderSnapshot(
                generation,
                generation.localBounds().move(struct.getBlockPos()),
                revision));
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
        BlockState elementState = HbmBlocks.WATZ_ELEMENT.get().defaultBlockState();
        BlockState coolerState = HbmBlocks.WATZ_COOLER.get().defaultBlockState();
        BlockState endState = boltedWatzEndState();
        CapturedModel element = captureModel(elementState);
        CapturedModel cooler = captureModel(coolerState);
        CapturedModel end = captureModel(endState);

        List<Placement> placements = buildPlacements(element, cooler, end);
        if (placements.size() != COMPONENT_COUNT) {
            throw new IllegalStateException("Watz preview component count changed: " + placements.size());
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

    private static List<Placement> buildPlacements(CapturedModel element, CapturedModel cooler,
                                                    CapturedModel end) {
        List<Placement> placements = new ArrayList<>(COMPONENT_COUNT);
        add(placements, cooler, 0, 1, 0);
        add(placements, cooler, 0, 2, 0);
        for (int y = 0; y < 3; y++) {
            add(placements, element, 1, y, 0);
            add(placements, element, 2, y, 0);
            add(placements, element, 0, y, 1);
            add(placements, element, 0, y, 2);
            add(placements, element, -1, y, 0);
            add(placements, element, -2, y, 0);
            add(placements, element, 0, y, -1);
            add(placements, element, 0, y, -2);
            add(placements, element, 1, y, 1);
            add(placements, element, 1, y, -1);
            add(placements, element, -1, y, 1);
            add(placements, element, -1, y, -1);

            add(placements, cooler, 2, y, 1);
            add(placements, cooler, 2, y, -1);
            add(placements, cooler, 1, y, 2);
            add(placements, cooler, -1, y, 2);
            add(placements, cooler, -2, y, 1);
            add(placements, cooler, -2, y, -1);
            add(placements, cooler, 1, y, -2);
            add(placements, cooler, -1, y, -2);

            for (int offset = -1; offset < 2; offset++) {
                add(placements, end, 3, y, offset);
                add(placements, end, offset, y, 3);
                add(placements, end, -3, y, offset);
                add(placements, end, offset, y, -3);
            }
            add(placements, end, 2, y, 2);
            add(placements, end, 2, y, -2);
            add(placements, end, -2, y, 2);
            add(placements, end, -2, y, -2);
        }
        return placements;
    }

    private static void add(List<Placement> placements, CapturedModel model, int x, int y, int z) {
        placements.add(new Placement(model, x, y, z));
    }

    private static void applyComponentPose(PoseStack poseStack, int x, int y, int z) {
        poseStack.translate(x + SMALL_BLOCK_OFFSET, y + SMALL_BLOCK_OFFSET, z + SMALL_BLOCK_OFFSET);
        poseStack.scale((float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE, (float) SMALL_BLOCK_SIZE);
    }

    /** Freezes exactly the layers/quads and face-used float positions later uploaded to the VBO. */
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

    private static BlockState boltedWatzEndState() {
        BlockState state = HbmBlocks.WATZ_END.get().defaultBlockState();
        return state.hasProperty(LegacyVariantBlock.VARIANT)
                ? state.setValue(LegacyVariantBlock.VARIANT, 1)
                : state;
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
