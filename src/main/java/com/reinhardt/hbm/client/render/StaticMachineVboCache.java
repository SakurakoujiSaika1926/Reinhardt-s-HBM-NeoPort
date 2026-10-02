package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GPU cache for immutable machine-model parts. The cache deliberately knows
 * nothing about machine orientation: every renderer retains its own authored
 * pose and supplies that pose when drawing the cached raw model vertices.
 */
final class StaticMachineVboCache {
    private static final Set<StaticMachineVboCache> INSTANCES =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private final List<CacheEntry> entries = new ArrayList<>();
    private final Map<ModelResourceLocation, ModelVariantKey> cutoutModelKeys = new LinkedHashMap<>();
    private final Map<ModelResourceLocation, Map<ResourceLocation, ModelVariantKey>> standaloneModelKeys =
            new LinkedHashMap<>();

    StaticMachineVboCache() {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    void render(ModelResourceLocation location, PoseStack poseStack, BlockState state,
                int packedLight, int packedOverlay) {
        renderInternal(location, null, poseStack, state, packedLight, packedOverlay);
    }

    /** Preserves renderUnculledCutoutNoCull while caching its immutable vertices. */
    void renderCutoutNoCull(ModelResourceLocation location, PoseStack poseStack, BlockState state,
                            int packedLight, int packedOverlay) {
        ModelVariantKey key = this.cutoutModelKeys.computeIfAbsent(location,
                model -> new ModelVariantKey(model, ModelRenderMode.CUTOUT_NO_CULL, null));
        renderInternal(key, null,
                poseStack, state, packedLight, packedOverlay);
    }

    /** Preserves the legacy standalone-texture UV conversion while caching immutable vertices. */
    void renderStandaloneTexture(ModelResourceLocation location, ResourceLocation texture,
                                 PoseStack poseStack, BlockState state,
                                 int packedLight, int packedOverlay) {
        Map<ResourceLocation, ModelVariantKey> textures = this.standaloneModelKeys.computeIfAbsent(
                location, ignored -> new LinkedHashMap<>());
        ModelVariantKey key = textures.computeIfAbsent(texture,
                value -> new ModelVariantKey(location, ModelRenderMode.STANDALONE_TEXTURE, value));
        renderInternal(key, null,
                poseStack, state, packedLight, packedOverlay);
    }

    /**
     * Caches an already captured immutable model generation by identity. This
     * keeps renderers with atomic model-reload snapshots on that snapshot
     * instead of resolving a possibly different model-manager generation.
     */
    void render(BakedModel model, PoseStack poseStack, BlockState state,
                int packedLight, int packedOverlay) {
        renderInternal(model, model, poseStack, state, packedLight, packedOverlay);
    }

    /**
     * Uploads an immutable post-bake quad snapshot once, while retaining the
     * caller's per-frame pose.  Tinted, UV-scrolling, or otherwise rewritten
     * vertices deliberately do not implement this contract.
     */
    void renderCaptured(CapturedGeometry geometry, PoseStack poseStack,
                        int packedLight, int packedOverlay) {
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Captured machine VBOs must be rendered on the render thread");
        }
        CacheEntry cached = find(geometry, null, packedLight, packedOverlay);
        if (cached == null || hasInvalidBuffer(cached.layers)) {
            if (cached != null) {
                closeLayers(cached.layers);
                this.entries.remove(cached);
            }
            cached = new CacheEntry(geometry, null, packedLight, packedOverlay,
                    buildCapturedLayers(geometry, packedLight, packedOverlay));
            this.entries.add(cached);
        }
        drawLayers(cached.layers, poseStack);
    }

    /** Uploads caller-authored immutable vertices once, retaining only the per-frame pose. */
    void renderDirect(DirectGeometry geometry, PoseStack poseStack,
                      int packedLight, int packedOverlay) {
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Direct machine VBOs must be rendered on the render thread");
        }
        CacheEntry cached = find(geometry, null, packedLight, packedOverlay);
        if (cached == null || hasInvalidBuffer(cached.layers)) {
            if (cached != null) {
                closeLayers(cached.layers);
                this.entries.remove(cached);
            }
            cached = new CacheEntry(geometry, null, packedLight, packedOverlay,
                    buildDirectLayers(geometry, packedLight, packedOverlay));
            this.entries.add(cached);
        }
        drawLayers(cached.layers, poseStack);
    }

    private void renderInternal(Object modelKey, BakedModel capturedModel, PoseStack poseStack,
                                BlockState state, int packedLight, int packedOverlay) {
        if (!RenderSystem.isOnRenderThread()) {
            throw new IllegalStateException("Static machine VBOs must be rendered on the render thread");
        }
        CacheEntry cached = find(modelKey, state, packedLight, packedOverlay);
        if (cached == null || hasInvalidBuffer(cached.layers)) {
            if (cached != null) {
                closeLayers(cached.layers);
                this.entries.remove(cached);
            }
            BakedModel model = capturedModel != null
                    ? capturedModel
                    : MachineModelRenderer.model(modelLocation(modelKey));
            cached = new CacheEntry(modelKey, state, packedLight, packedOverlay,
                    buildLayers(modelKey, model, state, packedLight, packedOverlay));
            this.entries.add(cached);
        }
        drawLayers(cached.layers, poseStack);
    }

    static void invalidateAll() {
        List<StaticMachineVboCache> caches;
        synchronized (INSTANCES) {
            caches = List.copyOf(INSTANCES);
        }
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> caches.forEach(StaticMachineVboCache::clearNow));
            return;
        }
        caches.forEach(StaticMachineVboCache::clearNow);
    }

    private void clearNow() {
        for (CacheEntry entry : this.entries) {
            closeLayers(entry.layers);
        }
        this.entries.clear();
    }

    private CacheEntry find(Object modelKey, BlockState state,
                            int packedLight, int packedOverlay) {
        for (CacheEntry entry : this.entries) {
            boolean sameModel = modelKey instanceof ModelResourceLocation || modelKey instanceof ModelVariantKey
                    ? entry.modelKey.equals(modelKey)
                    : entry.modelKey == modelKey;
            if (sameModel
                    && entry.state == state
                    && entry.packedLight == packedLight
                    && entry.packedOverlay == packedOverlay) {
                return entry;
            }
        }
        return null;
    }

    private static ModelResourceLocation modelLocation(Object modelKey) {
        if (modelKey instanceof ModelResourceLocation location) {
            return location;
        }
        if (modelKey instanceof ModelVariantKey variant) {
            return variant.location();
        }
        throw new IllegalArgumentException("Unsupported static model key " + modelKey);
    }

    private static List<GpuLayer> buildLayers(Object modelKey, BakedModel model, BlockState state,
                                              int packedLight, int packedOverlay) {
        if (modelKey instanceof ModelVariantKey variant) {
            return buildVariantLayers(variant, model, state, packedLight, packedOverlay);
        }
        RandomSource random = RandomSource.create(42L);
        random.setSeed(42L);
        Map<RenderType, List<RenderType>> sourcesByTarget = new LinkedHashMap<>();
        for (RenderType source : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            RenderType target = RenderTypeHelper.getEntityRenderType(source, false);
            sourcesByTarget.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        }

        List<GpuLayer> built = new ArrayList<>(sourcesByTarget.size());
        try {
            for (Map.Entry<RenderType, List<RenderType>> entry : sourcesByTarget.entrySet()) {
                RenderType target = entry.getKey();
                BufferBuilder builder = Tesselator.getInstance().begin(target.mode(), target.format());
                PoseStack buildPose = new PoseStack();
                for (RenderType source : entry.getValue()) {
                    MachineModelRenderer.emitUnculledLayer(model, buildPose, builder, state,
                            packedLight, packedOverlay, source);
                }
                MeshData mesh = builder.build();
                if (mesh == null) {
                    continue;
                }
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
                built.add(new GpuLayer(target, buffer));
            }
            return List.copyOf(built);
        } catch (RuntimeException exception) {
            closeLayers(built);
            throw exception;
        }
    }

    private static List<GpuLayer> buildVariantLayers(ModelVariantKey variant, BakedModel model,
                                                     BlockState state, int packedLight, int packedOverlay) {
        RenderType target = switch (variant.mode()) {
            case CUTOUT_NO_CULL -> RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
            case STANDALONE_TEXTURE -> RenderType.entityCutoutNoCull(variant.texture());
        };
        BufferBuilder builder = Tesselator.getInstance().begin(target.mode(), target.format());
        PoseStack buildPose = new PoseStack();
        RandomSource random = RandomSource.create(42L);
        random.setSeed(42L);
        for (RenderType source : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            if (variant.mode() == ModelRenderMode.STANDALONE_TEXTURE) {
                MachineModelRenderer.emitUnculledStandaloneTextureLayer(model, buildPose, builder, state,
                        packedLight, packedOverlay, source);
            } else {
                MachineModelRenderer.emitUnculledLayer(model, buildPose, builder, state,
                        packedLight, packedOverlay, source);
            }
        }
        return uploadSingleLayer(target, builder);
    }

    private static List<GpuLayer> buildDirectLayers(DirectGeometry geometry,
                                                     int packedLight, int packedOverlay) {
        Map<RenderType, List<DirectLayer>> layersByType = new LinkedHashMap<>();
        for (DirectLayer layer : geometry.layers()) {
            layersByType.computeIfAbsent(layer.renderType(), ignored -> new ArrayList<>()).add(layer);
        }

        List<GpuLayer> built = new ArrayList<>(layersByType.size());
        try {
            for (Map.Entry<RenderType, List<DirectLayer>> entry : layersByType.entrySet()) {
                RenderType target = entry.getKey();
                BufferBuilder builder = Tesselator.getInstance().begin(target.mode(), target.format());
                PoseStack buildPose = new PoseStack();
                for (DirectLayer layer : entry.getValue()) {
                    layer.emit(buildPose, builder, packedLight, packedOverlay);
                }
                MeshData mesh = builder.build();
                if (mesh == null) {
                    continue;
                }
                built.add(uploadLayer(target, mesh));
            }
            return List.copyOf(built);
        } catch (RuntimeException exception) {
            closeLayers(built);
            throw exception;
        }
    }

    private static List<GpuLayer> uploadSingleLayer(RenderType target, BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh == null) {
            return List.of();
        }
        return List.of(uploadLayer(target, mesh));
    }

    private static GpuLayer uploadLayer(RenderType target, MeshData mesh) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            buffer.bind();
            buffer.upload(mesh);
            return new GpuLayer(target, buffer);
        } catch (RuntimeException exception) {
            buffer.close();
            throw exception;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static List<GpuLayer> buildCapturedLayers(CapturedGeometry geometry,
                                                      int packedLight, int packedOverlay) {
        Map<RenderType, List<CapturedLayer>> layersByType = new LinkedHashMap<>();
        for (CapturedLayer layer : geometry.layers()) {
            layersByType.computeIfAbsent(layer.normalRenderType(), ignored -> new ArrayList<>()).add(layer);
        }

        List<GpuLayer> built = new ArrayList<>(layersByType.size());
        try {
            for (Map.Entry<RenderType, List<CapturedLayer>> entry : layersByType.entrySet()) {
                RenderType target = entry.getKey();
                BufferBuilder builder = Tesselator.getInstance().begin(target.mode(), target.format());
                PoseStack buildPose = new PoseStack();
                for (CapturedLayer layer : entry.getValue()) {
                    MachineModelRenderer.renderCapturedQuads(
                            layer.quads(), buildPose, builder, packedLight, packedOverlay);
                }
                MeshData mesh = builder.build();
                if (mesh == null) {
                    continue;
                }
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
                built.add(new GpuLayer(target, buffer));
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

    private static boolean hasInvalidBuffer(List<GpuLayer> layers) {
        for (GpuLayer layer : layers) {
            if (layer.buffer().isInvalid()) {
                return true;
            }
        }
        return false;
    }

    private static void closeLayers(List<GpuLayer> layers) {
        if (layers == null) {
            return;
        }
        for (GpuLayer layer : layers) {
            layer.buffer().close();
        }
    }

    private static final class CacheEntry {
        private final Object modelKey;
        private final BlockState state;
        private final int packedLight;
        private final int packedOverlay;
        private final List<GpuLayer> layers;

        private CacheEntry(Object modelKey, BlockState state,
                           int packedLight, int packedOverlay, List<GpuLayer> layers) {
            this.modelKey = modelKey;
            this.state = state;
            this.packedLight = packedLight;
            this.packedOverlay = packedOverlay;
            this.layers = layers;
        }
    }

    private record GpuLayer(RenderType renderType, VertexBuffer buffer) {
    }

    private enum ModelRenderMode {
        CUTOUT_NO_CULL,
        STANDALONE_TEXTURE
    }

    private record ModelVariantKey(ModelResourceLocation location, ModelRenderMode mode,
                                   ResourceLocation texture) {
        private ModelVariantKey {
            if (mode == ModelRenderMode.STANDALONE_TEXTURE && texture == null) {
                throw new IllegalArgumentException("Standalone-texture VBO requires a texture");
            }
        }
    }

    interface CapturedGeometry {
        List<? extends CapturedLayer> layers();
    }

    interface CapturedLayer {
        RenderType normalRenderType();

        List<BakedQuad> quads();
    }

    interface DirectGeometry {
        List<? extends DirectLayer> layers();
    }

    interface DirectLayer {
        RenderType renderType();

        void emit(PoseStack poseStack, VertexConsumer consumer,
                  int packedLight, int packedOverlay);
    }
}
