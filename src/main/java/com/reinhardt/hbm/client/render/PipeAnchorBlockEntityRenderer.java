package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.reinhardt.hbm.blockentity.PipeAnchorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Exact 1.7.10 anchor geometry with immutable, change-driven connection batches. */
public final class PipeAnchorBlockEntityRenderer implements LongRangeBlockEntityRenderer<PipeAnchorBlockEntity> {
    private static final ModelResourceLocation ANCHOR = part("pipe_anchor");
    private static final ModelResourceLocation PIPE = part("pipe_anchor_pipe");
    private static final ModelResourceLocation RING = part("pipe_anchor_ring");
    private static final Set<PipeAnchorBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeneration;

    /** Identity prevents equal coordinates in separate client levels sharing GPU state. */
    private final Map<PipeAnchorBlockEntity, ConnectionBuffers> connectionBuffers = new IdentityHashMap<>();

    public PipeAnchorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        PipeAnchorBlockEntity.setClientRenderSnapshotUpdater(PipeAnchorBlockEntityRenderer::updateRenderSnapshot);
        PipeAnchorBlockEntity.setClientGpuCacheDisposer(PipeAnchorBlockEntityRenderer::discardMachineCache);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ANCHOR);
        event.register(PIPE);
        event.register(RING);
    }

    static void beginGeometryReload() {
        geometryGeneration = null;
        closeAllGpuCaches();
        PipeAnchorBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Captures all render layers, quads and face-used float vertices from the completed bake. */
    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(PipeAnchorBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        Map<BlockState, ModelSet> models = new HashMap<>();
        for (BlockState state : HbmBlocks.PIPE_ANCHOR.get().getStateDefinition().getPossibleStates()) {
            models.put(state, new ModelSet(
                    captureModel(ANCHOR, state, false), captureModel(PIPE, state, true),
                    captureModel(RING, state, false)));
        }
        geometryGeneration = new GeometryGeneration(++nextGeneration, Map.copyOf(models));
        closeAllGpuCaches();
        PipeAnchorBlockEntity.setClientRenderSnapshotUpdater(PipeAnchorBlockEntityRenderer::updateRenderSnapshot);
        PipeAnchorBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    static void closeGpuCachesOnLogout() {
        closeAllGpuCaches();
    }

    @Override
    public void render(PipeAnchorBlockEntity anchor, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PipeAnchorBlockEntity.RenderSnapshot snapshot = anchor.renderSnapshot();
        GeometryGeneration current = geometryGeneration;
        if (snapshot.geometryToken() != current || current == null) {
            return;
        }
        ModelSet models = current.models().get(snapshot.blockState());
        if (models == null) {
            return;
        }

        poseStack.pushPose();
        applyAnchorPose(poseStack, snapshot.facing());
        models.anchor().render(poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        ConnectionBuffers buffers = connectionBuffers.computeIfAbsent(anchor, ignored -> new ConnectionBuffers());
        buffers.prepare(snapshot, current, models, packedLight, packedOverlay);
        drawLayers(buffers.layers(), poseStack);
    }

    @Override
    public AABB getRenderBoundingBox(PipeAnchorBlockEntity anchor) {
        return anchor.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(PipeAnchorBlockEntity anchor) {
        PipeAnchorBlockEntity.RenderInputs inputs = anchor.captureClientRenderInputs();
        GeometryGeneration generation = geometryGeneration;
        PipeAnchorBlockEntity.RenderSnapshot previous = anchor.renderSnapshot();
        if (generation != null && previous.geometryToken() == generation
                && previous.blockState() == inputs.blockState()
                && previous.facing() == inputs.facing()
                && previous.color() == inputs.color()
                && previous.links().equals(inputs.links())) {
            return;
        }

        if (generation == null) {
            BlockPos pos = anchor.getBlockPos();
            anchor.publishClientRenderSnapshot(new PipeAnchorBlockEntity.RenderSnapshot(
                    null, inputs.blockState(), inputs.links(), List.of(),
                    zeroVolumeBounds(pos), inputs.facing(), inputs.color()));
            return;
        }

        List<PipeAnchorBlockEntity.ConnectionPose> connections = new ArrayList<>();
        BlockPos origin = anchor.getBlockPos();
        for (BlockPos link : inputs.links()) {
            double dx = link.getX() - origin.getX();
            double dy = link.getY() - origin.getY();
            double dz = link.getZ() - origin.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length <= 0.0D) {
                continue;
            }
            float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
            double pitch = Math.toDegrees(Math.atan2(dy, horizontal));
            float xRotation = (float) (90.0D - pitch);
            connections.add(new PipeAnchorBlockEntity.ConnectionPose(
                    yaw, xRotation, (float) length, (float) (length / 2.0D - 1.5D)));
        }
        List<PipeAnchorBlockEntity.ConnectionPose> immutableConnections = List.copyOf(connections);
        ModelSet models = generation.models().get(inputs.blockState());
        AABB bounds = models == null
                ? zeroVolumeBounds(origin)
                : buildBounds(origin, models, inputs.facing(), immutableConnections);
        anchor.publishClientRenderSnapshot(new PipeAnchorBlockEntity.RenderSnapshot(
                generation, inputs.blockState(), inputs.links(), immutableConnections,
                bounds, inputs.facing(), inputs.color()));
    }

    private static AABB buildBounds(BlockPos pos, ModelSet models, Direction facing,
                                    List<PipeAnchorBlockEntity.ConnectionPose> connections) {
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(pos);
        PoseStack anchorPose = new PoseStack();
        applyAnchorPose(anchorPose, facing);
        models.anchor().include(bounds, anchorPose);
        for (PipeAnchorBlockEntity.ConnectionPose connection : connections) {
            PoseStack connectionPose = connectionPose(connection);
            connectionPose.pushPose();
            connectionPose.scale(1.0F, connection.length(), 1.0F);
            connectionPose.translate(0.0D, -0.5D, 0.0D);
            models.pipe().include(bounds, connectionPose);
            connectionPose.popPose();

            connectionPose.pushPose();
            connectionPose.translate(0.0D, connection.ringOffset(), 0.0D);
            models.ring().include(bounds, connectionPose);
            connectionPose.popPose();
        }
        return bounds.build();
    }

    private static List<GpuLayer> buildConnectionLayers(
            PipeAnchorBlockEntity.RenderSnapshot snapshot, ModelSet models,
            int packedLight, int packedOverlay) {
        if (snapshot.connections().isEmpty()) {
            return List.of();
        }
        Set<RenderType> renderTypes = new LinkedHashSet<>();
        models.pipe().layers().forEach(layer -> renderTypes.add(layer.renderType()));
        models.ring().layers().forEach(layer -> renderTypes.add(layer.renderType()));
        List<GpuLayer> built = new ArrayList<>(renderTypes.size());
        int pipeColor = lightenColor(snapshot.color());
        try {
            for (RenderType renderType : renderTypes) {
                BufferBuilder builder = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
                for (PipeAnchorBlockEntity.ConnectionPose connection : snapshot.connections()) {
                    PoseStack connectionPose = connectionPose(connection);
                    connectionPose.pushPose();
                    connectionPose.scale(1.0F, connection.length(), 1.0F);
                    connectionPose.translate(0.0D, -0.5D, 0.0D);
                    models.pipe().emitTinted(renderType, connectionPose, builder,
                            packedLight, packedOverlay, pipeColor);
                    connectionPose.popPose();

                    connectionPose.pushPose();
                    connectionPose.translate(0.0D, connection.ringOffset(), 0.0D);
                    models.ring().emit(renderType, connectionPose, builder, packedLight, packedOverlay);
                    connectionPose.popPose();
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
                built.add(new GpuLayer(renderType, buffer));
            }
            return List.copyOf(built);
        } catch (RuntimeException exception) {
            closeLayersNow(built);
            throw exception;
        }
    }

    private static CapturedModel captureModel(ModelResourceLocation location, BlockState state,
                                              boolean tintedEntityLayer) {
        BakedModel model = MachineModelRenderer.model(location);
        RandomSource random = RandomSource.create(42L);
        List<CapturedLayer> layers = new ArrayList<>();
        List<Float> coordinates = new ArrayList<>();
        random.setSeed(42L);
        for (RenderType sourceType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, coordinates, model, state, null, sourceType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, coordinates, model, state, side, sourceType, random);
            }
            if (!quads.isEmpty()) {
                layers.add(new CapturedLayer(
                        RenderTypeHelper.getEntityRenderType(sourceType, tintedEntityLayer),
                        List.copyOf(quads)));
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

    private static PoseStack connectionPose(PipeAnchorBlockEntity.ConnectionPose connection) {
        PoseStack pose = new PoseStack();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(yQuaternion(connection.yawDegrees()));
        pose.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(connection.xDegrees()), 1.0F, 0.0F, 0.0F)));
        return pose;
    }

    private static void applyAnchorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        rotateAnchor(poseStack, facing);
        poseStack.translate(0.0D, -0.5D, 0.0D);
    }

    private static void drawLayers(List<GpuLayer> layers, PoseStack poseStack) {
        for (GpuLayer layer : layers) {
            if (layer.buffer().isInvalid()) {
                continue;
            }
            layer.renderType().setupRenderState();
            try {
                layer.buffer().bind();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    layer.buffer().drawWithShader(MachineModelRenderer.composeModelView(poseStack),
                            RenderSystem.getProjectionMatrix(), shader);
                }
            } finally {
                VertexBuffer.unbind();
                layer.renderType().clearRenderState();
            }
        }
    }

    private static void discardMachineCache(PipeAnchorBlockEntity anchor) {
        runOnRenderThread(() -> {
            for (PipeAnchorBlockEntityRenderer renderer : rendererInstances()) {
                ConnectionBuffers removed = renderer.connectionBuffers.remove(anchor);
                if (removed != null) {
                    removed.closeNow();
                }
            }
        });
    }

    private static void closeAllGpuCaches() {
        runOnRenderThread(() -> {
            for (PipeAnchorBlockEntityRenderer renderer : rendererInstances()) {
                renderer.connectionBuffers.values().forEach(ConnectionBuffers::closeNow);
                renderer.connectionBuffers.clear();
            }
        });
    }

    private static List<PipeAnchorBlockEntityRenderer> rendererInstances() {
        synchronized (INSTANCES) {
            return List.copyOf(INSTANCES);
        }
    }

    private static void runOnRenderThread(Runnable action) {
        if (RenderSystem.isOnRenderThread()) {
            action.run();
        } else {
            RenderSystem.recordRenderCall(action::run);
        }
    }

    private static void closeLayersNow(List<GpuLayer> layers) {
        if (layers != null) {
            layers.forEach(layer -> layer.buffer().close());
        }
    }

    private static void rotateAnchor(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(MachineModelRenderer.xQuaternion(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
                poseStack.mulPose(MachineModelRenderer.zQuaternion(180.0F));
            }
            case SOUTH -> poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
            case WEST -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
                poseStack.mulPose(MachineModelRenderer.zQuaternion(90.0F));
            }
            case EAST -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
                poseStack.mulPose(MachineModelRenderer.zQuaternion(270.0F));
            }
        }
    }

    private static Quaternionf yQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static int lightenColor(int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        red = (int) (red + (255 - red) * 0.25D);
        green = (int) (green + (255 - green) * 0.25D);
        blue = (int) (blue + (255 - blue) * 0.25D);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static AABB zeroVolumeBounds(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    private record GeometryGeneration(int generation, Map<BlockState, ModelSet> models) {
    }

    private record ModelSet(CapturedModel anchor, CapturedModel pipe, CapturedModel ring) {
    }

    private record CapturedLayer(RenderType renderType, List<BakedQuad> quads) {
    }

    private record CapturedModel(List<CapturedLayer> layers, float[] vertices) {
        private void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            for (CapturedLayer layer : layers) {
                MachineModelRenderer.renderCapturedQuads(layer.quads(), pose,
                        buffers.getBuffer(layer.renderType()), light, overlay);
            }
        }

        private void emit(RenderType type, PoseStack pose, BufferBuilder builder, int light, int overlay) {
            for (CapturedLayer layer : layers) {
                if (layer.renderType().equals(type)) {
                    MachineModelRenderer.renderCapturedQuads(layer.quads(), pose, builder, light, overlay);
                }
            }
        }

        private void emitTinted(RenderType type, PoseStack pose, BufferBuilder builder,
                                int light, int overlay, int color) {
            for (CapturedLayer layer : layers) {
                if (layer.renderType().equals(type)) {
                    MachineModelRenderer.emitCapturedQuadsTinted(
                            layer.quads(), pose, builder, light, overlay, color);
                }
            }
        }

        private void include(RenderBounds.FloatBuilder bounds, PoseStack pose) {
            for (int index = 0; index < vertices.length; index += 3) {
                bounds.include(pose.last().pose().transformPosition(new Vector3f(
                        vertices[index], vertices[index + 1], vertices[index + 2])));
            }
        }
    }

    private record GpuLayer(RenderType renderType, VertexBuffer buffer) {
    }

    private static final class ConnectionBuffers {
        private PipeAnchorBlockEntity.RenderSnapshot snapshot;
        private GeometryGeneration generation;
        private int packedLight;
        private int packedOverlay;
        private List<GpuLayer> layers = List.of();

        private void prepare(PipeAnchorBlockEntity.RenderSnapshot requestedSnapshot,
                             GeometryGeneration requestedGeneration, ModelSet models,
                             int requestedLight, int requestedOverlay) {
            if (snapshot == requestedSnapshot && generation == requestedGeneration
                    && packedLight == requestedLight && packedOverlay == requestedOverlay) {
                return;
            }
            List<GpuLayer> replacement = buildConnectionLayers(
                    requestedSnapshot, models, requestedLight, requestedOverlay);
            closeLayersNow(layers);
            snapshot = requestedSnapshot;
            generation = requestedGeneration;
            packedLight = requestedLight;
            packedOverlay = requestedOverlay;
            layers = replacement;
        }

        private List<GpuLayer> layers() {
            return layers;
        }

        private void closeNow() {
            closeLayersNow(layers);
            layers = List.of();
            snapshot = null;
            generation = null;
        }
    }
}
