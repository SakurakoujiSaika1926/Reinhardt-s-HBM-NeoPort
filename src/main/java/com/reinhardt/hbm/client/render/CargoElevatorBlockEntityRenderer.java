package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.reinhardt.hbm.block.CargoElevatorBlock;
import com.reinhardt.hbm.blockentity.CargoElevatorBlockEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Cargo-elevator geometry is frozen after model bake. Each loaded elevator owns
 * its own GPU buffers: guides are rebuilt only when height/light changes and
 * the piston stack only when the exact legacy loop's segment count changes.
 */
public final class CargoElevatorBlockEntityRenderer implements LongRangeBlockEntityRenderer<CargoElevatorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/cargo_elevator_base");
    private static final ModelResourceLocation PLATFORM = MachineModelRenderer.standalone("block/cargo_elevator_platform");
    private static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/cargo_elevator_piston");
    private static final ModelResourceLocation GUIDES = MachineModelRenderer.standalone("block/cargo_elevator_guides");
    private static final Set<CargoElevatorBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeneration;

    /** Identity keys prevent two elevators at equal coordinates in different levels sharing GPU state. */
    private final Map<CargoElevatorBlockEntity, MachineBuffers> machineBuffers = new IdentityHashMap<>();

    public CargoElevatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        CargoElevatorBlockEntity.setClientRenderSnapshotUpdater(
                CargoElevatorBlockEntityRenderer::updateRenderSnapshot);
        CargoElevatorBlockEntity.setClientGpuCacheDisposer(
                CargoElevatorBlockEntityRenderer::discardMachineCache);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(PLATFORM);
        event.register(PISTON);
        event.register(GUIDES);
    }

    @Override
    public void render(CargoElevatorBlockEntity elevator, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        CargoElevatorBlockEntity.RenderSnapshot snapshot = elevator.renderSnapshot();
        if (!snapshot.core() || !(snapshot.geometryToken() instanceof GeometryGeneration generation)
                || !generation.hasGeometry()) {
            return;
        }

        double extension = snapshot.extension(partialTick);
        int pistonSegments = pistonSegmentCount(extension);
        MachineBuffers buffers = this.machineBuffers.computeIfAbsent(elevator, ignored -> new MachineBuffers());
        buffers.prepare(generation, snapshot.height(), snapshot.renderPlatform(), pistonSegments,
                packedLight, packedOverlay);

        poseStack.pushPose();
        applyRootPose(poseStack);
        if (snapshot.renderPlatform()) {
            drawLayers(buffers.base(), poseStack);
            poseStack.pushPose();
            poseStack.translate(0.0D, extension, 0.0D);
            drawLayers(buffers.platform(), poseStack);
            drawLayers(buffers.pistons(), poseStack);
            poseStack.popPose();
        }
        drawLayers(buffers.guides(), poseStack);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CargoElevatorBlockEntity elevator) {
        return elevator.renderSnapshot().bounds();
    }

    /** Stops an in-progress model generation from pairing old bounds with newly baked resources. */
    static void beginGeometryReload() {
        geometryGeneration = null;
        closeAllGpuCaches();
        CargoElevatorBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Captures immutable render layers, quads, and face-used float vertices from the completed bake. */
    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(CargoElevatorBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        BlockState state = HbmBlocks.CARGO_ELEVATOR.get().defaultBlockState()
                .setValue(CargoElevatorBlock.CORE, true);
        GeometryGeneration next = new GeometryGeneration(
                ++nextGeneration,
                captureModel(BASE, state),
                captureModel(PLATFORM, state),
                captureModel(PISTON, state),
                captureModel(GUIDES, state));
        geometryGeneration = next;
        closeAllGpuCaches();
        CargoElevatorBlockEntity.setClientRenderSnapshotUpdater(
                CargoElevatorBlockEntityRenderer::updateRenderSnapshot);
        CargoElevatorBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Releases GPU buffers on logout while retaining the valid CPU geometry generation. */
    static void closeGpuCachesOnLogout() {
        closeAllGpuCaches();
    }

    private static void updateRenderSnapshot(CargoElevatorBlockEntity elevator) {
        GeometryGeneration generation = geometryGeneration;
        CargoElevatorBlockEntity.RenderInputs inputs = elevator.captureRenderInputs();
        int height = Math.max(0, inputs.height());
        double previousExtension = legalExtension(inputs.previousExtension(), height);
        double extension = legalExtension(inputs.extension(), height);
        long revision = CargoElevatorBlockEntity.clientRenderSnapshotRevision();
        CargoElevatorBlockEntity.RenderSnapshot previous = elevator.renderSnapshot();
        Object token = generation;

        if (previous.geometryToken() == token
                && previous.core() == inputs.core()
                && previous.height() == height
                && previous.renderPlatform() == inputs.renderPlatform()
                && previous.previousExtension() == previousExtension
                && previous.extension() == extension
                && previous.revision() == revision) {
            return;
        }

        AABB bounds = generation != null && inputs.core()
                ? generation.buildBounds(elevator.getBlockPos(), height, inputs.renderPlatform(),
                previousExtension, extension)
                : zeroVolumeBounds(elevator.getBlockPos());
        elevator.publishClientRenderSnapshot(new CargoElevatorBlockEntity.RenderSnapshot(
                token, inputs.core(), height, inputs.renderPlatform(),
                previousExtension, extension, bounds, revision));
    }

    private static double legalExtension(double extension, int height) {
        return Double.isFinite(extension) ? Mth.clamp(extension, 0.0D, height) : 0.0D;
    }

    /** ceil(extension + 1) is exactly the number produced by the legacy i < extension + 1 loop. */
    private static int pistonSegmentCount(double extension) {
        return Mth.ceil(extension + 1.0D);
    }

    private static void applyRootPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }

    private static CapturedModel captureModel(ModelResourceLocation location, BlockState state) {
        BakedModel model = MachineModelRenderer.model(location);
        RandomSource random = RandomSource.create(42L);
        List<CapturedLayer> layers = new ArrayList<>();
        List<Float> coordinates = new ArrayList<>();
        random.setSeed(42L);
        for (RenderType sourceRenderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, coordinates, model, state, null, sourceRenderType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, coordinates, model, state, side, sourceRenderType, random);
            }
            if (!quads.isEmpty()) {
                layers.add(new CapturedLayer(
                        RenderTypeHelper.getEntityRenderType(sourceRenderType, false),
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

    private static List<GpuLayer> buildRepeated(CapturedModel model, int count, int verticalStep,
                                                int packedLight, int packedOverlay) {
        if (count <= 0 || model.layers().isEmpty()) {
            return List.of();
        }
        List<GpuLayer> built = new ArrayList<>(model.layers().size());
        try {
            for (CapturedLayer layer : model.layers()) {
                BufferBuilder builder = Tesselator.getInstance().begin(
                        layer.renderType().mode(), layer.renderType().format());
                PoseStack buildPose = new PoseStack();
                for (int index = 0; index < count; index++) {
                    MachineModelRenderer.renderCapturedQuads(
                            layer.quads(), buildPose, builder, packedLight, packedOverlay);
                    buildPose.translate(0.0D, verticalStep, 0.0D);
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
                built.add(new GpuLayer(layer.renderType(), buffer));
            }
            return List.copyOf(built);
        } catch (RuntimeException exception) {
            closeLayers(built);
            throw exception;
        }
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
                    layer.buffer().drawWithShader(
                            MachineModelRenderer.composeModelView(poseStack),
                            RenderSystem.getProjectionMatrix(), shader);
                }
            } finally {
                VertexBuffer.unbind();
                layer.renderType().clearRenderState();
            }
        }
    }

    private static void discardMachineCache(CargoElevatorBlockEntity elevator) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> discardMachineCache(elevator));
            return;
        }
        List<CargoElevatorBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (CargoElevatorBlockEntityRenderer renderer : renderers) {
            renderer.closeMachineCache(elevator);
        }
    }

    private static void closeAllGpuCaches() {
        List<CargoElevatorBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (CargoElevatorBlockEntityRenderer renderer : renderers) {
            renderer.closeAllMachineCaches();
        }
    }

    private void closeMachineCache(CargoElevatorBlockEntity elevator) {
        MachineBuffers removed = this.machineBuffers.remove(elevator);
        if (removed != null) {
            removed.close();
        }
    }

    private void closeAllMachineCaches() {
        for (MachineBuffers buffers : this.machineBuffers.values()) {
            buffers.close();
        }
        this.machineBuffers.clear();
    }

    private static void closeLayers(List<GpuLayer> layers) {
        for (GpuLayer layer : layers) {
            layer.buffer().close();
        }
    }

    private static AABB zeroVolumeBounds(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    private record GeometryGeneration(int generation, CapturedModel base, CapturedModel platform,
                                      CapturedModel piston, CapturedModel guides) {
        private boolean hasGeometry() {
            return base.hasGeometry() || platform.hasGeometry()
                    || piston.hasGeometry() || guides.hasGeometry();
        }

        /**
         * Uses the same float PoseStack sequence and face-used vertices as the
         * renderer. Translation is the only interpolated motion, so endpoint
         * extrema are exact. A newly appearing piston's open lower endpoint is
         * included as its closure; that segment is emitted at the same float
         * position immediately after crossing the legacy loop threshold.
         */
        private AABB buildBounds(BlockPos pos, int height, boolean renderPlatform,
                                 double previousExtension, double extension) {
            RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(pos);
            PoseStack root = new PoseStack();
            applyRootPose(root);

            PoseStack localIdentity = new PoseStack();
            if (renderPlatform) {
                base.includeVboTransformed(bounds, localIdentity, root);
                double minimum = Math.min(previousExtension, extension);
                double maximum = Math.max(previousExtension, extension);

                includeTranslated(platform, bounds, localIdentity, root, minimum);
                if (maximum != minimum) {
                    includeTranslated(platform, bounds, localIdentity, root, maximum);
                }

                int minimumCount = pistonSegmentCount(minimum);
                int maximumCount = pistonSegmentCount(maximum);
                includePistonInterpolationEnvelope(piston, bounds, root,
                        minimum, maximum, minimumCount, maximumCount);
            }

            PoseStack guideLocal = new PoseStack();
            for (int index = 0; index <= height; index++) {
                guides.includeVboTransformed(bounds, guideLocal, root);
                guideLocal.translate(0.0D, 1.0D, 0.0D);
            }
            return bounds.build();
        }

        private static void includeTranslated(CapturedModel model, RenderBounds.FloatBuilder bounds,
                                              PoseStack localPose, PoseStack root, double extension) {
            root.pushPose();
            root.translate(0.0D, extension, 0.0D);
            model.includeVboTransformed(bounds, localPose, root);
            root.popPose();
        }

        private static void includePistonInterpolationEnvelope(CapturedModel model,
                                                               RenderBounds.FloatBuilder bounds,
                                                               PoseStack root, double minimum,
                                                               double maximum, int minimumCount,
                                                               int maximumCount) {
            PoseStack pistonLocal = new PoseStack();
            for (int index = 0; index < maximumCount; index++) {
                // Existing segments are present at minimum. A segment added
                // later first appears immediately above extension=index-1;
                // include that open endpoint's exact float closure.
                double lowerExtension = index < minimumCount ? minimum : index - 1.0D;
                includeTranslated(model, bounds, pistonLocal, root, lowerExtension);
                if (maximum != lowerExtension) {
                    includeTranslated(model, bounds, pistonLocal, root, maximum);
                }
                pistonLocal.translate(0.0D, -1.0D, 0.0D);
            }
        }
    }

    private record CapturedModel(List<CapturedLayer> layers, float[] vertices) {
        private boolean hasGeometry() {
            return vertices.length != 0;
        }

        /** Mirrors VBO upload first, then the per-frame model-view pose. */
        private void includeVboTransformed(RenderBounds.FloatBuilder bounds,
                                           PoseStack localPose, PoseStack runtimePose) {
            for (int index = 0; index < vertices.length; index += 3) {
                Vector3f uploaded = localPose.last().pose().transformPosition(new Vector3f(
                        vertices[index], vertices[index + 1], vertices[index + 2]));
                bounds.include(runtimePose.last().pose().transformPosition(uploaded));
            }
        }
    }

    private record CapturedLayer(RenderType renderType, List<BakedQuad> quads) {
    }

    private record GpuLayer(RenderType renderType, VertexBuffer buffer) {
    }

    private static final class MachineBuffers {
        private GeometryGeneration generation;
        private int packedLight;
        private int packedOverlay;
        private int guideHeight = -1;
        private int pistonSegments = -1;
        private List<GpuLayer> base;
        private List<GpuLayer> platform;
        private List<GpuLayer> pistons;
        private List<GpuLayer> guides;

        private void prepare(GeometryGeneration requestedGeneration, int height, boolean renderPlatform,
                             int requestedPistonSegments, int requestedLight, int requestedOverlay) {
            if (this.generation != requestedGeneration
                    || this.packedLight != requestedLight
                    || this.packedOverlay != requestedOverlay
                    || hasInvalidBuffer()) {
                close();
                this.generation = requestedGeneration;
                this.packedLight = requestedLight;
                this.packedOverlay = requestedOverlay;
            }

            if (this.guides == null || this.guideHeight != height) {
                closeLayers(this.guides);
                this.guides = buildRepeated(requestedGeneration.guides(), height + 1, 1,
                        requestedLight, requestedOverlay);
                this.guideHeight = height;
            }

            if (renderPlatform) {
                if (this.base == null) {
                    this.base = buildRepeated(requestedGeneration.base(), 1, 0,
                            requestedLight, requestedOverlay);
                }
                if (this.platform == null) {
                    this.platform = buildRepeated(requestedGeneration.platform(), 1, 0,
                            requestedLight, requestedOverlay);
                }
                if (this.pistons == null || this.pistonSegments != requestedPistonSegments) {
                    closeLayers(this.pistons);
                    this.pistons = buildRepeated(requestedGeneration.piston(), requestedPistonSegments, -1,
                            requestedLight, requestedOverlay);
                    this.pistonSegments = requestedPistonSegments;
                }
            }
        }

        private List<GpuLayer> base() {
            return base == null ? List.of() : base;
        }

        private List<GpuLayer> platform() {
            return platform == null ? List.of() : platform;
        }

        private List<GpuLayer> pistons() {
            return pistons == null ? List.of() : pistons;
        }

        private List<GpuLayer> guides() {
            return guides == null ? List.of() : guides;
        }

        private boolean hasInvalidBuffer() {
            return hasInvalidBuffer(this.base) || hasInvalidBuffer(this.platform)
                    || hasInvalidBuffer(this.pistons) || hasInvalidBuffer(this.guides);
        }

        private static boolean hasInvalidBuffer(List<GpuLayer> layers) {
            return layers != null && layers.stream().anyMatch(layer -> layer.buffer().isInvalid());
        }

        private void close() {
            closeLayers(this.base);
            closeLayers(this.platform);
            closeLayers(this.pistons);
            closeLayers(this.guides);
            this.base = null;
            this.platform = null;
            this.pistons = null;
            this.guides = null;
            this.generation = null;
            this.guideHeight = -1;
            this.pistonSegments = -1;
        }

        private static void closeLayers(List<GpuLayer> layers) {
            if (layers != null) {
                CargoElevatorBlockEntityRenderer.closeLayers(layers);
            }
        }
    }
}
