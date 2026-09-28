package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class RbmkComponentBlockEntityRenderer implements LongRangeBlockEntityRenderer<RbmkComponentBlockEntity> {
    private static final Set<RbmkComponentBlockEntityRenderer> INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> MODELS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> SEGMENTS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> TOPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> GLASS_TOPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> FUEL_CAPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> FUEL_INNERS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final ModelResourceLocation FUEL_RODS = MachineModelRenderer.standalone("block/rbmk_rod_rods");
    private static final ModelResourceLocation CONTROL_LID = MachineModelRenderer.standalone("block/rbmk_rods_lid");
    private static final ModelResourceLocation CONTROL_LID_RED = MachineModelRenderer.standalone("block/rbmk_rods_lid_red");
    private static final ModelResourceLocation CONTROL_LID_YELLOW = MachineModelRenderer.standalone("block/rbmk_rods_lid_yellow");
    private static final ModelResourceLocation CONTROL_LID_GREEN = MachineModelRenderer.standalone("block/rbmk_rods_lid_green");
    private static final ModelResourceLocation CONTROL_LID_BLUE = MachineModelRenderer.standalone("block/rbmk_rods_lid_blue");
    private static final ModelResourceLocation CONTROL_LID_PURPLE = MachineModelRenderer.standalone("block/rbmk_rods_lid_purple");
    private static final ModelResourceLocation CONTROL_AUTO_LID = MachineModelRenderer.standalone("block/rbmk_rods_lid_auto");
    private static final ModelResourceLocation AUTOLOADER_BASE = MachineModelRenderer.standalone("block/rbmk_autoloader_base");
    private static final ModelResourceLocation AUTOLOADER_PISTON = MachineModelRenderer.standalone("block/rbmk_autoloader_piston");
    private static final ModelResourceLocation BUTTON_SOCKET = MachineModelRenderer.standalone("block/rbmk_button_socket");
    private static final ModelResourceLocation BUTTON_BUTTON = MachineModelRenderer.standalone("block/rbmk_button_button");
    private static final ModelResourceLocation MINI_PANEL_BASE = MachineModelRenderer.standalone("block/rbmk_mini_panel_base");
    private static final ModelResourceLocation GAUGE_BASE = MachineModelRenderer.standalone("block/rbmk_gauge_base");
    private static final ModelResourceLocation GAUGE_NEEDLE = MachineModelRenderer.standalone("block/rbmk_gauge_needle");
    private static final ModelResourceLocation INDICATOR_BASE = MachineModelRenderer.standalone("block/rbmk_indicator_base");
    private static final ModelResourceLocation INDICATOR_LIGHT = MachineModelRenderer.standalone("block/rbmk_indicator_light");
    private static final ModelResourceLocation LEVER_BASE = MachineModelRenderer.standalone("block/rbmk_lever_base");
    private static final ModelResourceLocation LEVER_HANDLE = MachineModelRenderer.standalone("block/rbmk_lever_handle");
    private static final ModelResourceLocation CRANE_GIRDER = MachineModelRenderer.standalone("block/rbmk_crane_girder");
    private static final ModelResourceLocation CRANE_MAIN = MachineModelRenderer.standalone("block/rbmk_crane_main");
    private static final ModelResourceLocation CRANE_TUBE = MachineModelRenderer.standalone("block/rbmk_crane_tube");
    private static final ModelResourceLocation CRANE_CARRIAGE = MachineModelRenderer.standalone("block/rbmk_crane_carriage");
    private static final ModelResourceLocation CRANE_LIFT = MachineModelRenderer.standalone("block/rbmk_crane_lift");
    private static final ModelResourceLocation CRANE_CONSOLE_BODY = MachineModelRenderer.standalone("block/rbmk_crane_console_body");
    private static final ModelResourceLocation CRANE_CONSOLE_JOYSTICK = MachineModelRenderer.standalone("block/rbmk_crane_console_joystick");
    private static final ModelResourceLocation CRANE_CONSOLE_METER1 = MachineModelRenderer.standalone("block/rbmk_crane_console_meter1");
    private static final ModelResourceLocation CRANE_CONSOLE_METER2 = MachineModelRenderer.standalone("block/rbmk_crane_console_meter2");
    private static final ModelResourceLocation CRANE_CONSOLE_LAMP1 = MachineModelRenderer.standalone("block/rbmk_crane_console_lamp1");
    private static final ModelResourceLocation CRANE_CONSOLE_LAMP2 = MachineModelRenderer.standalone("block/rbmk_crane_console_lamp2");
    private static volatile GeometryGeneration geometryGeneration;
    private static int nextGeometryGeneration;

    /** Identity keys prevent equal positions in different client levels from sharing GPU state. */
    private final Map<RbmkComponentBlockEntity, MachineBuffers> machineBuffers = new IdentityHashMap<>();

    static {
        for (RbmkComponentBlock.Kind kind : RbmkComponentBlock.Kind.values()) {
            String base = "block/rbmk_" + kind.getSerializedName();
            MODELS.put(kind, MachineModelRenderer.standalone(base));
            if (kind.isColumn()) {
                SEGMENTS.put(kind, MachineModelRenderer.standalone(base + "_segment"));
                TOPS.put(kind, MachineModelRenderer.standalone(base + "_top"));
                if (!kind.isControl() && !kind.hasTopPipes()) {
                    GLASS_TOPS.put(kind, MachineModelRenderer.standalone(base + "_glass_top"));
                }
            }
            if (kind.acceptsFuel()) {
                FUEL_CAPS.put(kind, MachineModelRenderer.standalone(base + "_cap"));
                FUEL_INNERS.put(kind, MachineModelRenderer.standalone(base + "_inner"));
            }
        }
    }

    public RbmkComponentBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        RbmkComponentBlockEntity.setClientRenderSnapshotUpdater(
                RbmkComponentBlockEntityRenderer::updateRenderSnapshot);
        RbmkComponentBlockEntity.setClientGpuCacheDisposer(
                RbmkComponentBlockEntityRenderer::discardMachineCache);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : MODELS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : SEGMENTS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : TOPS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : GLASS_TOPS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : FUEL_CAPS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : FUEL_INNERS.values()) {
            event.register(model);
        }
        event.register(FUEL_RODS);
        event.register(CONTROL_LID);
        event.register(CONTROL_LID_RED);
        event.register(CONTROL_LID_YELLOW);
        event.register(CONTROL_LID_GREEN);
        event.register(CONTROL_LID_BLUE);
        event.register(CONTROL_LID_PURPLE);
        event.register(CONTROL_AUTO_LID);
        event.register(AUTOLOADER_BASE);
        event.register(AUTOLOADER_PISTON);
        event.register(BUTTON_SOCKET);
        event.register(BUTTON_BUTTON);
        event.register(MINI_PANEL_BASE);
        event.register(GAUGE_BASE);
        event.register(GAUGE_NEEDLE);
        event.register(INDICATOR_BASE);
        event.register(INDICATOR_LIGHT);
        event.register(LEVER_BASE);
        event.register(LEVER_HANDLE);
        event.register(CRANE_GIRDER);
        event.register(CRANE_MAIN);
        event.register(CRANE_TUBE);
        event.register(CRANE_CARRIAGE);
        event.register(CRANE_LIFT);
        event.register(CRANE_CONSOLE_BODY);
        event.register(CRANE_CONSOLE_JOYSTICK);
        event.register(CRANE_CONSOLE_METER1);
        event.register(CRANE_CONSOLE_METER2);
        event.register(CRANE_CONSOLE_LAMP1);
        event.register(CRANE_CONSOLE_LAMP2);
    }

    /** Invalidates snapshots before the model manager starts exposing a new bake. */
    static void beginGeometryReload() {
        geometryGeneration = null;
        closeAllGpuCaches();
        RbmkComponentBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    /** Captures every applicable RBMK model/state pair on the client thread after baking. */
    static void rebuildGeometryGenerationAfterBake() {
        Map<StateModelKey, CapturedModel> captured = new HashMap<>();
        Set<BlockState> capturedStates = new HashSet<>();
        for (net.minecraft.world.level.block.Block rawBlock : BuiltInRegistries.BLOCK) {
            if (!(rawBlock instanceof RbmkComponentBlock rbmkBlock)) {
                continue;
            }
            Set<ModelResourceLocation> locations = modelLocationsFor(rbmkBlock.kind());
            for (BlockState state : rawBlock.getStateDefinition().getPossibleStates()) {
                capturedStates.add(state);
                for (ModelResourceLocation location : locations) {
                    captured.put(new StateModelKey(state, location), captureModel(location, state));
                }
            }
        }
        geometryGeneration = new GeometryGeneration(++nextGeometryGeneration,
                Map.copyOf(captured), Set.copyOf(capturedStates));
        closeAllGpuCaches();
        RbmkComponentBlockEntity.setClientRenderSnapshotUpdater(
                RbmkComponentBlockEntityRenderer::updateRenderSnapshot);
        RbmkComponentBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    static void closeGpuCachesOnLogout() {
        closeAllGpuCaches();
    }

    private static Set<ModelResourceLocation> modelLocationsFor(RbmkComponentBlock.Kind kind) {
        Set<ModelResourceLocation> locations = new LinkedHashSet<>();
        ModelResourceLocation primary = MODELS.get(kind);
        if (primary != null) {
            locations.add(primary);
        }
        if (kind.isColumn()) {
            addIfPresent(locations, SEGMENTS.get(kind));
            addIfPresent(locations, TOPS.get(kind));
            addIfPresent(locations, GLASS_TOPS.get(kind));
            addIfPresent(locations, FUEL_CAPS.get(kind));
            addIfPresent(locations, FUEL_INNERS.get(kind));
            if (kind.acceptsFuel()) {
                locations.add(FUEL_RODS);
            }
            if (kind.isControl()) {
                Collections.addAll(locations, CONTROL_LID, CONTROL_LID_RED, CONTROL_LID_YELLOW,
                        CONTROL_LID_GREEN, CONTROL_LID_BLUE, CONTROL_LID_PURPLE, CONTROL_AUTO_LID);
            }
            return Set.copyOf(locations);
        }
        if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            Collections.addAll(locations, AUTOLOADER_BASE, AUTOLOADER_PISTON);
        }
        if (kind.isMiniPanel()) {
            locations.add(MINI_PANEL_BASE);
        }
        switch (kind) {
            case GAUGE -> Collections.addAll(locations, GAUGE_BASE, GAUGE_NEEDLE);
            case GRAPH, NUMITRON -> addIfPresent(locations, MODELS.get(RbmkComponentBlock.Kind.NUMITRON));
            case TERMINAL -> addIfPresent(locations, MODELS.get(RbmkComponentBlock.Kind.TERMINAL));
            case LEVER -> Collections.addAll(locations, LEVER_BASE, LEVER_HANDLE);
            case INDICATOR -> Collections.addAll(locations, INDICATOR_BASE, INDICATOR_LIGHT);
            case KEY_PAD -> Collections.addAll(locations, BUTTON_SOCKET, BUTTON_BUTTON);
            case CRANE_CONSOLE -> Collections.addAll(locations,
                    CRANE_CONSOLE_BODY, CRANE_CONSOLE_JOYSTICK,
                    CRANE_CONSOLE_METER1, CRANE_CONSOLE_METER2,
                    CRANE_CONSOLE_LAMP1, CRANE_CONSOLE_LAMP2,
                    CRANE_GIRDER, CRANE_MAIN, CRANE_TUBE, CRANE_CARRIAGE, CRANE_LIFT);
            default -> {
            }
        }
        return Set.copyOf(locations);
    }

    private static void addIfPresent(Set<ModelResourceLocation> locations, ModelResourceLocation location) {
        if (location != null) {
            locations.add(location);
        }
    }

    @Override
    public void render(RbmkComponentBlockEntity rbmk, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        RbmkComponentBlockEntity.RenderSnapshot snapshot = rbmk.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration generation)) {
            return;
        }
        MachineBuffers buffers = this.machineBuffers.computeIfAbsent(rbmk, ignored -> new MachineBuffers());
        renderScene(rbmk, snapshot, generation, partialTick, poseStack, bufferSource,
                packedLight, packedOverlay, buffers);
    }

    private static void renderScene(RbmkComponentBlockEntity rbmk,
                                    RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                    GeometryGeneration generation,
                                    float partialTick, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight,
                                    int packedOverlay, MachineBuffers buffers) {
        RbmkComponentBlock.Kind kind = snapshot.kind();
        ModelResourceLocation location = MODELS.get(kind);
        if (location == null || generation.model(snapshot.blockState(), location) == null) {
            return;
        }
        BlockState state = snapshot.blockState();
        if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            renderCaptured(generation, AUTOLOADER_BASE, state, poseStack, bufferSource,
                    packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.0F, 4.0F - snapshot.autoloaderPiston(partialTick) * 4.0D, 0.0F);
            renderCaptured(generation, AUTOLOADER_PISTON, state, poseStack, bufferSource,
                    packedLight, packedOverlay);
            poseStack.popPose();
            poseStack.popPose();
            return;
        }
        if (!kind.isColumn()) {
            poseStack.pushPose();
            if (kind == RbmkComponentBlock.Kind.CONSOLE) {
                applyConsoleTransform(poseStack, rbmkConsoleFacing(state));
            } else if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                applyCraneConsoleTransform(poseStack, rbmkCraneConsoleFacing(state));
            }
            if (kind == RbmkComponentBlock.Kind.CONSOLE) {
                renderCaptured(generation, location, state, poseStack, bufferSource,
                        packedLight, packedOverlay);
                renderConsoleBoard(rbmk, poseStack, bufferSource);
                renderConsoleScreens(rbmk, poseStack, bufferSource);
            } else if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                renderCraneConsole(generation, snapshot, partialTick, poseStack, bufferSource,
                        state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.DISPLAY) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderDisplayBoard(rbmk, snapshot, poseStack, bufferSource);
            } else if (kind == RbmkComponentBlock.Kind.DISPLAY_BLANK) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.GAUGE) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderGaugePanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay, partialTick);
            } else if (kind == RbmkComponentBlock.Kind.GRAPH) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderGraphPanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.LEVER) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderLeverPanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay, partialTick);
            } else if (kind == RbmkComponentBlock.Kind.INDICATOR) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderIndicatorPanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.KEY_PAD) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderKeypadPanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.NUMITRON) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderNumitronPanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.TERMINAL) {
                renderMiniPanelBase(generation, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTerminalPanel(generation, rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else {
                renderCaptured(generation, location, state, poseStack, bufferSource,
                        packedLight, packedOverlay);
            }
            poseStack.popPose();
            if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                renderCraneStructure(generation, rbmk, snapshot, partialTick, poseStack,
                        bufferSource, state, packedLight, packedOverlay, buffers);
            }
            return;
        }

        int height = snapshot.columnHeight();
        if (buffers == null) {
            renderColumnDirect(generation, snapshot, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            buffers.prepareColumn(generation, snapshot, packedLight, packedOverlay);
            drawLayers(buffers.columnLayers(), poseStack);
            drawLayers(buffers.fuelLayers(), poseStack);
        }
        if (kind.acceptsFuel() && snapshot.hasFuelRod()) {
            renderCherenkov(snapshot, height, poseStack, bufferSource, packedOverlay);
        }
        if (kind.isControl()) {
            renderControlLid(generation, snapshot, kind, height, poseStack, bufferSource,
                    state, packedLight, packedOverlay, partialTick);
        }
    }

    private static void renderMiniPanelBase(GeometryGeneration generation,
                                            RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyMiniPanelBaseTransform(poseStack, snapshot.kind(), snapshot.facing());
        renderCaptured(generation, MINI_PANEL_BASE, state, poseStack, bufferSource,
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderGaugePanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, float partialTick) {
        poseStack.pushPose();
        applyGaugePanelTransform(poseStack, rbmkGaugeFacing(state));
        for (int slot = 0; slot < 4; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
            renderCaptured(generation, GAUGE_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pushPose();
            double value = slot == 0 ? rbmk.heat() : slot == 1 ? rbmk.lastFlux() : slot == 2 ? rbmk.controlLevel() * 100.0D : rbmk.redstoneLevel();
            double upper = slot == 0 ? Math.max(1.0D, rbmk.maxConsoleHeat()) : slot == 1 ? Math.max(1.0D, rbmk.lastFlux()) : slot == 2 ? 100.0D : 15.0D;
            double angle = Math.max(0.0D, Math.min(80.0D, value / upper * 50.0D));
            poseStack.translate(0.0D, 0.4375D, -0.125D);
            poseStack.mulPose(com.mojang.math.Axis.XN.rotationDegrees((float) angle - 85.0F));
            poseStack.translate(0.0D, -0.4375D, 0.125D);
            renderCapturedTinted(generation, GAUGE_NEEDLE, state, poseStack, bufferSource,
                    LightTexture.FULL_BRIGHT, packedOverlay, gaugeNeedleColor(slot));
            poseStack.popPose();
            drawPanelLabel(defaultGaugeLabel(slot), poseStack, bufferSource, 0.01D, 0.3125D, 0.0D, 0.4F, 0x00FF00);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderGraphPanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyGraphPanelTransform(poseStack, rbmkGraphFacing(state));
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
            renderCaptured(generation, MODELS.get(RbmkComponentBlock.Kind.NUMITRON), state,
                    poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderNumitronPanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyNumitronPanelTransform(poseStack, rbmkNumitronFacing(state));
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
            renderCaptured(generation, MODELS.get(RbmkComponentBlock.Kind.NUMITRON), state,
                    poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderTerminalPanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyTerminalPanelTransform(poseStack, rbmkTerminalFacing(state));
        poseStack.translate(0.25D, 0.0D, 0.0D);
        renderCaptured(generation, MODELS.get(RbmkComponentBlock.Kind.TERMINAL), state,
                poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLeverPanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, float partialTick) {
        poseStack.pushPose();
        applyLeverPanelTransform(poseStack, rbmkLeverFacing(state));
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, 0.0D, slot * -0.5D + 0.25D);
            renderCaptured(generation, LEVER_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            double progress = slot == 0 ? rbmk.redstoneLevel() / 15.0D : rbmk.controlLevel();
            poseStack.pushPose();
            poseStack.translate(0.125D, 0.5625D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) (-180.0D * progress)));
            poseStack.translate(-0.125D, -0.5625D, 0.0D);
            renderCaptured(generation, LEVER_HANDLE, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            drawPanelLabel(slot == 0 ? "Signal" : "Control", poseStack, bufferSource, 0.01D, 0.0625D, 0.0D, 0.4F, 0x00FF00);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderIndicatorPanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyIndicatorPanelTransform(poseStack, rbmkIndicatorFacing(state));
        for (int slot = 0; slot < 6; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.3125D + 0.3125D, (slot % 2) * -0.5D + 0.25D);
            renderCaptured(generation, INDICATOR_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            boolean lit = slot == 0 ? rbmk.heat() > 0.0D : slot == 1 ? rbmk.lastFlux() > 0.0D : slot == 2 ? rbmk.controlLevel() > 0.0D : slot == 3 ? rbmk.redstoneLevel() > 0 : slot == 4 ? rbmk.waterAmount() > 0 : rbmk.steamAmount() > 0;
            int color = (slot & 1) == 0 ? 0xFFFF0000 : 0xFFFFFF00;
            if (!lit) {
                color = dimColor(color, 0.35F);
            }
            renderCapturedTinted(generation, INDICATOR_LIGHT, state, poseStack, bufferSource,
                    lit ? LightTexture.FULL_BRIGHT : packedLight, packedOverlay, color);
            drawPanelLabel(defaultIndicatorLabel(slot), poseStack, bufferSource, 0.0725D, 0.5D, 0.0D, 0.3F, 0x000000);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderKeypadPanel(GeometryGeneration generation, RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyKeypadPanelTransform(poseStack, rbmkKeypadFacing(state));
        for (int slot = 0; slot < 4; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
            renderCaptured(generation, BUTTON_SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderCapturedTinted(generation, BUTTON_BUTTON, state, poseStack, bufferSource,
                    packedLight, packedOverlay, keypadColor(slot));
            drawPanelLabel(Integer.toString(slot + 1), poseStack, bufferSource, 0.01D, 0.3125D, 0.0D, 0.4F, 0x00FF00);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void drawPanelLabel(String text, PoseStack poseStack, MultiBufferSource bufferSource, double x, double y, double z, float maxWidth, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        int width = font.width(text);
        float scale = Math.min(0.0125F, maxWidth / Math.max(width, 1));
        drawPanelText(text, poseStack, bufferSource, 0.0D, 0.0D, 0.0D, scale, color, true);
        poseStack.popPose();
    }

    private static void drawPanelText(String text, PoseStack poseStack, MultiBufferSource bufferSource, double x, double y, double z, float scale, int color, boolean centered) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        int width = font.width(text);
        int height = font.lineHeight;
        poseStack.scale(scale, -scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        font.drawInBatch(text, centered ? -width / 2.0F : 0.0F, -height / 2.0F, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static int gaugeNeedleColor(int slot) {
        return switch (slot) {
            case 0 -> 0xFF800000;
            case 1 -> 0xFF804000;
            case 2 -> 0xFF808000;
            default -> 0xFF000080;
        };
    }

    private static int keypadColor(int slot) {
        return switch (slot) {
            case 0 -> 0xFFFF0000;
            case 1 -> 0xFFFFFF00;
            case 2 -> 0xFF00FF00;
            default -> 0xFF0080FF;
        };
    }

    private static int dimColor(int argb, float multiplier) {
        int red = Math.round(((argb >> 16) & 0xFF) * multiplier);
        int green = Math.round(((argb >> 8) & 0xFF) * multiplier);
        int blue = Math.round((argb & 0xFF) * multiplier);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static String defaultGaugeLabel(int slot) {
        return switch (slot) {
            case 0 -> "Heat";
            case 1 -> "Flux";
            case 2 -> "Rod";
            default -> "Redstone";
        };
    }

    private static String defaultIndicatorLabel(int slot) {
        return switch (slot) {
            case 0 -> "Heat";
            case 1 -> "Flux";
            case 2 -> "Rod";
            case 3 -> "Signal";
            case 4 -> "Water";
            default -> "Steam";
        };
    }

    private static void renderControlLid(GeometryGeneration generation,
                                         RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                         RbmkComponentBlock.Kind kind, int columnHeight,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         BlockState state, int packedLight, int packedOverlay,
                                         float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0.5D, columnHeight - 1.0D
                + snapshotControlLevel(snapshot, partialTick), 0.5D);
        renderCaptured(generation, controlLidModel(snapshot, kind), state, poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static double snapshotControlLevel(RbmkComponentBlockEntity.RenderSnapshot snapshot, float partialTick) {
        return snapshot.previousControlLevel()
                + (snapshot.controlLevel() - snapshot.previousControlLevel()) * partialTick;
    }

    private static ModelResourceLocation controlLidModel(RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                                          RbmkComponentBlock.Kind kind) {
        if (kind.isAutomaticControl()) {
            return CONTROL_AUTO_LID;
        }
        return switch (snapshot.colorGroup()) {
            case 0 -> CONTROL_LID_RED;
            case 1 -> CONTROL_LID_YELLOW;
            case 2 -> CONTROL_LID_GREEN;
            case 3 -> CONTROL_LID_BLUE;
            case 4 -> CONTROL_LID_PURPLE;
            default -> CONTROL_LID;
        };
    }

    private static EnumSet<Direction> hiddenColumnFaces(RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                                         int yOffset, int segments, boolean hasTopModel) {
        EnumSet<Direction> hidden = EnumSet.noneOf(Direction.class);
        if (yOffset > 0) {
            hidden.add(Direction.DOWN);
        }
        if (yOffset < segments || hasTopModel) {
            hidden.add(Direction.UP);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (snapshot.hiddenHorizontalFace(direction)) {
                hidden.add(direction);
            }
        }
        return hidden;
    }

    private static void renderFuelChannelInsert(GeometryGeneration generation,
                                                RbmkComponentBlock.Kind kind, PoseStack poseStack,
                                                MultiBufferSource bufferSource, BlockState state,
                                                int packedLight, int packedOverlay) {
        ModelResourceLocation cap = FUEL_CAPS.get(kind);
        ModelResourceLocation inner = FUEL_INNERS.get(kind);
        if (cap == null || inner == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        renderCaptured(generation, cap, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderCaptured(generation, inner, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderFuelRods(GeometryGeneration generation, int color, int height,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        for (int y = 0; y < height; y++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, y, 0.0D);
            renderCapturedTinted(generation, FUEL_RODS, state, poseStack, bufferSource,
                    packedLight, packedOverlay, 0xFF000000 | color);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderCherenkov(RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                        int height, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedOverlay) {
        if (snapshot.lastFlux() <= 5.0D) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        PoseStack.Pose pose = poseStack.last();
        double max = height;
        for (double y = 0.75D; y <= max; y += 0.25D) {
            cherenkovVertex(consumer, pose, -0.5D, y, -0.5D, packedOverlay);
            cherenkovVertex(consumer, pose, -0.5D, y, 0.5D, packedOverlay);
            cherenkovVertex(consumer, pose, 0.5D, y, 0.5D, packedOverlay);
            cherenkovVertex(consumer, pose, 0.5D, y, -0.5D, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void cherenkovVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(102, 230, 255, 26)
                .setUv(0.0F, 0.0F)
                .setOverlay(packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static ModelResourceLocation topModel(RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                                  RbmkComponentBlock.Kind kind,
                                                  ModelResourceLocation defaultTop) {
        if (kind.hasTopPipes() || kind.isControl()) {
            return defaultTop;
        }
        if (!snapshot.hasLid()) {
            return null;
        }
        if (snapshot.lidType() == RbmkComponentBlockEntity.LidType.GLASS) {
            return GLASS_TOPS.get(kind);
        }
        return defaultTop;
    }

    private static void renderCraneConsole(GeometryGeneration generation,
                                           RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                           float partialTick, PoseStack poseStack,
                                           MultiBufferSource bufferSource, BlockState state,
                                           int packedLight, int packedOverlay) {
        renderCaptured(generation, CRANE_CONSOLE_BODY, state, poseStack, bufferSource,
                packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.75D, 1.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) snapshot.craneTiltFront(partialTick)));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) snapshot.craneTiltLeft(partialTick)));
        poseStack.translate(-0.75D, -1.015D, 0.0D);
        renderCaptured(generation, CRANE_CONSOLE_JOYSTICK, state, poseStack, bufferSource,
                packedLight, packedOverlay);
        poseStack.popPose();

        double meterWave = Math.sin((System.currentTimeMillis() * 0.01D) % 360.0D) * 180.0D / Math.PI * 0.05D;
        renderCraneMeter(generation, CRANE_CONSOLE_METER1, 0.75D, meterWave + 135.0D - 270.0D * snapshot.craneLoadedHeat(), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderCraneMeter(generation, CRANE_CONSOLE_METER2, 0.25D, meterWave + 135.0D - 270.0D * snapshot.craneLoadedEnrichment(), poseStack, bufferSource, state, packedLight, packedOverlay);

        int lamp1 = snapshot.craneIsLoading() ? 0xFFCCCC00 : snapshot.craneHasLoadedItem() ? 0xFF00FF00 : 0xFF001900;
        int lamp2 = snapshot.craneAboveValidTarget() ? 0xFF00FF00 : 0xFFFF0000;
        renderCapturedTinted(generation, CRANE_CONSOLE_LAMP1, state, poseStack, bufferSource,
                LightTexture.FULL_BRIGHT, packedOverlay, lamp1);
        renderCapturedTinted(generation, CRANE_CONSOLE_LAMP2, state, poseStack, bufferSource,
                LightTexture.FULL_BRIGHT, packedOverlay, lamp2);
    }

    private static void renderCraneMeter(GeometryGeneration generation, ModelResourceLocation model,
                                         double z, double degrees, PoseStack poseStack,
                                         MultiBufferSource bufferSource, BlockState state,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.25D, z);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) degrees));
        poseStack.translate(0.0D, -1.25D, -z);
        renderCaptured(generation, model, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCraneStructure(GeometryGeneration generation,
                                             RbmkComponentBlockEntity rbmk,
                                             RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                             float partialTick, PoseStack poseStack,
                                             MultiBufferSource bufferSource, BlockState state,
                                             int packedLight, int packedOverlay,
                                             MachineBuffers buffers) {
        if (snapshot.craneCenter() == null) {
            return;
        }
        poseStack.pushPose();
        double cranePosX = -rbmk.getBlockPos().getX() + snapshot.craneCenter().getX();
        double cranePosY = -rbmk.getBlockPos().getY() + snapshot.craneCenter().getY() + 1.0D;
        double cranePosZ = -rbmk.getBlockPos().getZ() + snapshot.craneCenter().getZ();
        poseStack.translate(cranePosX, cranePosY, cranePosZ);
        poseStack.translate(0.5D, -1.0D, 0.5D);
        applyCraneStructureFacing(poseStack, rbmkCraneStructureFacing(state));

        double posFront = snapshot.cranePosFront(partialTick);
        double posLeft = snapshot.cranePosLeft(partialTick);
        poseStack.translate(-posFront, 0.0D, posLeft);
        int craneRotationOffset = snapshot.craneRotationOffset();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(craneRotationOffset));

        poseStack.pushPose();
        int girderSpan = 0;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-craneRotationOffset));
        switch (Math.floorMod(craneRotationOffset, 360)) {
            case 0 -> {
                girderSpan = snapshot.craneSpanF() + snapshot.craneSpanB() + 1;
                poseStack.translate(posFront + snapshot.craneSpanB(), 0.0D, 0.0D);
            }
            case 90 -> {
                girderSpan = snapshot.craneSpanL() + snapshot.craneSpanR() + 1;
                poseStack.translate(0.0D, 0.0D, -posLeft - snapshot.craneSpanR());
            }
            case 180 -> {
                girderSpan = snapshot.craneSpanF() + snapshot.craneSpanB() + 1;
                poseStack.translate(posFront - snapshot.craneSpanF(), 0.0D, 0.0D);
            }
            case 270 -> {
                girderSpan = snapshot.craneSpanL() + snapshot.craneSpanR() + 1;
                poseStack.translate(0.0D, 0.0D, -posLeft + snapshot.craneSpanL());
            }
            default -> throw new IllegalStateException("Unsupported RBMK crane rotation offset: " + craneRotationOffset);
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(craneRotationOffset));
        if (buffers == null) {
            renderRepeatedCaptured(generation, CRANE_GIRDER, state, girderSpan,
                    -1, 0, 0, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            buffers.prepareCrane(generation, snapshot, state, girderSpan,
                    Math.max(0, snapshot.craneHeight() - 6), packedLight, packedOverlay);
            drawLayers(buffers.girderLayers(), poseStack);
        }
        poseStack.popPose();

        renderCaptured(generation, CRANE_MAIN, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        int tubeCount = Math.max(0, snapshot.craneHeight() - 6);
        if (buffers == null) {
            renderRepeatedCaptured(generation, CRANE_TUBE, state, tubeCount,
                    0, 1, 0, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            drawLayers(buffers.tubeLayers(), poseStack);
        }
        poseStack.translate(0.0D, tubeCount, 0.0D);
        poseStack.translate(0.0D, -1.0D, 0.0D);
        renderCaptured(generation, CRANE_CARRIAGE, state, poseStack, bufferSource,
                packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.translate(0.0D, -3.25D * (1.0D - snapshot.craneProgress(partialTick)), 0.0D);
        renderCaptured(generation, CRANE_LIFT, state, poseStack, bufferSource,
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderColumnDirect(GeometryGeneration generation,
                                           RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        RbmkComponentBlock.Kind kind = snapshot.kind();
        BlockState state = snapshot.blockState();
        ModelResourceLocation segment = SEGMENTS.get(kind);
        ModelResourceLocation defaultTop = TOPS.get(kind);
        int height = snapshot.columnHeight();
        int topSegment = height - 1;
        for (int y = 0; y < height; y++) {
            ModelResourceLocation selectedTop = y == topSegment
                    ? topModel(snapshot, kind, defaultTop) : null;
            poseStack.pushPose();
            poseStack.translate(0.0F, y, 0.0F);
            renderCapturedExcept(generation, segment, state, poseStack, bufferSource,
                    packedLight, packedOverlay,
                    hiddenColumnFaces(snapshot, y, topSegment, selectedTop != null));
            renderFuelChannelInsert(generation, kind, poseStack, bufferSource, state,
                    packedLight, packedOverlay);
            if (selectedTop != null) {
                renderCaptured(generation, selectedTop, state, poseStack, bufferSource,
                        packedLight, packedOverlay);
            }
            poseStack.popPose();
        }
        if (kind.acceptsFuel() && snapshot.hasFuelRod()) {
            renderFuelRods(generation, snapshot.fuelChannelColor(), height, poseStack,
                    bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderRepeatedCaptured(GeometryGeneration generation,
                                               ModelResourceLocation location,
                                               BlockState state, int count,
                                               int stepX, int stepY, int stepZ,
                                               PoseStack poseStack,
                                               MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        for (int index = 0; index < count; index++) {
            renderCaptured(generation, location, state, poseStack, bufferSource,
                    packedLight, packedOverlay);
            poseStack.translate(stepX, stepY, stepZ);
        }
        poseStack.popPose();
    }

    private static void renderCaptured(GeometryGeneration generation,
                                       ModelResourceLocation location, BlockState state,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        CapturedModel model = generation.model(state, location);
        if (model != null) {
            model.render(poseStack, bufferSource, packedLight, packedOverlay, null, null);
        }
    }

    private static void renderCapturedTinted(GeometryGeneration generation,
                                             ModelResourceLocation location, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay, int argb) {
        CapturedModel model = generation.model(state, location);
        if (model != null) {
            model.render(poseStack, bufferSource, packedLight, packedOverlay, argb, null);
        }
    }

    private static void renderCapturedExcept(GeometryGeneration generation,
                                             ModelResourceLocation location, BlockState state,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay,
                                             EnumSet<Direction> excluded) {
        CapturedModel model = generation.model(state, location);
        if (model != null) {
            model.render(poseStack, bufferSource, packedLight, packedOverlay, null, excluded);
        }
    }

    private static void updateRenderSnapshot(RbmkComponentBlockEntity rbmk) {
        GeometryGeneration generation = geometryGeneration;
        RbmkComponentBlockEntity.RenderSnapshot inputs = rbmk.captureClientRenderInputs();
        long revision = RbmkComponentBlockEntity.clientRenderSnapshotRevision();
        if (generation == null || !generation.containsState(inputs.blockState())) {
            rbmk.publishClientRenderSnapshot(inputs.withGeometry(
                    null, zeroVolumeBounds(rbmk.getBlockPos()), revision));
            return;
        }
        RbmkComponentBlockEntity.RenderSnapshot previous = rbmk.renderSnapshot();
        if (sameBoundsInputs(previous, inputs, generation)) {
            rbmk.publishClientRenderSnapshot(inputs.withGeometry(
                    generation, previous.bounds(), revision));
            return;
        }
        RbmkComponentBlockEntity.RenderSnapshot provisional = inputs.withGeometry(
                generation, zeroVolumeBounds(rbmk.getBlockPos()), revision);
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        renderScene(rbmk, provisional, generation, 0.0F, new PoseStack(), capture,
                0, OverlayTexture.NO_OVERLAY, null);
        renderScene(rbmk, provisional, generation, 1.0F, new PoseStack(), capture,
                0, OverlayTexture.NO_OVERLAY, null);
        AABB bounds = capture.bounds(rbmk.getBlockPos());
        if (inputs.kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            bounds = bounds.minmax(craneConsoleRotationEnvelope(
                    generation, inputs.blockState(), inputs.facing(), rbmk.getBlockPos()));
        }
        rbmk.publishClientRenderSnapshot(inputs.withGeometry(generation, bounds, revision));
    }

    private static boolean sameBoundsInputs(RbmkComponentBlockEntity.RenderSnapshot previous,
                                            RbmkComponentBlockEntity.RenderSnapshot next,
                                            GeometryGeneration generation) {
        if (previous == null || previous.geometryToken() != generation
                || previous.kind() != next.kind()
                || !previous.blockState().equals(next.blockState())
                || previous.columnHeight() != next.columnHeight()) {
            return false;
        }
        RbmkComponentBlock.Kind kind = next.kind();
        if (kind.isColumn()) {
            return previous.hiddenHorizontalFaceMask() == next.hiddenHorizontalFaceMask()
                    && previous.hasLid() == next.hasLid()
                    && previous.lidType() == next.lidType()
                    && previous.hasFuelRod() == next.hasFuelRod()
                    && (previous.lastFlux() > 5.0D) == (next.lastFlux() > 5.0D)
                    && (!kind.isControl()
                    || previous.previousControlLevel() == next.previousControlLevel()
                    && previous.controlLevel() == next.controlLevel()
                    && previous.colorGroup() == next.colorGroup());
        }
        if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            return previous.previousAutoloaderPiston() == next.previousAutoloaderPiston()
                    && previous.autoloaderPiston() == next.autoloaderPiston();
        }
        if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE
                || kind == RbmkComponentBlock.Kind.CONSOLE || kind.isMiniPanel()) {
            return false;
        }
        return true;
    }

    /**
     * The joystick interpolates two rotations and both meters rotate from the
     * wall clock. Their immutable baked vertices define rotation spheres; a
     * sphere contains every continuous angle without sampling or a guessed margin.
     */
    private static AABB craneConsoleRotationEnvelope(GeometryGeneration generation,
                                                      BlockState state, Direction facing,
                                                      net.minecraft.core.BlockPos pos) {
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(pos);
        PoseStack outer = new PoseStack();
        applyCraneConsoleTransform(outer, facing);

        CapturedModel joystick = generation.model(state, CRANE_CONSOLE_JOYSTICK);
        includeRotationSphere(bounds, outer, joystick,
                0.75F, 1.0F, 0.0F, -0.75F, -1.015F, 0.0F);
        CapturedModel meter1 = generation.model(state, CRANE_CONSOLE_METER1);
        CapturedModel meter2 = generation.model(state, CRANE_CONSOLE_METER2);
        includeRotationSphere(bounds, outer, meter1,
                0.0F, 1.25F, 0.75F, 0.0F, -1.25F, -0.75F);
        includeRotationSphere(bounds, outer, meter2,
                0.0F, 1.25F, 0.25F, 0.0F, -1.25F, -0.25F);
        return bounds.build();
    }

    private static void includeRotationSphere(RenderBounds.FloatBuilder bounds,
                                              PoseStack outer, CapturedModel model,
                                              float pivotX, float pivotY, float pivotZ,
                                              float preX, float preY, float preZ) {
        if (model == null || model.vertices().isEmpty()) {
            return;
        }
        double squared = 0.0D;
        for (CapturedVertex vertex : model.vertices()) {
            double x = vertex.x() + preX;
            double y = vertex.y() + preY;
            double z = vertex.z() + preZ;
            squared = Math.max(squared, x * x + y * y + z * z);
        }
        float radius = Math.nextUp((float) Math.sqrt(squared));
        Vector3f center = outer.last().pose().transformPosition(
                new Vector3f(pivotX, pivotY, pivotZ));
        bounds.include(new Vector3f(Math.nextDown(center.x - radius),
                Math.nextDown(center.y - radius), Math.nextDown(center.z - radius)));
        bounds.include(new Vector3f(Math.nextUp(center.x + radius),
                Math.nextUp(center.y + radius), Math.nextUp(center.z + radius)));
    }

    private static AABB zeroVolumeBounds(net.minecraft.core.BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static CapturedModel captureModel(ModelResourceLocation location, BlockState state) {
        BakedModel model = MachineModelRenderer.model(location);
        RandomSource random = RandomSource.create(42L);
        List<CapturedLayer> layers = new ArrayList<>();
        List<CapturedVertex> vertices = new ArrayList<>();
        random.setSeed(42L);
        for (RenderType sourceRenderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> general = captureQuads(model, state, null, sourceRenderType,
                    random, vertices);
            EnumMap<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
            for (Direction side : Direction.values()) {
                List<BakedQuad> sideQuads = captureQuads(model, state, side, sourceRenderType,
                        random, vertices);
                if (!sideQuads.isEmpty()) {
                    faces.put(side, sideQuads);
                }
            }
            if (!general.isEmpty() || !faces.isEmpty()) {
                layers.add(new CapturedLayer(sourceRenderType, general, Map.copyOf(faces)));
            }
        }
        return new CapturedModel(List.copyOf(layers), List.copyOf(vertices));
    }

    private static List<BakedQuad> captureQuads(BakedModel model, BlockState state,
                                                Direction side, RenderType renderType,
                                                RandomSource random, List<CapturedVertex> vertices) {
        random.setSeed(42L);
        List<BakedQuad> quads = List.copyOf(
                model.getQuads(state, side, random, ModelData.EMPTY, renderType));
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                vertices.add(new CapturedVertex(
                        Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 1]),
                        Float.intBitsToFloat(data[offset + 2])));
            }
        }
        return quads;
    }

    private static List<GpuLayer> buildColumnLayers(GeometryGeneration generation,
                                                    RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                                    int packedLight, int packedOverlay) {
        Set<RenderType> targets = new LinkedHashSet<>();
        RbmkComponentBlock.Kind kind = snapshot.kind();
        BlockState state = snapshot.blockState();
        ModelResourceLocation selectedTop = topModel(snapshot, kind, TOPS.get(kind));
        addTargetTypes(targets, generation.model(state, SEGMENTS.get(kind)), false);
        addTargetTypes(targets, generation.model(state, FUEL_CAPS.get(kind)), false);
        addTargetTypes(targets, generation.model(state, FUEL_INNERS.get(kind)), false);
        addTargetTypes(targets, generation.model(state, selectedTop), false);
        int topSegment = snapshot.columnHeight() - 1;
        return buildGpuLayers(targets, (target, consumer) -> {
            PoseStack buildPose = new PoseStack();
            for (int y = 0; y < snapshot.columnHeight(); y++) {
                ModelResourceLocation top = y == topSegment ? selectedTop : null;
                CapturedModel segment = generation.model(state, SEGMENTS.get(kind));
                if (segment != null) {
                    segment.emit(target, buildPose, consumer, packedLight, packedOverlay,
                            null, hiddenColumnFaces(snapshot, y, topSegment, top != null));
                }
                CapturedModel cap = generation.model(state, FUEL_CAPS.get(kind));
                CapturedModel inner = generation.model(state, FUEL_INNERS.get(kind));
                buildPose.pushPose();
                buildPose.translate(0.5F, 0.0F, 0.5F);
                if (cap != null) {
                    cap.emit(target, buildPose, consumer, packedLight, packedOverlay, null, null);
                }
                if (inner != null) {
                    inner.emit(target, buildPose, consumer, packedLight, packedOverlay, null, null);
                }
                buildPose.popPose();
                CapturedModel topGeometry = generation.model(state, top);
                if (topGeometry != null) {
                    topGeometry.emit(target, buildPose, consumer, packedLight, packedOverlay, null, null);
                }
                buildPose.translate(0.0D, 1.0D, 0.0D);
            }
        });
    }

    private static List<GpuLayer> buildFuelLayers(GeometryGeneration generation,
                                                  RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                                  int packedLight, int packedOverlay) {
        if (!snapshot.kind().acceptsFuel() || !snapshot.hasFuelRod()) {
            return List.of();
        }
        CapturedModel rods = generation.model(snapshot.blockState(), FUEL_RODS);
        Set<RenderType> targets = new LinkedHashSet<>();
        addTargetTypes(targets, rods, true);
        int argb = 0xFF000000 | snapshot.fuelChannelColor();
        return buildGpuLayers(targets, (target, consumer) -> {
            PoseStack buildPose = new PoseStack();
            buildPose.translate(0.5F, 0.0F, 0.5F);
            for (int y = 0; y < snapshot.columnHeight(); y++) {
                rods.emit(target, buildPose, consumer, packedLight, packedOverlay, argb, null);
                buildPose.translate(0.0D, 1.0D, 0.0D);
            }
        });
    }

    private static List<GpuLayer> buildRepeatedLayers(GeometryGeneration generation,
                                                      ModelResourceLocation location,
                                                      BlockState state, int count,
                                                      int stepX, int stepY, int stepZ,
                                                      int packedLight, int packedOverlay) {
        CapturedModel model = generation.model(state, location);
        Set<RenderType> targets = new LinkedHashSet<>();
        addTargetTypes(targets, model, false);
        return buildGpuLayers(targets, (target, consumer) -> {
            PoseStack buildPose = new PoseStack();
            for (int index = 0; index < count; index++) {
                model.emit(target, buildPose, consumer, packedLight, packedOverlay, null, null);
                buildPose.translate(stepX, stepY, stepZ);
            }
        });
    }

    private static void addTargetTypes(Set<RenderType> targets, CapturedModel model, boolean tinted) {
        if (model != null) {
            model.addTargetTypes(targets, tinted);
        }
    }

    private static List<GpuLayer> buildGpuLayers(Set<RenderType> targets, BatchEmitter emitter) {
        if (targets.isEmpty()) {
            return List.of();
        }
        List<GpuLayer> built = new ArrayList<>(targets.size());
        try {
            for (RenderType target : targets) {
                BufferBuilder builder = Tesselator.getInstance().begin(target.mode(), target.format());
                emitter.emit(target, builder);
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
                    layer.buffer().drawWithShader(MachineModelRenderer.composeModelView(poseStack),
                            RenderSystem.getProjectionMatrix(), shader);
                }
            } finally {
                VertexBuffer.unbind();
                layer.renderType().clearRenderState();
            }
        }
    }

    private static void discardMachineCache(RbmkComponentBlockEntity rbmk) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> discardMachineCache(rbmk));
            return;
        }
        List<RbmkComponentBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (RbmkComponentBlockEntityRenderer renderer : renderers) {
            renderer.closeMachineCache(rbmk);
        }
    }

    private static void closeAllGpuCaches() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(RbmkComponentBlockEntityRenderer::closeAllGpuCaches);
            return;
        }
        List<RbmkComponentBlockEntityRenderer> renderers;
        synchronized (INSTANCES) {
            renderers = List.copyOf(INSTANCES);
        }
        for (RbmkComponentBlockEntityRenderer renderer : renderers) {
            renderer.closeAllMachineCaches();
        }
    }

    private void closeMachineCache(RbmkComponentBlockEntity rbmk) {
        MachineBuffers removed = machineBuffers.remove(rbmk);
        if (removed != null) {
            removed.close();
        }
    }

    private void closeAllMachineCaches() {
        for (MachineBuffers buffers : machineBuffers.values()) {
            buffers.close();
        }
        machineBuffers.clear();
    }

    private static void closeLayers(List<GpuLayer> layers) {
        if (layers != null) {
            for (GpuLayer layer : layers) {
                layer.buffer().close();
            }
        }
    }

    /** RenderRBMKConsole metadata-minus-offset table: 2/4/3/5 -> 90/180/270/0. */
    private static void applyConsoleTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK console facing must be horizontal: " + facing);
        }
        poseStack.translate(0.5F, 0.0F, 0.0F);
    }

    /** RenderCraneConsole uses its own copy of the console metadata table. */
    private static void applyCraneConsoleTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK crane console facing must be horizontal: " + facing);
        }
        poseStack.translate(0.5F, 0.0F, 0.0F);
    }

    /** RenderCraneConsole's separately based crane structure orientation. */
    private static void applyCraneStructureFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK crane structure facing must be horizontal: " + facing);
        }
    }

    private static void renderConsoleScreens(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource) {
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(-0.42F, 3.5F, 1.75F);
        for (int slot = 0; slot < 6; slot++) {
            Component text = consoleScreenText(rbmk, slot);
            if (text == null) {
                continue;
            }
            poseStack.pushPose();
            if (slot % 2 == 1) {
                poseStack.translate(0.0F, 0.0F, -3.5F);
            }
            poseStack.translate(0.0F, -0.75F * (slot / 2), 0.0F);
            int width = font.width(text);
            int height = font.lineHeight;
            float scale = Math.min(0.03F, 0.8F / Math.max(width, 1));
            poseStack.scale(scale, -scale, scale);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            font.drawInBatch(
                    text,
                    -width / 2.0F,
                    -height / 2.0F,
                    0x00FF00,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    LightTexture.FULL_BRIGHT
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static Component consoleScreenText(RbmkComponentBlockEntity rbmk, int slot) {
        RbmkComponentBlockEntity.ConsoleScreenType type = RbmkComponentBlockEntity.ConsoleScreenType.byOrdinal(rbmk.consoleScreenType(slot));
        if (type == RbmkComponentBlockEntity.ConsoleScreenType.NONE) {
            return null;
        }
        String value = formatTenths(rbmk.consoleScreenDisplay(slot));
        return switch (type) {
            case COL_TEMP -> Component.translatable("rbmk.screen.temp", value);
            case FUEL_DEPLETION -> Component.translatable("rbmk.screen.depletion", value);
            case FUEL_POISON -> Component.translatable("rbmk.screen.xenon", value);
            case FUEL_TEMP -> Component.translatable("rbmk.screen.core", value);
            case ROD_EXTRACTION -> Component.translatable("rbmk.screen.rod", value);
            case NONE -> null;
        };
    }

    private static String formatTenths(int tenths) {
        return tenths % 10 == 0
                ? Integer.toString(tenths / 10)
                : String.format(Locale.ROOT, "%.1f", tenths / 10.0D);
    }

    private static void renderConsoleBoard(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT; index++) {
            int kindOrdinal = rbmk.consoleKind(index);
            if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
                continue;
            }
            double x = -0.3725D;
            double y = -(index / RbmkComponentBlockEntity.CONSOLE_GRID_SIZE) * 0.125D + 3.625D;
            double z = -(index % RbmkComponentBlockEntity.CONSOLE_GRID_SIZE) * 0.125D + 0.125D * 7.0D;
            int color = consoleBoardColor(rbmk, index);
            addConsoleBoardColumn(consumer, pose, x, y, z, color);
            RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
            if (kind.acceptsFuel()) {
                // Legacy RenderRBMKConsole used the rod's enrichment, not
                // its spent fraction: green is brightest for fresh fuel and
                // fades as depletion rises.
                int enrichment = 1000 - rbmk.consoleFuelDepletion(index);
                int green = 64 + Math.min(191, Math.max(0, enrichment * 191 / 1000));
                addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, 0, green, 0);
            } else if (kind.isControl()) {
                int level = Math.min(255, Math.max(0, rbmk.consoleControl(index) * 255 / 100));
                if (kind.isAutomaticControl()) {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, 0, level);
                } else {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, level, 0);
                }
            }
        }
    }

    private static void renderDisplayBoard(RbmkComponentBlockEntity rbmk,
                                           RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                           PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        applyDisplayBoardTransform(poseStack, snapshot.facing());
        poseStack.translate(0.0D, 0.5D, 0.0D);
        poseStack.scale(1.0F, 8.0F / 7.0F, 8.0F / 7.0F);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < RbmkComponentBlockEntity.DISPLAY_COLUMN_COUNT; index++) {
            int kindOrdinal = rbmk.displayKind(index);
            if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
                continue;
            }
            double x = 0.28125D;
            double y = -(index / RbmkComponentBlockEntity.DISPLAY_GRID_SIZE) * 0.125D + 0.875D;
            double z = -(index % RbmkComponentBlockEntity.DISPLAY_GRID_SIZE) * 0.125D + 0.125D * 3.0D;
            int color = displayBoardColor(rbmk, index);
            addConsoleBoardColumn(consumer, pose, x, y, z, color);
            RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
            if (kind.acceptsFuel()) {
                // Same enrichment-based color as the 1.7.10 display
                // renderer; depletion is the inverse quantity in modern NBT.
                int enrichment = 1000 - rbmk.displayFuelDepletion(index);
                int green = 64 + Math.min(191, Math.max(0, enrichment * 191 / 1000));
                addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, 0, green, 0);
            } else if (kind.isControl()) {
                int level = Math.min(255, Math.max(0, rbmk.displayControl(index) * 255 / 100));
                if (kind.isAutomaticControl()) {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, 0, level);
                } else {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, level, 0);
                }
            }
        }
        poseStack.popPose();
    }

    private static int consoleBoardColor(RbmkComponentBlockEntity rbmk, int index) {
        if (rbmk.consoleCraneIndicator(index) > 0) {
            return 0xFFFFFF00;
        }
        int group = rbmk.consoleColorGroup(index);
        if (group >= 0) {
            return switch (group) {
                case 0 -> 0xFFFF0000;
                case 1 -> 0xFFFFFF00;
                case 2 -> 0xFF008000;
                case 3 -> 0xFF0000FF;
                case 4 -> 0xFF8000FF;
                default -> 0xFFFFFFFF;
            };
        }
        int maxHeat = Math.max(1, rbmk.consoleMaxHeat(index));
        double heat = Math.max(0.0D, Math.min(1.0D, rbmk.consoleHeat(index) / (double) maxHeat));
        double base = 0.65D + (index % 2) * 0.05D;
        int red = (int) Math.round((base + (1.0D - base) * heat) * 255.0D);
        int gb = (int) Math.round(base * 255.0D);
        return 0xFF000000 | (red << 16) | (gb << 8) | gb;
    }

    private static int displayBoardColor(RbmkComponentBlockEntity rbmk, int index) {
        if (rbmk.displayCraneIndicator(index) > 0) {
            return 0xFFFFFF00;
        }
        int group = rbmk.displayColorGroup(index);
        if (group >= 0) {
            return switch (group) {
                case 0 -> 0xFFFF0000;
                case 1 -> 0xFFFFFF00;
                case 2 -> 0xFF008000;
                case 3 -> 0xFF0000FF;
                case 4 -> 0xFF8000FF;
                default -> 0xFFFFFFFF;
            };
        }
        int maxHeat = Math.max(1, rbmk.displayMaxHeat(index));
        double heat = Math.max(0.0D, Math.min(1.0D, rbmk.displayHeat(index) / (double) maxHeat));
        double base = 0.65D + (index % 2) * 0.05D;
        int red = (int) Math.round((base + (1.0D - base) * heat) * 255.0D);
        int gb = (int) Math.round(base * 255.0D);
        return 0xFF000000 | (red << 16) | (gb << 8) | gb;
    }

    private static void addConsoleBoardColumn(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int argb) {
        double width = 0.0625D * 0.75D;
        addConsoleBoardVertex(consumer, pose, x, y + width, z - width, argb);
        addConsoleBoardVertex(consumer, pose, x, y + width, z + width, argb);
        addConsoleBoardVertex(consumer, pose, x, y - width, z + width, argb);
        addConsoleBoardVertex(consumer, pose, x, y - width, z - width, argb);
    }

    private static void addConsoleBoardDot(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int red, int green, int blue) {
        double width = 0.03125D;
        double edge = 0.022097D;
        int argb = 0xFF000000 | (red << 16) | (green << 8) | blue;
        addConsoleBoardVertex(consumer, pose, x, y + width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y + edge, z + edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y, z + width, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z + edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y + edge, z - edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y + width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z - edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y, z - width, argb);
        addConsoleBoardVertex(consumer, pose, x, y + width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z + edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y - width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z - edge, argb);
    }

    private static void addConsoleBoardVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int argb) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(argb)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 1.0F, 0.0F, 0.0F);
    }

    private static Direction rbmkConsoleFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK console state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK console state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkCraneConsoleFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK crane console state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK crane console state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkCraneStructureFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK crane structure state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK crane structure state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkGaugeFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK gauge state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK gauge state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkGraphFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK graph state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK graph state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkNumitronFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK numitron state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK numitron state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkTerminalFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK terminal state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK terminal state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkLeverFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK lever state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK lever state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkIndicatorFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK indicator state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK indicator state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction rbmkKeypadFacing(BlockState state) {
        if (!state.hasProperty(RbmkComponentBlock.FACING)) {
            throw new IllegalStateException("RBMK keypad state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RbmkComponentBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("RBMK keypad state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static void applyMiniPanelBaseTransform(PoseStack poseStack,
                                                    RbmkComponentBlock.Kind kind,
                                                    Direction facing) {
        switch (kind) {
            case DISPLAY -> applyDisplayMiniBaseTransform(poseStack, facing);
            case DISPLAY_BLANK -> applyDisplayBlankMiniBaseTransform(poseStack, facing);
            case GAUGE -> applyGaugePanelTransform(poseStack, facing);
            case GRAPH -> applyGraphPanelTransform(poseStack, facing);
            case LEVER -> applyLeverPanelTransform(poseStack, facing);
            case INDICATOR -> applyIndicatorPanelTransform(poseStack, facing);
            case KEY_PAD -> applyKeypadPanelTransform(poseStack, facing);
            case NUMITRON -> applyNumitronPanelTransform(poseStack, facing);
            case TERMINAL -> applyTerminalPanelTransform(poseStack, facing);
            default -> throw new IllegalArgumentException("Not an RBMK mini-panel kind: " + kind);
        }
    }

    /** RenderRBMKDisplay metadata table: 2/4/3/5 -> 90/180/270/0. */
    private static void applyDisplayBoardTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK display board facing must be horizontal: " + facing);
        }
    }

    private static void applyDisplayMiniBaseTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK display base facing must be horizontal: " + facing);
        }
    }

    private static void applyDisplayBlankMiniBaseTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK blank display base facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKGauge metadata table. */
    private static void applyGaugePanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK gauge facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKGraph metadata table. */
    private static void applyGraphPanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK graph facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKLever metadata table. */
    private static void applyLeverPanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK lever facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKIndicator metadata table. */
    private static void applyIndicatorPanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK indicator facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKKeyPad metadata table. */
    private static void applyKeypadPanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK keypad facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKNumitron metadata table. */
    private static void applyNumitronPanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK numitron facing must be horizontal: " + facing);
        }
    }

    /** RenderRBMKTerminal metadata table. */
    private static void applyTerminalPanelTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("RBMK terminal facing must be horizontal: " + facing);
        }
    }

    private record StateModelKey(BlockState state, ModelResourceLocation location) {
    }

    private record GeometryGeneration(int generation,
                                      Map<StateModelKey, CapturedModel> models,
                                      Set<BlockState> states) {
        private CapturedModel model(BlockState state, ModelResourceLocation location) {
            return location == null ? null : models.get(new StateModelKey(state, location));
        }

        private boolean containsState(BlockState state) {
            return states.contains(state);
        }
    }

    private record CapturedLayer(RenderType sourceRenderType,
                                 List<BakedQuad> general,
                                 Map<Direction, List<BakedQuad>> faces) {
        private RenderType target(boolean tinted) {
            return RenderTypeHelper.getEntityRenderType(sourceRenderType, tinted);
        }
    }

    private record CapturedVertex(float x, float y, float z) {
    }

    private record CapturedModel(List<CapturedLayer> layers, List<CapturedVertex> vertices) {
        private void render(PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay, Integer argb,
                            EnumSet<Direction> excluded) {
            boolean tinted = argb != null;
            for (CapturedLayer layer : layers) {
                VertexConsumer consumer = bufferSource.getBuffer(layer.target(tinted));
                emitLayer(layer, poseStack, consumer, packedLight, packedOverlay, argb, excluded);
            }
        }

        private void emit(RenderType target, PoseStack poseStack, VertexConsumer consumer,
                          int packedLight, int packedOverlay, Integer argb,
                          EnumSet<Direction> excluded) {
            boolean tinted = argb != null;
            for (CapturedLayer layer : layers) {
                if (layer.target(tinted).equals(target)) {
                    emitLayer(layer, poseStack, consumer, packedLight, packedOverlay, argb, excluded);
                }
            }
        }

        private void addTargetTypes(Set<RenderType> targets, boolean tinted) {
            for (CapturedLayer layer : layers) {
                targets.add(layer.target(tinted));
            }
        }

        private static void emitLayer(CapturedLayer layer, PoseStack poseStack,
                                      VertexConsumer consumer, int packedLight,
                                      int packedOverlay, Integer argb,
                                      EnumSet<Direction> excluded) {
            emitQuads(layer.general(), poseStack, consumer, packedLight, packedOverlay, argb);
            for (Direction side : Direction.values()) {
                if (excluded != null && excluded.contains(side)) {
                    continue;
                }
                List<BakedQuad> sideQuads = layer.faces().get(side);
                if (sideQuads != null) {
                    emitQuads(sideQuads, poseStack, consumer, packedLight, packedOverlay, argb);
                }
            }
        }

        private static void emitQuads(List<BakedQuad> quads, PoseStack poseStack,
                                      VertexConsumer consumer, int packedLight,
                                      int packedOverlay, Integer argb) {
            if (argb == null) {
                MachineModelRenderer.renderCapturedQuads(
                        quads, poseStack, consumer, packedLight, packedOverlay);
            } else {
                MachineModelRenderer.emitCapturedQuadsTinted(
                        quads, poseStack, consumer, packedLight, packedOverlay, argb);
            }
        }
    }

    @FunctionalInterface
    private interface BatchEmitter {
        void emit(RenderType target, VertexConsumer consumer);
    }

    private record GpuLayer(RenderType renderType, VertexBuffer buffer) {
    }

    private record ColumnKey(int generation, RbmkComponentBlock.Kind kind,
                             int height, int hiddenFaces, BlockState state,
                             boolean hasLid, RbmkComponentBlockEntity.LidType lidType,
                             int packedLight, int packedOverlay) {
    }

    private record FuelKey(int generation, RbmkComponentBlock.Kind kind,
                           int height, BlockState state, boolean present,
                           int color, int packedLight, int packedOverlay) {
    }

    private record GirderKey(int generation, BlockState state,
                             int spanF, int spanB, int spanL, int spanR,
                             int rotation, int count,
                             int packedLight, int packedOverlay) {
    }

    private record TubeKey(int generation, BlockState state, int craneHeight,
                           int count, int packedLight, int packedOverlay) {
    }

    private static final class MachineBuffers {
        private ColumnKey columnKey;
        private FuelKey fuelKey;
        private GirderKey girderKey;
        private TubeKey tubeKey;
        private List<GpuLayer> columnLayers;
        private List<GpuLayer> fuelLayers;
        private List<GpuLayer> girderLayers;
        private List<GpuLayer> tubeLayers;

        private void prepareColumn(GeometryGeneration generation,
                                   RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                   int packedLight, int packedOverlay) {
            ColumnKey requestedColumn = new ColumnKey(generation.generation(), snapshot.kind(),
                    snapshot.columnHeight(), snapshot.hiddenHorizontalFaceMask(),
                    snapshot.blockState(), snapshot.hasLid(), snapshot.lidType(),
                    packedLight, packedOverlay);
            if (!requestedColumn.equals(columnKey)) {
                closeLayers(columnLayers);
                columnLayers = buildColumnLayers(generation, snapshot, packedLight, packedOverlay);
                columnKey = requestedColumn;
            }
            FuelKey requestedFuel = new FuelKey(generation.generation(), snapshot.kind(),
                    snapshot.columnHeight(), snapshot.blockState(), snapshot.hasFuelRod(),
                    snapshot.fuelChannelColor(), packedLight, packedOverlay);
            if (!requestedFuel.equals(fuelKey)) {
                closeLayers(fuelLayers);
                fuelLayers = buildFuelLayers(generation, snapshot, packedLight, packedOverlay);
                fuelKey = requestedFuel;
            }
        }

        private void prepareCrane(GeometryGeneration generation,
                                  RbmkComponentBlockEntity.RenderSnapshot snapshot,
                                  BlockState state, int girderCount, int tubeCount,
                                  int packedLight, int packedOverlay) {
            GirderKey requestedGirder = new GirderKey(generation.generation(), state,
                    snapshot.craneSpanF(), snapshot.craneSpanB(),
                    snapshot.craneSpanL(), snapshot.craneSpanR(),
                    snapshot.craneRotationOffset(), girderCount, packedLight, packedOverlay);
            if (!requestedGirder.equals(girderKey)) {
                closeLayers(girderLayers);
                girderLayers = buildRepeatedLayers(generation, CRANE_GIRDER, state,
                        girderCount, -1, 0, 0, packedLight, packedOverlay);
                girderKey = requestedGirder;
            }
            TubeKey requestedTube = new TubeKey(generation.generation(), state,
                    snapshot.craneHeight(), tubeCount, packedLight, packedOverlay);
            if (!requestedTube.equals(tubeKey)) {
                closeLayers(tubeLayers);
                tubeLayers = buildRepeatedLayers(generation, CRANE_TUBE, state,
                        tubeCount, 0, 1, 0, packedLight, packedOverlay);
                tubeKey = requestedTube;
            }
        }

        private List<GpuLayer> columnLayers() {
            return columnLayers == null ? List.of() : columnLayers;
        }

        private List<GpuLayer> fuelLayers() {
            return fuelLayers == null ? List.of() : fuelLayers;
        }

        private List<GpuLayer> girderLayers() {
            return girderLayers == null ? List.of() : girderLayers;
        }

        private List<GpuLayer> tubeLayers() {
            return tubeLayers == null ? List.of() : tubeLayers;
        }

        private void close() {
            closeLayers(columnLayers);
            closeLayers(fuelLayers);
            closeLayers(girderLayers);
            closeLayers(tubeLayers);
            columnLayers = null;
            fuelLayers = null;
            girderLayers = null;
            tubeLayers = null;
            columnKey = null;
            fuelKey = null;
            girderKey = null;
            tubeKey = null;
        }
    }

    @Override
    public AABB getRenderBoundingBox(RbmkComponentBlockEntity blockEntity) {
        return blockEntity.renderSnapshot().bounds();
    }
}
