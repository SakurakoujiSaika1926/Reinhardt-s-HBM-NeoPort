package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.SnowglobeType;
import com.reinhardt.hbm.blockentity.SnowglobeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.mojang.math.Axis;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SnowglobeBlockEntityRenderer implements LongRangeBlockEntityRenderer<SnowglobeBlockEntity> {
    private static final ModelResourceLocation SOCKET = model("snowglobe_socket");
    private static final ModelResourceLocation GLASS = model("snowglobe_glass");
    private static final Map<SnowglobeType, ModelResourceLocation> FEATURES = featureModels();
    private static final ResourceLocation SOCKET_TEXTURE = texture("snowglobe.png");
    private static final ResourceLocation GLASS_TEXTURE = texture("snowglobe_glass.png");
    private static final ResourceLocation FEATURES_TEXTURE = texture("snowglobe_features.png");
    private static final List<ModelResourceLocation> ALL_MODELS = allModels();
    private static final ClientRenderSnapshotInstances<SnowglobeBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile BakedModelGeneration geometryGeneration;

    public SnowglobeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        SnowglobeBlockEntity.setClientSnapshotUpdater(SnowglobeBlockEntityRenderer::updateRenderSnapshot);
    }

    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = BakedModelGeneration.capture(ALL_MODELS);
        SnowglobeBlockEntity.setClientSnapshotUpdater(SnowglobeBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(SnowglobeBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOCKET);
        event.register(GLASS);
        FEATURES.values().forEach(event::register);
    }

    @Override
    public void render(SnowglobeBlockEntity globe, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        SnowglobeBlockEntity.RenderSnapshot snapshot = globe.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof BakedModelGeneration generation)) {
            return;
        }
        BlockState state = snapshot.blockState();
        switch (snapshot.type()) {
            case RIVET_CITY -> renderRivetCityWorld(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
            case TENPENNY_TOWER -> renderTenpennyTowerWorld(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
            case LUCKY_38 -> renderLucky38World(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
            case SIERRA_MADRE -> renderSierraMadreWorld(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
            case PRYDWEN -> renderPrydwenWorld(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
            case NONE -> renderNoneWorld(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    public static void renderRivetCityItem(BlockState state, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderRivetCitySnowglobe(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderTenpennyTowerItem(BlockState state, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderTenpennyTowerSnowglobe(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderLucky38Item(BlockState state, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderLucky38Snowglobe(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderSierraMadreItem(BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderSierraMadreSnowglobe(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderPrydwenItem(BlockState state, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderPrydwenSnowglobe(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderNoneItem(BlockState state, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderNoneSnowglobe(null, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderRivetCityWorld(BakedModelGeneration generation, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderRivetCitySnowglobe(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTenpennyTowerWorld(BakedModelGeneration generation, BlockState state,
                                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderTenpennyTowerSnowglobe(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLucky38World(BakedModelGeneration generation, BlockState state,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderLucky38Snowglobe(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSierraMadreWorld(BakedModelGeneration generation, BlockState state,
                                               PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderSierraMadreSnowglobe(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPrydwenWorld(BakedModelGeneration generation, BlockState state,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderPrydwenSnowglobe(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNoneWorld(BakedModelGeneration generation, BlockState state,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderNoneSnowglobe(generation, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderRivetCitySnowglobe(BakedModelGeneration generation, BlockState state,
                                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(generation, SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, FEATURES.get(SnowglobeType.RIVET_CITY), FEATURES_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);

        Font font = Minecraft.getInstance().font;
        String label = SnowglobeType.RIVET_CITY.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderTenpennyTowerSnowglobe(BakedModelGeneration generation, BlockState state,
                                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(generation, SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, FEATURES.get(SnowglobeType.TENPENNY_TOWER), FEATURES_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = SnowglobeType.TENPENNY_TOWER.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderLucky38Snowglobe(BakedModelGeneration generation, BlockState state,
                                               PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(generation, SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, FEATURES.get(SnowglobeType.LUCKY_38), FEATURES_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = SnowglobeType.LUCKY_38.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderSierraMadreSnowglobe(BakedModelGeneration generation, BlockState state,
                                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(generation, SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, FEATURES.get(SnowglobeType.SIERRA_MADRE), FEATURES_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = SnowglobeType.SIERRA_MADRE.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderPrydwenSnowglobe(BakedModelGeneration generation, BlockState state,
                                               PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(generation, SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, FEATURES.get(SnowglobeType.PRYDWEN), FEATURES_TEXTURE,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = SnowglobeType.PRYDWEN.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderNoneSnowglobe(BakedModelGeneration generation, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        renderPart(generation, SOCKET, SOCKET_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPart(generation, GLASS, GLASS_TEXTURE, state, poseStack, bufferSource, packedLight, packedOverlay);
        Font font = Minecraft.getInstance().font;
        String label = SnowglobeType.NONE.label();
        poseStack.translate(4.025F, 0.5F, 0.0F);
        poseStack.scale(0.05F, -0.05F, 0.05F);
        poseStack.translate(0.0F, -font.lineHeight / 2.0F, font.width(label) * 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 1.0F, 0.0F);
        font.drawInBatch(label, 0.0F, 0.0F, 0xFFFFFFFF, true, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    private static void renderPart(BakedModelGeneration generation, ModelResourceLocation model,
                                   ResourceLocation texture, BlockState state,
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel baked = generation == null ? MachineModelRenderer.model(model) : generation.model(model);
        MachineModelRenderer.renderUnculledUv(baked, poseStack, bufferSource, state, packedLight, packedOverlay,
                texture, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(SnowglobeBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(SnowglobeBlockEntity globe) {
        CLIENT_INSTANCES.track(globe);
        BakedModelGeneration generation = geometryGeneration();
        BlockState state = globe.getBlockState();
        SnowglobeType type = globe.type();
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        switch (type) {
            case RIVET_CITY -> captureRivetCityBounds(generation, state, capture);
            case TENPENNY_TOWER -> captureTenpennyTowerBounds(generation, state, capture);
            case LUCKY_38 -> captureLucky38Bounds(generation, state, capture);
            case SIERRA_MADRE -> captureSierraMadreBounds(generation, state, capture);
            case PRYDWEN -> capturePrydwenBounds(generation, state, capture);
            case NONE -> captureNoneBounds(generation, state, capture);
        }
        globe.publishClientRenderSnapshot(new SnowglobeBlockEntity.RenderSnapshot(
                state, type, generation, capture.bounds(globe.getBlockPos()),
                SnowglobeBlockEntity.clientSnapshotRevision()));
    }

    private static void captureRivetCityBounds(BakedModelGeneration generation, BlockState state,
                                               GeometryCaptureBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderRivetCitySnowglobe(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
    }

    private static void captureTenpennyTowerBounds(BakedModelGeneration generation, BlockState state,
                                                   GeometryCaptureBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderTenpennyTowerSnowglobe(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
    }

    private static void captureLucky38Bounds(BakedModelGeneration generation, BlockState state,
                                             GeometryCaptureBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderLucky38Snowglobe(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
    }

    private static void captureSierraMadreBounds(BakedModelGeneration generation, BlockState state,
                                                 GeometryCaptureBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderSierraMadreSnowglobe(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
    }

    private static void capturePrydwenBounds(BakedModelGeneration generation, BlockState state,
                                             GeometryCaptureBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderPrydwenSnowglobe(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
    }

    private static void captureNoneBounds(BakedModelGeneration generation, BlockState state,
                                          GeometryCaptureBufferSource capture) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(22.5F
                * state.getValue(com.reinhardt.hbm.block.SnowglobeBlock.ROTATION) + 90.0F));
        renderNoneSnowglobe(generation, state, poseStack, capture, LightTexture.FULL_BRIGHT, 0);
    }

    private static BakedModelGeneration geometryGeneration() {
        BakedModelGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (SnowglobeBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = BakedModelGeneration.capture(ALL_MODELS);
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static List<ModelResourceLocation> allModels() {
        ArrayList<ModelResourceLocation> result = new ArrayList<>();
        result.add(SOCKET);
        result.add(GLASS);
        result.addAll(FEATURES.values());
        return List.copyOf(result);
    }

    private static Map<SnowglobeType, ModelResourceLocation> featureModels() {
        Map<SnowglobeType, ModelResourceLocation> models = new EnumMap<>(SnowglobeType.class);
        for (SnowglobeType type : SnowglobeType.values()) {
            if (type != SnowglobeType.NONE) {
                models.put(type, model("snowglobe_" + type.modelGroup().toLowerCase()));
            }
        }
        return Map.copyOf(models);
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/trinkets/" + name);
    }

    private static ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/models/trinkets/" + name);
    }
}
