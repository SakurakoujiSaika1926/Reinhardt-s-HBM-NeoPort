package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ExcavatorBlock;
import com.reinhardt.hbm.blockentity.ExcavatorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ExcavatorBlockEntityRenderer implements LongRangeBlockEntityRenderer<ExcavatorBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_excavator_main");
    private static final ModelResourceLocation CRUSHER_1 = MachineModelRenderer.standalone("block/machine_excavator_crusher1");
    private static final ModelResourceLocation CRUSHER_2 = MachineModelRenderer.standalone("block/machine_excavator_crusher2");
    private static final ModelResourceLocation DRILLBIT = MachineModelRenderer.standalone("block/machine_excavator_drillbit");
    private static final ModelResourceLocation SHAFT = MachineModelRenderer.standalone("block/machine_excavator_shaft");
    private static final ResourceLocation COBBLE = ReinhardtsHBM.id("textures/models/machines/cobblestone.png");
    private static final ResourceLocation GRAVEL = ReinhardtsHBM.id("textures/models/machines/gravel.png");
    private static final Set<ExcavatorBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeneration;
    private final Map<ExcavatorBlockEntity, ShaftBuffers> shaftBuffers = new IdentityHashMap<>();
    private static final double FLOAT_UNIT_ROUNDOFF = 0x1.0p-24;
    private static final double LERP_GAMMA = gamma(3);
    private static final double POSE_GAMMA = gamma(32);

    public ExcavatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        ExcavatorBlockEntity.setClientRenderSnapshotUpdater(
                ExcavatorBlockEntityRenderer::updateRenderSnapshot);
        ExcavatorBlockEntity.setClientGpuCacheDisposer(
                ExcavatorBlockEntityRenderer::discardMachineCache);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(CRUSHER_1);
        event.register(CRUSHER_2);
        event.register(DRILLBIT);
        event.register(SHAFT);
    }

    static void beginGeometryReload() {
        geometryGeneration = null;
        closeAllGpuCaches();
    }

    /** Captures immutable layers/quads and face-used float vertices after model baking. */
    static void rebuildGeometryGenerationAfterBake() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(ExcavatorBlockEntityRenderer::rebuildGeometryGenerationAfterBake);
            return;
        }
        Map<BlockState, ModelSet> models = new HashMap<>();
        for (BlockState state : HbmBlocks.MACHINE_EXCAVATOR.get().getStateDefinition().getPossibleStates()) {
            models.put(state, new ModelSet(
                    captureModel(MAIN, state), captureModel(CRUSHER_1, state),
                    captureModel(CRUSHER_2, state), captureModel(DRILLBIT, state),
                    captureModel(SHAFT, state)));
        }
        geometryGeneration = new GeometryGeneration(++nextGeneration, Map.copyOf(models));
        closeAllGpuCaches();
        ExcavatorBlockEntity.setClientRenderSnapshotUpdater(
                ExcavatorBlockEntityRenderer::updateRenderSnapshot);
    }

    static void closeGpuCachesOnLogout() {
        closeAllGpuCaches();
    }

    @Override
    public void render(ExcavatorBlockEntity excavator, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ExcavatorBlockEntity.RenderSnapshot snapshot = excavator.renderSnapshot();
        GeometryGeneration current = geometryGeneration;
        if (snapshot.geometryToken() != current || current == null) {
            return;
        }
        ExcavatorBlockEntity.RenderInputs inputs = snapshot.inputs();
        BlockState state = inputs.state();
        ModelSet models = current.models().get(state);
        if (models == null) {
            return;
        }
        poseStack.pushPose();
        applyExcavatorPose(poseStack, excavatorFacing(state));
        models.main().render(poseStack, bufferSource, packedLight, packedOverlay);

        float crusher = inputs.crusherRotation(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 2.8125F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-crusher));
        poseStack.translate(0.0F, -2.0F, -2.8125F);
        models.crusher1().render(poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 2.1875F);
        poseStack.mulPose(Axis.XP.rotationDegrees(crusher));
        poseStack.translate(0.0F, -2.0F, -2.1875F);
        models.crusher2().render(poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-inputs.drillRotation(partialTick)));
        float ext = inputs.drillExtension(partialTick);
        poseStack.translate(0.0F, -ext, 0.0F);
        models.drillbit().render(poseStack, bufferSource, packedLight, packedOverlay);
        int shaftCount = shaftSegmentCount(ext);
        ShaftBuffers buffers = shaftBuffers.computeIfAbsent(excavator, ignored -> new ShaftBuffers());
        buffers.prepare(current, models.shaft(), state, shaftCount, packedLight, packedOverlay);
        drawLayers(buffers.layers(), poseStack);
        poseStack.popPose();

        if (inputs.chuteTimer() > 0) {
            renderChute(inputs.crusherEnabled(), poseStack, bufferSource, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ExcavatorBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    /** Publishes one immutable animation/bounds view from the client level thread. */
    private static void updateRenderSnapshot(ExcavatorBlockEntity excavator) {
        ExcavatorBlockEntity.RenderInputs inputs = excavator.captureRenderInputs();
        GeometryGeneration generation = geometryGeneration;
        BlockState state = inputs.state();
        Direction facing = excavatorFacing(state);
        BlockPos pos = excavator.getBlockPos();
        ModelSet models = generation == null ? null : generation.models().get(state);
        AABB bounds = models == null
                ? zeroVolumeBounds(pos)
                : buildBounds(pos, models, facing, inputs);
        excavator.publishClientRenderSnapshot(
                new ExcavatorBlockEntity.RenderSnapshot(generation, inputs, bounds));
    }

    private static double gamma(int operations) {
        double product = operations * FLOAT_UNIT_ROUNDOFF;
        return product / (1.0D - product);
    }

    private static Direction excavatorFacing(BlockState state) {
        if (!state.hasProperty(ExcavatorBlock.FACING)) {
            throw new IllegalStateException("Excavator state is missing its dedicated facing property");
        }
        return state.getValue(ExcavatorBlock.FACING);
    }

    private static void applyExcavatorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Excavator facing must be horizontal: " + facing);
        }
        poseStack.translate(0.0F, -3.0F, 0.0F);
    }

    private static AABB buildBounds(BlockPos pos, ModelSet models, Direction facing,
                                    ExcavatorBlockEntity.RenderInputs inputs) {
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(pos);
        PoseStack root = new PoseStack();
        applyExcavatorPose(root, facing);
        models.main().include(bounds, root);

        includeXAxisRotationEnvelope(bounds, root, models.crusher1(), 2.0F, 2.8125F);
        includeXAxisRotationEnvelope(bounds, root, models.crusher2(), 2.0F, 2.1875F);

        double[] extension = extensionEnvelope(inputs.prevDrillExtension(), inputs.drillExtension());
        includeYAxisRotationEnvelope(bounds, root, models.drillbit(), extension[0], 0.0F);
        if (extension[1] != extension[0]) {
            includeYAxisRotationEnvelope(bounds, root, models.drillbit(), extension[1], 0.0F);
        }
        int maximumSegments = shaftSegmentCount((float) extension[1]);
        for (int index = 0; index < maximumSegments; index++) {
            float verticalOffset = 2.0F * index;
            includeYAxisRotationEnvelope(bounds, root, models.shaft(), extension[0], verticalOffset);
            if (extension[1] != extension[0]) {
                includeYAxisRotationEnvelope(bounds, root, models.shaft(), extension[1], verticalOffset);
            }
        }

        if (inputs.chuteTimer() > 0) {
            bounds.includeBox(root, -0.125F, 2.0F, 2.375F, 0.125F, 3.0F, 2.625F);
            float outputWidth = inputs.crusherEnabled() ? 0.5F : 0.25F;
            bounds.includeBox(root, -outputWidth, 1.0F, 2.4375F,
                    outputWidth, 2.0F, 2.5625F);
        }
        return bounds.build();
    }

    /** Continuous X-axis sweep, bounded from face-used vertices and float-operation gamma. */
    private static void includeXAxisRotationEnvelope(RenderBounds.FloatBuilder bounds, PoseStack root,
                                                     CapturedModel model, float pivotY, float pivotZ) {
        for (int index = 0; index < model.vertices().length; index += 3) {
            float x = model.vertices()[index];
            float dy = model.vertices()[index + 1] - pivotY;
            float dz = model.vertices()[index + 2] - pivotZ;
            float radius = outwardRadius(dy, dz);
            for (float y : new float[] {pivotY - radius, pivotY + radius}) {
                for (float z : new float[] {pivotZ - radius, pivotZ + radius}) {
                    bounds.include(root.last().pose().transformPosition(new Vector3f(x, y, z)));
                }
            }
        }
    }

    /** Continuous Y-axis sweep; Y is invariant, so extension endpoints are sufficient. */
    private static void includeYAxisRotationEnvelope(RenderBounds.FloatBuilder bounds, PoseStack root,
                                                     CapturedModel model, double extension,
                                                     float verticalOffset) {
        for (int index = 0; index < model.vertices().length; index += 3) {
            float x = model.vertices()[index];
            float y = (float) (model.vertices()[index + 1] - extension + verticalOffset);
            float z = model.vertices()[index + 2];
            float radius = outwardRadius(x, z);
            for (float rotatedX : new float[] {-radius, radius}) {
                for (float rotatedZ : new float[] {-radius, radius}) {
                    bounds.include(root.last().pose().transformPosition(
                            new Vector3f(rotatedX, y, rotatedZ)));
                }
            }
        }
    }

    private static float outwardRadius(float first, float second) {
        double exactL1 = Math.abs((double) first) + Math.abs((double) second);
        return Math.nextUp((float) (exactL1 * (1.0D + POSE_GAMMA)));
    }

    private static double[] extensionEnvelope(float previous, float current) {
        double magnitude = Math.abs((double) previous) + Math.abs((double) current - previous);
        double error = LERP_GAMMA * magnitude;
        return new double[] {
                Math.nextDown(Math.min(previous, current) - error),
                Math.nextUp(Math.max(previous, current) + error)
        };
    }

    /** Exact count of the legacy {@code while (ext >= -1.5F) { ext -= 2F; }} loop. */
    private static int shaftSegmentCount(float extension) {
        if (!Float.isFinite(extension) || extension < -1.5F) {
            return 0;
        }
        return (int) Math.floor(((double) extension + 1.5D) / 2.0D) + 1;
    }

    private static CapturedModel captureModel(ModelResourceLocation location, BlockState state) {
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
                        RenderTypeHelper.getEntityRenderType(sourceType, false), List.copyOf(quads)));
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

    private static List<GpuLayer> buildShaftLayers(CapturedModel shaft, int count,
                                                   int packedLight, int packedOverlay) {
        if (count <= 0) {
            return List.of();
        }
        List<GpuLayer> built = new ArrayList<>(shaft.layers().size());
        try {
            for (CapturedLayer layer : shaft.layers()) {
                BufferBuilder builder = Tesselator.getInstance().begin(
                        layer.renderType().mode(), layer.renderType().format());
                PoseStack local = new PoseStack();
                for (int index = 0; index < count; index++) {
                    MachineModelRenderer.renderCapturedQuads(
                            layer.quads(), local, builder, packedLight, packedOverlay);
                    local.translate(0.0F, 2.0F, 0.0F);
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
            closeLayersNow(built);
            throw exception;
        }
    }

    private static void drawLayers(List<GpuLayer> layers, PoseStack pose) {
        for (GpuLayer layer : layers) {
            if (layer.buffer().isInvalid()) {
                continue;
            }
            layer.renderType().setupRenderState();
            try {
                layer.buffer().bind();
                ShaderInstance shader = RenderSystem.getShader();
                if (shader != null) {
                    layer.buffer().drawWithShader(MachineModelRenderer.composeModelView(pose),
                            RenderSystem.getProjectionMatrix(), shader);
                }
            } finally {
                VertexBuffer.unbind();
                layer.renderType().clearRenderState();
            }
        }
    }

    private static void discardMachineCache(ExcavatorBlockEntity excavator) {
        runOnRenderThread(() -> {
            for (ExcavatorBlockEntityRenderer renderer : rendererInstances()) {
                ShaftBuffers removed = renderer.shaftBuffers.remove(excavator);
                if (removed != null) {
                    removed.closeNow();
                }
            }
        });
    }

    private static void closeAllGpuCaches() {
        runOnRenderThread(() -> {
            for (ExcavatorBlockEntityRenderer renderer : rendererInstances()) {
                renderer.shaftBuffers.values().forEach(ShaftBuffers::closeNow);
                renderer.shaftBuffers.clear();
            }
        });
    }

    private static List<ExcavatorBlockEntityRenderer> rendererInstances() {
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

    private static AABB zeroVolumeBounds(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static void renderChute(boolean crusherEnabled, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedOverlay) {
        double speed = 250.0D;
        float dropU = (float) (-(System.currentTimeMillis() % (long) speed) / speed);
        float dropL = dropU + 4.0F;

        VertexConsumer cobble = bufferSource.getBuffer(RenderType.entityCutoutNoCull(COBBLE));
        renderDropColumn(poseStack, cobble, 0.125D, 0.125D, 1.0F, 1.0F, 3.0D, 2.0D, dropU, dropL, packedOverlay);

        VertexConsumer output = bufferSource.getBuffer(RenderType.entityCutoutNoCull(
                crusherEnabled ? GRAVEL : COBBLE));
        renderDropColumn(poseStack, output, crusherEnabled ? 0.5D : 0.25D, 0.0625D,
                crusherEnabled ? 4.0F : 2.0F, 0.5F, 2.0D, 1.0D,
                dropU, dropL, packedOverlay);
    }

    private static void renderDropColumn(
            PoseStack poseStack,
            VertexConsumer consumer,
            double widthX,
            double widthZ,
            float faceUMax,
            float sideUMax,
            double top,
            double bottom,
            float dropU,
            float dropL,
            int packedOverlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        quad(consumer, pose, widthX, top, 2.5D + widthZ, -widthX, top, 2.5D + widthZ, -widthX, bottom, 2.5D + widthZ, widthX, bottom, 2.5D + widthZ, 0.0F, dropU, faceUMax, dropL, packedOverlay);
        quad(consumer, pose, -widthX, top, 2.5D - widthZ, widthX, top, 2.5D - widthZ, widthX, bottom, 2.5D - widthZ, -widthX, bottom, 2.5D - widthZ, 0.0F, dropU, faceUMax, dropL, packedOverlay);
        quad(consumer, pose, -widthX, top, 2.5D + widthZ, -widthX, top, 2.5D - widthZ, -widthX, bottom, 2.5D - widthZ, -widthX, bottom, 2.5D + widthZ, 0.0F, dropU, sideUMax, dropL, packedOverlay);
        quad(consumer, pose, widthX, top, 2.5D - widthZ, widthX, top, 2.5D + widthZ, widthX, bottom, 2.5D + widthZ, widthX, bottom, 2.5D - widthZ, 0.0F, dropU, sideUMax, dropL, packedOverlay);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double ax,
            double ay,
            double az,
            double bx,
            double by,
            double bz,
            double cx,
            double cy,
            double cz,
            double dx,
            double dy,
            double dz,
            float uMin,
            float vMin,
            float uMax,
            float vMax,
            int packedOverlay
    ) {
        vertex(consumer, pose, ax, ay, az, uMin, vMin, packedOverlay);
        vertex(consumer, pose, bx, by, bz, uMax, vMin, packedOverlay);
        vertex(consumer, pose, cx, cy, cz, uMax, vMax, packedOverlay);
        vertex(consumer, pose, dx, dy, dz, uMin, vMax, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float u, float v, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(0x00F000F0)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private record GeometryGeneration(int generation, Map<BlockState, ModelSet> models) {
    }

    private record ModelSet(CapturedModel main, CapturedModel crusher1, CapturedModel crusher2,
                            CapturedModel drillbit, CapturedModel shaft) {
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

        private void include(RenderBounds.FloatBuilder bounds, PoseStack pose) {
            for (int index = 0; index < vertices.length; index += 3) {
                bounds.include(pose.last().pose().transformPosition(new Vector3f(
                        vertices[index], vertices[index + 1], vertices[index + 2])));
            }
        }
    }

    private record GpuLayer(RenderType renderType, VertexBuffer buffer) {
    }

    private static final class ShaftBuffers {
        private GeometryGeneration generation;
        private CapturedModel shaft;
        private BlockState state;
        private int count = -1;
        private int packedLight;
        private int packedOverlay;
        private List<GpuLayer> layers = List.of();

        private void prepare(GeometryGeneration requestedGeneration, CapturedModel requestedShaft,
                             BlockState requestedState, int requestedCount,
                             int requestedLight, int requestedOverlay) {
            if (generation == requestedGeneration && shaft == requestedShaft && state == requestedState
                    && count == requestedCount && packedLight == requestedLight
                    && packedOverlay == requestedOverlay) {
                return;
            }
            List<GpuLayer> replacement = buildShaftLayers(
                    requestedShaft, requestedCount, requestedLight, requestedOverlay);
            closeLayersNow(layers);
            generation = requestedGeneration;
            shaft = requestedShaft;
            state = requestedState;
            count = requestedCount;
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
            generation = null;
            shaft = null;
            state = null;
            count = -1;
        }
    }

}
