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
import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.blockentity.PowerPylonWireLayout;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class PowerPylonBlockEntityRenderer implements LongRangeBlockEntityRenderer<PowerPylonBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    static final ModelResourceLocation RED_CONNECTOR = MachineModelRenderer.standalone("block/red_connector_world");
    static final ModelResourceLocation CONNECTOR_RED_SUPER = MachineModelRenderer.standalone("block/connector_red_super_world");
    static final ModelResourceLocation RED_PYLON_STEEL = MachineModelRenderer.standalone("block/red_pylon_small_steel_world");
    static final ModelResourceLocation RED_PYLON_LARGE = MachineModelRenderer.standalone("block/red_pylon_large_world");
    static final ModelResourceLocation RED_PYLON_MEDIUM_WOOD = MachineModelRenderer.standalone("block/red_pylon_medium_wood_world");
    static final ModelResourceLocation RED_PYLON_MEDIUM_WOOD_TRANSFORMER = MachineModelRenderer.standalone("block/red_pylon_medium_transformer_world");
    static final ModelResourceLocation RED_PYLON_MEDIUM_STEEL = MachineModelRenderer.standalone("block/red_pylon_steel_world");
    static final ModelResourceLocation RED_PYLON_MEDIUM_STEEL_TRANSFORMER = MachineModelRenderer.standalone("block/red_pylon_steel_transformer_world");
    static final ModelResourceLocation SUBSTATION = MachineModelRenderer.standalone("block/substation_world");
    private static final ResourceLocation WIRE_TEXTURE = ReinhardtsHBM.id("textures/models/network/wire.png");
    private static final ResourceLocation WIRE_GREYSCALE_TEXTURE = ReinhardtsHBM.id("textures/models/network/wire_greyscale.png");
    private static final ResourceLocation RED_PYLON_TEXTURE =
            ReinhardtsHBM.id("textures/models/network/modelpylon.png");
    private static final Set<PowerPylonBlockEntityRenderer> INSTANCES =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private final Map<PowerPylonBlockEntity, CachedWireGeometry> wireGeometryCache = new IdentityHashMap<>();

    public PowerPylonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        INSTANCES.add(this);
    }

    static List<ModelResourceLocation> geometryModels() {
        return List.of(
                RED_CONNECTOR,
                CONNECTOR_RED_SUPER,
                RED_PYLON_STEEL,
                RED_PYLON_MEDIUM_WOOD,
                RED_PYLON_MEDIUM_WOOD_TRANSFORMER,
                RED_PYLON_MEDIUM_STEEL,
                RED_PYLON_MEDIUM_STEEL_TRANSFORMER,
                RED_PYLON_LARGE,
                SUBSTATION
        );
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RED_CONNECTOR);
        event.register(CONNECTOR_RED_SUPER);
        event.register(RED_PYLON_STEEL);
        event.register(RED_PYLON_LARGE);
        event.register(RED_PYLON_MEDIUM_WOOD);
        event.register(RED_PYLON_MEDIUM_WOOD_TRANSFORMER);
        event.register(RED_PYLON_MEDIUM_STEEL);
        event.register(RED_PYLON_MEDIUM_STEEL_TRANSFORMER);
        event.register(SUBSTATION);
    }

    static void invalidateWireCaches() {
        for (PowerPylonBlockEntityRenderer renderer : rendererInstances()) {
            renderer.closeWireCache();
        }
    }

    static void invalidateWireCaches(net.minecraft.client.multiplayer.ClientLevel level,
                                     net.minecraft.world.level.ChunkPos chunkPos) {
        for (PowerPylonBlockEntityRenderer renderer : rendererInstances()) {
            renderer.closeWireCache(level, chunkPos);
        }
    }

    private static List<PowerPylonBlockEntityRenderer> rendererInstances() {
        synchronized (INSTANCES) {
            return List.copyOf(INSTANCES);
        }
    }

    @Override
    public void render(PowerPylonBlockEntity pylon, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PowerPylonBlockEntity.RenderSnapshot snapshot = pylon.renderSnapshot();
        renderModel(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
        renderLines(pylon, snapshot, poseStack, bufferSource, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(PowerPylonBlockEntity blockEntity) {
        return blockEntity.getRenderBoundingBox();
    }

    private void renderModel(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (snapshot.kind()) {
            case RED_CONNECTOR -> renderRedConnector(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case CONNECTOR_RED_SUPER -> renderConnectorRedSuper(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON -> renderLegacyRedPylon(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_STEEL -> renderRedPylonSteel(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_WOOD -> renderRedPylonMediumWood(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> renderRedPylonMediumWoodTransformer(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_STEEL -> renderRedPylonMediumSteel(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> renderRedPylonMediumSteelTransformer(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case RED_PYLON_LARGE -> renderRedPylonLarge(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
            case SUBSTATION -> renderSubstation(snapshot, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderRedConnector(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedConnector(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.redConnectorModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderConnectorRedSuper(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientConnectorRedSuper(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.connectorRedSuperModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderRedPylonSteel(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylonSteel(poseStack);
        BakedModel model = PowerPylonClientGeometry.redPylonSteelModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderRedPylonMediumWood(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylonMediumWood(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.redPylonMediumWoodModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderRedPylonMediumWoodTransformer(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylonMediumWoodTransformer(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.redPylonMediumWoodTransformerModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderRedPylonMediumSteel(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylonMediumSteel(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.redPylonMediumSteelModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderRedPylonMediumSteelTransformer(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylonMediumSteelTransformer(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.redPylonMediumSteelTransformerModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderRedPylonLarge(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylonLarge(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.redPylonLargeModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderSubstation(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientSubstation(poseStack, snapshot.facing());
        BakedModel model = PowerPylonClientGeometry.substationModel(snapshot.modelGeometryToken());
        STATIC_MESHES.render(model, poseStack, snapshot.blockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderLegacyRedPylon(PowerPylonBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                      MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientRedPylon(poseStack);
        PowerPylonClientGeometry.redPylonModel(snapshot.modelGeometryToken()).render(poseStack,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(RED_PYLON_TEXTURE)),
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderLines(PowerPylonBlockEntity pylon, PowerPylonBlockEntity.RenderSnapshot snapshot,
                             PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        if (pylon.getLevel() == null || snapshot.connections().isEmpty()) {
            releaseCachedWireGeometry(pylon);
            return;
        }

        CachedWireGeometry geometry = cachedWireGeometry(pylon, snapshot, packedOverlay);
        if (geometry.buffer() == null || geometry.buffer().isInvalid()) {
            return;
        }

        geometry.renderType().setupRenderState();
        try {
            geometry.buffer().bind();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                geometry.buffer().drawWithShader(
                        MachineModelRenderer.composeModelView(poseStack),
                        RenderSystem.getProjectionMatrix(),
                        shader
                );
            }
        } finally {
            VertexBuffer.unbind();
            geometry.renderType().clearRenderState();
        }
    }

    private CachedWireGeometry cachedWireGeometry(PowerPylonBlockEntity pylon,
                                                  PowerPylonBlockEntity.RenderSnapshot initialSnapshot,
                                                  int packedOverlay) {
        CachedWireGeometry cached = this.wireGeometryCache.get(pylon);
        if (cached != null && cacheMatches(initialSnapshot, cached, packedOverlay)) {
            return cached;
        }

        CachedWireGeometry rebuilt = buildWireGeometry(pylon, initialSnapshot, packedOverlay);
        if (cached != null) {
            cached.close();
        }
        this.wireGeometryCache.put(pylon, rebuilt);
        pylon.registerClientRenderResource(rebuilt);
        return rebuilt;
    }

    private boolean cacheMatches(PowerPylonBlockEntity.RenderSnapshot snapshot,
                                 CachedWireGeometry cached, int packedOverlay) {
        return cached.snapshot() == snapshot
                && cached.packedOverlay() == packedOverlay
                && cached.isUsable();
    }

    private CachedWireGeometry buildWireGeometry(PowerPylonBlockEntity pylon,
                                                 PowerPylonBlockEntity.RenderSnapshot snapshot,
                                                 int packedOverlay) {
        // The immutable snapshot owns the exact block-local, float-emitted vertices
        // from which its world AABB was calculated. Upload that same data; do not
        // independently regenerate a numerically different curve on the render thread.
        List<PowerPylonWireLayout.WireQuad> quads = snapshot.wireQuads();

        int color = snapshot.color() == 0 ? 0xFFFFFF : snapshot.color();
        RenderType renderType = RenderType.entityCutoutNoCull(
                snapshot.color() == 0 ? WIRE_TEXTURE : WIRE_GREYSCALE_TEXTURE
        );
        VertexBuffer buffer = buildWireBuffer(quads, renderType, color, packedOverlay);
        return new CachedWireGeometry(
                pylon,
                snapshot,
                packedOverlay,
                renderType,
                buffer
        );
    }

    private VertexBuffer buildWireBuffer(List<PowerPylonWireLayout.WireQuad> quads, RenderType renderType,
                                         int color, int packedOverlay) {
        if (quads.isEmpty()) {
            return null;
        }
        BufferBuilder builder = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
        PoseStack buildPose = new PoseStack();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        for (PowerPylonWireLayout.WireQuad quad : quads) {
            emitQuad(buildPose, builder, quad, r, g, b, packedOverlay);
        }
        MeshData mesh = builder.buildOrThrow();
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            buffer.bind();
            buffer.upload(mesh);
            return buffer;
        } catch (RuntimeException exception) {
            buffer.close();
            throw exception;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private void closeWireCache() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::closeWireCache);
            return;
        }
        for (CachedWireGeometry geometry : List.copyOf(this.wireGeometryCache.values())) {
            geometry.close();
        }
    }

    private void releaseCachedWireGeometry(PowerPylonBlockEntity pylon) {
        CachedWireGeometry cached = this.wireGeometryCache.get(pylon);
        if (cached != null) {
            cached.close();
        }
    }

    private void closeWireCache(net.minecraft.client.multiplayer.ClientLevel level,
                                net.minecraft.world.level.ChunkPos chunkPos) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> closeWireCache(level, chunkPos));
            return;
        }
        List<CachedWireGeometry> affected = this.wireGeometryCache.entrySet().stream()
                .filter(entry -> entry.getKey().getLevel() == level)
                .filter(entry -> chunkPos.equals(new net.minecraft.world.level.ChunkPos(entry.getKey().getBlockPos()))
                        || entry.getValue().snapshot().connectionPositions().stream().anyMatch(connection ->
                        chunkPos.equals(new net.minecraft.world.level.ChunkPos(connection))))
                .map(Map.Entry::getValue)
                .toList();
        affected.forEach(CachedWireGeometry::close);
    }

    private void emitQuad(PoseStack poseStack, VertexConsumer consumer, PowerPylonWireLayout.WireQuad quad,
                          int r, int g, int bl, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, quad.a(), 0.0F, 0.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, quad.b(), 0.0F, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, quad.c(), quad.wrap(), 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, quad.d(), quad.wrap(), 0.0F, r, g, bl, packedOverlay);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private final class CachedWireGeometry implements AutoCloseable {
        private final PowerPylonBlockEntity pylon;
        private final PowerPylonBlockEntity.RenderSnapshot snapshot;
        private final int packedOverlay;
        private final RenderType renderType;
        private final VertexBuffer buffer;
        private boolean closed;

        private CachedWireGeometry(PowerPylonBlockEntity pylon,
                                   PowerPylonBlockEntity.RenderSnapshot snapshot,
                                   int packedOverlay, RenderType renderType, VertexBuffer buffer) {
            this.pylon = pylon;
            this.snapshot = snapshot;
            this.packedOverlay = packedOverlay;
            this.renderType = renderType;
            this.buffer = buffer;
        }

        private PowerPylonBlockEntity.RenderSnapshot snapshot() {
            return this.snapshot;
        }

        private int packedOverlay() {
            return this.packedOverlay;
        }

        private RenderType renderType() {
            return this.renderType;
        }

        private VertexBuffer buffer() {
            return this.buffer;
        }

        private boolean isUsable() {
            return !this.closed && (this.buffer == null || !this.buffer.isInvalid());
        }

        @Override
        public void close() {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(this::close);
                return;
            }
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (wireGeometryCache.get(this.pylon) == this) {
                wireGeometryCache.remove(this.pylon);
            }
            this.pylon.unregisterClientRenderResource(this);
            if (this.buffer != null) {
                this.buffer.close();
            }
        }
    }

    static void orientRedPylon(PoseStack poseStack) {
        // RenderPylon: translate(x + 0.5, y + 0.625, z + 0.5), then rotate Z 180 degrees.
        poseStack.translate(0.5F, 0.625F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }

    static void orientRedPylonSteel(PoseStack poseStack) {
        // RenderPylon renders Pylon_steel at the block center without applying a rotation.
        poseStack.translate(0.5F, 0.0F, 0.5F);
    }

    static void orientRedConnector(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case DOWN -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 1.0F, 0.0F, 0.0F)));
            case NORTH -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 0.0F, 1.0F)));
            }
            case SOUTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
            case WEST -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 0.0F, 1.0F)));
            }
            case EAST -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 0.0F, 1.0F)));
            }
            case UP -> {
            }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
    }

    static void orientConnectorRedSuper(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case DOWN -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 1.0F, 0.0F, 0.0F)));
            case NORTH -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 0.0F, 1.0F)));
            }
            case SOUTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
            case WEST -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 0.0F, 1.0F)));
            }
            case EAST -> {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1.0F, 0.0F, 0.0F)));
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 0.0F, 1.0F)));
            }
            case UP -> {
            }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
    }

    static void orientRedPylonMediumWood(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 1.0F, 0.0F)));
            case EAST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> {
            }
            case WEST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 1.0F, 0.0F)));
            case UP -> {
            }
            case DOWN -> {
            }
        }
    }

    static void orientRedPylonMediumWoodTransformer(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 1.0F, 0.0F)));
            case EAST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> {
            }
            case WEST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 1.0F, 0.0F)));
            case UP -> {
            }
            case DOWN -> {
            }
        }
    }

    static void orientRedPylonMediumSteel(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 1.0F, 0.0F)));
            case EAST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> {
            }
            case WEST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 1.0F, 0.0F)));
            case UP -> {
            }
            case DOWN -> {
            }
        }
    }

    static void orientRedPylonMediumSteelTransformer(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 0.0F, 1.0F, 0.0F)));
            case EAST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> {
            }
            case WEST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(270.0D), 0.0F, 1.0F, 0.0F)));
            case UP -> {
            }
            case DOWN -> {
            }
        }
    }

    static void orientRedPylonLarge(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case EAST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(45.0D), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> {
            }
            case WEST -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(135.0D), 0.0F, 1.0F, 0.0F)));
            case UP -> {
            }
            case DOWN -> {
            }
        }
    }

    static void orientSubstation(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case SOUTH -> poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 0.0F, 1.0F, 0.0F)));
            case WEST -> {
            }
            case EAST -> {
            }
            case UP -> {
            }
            case DOWN -> {
            }
        }
    }
}
